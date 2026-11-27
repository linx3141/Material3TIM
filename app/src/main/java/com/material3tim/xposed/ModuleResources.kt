package com.material3tim.xposed

import android.app.Activity
import android.content.res.Resources
import android.os.Build
import android.util.Log
import com.material3tim.BuildConfig
import com.material3tim.R

/**
 * 宿主（TIM 进程）里的「模块资源」。
 *
 * 模块 UI 的 `R.string.*` 等 id 必须解析到**模块 APK 自己的资源表**，否则会落到
 * TIM 的资源表上显示成完全无关的内容。
 *
 * 优先自建一套 Resources（独立、不会污染 TIM 的资源）：
 * `AssetManager.addAssetPath(module.apk)` 是隐藏 API，用反射调用；拿不到就退化为
 * 「把模块资源注入宿主 Activity 的 Resources」（TimMonet 在 TIM 4.1.0 上验证过的路径）。
 */
internal object ModuleResources {

    private const val TAG = "Material3Tim"

    /** 必须与 res/values/strings.xml 里的 module_res_marker 一致。 */
    private const val MARKER = "Material3Tim"

    private fun markerOk(res: Resources): Boolean = try {
        res.getString(R.string.module_res_marker) == MARKER
    } catch (t: Throwable) {
        false
    }

    /** 模块 APK 路径：LSPosed 注入的路径可能因刚升级而失效，逐个候选校验文件存在。 */
    private fun moduleApkPath(module: MainModule, activity: Activity?): String? {
        fun valid(path: String?): String? =
            path?.takeIf { it.isNotEmpty() && java.io.File(it).isFile }

        valid(runCatching { module.moduleApplicationInfo?.sourceDir }.getOrNull())?.let { return it }
        valid(
            runCatching {
                ModuleResources::class.java.protectionDomain?.codeSource?.location?.path
            }.getOrNull(),
        )?.let { return it }
        // classloader 里的 AndroidManifest.xml 来自模块 APK，路径一定是当前进程实际加载的那份。
        valid(
            runCatching {
                val url = ModuleResources::class.java.classLoader
                    ?.getResource("AndroidManifest.xml")?.toString().orEmpty()
                Regex("jar:file:(.*)!/").find(url)?.groupValues?.get(1)
            }.getOrNull(),
        )?.let { return it }
        // NPatch 集成模式：模块 APK 被解包到
        // cache/code_cache/mods/io.github.linx3141.material3tim/<crc>.apk。
        // 这是 embedded 模式下最直接的路径（不需要模块被 PackageManager 安装）。
        if (activity != null) {
            for (dir in listOf(
                java.io.File(activity.cacheDir, "code_cache/mods/io.github.linx3141.material3tim"),
                java.io.File(activity.filesDir, "code_cache/mods/io.github.linx3141.material3tim"),
            )) {
                valid(
                    runCatching {
                        dir.listFiles()
                            ?.firstOrNull { it.isFile && it.extension.equals("apk", true) }
                            ?.absolutePath
                    }.getOrNull(),
                )?.let { return it }
            }
        }
        // 通过 PackageManager 直接查当前已安装模块的 base.apk（刚升级时最可靠）。
        if (activity != null) {
            valid(
                runCatching {
                    activity.packageManager
                        .getApplicationInfo("io.github.linx3141.material3tim", 0)
                        .sourceDir
                }.getOrNull(),
            )?.let { return it }
        }
        valid(
            runCatching {
                java.io.File("/proc/self/maps").useLines { lines ->
                    lines.mapNotNull { line ->
                        val path = line.substringAfterLast(' ').trim()
                        if (path.endsWith(".apk") && path.contains("material3tim", ignoreCase = true)) {
                            path
                        } else {
                            null
                        }
                    }.firstOrNull()
                }
            }.getOrNull(),
        )?.let { return it }
        return null
    }

    /**
     * 为宿主 Activity 准备模块 Resources。
     *
     * @return 模块自己的 Resources；失败返回 null（调用方回退到宿主 Resources）。
     */
    fun createModuleResources(module: MainModule, activity: Activity): Resources? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.w(TAG, "module resources need API 26+")
            return null
        }
        // 刚升级模块时 LSPosed 注入的 APK 路径可能短暂失效，重试几次直到拿到有效资源。
        repeat(4) { attempt ->
            var apkPath = runCatching { moduleApkPath(module, activity) }.getOrNull()
            if (apkPath == null) {
                // 覆盖安装后旧路径被删除：优先用同版本缓存副本（资源 id 一致）。
                apkPath = cachedApkForCurrentVersion(activity)
                if (apkPath != null) Log.i(TAG, "use cached module apk: $apkPath (attempt $attempt)")
            }
            if (apkPath.isNullOrEmpty()) {
                Log.w(TAG, "module apk path not found (attempt $attempt)")
            } else {
                val res = try {
                    val assetManager = android.content.res.AssetManager::class.java
                        .getDeclaredConstructor()
                        .newInstance()
                    val addAssetPath = android.content.res.AssetManager::class.java
                        .getDeclaredMethod("addAssetPath", String::class.java)
                    // 先挂 framework-res：模块 UI 的 `android.R.style.Theme_DeviceDefault`
                    // 以及「跟随系统」要读的 `android.R.color.system_*` 都在这里。
                    // 不挂的话 ModuleContext 的资源表只有模块 APK，平台主题属性/系统色
                    // 会解析失败（表现为放大镜尺寸为 0、系统取色退化成默认紫）。
                    runCatching {
                        addAssetPath.invoke(assetManager, "/system/framework/framework-res.apk")
                    }
                    val added = addAssetPath.invoke(assetManager, apkPath) as? Int ?: 0
                    if (added == 0) {
                        Log.w(TAG, "addAssetPath failed: $apkPath (attempt $attempt)")
                        null
                    } else {
                        val hostRes = activity.resources
                        @Suppress("DEPRECATION")
                        val candidate = Resources(assetManager, hostRes.displayMetrics, hostRes.configuration)
                        if (!markerOk(candidate)) {
                            Log.w(TAG, "module resources marker mismatch, path=$apkPath (attempt $attempt)")
                            null
                        } else {
                            candidate
                        }
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "create module resources failed: ${t.javaClass.simpleName} ${t.message} (attempt $attempt)")
                    null
                }
                if (res != null) {
                    Log.i(TAG, "module resources ready from $apkPath")
                    cacheModuleApk(activity, apkPath)
                    return res
                }
            }
            if (attempt < 3) {
                android.os.SystemClock.sleep(150L)
            }
        }
        return null
    }

    /** 把当前模块 APK 复制一份到宿主私有目录，避免覆盖安装后原路径失效导致资源找不到。 */
    private fun cacheModuleApk(context: android.content.Context, apkPath: String) {
        runCatching {
            val dir = java.io.File(context.filesDir, "m3tim_module_apk").apply { mkdirs() }
            val target = java.io.File(dir, "module-${BuildConfig.VERSION_NAME}.apk")
            if (target.isFile && target.length() > 0L) return
            java.io.File(apkPath).copyTo(target, overwrite = true)
            Log.i(TAG, "cached module apk: ${target.absolutePath} (${target.length()}B)")
        }.onFailure { Log.w(TAG, "cache module apk failed: ${it.message}") }
    }

    /** 从缓存里找与当前代码版本一致的模块 APK（同版本覆盖安装时资源 id 不变，可直接用）。 */
    private fun cachedApkForCurrentVersion(context: android.content.Context): String? {
        val dir = java.io.File(context.filesDir, "m3tim_module_apk")
        if (!dir.isDirectory) return null
        return runCatching {
            dir.listFiles()
                ?.filter { it.isFile && it.length() > 0L }
                ?.firstOrNull { file ->
                    val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                    info?.versionName == BuildConfig.VERSION_NAME
                }
                ?.absolutePath
        }.getOrNull()
    }

    /**
     * 模块 APK 被覆盖安装替换、当前进程注入的是已删除的旧路径时，资源永远拿不到。
     * 此时自动重启 TIM 进程（新的 fork 会拿到新 APK 路径），避免界面用 TIM 资源表显示成 false。
     *
     * @return true 表示已安排重启（调用方不要继续挂载 UI）。
     */
    fun tryRecoverByRestart(activity: Activity): Boolean {
        val prefs = activity.getSharedPreferences("material3tim_host", android.content.Context.MODE_PRIVATE)
        val now = android.os.SystemClock.elapsedRealtime()
        val last = prefs.getLong(KEY_RES_RESTART_AT, 0L)
        if (last > 0L && now - last < 90_000L) {
            Log.e(TAG, "module resources unavailable; restart suppressed to avoid loop")
            return false
        }
        prefs.edit().putLong(KEY_RES_RESTART_AT, now).apply()
        Log.w(TAG, "module resources unavailable; restarting TIM to reload the module apk")
        runCatching {
            android.widget.Toast.makeText(
                activity,
                "模块已更新，正在重启 TIM…",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            runCatching { activity.finishAffinity() }
            android.os.Process.killProcess(android.os.Process.myPid())
        }, 600L)
        return true
    }

    private const val KEY_RES_RESTART_AT = "module_res_restart_at"

}
