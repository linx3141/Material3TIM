package com.material3tim.ui.screen.profile

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.theme.LocalEnableBlur
import java.io.File
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 支持开发页：支付宝 / 微信赞赏码入口。 */
@Composable
fun SupportDevelopmentScreen() {
    when (LocalUiMode.current) {
        UiMode.Material -> SupportDevelopmentMaterial()
        UiMode.Miuix -> SupportDevelopmentMiuix()
    }
}

/** 把 drawable 资源复制到 cache 文件，交给聊天页同款图片查看器打开。 */
private fun supportImagePath(
    context: Context,
    @DrawableRes resId: Int,
    fileName: String,
): String {
    val dir = File(context.cacheDir, "support_development").apply { mkdirs() }
    val file = File(dir, fileName)
    if (!file.exists() || file.length() == 0L) {
        context.resources.openRawResource(resId).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    }
    return file.absolutePath
}

@Composable
private fun SupportDevelopmentMaterial() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val alipayPath = supportImagePath(context, R.drawable.support_alipay, "alipay.jpg")
    val wechatPath = supportImagePath(context, R.drawable.support_wechat, "wechat.png")
    // 与设置页一致：LargeFlexible 可展开大标题，随内容滚动收起。
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.profile_support_development)) },
                navigationIcon = {
                    TopBarBackButton(
                        onClick = { navigator.pop() },
                        contentDescription = null,
                    )
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            SegmentedColumn(
                content = listOf(
                    {
                        SegmentedListItem(
                            onClick = { navigator.push(Route.ChatImage(imagePath = alipayPath)) },
                            headlineContent = { Text(stringResource(R.string.support_alipay)) },
                            supportingContent = { Text(stringResource(R.string.support_alipay_summary)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalanceWallet,
                                    contentDescription = null,
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    },
                    {
                        SegmentedListItem(
                            onClick = { navigator.push(Route.ChatImage(imagePath = wechatPath)) },
                            headlineContent = { Text(stringResource(R.string.support_wechat)) },
                            supportingContent = { Text(stringResource(R.string.support_wechat_summary)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Filled.Chat,
                                    contentDescription = null,
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    },
                ),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SupportDevelopmentMiuix() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val alipayPath = supportImagePath(context, R.drawable.support_alipay, "alipay.jpg")
    val wechatPath = supportImagePath(context, R.drawable.support_wechat, "wechat.png")
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.profile_support_development),
                    isContentScrolled = listState.canScrollBackward,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        HyperTopBarGlassButton(onClick = { navigator.pop() }) {
                            MiuixIcon(
                                imageVector = MiuixIcons.Back,
                                contentDescription = null,
                                tint = colorScheme.onSurface,
                            )
                        }
                    },
                )
            },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .then(
                        if (backdrop != null) {
                            Modifier
                                .layerBackdrop(backdrop)
                                .background(colorScheme.surface)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                groupedCardItems(
                    keyPrefix = "supportDevelopment",
                    outerTopPadding = 12.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("alipay") {
                            ArrowPreference(
                                title = stringResource(R.string.support_alipay),
                                summary = stringResource(R.string.support_alipay_summary),
                                startAction = {
                                    MiuixIcon(
                                        imageVector = Icons.Filled.AccountBalanceWallet,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = { navigator.push(Route.ChatImage(imagePath = alipayPath)) },
                            )
                        },
                        CardItem("wechat") {
                            ArrowPreference(
                                title = stringResource(R.string.support_wechat),
                                summary = stringResource(R.string.support_wechat_summary),
                                startAction = {
                                    MiuixIcon(
                                        imageVector = Icons.Filled.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = colorScheme.onBackground,
                                    )
                                },
                                onClick = { navigator.push(Route.ChatImage(imagePath = wechatPath)) },
                            )
                        },
                    ),
                )
                item(key = "bottom_padding") {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
