package com.material3tim.ui.screen.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.material3tim.R
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

/**
 * 应用内扫码页：CameraX 预览 + 扫码框 + ZXing 实时解码。
 *
 * 不再拉起系统相机。权限/相机不可用时显示说明和「选择图片 / 取消」，相册路径仍可用。
 */
@Composable
internal fun QrCameraScanner(
    state: ScanQrState,
    permissionGranted: Boolean,
    permissionDenied: Boolean,
    onRequestPermission: () -> Unit,
    onPickImage: () -> Unit,
    onCancel: () -> Unit,
    onCopy: (String) -> Unit,
    onQrDetected: (String) -> Unit,
    onCameraUnavailable: () -> Unit,
) {
    var torchOn by remember { mutableStateOf(false) }
    val working = state is ScanQrState.Working

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (permissionGranted) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onQrDetected = onQrDetected,
                onCameraError = onCameraUnavailable,
                torchOn = torchOn,
            )
            ScanFrameOverlay(modifier = Modifier.fillMaxSize())
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF111111)),
            )
        }

        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.scan_cancel),
                    tint = Color.White,
                )
            }
            Text(
                text = stringResource(R.string.scan_title),
                color = Color.White,
            )
        }

        if (!permissionGranted) {
            PermissionCard(
                denied = permissionDenied,
                onRequestPermission = onRequestPermission,
                onPickImage = onPickImage,
                onCancel = onCancel,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // 失败 / 已识别但打不开的内容
        val idle = state as? ScanQrState.Idle
        if (idle?.errorKind != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 112.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = scanQrErrorTitle(idle.errorKind),
                    color = Color(0xFFFFB4AB),
                    textAlign = TextAlign.Center,
                )
                if (idle.errorContent.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = idle.errorContent,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 3,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = { onCopy(idle.errorContent) }) {
                        Text(stringResource(R.string.scan_copy), color = Color.White)
                    }
                }
            }
        }

        if (working) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // 底部操作
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { torchOn = !torchOn },
                enabled = permissionGranted && !working,
            ) {
                Text(
                    text = stringResource(
                        if (torchOn) R.string.scan_torch_on else R.string.scan_torch,
                    ),
                    color = if (permissionGranted) Color.White else Color.White.copy(alpha = 0.4f),
                )
            }
            TextButton(onClick = onPickImage, enabled = !working) {
                Text(stringResource(R.string.scan_pick_image), color = Color.White)
            }
            TextButton(onClick = onCancel, enabled = !working) {
                Text(stringResource(R.string.scan_cancel), color = Color.White)
            }
        }
    }
}

@Composable
private fun PermissionCard(
    denied: Boolean,
    onRequestPermission: () -> Unit,
    onPickImage: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.82f)
            .background(Color(0xFF1E1E1E), androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                if (denied) R.string.scan_permission_denied else R.string.scan_permission_needed,
            ),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRequestPermission) {
            Text(stringResource(R.string.scan_grant_permission), color = Color(0xFF8AB4F8))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onPickImage) {
                Text(stringResource(R.string.scan_pick_image), color = Color.White)
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.scan_cancel), color = Color.White)
            }
        }
    }
}

/** 半透明遮罩 + 四角扫描框 + 扫描线。 */
@Composable
private fun ScanFrameOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scanLine")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scanProgress",
    )
    Canvas(modifier = modifier) {
        val side = min(size.width, size.height) * 0.70f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val scrim = Color.Black.copy(alpha = 0.52f)
        // 四块遮罩，中间留出取景框
        drawRect(scrim, Offset(0f, 0f), Size(size.width, top))
        drawRect(scrim, Offset(0f, top + side), Size(size.width, size.height - top - side))
        drawRect(scrim, Offset(0f, top), Size(left, side))
        drawRect(scrim, Offset(left + side, top), Size(size.width - left - side, side))

        val corner = side * 0.16f
        val stroke = 3.dp.toPx()
        val accent = Color(0xFF8AB4F8)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(accent, Offset(x1, y1), Offset(x2, y2), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)

        line(left, top, left + corner, top)
        line(left, top, left, top + corner)
        line(left + side, top, left + side - corner, top)
        line(left + side, top, left + side, top + corner)
        line(left, top + side, left + corner, top + side)
        line(left, top + side, left, top + side - corner)
        line(left + side, top + side, left + side - corner, top + side)
        line(left + side, top + side, left + side, top + side - corner)

        drawLine(
            color = Color.White.copy(alpha = 0.75f),
            start = Offset(left + 8.dp.toPx(), top + side * progress),
            end = Offset(left + side - 8.dp.toPx(), top + side * progress),
            strokeWidth = 1.5.dp.toPx(),
        )
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier,
    onQrDetected: (String) -> Unit,
    onCameraError: () -> Unit,
    torchOn: Boolean,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnQrDetected by rememberUpdatedState(onQrDetected)
    // PreviewView 构造本身也可能因宿主资源/类加载问题抛异常；拿不到就报“无法启动相机”，
    // 不要让扫码页直接闪退。
    val previewView: PreviewView? = remember {
        runCatching {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }.onFailure {
            android.util.Log.w("Material3Tim", "PreviewView create failed: ${it.javaClass.simpleName} ${it.message}")
        }.getOrNull()
    }
    if (previewView == null) {
        LaunchedEffect(Unit) { onCameraError() }
        return
    }
    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(lifecycleOwner) {
        // NPatch 嵌入模式下模块的 AndroidManifest 不会合并进 TIM：CameraX 默认
        // 从宿主清单的 MetadataHolderService 找 Camera2Config$DefaultProvider，
        // 在 TIM 里找不到会直接抛 IllegalStateException（扫码页闪退）。
        // 显式注入 Camera2Config 即可绕开清单发现；独立 App 里同样安全。
        runCatching { ProcessCameraProvider.configureInstance(Camera2Config.defaultConfig()) }
        val providerFuture = runCatching {
            ProcessCameraProvider.getInstance(context)
        }.getOrNull()
        if (providerFuture == null) {
            onCameraError()
            return@DisposableEffect onDispose { }
        }
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            try {
                val cameraProvider = providerFuture.get()
                provider = cameraProvider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(
                    analysisExecutor,
                    QrFrameAnalyzer { text -> mainExecutor.execute { latestOnQrDetected(text) } },
                )
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (t: Throwable) {
                android.util.Log.w("Material3Tim", "camera bind failed: ${t.javaClass.simpleName} ${t.message}")
                onCameraError()
            }
        }, mainExecutor)

        onDispose {
            runCatching { provider?.unbindAll() }
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(torchOn, camera) {
        runCatching { camera?.cameraControl?.enableTorch(torchOn) }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** CameraX 帧分析：Y 平面 → ZXing；限制 250ms 一帧，避免主线程/CPU 压力。 */
private class QrFrameAnalyzer(
    private val onText: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            ),
        )
    }
    private val busy = AtomicBoolean(false)
    private var lastAnalyzeAt = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val now = SystemClock.uptimeMillis()
            if (now - lastAnalyzeAt < 250L) return
            if (!busy.compareAndSet(false, true)) return
            lastAnalyzeAt = now
            val text = decode(image) ?: return
            onText(text)
        } catch (t: Throwable) {
            android.util.Log.w("Material3Tim", "qr decode failed: ${t.javaClass.simpleName} ${t.message}")
        } finally {
            busy.set(false)
            image.close()
        }
    }

    private fun decode(image: ImageProxy): String? {
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0) return null
        val plane = image.planes.firstOrNull() ?: return null
        val rowStride = plane.rowStride
        if (rowStride < width) return null
        val y = ByteArray(width * height)
        val buffer = plane.buffer.duplicate()
        for (row in 0 until height) {
            buffer.position(row * rowStride)
            buffer.get(y, row * width, width)
        }
        var source: LuminanceSource =
            PlanarYUVLuminanceSource(y, width, height, 0, 0, width, height, false)
        // 相机帧通常是横向的；依次旋转尝试，直到认出二维码。
        repeat(4) {
            val text = runCatching {
                reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
            }.getOrNull()
            if (!text.isNullOrBlank()) return text.trim()
            reader.reset()
            if (!source.isRotateSupported) return@repeat
            source = runCatching { source.rotateCounterClockwise() }.getOrNull() ?: return null
        }
        return null
    }
}

/** 权限辅助：CameraX 所需 CAMERA 是否已授予。 */
internal fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
