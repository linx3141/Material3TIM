package com.material3tim.ui.screen.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GifBox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.ui.component.material.ExpressiveToggleButton
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.component.material.OffsetAnchoredExpressiveMenu
import com.material3tim.ui.component.miuix.glass.GlassMenuEntry
import com.material3tim.ui.component.miuix.glass.LocalGlassMenu
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.AnimatedOrStaticImage
import com.material3tim.ui.component.SuperEmojiAnimation
import com.material3tim.data.model.CustomEmoji
import com.material3tim.data.model.SuperEmojiItem
import com.material3tim.data.model.SystemFaceItem
import com.material3tim.data.model.StickerPack
import com.material3tim.data.repository.EmojiRepository
import com.material3tim.data.source.AppDataSources
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.glass.GlassSegmentedTabRow
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

/** 表情面板顶部标签页。 */
internal enum class ChatEmojiPanelTab(val labelRes: Int) {
    Emoji(R.string.emoji_tab_emoji),
    Stickers(R.string.emoji_tab_stickers),
}

/** 表情面板高度（约 320dp，键盘高度量级）。 */
internal val ChatEmojiPanelHeight = 320.dp

/** 表情包底部入口索引（「最近」已按需求移除，只保留收藏 / 全部表情包）。 */
private const val STICKER_TAB_FAVORITES = 0
private const val STICKER_TAB_ALL = 1

/** 表情包按名称搜索（两种 UI 模式共用）。 */
internal fun searchStickerPacks(
    query: String,
    base: List<StickerPack> = EmojiRepository.stickerPacks,
): List<StickerPack> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return base
    return base.filter { it.name.contains(keyword, ignoreCase = true) }
}

/** 表情面板数据状态：收藏表情 + 表情包，可由面板打开/切 tab 触发刷新（bridge 内部 30s 节流）。 */
internal class EmojiPanelData(
    val favorites: List<CustomEmoji>,
    val packs: List<StickerPack>,
    val superEmojis: List<SuperEmojiItem>,
    val systemFaces: List<SystemFaceItem>,
    val favoritesLoading: Boolean,
    val packsLoading: Boolean,
    val refresh: () -> Unit,
)

@Composable
internal fun rememberEmojiPanelData(): EmojiPanelData {
    val source = remember { AppDataSources.emojiSource }
    var favorites by remember { mutableStateOf<List<CustomEmoji>>(emptyList()) }
    var packs by remember { mutableStateOf<List<StickerPack>>(EmojiRepository.stickerPacks) }
    var superEmojis by remember { mutableStateOf<List<SuperEmojiItem>>(emptyList()) }
    var systemFaces by remember { mutableStateOf<List<SystemFaceItem>>(emptyList()) }
    var favoritesLoading by remember { mutableStateOf(false) }
    var packsLoading by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }
    // 首次：读缓存（不触发 TIM 同步）。
    LaunchedEffect(source) {
        if (source == null) return@LaunchedEffect
        favoritesLoading = true
        packsLoading = true
        withContext(Dispatchers.IO) {
            source.loadFavoriteEmojis()
        }?.let { favorites = it }
        withContext(Dispatchers.IO) {
            source.loadStickerPacks()
        }?.let { packs = it }
        withContext(Dispatchers.IO) {
            runCatching { source.loadSuperEmojis() }.getOrNull()
        }?.let { superEmojis = it }
        withContext(Dispatchers.IO) {
            runCatching { source.loadSystemFaces() }.getOrNull()
        }?.let { systemFaces = it }
        favoritesLoading = false
        packsLoading = false
    }
    // 面板打开 / 切到收藏/全部表情包：refresh 计数 +1 → IO 拉最新（失败保留旧数据）。
    LaunchedEffect(refreshTick, source) {
        if (refreshTick == 0 || source == null) return@LaunchedEffect
        favoritesLoading = true
        packsLoading = true
        val latestFavorites = withContext(Dispatchers.IO) {
            runCatching { source.refreshFavoriteEmojis() }.getOrNull()
        }
        if (latestFavorites != null) favorites = latestFavorites
        val latestPacks = withContext(Dispatchers.IO) {
            runCatching { source.loadStickerPacks() }.getOrNull()
        }
        if (latestPacks != null) packs = latestPacks
        favoritesLoading = false
        packsLoading = false
    }
    return EmojiPanelData(
        favorites = favorites,
        packs = packs,
        superEmojis = superEmojis,
        systemFaces = systemFaces,
        favoritesLoading = favoritesLoading,
        packsLoading = packsLoading,
        refresh = { refreshTick += 1 },
    )
}

// ============================================================================================
// 表情包详情（M3E / Miuix 共用）
// ============================================================================================

private data class StickerPackEmojisState(
    val emojis: List<CustomEmoji>?,
    val loading: Boolean,
    val failed: Boolean,
)

/** 包内表情加载状态：packId 变化或 retryTick 变化时 IO 调 bridge（bridge 有 5 分钟缓存）。 */
@Composable
private fun rememberStickerPackEmojis(packId: String?, retryTick: Int): StickerPackEmojisState {
    var state by remember(packId) {
        mutableStateOf(StickerPackEmojisState(null, packId != null, false))
    }
    LaunchedEffect(packId, retryTick) {
        if (packId == null) {
            state = StickerPackEmojisState(null, false, false)
            return@LaunchedEffect
        }
        state = StickerPackEmojisState(state.emojis, true, false)
        val source = AppDataSources.emojiSource
        val loaded = if (source == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching { source.loadStickerPackEmojis(packId) }.getOrNull()
            }
        }
        state = StickerPackEmojisState(loaded, false, loaded == null)
    }
    return state
}

/** 包详情视图：顶部「← 包名」+ 包内表情网格（复用 FavoriteEmojiGrid 样式与发送回调）。 */
@Composable
private fun StickerPackDetailContent(
    packId: String,
    packName: String,
    state: StickerPackEmojisState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSendSticker: (packId: String, eId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 返回键：表情包详情内优先回到表情包列表（不关闭整个面板、不退出聊天页）。
    BackHandler { onBack() }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = packName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        val emojis = state.emojis
        when {
            state.loading && emojis == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.failed -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.emoji_pack_load_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.group_files_retry))
                }
            }
            emojis.isNullOrEmpty() -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.emoji_pack_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> FavoriteEmojiGrid(
                items = emojis,
                // 包内表情走 MarketFace 发送接口（bridge 失败时回退图片表情）。
                onEmojiClick = { emoji -> onSendSticker(packId, emoji.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

// ============================================================================================
// Material 3 版本
// ============================================================================================

/**
 * 表情 / 表情包面板（Material 3 Expressive）。
 * 由输入栏的表情按钮展开，位于输入栏上方、键盘隐藏，高度约 320dp。
 */
@Composable
internal fun ChatEmojiPanelMaterial(
    composer: ChatComposerState,
    onSendCustomEmoji: (CustomEmoji) -> Unit = {},
    onSendStickerPackEmoji: (packId: String, eId: String) -> Unit = { _, _ -> },
    onSendSuperEmoji: (SuperEmojiItem) -> Unit = {},
    onSendSystemFace: (SystemFaceItem) -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(ChatEmojiPanelTab.Stickers) }
    var query by rememberSaveable { mutableStateOf("") }
    // 面板打开即触发一次刷新（收藏/表情包）；切换底部 tab 也会再触发（bridge 内部 30s 节流）。
    val emojiData = rememberEmojiPanelData()
    LaunchedEffect(Unit) { emojiData.refresh() }
    val packs = remember(query, emojiData.packs) { searchStickerPacks(query, emojiData.packs) }
    val scope = rememberCoroutineScope()
    // 收藏表情管理：移到首位 / 删除（IO 调 bridge，成功后刷新收藏列表）。
    val onMoveFavoriteToTop: (CustomEmoji) -> Unit = { emoji ->
        scope.launch {
            val source = AppDataSources.emojiSource
            val ok = if (source == null) {
                false
            } else {
                withContext(Dispatchers.IO) {
                    runCatching { source.moveFavoriteEmojiToTop(emoji.id) }.getOrDefault(false)
                }
            }
            Toast.makeText(
                context,
                if (ok) R.string.emoji_favorite_moved_top else R.string.emoji_favorite_operation_failed,
                Toast.LENGTH_SHORT,
            ).show()
            if (ok) emojiData.refresh()
        }
    }
    val onDeleteFavorite: (CustomEmoji) -> Unit = { emoji ->
        scope.launch {
            val source = AppDataSources.emojiSource
            val ok = if (source == null) {
                false
            } else {
                withContext(Dispatchers.IO) {
                    runCatching { source.deleteFavoriteEmoji(emoji.id) }.getOrDefault(false)
                }
            }
            Toast.makeText(
                context,
                if (ok) R.string.emoji_favorite_deleted else R.string.emoji_favorite_operation_failed,
                Toast.LENGTH_SHORT,
            ).show()
            if (ok) emojiData.refresh()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(ChatEmojiPanelHeight),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = colorScheme.surfaceContainerLow,
        contentColor = colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 8.dp),
        ) {
            EmojiTabRowMaterial(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
            when (tab) {
                ChatEmojiPanelTab.Emoji -> EmojiGridMaterial(
                    onEmojiClick = composer::appendEmoji,
                    superEmojis = emojiData.superEmojis,
                    onSuperEmojiClick = onSendSuperEmoji,
                    systemFaces = emojiData.systemFaces,
                    onSystemFaceClick = onSendSystemFace,
                    modifier = Modifier.weight(1f),
                )

                ChatEmojiPanelTab.Stickers -> StickerTabMaterial(
                    query = query,
                    onQueryChange = { query = it },
                    packs = packs,
                    favorites = emojiData.favorites,
                    favoritesLoading = emojiData.favoritesLoading,
                    packsLoading = emojiData.packsLoading,
                    onRefresh = emojiData.refresh,
                    onSendCustomEmoji = onSendCustomEmoji,
                    onSendStickerPackEmoji = onSendStickerPackEmoji,
                    onMoveFavoriteToTop = onMoveFavoriteToTop,
                    onDeleteFavorite = onDeleteFavorite,
                    modifier = Modifier.weight(1f),
                )

            }
        }
    }
}

@Composable
private fun EmojiTabRowMaterial(
    selected: ChatEmojiPanelTab,
    onSelect: (ChatEmojiPanelTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Material 3 Expressive Connected Button Group：三个分段共用外轮廓，只用 2dp 间隙，
    // 首/尾分段是外侧大圆角，中间分段是小圆角。
    val entries = ChatEmojiPanelTab.entries
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        entries.forEachIndexed { index, tab ->
            val isSelected = tab == selected
            ExpressiveToggleButton(
                checked = isSelected,
                onCheckedChange = { checked -> if (checked) onSelect(tab) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(tab.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EmojiGridMaterial(
    onEmojiClick: (String) -> Unit,
    superEmojis: List<SuperEmojiItem> = emptyList(),
    onSuperEmojiClick: (SuperEmojiItem) -> Unit = {},
    systemFaces: List<SystemFaceItem> = emptyList(),
    onSystemFaceClick: (SystemFaceItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            EmojiSectionTitleMaterial(stringResource(R.string.emoji_recent_title))
        }
        items(
            count = EmojiRepository.recentEmojis.size,
            key = { index -> "recent_$index" },
        ) { index ->
            EmojiCellMaterial(EmojiRepository.recentEmojis[index], onEmojiClick)
        }
        // 超级表情放在「全部表情」之前，打开表情页更容易看到（TIM 原生也是单独一栏大表情）。
        if (superEmojis.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmojiSectionTitleMaterial(
                    text = stringResource(R.string.emoji_super_title),
                    topPadding = 8.dp,
                )
            }
            items(
                count = superEmojis.size,
                key = { index -> "super_${superEmojis[index].serverId}" },
            ) { index ->
                SuperEmojiCellMaterial(
                    item = superEmojis[index],
                    onClick = { onSuperEmojiClick(superEmojis[index]) },
                )
            }
        }
        if (systemFaces.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmojiSectionTitleMaterial(
                    text = stringResource(R.string.emoji_system_face_title),
                    topPadding = 8.dp,
                )
            }
            items(
                count = systemFaces.size,
                key = { index -> "sysface_${systemFaces[index].serverId}" },
            ) { index ->
                SystemFaceCellMaterial(
                    item = systemFaces[index],
                    onClick = { onSystemFaceClick(systemFaces[index]) },
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            EmojiSectionTitleMaterial(
                text = stringResource(R.string.chat_v2_all_emoji_title),
                topPadding = 8.dp,
            )
        }
        items(
            count = EmojiRepository.emojis.size,
            key = { index -> "all_$index" },
        ) { index ->
            EmojiCellMaterial(EmojiRepository.emojis[index], onEmojiClick)
        }
    }
}

/** 表情页「小黄脸」单元格：TIM 内置静态表情小图。 */
@Composable
private fun SystemFaceCellMaterial(item: SystemFaceItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedOrStaticImage(
            path = item.imagePath,
            contentDescription = item.desc,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(32.dp),
        )
    }
}

/** 表情页「超级表情」单元格：面板内只显示静态首帧（避免几十个 Lottie 同时播放卡顿）。 */
@Composable
private fun SuperEmojiCellMaterial(item: SuperEmojiItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SuperEmojiAnimation(
            lottiePath = item.lottiePath,
            imagePath = item.imagePath,
            contentDescription = item.desc,
            fallbackText = item.desc.take(1).ifBlank { "表" },
            modifier = Modifier.size(34.dp),
            animate = false,
        )
    }
}

@Composable
private fun EmojiSectionTitleMaterial(text: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = 4.dp,
            top = 4.dp + topPadding,
            bottom = 2.dp,
        ),
    )
}

@Composable
private fun EmojiCellMaterial(emoji: String, onEmojiClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onEmojiClick(emoji) },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 24.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun StickerTabMaterial(
    query: String,
    onQueryChange: (String) -> Unit,
    packs: List<StickerPack>,
    favorites: List<CustomEmoji>,
    favoritesLoading: Boolean,
    packsLoading: Boolean,
    onRefresh: () -> Unit,
    onSendCustomEmoji: (CustomEmoji) -> Unit,
    onSendStickerPackEmoji: (packId: String, eId: String) -> Unit,
    onMoveFavoriteToTop: (CustomEmoji) -> Unit,
    onDeleteFavorite: (CustomEmoji) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // 底部入口：收藏 / 全部表情包（「最近」已按需求移除），默认「全部表情包」。
    var bottomTab by rememberSaveable { mutableStateOf(STICKER_TAB_FAVORITES) }
    // 包详情：rememberSaveable 记录当前打开的 packId/包名；切底部 tab / 面板重建时重置。
    var selectedPackId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPackName by rememberSaveable { mutableStateOf("") }
    var packRetryTick by remember { mutableIntStateOf(0) }
    val packDetailState = rememberStickerPackEmojis(selectedPackId, packRetryTick)
    LaunchedEffect(Unit) { selectedPackId = null }
    // 切到收藏/全部表情包 tab 时重新刷新（打开面板时父级已触发一次）并退出包详情。
    LaunchedEffect(bottomTab) {
        selectedPackId = null
        onRefresh()
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedPackId != null) {
            StickerPackDetailContent(
                packId = selectedPackId.orEmpty(),
                packName = selectedPackName,
                state = packDetailState,
                onBack = { selectedPackId = null },
                onRetry = { packRetryTick += 1 },
                onSendSticker = onSendStickerPackEmoji,
                modifier = Modifier.weight(1f),
            )
        } else if (bottomTab == STICKER_TAB_FAVORITES) {
            // 收藏：用户自己「添加到表情」收集的单张表情图片；点击直接以表情形态发送。
            FavoriteEmojiGrid(
                items = favorites,
                loading = favoritesLoading,
                onEmojiClick = onSendCustomEmoji,
                onMoveToTop = onMoveFavoriteToTop,
                onDelete = onDeleteFavorite,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            StickerSearchFieldMaterial(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (packs.isEmpty() && packsLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        StickerEmptyText(text = stringResource(R.string.loading))
                    }
                } else if (packs.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        StickerEmptyText()
                    }
                } else {
                    items(count = packs.size, key = { index -> packs[index].id }) { index ->
                        StickerPackCardMaterial(
                            pack = packs[index],
                            onClick = {
                                selectedPackId = packs[index].id
                                selectedPackName = packs[index].name
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        EmojiBottomRowMaterial(
            selectedTab = bottomTab,
            onSelect = { bottomTab = it },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

/** 收藏表情页：用户「添加到表情」收集的表情图片网格（M3E / Miuix 共用）。 */
@Composable
private fun FavoriteEmojiGrid(
    items: List<CustomEmoji>,
    onEmojiClick: (CustomEmoji) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    loading: Boolean = false,
    onMoveToTop: ((CustomEmoji) -> Unit)? = null,
    onDelete: ((CustomEmoji) -> Unit)? = null,
) {
    // 仅收藏 tab 传入管理回调：长按弹出「移动到首位 / 删除」菜单（包详情不传 → 无菜单）。
    val moveToTopAction = onMoveToTop
    val deleteAction = onDelete
    val canManage = moveToTopAction != null && deleteAction != null
    var menuTarget by remember { mutableStateOf<CustomEmoji?>(null) }
    val haptic = LocalHapticFeedback.current
    // 滚动中暂停动图：收藏网格可能同时存在多张 GIF，滚动时解码/绘制会叠加卡顿。
    val gridState = rememberLazyGridState()
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(if (loading) R.string.loading else R.string.emoji_favorites_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(count = items.size, key = { index -> items[index].id }) { index ->
            val emoji = items[index]
            val imagePath = emoji.imagePath
            // 1x1 morph 锚点：GlassTransformPopup 从手指长按点展开、预测性返回跟手收缩回该点。
            val cellAnchor = rememberGlassPopupAnchor()
            // 长按点（相对表情格左上角）：1dp 透明锚点 Box 放在这里，作为 morph 展开/收缩的中心。
            var pressInCell by remember(emoji.id) { mutableStateOf(Offset.Zero) }
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .pointerInput(emoji.id) {
                        detectTapGestures(
                            onTap = { onEmojiClick(emoji) },
                            onLongPress = { offset ->
                                // 仅收藏 tab（canManage）长按弹菜单；长按不触发发送。
                                if (canManage) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // 锚点 = 表情格 + 手指长按点（GlassPopup 在手指旁侧展开）。
                                    pressInCell = offset
                                    menuTarget = emoji
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                // 1dp 透明锚点：放在手指长按位置（窗口矩形≈1x1），菜单从该点展开/收缩；
                // cornerRadius 与展开面板一致（24dp，start==end）→ 不会出现负圆角。
                if (canManage) {
                    Box(
                        modifier = Modifier
                            // 单元格 Box 的 contentAlignment 是 Center，锚点必须先归到左上角，
                            // 再按长按点偏移；否则会以中心为原点导致菜单出现在错误位置。
                            .align(Alignment.TopStart)
                            .offset {
                                IntOffset(pressInCell.x.roundToInt(), pressInCell.y.roundToInt())
                            }
                            .size(1.dp)
                            .glassPopupAnchor(
                                anchor = cellAnchor,
                                cornerRadius = 24.dp,
                                floating = true,
                            ),
                    )
                }
                if (!imagePath.isNullOrBlank()) {
                    // 动图（GIF/APNG/动态 WebP）走 AnimatedOrStaticImage 循环播放；静态图自动回退。
                    AnimatedOrStaticImage(
                        path = imagePath,
                        contentDescription = emoji.desc.ifBlank { null },
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                    )
                } else {
                    Text(
                        text = emoji.desc.ifBlank { "🙂" },
                        fontSize = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (canManage && menuTarget?.id == emoji.id) {
                    when (LocalUiMode.current) {
                        UiMode.Material -> FavoriteEmojiMenuMaterial(
                            onDismiss = { menuTarget = null },
                            onMoveToTop = {
                                menuTarget = null
                                moveToTopAction.invoke(emoji)
                            },
                            onDelete = {
                                menuTarget = null
                                deleteAction.invoke(emoji)
                            },
                        )
                        UiMode.Miuix -> FavoriteEmojiMenuMiuix(
                            anchor = cellAnchor,
                            onDismiss = { menuTarget = null },
                            onMoveToTop = {
                                menuTarget = null
                                moveToTopAction.invoke(emoji)
                            },
                            onDelete = {
                                menuTarget = null
                                deleteAction.invoke(emoji)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** M3E 收藏表情长按菜单：复用聊天页气泡菜单的 Expressive 下拉样式（图标 + 文字、圆角）。 */
@Composable
private fun FavoriteEmojiMenuMaterial(
    onDismiss: () -> Unit,
    onMoveToTop: () -> Unit,
    onDelete: () -> Unit,
) {
    OffsetAnchoredExpressiveMenu(
        expanded = true,
        onDismissRequest = onDismiss,
    ) {
        SelectableDropdownMenuItem(
            text = { Text(stringResource(R.string.emoji_favorite_move_top)) },
            selected = false,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.VerticalAlignTop,
                    contentDescription = null,
                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                )
            },
            shapes = MenuDefaults.itemShape(index = 0, count = 2),
            onClick = onMoveToTop,
        )
        SelectableDropdownMenuItem(
            text = { Text(stringResource(R.string.delete)) },
            selected = false,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                )
            },
            shapes = MenuDefaults.itemShape(index = 1, count = 2),
            onClick = onDelete,
        )
    }
}

/**
 * Miuix 收藏表情长按菜单：锚点 = 表情格内 1dp 透明 Box（跟随手指长按点），
 * `GlassTransformPopup` 从手指点展开/预测性返回跟手收缩；「删除」用统一危险红。
 */
@Composable
private fun FavoriteEmojiMenuMiuix(
    anchor: GlassPopupAnchor,
    onDismiss: () -> Unit,
    onMoveToTop: () -> Unit,
    onDelete: () -> Unit,
) {
    val glassMenu = LocalGlassMenu.current
    // 页面采样只能在页面 CompositionLocal 作用域内读，这里读到后交给根级状态保存。
    val pageBackdrop = LocalPageBackdrop.current
    // 菜单发出方 token：表情单元格被移出组合（如滚动/删除）时关闭残留菜单。
    val menuOwner = remember { Any() }
    DisposableEffect(menuOwner) {
        onDispose { glassMenu?.hideIfOwner(menuOwner) }
    }
    val entries = listOf(
        GlassMenuEntry(
            label = stringResource(R.string.emoji_favorite_move_top),
            icon = Icons.Filled.VerticalAlignTop,
            onClick = onMoveToTop,
        ),
        GlassMenuEntry(
            label = stringResource(R.string.delete),
            icon = Icons.Filled.Delete,
            isDanger = true,
            onClick = onDelete,
        ),
    )
    // 本 composable 只在 menuTarget 命中时进入组合：先等一帧让 1dp 锚点 Box 移动到本次长按点，
    // 再交给页面级宿主用 GlassTransformPopup 从手指点展开（预测性返回跟手收缩回手指点）。
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

@Composable
private fun StickerEmptyText(text: String = stringResource(R.string.chat_v2_sticker_empty)) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StickerSearchFieldMaterial(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(50),
        color = colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colorScheme.onSurface),
                singleLine = true,
                cursorBrush = SolidColor(colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.emoji_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                },
            )
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.chat_v2_clear_search),
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerPackCardMaterial(pack: StickerPack, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val coverPath = pack.coverPath
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp),
        shape = RoundedCornerShape(18.dp),
        color = colorScheme.surfaceContainerHigh,
        contentColor = colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (!coverPath.isNullOrBlank()) {
                    // 封面可能是动图：走 AnimatedOrStaticImage，静态封面自动回退。
                    AnimatedOrStaticImage(
                        path = coverPath,
                        contentDescription = pack.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    // 无本地封面：回退代表性 emoji / 包名首字占位（不空白）。
                    Text(
                        text = pack.previewEmoji.ifBlank { pack.name.take(1) },
                        fontSize = 38.sp,
                        maxLines = 1,
                    )
                }
            }
            Text(
                text = pack.name,
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmojiBottomRowMaterial(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 与顶部「表情 / 表情包」一致：Material 3 Expressive Connected Button Group，
    // 两段共用外轮廓、2dp 间隙，首/尾为外侧大圆角。
    val entries = listOf(
        STICKER_TAB_FAVORITES to R.string.emoji_favorites,
        STICKER_TAB_ALL to R.string.emoji_all_packs,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        entries.forEachIndexed { index, (tabId, labelRes) ->
            val isSelected = selectedTab == tabId
            ExpressiveToggleButton(
                checked = isSelected,
                onCheckedChange = { checked -> if (checked) onSelect(tabId) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun ChatEmojiPanelMiuix(
    composer: ChatComposerState,
    onSendCustomEmoji: (CustomEmoji) -> Unit = {},
    onSendStickerPackEmoji: (packId: String, eId: String) -> Unit = { _, _ -> },
    onSendSuperEmoji: (SuperEmojiItem) -> Unit = {},
    onSendSystemFace: (SystemFaceItem) -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(ChatEmojiPanelTab.Stickers) }
    val tabs = ChatEmojiPanelTab.entries.map { stringResource(it.labelRes) }
    val emojiData = rememberEmojiPanelData()
    LaunchedEffect(Unit) { emojiData.refresh() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 收藏表情管理：移到首位 / 删除（IO 调 bridge，成功后刷新收藏列表）。
    val onMoveFavoriteToTop: (CustomEmoji) -> Unit = { emoji ->
        scope.launch {
            val source = AppDataSources.emojiSource
            val ok = if (source == null) {
                false
            } else {
                withContext(Dispatchers.IO) {
                    runCatching { source.moveFavoriteEmojiToTop(emoji.id) }.getOrDefault(false)
                }
            }
            Toast.makeText(
                context,
                if (ok) R.string.emoji_favorite_moved_top else R.string.emoji_favorite_operation_failed,
                Toast.LENGTH_SHORT,
            ).show()
            if (ok) emojiData.refresh()
        }
    }
    val onDeleteFavorite: (CustomEmoji) -> Unit = { emoji ->
        scope.launch {
            val source = AppDataSources.emojiSource
            val ok = if (source == null) {
                false
            } else {
                withContext(Dispatchers.IO) {
                    runCatching { source.deleteFavoriteEmoji(emoji.id) }.getOrDefault(false)
                }
            }
            Toast.makeText(
                context,
                if (ok) R.string.emoji_favorite_deleted else R.string.emoji_favorite_operation_failed,
                Toast.LENGTH_SHORT,
            ).show()
            if (ok) emojiData.refresh()
        }
    }

    // Phase 2f：面板内玻璃 Tab 的独立采样源。面板是不透明圆角底且位于聊天页的录制层内，
    // 直接采样页面 backdrop 会把 Tab 自身（上一帧）录进去形成反馈叠亮；这里只录制面板底色。
    val panelBackdrop = rememberPageBackdrop(LocalEnableBlur.current)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(ChatEmojiPanelHeight)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(colorScheme.surface),
    ) {
        // 采样源只录制不透明面板底色（不含 Tab 自身），玻璃 Tab 采样时不会采到自己。
        if (panelBackdrop != null) {
            Box(Modifier.matchParentSize().layerBackdrop(panelBackdrop))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, bottom = 8.dp),
        ) {
            if (panelBackdrop != null) {
                // backdrop 可用（模糊开启 + 设备支持 runtime shader）→ miuix-glass 分段玻璃 Tab。
                GlassSegmentedTabRow(
                    tabs = tabs,
                    selectedIndex = tab.ordinal,
                    onSelect = { index ->
                        tab = ChatEmojiPanelTab.entries.getOrElse(index) { ChatEmojiPanelTab.Emoji }
                    },
                    backdrop = panelBackdrop,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            } else {
                // 回退：关闭模糊 / 设备不支持 runtime shader → 原 Miuix TabRow。
                TabRow(
                    tabs = tabs,
                    selectedTabIndex = tab.ordinal,
                    onTabSelected = { index ->
                        tab = ChatEmojiPanelTab.entries.getOrElse(index) { ChatEmojiPanelTab.Emoji }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            when (tab) {
                ChatEmojiPanelTab.Emoji -> EmojiGridMiuix(
                    onEmojiClick = composer::appendEmoji,
                    superEmojis = emojiData.superEmojis,
                    onSuperEmojiClick = onSendSuperEmoji,
                    systemFaces = emojiData.systemFaces,
                    onSystemFaceClick = onSendSystemFace,
                    modifier = Modifier.weight(1f),
                )

                ChatEmojiPanelTab.Stickers -> StickerTabMiuix(
                    allPacks = emojiData.packs,
                    favorites = emojiData.favorites,
                    favoritesLoading = emojiData.favoritesLoading,
                    packsLoading = emojiData.packsLoading,
                    onRefresh = emojiData.refresh,
                    onSendCustomEmoji = onSendCustomEmoji,
                    onSendStickerPackEmoji = onSendStickerPackEmoji,
                    onMoveFavoriteToTop = onMoveFavoriteToTop,
                    onDeleteFavorite = onDeleteFavorite,
                    backdrop = panelBackdrop,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StickerTabMiuix(
    allPacks: List<StickerPack>,
    favorites: List<CustomEmoji>,
    favoritesLoading: Boolean,
    packsLoading: Boolean,
    onRefresh: () -> Unit,
    onSendCustomEmoji: (CustomEmoji) -> Unit,
    onSendStickerPackEmoji: (packId: String, eId: String) -> Unit,
    onMoveFavoriteToTop: (CustomEmoji) -> Unit,
    onDeleteFavorite: (CustomEmoji) -> Unit,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    // 底部 TabRow：0 = 收藏，1 = 全部表情包（默认）。「最近」已按需求移除。
    var bottomTab by rememberSaveable { mutableStateOf(STICKER_TAB_FAVORITES) }
    // 包详情：rememberSaveable 记录当前打开的 packId/包名；切底部 tab / 面板重建时重置。
    var selectedPackId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPackName by rememberSaveable { mutableStateOf("") }
    var packRetryTick by remember { mutableIntStateOf(0) }
    val packDetailState = rememberStickerPackEmojis(selectedPackId, packRetryTick)
    LaunchedEffect(Unit) { selectedPackId = null }
    LaunchedEffect(bottomTab) {
        selectedPackId = null
        onRefresh()
    }
    val packs = remember(query, allPacks) { searchStickerPacks(query, allPacks) }
    val favoritesOnly = bottomTab == STICKER_TAB_FAVORITES
    val bottomLabels = listOf(
        stringResource(R.string.emoji_favorites),
        stringResource(R.string.emoji_all_packs),
    )
    Column(modifier = modifier.fillMaxWidth()) {
        if (selectedPackId != null) {
            StickerPackDetailContent(
                packId = selectedPackId.orEmpty(),
                packName = selectedPackName,
                state = packDetailState,
                onBack = { selectedPackId = null },
                onRetry = { packRetryTick += 1 },
                onSendSticker = onSendStickerPackEmoji,
                modifier = Modifier.weight(1f),
            )
        } else if (favoritesOnly) {
            // 收藏：用户自己「添加到表情」收集的单张表情图片；点击直接以表情形态发送。
            FavoriteEmojiGrid(
                items = favorites,
                loading = favoritesLoading,
                onEmojiClick = onSendCustomEmoji,
                onMoveToTop = onMoveFavoriteToTop,
                onDelete = onDeleteFavorite,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        } else {
            TextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.emoji_search_hint),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (packs.isEmpty() && packsLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            MiuixText(
                                text = stringResource(R.string.loading),
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                } else if (packs.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            MiuixText(
                                text = stringResource(R.string.chat_v2_sticker_empty),
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                } else {
                    items(count = packs.size, key = { index -> packs[index].id }) { index ->
                        val pack = packs[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                // 进入表情包详情：显示包内表情网格（不再 Toast「发送表情包」）。
                                selectedPackId = pack.id
                                selectedPackName = pack.name
                            },
                            showIndication = true,
                            insideMargin = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (!pack.coverPath.isNullOrBlank()) {
                                    // 封面可能是动图：走 AnimatedOrStaticImage，静态封面自动回退。
                                    AnimatedOrStaticImage(
                                        path = pack.coverPath,
                                        contentDescription = pack.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(44.dp),
                                    )
                                } else {
                                    // 无本地封面：回退代表性 emoji / 包名首字占位（不空白）。
                                    MiuixText(
                                        text = pack.previewEmoji.ifBlank { pack.name.take(1) },
                                        fontSize = 34.sp,
                                        maxLines = 1,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                MiuixText(
                                    text = pack.name,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        if (backdrop != null) {
            // 同上层 Tab：面板级玻璃分段 Tab（采样面板底色，不采样自身）。
            GlassSegmentedTabRow(
                tabs = bottomLabels,
                selectedIndex = bottomTab,
                onSelect = { bottomTab = it },
                backdrop = backdrop,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        } else {
            // 回退：关闭模糊 / 设备不支持 runtime shader → 原 Miuix TabRow。
            TabRow(
                tabs = bottomLabels,
                selectedTabIndex = bottomTab,
                onTabSelected = { bottomTab = it },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun EmojiGridMiuix(
    onEmojiClick: (String) -> Unit,
    superEmojis: List<SuperEmojiItem> = emptyList(),
    onSuperEmojiClick: (SuperEmojiItem) -> Unit = {},
    systemFaces: List<SystemFaceItem> = emptyList(),
    onSystemFaceClick: (SystemFaceItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(8),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            EmojiSectionTitleMiuix(stringResource(R.string.emoji_recent_title))
        }
        items(
            count = EmojiRepository.recentEmojis.size,
            key = { index -> "recent_$index" },
        ) { index ->
            EmojiCellMiuix(EmojiRepository.recentEmojis[index], onEmojiClick)
        }
        if (superEmojis.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmojiSectionTitleMiuix(
                    text = stringResource(R.string.emoji_super_title),
                    topPadding = 8.dp,
                )
            }
            items(
                count = superEmojis.size,
                key = { index -> "super_${superEmojis[index].serverId}" },
            ) { index ->
                SuperEmojiCellMiuix(
                    item = superEmojis[index],
                    onClick = { onSuperEmojiClick(superEmojis[index]) },
                )
            }
        }
        if (systemFaces.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmojiSectionTitleMiuix(
                    text = stringResource(R.string.emoji_system_face_title),
                    topPadding = 8.dp,
                )
            }
            items(
                count = systemFaces.size,
                key = { index -> "sysface_${systemFaces[index].serverId}" },
            ) { index ->
                SystemFaceCellMiuix(
                    item = systemFaces[index],
                    onClick = { onSystemFaceClick(systemFaces[index]) },
                )
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            EmojiSectionTitleMiuix(
                text = stringResource(R.string.chat_v2_all_emoji_title),
                topPadding = 8.dp,
            )
        }
        items(
            count = EmojiRepository.emojis.size,
            key = { index -> "all_$index" },
        ) { index ->
            EmojiCellMiuix(EmojiRepository.emojis[index], onEmojiClick)
        }
    }
}

/** 表情页「小黄脸」Miuix 单元格。 */
@Composable
private fun SystemFaceCellMiuix(item: SystemFaceItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedOrStaticImage(
            path = item.imagePath,
            contentDescription = item.desc,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(32.dp),
        )
    }
}

/** 表情页「超级表情」Miuix 单元格（静态首帧）。 */
@Composable
private fun SuperEmojiCellMiuix(item: SuperEmojiItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SuperEmojiAnimation(
            lottiePath = item.lottiePath,
            imagePath = item.imagePath,
            contentDescription = item.desc,
            fallbackText = item.desc.take(1).ifBlank { "表" },
            modifier = Modifier.size(34.dp),
            animate = false,
        )
    }
}

@Composable
private fun EmojiSectionTitleMiuix(text: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    MiuixText(
        text = text,
        fontSize = 12.sp,
        color = colorScheme.onSurfaceVariantSummary,
        maxLines = 1,
        modifier = Modifier.padding(
            start = 4.dp,
            top = 4.dp + topPadding,
            bottom = 2.dp,
        ),
    )
}

@Composable
private fun EmojiCellMiuix(emoji: String, onEmojiClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onEmojiClick(emoji) },
        contentAlignment = Alignment.Center,
    ) {
        MiuixText(text = emoji, fontSize = 24.sp, maxLines = 1)
    }
}

@Composable
private fun GifPlaceholderMiuix(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MiuixIcon(
                imageVector = Icons.Filled.GifBox,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(10.dp))
            MiuixText(
                text = stringResource(R.string.chat_v2_gif_placeholder),
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
