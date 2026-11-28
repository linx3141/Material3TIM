package com.material3tim.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults.flingBehavior
import androidx.compose.foundation.pager.PagerDefaults.pageNestedScrollConnection
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.material3tim.BuildConfig
import com.material3tim.R
import com.material3tim.data.repository.ChatRepository
import com.material3tim.ui.component.bottombar.BottomBar
import com.material3tim.ui.component.bottombar.MainPagerState
import com.material3tim.ui.component.bottombar.NavigationBadgeState
import com.material3tim.ui.component.bottombar.SideRail
import com.material3tim.ui.component.bottombar.rememberMainPagerState
import com.material3tim.ui.component.bottombar.useNavigationRail
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Navigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.navigation3.rememberNavigator
import com.material3tim.ui.screen.about.AboutScreen
import com.material3tim.ui.screen.chat.ChatDetailScreen
import com.material3tim.ui.screen.forward.ForwardScreen
import com.material3tim.ui.screen.chat.ChatInfoScreen
import com.material3tim.ui.screen.chat.ChatMembersScreen
import com.material3tim.ui.screen.chat.AddFriendScreen
import com.material3tim.ui.screen.chat.ScanQrScreen
import com.material3tim.ui.screen.chat.AddFriendVerifyScreen
import com.material3tim.ui.screen.chat.ChatSearchScreen
import com.material3tim.ui.screen.search.GlobalSearchScreen
import com.material3tim.ui.screen.chat.CreateGroupScreen
import com.material3tim.ui.screen.chat.ChatNoticeDetailScreen
import com.material3tim.ui.screen.chat.GroupFilesScreen
import com.material3tim.ui.screen.chat.ChatNoticeScreen
import com.material3tim.ui.screen.chat.ChatPager
import com.material3tim.ui.screen.chat.ChatImageScreen
import com.material3tim.ui.screen.chat.GroupAssistantScreen
import com.material3tim.ui.screen.chat.ChatRecordScreen
import com.material3tim.ui.screen.chat.ChatVideoScreen
import com.material3tim.ui.screen.colorpalette.ColorPaletteScreen
import com.material3tim.ui.screen.contacts.ContactDetailScreen
import com.material3tim.ui.screen.contacts.ContactsPager
import com.material3tim.ui.screen.contacts.GroupChatsScreen
import com.material3tim.ui.screen.contacts.NewFriendsScreen
import com.material3tim.ui.screen.contacts.OfficialAccountsScreen
import com.material3tim.ui.screen.contacts.TagsScreen
import com.material3tim.ui.screen.profile.EditProfileScreen
import com.material3tim.ui.screen.profile.FavoritesScreen
import com.material3tim.ui.screen.profile.FilesScreen
import com.material3tim.ui.screen.profile.ProfilePager
import com.material3tim.ui.screen.profile.ServicesScreen
import com.material3tim.ui.screen.profile.WalletScreen
import com.material3tim.ui.screen.settings.SettingPager
import com.material3tim.ui.theme.LocalColorMode
import com.material3tim.ui.theme.LocalEnableBlur
import com.material3tim.ui.theme.LocalEnableFloatingBottomBar
import com.material3tim.ui.theme.LocalEnableFloatingBottomBarBlur
import com.material3tim.ui.theme.LocalEnableNavigationBadge
import com.material3tim.ui.theme.LocalEnableLauncherFloatingToolbar
import com.material3tim.ui.theme.LocalShowNavigationBar
import com.material3tim.ui.theme.LocalShowTopAppBar
import com.material3tim.ui.theme.LocalTopAppBarStyle
import com.material3tim.ui.theme.LocalFloatingNavigationBarPosition
import com.material3tim.ui.theme.LocalFloatingNavigationBarStyle
import com.material3tim.ui.theme.LocalNavigationBarMode
import com.material3tim.ui.theme.M3TimTheme
import com.material3tim.ui.util.rememberBlurBackdrop
import com.material3tim.ui.util.rememberContentReady
import com.material3tim.ui.viewmodel.MainActivityViewModel
import com.material3tim.ui.viewmodel.MainPagerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerGestureNestedScrollConnection
import top.yukonga.miuix.kmp.utils.PagerInterceptionMode
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec
import top.yukonga.miuix.kmp.utils.pagerGestureOverride

/**
 * Material3 TIM 的根 Compose 内容。
 *
 * 独立 App（[MainActivity]）与 Xposed 宿主（TIM 进程里的 ComposeView）共用这一份 UI：
 * 宿主侧通过 [com.material3tim.xposed.UiHost] 提供模块 Resources / Context / 只读远端设置。
 */
@androidx.compose.runtime.Composable
fun M3TimRoot(
    viewModel: MainActivityViewModel,
    qaRoute: String? = null,
    onDarkModeChanged: (Boolean) -> Unit = {},
    onContentReady: () -> Unit = {},
    monetContext: android.content.Context? = null,
    activityResultRegistryOwner: androidx.activity.result.ActivityResultRegistryOwner? = null,
) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val selectedMainPage by viewModel.selectedMainPage.collectAsStateWithLifecycle()
        val appSettings = uiState.appSettings
        val uiMode = uiState.uiMode
        val darkMode = appSettings.colorMode.isDark ||
            (appSettings.colorMode.isSystem && isSystemInDarkTheme())

        LaunchedEffect(darkMode) { onDarkModeChanged(darkMode) }

        val navigator = rememberNavigator(Route.Main)
        // 通知点击跳转：TimUiHook 从 Intent 里取出会话 id 后在这里消费并打开聊天页。
        val pendingChatId by
            com.material3tim.data.source.NavigationRequests.requests.collectAsStateWithLifecycle()
        LaunchedEffect(pendingChatId) {
            val id = pendingChatId ?: return@LaunchedEffect
            com.material3tim.data.source.NavigationRequests.consume(id)
            navigator.push(Route.ChatDetail(id))
        }
        // 前后台状态（通知/后台提醒用来判断是否需要在通知栏提醒）。
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_START ->
                        com.material3tim.data.source.AppUiState.isForeground = true
                    androidx.lifecycle.Lifecycle.Event.ON_STOP ->
                        com.material3tim.data.source.AppUiState.isForeground = false
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val systemDensity = LocalDensity.current
        val density = remember(systemDensity, uiState.pageScale) {
            Density(systemDensity.density * uiState.pageScale, systemDensity.fontScale)
        }

        CompositionLocalProvider(
            LocalUriHandler provides object : UriHandler {
                override fun openUri(uri: String) {
                    try {
                        uriHandler.openUri(uri)
                    } catch (_: IllegalArgumentException) {
                        Toast.makeText(context, R.string.external_link_open_failed, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            },
            LocalNavigator provides navigator,
            LocalDensity provides density,
            LocalColorMode provides appSettings.colorMode.value,
            LocalEnableBlur provides uiState.enableBlur,
            LocalEnableFloatingBottomBar provides uiState.enableFloatingBottomBar,
            LocalEnableFloatingBottomBarBlur provides uiState.enableFloatingBottomBarBlur,
            LocalEnableNavigationBadge provides uiState.enableNavigationBadge,
            LocalEnableLauncherFloatingToolbar provides uiState.enableLauncherFloatingToolbar,
            LocalShowTopAppBar provides uiState.showTopAppBar,
            LocalTopAppBarStyle provides uiState.topAppBarStyle,
            LocalShowNavigationBar provides uiState.showNavigationBar,
            LocalNavigationBarMode provides uiState.navigationBarMode,
            LocalFloatingNavigationBarStyle provides uiState.floatingNavigationBarStyle,
            LocalFloatingNavigationBarPosition provides uiState.floatingNavigationBarPosition,
            LocalUiMode provides uiMode,
            com.material3tim.ui.theme.LocalMonetContext provides monetContext,
            androidx.activity.compose.LocalActivityResultRegistryOwner provides
                (activityResultRegistryOwner
                    ?: requireNotNull(androidx.activity.compose.LocalActivityResultRegistryOwner.current)),
        ) {
            M3TimTheme(appSettings = appSettings, uiMode = uiMode) {
                val swipeDismiss = if (uiState.enableSwipeDismiss) {
                    if (LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl) {
                        NavSwipeDirection.RightToLeft
                    } else {
                        NavSwipeDirection.LeftToRight
                    }
                } else {
                    NavSwipeDirection.None
                }
                val mainScreenEntry = @Composable {
                    MainScreen(
                        initialPage = selectedMainPage,
                        pagerInterceptionMode = uiState.pagerInterceptionMode,
                        onPageChanged = viewModel::setSelectedMainPage,
                    )
                }

                val navDisplay = @Composable {
                    NavDisplay(
                        backStack = navigator.backStack,
                        effects = NavDisplayEffects(cornerClipRadius = rememberNavSystemCornerRadius()),
                        onBack = { navigator.pop() },
                    ) {
                        entry<Route.Main>(swipeDismiss = swipeDismiss) { mainScreenEntry() }
                        entry<Route.Settings>(swipeDismiss = swipeDismiss) { SettingPager(navigator) }
                        entry<Route.ColorPalette>(swipeDismiss = swipeDismiss) { ColorPaletteScreen() }
                        entry<Route.About>(swipeDismiss = swipeDismiss) { AboutScreen() }
                        entry<Route.ChatDetail>(swipeDismiss = swipeDismiss) { key ->
                            ChatDetailScreen(key.chatId, key.jumpToSeq, key.jumpToMsgId)
                        }
                        entry<Route.GroupAssistant>(swipeDismiss = swipeDismiss) {
                            GroupAssistantScreen()
                        }
                        entry<Route.ChatSearch>(swipeDismiss = swipeDismiss) { key ->
                            ChatSearchScreen(key.chatId)
                        }
                        entry<Route.GlobalSearch>(swipeDismiss = swipeDismiss) {
                            GlobalSearchScreen()
                        }
                        entry<Route.ForwardGroupAssistant>(swipeDismiss = swipeDismiss) { key ->
                            GroupAssistantScreen(
                                forwardSrcChatId = key.srcChatId,
                                forwardMsgIds = key.msgIds,
                                forwardPreview = key.preview,
                            )
                        }
                        entry<Route.AddFriend>(swipeDismiss = swipeDismiss) { AddFriendScreen() }
                        entry<Route.ScanQr>(swipeDismiss = swipeDismiss) {
                            ScanQrScreen(onClose = { navigator.pop() })
                        }
                        entry<Route.AddFriendVerify>(swipeDismiss = swipeDismiss) { key ->
                            AddFriendVerifyScreen(
                                uid = key.uid,
                                uin = key.uin,
                                name = key.name,
                                detail = key.detail,
                                phone = key.phone,
                                token = key.token,
                            )
                        }
                        entry<Route.CreateGroup>(swipeDismiss = swipeDismiss) { CreateGroupScreen() }
                        entry<Route.ChatInfo>(swipeDismiss = swipeDismiss) { key ->
                            ChatInfoScreen(key.chatId)
                        }
                        entry<Route.ChatMembers>(swipeDismiss = swipeDismiss) { key ->
                            ChatMembersScreen(key.chatId)
                        }
                        entry<Route.ChatNotice>(swipeDismiss = swipeDismiss) { key ->
                            ChatNoticeScreen(key.chatId)
                        }
                        entry<Route.ChatNoticeDetail>(swipeDismiss = swipeDismiss) { key ->
                            ChatNoticeDetailScreen(key.chatId, key.noticeId)
                        }
                        entry<Route.ChatFiles>(swipeDismiss = swipeDismiss) { key ->
                            GroupFilesScreen(
                                chatId = key.chatId,
                                initialFolderId = key.folderId,
                                initialFolderName = key.folderName,
                            )
                        }
                        entry<Route.ChatRecord>(swipeDismiss = swipeDismiss) { key ->
                            ChatRecordScreen(
                                chatId = key.chatId,
                                msgId = key.msgId,
                                elementId = key.elementId,
                                parentMsgId = key.parentMsgId,
                                rootMsgId = key.rootMsgId,
                            )
                        }
                        entry<Route.Forward>(swipeDismiss = swipeDismiss) { key ->
                            ForwardScreen(key.srcChatId, key.msgIds, key.preview)
                        }
                        entry<Route.ChatImage>(swipeDismiss = swipeDismiss) { key ->
                            ChatImageScreen(
                                imagePath = key.imagePath,
                                chatId = key.chatId,
                                msgId = key.msgId,
                                elementId = key.elementId,
                            )
                        }
                        entry<Route.ChatVideo>(swipeDismiss = swipeDismiss) { key ->
                            ChatVideoScreen(
                                videoPath = key.videoPath,
                                thumbnailPath = key.thumbnailPath,
                                chatId = key.chatId,
                                msgId = key.msgId,
                                elementId = key.elementId,
                                videoWidth = key.videoWidth,
                                videoHeight = key.videoHeight,
                            )
                        }
                        entry<Route.ContactDetail>(swipeDismiss = swipeDismiss) { key ->
                            ContactDetailScreen(
                                contactId = key.contactId,
                                chatId = key.chatId,
                                uid = key.uid,
                                uin = key.uin,
                            )
                        }
                        entry<Route.NewFriends>(swipeDismiss = swipeDismiss) { NewFriendsScreen() }
                        entry<Route.GroupChats>(swipeDismiss = swipeDismiss) { GroupChatsScreen() }
                        entry<Route.Tags>(swipeDismiss = swipeDismiss) { TagsScreen() }
                        entry<Route.OfficialAccounts>(swipeDismiss = swipeDismiss) { OfficialAccountsScreen() }
                        entry<Route.EditProfile>(swipeDismiss = swipeDismiss) { EditProfileScreen() }
                        entry<Route.Favorites>(swipeDismiss = swipeDismiss) { FavoritesScreen() }
                        entry<Route.Files>(swipeDismiss = swipeDismiss) { FilesScreen() }
                        entry<Route.Wallet>(swipeDismiss = swipeDismiss) { WalletScreen() }
                        entry<Route.Services>(swipeDismiss = swipeDismiss) { ServicesScreen() }
                    }
                }

                // 调试/QA 用：debug 包可以通过 intent extra 直接深链到某个页面：
                // adb shell am start -n <pkg>/com.material3tim.ui.MainActivity --es qa_route wallet
                if (BuildConfig.DEBUG && qaRoute != null) {
                    LaunchedEffect(qaRoute) {
                        when (qaRoute) {
                            "chat" -> navigator.push(Route.ChatDetail("chat_project"))
                            "chatinfo" -> navigator.push(Route.ChatInfo("chat_project"))
                            "contact" -> navigator.push(Route.ContactDetail("c_baba"))
                            "newfriends" -> navigator.push(Route.NewFriends)
                            "groupchats" -> navigator.push(Route.GroupChats)
                            "tags" -> navigator.push(Route.Tags)
                            "official" -> navigator.push(Route.OfficialAccounts)
                            "editprofile" -> navigator.push(Route.EditProfile)
                            "favorites" -> navigator.push(Route.Favorites)
                            "files" -> navigator.push(Route.Files)
                            "wallet" -> navigator.push(Route.Wallet)
                            "services" -> navigator.push(Route.Services)
                            "settings" -> navigator.push(Route.Settings)
                            "theme" -> navigator.push(Route.ColorPalette)
                            "about" -> navigator.push(Route.About)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (uiMode) {
                        UiMode.Material -> androidx.compose.material3.Scaffold(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) { navDisplay() }

                        UiMode.Miuix -> Scaffold { navDisplay() }
                    }
                }
                SideEffect { onContentReady() }
            }
        }
}
