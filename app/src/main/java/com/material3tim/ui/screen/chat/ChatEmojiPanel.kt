package com.material3tim.ui.screen.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.CustomEmoji
import com.material3tim.data.model.StickerPack
import com.material3tim.data.source.AppDataSources
import com.material3tim.telegram.StickerLibrary
import com.material3tim.telegram.TelegramLibraries
import com.material3tim.telegram.prepareStickerForTim
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.component.AnimatedOrStaticImage
import com.material3tim.ui.component.material.OffsetAnchoredExpressiveMenu
import com.material3tim.ui.component.miuix.glass.GlassMenuEntry
import com.material3tim.ui.component.miuix.glass.LocalGlassMenu
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.screen.settings.rememberStickerAccount
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

private val ChatEmojiPanelHeight = 320.dp

@Composable
internal fun ChatStickerPanel(
    onSendCustomEmoji: (CustomEmoji) -> Unit,
    onSendStickerPackEmoji: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val account = rememberStickerAccount()
    val source = AppDataSources.emojiSource
    val material = LocalUiMode.current == UiMode.Material
    val background = if (material) MaterialTheme.colorScheme.surfaceContainerLow else colorScheme.surface
    val tint = if (material) MaterialTheme.colorScheme.onSurface else colorScheme.onSurface
    val active = if (material) MaterialTheme.colorScheme.secondaryContainer else colorScheme.secondaryContainer
    val scope = rememberCoroutineScope()
    var favorites by remember(account) { mutableStateOf(emptyList<CustomEmoji>()) }
    var nativePacks by remember(account) { mutableStateOf(emptyList<StickerPack>()) }
    var library by remember(account) { mutableStateOf<StickerLibrary?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var loading by remember(account) { mutableStateOf(true) }
    var failed by remember(account) { mutableStateOf(false) }
    var selected by rememberSaveable(account) { mutableStateOf<String?>(null) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var preparing by remember { mutableStateOf(false) }

    LaunchedEffect(account, source, revision) {
        loading = true
        failed = false
        if (account != null) {
            try {
                library = withContext(Dispatchers.IO) { TelegramLibraries.load(TelegramLibraries.root(context, account)) }
            } catch (e: java.io.IOException) { failed = true }
            catch (e: IllegalArgumentException) { failed = true }
        }
        if (source != null) {
            val (f, p) = withContext(Dispatchers.IO) {
                runCatching { source.refreshFavoriteEmojis() ?: source.loadFavoriteEmojis() }.getOrNull() to
                    runCatching { source.loadStickerPacks() }.getOrNull()
            }
            if (f != null) favorites = f
            if (p != null) nativePacks = p
            if (f == null || p == null) failed = true
        }
        loading = false
    }
    val packs = remember(nativePacks, library) { nativePacks + library?.covers().orEmpty() }
    LaunchedEffect(packs, loading) {
        if (!loading && selected != null && packs.none { it.id == selected }) selected = null
    }
    val pack = packs.firstOrNull { it.id == selected }
    val telegram = remember(pack, library) { library?.packs?.firstOrNull { "tg:${it.id}" == pack?.id } }
    var contents by remember(pack?.id, library) { mutableStateOf<List<CustomEmoji>?>(null) }
    var packFailed by remember(pack?.id) { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(pack, telegram, library, retry) {
        packFailed = false
        if (pack != null) {
            val items = if (telegram != null) library?.cells(telegram) else withContext(Dispatchers.IO) {
                runCatching { source?.loadStickerPackEmojis(pack.id) }.getOrNull()
            }
            contents = items
            packFailed = items == null
        }
    }
    fun manage(emoji: CustomEmoji, move: Boolean) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    if (move) source?.moveFavoriteEmojiToTop(emoji.id) else source?.deleteFavoriteEmoji(emoji.id)
                }.getOrNull() == true
            }
            val message = when {
                !ok -> R.string.emoji_favorite_operation_failed
                move -> R.string.emoji_favorite_moved_top
                else -> R.string.emoji_favorite_deleted
            }
            Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
            if (ok) revision++
        }
    }
    val select: (String?) -> Unit = { selected = it; searching = false; query = "" }
    BackHandler(enabled = searching || selected != null) {
        if (searching) { searching = false; query = "" } else select(null)
    }

    Column(Modifier.fillMaxWidth().padding(top = 8.dp).height(ChatEmojiPanelHeight)
        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        .background(background).padding(vertical = 6.dp)) {
        if (searching) {
            if (material) {
                androidx.compose.material3.OutlinedTextField(query, { query = it },
                    placeholder = { Text(stringResource(R.string.emoji_search_hint)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            } else {
                top.yukonga.miuix.kmp.basic.TextField(value = query, onValueChange = { query = it },
                    label = stringResource(R.string.emoji_search_hint), singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (pack == null && (!failed || favorites.isNotEmpty())) {
                FavoriteEmojiGrid(favorites, onSendCustomEmoji, Modifier.fillMaxSize(), loading = loading,
                    onMoveToTop = { manage(it, true) }, onDelete = { manage(it, false) })
            } else if (packFailed || (pack == null && failed)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.emoji_pack_load_failed), color = tint)
                    TextButton(onClick = { if (pack == null) revision++ else retry++ }) {
                        Text(stringResource(R.string.group_files_retry))
                    }
                }
            } else if (pack != null) {
                val emojis = contents
                if (emojis.isNullOrEmpty()) {
                    Text(stringResource(if (emojis == null) R.string.loading else R.string.emoji_pack_empty), color = tint)
                } else key(pack.id) {
                    FavoriteEmojiGrid(emojis, { emoji ->
                        if (telegram == null) onSendStickerPackEmoji(pack.id, emoji.id)
                        else if (!preparing) {
                            val sticker = telegram.stickers.first { it.id == emoji.id }
                            val folder = checkNotNull(library).folder
                            preparing = true
                            scope.launch {
                                try {
                                    val path = withContext(Dispatchers.IO) { prepareStickerForTim(folder, sticker) }
                                    onSendCustomEmoji(emoji.copy(imagePath = path))
                                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                catch (_: Exception) {
                                    Toast.makeText(context, context.getString(R.string.emoji_sticker_send_failed), Toast.LENGTH_SHORT).show()
                                } finally { preparing = false }
                            }
                        }
                    }, Modifier.fillMaxSize())
                }
            }
        }
        if (preparing) Text(stringResource(R.string.tg_converting), color = tint, modifier = Modifier.padding(start = 12.dp))
        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            item(key = "search") {
                IconButton(onClick = { searching = !searching; query = "" }) {
                    Icon(Icons.Filled.Search, stringResource(R.string.emoji_search_hint), tint = tint)
                }
            }
            item(key = "favorites") {
                PackButton(stringResource(R.string.emoji_favorites), null, "★", selected == null, tint, active) { select(null) }
            }
            items(packs.filter { it.name.contains(query.trim(), ignoreCase = true) }, key = { it.id }) { item ->
                PackButton(item.name, item.coverPath, item.previewEmoji.ifBlank { item.name.take(1) },
                    item.id == selected, tint, active) { select(item.id) }
            }
        }
    }
}

@Composable
private fun PackButton(name: String, cover: String?, fallback: String, selected: Boolean,
    tint: Color, active: Color, onClick: () -> Unit) {
    Column(Modifier.width(72.dp).clip(RoundedCornerShape(12.dp))
        .background(if (selected) active else Color.Transparent)
        .selectable(selected = selected, role = Role.Tab, onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            if (cover != null) AnimatedOrStaticImage(path = cover, contentDescription = null,
                contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            else Text(fallback, color = tint, fontSize = 20.sp, maxLines = 1)
        }
        Text(name, color = tint, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

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
