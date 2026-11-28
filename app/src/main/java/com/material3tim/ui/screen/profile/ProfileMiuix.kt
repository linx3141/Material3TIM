package com.material3tim.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.UserProfile
import com.material3tim.data.repository.ChatRepository
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import com.material3tim.ui.navigation3.Navigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 「我的」页 Miuix 实现：资料头卡 + HyperOS4 分段卡片入口（收藏/文件/钱包/服务、设置/主题/关于）。
 */
@Composable
fun ProfileMiuix(
    navigator: Navigator,
    bottomInnerPadding: Dp,
) {
    val profile = ChatRepository.profile
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val enableBlur = LocalEnableBlur.current
    // Phase 2e：页面级 Glass 采样 backdrop；关闭模糊/无 runtime shader 时为 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.profile),
                    isContentScrolled = listState.canScrollBackward,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                )
            },
            popupHost = { },
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
                    // 采样层要早于 inset/padding：记录不透明底 + 列表内容（不含玻璃顶栏自身）。
                    .then(
                        if (backdrop != null) {
                            Modifier
                                .layerBackdrop(backdrop)
                                .background(colorScheme.surface)
                        } else {
                            Modifier
                        },
                    ),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                item(key = "header") {
                    ProfileHeaderMiuix(
                        profile = profile,
                        onEditClick = { navigator.push(Route.EditProfile) },
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(top = 12.dp),
                    )
                }
                // HyperOS4 分组卡片：快捷入口合并为 squircle 分段卡片。
                groupedCardItems(
                    keyPrefix = "profileQuick",
                    outerTopPadding = 6.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("favorites") {
                            ProfileEntryMiuix(
                                icon = Icons.Filled.Star,
                                title = stringResource(R.string.profile_favorites),
                                onClick = { navigator.push(Route.Favorites) },
                            )
                        },
                        CardItem("files") {
                            ProfileEntryMiuix(
                                icon = Icons.Filled.Folder,
                                title = stringResource(R.string.profile_files),
                                onClick = { navigator.push(Route.Files) },
                            )
                        },
                        CardItem("wallet") {
                            ProfileEntryMiuix(
                                icon = Icons.Filled.AccountBalanceWallet,
                                title = stringResource(R.string.profile_wallet),
                                onClick = { navigator.push(Route.Wallet) },
                            )
                        },
                        CardItem("services") {
                            ProfileEntryMiuix(
                                icon = Icons.Filled.Apps,
                                title = stringResource(R.string.profile_services),
                                onClick = { navigator.push(Route.Services) },
                            )
                        },
                    ),
                )
                groupedCardItems(
                    keyPrefix = "profileSettings",
                    outerTopPadding = 0.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("settings") {
                            ProfileEntryMiuix(
                                icon = Icons.Filled.Settings,
                                title = stringResource(R.string.profile_settings),
                                summary = stringResource(R.string.profile_settings_summary),
                                onClick = { navigator.push(Route.Settings) },
                            )
                        },
                        CardItem("support_development") {
                            ProfileEntryMiuix(
                                icon = Icons.Filled.VolunteerActivism,
                                title = stringResource(R.string.profile_support_development),
                                summary = stringResource(R.string.profile_support_development_summary),
                                onClick = { navigator.push(Route.SupportDevelopment) },
                            )
                        },
                        CardItem("about") {
                            ProfileEntryMiuix(
                                icon = Icons.Filled.Info,
                                title = stringResource(R.string.profile_about),
                                summary = stringResource(R.string.profile_about_summary),
                                onClick = { navigator.push(Route.About) },
                            )
                        },
                    ),
                )
                item(key = "bottom_padding") {
                    Spacer(
                        CardItem("native_ui") {
                            // 隐藏 Compose 覆盖层，切到 TIM 原生界面。
                            ProfileEntryMiuix(
                                icon = Icons.Filled.ExitToApp,
                                title = stringResource(R.string.profile_native_ui),
                                summary = stringResource(R.string.profile_native_ui_summary),
                                onClick = { enterNativeTimUi(context) },
                            )
                        },
                        Modifier.height(
                            bottomInnerPadding +
                                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                    8.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderMiuix(
    profile: UserProfile,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                name = profile.name,
                avatarSeed = profile.avatarSeed,
                isOnline = profile.isOnline,
                size = 64.dp,
                avatarPath = profile.avatarPath,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    fontSize = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(R.string.profile_account_id, profile.timId),
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = profile.signature.ifBlank { stringResource(R.string.profile_signature) },
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            TextButton(
                text = stringResource(R.string.profile_edit),
                onClick = onEditClick,
            )
        }
    }
}

@Composable
private fun ProfileEntryMiuix(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = title,
        summary = summary,
        startAction = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
                tint = colorScheme.onBackground,
            )
        },
        onClick = onClick,
    )
}
