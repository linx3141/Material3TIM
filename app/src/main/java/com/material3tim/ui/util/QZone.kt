package com.material3tim.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.material3tim.R

/**
 * 打开 QQ 空间。
 *
 * 宿主(TIM)里优先用 TIM 自己的 qzone scheme（`mqqapi://qzone/...`），
 * 这样会走 TIM/QZone 原生页；独立 App 或 scheme 打不开时回退到
 * `https://user.qzone.qq.com/<uin>` 浏览器页面。
 *
 * @param uin QQ 号（他人资料页传目标 QQ 号；自己的「我的」页传当前账号）。
 */
fun openQZone(context: Context, uin: String): Boolean {
    val qq = uin.trim().takeIf { value ->
        value.all { it in '0'..'9' } && (value.toLongOrNull() ?: 0L) > 0L
    }.orEmpty()
    val candidates = buildList {
        if (qq.isNotEmpty()) {
            // 只回退到同一用户的个人主页，不用好友动态流替代。
            add("mqqapi://qzone/open_homepage?uin=$qq&src_type=internal&version=1")
            add("mqqapi://qzone/open_homepage?uin=$qq")
        } else {
            add("mqqapi://qzone/open_homepage")
        }
    }
    val isHost = context.packageName == "com.tencent.tim"
    if (isHost) {
        for (url in candidates) {
            val ok = runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        setPackage("com.tencent.tim")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }.isSuccess
            if (ok) {
                Log.i("Material3Tim", "qzone: opened $url")
                return true
            }
        }
    }
    if (qq.isNotEmpty()) {
        val url = "https://user.qzone.qq.com/$qq"
        val ok = runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }.isSuccess
        if (ok) return true
    }
    Toast.makeText(context, R.string.qzone_open_failed, Toast.LENGTH_SHORT).show()
    return false
}
