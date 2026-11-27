package com.material3tim.xposed

import android.content.Context
import android.util.Log
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Proxy

/**
 * 在 **TIM 原生设置页** 注入「Material3 TIM」入口。
 *
 * 用途：用户点「回到原版 TIM」后，Material3TIM 覆盖层不再挂载；此时需要
 * 一个入口把界面重新启用。入口放在 TIM 设置页（`MainSettingConfigProvider.d`
 * 返回的分组列表），点击后写 `material3tim_enabled=true` 并重启 TIM。
 *
 * 与 QAuxv 共存：TimMonet/QAuxv 都在这个列表里插自己的分组，过去大家都抢
 * index 1。这里统一**追加到末尾**，不动其它模块的相对位置。
 */
internal object NativeSettingsEntry {

    private const val TAG = "Material3Tim"
    private const val MODULE_TITLE = "Material3 TIM"

    /**
     * 入口 id。TIM 的 setting id 有一部分直接复用了资源 id（0x7F…），
     * 这里用资源 id 范围之外的固定值，避免 [com.tencent.mobileqq.setting.processor.SettingConfigProvider.b]
     * 误命中别的设置项，也避免和 QAuxv / TimMonet 的入口冲突。
     */
    private const val ENTRY_ID = 0x4D335401

    fun install(module: XposedModule, cl: ClassLoader) {
        val providerCls = runCatching {
            Class.forName("com.tencent.mobileqq.setting.main.MainSettingConfigProvider", false, cl)
        }.getOrNull() ?: run {
            Log.w(TAG, "native settings: MainSettingConfigProvider not found")
            return
        }
        val method = runCatching {
            providerCls.getDeclaredMethod("d", Context::class.java)
        }.getOrNull() ?: run {
            Log.w(TAG, "native settings: MainSettingConfigProvider.d(Context) not found")
            return
        }
        runCatching { module.deoptimize(method) }
        module.hook(method).intercept { chain ->
            val result = chain.proceed()
            val list = result as? MutableList<*>
            val ctx = chain.args.getOrNull(0) as? Context
            if (list != null && ctx != null) {
                runCatching { inject(list, ctx, cl) }
                    .onFailure {
                        Log.w(TAG, "native settings entry inject failed: ${it.javaClass.simpleName} ${it.message}")
                    }
            }
            result
        }
        Log.i(TAG, "hook installed: MainSettingConfigProvider.d (Material3 TIM entry)")
    }

    @Suppress("UNCHECKED_CAST")
    private fun inject(list: MutableList<*>, ctx: Context, cl: ClassLoader) {
        val groups = list as MutableList<Any?>

        // 已经插过（同一个列表被多次构建时）就跳过，避免重复入口。
        if (groups.any { group ->
                runCatching {
                    (group?.javaClass?.getMethod("b")?.invoke(group) as? List<*>)
                        ?.any { item ->
                            runCatching {
                                item?.javaClass?.getMethod("e")?.invoke(item) as? Int
                            }.getOrNull() == ENTRY_ID
                        } == true
                }.getOrDefault(false)
            }
        ) {
            return
        }

        val itemCls = runCatching {
            Class.forName("com.tencent.mobileqq.setting.processor.h", false, cl)
        }.getOrNull() ?: return
        val groupCls = runCatching {
            Class.forName("com.tencent.mobileqq.setting.processor.c", false, cl)
        }.getOrNull() ?: return

        val itemCtor = runCatching {
            itemCls.getConstructor(
                Context::class.java,
                Int::class.javaPrimitiveType,
                CharSequence::class.java,
                Int::class.javaPrimitiveType,
            )
        }.getOrNull() ?: return

        val setClick = itemCls.methods.firstOrNull { m ->
            m.returnType == Void.TYPE && m.parameterTypes.size == 1 &&
                m.parameterTypes[0].name == "kotlin.jvm.functions.Function0"
        } ?: return

        val res = ctx.resources
        val iconRes = res.getIdentifier("qui_fill_color", "drawable", ctx.packageName)
            .takeIf { it != 0 }
            ?: res.getIdentifier("qui_tuning", "drawable", ctx.packageName)

        val entry = itemCtor.newInstance(ctx, ENTRY_ID, MODULE_TITLE, iconRes)

        val function0 = runCatching { cl.loadClass("kotlin.jvm.functions.Function0") }.getOrNull()
            ?: return
        val unit = runCatching {
            cl.loadClass("kotlin.Unit").getField("INSTANCE").get(null)
        }.getOrNull()
        val click = Proxy.newProxyInstance(cl, arrayOf(function0)) { proxy, method, args ->
            when (method.name) {
                "invoke" -> {
                    // 只在当前是原生界面时重新启用；已经启用时不要平白重启 TIM
                    // （用户在设置页点这个入口通常只是想看看/确认）。
                    if (!UiHost.isMaterialUiEnabled(ctx)) {
                        UiHost.setMaterialUiEnabled(ctx, true)
                        UiHost.restartTim(ctx)
                    }
                    unit
                }
                "toString" -> "Material3TimSettingsEntry"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> null
            }
        }
        runCatching { setClick.invoke(entry, click) }

        val group = runCatching {
            groupCls.getConstructor(
                List::class.java,
                CharSequence::class.java,
                CharSequence::class.java,
            ).newInstance(listOf(entry), "", "")
        }.getOrElse {
            val marker = runCatching {
                Class.forName("kotlin.jvm.internal.DefaultConstructorMarker", false, cl)
            }.getOrNull() ?: return
            groupCls.getConstructor(
                List::class.java,
                CharSequence::class.java,
                CharSequence::class.java,
                Int::class.javaPrimitiveType,
                marker,
            ).newInstance(listOf(entry), "", "", 6, null)
        }

        // 追加到末尾：不挤 QAuxv / TimMonet 的固定 index。
        groups.add(groups.size, group)
        Log.i(TAG, "native settings entry injected at end, groups=${groups.size}")
    }
}
