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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.material3tim.BuildConfig
import com.material3tim.R
import com.material3tim.data.repository.ChatRepository
import com.material3tim.data.repository.LiveChats
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.component.bottombar.BottomBar
import com.material3tim.ui.component.bottombar.LauncherFloatingBottomBar
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
import com.material3tim.ui.screen.chat.ChatInfoScreen
import com.material3tim.ui.screen.chat.ChatPager
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
import com.material3tim.ui.theme.LocalEnableLauncherFloatingToolbar
import com.material3tim.ui.theme.LocalEnableNavigationBadge
import com.material3tim.ui.theme.LocalShowNavigationBar
import com.material3tim.ui.theme.M3TimTheme
import com.material3tim.ui.util.rememberBlurBackdrop
import com.material3tim.ui.util.rememberContentReady
import com.material3tim.ui.viewmodel.MainActivityViewModel
import com.material3tim.ui.viewmodel.MainPagerConfig
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

class MainActivity : ComponentActivity() {

    private var contentReady = false
    private var splashStartedAt = 0L
    private val splashAnimationDurationMs = 500L

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashStartedAt = SystemClock.uptimeMillis()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition {
            !contentReady || SystemClock.uptimeMillis() - splashStartedAt < splashAnimationDurationMs
        }

        setContent {
            val viewModel = viewModel<MainActivityViewModel>()
            M3TimRoot(
                viewModel = viewModel,
                qaRoute = intent?.getStringExtra("qa_route"),
                onDarkModeChanged = { dark -> applySystemBars(dark) },
                onContentReady = { contentReady = true },
            )
        }
    }

    /** 状态栏 / 导航栏透明 + 图标明暗跟随主题。 */
    private fun applySystemBars(darkMode: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { darkMode },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { darkMode },
        )
        window.isNavigationBarContrastEnforced = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

val LocalMainPagerState = staticCompositionLocalOf<MainPagerState> { error("LocalMainPagerState not provided") }

/**
 * 底栏「消息」重复点击（已经在消息页）时的列表跳转事件：递增计数器。
 * 聊天列表页（M3E ChatPagerMaterial / Miuix ChatPagerMiuix）监听该计数，
 * 每次自增跳到「下一个未读会话」（排除免打扰，与底栏角标口径一致）并循环。
 */
object ChatListJumpRequests {
    private val _requests = MutableStateFlow(0L)
    val requests: StateFlow<Long> = _requests.asStateFlow()

    fun requestJump() {
        _requests.value = _requests.value + 1L
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    initialPage: Int = 0,
    pagerInterceptionMode: Int = PagerInterceptionMode.CrossAxis.ordinal,
    onPageChanged: (Int) -> Unit = {},
) {
    val navController = LocalNavigator.current
    val enableBlur = LocalEnableBlur.current
    val enableFloatingBottomBar = LocalEnableFloatingBottomBar.current
    val enableFloatingBottomBarBlur = LocalEnableFloatingBottomBarBlur.current
    // Launcher3 悬浮底栏是 M3E 专属（Miuix 使用自己的底栏体系）。
    val enableLauncherFloatingToolbar =
        LocalEnableLauncherFloatingToolbar.current && LocalUiMode.current == UiMode.Material
    val uiMode = LocalUiMode.current
    // 「显示底栏」关闭：底栏和宽屏侧栏都不渲染（内容 bottom padding 由 Scaffold insets 自然给出）。
    // 该设置只属于 Miuix 设置页（M3E 保持自己的底栏不变，避免切到 M3E 后无法从 M3E 设置页改回）。
    val showNavigationBar = LocalShowNavigationBar.current || uiMode == UiMode.Material
    val useNavigationRail = useNavigationRail(enableFloatingBottomBar) &&
        showNavigationBar && !enableLauncherFloatingToolbar
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { MainPagerConfig.PAGE_COUNT })
    val mainPagerState = rememberMainPagerState(
        pagerState = pagerState,
        animatePageChanges = !useNavigationRail,
    )
    val pagerMode = PagerInterceptionMode.entries.getOrElse(pagerInterceptionMode) {
        PagerInterceptionMode.Native
    }
    val interceptPagerGestures = pagerMode == PagerInterceptionMode.CrossAxis

    val enableNavigationBadge = LocalEnableNavigationBadge.current
    // 底栏未读角标：宿主(TIM 进程)里订阅 [LiveChats.liveFlow] 计算真实未读；独立 App(无数据源)
    // 才用示例会话兜底；开关关闭时固定 0。[LiveChats.liveFlow] 在独立 App 恒为 null，
    // 订阅它不会产生额外刷新。列表每次推送都会替换实例，用 derivedStateOf 收敛成未读总数，
    // 只有总数真正变化时才让 MainScreen 重组。
    val liveChatsState = LiveChats.liveFlow.collectAsStateWithLifecycle()
    val chatUnreadCount by remember(enableNavigationBadge) {
        derivedStateOf {
            when {
                !enableNavigationBadge -> 0
                AppDataSources.hasLiveDataSource ->
                    liveChatsState.value.orEmpty().sumOf { chat ->
                        // 只统计正常通知的未读，排除免打扰会话（否则数字会非常大）。
                        if (chat.unreadCount > 0 && !chat.isMuted) chat.unreadCount else 0
                    }
                else -> ChatRepository.sampleChats.sumOf { chat ->
                    if (chat.unreadCount > 0 && !chat.isMuted) chat.unreadCount else 0
                }
            }
        }
    }

    val navigationBadge = NavigationBadgeState(unreadChatCount = chatUnreadCount)
    val surfaceColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surface
        UiMode.Miuix -> MiuixTheme.colorScheme.surface
    }
    // 全局 blurBackdrop 只服务于 Miuix「标准」底栏的磨砂（BottomBarMiuix 的 BlurredBar）。
    // M3E 底栏不使用它；Miuix 浮动底栏（Glass/iOS-like/Original）用页面/悬浮栏自己的 backdrop。
    // 之前无条件录制：只要用户开了「模糊」，M3E/Miuix 首页 pager 内容就会被整层录制进
    // GraphicsLayer（滚动时每帧重录），是首页滑动卡顿的主要来源之一。
    val blurBackdrop = rememberBlurBackdrop(
        enableBlur && uiMode == UiMode.Miuix && !enableFloatingBottomBar,
    )

    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val settledPage = mainPagerState.pagerState.settledPage
    LaunchedEffect(settledPage) {
        onPageChanged(settledPage)
    }

    val currentPage = mainPagerState.pagerState.currentPage
    LaunchedEffect(currentPage) {
        mainPagerState.syncPage()
    }

    MainScreenBackHandler(mainPagerState, navController)

    CompositionLocalProvider(
        LocalMainPagerState provides mainPagerState
    ) {
        val contentReady = rememberContentReady()
        // 新 API：pagerGestureOverride 需要与 HorizontalPager 共用同一个 flingBehavior。
        val pagerFlingBehavior = flingBehavior(
            state = mainPagerState.pagerState,
            snapAnimationSpec = PagerNavigationSpringSpec,
        )
        val pagerContent = @Composable { bottomInnerPadding: Dp ->
            Box(modifier = if (blurBackdrop != null) Modifier.layerBackdrop(blurBackdrop) else Modifier) {
                HorizontalPager(
                    modifier = Modifier
                        .pagerGestureOverride(
                            pagerState = mainPagerState.pagerState,
                            flingBehavior = pagerFlingBehavior,
                            mode = pagerMode,
                            enabled = true,
                        )
                        .then(
                            if (enableFloatingBottomBar && enableFloatingBottomBarBlur) {
                                Modifier.layerBackdrop(backdrop)
                            } else Modifier
                        ),
                    state = mainPagerState.pagerState,
                    beyondViewportPageCount = if (contentReady) 3 else 0,
                    overscrollEffect = null,
                    userScrollEnabled = !interceptPagerGestures,
                    pageNestedScrollConnection = if (interceptPagerGestures) {
                        PagerGestureNestedScrollConnection
                    } else {
                        pageNestedScrollConnection(
                            state = mainPagerState.pagerState,
                            orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                        )
                    },
                    flingBehavior = pagerFlingBehavior,
                ) { page ->
                    val isCurrentPage = page == settledPage
                    when (page) {
                        0 -> if (contentReady || isCurrentPage) ChatPager(navController, bottomInnerPadding, isCurrentPage)
                        1 -> if (contentReady || isCurrentPage) ContactsPager(navController, bottomInnerPadding, isCurrentPage)
                        2 -> if (contentReady || isCurrentPage) ProfilePager(navController, bottomInnerPadding, isCurrentPage)
                    }
                }
            }
        }

        if (useNavigationRail) {
            val startInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Start)
            val navBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row {
                        SideRail(navigationBadge)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }

                UiMode.Miuix -> Scaffold { _ ->
                    Row {
                        SideRail(navigationBadge)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .consumeWindowInsets(startInsets)
                        ) {
                            pagerContent(navBarBottomPadding)
                        }
                    }
                }
            }
        } else {
            // showNavigationBar=false 时 bottomBar 组合为空（等价 Scaffold bottomBar = {}），
            // innerPadding 底部退化为系统导航栏 inset，页面内容 bottom padding 随之为 0。
            val bottomBar = @Composable {
                if (showNavigationBar) {
                    if (enableLauncherFloatingToolbar) {
                        // Launcher3 微件页同款可滚动悬浮底栏（Material3 FloatingToolbar 风格）。
                        Box(modifier = Modifier.fillMaxWidth()) {
                            LauncherFloatingBottomBar(
                                navigationBadge = navigationBadge,
                                selectedPage = mainPagerState.selectedPage,
                                onSelectPage = { mainPagerState.animateToPage(it) },
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BottomBar(
                                blurBackdrop = blurBackdrop,
                                backdrop = backdrop,
                                navigationBadge = navigationBadge,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                    }
                }
            }

            when (uiMode) {
                UiMode.Material -> androidx.compose.material3.Scaffold(
                    bottomBar = bottomBar,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }

                UiMode.Miuix -> Scaffold(bottomBar = bottomBar) { innerPadding ->
                    pagerContent(innerPadding.calculateBottomPadding())
                }
            }
        }
    }
}

@Composable
private fun MainScreenBackHandler(
    mainState: MainPagerState,
    navController: Navigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navController.current() is Route.Main && navController.backStackSize() == 1 &&
                mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        }
    )
}
