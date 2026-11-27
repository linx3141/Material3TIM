package com.material3tim.telegram

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.material3tim.BuildConfig
import com.material3tim.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.io.File

class TelegramSyncActivity : ComponentActivity() {
    private val model by viewModels<TelegramSyncModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 登录信息不通过 Intent 传递，只有用户点击同步后才返回只读贴纸文件。
        if (callingPackage !in setOf(packageName, "com.tencent.tim")) {
            finish()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    LoginAndSync(model, onClose = ::finish)
                }
            }
            val file = model.result
            LaunchedEffect(file) {
                if (file == null) return@LaunchedEffect
                val uri = FileProvider.getUriForFile(this@TelegramSyncActivity, "$packageName.stickers", file)
                setResult(Activity.RESULT_OK, Intent().setData(uri).apply {
                    clipData = ClipData.newRawUri("stickers", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                })
                finish()
            }
        }
    }
}

class TelegramSyncModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val dir = File(app.noBackupFilesDir, "telegram")
    private val config = File(dir, "api")
    internal val authorization = TelegramClient.authorization
    internal var error by mutableStateOf("")
    internal var busy by mutableStateOf(false)
    internal var progress by mutableStateOf("")
    internal var result by mutableStateOf<File?>(null)

    internal fun act(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = ""
        viewModelScope.launch {
            try {
                block()
            } catch (_: TimeoutCancellationException) {
                error = app.getString(R.string.tg_timeout)
            } catch (e: CancellationException) {
                throw e
            } catch (e: TelegramError) {
                error = app.getString(R.string.tg_api_error, e.code, e.reason)
            } catch (_: Exception) {
                error = app.getString(R.string.tg_failed)
            } catch (_: LinkageError) {
                error = app.getString(R.string.tg_client_unavailable)
            } finally {
                busy = false
                progress = ""
            }
        }
    }

    internal fun start() = act { withContext(Dispatchers.IO) { TelegramClient.start() } }

    internal suspend fun configure(id: Int, hash: String) = withContext(Dispatchers.IO) {
        TelegramClient.configure(dir, id, hash)
        dir.mkdirs()
        config.writeText("$id\n$hash")
    }

    internal suspend fun previousConfig(): Pair<Int, String>? = withContext(Dispatchers.IO) {
        val lines = if (config.isFile) config.readLines() else emptyList()
        val id = lines.firstOrNull()?.toIntOrNull() ?: BuildConfig.TELEGRAM_API_ID
        val hash = lines.getOrNull(1) ?: BuildConfig.TELEGRAM_API_HASH
        if (id > 0 && hash.matches(Regex("[0-9a-fA-F]{32}"))) id to hash else null
    }

    internal fun sync() = act {
        result = withContext(Dispatchers.IO) {
            exportInstalledStickers(File(app.cacheDir, "sticker-exports")) { n, total, name ->
                viewModelScope.launch { progress = app.getString(R.string.tg_progress, n, total, name) }
            }
        }
    }
}

@Composable
private fun LoginAndSync(model: TelegramSyncModel, onClose: () -> Unit) {
    val auth by model.authorization.collectAsState()
    var input by remember(auth?.constructor) { mutableStateOf("") }
    var apiId by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { model.start() }
    LaunchedEffect(auth) {
        if (auth is TdApi.AuthorizationStateWaitTdlibParameters) {
            val previous = model.previousConfig()
            if (previous != null) {
                // 初始状态可能与 start() 的收尾同时到达，等待表单解锁。
                snapshotFlow { model.busy }.first { !it }
                model.act { model.configure(previous.first, previous.second) }
            }
        }
    }
    Column(
        Modifier.fillMaxSize().systemBarsPadding().imePadding().verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.tg_title), style = MaterialTheme.typography.headlineSmall)
        val state = auth
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                Text(stringResource(R.string.tg_api_setup))
                OutlinedTextField(apiId, { apiId = it }, label = { Text("API ID") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                InputField(input, { input = it }, "API hash", KeyboardType.Password)
                Button(enabled = !model.busy && (apiId.toIntOrNull() ?: 0) > 0 &&
                    input.matches(Regex("[0-9a-fA-F]{32}")), onClick = {
                    val hash = input
                    model.act { model.configure(apiId.toInt(), hash) }
                }) { Text(stringResource(R.string.tg_continue)) }
            }
            is TdApi.AuthorizationStateWaitPhoneNumber, is TdApi.AuthorizationStateWaitCode,
            is TdApi.AuthorizationStateWaitPassword, is TdApi.AuthorizationStateWaitEmailAddress,
            is TdApi.AuthorizationStateWaitEmailCode -> {
                val (label, type) = when (state) {
                    is TdApi.AuthorizationStateWaitPhoneNumber -> R.string.tg_phone to KeyboardType.Phone
                    is TdApi.AuthorizationStateWaitPassword -> R.string.tg_password to KeyboardType.Password
                    is TdApi.AuthorizationStateWaitEmailAddress -> R.string.tg_email to KeyboardType.Email
                    is TdApi.AuthorizationStateWaitEmailCode -> R.string.tg_email_code to KeyboardType.Number
                    else -> R.string.tg_code to KeyboardType.Text
                }
                InputField(input, { input = it }, stringResource(label), type)
                Button(enabled = !model.busy && input.isNotBlank(), onClick = {
                    val value = input
                    model.act {
                        val request = when (state) {
                            is TdApi.AuthorizationStateWaitPhoneNumber -> TdApi.SetAuthenticationPhoneNumber(value.trim(), null)
                            is TdApi.AuthorizationStateWaitPassword -> TdApi.CheckAuthenticationPassword(value)
                            is TdApi.AuthorizationStateWaitEmailAddress -> TdApi.SetAuthenticationEmailAddress(value.trim())
                            is TdApi.AuthorizationStateWaitEmailCode -> TdApi.CheckAuthenticationEmailCode(TdApi.EmailAddressAuthenticationCode(value.trim()))
                            else -> TdApi.CheckAuthenticationCode(value.trim())
                        }
                        TelegramClient.request(request)
                    }
                }) { Text(stringResource(R.string.tg_continue)) }
                if (state is TdApi.AuthorizationStateWaitPhoneNumber) {
                    OutlinedButton(enabled = !model.busy, onClick = {
                        model.act { TelegramClient.request(TdApi.RequestQrCodeAuthentication(longArrayOf())) }
                    }) { Text(stringResource(R.string.tg_use_qr)) }
                }
            }
            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> {
                Text(stringResource(R.string.tg_scan))
                val image = remember(state.link) {
                    val size = 320
                    val qr = QRCodeWriter().encode(state.link, BarcodeFormat.QR_CODE, size, size)
                    val pixels = IntArray(size * size) { i -> if (qr[i % size, i / size]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
                    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
                }
                Image(image, stringResource(R.string.tg_use_qr), Modifier.size(240.dp))
            }
            is TdApi.AuthorizationStateReady -> {
                Text(stringResource(R.string.tg_sync_explanation))
                Button(enabled = !model.busy, onClick = model::sync) { Text(stringResource(R.string.tg_sync_now)) }
                TextButton(enabled = !model.busy, onClick = { model.act { TelegramClient.request(TdApi.LogOut()) } }) {
                    Text(stringResource(R.string.tg_logout))
                }
            }
            is TdApi.AuthorizationStateClosed, null -> {
                if (state != null || model.error.isNotEmpty()) {
                    OutlinedButton(onClick = model::start, enabled = !model.busy) { Text(stringResource(R.string.tg_reconnect)) }
                } else Text(stringResource(R.string.loading))
            }
            is TdApi.AuthorizationStateLoggingOut, is TdApi.AuthorizationStateClosing -> Text(stringResource(R.string.loading))
            else -> Text(stringResource(R.string.tg_unsupported_auth))
        }
        if (model.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            if (model.progress.isNotEmpty()) Text(model.progress)
        }
        if (model.error.isNotEmpty()) Text(model.error, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onClose) { Text(stringResource(android.R.string.cancel)) }
    }
}

@Composable
private fun InputField(value: String, change: (String) -> Unit, label: String, type: KeyboardType) {
    OutlinedTextField(value, change, label = { Text(label) }, singleLine = true,
        modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = type),
        visualTransformation = if (type == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None)
}
