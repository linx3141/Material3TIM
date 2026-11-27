package com.material3tim.xposed

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.util.Log
import android.webkit.MimeTypeMap
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.model.ChatMessageType
import com.material3tim.data.source.PersonalLibrarySource
import java.io.File
import java.lang.ref.WeakReference

internal class TimPersonalLibrary(activity: Activity) : PersonalLibrarySource {
    private val host = WeakReference(activity)

    override fun openFiles() = open { activity ->
        activity.startActivity(Intent().setClassName(activity.packageName,
            "com.tencent.mobileqq.filemanager.activity.fileassistant.FileAssistantActivity"))
        true
    }

    override fun openFavorites() = open { activity ->
        val uin = TimBridge.currentAccountUin()
        if (uin.isBlank()) return@open false
        // TIM 4.1.0.4050 的代理用操作 2 加载 qqfav 插件中的 FavoritesListActivity。
        val proxy = activity.classLoader.loadClass("cooperation.qqfav.QfavPluginProxyActivity")
        proxy.getMethod("u3", Context::class.java, String::class.java, Intent::class.java, Int::class.javaPrimitiveType)
            .invoke(null, activity, uin, Intent().putExtra("nOperation", 2).putExtra("bShowProgress", true), -1) == true
    }

    override fun collect(message: ChatMessage) = open { activity ->
        val intent = Intent(Intent.ACTION_SEND).setClassName(activity.packageName,
            "cooperation.qqfav.widget.QfavJumpActivity")
        if (message.type == ChatMessageType.TEXT) {
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, message.content)
        } else {
            val path = when (message.type) {
                ChatMessageType.IMAGE -> message.imagePath
                ChatMessageType.VIDEO -> message.videoPath
                ChatMessageType.FILE, ChatMessageType.VOICE -> message.mediaPath
                else -> null
            } ?: return@open false
            val file = File(path).takeIf { it.isFile } ?: return@open false
            val uri = TimBridge.hostFileUri(activity, activity.classLoader, file) ?: return@open false
            intent.type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: when (message.type) {
                ChatMessageType.IMAGE -> "image/*"
                ChatMessageType.VIDEO -> "video/*"
                ChatMessageType.VOICE -> "audio/*"
                else -> "application/octet-stream"
            }
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.clipData = ClipData.newRawUri(file.name, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(intent)
        true
    }

    private fun open(action: (Activity) -> Boolean): Boolean {
        val activity = host.get()?.takeUnless { it.isFinishing || it.isDestroyed } ?: return false
        return runCatching { action(activity) }.onFailure {
            Log.w("Material3Tim", "personalLibrary: open failed", it)
        }.getOrDefault(false)
    }
}
