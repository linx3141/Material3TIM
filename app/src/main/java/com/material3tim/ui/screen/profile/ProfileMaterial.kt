package com.material3tim.ui.screen.profile

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.UserProfile
import com.material3tim.data.repository.ChatRepository
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.expressiveTopAppBarColors
import com.material3tim.ui.navigation3.Navigator
import com.material3tim.ui.navigation3.Route

@Composable
fun ProfileMaterial(
    navigator: Navigator,
    bottomInnerPadding: Dp,
) {
    val profile = ChatRepository.profile
    val context = LocalContext.current

    ExpressiveScaffold(
        topBar = {
            // 首页顶栏固定收起态：始终使用小标题 TopAppBar，不随滑动展开大标题。
            TopAppBar(
                // M3E 官方首页标题左边距 24dp：TopAppBar 默认约 16dp，这里补 8dp。
                title = {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        Text(stringResource(R.string.profile))
                    }
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            // 与消息/联系人页一致：内容紧跟顶栏（此前多 4dp 间距，导致「我的」首卡离标题稍远）。
            ProfileHeaderMaterial(
                profile = profile,
                onEditClick = { navigator.push(Route.EditProfile) },
            )
            SegmentedColumn(
                content = listOf(
                    {
                        ProfileEntryMaterial(
                            icon = Icons.Filled.Star,
                            title = stringResource(R.string.profile_favorites),
                            onClick = { navigator.push(Route.Favorites) },
                        )
                    },
                    {
                        ProfileEntryMaterial(
                            icon = Icons.Filled.Folder,
                            title = stringResource(R.string.profile_files),
                            onClick = { navigator.push(Route.Files) },
                        )
                    },
                    {
                        ProfileEntryMaterial(
                            icon = Icons.Filled.AccountBalanceWallet,
                            title = stringResource(R.string.profile_wallet),
                            onClick = { navigator.push(Route.Wallet) },
                        )
                    },
                    {
                        ProfileEntryMaterial(
                            icon = Icons.Filled.Apps,
                            title = stringResource(R.string.profile_services),
                            onClick = { navigator.push(Route.Services) },
                        )
                    },
                )
            )
            SegmentedColumn(
                content = listOf(
                    {
                        ProfileEntryMaterial(
                            icon = Icons.Filled.Settings,
                            title = stringResource(R.string.profile_settings),
                            supporting = stringResource(R.string.profile_settings_summary),
                            onClick = { navigator.push(Route.Settings) },
                        )
                    },
                    {
                        ProfileEntryMaterial(
                            icon = Icons.Filled.VolunteerActivism,
                            title = stringResource(R.string.profile_support_development),
                            supporting = stringResource(R.string.profile_support_development_summary),
                            onClick = { navigator.push(Route.SupportDevelopment) },
                        )
                    },
                    {
                        ProfileEntryMaterial(
                            icon = Icons.Filled.Info,
                            title = stringResource(R.string.profile_about),
                            supporting = stringResource(R.string.profile_about_summary),
                            onClick = { navigator.push(Route.About) },
                        )
                    },
                    {
                        // 隐藏我们的覆盖层，露出 TIM 原生界面；原生界面底部有常驻「返回 Material3Tim」按钮。
                        ProfileEntryMaterial(
                            icon = Icons.Filled.ExitToApp,
                            title = stringResource(R.string.profile_native_ui),
                            supporting = stringResource(R.string.profile_native_ui_summary),
                            onClick = { enterNativeTimUi(context) },
                        )
                    },
                )
            )
            Spacer(
                Modifier.height(
                    bottomInnerPadding +
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                            8.dp
                )
            )
        }
    }
}

@Composable
private fun ProfileHeaderMaterial(
    profile: UserProfile,
    onEditClick: () -> Unit,
) {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
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
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.profile_account_id, profile.timId),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.signature.ifBlank { stringResource(R.string.profile_signature) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onEditClick) {
                    Text(stringResource(R.string.profile_edit))
                }
            }
        }
    }
}

@Composable
private fun ProfileEntryMaterial(
    icon: ImageVector,
    title: String,
    supporting: String? = null,
    onClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        headlineContent = { Text(title) },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = { Icon(imageVector = icon, contentDescription = null) },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        },
    )
}

/** 「进入原生 TIM 页面」入口：宿主模式隐藏 Compose 覆盖层，独立 App 提示不可用。 */
internal fun enterNativeTimUi(context: android.content.Context) {
    val ok = AppDataSources.nativeUiSource?.switchToNativeUi() == true
    if (!ok) {
        Toast.makeText(context, context.getString(R.string.native_ui_unavailable), Toast.LENGTH_SHORT).show()
    }
}
