package com.material3tim.xposed

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.Log
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

private const val MaxNotificationReplyLength = 4000

internal fun normalizedNotificationReply(input: CharSequence?): String? =
    input?.toString()?.trim()?.takeIf { it.isNotEmpty() && it.length <= MaxNotificationReplyLength }

/** 用 TIM 应用进程里的动态接收器，模块 APK 的 manifest 组件不属于 TIM。 */
internal class TimNotificationReply(
    private val send: (Context, String, String, PendingIntent, String?) -> Unit,
) {
    private val action = "com.material3tim.NOTIFICATION_REPLY.${UUID.randomUUID()}"
    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val executor = ThreadPoolExecutor(0, 4, 30L, TimeUnit.SECONDS, SynchronousQueue()) { task ->
        Thread(task, "m3tim-notify-reply").apply { isDaemon = true }
    }
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val target = intent.data ?: return
            val segments = target.pathSegments
            if (intent.action != action || target.authority != context.packageName || segments.size != 2) return
            val (account, chatId) = segments
            val input = runCatching { RemoteInput.getResultsFromIntent(intent)?.getCharSequence(InputKey) }
                .getOrNull() ?: return
            if (!inFlight.add(chatId)) return
            val pending = goAsync()
            runCatching {
                executor.execute {
                    try {
                        val replyIntent = Intent(action, target).setPackage(context.packageName)
                        val token = PendingIntent.getBroadcast(
                            context, 0, replyIntent, mutationFlag() or PendingIntent.FLAG_NO_CREATE,
                        ) ?: return@execute
                        send(context, chatId, account, token, normalizedNotificationReply(input))
                    } catch (t: Throwable) {
                        Log.w(MainModule.TAG, "notify: inline reply failed", t)
                    } finally {
                        inFlight.remove(chatId)
                        pending.finish()
                    }
                }
            }.onFailure {
                inFlight.remove(chatId)
                pending.finish()
                Log.w(MainModule.TAG, "notify: inline reply worker unavailable", it)
            }
        }
    }

    @Synchronized
    @Suppress("DEPRECATION", "UnspecifiedRegisterReceiverFlag")
    private fun ensureRegistered(context: Context) {
        if (registered) return
        val filter = IntentFilter(action).apply { addDataScheme(Scheme) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            // 旧系统没有 NOT_EXPORTED。每次启动的高熵 action 不对外公开，只放在 TIM 的 PendingIntent 中。
            context.registerReceiver(receiver, filter)
        }
        registered = true
    }

    fun createAction(context: Context, chatId: String, account: String, label: String): Notification.Action? =
        runCatching {
            val host = context.applicationContext ?: return@runCatching null
            ensureRegistered(host)
            // 会话和账号放在已有的 data URI，不让 RemoteInput 的 fill-in 参数改写目标。
            // 同一会话收到新通知时复用 PendingIntent，避免系统因按钮更换而丢弃正在输入的内容。
            val target = Uri.Builder().scheme(Scheme).authority(host.packageName)
                .appendPath(account).appendPath(chatId).build()
            val intent = Intent(action, target).setPackage(host.packageName)
            val pending = PendingIntent.getBroadcast(host, 0, intent, mutationFlag())
            val input = RemoteInput.Builder(InputKey).setLabel(label).build()
            Notification.Action.Builder(Icon.createWithResource("android", android.R.drawable.ic_menu_send), label, pending)
                .addRemoteInput(input)
                .setAllowGeneratedReplies(false)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        setSemanticAction(Notification.Action.SEMANTIC_ACTION_REPLY)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setAuthenticationRequired(true)
                }
                .build()
        }.onFailure { Log.w(MainModule.TAG, "notify: inline reply action unavailable", it) }.getOrNull()

    // Android 12+ 明确声明可变；Android 11 及以前 PendingIntent 本身默认可变。
    private fun mutationFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    private companion object {
        const val InputKey = "com.material3tim.notification.REPLY_TEXT"
        const val Scheme = "m3tim-notification-reply"
    }
}
