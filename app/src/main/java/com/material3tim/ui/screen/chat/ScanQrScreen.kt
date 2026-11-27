package com.material3tim.ui.screen.chat

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.material3tim.R
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.util.ExternalLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 相机权限请求码（宿主 Activity 直接用 ActivityCompat.requestPermissions）。 */
private const val CAMERA_PERMISSION_REQUEST = 0x4D33

/** 扫一扫页面的状态。 */
internal sealed interface ScanQrState {
    /**
     * 默认操作页：相机实时预览；可带上一次失败的原因（未识别/相机不可用/无法打开）
     * 与已识别但打不开的原始文本（供复制）。
     */
    data class Idle(
        val errorKind: ScanQrError? = null,
        val errorContent: String = "",
    ) : ScanQrState

    /** 正在 IO 解码二维码（loading，按钮禁用）。 */
    data object Working : ScanQrState
}

/** 扫一扫失败原因。 */
internal enum class ScanQrError {
    /** Camerax 初始化/绑定失败（设备无相机、被占用、权限被系统撤销）。 */
    CameraUnavailable,

    /** 照片/相册图片里没有识别到二维码。 */
    NoQrCode,

    /** 识别成功但内容无法分发（无可用方式打开）。 */
    OpenFailed,
}

/**
 * 扫一扫页面入口（消息页加号菜单「扫一扫」→ `Route.ScanQr`）。
 *
 * 页面内直接用 CameraX 预览 + 扫码框 + ZXing 实时解码，不再拉起系统相机：
 * - 首次进入请求 CAMERA 权限；拒绝后显示权限说明，仍可「选择图片」；
 * - 预览下方有手电筒、相册、取消；
 * - 解出内容后按内容分发（http(s) → [ExternalLink]；其它优先
 *   [AppDataSources.qrActionSource]，再退化 ACTION_VIEW）；成功 Toast 并关闭，
 *   失败在取景框下方显示原因（可复制内容、继续扫）。
 */
@Composable
fun ScanQrScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ScanQrState>(ScanQrState.Idle()) }
    var granted by remember { mutableStateOf(hasCameraPermission(context)) }
    var denied by rememberSaveable { mutableStateOf(false) }
    var requested by rememberSaveable { mutableStateOf(false) }
    var lastText by remember { mutableStateOf("") }
    var lastDetectedAt by remember { mutableLongStateOf(0L) }

    // 从权限弹窗 / 系统设置回来时重新读权限状态。
    //
    // 这里不用 ActivityResultContracts.RequestPermission：宿主 TIM 的 Activity
    // 不是 ComponentActivity，我们的 HostOwners 只转发 onActivityResult，
    // 收不到 onRequestPermissionsResult；直接 requestPermissions + onResume 复查
    // 对宿主/独立 App 都成立。
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val ok = hasCameraPermission(context)
                granted = ok
                if (requested && !ok) denied = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 首次进入自动申请一次。
    LaunchedEffect(Unit) {
        if (!granted && !requested) {
            requested = true
            context.findActivity()?.let { activity ->
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_REQUEST,
                )
            }
        }
    }

    fun finishWithText(text: String) {
        val opened = runCatching { dispatchQrContent(context, text) }.getOrDefault(false)
        if (opened) {
            Toast.makeText(context, R.string.scan_opened, Toast.LENGTH_SHORT).show()
            onClose()
        } else {
            // 没有可打开的方式：回到取景页显示原因 + 可复制文本。
            state = ScanQrState.Idle(
                errorKind = ScanQrError.OpenFailed,
                errorContent = text,
            )
        }
    }

    fun handlePickedUri(uri: Uri) {
        state = ScanQrState.Working
        scope.launch {
            val text = withContext(Dispatchers.IO) { decodeQrFromUri(context, uri) }
            if (text.isNullOrBlank()) {
                state = ScanQrState.Idle(errorKind = ScanQrError.NoQrCode)
                return@launch
            }
            finishWithText(text)
        }
    }

    // 相机实时识别回调（在相机分析线程 post 回主线程后才进来）。
    fun onDetected(text: String) {
        if (state is ScanQrState.Working) return
        val now = SystemClock.uptimeMillis()
        // 同一个码连续出现（失败后仍未移开镜头）时不要反复分发/弹 Toast。
        if (text == lastText && now - lastDetectedAt < 3_000L) return
        lastText = text
        lastDetectedAt = now
        state = ScanQrState.Working
        scope.launch {
            // 分发本身要跳转 Activity，放主线程；只为统一 loading 状态。
            finishWithText(text)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            handlePickedUri(uri)
        }
    }

    val onPickImage: () -> Unit = {
        if (state !is ScanQrState.Working) {
            state = ScanQrState.Idle()
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }

    val onRequestPermission: () -> Unit = {
        val activity = context.findActivity()
        val canAskAgain = activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        if (denied && !canAskAgain) {
            // 已「拒绝且不再询问」：只能去应用详情页手动开。
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
                .onFailure { Toast.makeText(context, R.string.scan_permission_denied, Toast.LENGTH_SHORT).show() }
        } else if (activity != null) {
            requested = true
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST,
            )
        }
    }

    val onCopy: (String) -> Unit = { text ->
        runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("scan_result", text))
            Toast.makeText(context, R.string.scan_copied, Toast.LENGTH_SHORT).show()
        }
    }

    QrCameraScanner(
        state = state,
        permissionGranted = granted,
        permissionDenied = denied,
        onRequestPermission = onRequestPermission,
        onPickImage = onPickImage,
        onCancel = onClose,
        onCopy = onCopy,
        onQrDetected = ::onDetected,
        onCameraUnavailable = {
            if (state !is ScanQrState.Working) {
                state = ScanQrState.Idle(errorKind = ScanQrError.CameraUnavailable)
            }
        },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 失败提示文案（扫码页共用）。 */
@Composable
internal fun scanQrErrorTitle(kind: ScanQrError): String = when (kind) {
    ScanQrError.CameraUnavailable -> stringResource(R.string.scan_error_camera)
    ScanQrError.NoQrCode -> stringResource(R.string.scan_error_no_qr)
    ScanQrError.OpenFailed -> stringResource(R.string.scan_error_open_failed)
}

/**
 * 从 Uri 读取图片并解码二维码。
 * 先读尺寸再按 2048 长边降采样，避免高像素照片解码时内存翻倍/被系统杀（二维码识别率不受影响）。
 */
private fun decodeQrFromUri(context: Context, uri: Uri): String? {
    val bitmap = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, 2048)
        }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
    return try {
        bitmap?.let { decodeQrBitmap(it) }
    } finally {
        if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
    }
}

/** 计算 BitmapFactory 降采样比例：长边不超过 [maxDimension]。 */
private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    if (width <= 0 || height <= 0) return 1
    var sampleSize = 1
    while (maxOf(width, height) / sampleSize > maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

/** ZXing 解码：QR_CODE + TRY_HARDER；失败依次尝试旋转 90/180/270 度。 */
private fun decodeQrBitmap(source: Bitmap): String? {
    val hints: Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
    )
    val reader = MultiFormatReader()
    reader.setHints(hints)
    val rotatedBitmaps = ArrayList<Bitmap>(3)
    try {
        var current = source
        repeat(4) {
            val text = runCatching {
                reader.decodeWithState(toBinaryBitmap(current)).text
            }.getOrNull()
            if (!text.isNullOrBlank()) return text.trim()
            reader.reset()
            if (rotatedBitmaps.size < 3) {
                val next = Bitmap.createBitmap(
                    current,
                    0,
                    0,
                    current.width,
                    current.height,
                    Matrix().apply { postRotate(90f) },
                    true,
                )
                // 中间旋转图用完即回收，避免整张图叠加占内存。
                if (current !== source && !current.isRecycled) current.recycle()
                rotatedBitmaps.add(next)
                current = next
            }
        }
        return null
    } finally {
        rotatedBitmaps.forEach { if (!it.isRecycled) it.recycle() }
    }
}

private fun toBinaryBitmap(bitmap: Bitmap): BinaryBitmap {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    return BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
}

/**
 * 二维码内容分发（按顺序）：
 * 1. http(s) 链接 → [ExternalLink.open]（与聊天气泡一致）；
 * 2. 其它内容 → [AppDataSources.qrActionSource] 交给 TIM 原生分发；
 * 3. 仍失败 → 内容带 scheme 时尝试系统 ACTION_VIEW。
 *
 * @return 是否已成功发起打开动作。
 */
private fun dispatchQrContent(context: Context, rawText: String): Boolean {
    val text = rawText.trim()
    if (text.isEmpty()) return false
    val lower = text.lowercase()
    if (lower.startsWith("http://") || lower.startsWith("https://")) {
        ExternalLink.open(context, text)
        return true
    }
    val source = AppDataSources.qrActionSource
    if (source != null && runCatching { source.openQrContent(text) }.getOrDefault(false)) {
        return true
    }
    val uri = runCatching { Uri.parse(text) }.getOrNull() ?: return false
    if (uri.scheme.isNullOrBlank()) return false
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (intent.resolveActivity(context.packageManager) == null) return false
    return runCatching { context.startActivity(intent) }.isSuccess
}
