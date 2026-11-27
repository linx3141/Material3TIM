package com.material3tim.telegram

import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** 仅独立模块 Activity 创建客户端。TIM 只接收贴纸，不接触 Telegram 登录会话。 */
internal object TelegramClient {
    val authorization = MutableStateFlow<TdApi.AuthorizationState?>(null)
    private var client: Client? = null

    @Synchronized fun start() {
        if (client != null) return
        authorization.value = null
        Client.execute(TdApi.SetLogVerbosityLevel(0))
        client = Client.create({ update ->
            if (update is TdApi.UpdateAuthorizationState) {
                synchronized(this) {
                    if (update.authorizationState is TdApi.AuthorizationStateClosed) client = null
                    authorization.value = update.authorizationState
                }
            }
        }, null, null)
    }

    suspend fun configure(dir: File, id: Int, hash: String) {
        request(TdApi.SetTdlibParameters().apply {
            databaseDirectory = File(dir, "session").path
            filesDirectory = File(dir, "media").path
            databaseEncryptionKey = byteArrayOf()
            useFileDatabase = true
            apiId = id
            apiHash = hash
            systemLanguageCode = "en"
            deviceModel = Build.MODEL
            systemVersion = Build.VERSION.RELEASE
            applicationVersion = "Material3TIM"
        })
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : TdApi.Object> request(function: TdApi.Function<T>): T = withTimeout(120_000L) {
        suspendCancellableCoroutine { continuation ->
            val active = synchronized(this@TelegramClient) { checkNotNull(client) }
            active.send(function, { result ->
                if (result is TdApi.Error) continuation.resumeWithException(TelegramError(result.code, result.message))
                else continuation.resume(result as T)
            }, { error -> continuation.resumeWithException(error) })
        }
    }
}

internal class TelegramError(val code: Int, val reason: String) : Exception()
