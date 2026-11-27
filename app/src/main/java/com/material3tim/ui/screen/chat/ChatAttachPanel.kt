package com.material3tim.ui.screen.chat

import kotlin.math.roundToInt
import com.material3tim.ui.component.miuix.glass.GlassMenuEntry
import com.material3tim.ui.component.miuix.glass.LocalGlassMenu
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.material.trackPressPosition
import com.material3tim.ui.component.material.OffsetAnchoredExpressiveMenu
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.ContactPage
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.automirrored.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.ui.theme.LocalEnableBlur
import com.material3tim.ui.theme.isInDarkTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.TabRowDefaults
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close as MiuixClose
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.withFrameNanos

/**
 * 附件面板的 12 个快捷动作（4 列 × 3 行，按「媒体 / 文件工具 / 会话工具」分组）。
 */
internal enum class ChatAttachAction(val labelRes: Int) {
    Photo(R.string.attach_panel_gallery),
    Camera(R.string.attach_panel_camera),
    VoiceCall(R.string.attach_panel_voice_call),
    VideoCall(R.string.attach_panel_video_call),
    Files(R.string.attach_panel_files),
    Favorites(R.string.attach_panel_favorites),
    ScreenShare(R.string.attach_panel_screen_share),
    RedPacket(R.string.attach_panel_red_packet),
    Contact(R.string.attach_panel_contacts),
    Location(R.string.attach_panel_pin),
    ReceiveMoney(R.string.attach_panel_receive_money),
    TencentDocs(R.string.attach_panel_tencent_docs),
}

/** 两种 UI 风格共用的图标：统一 Material Rounded 图标族，避免混用导致字形风格不一致。 */
private fun ChatAttachAction.icon(): ImageVector = when (this) {
    ChatAttachAction.Photo -> Icons.Rounded.PhotoLibrary
    ChatAttachAction.Camera -> Icons.Rounded.PhotoCamera
    ChatAttachAction.VoiceCall -> Icons.Rounded.Call
    ChatAttachAction.VideoCall -> Icons.Rounded.Videocam
    ChatAttachAction.Files -> Icons.Rounded.Folder
    ChatAttachAction.Favorites -> Icons.Rounded.Star
    ChatAttachAction.ScreenShare -> Icons.AutoMirrored.Rounded.ScreenShare
    ChatAttachAction.RedPacket -> Icons.Rounded.Redeem
    ChatAttachAction.Contact -> Icons.Rounded.ContactPage
    ChatAttachAction.Location -> Icons.Rounded.Place
    ChatAttachAction.ReceiveMoney -> Icons.Rounded.Paid
    ChatAttachAction.TencentDocs -> Icons.AutoMirrored.Rounded.Article
}

/** 4 列 × 3 行，按语义分组的行顺序。 */
private val ChatAttachRows: List<List<ChatAttachAction>> = listOf(
    // TIM 第一排：照片 / 拍照 / 语音通话 / 视频通话
    listOf(
        ChatAttachAction.Photo,
        ChatAttachAction.Camera,
        ChatAttachAction.VoiceCall,
        ChatAttachAction.VideoCall,
    ),
    // TIM 第二排：文件 / 收藏 / 分享屏幕 / 红包
    listOf(
        ChatAttachAction.Files,
        ChatAttachAction.Favorites,
        ChatAttachAction.ScreenShare,
        ChatAttachAction.RedPacket,
    ),
    // TIM 第二页：名片 / 位置 / 收钱 / 腾讯文档
    listOf(
        ChatAttachAction.Contact,
        ChatAttachAction.Location,
        ChatAttachAction.ReceiveMoney,
        ChatAttachAction.TencentDocs,
    ),
)

// ============================================================================================
// Material 3 Expressive 版本
// ============================================================================================

/**
 * “+” 附件面板（Material 3 Expressive）。
 *
 * 结构：顶部 28dp 圆角的 surfaceContainerLow 面板 → 4×3 圆形按钮网格（56dp，surfaceContainerHigh）
 * → 底部一条低干扰的提示条（surfaceContainerHigh，圆角 12dp）。
 */
@Composable
internal fun ChatAttachPanelMaterial(
    hintVisible: Boolean,
    onDismissHint: () -> Unit,
    onAction: (ChatAttachAction) -> Unit,
    onCameraAction: (Boolean) -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    // 相机单击：弹出「拍照 / 录像」气泡菜单（锚点=点击位置），不直接拉起相机。
    var cameraMenuVisible by remember { mutableStateOf(false) }
    var cameraMenuOffset by remember { mutableStateOf(IntOffset.Zero) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 22.dp),
        ) {
            ChatAttachRows.forEachIndexed { index, rowActions ->
                if (index > 0) Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    rowActions.forEach { action ->
                        Box(modifier = Modifier.weight(1f)) {
                            AttachItemMaterial(
                                action = action,
                                onClick = {
                                    if (action == ChatAttachAction.Camera) {
                                        cameraMenuVisible = true
                                    } else {
                                        onAction(action)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (action == ChatAttachAction.Camera) {
                                            Modifier.trackPressPosition { offset ->
                                                cameraMenuOffset = IntOffset(
                                                    offset.x.roundToInt(),
                                                    offset.y.roundToInt(),
                                                )
                                            }
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                            if (action == ChatAttachAction.Camera && cameraMenuVisible) {
                                CameraMenuMaterial(
                                    anchorOffset = cameraMenuOffset,
                                    onDismiss = { cameraMenuVisible = false },
                                    onSelect = { isVideo ->
                                        cameraMenuVisible = false
                                        onCameraAction(isVideo)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (hintVisible) {
                Spacer(Modifier.height(24.dp))
                AttachHintMaterial(onDismissHint = onDismissHint)
            }
        }
    }
}

@Composable
private fun AttachItemMaterial(
    action: ChatAttachAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val label = stringResource(action.labelRes)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 横向大胶囊按钮：宽度占满列、高度 52dp，两端完全圆角（CircleShape 随宽度拉伸为胶囊）。
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            color = colorScheme.surfaceContainerHigh,
            contentColor = colorScheme.onSurfaceVariant,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = action.icon(),
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AttachHintMaterial(onDismissHint: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = colorScheme.surfaceContainerHigh,
        contentColor = colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.attach_panel_hint),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(
                onClick = onDismissHint,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.chat_v2_hint_dismiss),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

// ============================================================================================
// Miuix 版本
// ============================================================================================

/** 附件胶囊卡片的统一圆角（图标与卡片共用同一档圆角）。 */
private val MiuixAttachCornerRadius = 12.dp

/** 每个动作的图标底色（固定彩色，参考系统文件类型图标的配色，不随主题取色）。 */
private fun ChatAttachAction.miuixAccent(): androidx.compose.ui.graphics.Color = when (this) {
    ChatAttachAction.Photo -> androidx.compose.ui.graphics.Color(0xFF6C63FF)
    ChatAttachAction.Camera -> androidx.compose.ui.graphics.Color(0xFF2979FF)
    ChatAttachAction.VoiceCall -> androidx.compose.ui.graphics.Color(0xFF34C759)
    ChatAttachAction.VideoCall -> androidx.compose.ui.graphics.Color(0xFF00B8D4)
    ChatAttachAction.Files -> androidx.compose.ui.graphics.Color(0xFFFF9F1C)
    ChatAttachAction.Favorites -> androidx.compose.ui.graphics.Color(0xFFFFB300)
    ChatAttachAction.ScreenShare -> androidx.compose.ui.graphics.Color(0xFF7C4DFF)
    ChatAttachAction.RedPacket -> androidx.compose.ui.graphics.Color(0xFFFF5A5F)
    ChatAttachAction.Contact -> androidx.compose.ui.graphics.Color(0xFF3D9BFF)
    ChatAttachAction.Location -> androidx.compose.ui.graphics.Color(0xFFFF3B30)
    ChatAttachAction.ReceiveMoney -> androidx.compose.ui.graphics.Color(0xFFFF9500)
    ChatAttachAction.TencentDocs -> androidx.compose.ui.graphics.Color(0xFF00C853)
}

/**
 * “+” 附件面板（Miuix）—— 两列胶囊卡片：每项由一张圆角很大的 [Card]（近似胶囊）包住
 * 「彩色圆角图标 + 标题」，点击即触发；底部保留一行低干扰提示文字。
 *
 * 旧的实现保留为 [ChatAttachPanelMiuixCardList]（Card + BasicComponent 列表）与
 * [ChatAttachPanelMiuixGrid]（无容器 4×3 网格），方便随时回退。
 */
@Composable
internal fun ChatAttachPanelMiuix(
    hintVisible: Boolean,
    onDismissHint: () -> Unit,
    onAction: (ChatAttachAction) -> Unit,
    onCameraAction: (Boolean) -> Unit = {},
) {
    // 相机单击：弹出「拍照 / 录像」气泡菜单（交给根级 GlassMenuHost 的 GlassPopup）。
    var cameraMenuVisible by remember { mutableStateOf(false) }
    // 玻璃菜单锚点（窗口坐标）：相机项布局后记录，点击时交给根级 GlassMenuHost。
    // 相机项是点击触发（没有长按点）：用项中心点的 1x1 morph 锚点，
    // 菜单从中心点展开/收缩，不会扫过整个控件矩形。
    val cameraAnchor = rememberGlassPopupAnchor()
    // 每页最多 3 行 × 2 列 = 6 项，左右滑动切换页面；底栏用 Miuix TabRow。
    val pages = ChatAttachAction.entries.chunked(MiuixAttachPageSize)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val pageTitles = listOf(
        stringResource(R.string.attach_page_basic),
        stringResource(R.string.attach_page_more),
    )

    // Phase 2f：附件面板没有自己的底色容器（直接铺在页面背景上），这里给面板内玻璃 Tab 建独立采样源：
    // 只录制 Tab 所在区域的页面底色，避免采样到页面录制层里 Tab 自己的上一帧（反馈叠亮）。
    val panelBackdrop = rememberPageBackdrop(LocalEnableBlur.current)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(MiuixAttachPagerHeight),
        ) { pageIndex ->
            Column(modifier = Modifier.fillMaxSize()) {
                pages[pageIndex].chunked(2).forEachIndexed { rowIndex, pair ->
                    if (rowIndex > 0) Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pair.forEach { action ->
                            Box(
                                modifier = Modifier
                                    .weight(1f),
                            ) {
                                if (action == ChatAttachAction.Camera) {
                                    // 相机项中心点 1dp 透明锚点：从中心展开/收缩，不扫过整项。
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(1.dp)
                                            .glassPopupAnchor(
                                                anchor = cameraAnchor,
                                                cornerRadius = 24.dp,
                                                floating = true,
                                            ),
                                    )
                                }
                                AttachCapsuleMiuix(
                                    action = action,
                                    onClick = {
                                        if (action == ChatAttachAction.Camera) {
                                            cameraMenuVisible = true
                                        } else {
                                            onAction(action)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (action == ChatAttachAction.Camera && cameraMenuVisible) {
                                    CameraMenuMiuix(
                                        anchor = cameraAnchor,
                                        onDismiss = { cameraMenuVisible = false },
                                        onSelect = { isVideo ->
                                            cameraMenuVisible = false
                                            onCameraAction(isVideo)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // 提示行：放在分页内容与底部 TabRow 之间；关闭后不影响下面的固定间距。
        if (hintVisible) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixText(
                    text = stringResource(R.string.attach_panel_hint),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                MiuixIconButton(
                    onClick = onDismissHint,
                    modifier = Modifier.size(32.dp),
                ) {
                    MiuixIcon(
                        imageVector = MiuixIcons.MiuixClose,
                        contentDescription = stringResource(R.string.chat_v2_hint_dismiss),
                        tint = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        // 固定间距：无论提示行是否显示都保留，避免关闭提示后卡片与 TabRow 贴到 0 间距。
        Spacer(Modifier.height(10.dp))
        // 底栏沿用原来的 TabRowWithContour（容器 + 内嵌胶囊），配色用 Miuix 默认值：
        // 之前自定义的 surfaceContainer / surfaceContainerHigh 在深色下与页面背景太接近，
        // 选中胶囊和容器会融为一体，默认配色对明暗主题都有足够对比。
        if (pages.size > 1) {
            // 玻璃 Tab 的独立采样层：只录制 Tab 所在区域的页面底色（不含 Tab 自身）。
            Box(modifier = Modifier.fillMaxWidth()) {
                if (panelBackdrop != null) {
                    Box(Modifier.matchParentSize().layerBackdrop(panelBackdrop))
                }
                if (panelBackdrop != null) {
                    // backdrop 可用（模糊开启 + 设备支持 runtime shader）→ miuix-glass 分段玻璃 Tab。
                    GlassSegmentedTabRow(
                        tabs = pageTitles,
                        selectedIndex = pagerState.currentPage,
                        onSelect = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        backdrop = panelBackdrop,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    // 回退：关闭模糊 / 设备不支持 runtime shader → 原 TabRowWithContour。
                    // 容器 / 选中胶囊配色（明暗主题都保证三层可见）：
                    // - 深色：页面背景 surface=#000000，容器 #242424，胶囊 #2D2D2D（不再与容器同色）
                    // - 浅色：页面背景 #F7F7F7，容器 #E8E8E8，胶囊 #F7F7F7（还原原来的浅灰容器 + 白胶囊观感）
                    val darkTheme = isInDarkTheme()
                    val trackColor = colorScheme.surfaceContainerHigh
                    val pillColor = if (darkTheme) colorScheme.surfaceContainerHighest else colorScheme.surface
                    TabRowWithContour(
                        tabs = pageTitles,
                        selectedTabIndex = pagerState.currentPage,
                        onTabSelected = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TabRowDefaults.tabRowColors(
                            backgroundColor = trackColor,
                            contentColor = colorScheme.onSurfaceVariantSummary,
                            selectedBackgroundColor = pillColor,
                            selectedContentColor = colorScheme.onSurface,
                        ),
                    )
                }
            }
        }
    }
}

/** Miuix 附加菜单：每页最多 3 行 × 2 列。 */
private const val MiuixAttachPageSize = 6

/** 3 行胶囊卡片（每行 58dp）+ 2 个 8dp 行距。 */
private val MiuixAttachPagerHeight = 58.dp * 3 + 8.dp * 2

/** 单个胶囊卡片：彩色圆角图标 + 标题，整卡可点击。 */
@Composable
private fun AttachCapsuleMiuix(
    action: ChatAttachAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(action.labelRes)
    Card(
        onClick = onClick,
        modifier = modifier,
        cornerRadius = MiuixAttachCornerRadius,
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        showIndication = true,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 图标与卡片统一圆角：都使用 MiuixAttachCornerRadius（圆角矩形，不再是胶囊/圆形）。
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(MiuixAttachCornerRadius))
                    .background(action.miuixAccent()),
                contentAlignment = Alignment.Center,
            ) {
                MiuixIcon(
                    imageVector = action.icon(),
                    contentDescription = label,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            MiuixText(
                text = label,
                fontSize = 15.sp,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ============================================================================================
// 旧版（保留回退）
// ============================================================================================

/**
 * “+” 附件面板（Miuix）—— 试验版：Card + BasicComponent。
 *
 * 一张 Miuix Card 内用 [BasicComponent] 两列排布 12 个动作（6 行 × 2 列），
 * 每个组件 = 图标 + 标题，点击即触发；底部保留一行低干扰提示文字。
 *
 * 说明：旧的「内联网格」实现保留在 [ChatAttachPanelMiuixGrid]，方便随时回退。
 */
@Composable
internal fun ChatAttachPanelMiuixCardList(
    hintVisible: Boolean,
    onDismissHint: () -> Unit,
    onAction: (ChatAttachAction) -> Unit,
    onCameraAction: (Boolean) -> Unit = {},
) {
    // 相机单击：弹出「拍照 / 录像」气泡菜单。
    var cameraMenuVisible by remember { mutableStateOf(false) }
    // 玻璃菜单锚点（窗口坐标）：相机项布局后记录，点击时交给根级 GlassMenuHost。
    // 相机项是点击触发（没有长按点）：用项中心点的 1x1 morph 锚点，
    // 菜单从中心点展开/收缩，不会扫过整个控件矩形。
    val cameraAnchor = rememberGlassPopupAnchor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            ChatAttachAction.entries.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { action ->
                        Box(
                            modifier = Modifier
                                .weight(1f),
                        ) {
                            if (action == ChatAttachAction.Camera) {
                                // 相机项中心点 1dp 透明锚点：从中心展开/收缩，不扫过整项。
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(1.dp)
                                        .glassPopupAnchor(
                                            anchor = cameraAnchor,
                                            cornerRadius = 24.dp,
                                            floating = true,
                                        ),
                                )
                            }
                            val label = stringResource(action.labelRes)
                            BasicComponent(
                                modifier = Modifier.fillMaxWidth(),
                                title = label,
                                startAction = {
                                    MiuixIcon(
                                        imageVector = action.icon(),
                                        contentDescription = label,
                                        tint = colorScheme.onSurfaceVariantSummary,
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(22.dp),
                                    )
                                },
                                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                onClick = {
                                    if (action == ChatAttachAction.Camera) {
                                        cameraMenuVisible = true
                                    } else {
                                        onAction(action)
                                    }
                                },
                            )
                            if (action == ChatAttachAction.Camera && cameraMenuVisible) {
                                CameraMenuMiuix(
                                    anchor = cameraAnchor,
                                    onDismiss = { cameraMenuVisible = false },
                                    onSelect = { isVideo ->
                                        cameraMenuVisible = false
                                        onCameraAction(isVideo)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (hintVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixText(
                    text = stringResource(R.string.attach_panel_hint),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                MiuixIconButton(
                    onClick = onDismissHint,
                    modifier = Modifier.size(32.dp),
                ) {
                    MiuixIcon(
                        imageVector = MiuixIcons.MiuixClose,
                        contentDescription = stringResource(R.string.chat_v2_hint_dismiss),
                        tint = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * 旧版（当前线上方案）：不加额外背景容器的 4×3 内联网格。
 * 保留用于随时回退。
 */
@Composable
internal fun ChatAttachPanelMiuixGrid(
    hintVisible: Boolean,
    onDismissHint: () -> Unit,
    onAction: (ChatAttachAction) -> Unit,
    onCameraAction: (Boolean) -> Unit = {},
) {
    // 相机单击：弹出「拍照 / 录像」气泡菜单。
    var cameraMenuVisible by remember { mutableStateOf(false) }
    // 玻璃菜单锚点（窗口坐标）：相机项布局后记录，点击时交给根级 GlassMenuHost。
    // 相机项是点击触发（没有长按点）：用项中心点的 1x1 morph 锚点，
    // 菜单从中心点展开/收缩，不会扫过整个控件矩形。
    val cameraAnchor = rememberGlassPopupAnchor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Column {
            ChatAttachRows.forEachIndexed { index, rowActions ->
                if (index > 0) Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowActions.forEach { action ->
                        Box(
                            modifier = Modifier
                                .weight(1f),
                        ) {
                            if (action == ChatAttachAction.Camera) {
                                // 相机项中心点 1dp 透明锚点：从中心展开/收缩，不扫过整项。
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(1.dp)
                                        .glassPopupAnchor(
                                            anchor = cameraAnchor,
                                            cornerRadius = 24.dp,
                                            floating = true,
                                        ),
                                )
                            }
                            AttachItemMiuix(
                                action = action,
                                onClick = {
                                    if (action == ChatAttachAction.Camera) {
                                        cameraMenuVisible = true
                                    } else {
                                        onAction(action)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (action == ChatAttachAction.Camera && cameraMenuVisible) {
                                CameraMenuMiuix(
                                    anchor = cameraAnchor,
                                    onDismiss = { cameraMenuVisible = false },
                                    onSelect = { isVideo ->
                                        cameraMenuVisible = false
                                        onCameraAction(isVideo)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (hintVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MiuixText(
                    text = stringResource(R.string.attach_panel_hint),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                MiuixIconButton(
                    onClick = onDismissHint,
                    modifier = Modifier.size(32.dp),
                ) {
                    MiuixIcon(
                        imageVector = MiuixIcons.MiuixClose,
                        contentDescription = stringResource(R.string.chat_v2_hint_dismiss),
                        tint = colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachItemMiuix(
    action: ChatAttachAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(action.labelRes)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            MiuixIcon(
                imageVector = action.icon(),
                contentDescription = label,
                tint = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        MiuixText(
            text = label,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ============================================================================================
// 相机单击菜单（拍照 / 录像）：样式与长按消息气泡菜单一致
// ============================================================================================

/** 相机菜单选项：false=拍照，true=录像。 */
private val CameraMenuOptions: List<Pair<Int, Boolean>> = listOf(
    R.string.attach_panel_camera to false,
    R.string.attach_panel_record_video to true,
)

private fun cameraMenuIcon(isVideo: Boolean): ImageVector =
    if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.PhotoCamera

/** M3E：与长按消息气泡同款 OffsetAnchoredExpressiveMenu，锚点=单击位置。 */
@Composable
private fun CameraMenuMaterial(
    anchorOffset: IntOffset,
    onDismiss: () -> Unit,
    onSelect: (Boolean) -> Unit,
) {
    OffsetAnchoredExpressiveMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        anchorOffset = anchorOffset,
    ) {
        CameraMenuOptions.forEachIndexed { index, (labelRes, isVideo) ->
            SelectableDropdownMenuItem(
                text = { Text(stringResource(labelRes)) },
                selected = false,
                leadingIcon = {
                    Icon(
                        imageVector = cameraMenuIcon(isVideo),
                        contentDescription = null,
                        modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                    )
                },
                shapes = MenuDefaults.itemShape(index = index, count = CameraMenuOptions.size),
                onClick = { onSelect(isVideo) },
            )
        }
    }
}

/**
 * 相机菜单（Miuix）：锚点 = 相机项中心点的 1dp 透明 Box（点击触发，没有长按位置），
 * 页面级宿主用 `GlassTransformPopup` 从中心点展开，预测性返回跟手收缩回该点。
 */
@Composable
private fun CameraMenuMiuix(
    anchor: GlassPopupAnchor,
    onDismiss: () -> Unit,
    onSelect: (Boolean) -> Unit,
) {
    val glassMenu = LocalGlassMenu.current
    // 页面采样只能在页面 CompositionLocal 作用域内读，这里读到后交给根级状态保存。
    val pageBackdrop = LocalPageBackdrop.current
    // 菜单发出方 token：附件面板被收起时关闭残留菜单。
    val menuOwner = remember { Any() }
    DisposableEffect(menuOwner) {
        onDispose { glassMenu?.hideIfOwner(menuOwner) }
    }
    val entries = CameraMenuOptions.map { (labelRes, isVideo) ->
        GlassMenuEntry(
            label = stringResource(labelRes),
            icon = cameraMenuIcon(isVideo),
            onClick = { onSelect(isVideo) },
        )
    }
    // 本 composable 只在 cameraMenuVisible 时进入组合：先等一帧让中心点 1dp 锚点完成测量，
    // 再交给页面级宿主用 GlassTransformPopup 从相机项中心展开（预测性返回跟手收缩）。
    LaunchedEffect(Unit) {
        withFrameNanos { }
        glassMenu?.show(
            owner = menuOwner,
            anchor = anchor,
            entries = entries,
            backdrop = pageBackdrop,
            onDismiss = onDismiss,
        )
    }
}
