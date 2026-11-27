package com.material3tim.ui.screen.chat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.material3tim.R
import java.util.Locale

/**
 * 按住说话的语音输入（语音转文字）。
 *
 * 使用系统 [SpeechRecognizer]：按下开始听，松开停止并取最终识别结果；部分识别结果会
 * 实时写进输入框。识别失败/设备没有语音识别服务时只 Toast 提示，不影响继续输入。
 */
internal class ChatVoiceInputController(
    private val context: Context,
    private val onText: (String) -> Unit,
    private val onMessage: (Int) -> Unit,
) {
    var isListening by mutableStateOf(false)
        private set

    private var recognizer: SpeechRecognizer? = null
    private var baseText = ""
    private var lastPartial = ""
    private var active = false

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            if (!active) return
            active = false
            isListening = false
            onMessage(errorMessage(error))
        }

        override fun onResults(results: Bundle?) {
            if (!active) return
            val text = results.firstResult().ifBlank { lastPartial }
            if (text.isNotBlank()) onText(joinDictation(baseText, text))
            active = false
            isListening = false
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!active) return
            val text = partialResults.firstResult()
            if (text.isNotBlank()) {
                lastPartial = text
                onText(joinDictation(baseText, text))
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    /** 按下：开始听。返回 false 表示权限/服务不可用，没有真正开始。 */
    fun start(base: String): Boolean {
        if (active) return true
        if (!hasRecordPermission()) {
            requestRecordPermission()
            onMessage(R.string.chat_voice_input_permission)
            return false
        }
        val engine = ensureRecognizer() ?: run {
            onMessage(R.string.chat_voice_input_unavailable)
            return false
        }
        baseText = base
        lastPartial = ""
        active = true
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        }
        val ok = runCatching { engine.startListening(intent) }.isSuccess
        if (!ok) {
            active = false
            isListening = false
            onMessage(R.string.chat_voice_input_unavailable)
        }
        return ok
    }

    /** 松开：停止录音，等待最终结果（onResults / onError 里收尾）。 */
    fun stop() {
        if (!active) return
        runCatching { recognizer?.stopListening() }
    }

    /** 手势取消：立即丢弃本次识别。 */
    fun cancel() {
        if (!active) return
        active = false
        isListening = false
        runCatching { recognizer?.cancel() }
    }

    fun release() {
        active = false
        isListening = false
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun ensureRecognizer(): SpeechRecognizer? {
        recognizer?.let { return it }
        // Android 11+ 的包可见性会过滤 queryIntentServices；宿主 TIM 的清单里
        // 没有 <queries>，直接 createSpeechRecognizer(context) 可能找不到服务。
        // 系统安全设置里的 voice_recognition_service 给出显式组件，用它绑定可绕开枚举。
        val explicit = runCatching {
            Settings.Secure.getString(context.contentResolver, "voice_recognition_service")
        }.getOrNull()?.let { ComponentName.unflattenFromString(it) }

        fun create(component: ComponentName?): SpeechRecognizer? = runCatching {
            val engine = if (component != null) {
                SpeechRecognizer.createSpeechRecognizer(context, component)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
            engine.also {
                it.setRecognitionListener(listener)
                recognizer = it
            }
        }.getOrNull()

        // 显式组件优先；不可用时再退回系统默认识别服务。
        return create(explicit) ?: create(null)
    }

    private fun hasRecordPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestRecordPermission() {
        val activity = context.findActivity() ?: return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_REQUEST,
        )
    }

    private fun errorMessage(error: Int): Int = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        -> R.string.chat_voice_input_no_match

        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.chat_voice_input_permission
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        -> R.string.chat_voice_input_network

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> R.string.chat_voice_input_busy
        SpeechRecognizer.ERROR_CLIENT -> R.string.chat_voice_input_unavailable
        else -> R.string.chat_voice_input_failed
    }
}

@Composable
internal fun rememberChatVoiceInput(
    onText: (String) -> Unit,
    onMessage: (Int) -> Unit,
): ChatVoiceInputController {
    val context = LocalContext.current
    val latestOnText by rememberUpdatedState(onText)
    val latestOnMessage by rememberUpdatedState(onMessage)
    val controller = remember(context) {
        ChatVoiceInputController(
            context = context,
            onText = { latestOnText(it) },
            onMessage = { latestOnMessage(it) },
        )
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}

/**
 * 右侧固定的语音输入按钮：按住开始识别，松开结束；识别中显示高亮圈。
 */
@Composable
internal fun VoiceInputButton(
    controller: ChatVoiceInputController,
    currentText: () -> String,
    icon: ImageVector,
    tint: Color,
    activeTint: Color,
    modifier: Modifier = Modifier,
) {
    val latestText by rememberUpdatedState(currentText)
    val listening = controller.isListening
    val description = stringResource(
        if (listening) R.string.chat_voice_listening else R.string.chat_voice_input,
    )
    Box(
        modifier = modifier
            .size(44.dp)
            .background(
                color = if (listening) activeTint.copy(alpha = 0.12f) else Color.Transparent,
                shape = CircleShape,
            )
            .then(
                if (listening) {
                    Modifier.border(1.5.dp, activeTint.copy(alpha = 0.7f), CircleShape)
                } else {
                    Modifier
                },
            )
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .pointerInput(controller) {
                detectTapGestures(
                    onPress = {
                        controller.start(latestText())
                        if (tryAwaitRelease()) controller.stop() else controller.cancel()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (listening) activeTint else tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

private const val RECORD_AUDIO_REQUEST = 0x4D36

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Bundle?.firstResult(): String =
    this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull { it.isNotBlank() }
        ?.trim()
        .orEmpty()

/** 把听写结果拼到按按钮时已有的文字后面；中英文之间只有两侧都是 ASCII 字母数字才补空格。 */
private fun joinDictation(base: String, dictated: String): String {
    if (base.isBlank()) return dictated
    if (dictated.isBlank()) return base
    val left = base.last()
    val right = dictated.first()
    val needSpace = left.code < 128 && right.code < 128 &&
        left.isLetterOrDigit() && right.isLetterOrDigit()
    return base + (if (needSpace) " " else "") + dictated
}
