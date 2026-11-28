package com.material3tim.ui.screen.chat

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.DisposableEffect
import com.material3tim.ui.component.rememberCachedLocalImage
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.layout
import androidx.compose.runtime.State
import android.util.Log
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.layout.offset
import java.io.File

import com.material3tim.ui.component.AnimatedOrStaticImage
import com.material3tim.ui.component.rememberLocalImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.data.model.ChatItem
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.repository.LiveProfile
import com.material3tim.data.source.AppDataSources
import com.material3tim.data.model.ChatMember
import com.material3tim.data.model.CustomEmoji
import com.material3tim.data.source.AtTarget
import com.material3tim.data.model.ChatMessageType
import com.material3tim.data.model.ChatPartKind
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.ime.ChatImeController
import com.material3tim.ui.component.ime.rememberKeyboardHeightDp
import com.material3tim.ui.component.MiuixProgressBar
import com.material3tim.ui.component.DraggableMiuixProgressBar
import com.material3tim.ui.component.VoicePlaybackController
import com.material3tim.ui.theme.LocalEnableBlur
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.GlassMenuHost
import com.material3tim.ui.component.miuix.glass.GlassMenuState
import com.material3tim.ui.component.miuix.glass.LocalGlassMenu
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.HyperTopBarIconButton
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.PullToRefresh
import com.material3tim.ui.component.SuperEmojiAnimation
import com.material3tim.ui.component.DataLineAvatar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Mic
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Phone
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 聊天详情页（Miuix 风格）。
 *
 * 与 Material 版本结构一致：Miuix Scaffold + HyperTopAppBar（玻璃顶栏，标题 + 副标题），
 * 底部锚定的反向消息列表（时间分隔 / 系统消息 / 六种气泡，分组圆角与 Material 版一致），
 * 输入栏 + 上方表情面板 / 下方附件面板（加号面板不加额外背景容器）；输入法弹出时整体上移。
 */
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun ChatDetailMiuix(
    chat: ChatItem,
    messages: List<ChatMessage>,
    composer: ChatComposerState,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
    onSend: (String) -> Unit,
    replyTarget: ChatMessage? = null,
    onReplyRequest: (ChatMessage) -> Unit = {},
    onClearReply: () -> Unit = {},
    onDeleteMessage: (ChatMessage) -> Unit = {},
    onRecallMessage: (ChatMessage) -> Unit = {},
    onFileClick: (ChatMessage) -> Unit = {},
    downloadingFileMsgId: Long = 0L,
    fileDownloadProgress: Int = -1,
    atMembers: List<ChatMember> = emptyList(),
    atMembersLoading: Boolean = false,
    atMembersFailed: Boolean = false,
    onAtMembersRequested: () -> Unit = {},
    atSheetVisible: Boolean = false,
    onAtConfirm: (List<AtTarget>) -> Unit = {},
    onAtDismiss: () -> Unit = {},
    canRecallOthers: Boolean = false,
    onReplyQuoteClick: (ChatMessage) -> Unit = {},
    selectionMode: Boolean = false,
    selectedMessageIds: Set<Long> = emptySet(),
    onToggleSelectMessage: (ChatMessage) -> Unit = {},
    onEnterSelection: (ChatMessage) -> Unit = {},
    onExitSelection: () -> Unit = {},
    onForwardSelected: () -> Unit = {},
    onForwardMessage: (ChatMessage) -> Unit = {},
    onPlusOne: (ChatMessage) -> Unit = {},
    plusOneMessageIds: Set<Long> = emptySet(),
    jumpTarget: ChatMessage? = null,
    onJumpConsumed: () -> Unit = {},
    onLoadOlder: () -> Unit = {},
    isLoadingOlder: Boolean = false,
    hasMoreOlder: Boolean = true,
    onAttachAction: (ChatAttachAction) -> Unit = {},
    onCameraAction: (Boolean) -> Unit = {},
    onSendCustomEmoji: (CustomEmoji) -> Unit = {},
    onSendStickerPackEmoji: (packId: String, eId: String) -> Unit = { _, _ -> },
    onAvatarClick: (ChatMessage) -> Unit = {},
    onArkGroupNoticeClick: (ChatMessage) -> Unit = {},
    onAtSender: (ChatMessage) -> Unit = {},
    onAddToFavorite: (ChatMessage) -> Unit = {},
    onStartVoiceRecord: () -> Unit = {},
    initialMessagesLoaded: Boolean = true,
    imeController: ChatImeController,
) {
    val context = LocalContext.current
    val enableBlur = LocalEnableBlur.current
    // Phase 2b：页面级 Glass 采样 backdrop（关闭模糊/无 runtime shader 时为 null → 纯色回退）。
    val backdrop = rememberPageBackdrop(enableBlur)
    // 页面内容层长按菜单宿主状态：host 在 NavDisplay 子树内，返回手势优先关菜单。
    val glassMenu = remember { GlassMenuState() }
    // 聊天页顶栏始终是钉住的小标题（不展开大标题），scrollBehavior 只用于玻璃栏的滚动态样式。
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val groupedMessages = remember(messages) { groupChatMessages(messages) }
    // 群聊里自己消息上方也要显示昵称：优先群成员列表里的「我」的群名片。
    val selfGroupName = remember(atMembers) {
        atMembers.firstOrNull { it.isMe }?.name?.takeIf { it.isNotBlank() }
    }
    val hasEmojiSource = remember { AppDataSources.emojiSource != null }
    // 右下角浮动按钮（@我 / 回到最底部）：输入栏 + 面板整体高度（用于让按钮贴输入栏上方）。
    var composerBarHeightPx by remember { mutableIntStateOf(0) }
    var canScrollForward by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.canScrollForward }.collect { canScrollForward = it }
    }
    // @我：只遍历「未读窗口」内的 @我 消息，从新到旧；遍历完自动隐藏。
    // 进入时快照未读数（进入聊天后会被内核标记已读，之后 chat.unreadCount 不可靠）。
    val unreadCountSnapshot = remember(chat.id) { chat.unreadCount }
    val unreadAtMeIndices = remember(groupedMessages, unreadCountSnapshot) {
        val unreadWindow = unreadCountSnapshot.coerceIn(0, groupedMessages.size)
        val start = (groupedMessages.size - unreadWindow).coerceAtLeast(0)
        (start until groupedMessages.size).filter { groupedMessages[it].message.atMe }
    }
    // atMeCursor：下一次跳转距未读 @我 列表末尾的偏移（0 = 最新一条）；每次跳转 +1。
    var atMeCursor by remember(chat.id) { mutableIntStateOf(0) }
    val hasUnreadAtMe = unreadAtMeIndices.isNotEmpty() && atMeCursor < unreadAtMeIndices.size
    // 回到最底部按钮：手势驱动显隐（下滑累计 ≥ 一行气泡高度出现；上滑立即隐藏；底部始终隐藏）。
    val backToBottomThresholdPx = with(LocalDensity.current) { 56.dp.roundToPx() }
    var backToBottomGestureVisible by remember { mutableStateOf(false) }
    var downAccumulatedPx by remember { mutableFloatStateOf(0f) }
    val fabNestedScrollConnection = remember(backToBottomThresholdPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (consumed.y > 0f) {
                    // 向下拖动内容（看更早消息）：累计超过一行气泡高度后显示回底按钮。
                    downAccumulatedPx += consumed.y
                    if (downAccumulatedPx >= backToBottomThresholdPx) {
                        backToBottomGestureVisible = true
                    }
                } else if (consumed.y < 0f) {
                    // 向上拖动内容（看更新消息）：立即隐藏（动画负责过渡）。
                    downAccumulatedPx = 0f
                    backToBottomGestureVisible = false
                }
                return Offset.Zero
            }
        }
    }
    // 回到底部（无法继续向前滚动）后隐藏按钮。
    LaunchedEffect(canScrollForward) {
        if (!canScrollForward) {
            backToBottomGestureVisible = false
            downAccumulatedPx = 0f
        }
    }
    val fabScope = rememberCoroutineScope()

    // 非 reverseLayout 列表的滚动锚定：进入时到底部、有新消息且原本在底部时跟随；
    // 加载更早消息时的视口保持由下面的 pendingOlderAnchor 逻辑处理。
    var scrollAnchor by remember(chat.id) {
        mutableStateOf(ChatScrollAnchor(count = -1, lastId = null, atBottom = true))
    }
    val latestMessagesForRefresh = androidx.compose.runtime.rememberUpdatedState(messages)
    val latestLoadingForRefresh = androidx.compose.runtime.rememberUpdatedState(isLoadingOlder)

    // 下拉加载更早消息前记录的视口锚点（第一条可见消息 id + offset）；加载完成后
    // 延迟按 id 找回位置，避免被 PullToRefresh 收回下拉动画时的滚动抵消。
    var pendingOlderAnchorId by remember(chat.id) { mutableStateOf<String?>(null) }
    var pendingOlderOffset by remember(chat.id) { mutableStateOf(0) }
    var pendingOlderCount by remember(chat.id) { mutableStateOf(0) }
    // 需要延迟兜底校正的目标（index + offset）
    var olderCorrectionTarget by remember(chat.id) { mutableStateOf<Pair<Int, Int>?>(null) }

    // 在组合阶段就算出恢复目标，并用 SideEffect 在「布局之前」请求滚动（requestScrollToItem），
    // 这样不会出现「先闪到列表顶部、再被拉回原位」的中间帧（Miuix 下拉刷新尤其明显）。
    val olderRestoreTarget = remember(messages, pendingOlderAnchorId) {
        val anchorId = pendingOlderAnchorId
        if (anchorId.isNullOrBlank() || messages.size <= pendingOlderCount) {
            null
        } else {
            messages.indexOfFirst { it.id == anchorId }
                .takeIf { it >= 0 }
                ?.let { it to pendingOlderOffset }
        }
    }
    LaunchedEffect(pendingOlderAnchorId) {
        if (pendingOlderAnchorId != null) {
            kotlinx.coroutines.delay(4_000)
            pendingOlderAnchorId = null
        }
    }
    // 首次拿到消息时，在「布局之前」请求滚动位置：否则 LazyColumn 会先按 index 0 渲染一帧
    // （打开聊天时先看到列表顶部），下一帧才跳过去，视觉上会闪一下。
    // requestScrollToItem 在 SideEffect（measure 之前）调用，可让第一帧就落在目标位置。
    var initialScrollRequested by remember(chat.id) { mutableStateOf(false) }
    // 从图片/视频查看器返回时 ChatDetail 组合可能被重建：用可保存的锚点（首条可见 index+offset）
    // 恢复原滚动位置，避免再次执行「首次进入滚到底部」把位置丢失。
    var savedAnchorIndex by rememberSaveable(chat.id) { mutableIntStateOf(-1) }
    var savedAnchorOffset by rememberSaveable(chat.id) { mutableIntStateOf(0) }
    var savedAnchorValid by rememberSaveable(chat.id) { mutableStateOf(false) }
    // 锚点所属会话：saveable 状态可能被其它 ChatDetail 实例/旧会话复用，必须校验 chatId。
    var savedAnchorChatId by rememberSaveable(chat.id) { mutableStateOf<String?>(null) }
    val savedAnchorMatchesChat = savedAnchorValid &&
        savedAnchorChatId == chat.id &&
        savedAnchorIndex >= 0
    // 持续记录首条可见 item（首帧定位完成后才记录，避免把未定位时的 0 当作锚点）。
    LaunchedEffect(listState, chat.id) {
        snapshotFlow {
            if (!initialScrollRequested) {
                null
            } else {
                val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                if (first == null) null else first.index to listState.firstVisibleItemScrollOffset
            }
        }.collect { anchor ->
            if (anchor != null) {
                savedAnchorIndex = anchor.first
                savedAnchorOffset = anchor.second
                savedAnchorValid = true
                savedAnchorChatId = chat.id
            }
        }
    }
    androidx.compose.runtime.SideEffect {
        if (initialScrollRequested) return@SideEffect
        // 消息还没到时不要标记完成：否则消息到达后会先按 index 0 渲染一帧再跳到底部（闪一下顶部）。
        if (groupedMessages.isEmpty()) return@SideEffect
        if (savedAnchorMatchesChat) {
            // 恢复到上次首条可见消息（measure 前请求，第一帧就在原位置，不闪底部）。
            listState.requestScrollToItem(
                savedAnchorIndex.coerceIn(0, groupedMessages.lastIndex.coerceAtLeast(0)),
                savedAnchorOffset,
            )
        } else {
            // 无匹配锚点（真正首次进入 / 锚点属于其它会话）：清除残留并滚到底部显示最新消息。
            if (savedAnchorValid) {
                savedAnchorValid = false
                savedAnchorIndex = -1
                savedAnchorOffset = 0
                savedAnchorChatId = null
            }
            listState.requestScrollToItem(groupedMessages.lastIndex)
        }
        initialScrollRequested = true
    }
    // 必要时第二帧校正一次（listState 重建后 requestScrollToItem 偶尔被测量覆盖），防闪。
    LaunchedEffect(chat.id) {
        // 锚点必须属于当前会话，否则不校正（避免把新会话强制拉回旧 index）。
        if (!savedAnchorMatchesChat || messages.isEmpty()) return@LaunchedEffect
        delay(24)
        if (listState.isScrollInProgress) return@LaunchedEffect
        val target = savedAnchorIndex.coerceIn(0, groupedMessages.lastIndex.coerceAtLeast(0))
        if (listState.firstVisibleItemIndex != target ||
            kotlin.math.abs(listState.firstVisibleItemScrollOffset - savedAnchorOffset) > 2
        ) {
            runCatching { listState.scrollToItem(target, savedAnchorOffset) }
        }
    }
    androidx.compose.runtime.SideEffect {
        val target = olderRestoreTarget ?: return@SideEffect
        listState.requestScrollToItem(
            target.first.coerceIn(0, messages.lastIndex.coerceAtLeast(0)),
            target.second,
        )
        pendingOlderAnchorId = null
        olderCorrectionTarget = target
    }
    LaunchedEffect(olderCorrectionTarget) {
        val target = olderCorrectionTarget ?: return@LaunchedEffect
        // 下拉刷新头部收回动画可能持续 1s 以上：每 100ms 检查一次位置，
        // 一旦发生漂移就拉回目标，连续 4 次稳定后结束（最多 2.5s）。
        var stable = 0
        repeat(25) {
            kotlinx.coroutines.delay(100)
            if (messages.isEmpty()) return@repeat
            val index = target.first.coerceIn(0, messages.lastIndex)
            val matched = listState.firstVisibleItemIndex == index &&
                listState.firstVisibleItemScrollOffset == target.second
            if (matched) {
                stable += 1
                if (stable >= 4) return@LaunchedEffect
            } else {
                stable = 0
                listState.scrollToItem(index, target.second)
            }
        }
        olderCorrectionTarget = null
    }
    LaunchedEffect(messages.size, messages.lastOrNull()?.id) {
        if (messages.isEmpty()) return@LaunchedEffect
        val prev = scrollAnchor
        val newLastId = messages.lastOrNull()?.id
        val atBottomNow = !listState.canScrollForward
        when {
            // 首次定位已在布局前由 requestScrollToItem 完成；这里仅作异常兜底。
            // 锚点 chatId 匹配时按锚点定位（查看器返回/组合重建）；
            // 否则（首次进入 / 串会话的旧锚点）滚到底部显示最新消息。
            prev.count < 0 -> if (savedAnchorMatchesChat) {
                listState.scrollToItem(
                    savedAnchorIndex.coerceIn(0, groupedMessages.lastIndex.coerceAtLeast(0)),
                    savedAnchorOffset,
                )
            } else {
                listState.scrollToItem(groupedMessages.lastIndex.coerceAtLeast(0), 0)
            }
            newLastId != prev.lastId -> {
                if (atBottomNow || prev.atBottom) listState.animateScrollToItem(messages.lastIndex)
            }
        }
        scrollAnchor = ChatScrollAnchor(
            count = messages.size,
            lastId = newLastId,
            atBottom = !listState.canScrollForward,
        )
    }
    // 键盘高度：宿主窗口 Compose ime insets 可能为 0，由 View 层检测兜底；仅用于布局平移，不影响 IME show/hide。
    val keyboardHeightState = rememberKeyboardHeightDp()
    val navigationBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 结构性优化：内容保持完整高度测量，键盘弹出时不再逐帧缩减测量高度，也不再逐帧 scrollBy/scrollToItem。
    // 键盘高度只在下方 Modifier.offset{}（placement 阶段）读取，整块内容（消息列表/输入栏/面板）随之上移，
    // 底部由平移跟随键盘；低端机键盘动画不再触发 LazyColumn 每帧重排。
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val comingSoon = remember(context) {
        {
            Toast.makeText(context, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show()
        }
    }
    // 引用跳转：目标已在列表里时滚动过去（msgId 或 seq 任一匹配）。
    LaunchedEffect(jumpTarget, groupedMessages) {
        val target = jumpTarget ?: return@LaunchedEffect
        val index = groupedMessages.indexOfFirst { item ->
            // 引用跳转：找「引用了目标消息」的那条；聊天记录搜索跳转：直接找目标消息本身。
            item.message.id == target.id ||
                (target.seq > 0L && item.message.seq == target.seq) ||
                (target.replyToMsgId != 0L && item.message.chatRecordMsgId == target.replyToMsgId) ||
                (target.replyToSeq > 0L && item.message.seq == target.replyToSeq)
        }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            onJumpConsumed()
        }
    }

    val chatInputFocusRequester = remember { FocusRequester() }
    // 打开面板前记录键盘是否展开：关闭面板时若之前是展开状态则恢复键盘，否则保持收起不动 IME。
    var imeWasVisibleBeforePanel by remember { mutableStateOf(false) }
    val onTogglePanel: (ChatPanel) -> Unit = { target ->
        val imeWasVisible = imeController.isImeVisibleNow()
        composer.dismissAtPicker()
        composer.togglePanel(target)
        if (composer.isPanelOpen) {
            imeWasVisibleBeforePanel = imeWasVisible
            focusManager.clearFocus()
            keyboardController?.hide()
            // keyboardController.hide() 在宿主窗口里可能不生效，显式走 IMM 收起键盘。
            imeController.hideIme("panel")
        } else if (imeWasVisibleBeforePanel) {
            // 关闭面板且打开前键盘是展开的：重新聚焦输入框并弹回键盘。
            imeWasVisibleBeforePanel = false
            runCatching { chatInputFocusRequester.requestFocus() }
            imeController.requestShow("panel-closed")
        }
    }
    val onPanelAction: (ChatPanel) -> Unit = { target ->
        imeWasVisibleBeforePanel = imeController.isImeVisibleNow()
        composer.dismissAtPicker()
        composer.showPanel(target)
        focusManager.clearFocus()
        keyboardController?.hide()
        imeController.hideIme("panel")
    }
    val onInputFocused: () -> Unit = {
        imeWasVisibleBeforePanel = false
        composer.closePanels()
        composer.dismissAtPicker()
    }
    // 系统返回键/返回手势：有内联面板打开时先关闭面板（不 pop 导航）；无面板时 enabled=false
    // 交给系统/导航默认返回。@ 成员 Sheet 是最上层（ModalBottomSheet/Overlay 自身也会消费返回），
    // 若事件到达页面则优先关闭它，再关表情/附件面板。
    val hasBackDismissablePanel = atSheetVisible || composer.isPanelOpen
    BackHandler(enabled = hasBackDismissablePanel) {
        when {
            atSheetVisible -> composer.dismissAtPicker()
            composer.isPanelOpen -> composer.closePanels()
            else -> Unit
        }
    }

    val selectionState = MessageSelectionState(
        active = selectionMode,
        selectedIds = selectedMessageIds,
        onToggle = onToggleSelectMessage,
    )
    // @ 成员选择底部 Sheet：自带 overlay，不占聊天页布局。
    LaunchedEffect(atSheetVisible) {
        // 面板打开且成员为空：按需强制加载群成员（带 loading 态，避免空列表）。
        if (atSheetVisible && atMembers.isEmpty() && !atMembersLoading) {
            onAtMembersRequested()
        }
    }
    ChatAtSheetMiuix(
        visible = atSheetVisible,
        members = atMembers,
        onDismiss = onAtDismiss,
        onConfirm = onAtConfirm,
        loading = atMembersLoading,
        failed = atMembersFailed,
        onRetry = onAtMembersRequested,
    )

    // 气泡/表情/附件长按菜单：LocalGlassMenu 下发给页面内触发方；host 放在页面 Box 末尾，
    // 位于 NavDisplay 内容层（返回处理器注册在页面处理器之后 → 返回先关菜单再退出页面）。
    CompositionLocalProvider(LocalGlassMenu provides glassMenu) {
    Box(modifier = Modifier.fillMaxSize()) {
    CompositionLocalProvider(
        LocalMessageMenuAction provides { action, menuMessage ->
            if (action == MessageMenuAction.ADD_TO_FAVORITE) {
                // 添加到表情：IO 由页面处理并 Toast 反馈。
                onAddToFavorite(menuMessage)
            } else if (action == MessageMenuAction.AT_SENDER) {
                // at对方：插入 @ 文本后聚焦输入框并弹出键盘。
                onAtSender(menuMessage)
                runCatching { chatInputFocusRequester.requestFocus() }
                imeController.requestShow("at-sender")
            } else {
            handleMessageMenuAction(
                context = context,
                action = action,
                message = menuMessage,
                onReply = onReplyRequest,
                onDelete = onDeleteMessage,
                onRecall = onRecallMessage,
                onMultiSelect = onEnterSelection,
                onForward = onForwardMessage,
                onPlusOne = onPlusOne,
            )
            }
        },
        LocalMessageSelection provides selectionState,
    ) {
    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
    Scaffold(
        topBar = {
            // 聊天页与 M3E 对齐：顶栏始终是钉住的小标题（expandable = false），
            // 不存在「大标题展开到下方」的形态，也不随消息列表滚动展开/收起。
            if (selectionMode) {
                // 多选态：标题「已选 N 条」+ 退出多选 + 转发（交互/计数/全选语义保持不变）。
                HyperTopAppBar(
                    title = "已选 ${selectedMessageIds.size} 条",
                    scrollBehavior = scrollBehavior,
                    backdrop = backdrop,
                    expandable = false,
                    isContentScrolled = listState.canScrollBackward,
                    navigationIcon = {
                        HyperTopBarIconButton(
                            onClick = onExitSelection,
                            contentDescription = "退出多选",
                            icon = MiuixIcons.Close,
                        )
                    },
                    actions = {
                        TextButton(
                            text = "转发",
                            onClick = onForwardSelected,
                            enabled = selectedMessageIds.isNotEmpty(),
                        )
                    },
                )
            } else {
                HyperTopAppBar(
                    title = chat.name,
                    subtitle = chatSubtitle(chat).orEmpty(),
                    scrollBehavior = scrollBehavior,
                    backdrop = backdrop,
                    expandable = false,
                    isContentScrolled = listState.canScrollBackward,
                    navigationIcon = {
                        HyperTopBarIconButton(
                            onClick = onBack,
                            contentDescription = stringResource(R.string.chat_detail_back),
                            icon = MiuixIcons.Back,
                        )
                    },
                    actions = {
                        if (!chat.isGroup) {
                            HyperTopBarIconButton(
                                onClick = comingSoon,
                                contentDescription = stringResource(R.string.contact_audio_call),
                                icon = MiuixIcons.Phone,
                            )
                        }
                        HyperTopBarIconButton(
                            onClick = onOpenInfo,
                            contentDescription = stringResource(R.string.chat_more_actions),
                            icon = MiuixIcons.More,
                        )
                    },
                )
            }
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        // 内容容器只保留 start/end/bottom inset：top 交给消息列表 contentPadding，
        // 让列表视口延伸到 Glass 顶栏下方——否则内容永远被压在栏下，玻璃只能采到 surface 色。
        val contentStartPadding = innerPadding.calculateStartPadding(layoutDirection)
        val contentEndPadding = innerPadding.calculateEndPadding(layoutDirection)
        val contentBottomPadding = innerPadding.calculateBottomPadding()
        val contentTopPadding = innerPadding.calculateTopPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                // 采样层要早于 inset/padding：记录不透明底 + 页面内容（不含玻璃顶栏自身；
                // 输入栏/面板是普通不透明内容，保留在采样层里，玻璃菜单叠加时几何位置才正确）。
                .then(
                    if (backdrop != null) {
                        Modifier
                            .layerBackdrop(backdrop)
                            .background(colorScheme.surface)
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // 只吃 start/end/bottom inset；top 由消息列表 contentPadding 承担（内容可滚到顶栏下方）。
                    .padding(
                        start = contentStartPadding,
                        end = contentEndPadding,
                        bottom = contentBottomPadding,
                    )
                    // 键盘收起时保留导航栏 padding；键盘弹出由 offset 整体上移（placement 阶段读取，不重组/不重测）。
                    .padding(bottom = navigationBarBottom)
                    .offset {
                        val shift = keyboardHeightState.value - navigationBarBottom
                        if (shift > 0.dp) IntOffset(0, -shift.roundToPx()) else IntOffset.Zero
                    },
            ) {
                // 下拉加载更早消息：Miuix 的 PullToRefresh（与消息列表页/KSU 超级用户列表一致的样式）。
                val pullToRefreshState = rememberPullToRefreshState()
                val loadOlderTexts = listOf(
                    stringResource(R.string.chat_load_older_pulling),
                    stringResource(R.string.chat_load_older_release),
                    stringResource(R.string.chat_load_older_loading),
                    stringResource(R.string.chat_load_older_complete),
                )
                Box(modifier = Modifier.weight(1f)) {
                    PullToRefresh(
                        isRefreshing = isLoadingOlder,
                        pullToRefreshState = pullToRefreshState,
                        onRefresh = {
                            if (!latestLoadingForRefresh.value) {
                                pendingOlderAnchorId =
                                    listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key as? String
                                pendingOlderOffset = listState.firstVisibleItemScrollOffset
                                pendingOlderCount = latestMessagesForRefresh.value.size
                                onLoadOlder()
                            }
                        },
                        refreshTexts = loadOlderTexts,
                        // 下拉指示器放到顶栏下方（内容可穿过顶栏滚动）。
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = contentTopPadding + 10.dp,
                            bottom = 10.dp,
                        ),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(fabNestedScrollConnection)
                                .scrollEndHaptic()
                                .overScrollVertical(),
                            // 首帧消息不被顶栏遮住；滚动时消息从顶栏下方穿过（玻璃顶栏可采样到内容）。
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = contentTopPadding + 10.dp,
                                bottom = 10.dp,
                            ),
                            overscrollEffect = null,
                        ) {
                            if (groupedMessages.isEmpty() && !initialMessagesLoaded) {
                                items(count = 3, key = { "chat_preview_skeleton_$it" }) { index ->
                                    ChatPreviewSkeletonMiuix(isMe = index % 2 == 1)
                                }
                            }
                            items(groupedMessages, key = { it.message.id }, contentType = { it.message.type }) { item ->
                                SwipeToReplyRow(
                                    messageId = item.message.id,
                                    onReply = { onReplyRequest(item.message) },
                                    enabled = !selectionMode,
                                ) {
                                    ChatMessageItemMiuix(
                                        item = item,
                                        onReplyQuoteClick = onReplyQuoteClick,
                                        plusOneMessageIds = plusOneMessageIds,
                                        onFileClick = onFileClick,
                                        downloadingFileMsgId = downloadingFileMsgId,
                                        fileDownloadProgress = fileDownloadProgress,
                                        canRecallOthers = canRecallOthers,
                                        canGroupAtSender = chat.isGroup,
                                        canAddToFavorite = hasEmojiSource,
                                        onAvatarClick = onAvatarClick,
                                        isGroupChat = chat.isGroup,
                                        selfGroupName = selfGroupName,
                                        onArkGroupNoticeClick = onArkGroupNoticeClick,
                                    )
                                }
                            }
                        }
                    }
                }
                AnimatedReplyBar(target = replyTarget) { target ->
                    ReplyComposerBarMiuix(target = target, onClear = onClearReply)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { composerBarHeightPx = it.height },
                ) {
                ChatInputBarMiuix(
                    composer = composer,
                    onSend = onSend,
                    onEmojiClick = { onTogglePanel(ChatPanel.Emoji) },
                    onAttachClick = { onTogglePanel(ChatPanel.Attach) },
                    onInputFocused = onInputFocused,
                    isGroup = chat.isGroup,
                    imeController = imeController,
                    inputFocusRequester = chatInputFocusRequester,
                    onStartVoiceRecord = onStartVoiceRecord,
                )
                // 表情面板：输入栏下方的“键盘位”面板。
                AnimatedVisibility(
                    visible = composer.panel == ChatPanel.Emoji,
                    enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
                ) {
                    ChatEmojiPanelMiuix(
                        composer = composer,
                        onSendCustomEmoji = onSendCustomEmoji,
                        onSendStickerPackEmoji = onSendStickerPackEmoji,
                    )
                }
                // 附件面板：输入栏下方就地展开（不加额外背景容器，直接铺在页面背景上）。
                AnimatedVisibility(
                    visible = composer.panel == ChatPanel.Attach,
                    enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
                ) {
                    ChatAttachPanelMiuix(
                        hintVisible = composer.attachHintVisible,
                        onDismissHint = composer::dismissAttachHint,
                        onAction = { action ->
                            composer.closePanels()
                            onAttachAction(action)
                        },
                        onCameraAction = { isVideo ->
                            composer.closePanels()
                            onCameraAction(isVideo)
                        },
                    )
                }
                }
            }
        ChatFloatingActionsMiuix(
            bottomButtonVisible = canScrollForward && backToBottomGestureVisible,
            atMeVisible = hasUnreadAtMe,
            bottomOffsetPx = composerBarHeightPx,
            keyboardBottomInset = keyboardHeightState,
            onAtMe = {
                // 从最新一条未读 @我 开始，逐条向更早遍历。
                val target = unreadAtMeIndices.getOrNull(
                    unreadAtMeIndices.lastIndex - atMeCursor,
                )
                if (target != null) {
                    atMeCursor += 1
                    fabScope.launch { listState.animateScrollToItem(target) }
                }
            },
            onBackToBottom = {
                backToBottomGestureVisible = false
                downAccumulatedPx = 0f
                fabScope.launch {
                    listState.animateScrollToItem(groupedMessages.lastIndex.coerceAtLeast(0))
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd),
        )
        }
    }
    }
    }
    // 长按菜单宿主：页面内容层最后渲染（不被顶栏/输入栏裁剪，返回优先级在页面处理器之后）。
    GlassMenuHost(glassMenu)
    }
    }
}

@Composable
internal fun ChatMessageItemMiuix(
    item: GroupedChatMessage,
    onReplyQuoteClick: (ChatMessage) -> Unit = {},
    plusOneMessageIds: Set<Long> = emptySet(),
    onFileClick: (ChatMessage) -> Unit = {},
    downloadingFileMsgId: Long = 0L,
    fileDownloadProgress: Int = -1,
    canRecallOthers: Boolean = false,
    canGroupAtSender: Boolean = false,
    canAddToFavorite: Boolean = false,
    onAvatarClick: (ChatMessage) -> Unit = {},
    isGroupChat: Boolean = false,
    selfGroupName: String? = null,
    onArkGroupNoticeClick: (ChatMessage) -> Unit = {},
) {
    val message = item.message
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = item.group.spacingBefore),
    ) {
        message.timeSeparator?.let { separator ->
            TimeSeparatorMiuix(separator)
        }
        if (message.type == ChatMessageType.SYSTEM) {
            SystemMessageMiuix(message.content)
        } else {
            MessageRowMiuix(
                message = message,
                group = item.group,
                onReplyQuoteClick = onReplyQuoteClick,
                plusOne = message.chatRecordMsgId in plusOneMessageIds,
                onFileClick = onFileClick,
                downloadingFileMsgId = downloadingFileMsgId,
                fileDownloadProgress = fileDownloadProgress,
                canRecallOthers = canRecallOthers,
                canGroupAtSender = canGroupAtSender,
                canAddToFavorite = canAddToFavorite,
                onAvatarClick = onAvatarClick,
                isGroupChat = isGroupChat,
                selfGroupName = selfGroupName,
                onArkGroupNoticeClick = onArkGroupNoticeClick,
            )
        }
    }
}

@Composable
private fun MessageRowMiuix(
    message: ChatMessage,
    group: ChatBubbleGroup,
    onReplyQuoteClick: (ChatMessage) -> Unit = {},
    plusOne: Boolean = false,
    onFileClick: (ChatMessage) -> Unit = {},
    downloadingFileMsgId: Long = 0L,
    fileDownloadProgress: Int = -1,
    canRecallOthers: Boolean = false,
    canGroupAtSender: Boolean = false,
    canAddToFavorite: Boolean = false,
    onAvatarClick: (ChatMessage) -> Unit = {},
    isGroupChat: Boolean = false,
    selfGroupName: String? = null,
    onArkGroupNoticeClick: (ChatMessage) -> Unit = {},
) {
    val isMe = message.isMe
    val selection = LocalMessageSelection.current
    val selectionActive = selection?.active == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 多选：点击整行任意位置切换选中；同时消费抬起事件，阻止图片/视频/链接等子组件点击。
            .selectMessageRow(
                active = selectionActive,
                messageId = message.id,
                onToggle = { selection?.onToggle(message) },
            ),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isMe) {
            ChatAvatarSlotMiuix(
                message = message,
                group = group,
                trailing = false,
                onAvatarClick = onAvatarClick,
            )
        }
        // 多选：自己的消息 checkbox 放整行最左侧，气泡仍贴右（用 weight 占位推开）。
        if (isMe && selection?.active == true) {
            MiuixSelectionCheckbox(
                checked = message.chatRecordMsgId in selection.selectedIds,
                onToggle = { selection.onToggle(message) },
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Spacer(Modifier.weight(1f))
        }
        Column(
            modifier = Modifier.widthIn(max = 284.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
        ) {
            // 群聊：别人消息按 showSenderName 显示；自己消息（bridge showSenderName=false）在群聊里同样显示昵称。
            val showSenderLabel = group.isGroupStart && if (isMe) isGroupChat else message.showSenderName
            if (showSenderLabel) {
                Text(
                    text = if (isMe) {
                        // bridge 对自己消息的 senderName 通常是「我」：优先真实群名片，其次 LiveProfile 昵称，最后「我」。
                        message.senderName
                            .takeIf { it.isNotBlank() && it != stringResource(R.string.chat_sender_me) }
                            ?: selfGroupName
                            ?: LiveProfile.peek()?.name?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.chat_sender_me)
                    } else {
                        message.senderName
                    },
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // 自己的昵称右对齐到气泡右侧（别人左对齐），间距与别人一致。
                    modifier = if (isMe) {
                        Modifier.padding(end = 4.dp, bottom = 2.dp)
                    } else {
                        Modifier.padding(start = 4.dp, bottom = 2.dp)
                    },
                )
            }
            if (message.hasReply || message.replyToContent.isNotBlank() || message.replyToName.isNotBlank()) {
                ReplyQuoteMiuix(message, onClick = { onReplyQuoteClick(message) })
            }
            // 多选模式下不挂长按菜单（点击整行只用于选择），直接渲染气泡。
            if (selectionActive) {
                ChatBubbleMiuix(
                    message = message,
                    position = group.position,
                    onFileClick = onFileClick,
                    downloadingFileMsgId = downloadingFileMsgId,
                    fileDownloadProgress = fileDownloadProgress,
                    onArkGroupNoticeClick = onArkGroupNoticeClick,
                )
            } else {
                BubbleActionMenuHost(
                    message = message,
                    canRecallOthers = canRecallOthers,
                    canAtSender = canGroupAtSender,
                    canAddToFavorite = canAddToFavorite,
                ) {
                    ChatBubbleMiuix(
                        message = message,
                        position = group.position,
                        onFileClick = onFileClick,
                        downloadingFileMsgId = downloadingFileMsgId,
                        fileDownloadProgress = fileDownloadProgress,
                        onArkGroupNoticeClick = onArkGroupNoticeClick,
                    )
                }
            }
            if (group.isGroupEnd) {
                MessageMetaMiuix(message = message, plusOne = plusOne)
            } else if (plusOne) {
                // 组内消息没有时间/已读行，+1 标记仍需立即出现在气泡下方。
                PlusOneTagMiuix(modifier = Modifier.padding(top = 2.dp))
            }
        }
        // 多选：对方的消息 checkbox 放整行最右侧，头像+气泡仍贴左。
        if (!isMe && selection?.active == true) {
            Spacer(Modifier.weight(1f))
            MiuixSelectionCheckbox(
                checked = message.chatRecordMsgId in selection.selectedIds,
                onToggle = { selection.onToggle(message) },
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        if (isMe) {
            ChatAvatarSlotMiuix(
                message = message,
                group = group,
                trailing = true,
                onAvatarClick = onAvatarClick,
            )
        }
    }
}

@Composable
private fun MiuixSelectionCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    top.yukonga.miuix.kmp.basic.Checkbox(
        state = if (checked) {
            androidx.compose.ui.state.ToggleableState.On
        } else {
            androidx.compose.ui.state.ToggleableState.Off
        },
        onClick = onToggle,
        modifier = modifier,
    )
}

/** 头像只出现在分组最后一条；其余消息保留同宽占位，保证同组气泡左/右对齐。 */
@Composable
private fun ChatAvatarSlotMiuix(
    message: ChatMessage,
    group: ChatBubbleGroup,
    trailing: Boolean,
    onAvatarClick: (ChatMessage) -> Unit = {},
) {
    val sidePadding = if (trailing) Modifier.padding(start = 8.dp) else Modifier.padding(end = 8.dp)
    if (group.isGroupEnd) {
        // 分组末尾的气泡下方还有时间 / 已读行，头像下移让底边对齐气泡底边。
        val hasMeta = message.isMe || message.timeLabel.isNotEmpty()
        // 系统消息/无发送者 uid 的消息不响应点击。
        val avatarClickable = message.type != ChatMessageType.SYSTEM && message.senderUid.isNotBlank()
        val dataLineKind = LocalChatDataLineKind.current
        val avatarModifier = sidePadding
            .padding(bottom = if (hasMeta) ChatAvatarMetaLift else 0.dp)
            .then(
                if (avatarClickable) {
                    Modifier.clickable { onAvatarClick(message) }
                } else {
                    Modifier
                },
            )
        // 设备会话里对方（我的手机/电脑/平板）发来的消息：头像用固定设备图标，而不是首字渐变。
        if (dataLineKind != 0 && !message.isMe) {
            DataLineAvatar(
                kind = dataLineKind,
                modifier = avatarModifier,
                size = 36.dp,
            )
        } else {
            ChatAvatar(
                name = message.senderName,
                seed = message.senderSeed,
                size = 36.dp,
                avatarPath = message.senderAvatarPath,
                modifier = avatarModifier,
            )
        }
    } else {
        Spacer(modifier = sidePadding.then(Modifier.size(36.dp)))
    }
}

/** 分组最后一条下方的时间（+1 本地反馈标记并排显示）。 */
@Composable
private fun MessageMetaMiuix(message: ChatMessage, plusOne: Boolean = false) {
    val timeLabel = message.timeLabel
    if (timeLabel.isEmpty() && !plusOne) return
    Row(
        modifier = Modifier.padding(
            top = 2.dp,
            end = if (message.isMe) 4.dp else 0.dp,
            start = if (message.isMe) 0.dp else 4.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (timeLabel.isNotEmpty()) {
            Text(
                text = timeLabel,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
            )
        }
        if (plusOne) {
            PlusOneTagMiuix()
        }
    }
}

/**
 * 「+1」本地反馈标记：只表示本次会话里自己成功 +1 过该消息（不落库），
 * 样式小巧，贴在时间/已读旁边，不影响气泡分组布局。
 */
@Composable
private fun PlusOneTagMiuix(modifier: Modifier = Modifier) {
    Text(
        text = "+1",
        fontSize = 11.sp,
        color = colorScheme.onPrimaryContainer,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colorScheme.primaryContainer)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

@Composable
private fun ChatBubbleMiuix(
    message: ChatMessage,
    position: ChatBubblePosition,
    onFileClick: (ChatMessage) -> Unit = {},
    downloadingFileMsgId: Long = 0L,
    fileDownloadProgress: Int = -1,
    onArkGroupNoticeClick: (ChatMessage) -> Unit = {},
) {
    val isMe = message.isMe
    val container = if (isMe) colorScheme.primary else colorScheme.surfaceContainerHigh
    val content = if (isMe) colorScheme.onPrimary else colorScheme.onSurface
    if (message.parts.isNotEmpty()) {
        // 多元素消息（文本+表情+图片混排）：按 parts 顺序渲染，避免只显示第一个元素丢图。
        MultiPartBubbleMiuix(message, container, content, position)
    } else when (message.type) {
        ChatMessageType.TEXT -> TextBubbleMiuix(message, container, content, position)
        ChatMessageType.IMAGE -> ImageBubbleMiuix(message, position)
        ChatMessageType.FILE -> FileBubbleMiuix(
            message = message,
            container = container,
            content = content,
            position = position,
            onClick = { onFileClick(message) },
            downloading = message.chatRecordMsgId != 0L &&
                message.chatRecordMsgId == downloadingFileMsgId,
            downloadProgress = fileDownloadProgress,
        )
        ChatMessageType.VOICE -> VoiceBubbleMiuix(message, container, content, position)
        ChatMessageType.RED_PACKET -> RedPacketBubbleMiuix(message, position)
        ChatMessageType.LINK -> LinkBubbleMiuix(message, container, content, position)
        ChatMessageType.CHAT_RECORD -> ChatRecordBubbleMiuix(message, container, content, position)
        ChatMessageType.VIDEO -> VideoBubbleMiuix(message, position)
        ChatMessageType.ARK -> ArkCardBubbleMiuix(
            message = message,
            container = container,
            content = content,
            position = position,
            onArkGroupNoticeClick = onArkGroupNoticeClick,
        )
        ChatMessageType.SYSTEM -> Unit
    }
}

@Composable
private fun TextBubbleMiuix(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    Box(
        modifier = Modifier
            .clip(ChatMiuixBubbleShape)
            .background(container)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val uriHandler = remember(context) { openLinkHandler(context) }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalUriHandler provides uriHandler,
        ) {
            Text(
                // 自己气泡背景是 primary，链接再用 primary 会看不见：改用内容色 + 下划线。
                text = linkifyContent(
                    message.content,
                    if (message.isMe) content else colorScheme.primary,
                ),
                fontSize = 15.sp,
                color = content,
            )
        }
    }
}

/**
 * 多元素消息气泡（Miuix）：外层沿用气泡形状/背景，内部按 [ChatMessage.parts] 顺序竖排。
 * 文本可点链接，图片宽 ≤200dp / 表情图约 110dp（无气泡底），无路径时显示占位。
 */
@Composable
private fun MultiPartBubbleMiuix(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    val navigator = LocalNavigator.current
    val imageLabel = stringResource(R.string.chat_image_message)
    val secondaryContainer = colorScheme.secondaryContainer
    val tertiaryContainer = colorScheme.tertiaryContainer
    val gradient = remember(secondaryContainer, tertiaryContainer) {
        Brush.linearGradient(listOf(secondaryContainer, tertiaryContainer))
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = remember(context) { openLinkHandler(context) }
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalUriHandler provides uriHandler,
    ) {
        Column(
            modifier = Modifier
                .clip(ChatMiuixBubbleShape)
                .background(container)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            message.parts.forEach { part ->
                when (part.kind) {
                    ChatPartKind.TEXT -> Text(
                        text = linkifyContent(
                            part.text,
                            if (message.isMe) content else colorScheme.primary,
                        ),
                        fontSize = 15.sp,
                        color = content,
                    )
                    ChatPartKind.IMAGE -> {
                        val emojiPart = part.emojiImage
                        val ratio = remember(
                            part.imagePath,
                            part.imageWidth,
                            part.imageHeight,
                        ) {
                            val raw = if (part.imageWidth > 0 && part.imageHeight > 0) {
                                part.imageWidth.toFloat() / part.imageHeight
                            } else {
                                1.4f
                            }
                            if (emojiPart) raw.coerceIn(0.5f, 2f) else raw.coerceIn(0.6f, 1.8f)
                        }
                        val imageModifier = if (emojiPart) {
                            Modifier.width(110.dp).aspectRatio(ratio)
                        } else {
                            Modifier.widthIn(max = 200.dp).aspectRatio(ratio)
                        }
                        Box(
                            modifier = imageModifier
                                .clip(RoundedCornerShape(if (emojiPart) 6.dp else 10.dp))
                                .then(
                                    if (emojiPart) {
                                        Modifier
                                    } else {
                                        Modifier.background(gradient)
                                    },
                                )
                                .clickable {
                                    part.imagePath?.let {
                                        navigator.push(
                                            Route.ChatImage(
                                                imagePath = it,
                                                chatId = message.chatId,
                                                msgId = message.msgId,
                                                elementId = part.elementId,
                                            ),
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (part.imagePath != null) {
                                AnimatedOrStaticImage(
                                    path = part.imagePath,
                                    contentDescription = imageLabel,
                                    contentScale = if (emojiPart) {
                                        ContentScale.Fit
                                    } else {
                                        ContentScale.Crop
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                                                        )
                            } else {
                                Icon(
                                    imageVector = MiuixIcons.Image,
                                    contentDescription = imageLabel,
                                    tint = if (emojiPart) {
                                        colorScheme.onSurfaceVariantSummary
                                    } else {
                                        colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                                    },
                                    modifier = Modifier.size(if (emojiPart) 26.dp else 38.dp),
                                )
                                if (!emojiPart) {
                                    Text(
                                        text = part.text.ifBlank { imageLabel },
                                        fontSize = 12.sp,
                                        color = colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                colorScheme.surface.copy(alpha = 0.72f),
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageBubbleMiuix(message: ChatMessage, position: ChatBubblePosition) {
    // 收藏表情 / 表情包图片：不套图片气泡，按表情尺寸小图渲染（TIM 里的「表情」形态）。
    if (message.isEmojiImage) {
        EmojiImageBubbleMiuix(message)
        return
    }
    val navigator = LocalNavigator.current
    val secondaryContainer = colorScheme.secondaryContainer
    val tertiaryContainer = colorScheme.tertiaryContainer
    val gradient = remember(secondaryContainer, tertiaryContainer) {
        Brush.linearGradient(listOf(secondaryContainer, tertiaryContainer))
    }
    val ratio = remember(message.imagePath, message.imageWidth, message.imageHeight) {
        when {
            message.imageWidth > 0 && message.imageHeight > 0 ->
                message.imageWidth.toFloat() / message.imageHeight
            else -> 1.4f
        }.coerceIn(0.6f, 1.8f)
    }
    Box(
        modifier = Modifier
            .width(220.dp)
            .height((220f / ratio).dp)
            .clip(ChatMiuixBubbleShape)
            .background(gradient)
            .clickable {
                message.imagePath?.let {
                navigator.push(
                    Route.ChatImage(
                        imagePath = it,
                        chatId = message.chatId,
                        msgId = message.msgId,
                        elementId = message.elementId,
                    ),
                )
            }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (message.imagePath != null) {
            AnimatedOrStaticImage(
                path = message.imagePath,
                contentDescription = stringResource(R.string.chat_image_message),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = MiuixIcons.Image,
                contentDescription = stringResource(R.string.chat_image_message),
                tint = colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                modifier = Modifier.size(38.dp),
            )
            Text(
                text = message.content,
                fontSize = 12.sp,
                color = colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surface.copy(alpha = 0.72f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

/** 表情图片：无气泡框、无背景，限制最大尺寸类似 TIM 的表情展示。 */
@Composable
private fun EmojiImageBubbleMiuix(message: ChatMessage) {
    val navigator = LocalNavigator.current
    val ratio = remember(message.imagePath, message.imageWidth, message.imageHeight) {
        when {
            message.imageWidth > 0 && message.imageHeight > 0 ->
                message.imageWidth.toFloat() / message.imageHeight
            else -> 1f
        }.coerceIn(0.5f, 2f)
    }
    val widthDp = 132f
    val heightDp = (widthDp / ratio).coerceIn(72f, 190f)
    Box(
        modifier = Modifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clickable {
                message.imagePath?.let {
                navigator.push(
                    Route.ChatImage(
                        imagePath = it,
                        chatId = message.chatId,
                        msgId = message.msgId,
                        elementId = message.elementId,
                    ),
                )
            }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (message.imagePath != null) {
            AnimatedOrStaticImage(
                path = message.imagePath,
                contentDescription = stringResource(R.string.chat_image_message),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = message.content,
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun FileBubbleMiuix(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
    onClick: () -> Unit = {},
    downloading: Boolean = false,
    downloadProgress: Int = -1,
) {
    val iconBackground = if (message.isMe) {
        colorScheme.onPrimary.copy(alpha = 0.18f)
    } else {
        colorScheme.secondaryContainer
    }
    val iconTint = if (message.isMe) colorScheme.onPrimary else colorScheme.onSecondaryContainer
    // 已下载（本地文件存在）→ 打开图标；未下载/文件缺失 → 下载图标。
    val downloaded = !message.mediaPath.isNullOrBlank() && File(message.mediaPath).isFile
    Row(
        modifier = Modifier
            .clip(ChatMiuixBubbleShape)
            .clickable(onClick = onClick)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.File,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.widthIn(max = 190.dp)) {
            Text(
                text = message.content,
                fontSize = 14.sp,
                color = content,
                maxLines = 1,
                softWrap = false,
            )
            if (message.fileSize.isNotEmpty()) {
                Text(
                    text = message.fileSize,
                    fontSize = 11.sp,
                    color = content.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
            if (downloading) {
                Spacer(Modifier.height(6.dp))
                MiuixProgressBar(
                    progress = if (downloadProgress >= 0) {
                        downloadProgress / 100f
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = content,
                    trackColor = content.copy(alpha = 0.24f),
                )
            }
        }
        // 下载中隐藏右侧状态图标（进度条已表达状态）。
        if (!downloading) {
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = if (downloaded) {
                    Icons.AutoMirrored.Filled.OpenInNew
                } else {
                    MiuixIcons.Download
                },
                contentDescription = null,
                tint = content.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun VoiceBubbleMiuix(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    val context = LocalContext.current
    val playing = VoicePlaybackController.isPlaying(message.id)
    val unavailableText = stringResource(R.string.chat_voice_unavailable)
    val playFailedText = stringResource(R.string.chat_voice_play_failed)
    // 听筒/扬声器路由：状态来自宿主播放器，点击只切换路由、不触发气泡播放/暂停。
    val voiceSource = AppDataSources.voicePlaybackSource
    var speakerEnabled by remember { mutableStateOf(voiceSource?.isSpeakerEnabled() == true) }
    // 常驻进度条：未播放 0%，播放中实时进度，暂停后停在暂停位置。
    val progress = VoicePlaybackController.progressFor(message.id)
    Row(
        modifier = Modifier
            .clip(ChatMiuixBubbleShape)
            .clickable {
                // 宿主播放是异步的：参数/文件问题同步返回 false；播放失败走回调 Toast。
                if (!VoicePlaybackController.toggle(message) {
                        Toast.makeText(context, playFailedText, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Toast.makeText(context, unavailableText, Toast.LENGTH_SHORT).show()
                }
            }
            .background(container)
            .widthIn(min = 160.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (voiceSource != null) {
            Icon(
                imageVector = if (speakerEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.Filled.Call,
                contentDescription = stringResource(
                    if (speakerEnabled) R.string.chat_voice_speaker else R.string.chat_voice_earpiece,
                ),
                tint = content,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable {
                        val next = !speakerEnabled
                        val ok = runCatching { voiceSource.setSpeakerEnabled(next) }
                            .getOrDefault(false)
                        if (ok) speakerEnabled = next
                    },
            )
            Spacer(Modifier.width(6.dp))
        }
        Icon(
            imageVector = if (playing) MiuixIcons.Pause else MiuixIcons.Play,
            contentDescription = stringResource(R.string.chat_voice),
            tint = content,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        // 中间常驻进度条：可拖动 seek（命中高度 24dp，Miuix 标准 4dp 直线）。
        DraggableMiuixProgressBar(
            progress = progress,
            onSeekFinished = { fraction -> VoicePlaybackController.seekTo(message, fraction) },
            modifier = Modifier
                .weight(1f)
                .height(24.dp),
            color = content,
            trackColor = content.copy(alpha = 0.24f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.chat_voice_message, message.voiceSeconds),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = content,
            maxLines = 1,
        )
    }
}

/**
 * 红包气泡（Miuix）：结构与 M3E 一致（暖色卡片 + 祝福语/副标题/领取状态），
 * 有 bridge 数据时可点击打开 TIM 原生领取；无数据保持旧样式且不可点。
 */
@Composable
private fun RedPacketBubbleMiuix(message: ChatMessage, position: ChatBubblePosition) {
    val context = LocalContext.current
    val canOpen = message.canOpenRedPacket()
    val grabbed = message.isRedPacketGrabbed()
    val subtitle = message.redPacketSubtitle
    val subtitleLine = when {
        subtitle.isNotBlank() -> subtitle
        !canOpen -> stringResource(R.string.chat_red_packet)
        else -> ""
    }
    val stateText = message.redPacketStateText
    val claimed = grabbed || stateText.contains("已")
    val statusRes = when {
        !canOpen -> R.string.chat_detail_red_packet_open
        claimed -> R.string.chat_red_packet_grabbed
        else -> R.string.chat_red_packet_open
    }
    // TIM 本地有更准确的状态文案（已领取/已领完/…）时直接显示它。
    val statusText = stateText.ifBlank { stringResource(statusRes) }
    Row(
        modifier = Modifier
            .widthIn(min = 210.dp)
            .clip(ChatMiuixBubbleShape)
            .background(colorScheme.primary)
            .then(
                if (canOpen) {
                    Modifier.clickable {
                        val ok = runCatching {
                            val source = AppDataSources.messageSource
                            // 已领取/已领完：打开 TIM 红包详情页（redgiftDetail PayBridge）；
                            // 未领取：优先 TIM 自己的 GrabHbViewModel（含 hb_pre_grap 预领取），
                            // 和原生 AIO 点击路径一致；失败再退回手拼 PayBridge 参数。
                            if (claimed) {
                                source?.openRedPacket(
                                    chatId = message.chatId,
                                    billNo = message.redPacketBillNo,
                                    authkey = message.redPacketAuthkey,
                                    channel = message.redPacketChannel,
                                    senderUin = message.redPacketSenderUin,
                                    grabState = message.redPacketGrabState,
                                ) ?: false
                            } else {
                                (source?.openRedPacketMsg(message.chatId, message.chatRecordMsgId) ?: false) ||
                                    (source?.openRedPacket(
                                        chatId = message.chatId,
                                        billNo = message.redPacketBillNo,
                                        authkey = message.redPacketAuthkey,
                                        channel = message.redPacketChannel,
                                        senderUin = message.redPacketSenderUin,
                                        grabState = message.redPacketGrabState,
                                    ) ?: false)
                            }
                        }.getOrDefault(false)
                        if (!ok) {
                            Toast.makeText(
                                context,
                                R.string.feature_coming_soon,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MiuixIcons.Promotions,
            contentDescription = stringResource(R.string.chat_red_packet),
            tint = colorScheme.onPrimary,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.content.ifBlank { stringResource(R.string.chat_red_packet) },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onPrimary,
                maxLines = 1,
                softWrap = false,
            )
            if (subtitleLine.isNotEmpty()) {
                Text(
                    text = subtitleLine,
                    fontSize = 11.sp,
                    color = colorScheme.onPrimary.copy(alpha = 0.85f),
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = statusText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun LinkBubbleMiuix(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    Row(
        modifier = Modifier
            .clip(ChatMiuixBubbleShape)
            .background(container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MiuixIcons.Link,
            contentDescription = stringResource(R.string.chat_link_message),
            tint = content,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.widthIn(max = 200.dp)) {
            Text(
                text = message.content,
                fontSize = 14.sp,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.chat_link_message),
                fontSize = 11.sp,
                color = content.copy(alpha = 0.72f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TimeSeparatorMiuix(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(colorScheme.surfaceContainer),
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SystemMessageMiuix(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(colorScheme.surfaceContainerHigh),
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/** 输入栏：+ / 关闭附件面板、圆角输入框、表情、空态麦克风 / 非空发送按钮。 */
@Composable
private fun ChatInputBarMiuix(
    composer: ChatComposerState,
    onSend: (String) -> Unit,
    onEmojiClick: () -> Unit,
    onAttachClick: () -> Unit,
    onInputFocused: () -> Unit,
    isGroup: Boolean = false,
    imeController: ChatImeController,
    inputFocusRequester: FocusRequester? = null,
    onStartVoiceRecord: () -> Unit = {},
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val attachOpen = composer.panel == ChatPanel.Attach
    val emojiOpen = composer.panel == ChatPanel.Emoji
    // 固定行高：按布局行数决定输入框高度（1/2/3/4 行各自固定），不随中英文字形高度变化。
    var lineCount by remember { mutableStateOf(1) }
    val submit: () -> Unit = {
        val text = composer.text.trim()
        if (text.isNotEmpty()) {
            onSend(text)
            composer.clearText()
            composer.closePanels()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(
            onClick = onAttachClick,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = if (attachOpen) MiuixIcons.Close else MiuixIcons.AddCircle,
                contentDescription = stringResource(
                    if (attachOpen) R.string.chat_input_close else R.string.chat_more_actions,
                ),
                tint = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(26.dp),
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(42.dp + ((lineCount.coerceIn(1, 4) - 1) * 20).dp)
                // 点击输入框区域（即使已聚焦但键盘被系统收起）重新请求键盘：不消费事件。
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        if (!imeController.isImeVisibleNow()) {
                            imeController.requestShow("click")
                        }
                    }
                }
                .clip(RoundedCornerShape(22.dp))
                .background(colorScheme.surfaceContainerHigh)
                .padding(start = 14.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = composer.text,
                onValueChange = { newValue ->
                    // 输入 @ 唤起成员面板前先收起键盘（同 Material 版本）。
                    val willOpenAt = isGroup &&
                        (newValue.endsWith("@") || newValue.endsWith("＠")) &&
                        newValue.length > composer.text.length
                    if (willOpenAt) {
                        // 保留 hide；同时清焦点，避免「focused 但 IME 已关」的坏状态
                        // （系统 HIDE_SAME_WINDOW_FOCUSED_WITHOUT_EDITOR 后不再有 onRequestShow）。
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                    composer.onAtTextChanged(composer.text, newValue, isGroup)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp)
                    .then(
                        if (inputFocusRequester != null) {
                            Modifier.focusRequester(inputFocusRequester)
                        } else {
                            Modifier
                        },
                    )
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onInputFocused()
                            imeController.onInputFocusGained()
                        } else {
                            imeController.onInputFocusLost()
                        }
                    },
                // 显式 lineHeight：保证空态占位符与有文本时的输入框高度一致。
                textStyle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, color = colorScheme.onSurface),
                onTextLayout = { lineCount = it.lineCount },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                maxLines = 4,
                cursorBrush = SolidColor(colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (composer.text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.chat_input_hint),
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                },
            )
            IconButton(
                onClick = onEmojiClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEmotions,
                    contentDescription = stringResource(
                        if (emojiOpen) R.string.chat_input_close else R.string.chat_emoji,
                    ),
                    tint = if (emojiOpen) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        // 右侧操作位固定占位（空态麦克风 / 有文本发送），保证输入框宽度和输入栏高度不随文本变化。
        if (composer.text.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onStartVoiceRecord),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.Mic,
                    contentDescription = stringResource(R.string.chat_voice),
                    tint = colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary)
                    .clickable(onClick = submit),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.Send,
                    contentDescription = stringResource(R.string.chat_send),
                    tint = colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatRecordBubbleMiuix(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    val navigator = LocalNavigator.current
    // 共用点击逻辑：读取当前页 parent/root、打日志并构造 Route（聊天页 parent/root=0）。
    val onRecordClick = onChatRecordBubbleClick(message) { route -> navigator.push(route) }
    val count = message.chatRecordCount.coerceAtLeast(message.chatRecordSummary.size)
    Column(
        modifier = Modifier
            .widthIn(max = 252.dp)
            .clip(ChatMiuixBubbleShape)
            .background(container)
            .clickable(onClick = onRecordClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = message.chatRecordTitle.ifBlank { stringResource(R.string.chat_record_title) },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        message.chatRecordSummary.take(4).forEach { line ->
            Spacer(Modifier.height(2.dp))
            Text(
                text = line,
                fontSize = 12.sp,
                color = content.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = stringResource(R.string.chat_record_view, count),
            fontSize = 11.sp,
            color = content.copy(alpha = 0.6f),
            maxLines = 1,
        )
    }
}

/**
 * ARK 卡片气泡（Miuix）：结构同 M3E（预览图 + 来源/标签 + 标题 + 描述 + 页脚箭头），
 * 用 Miuix Card/MiuixText 呈现；所有 ARK 字段为空时退回普通文本气泡。
 */
@Composable
private fun ArkCardBubbleMiuix(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
    onArkGroupNoticeClick: (ChatMessage) -> Unit = {},
) {
    val hasCardContent = message.arkSource.isNotBlank() ||
        message.arkTitle.isNotBlank() ||
        message.arkDesc.isNotBlank() ||
        message.arkTag.isNotBlank() ||
        message.arkPrompt.isNotBlank() ||
        message.arkJumpUrl.isNotBlank() ||
        !message.arkPreviewPath.isNullOrBlank()
    if (!hasCardContent) {
        TextBubbleMiuix(message, container, content, position)
        return
    }
    val context = LocalContext.current
    val previewBitmap = rememberLocalImage(message.arkPreviewPath, maxDimension = 720)
    val jumpUrl = message.arkJumpUrl
    val clickable = message.arkGroupNotice || jumpUrl.isNotBlank()
    Card(
        modifier = Modifier.widthIn(max = 280.dp),
        onClick = {
            if (message.arkGroupNotice) {
                // 群公告 ARK 卡片：进入本群群公告页，不按外部链接打开。
                onArkGroupNoticeClick(message)
            } else if (jumpUrl.isNotBlank()) {
                com.material3tim.ui.util.ExternalLink.open(context, jumpUrl)
            }
        },
        showIndication = clickable,
        insideMargin = PaddingValues(0.dp),
        // 与普通气泡同色：自己=primary/onPrimary，别人=surfaceContainerHigh/onSurface。
        colors = CardDefaults.defaultColors(color = container, contentColor = content),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = message.arkTitle.ifBlank { null },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                )
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (message.arkSource.isNotBlank() || message.arkTag.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.arkSource,
                            fontSize = 12.sp,
                            color = content.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (message.arkTag.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = message.arkTag,
                                fontSize = 11.sp,
                                color = content.copy(alpha = 0.9f),
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(content.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (message.arkTitle.isNotBlank()) {
                    Text(
                        text = message.arkTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (message.arkDesc.isNotBlank()) {
                    Text(
                        text = message.arkDesc,
                        fontSize = 12.sp,
                        color = content.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val footer = when {
                    message.arkPrompt.isNotBlank() &&
                        message.arkPrompt != message.arkSource -> message.arkPrompt
                    message.arkSource.isNotBlank() -> message.arkSource
                    else -> message.arkApp
                }
                if (footer.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = footer,
                            fontSize = 11.sp,
                            color = content.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = content.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoBubbleMiuix(message: ChatMessage, position: ChatBubblePosition) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    // 视频宽高由 bridge 放在 imageWidth/imageHeight 上；无元数据时退回 16:9。
    val width = message.imageWidth
    val height = message.imageHeight
    val ratio = when {
        width > 0 && height > 0 -> width.toFloat() / height
        else -> 16f / 9f
    }.coerceIn(0.6f, 2.4f)
    val hasMetadata = message.videoSeconds > 0 ||
        width > 0 ||
        height > 0 ||
        message.fileSize.isNotBlank()
    // 缩略图兜底：videoThumbPath 缺失时从本地 videoPath 抽首帧（异步、缓存）。
    val thumbPath = rememberVideoThumbnailPath(message)
    val bitmap = rememberLocalImage(thumbPath, maxDimension = 512)
    val placeholderColor = colorScheme.surfaceContainerHighest
    val placeholderContent = colorScheme.onSurfaceVariantSummary
    // 点击统一走共享分支（本地播放 / 在线 URL / 无法播放 Toast）。
    val onVideoClick: () -> Unit = {
        openVideoMessage(
            context = context,
            message = message,
            thumbnailPath = thumbPath.orEmpty(),
        ) { route -> navigator.push(route) }
    }
    Box(
        modifier = Modifier
            .width(if (hasMetadata) 220.dp else 120.dp)
            .height(
                if (hasMetadata) {
                    (220f / ratio).dp.coerceAtMost(300.dp)
                } else {
                    80.dp
                },
            )
            .clip(ChatMiuixBubbleShape)
            .background(if (bitmap != null) Color.Black else placeholderColor)
            .clickable(onClick = onVideoClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = stringResource(R.string.chat_video_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // 播放图标：有缩略图时叠在图上（黑底白图标），无缩略图时作为占位主体。
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(if (hasMetadata) 46.dp else 32.dp)
                    .clip(CircleShape)
                    .background(
                        if (bitmap != null) {
                            Color.Black.copy(alpha = 0.45f)
                        } else {
                            placeholderContent.copy(alpha = 0.16f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MiuixIcons.Play,
                    contentDescription = stringResource(R.string.chat_video_title),
                    tint = if (bitmap != null) Color.White else placeholderContent,
                    modifier = Modifier.size(if (hasMetadata) 26.dp else 18.dp),
                )
            }
            if (!hasMetadata) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.chat_video_title),
                    fontSize = 12.sp,
                    color = placeholderContent,
                    maxLines = 1,
                )
            }
        }
        if (message.videoSeconds > 0) {
            Text(
                text = formatVideoDuration(message.videoSeconds),
                fontSize = 11.sp,
                color = if (bitmap != null) Color.White else placeholderContent,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            )
        }
        if (bitmap == null && hasMetadata && message.fileSize.isNotBlank()) {
            Text(
                text = message.fileSize,
                fontSize = 11.sp,
                color = placeholderContent,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }
    }
}

/** 引用预览（放在气泡上方，点开后不再额外跳转）。 */
@Composable
private fun ReplyQuoteMiuix(message: ChatMessage, onClick: () -> Unit = {}) {
    val name = message.replyToName.ifBlank { "对方" }
    val content = message.replyToContent.ifBlank { "[消息]" }
    Row(
        modifier = Modifier
            .widthIn(max = 284.dp)
            .padding(bottom = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .heightIn(min = 28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colorScheme.primary),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = "回复 $name",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = content,
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 输入栏上方的「正在回复」条。 */
@Composable
private fun ReplyComposerBarMiuix(target: ChatMessage, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceContainerHigh)
            .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "回复 ${target.senderName}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = target.content.ifBlank { "[消息]" },
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** 长按菜单动作处理：复制/引用/删除/转发/+1 由页面实现，未识别动作保留兜底提示。 */
private fun handleMessageMenuAction(
    context: android.content.Context,
    action: MessageMenuAction,
    message: ChatMessage,
    onReply: (ChatMessage) -> Unit,
    onDelete: (ChatMessage) -> Unit,
    onRecall: (ChatMessage) -> Unit = {},
    onMultiSelect: (ChatMessage) -> Unit = {},
    onForward: (ChatMessage) -> Unit = {},
    onPlusOne: (ChatMessage) -> Unit = {},
) {
    when (action) {
        MessageMenuAction.COPY -> {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                as android.content.ClipboardManager
            clipboard.setPrimaryClip(
                android.content.ClipData.newPlainText("Material3Tim", message.content),
            )
            android.widget.Toast.makeText(context, "已复制", android.widget.Toast.LENGTH_SHORT).show()
        }
        MessageMenuAction.REPLY -> onReply(message)
        MessageMenuAction.RECALL -> onRecall(message)
        MessageMenuAction.DELETE -> onDelete(message)
        MessageMenuAction.MULTI_SELECT -> onMultiSelect(message)
        MessageMenuAction.FORWARD -> onForward(message)
        MessageMenuAction.PLUS_ONE -> onPlusOne(message)
        // at对方在 LocalMessageMenuAction provider 中单独处理（需聚焦输入框 + 弹键盘）。
        MessageMenuAction.AT_SENDER -> Unit
        MessageMenuAction.ADD_TO_FAVORITE -> Unit
    }
}


/** 首次打开聊天且无缓存时的轻量骨架预览：转场期间避免列表空白。 */
@Composable
private fun ChatPreviewSkeletonMiuix(isMe: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .width(if (isMe) 200.dp else 240.dp)
                .height(if (isMe) 40.dp else 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.onSurface.copy(alpha = 0.08f)),
        )
    }
}

/** 聊天页右下角浮动按钮层：@我（跳到最上方 @我的消息）/ 回到最底部。 */
@Composable
private fun ChatFloatingActionsMiuix(
    bottomButtonVisible: Boolean,
    atMeVisible: Boolean,
    bottomOffsetPx: Int,
    keyboardBottomInset: State<Dp>,
    onAtMe: () -> Unit,
    onBackToBottom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val margin = 16.dp
    val density = LocalDensity.current
    val marginPx = with(density) { margin.roundToPx() }
    Column(
        modifier = modifier
            .padding(end = margin)
            .offset {
                // 键盘 inset 在 layout 阶段读取：键盘动画每帧变化时只重排放置，不重组。
                IntOffset(0, -(bottomOffsetPx + keyboardBottomInset.value.roundToPx() + marginPx))
            },
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(margin),
    ) {
        if (atMeVisible) {
            MiuixChatFloatingButton(onClick = onAtMe) {
                MiuixText(text = "@", fontSize = 20.sp, color = colorScheme.onPrimary)
            }
        }
        // Miuix：animateFloatAsState 做 alpha/scale 显隐动画（与 M3E AnimatedVisibility 等价）。
        val backAlpha by animateFloatAsState(
            targetValue = if (bottomButtonVisible) 1f else 0f,
            label = "backToBottomAlpha",
        )
        val backScale by animateFloatAsState(
            targetValue = if (bottomButtonVisible) 1f else 0.85f,
            label = "backToBottomScale",
        )
        if (backAlpha > 0.01f) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = backAlpha
                    scaleX = backScale
                    scaleY = backScale
                },
            ) {
                MiuixChatFloatingButton(onClick = onBackToBottom) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.chat_back_to_bottom),
                        tint = colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/** Miuix 浮动圆形按钮（44dp，阴影 + primary 底色，与 M3E 尺寸/间距一致）。 */
@Composable
private fun ChatTopBarTitleWithAvatarMiuix(chat: ChatItem, subtitle: String) {
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val barHeight = 64.dp
    val gapPx = with(density) { 8.dp.roundToPx() }
    val avatarSizePx = with(density) { 44.dp.roundToPx() }
    Layout(
        contents = listOf(
            {
                // 与返回按钮同直径（Miuix Glass 顶栏按钮 44dp）。
                if (chat.dataLineKind != 0) {
                    DataLineAvatar(kind = chat.dataLineKind, size = 44.dp)
                } else {
                    ChatAvatar(
                        name = chat.name,
                        seed = chat.avatarSeed,
                        size = 44.dp,
                        isOnline = chat.isOnline,
                        isGroup = chat.isGroup,
                        avatarPath = chat.avatarPath,
                    )
                }
            },
            {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = chat.name,
                        style = MiuixTheme.textStyles.title3,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
        ),
        modifier = Modifier
            .size(44.dp)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
