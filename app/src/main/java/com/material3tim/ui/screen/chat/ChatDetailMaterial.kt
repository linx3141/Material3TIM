package com.material3tim.ui.screen.chat

import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.DisposableEffect
import com.material3tim.ui.component.rememberCachedLocalImage
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.layout.layout
import androidx.compose.runtime.State
import android.content.Context
import android.util.Log
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.layout.offset
import java.io.File
import androidx.compose.material.icons.filled.KeyboardArrowUp

import com.material3tim.ui.component.AnimatedOrStaticImage
import com.material3tim.ui.component.rememberLocalImage
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.material3tim.data.model.SuperEmojiItem
import com.material3tim.data.model.SystemFaceItem
import com.material3tim.ui.component.ime.ChatImeController
import com.material3tim.ui.component.ime.rememberKeyboardHeightDp
import com.material3tim.ui.component.DraggableWavyProgressBar
import com.material3tim.ui.component.VoicePlaybackController
import com.material3tim.ui.component.WavyProgressBar
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 聊天详情页（Material 3 Expressive 风格）。
 *
 * 结构：紧凑顶栏（返回 / 会话名 + 副标题 / 更多 + 单聊通话入口）
import com.material3tim.ui.component.SuperEmojiAnimation
import com.material3tim.ui.component.DataLineAvatar
 * + 底部锚定的反向消息列表（时间分隔、系统消息、六种气泡，同一发送者连续消息按
 *   M3 Expressive 分段圆角成组）
 * + 输入栏（表情面板在输入栏上方、附件面板在输入栏下方，二者互斥）
 * + 输入法 / 导航栏 insets 由外层 Column 统一处理。
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
@Suppress("UNUSED_PARAMETER")
internal fun ChatDetailMaterial(
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
    showPlusOne: Boolean = false,
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
    onSendSuperEmoji: (SuperEmojiItem) -> Unit = {},
    onSendSystemFace: (SystemFaceItem) -> Unit = {},
    imeController: ChatImeController,
) {
    val context = LocalContext.current
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
    // 右上角「向上箭头」跳转：把未读 @我 消息与最近会话 msgBox 事件消息
    // （有新文件 / 有人回复我 / 群公告 / 红包等，来自 chat.previewEventSeqs）合并，
    // 按列表位置从新到旧逐条跳转；全部跳完后按钮隐藏。
    // 进入时快照未读数（进入聊天后会被内核标记已读，之后 chat.unreadCount 不可靠）。
    val unreadCountSnapshot = remember(chat.id) { chat.unreadCount }
    val unreadAtMeIndices = remember(groupedMessages, unreadCountSnapshot) {
        val unreadWindow = unreadCountSnapshot.coerceIn(0, groupedMessages.size)
        val start = (groupedMessages.size - unreadWindow).coerceAtLeast(0)
        (start until groupedMessages.size).filter { groupedMessages[it].message.atMe }
    }
    // msgBox 事件消息：按 seq 匹配已加载消息（事件 seq 可能有多条，按在列表里的位置排序）。
    val eventJumpIndices = remember(groupedMessages, chat.previewEventSeqs) {
        if (chat.previewEventSeqs.isEmpty()) {
            emptyList()
        } else {
            val seqs = chat.previewEventSeqs.toHashSet()
            groupedMessages.indices.filter { idx ->
                groupedMessages[idx].message.seq in seqs
            }
        }
    }
    val jumpTargets = remember(unreadAtMeIndices, eventJumpIndices) {
        (unreadAtMeIndices + eventJumpIndices).distinct().sortedDescending()
    }
    val fabScope = rememberCoroutineScope()

    // 非 reverseLayout 列表的滚动锚定：进入时到底部、有新消息且原本在底部时跟随；
    // 加载更早消息时的视口保持由下面的 pendingOlderAnchor 逻辑处理。
    var scrollAnchor by remember(chat.id) {
    // jumpCursor：下一次跳转距列表末尾的偏移（0 = 最新一条）；每次跳转 +1。
    var jumpCursor by remember(chat.id) { mutableIntStateOf(0) }
    val hasJumpTargets = jumpTargets.isNotEmpty() && jumpCursor < jumpTargets.size
    // 回到最底部按钮：只要列表还能向前滚动（不在底部）就显示，回到底部自动隐藏。
    // 不再按手势方向即时收起：正常滑动/停顿/惯性回弹时按钮不会闪来闪去。
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
    // 展开任一面板时收起键盘，保证面板是“键盘位”的替代品。
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
    // @ 成员选择底部 Sheet：标准 material3 ModalBottomSheet（Dialog 自带 scrim/inset/大屏限宽）。
    // 输入 @ 时我们已请求收起 IME；再等 IME 完全收起后才挂载 Dialog，避免 Dialog 抢焦点
    // 与 IME 收起动画叠加造成键盘/Sheet 快速闪动（Miuix 是内嵌 overlay，不存在此问题）。
    val imeVisibleNow = WindowInsets.isImeVisible
    val imeVisibleState = rememberUpdatedState(imeVisibleNow)
    var atSheetReady by remember(atSheetVisible) { mutableStateOf(!atSheetVisible) }
    LaunchedEffect(atSheetVisible) {
        if (atSheetVisible) {
            // 最多等 400ms，避免键盘异常时面板永远打不开。
            withTimeoutOrNull(400) {
                snapshotFlow { imeVisibleState.value }.first { !it }
            }
            // IME 完全收起后立即挂 Dialog；不加额外延迟，缩短“键盘消失→Sheet 弹出”的空白感。
            atSheetReady = true
        }
    }
    LaunchedEffect(atSheetVisible) {
        // 面板打开且成员为空：按需强制加载群成员（带 loading 态，避免空列表）。
        if (atSheetVisible && atMembers.isEmpty() && !atMembersLoading) {
            onAtMembersRequested()
        }
    }
    ChatAtSheetMaterial(
        visible = atSheetVisible && atSheetReady,
        members = atMembers,
        onDismiss = onAtDismiss,
        onConfirm = onAtConfirm,
        loading = atMembersLoading,
        failed = atMembersFailed,
        onRetry = onAtMembersRequested,
    )

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
            handleMessageMenuActionMaterial(
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
    ExpressiveScaffold(
        topBar = {
            if (selectionMode) {
                TopAppBar(
                    title = { Text("已选 ${selectedMessageIds.size} 条") },
                    navigationIcon = {
                        IconButton(onClick = onExitSelection) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "退出多选",
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = onForwardSelected,
                            enabled = selectedMessageIds.isNotEmpty(),
                        ) { Text("转发") }
                    },
                    colors = expressiveTopAppBarColors(),
                )
            } else {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showTopBarAvatar) {
                            if (chat.dataLineKind != 0) {
                                DataLineAvatar(kind = chat.dataLineKind, size = 40.dp)
                            } else {
                                ChatAvatar(
                                    name = chat.name,
                                    seed = chat.avatarSeed,
                                    size = 40.dp,
                                    isOnline = chat.isOnline,
                                    isGroup = chat.isGroup,
                                    avatarPath = chat.avatarPath,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        ChatTopBarTitleMaterial(chat)
                    }
                },
                navigationIcon = {
                    TopBarBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.chat_detail_back),
                    )
                },
                actions = {
                    if (!chat.isGroup) {
                        IconButton(onClick = comingSoon) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = stringResource(R.string.contact_audio_call),
                            )
                        }
                    }
                    IconButton(onClick = onOpenInfo) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = stringResource(R.string.chat_more_actions),
                        )
                    }
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds(),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // 键盘收起时保留导航栏 padding；键盘弹出由 offset 整体上移（placement 阶段读取，不重组/不重测）。
                .padding(bottom = navigationBarBottom)
                .offset {
                    val shift = keyboardHeightState.value - navigationBarBottom
                    if (shift > 0.dp) IntOffset(0, -shift.roundToPx()) else IntOffset.Zero
                },
        ) {
            // 下拉加载更早消息：M3E 的 PullToRefreshBox（与消息列表页同一套加载动画）。
            val pullToRefreshState = rememberPullToRefreshState()
            PullToRefreshBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                isRefreshing = isLoadingOlder,
                onRefresh = {
                    if (!latestLoadingForRefresh.value) {
                        pendingOlderAnchorId =
                            listState.layoutInfo.visibleItemsInfo.firstOrNull()?.key as? String
                        pendingOlderOffset = listState.firstVisibleItemScrollOffset
                        pendingOlderCount = latestMessagesForRefresh.value.size
                        onLoadOlder()
                    }
                },
                state = pullToRefreshState,
                indicator = {
                    PullToRefreshDefaults.LoadingIndicator(
                        modifier = Modifier.align(Alignment.TopCenter),
                        isRefreshing = isLoadingOlder,
                        state = pullToRefreshState,
                    )
                },
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(fabNestedScrollConnection),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (groupedMessages.isEmpty() && !initialMessagesLoaded) {
                        items(count = 3, key = { "chat_preview_skeleton_$it" }) { index ->
                            ChatPreviewSkeletonMaterial(isMe = index % 2 == 1)
                        }
                    }
                    items(groupedMessages, key = { it.message.id }, contentType = { it.message.type }) { item ->
                        SwipeToReplyRow(
                            messageId = item.message.id,
                            onReply = { onReplyRequest(item.message) },
                            enabled = !selectionMode,
                        ) {
                            ChatMessageItemMaterial(
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
                                showPlusOne = showPlusOne,
                                onPlusOne = onPlusOne,
                                onArkGroupNoticeClick = onArkGroupNoticeClick,
                            )
                        }
                    }
                }
            }
            AnimatedReplyBar(target = replyTarget) { target ->
                ReplyComposerBarMaterial(target = target, onClear = onClearReply)
            }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { composerBarHeightPx = it.height },
        ) {
            ChatInputBarMaterial(
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
            // 表情面板：与参考图一致，位于输入栏下方（占据键盘位）。
            AnimatedVisibility(
                visible = composer.panel == ChatPanel.Emoji,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
                ChatEmojiPanelMaterial(
                    composer = composer,
                    onSendCustomEmoji = onSendCustomEmoji,
                    onSendStickerPackEmoji = onSendStickerPackEmoji,
                )
            }
            // 附件面板：输入栏下方的就地展开面板，把消息列表向上顶。
            AnimatedVisibility(
                visible = composer.panel == ChatPanel.Attach,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
                ChatAttachPanelMaterial(
                    hintVisible = composer.attachHintVisible,
                    onDismissHint = composer::dismissAttachHint,
                    onAction = { action ->
                        composer.closePanels()
                        onAttachAction(action)
                    },
                    onCameraAction = { isVideo ->
                        composer.closePanels()
                    // 与点击 Unicode emoji 一致：把表情作为 `[描述]` 文本插入输入框，
                    // 发送时由 bridge 解析成 FaceElement（不直接发出）。
                    onSendSuperEmoji = { item -> composer.appendEmoji("[超级:${item.desc}]") },
                    onSendSystemFace = { item -> composer.appendEmoji("[${item.desc}]") },
                        onCameraAction(isVideo)
                    },
                )
            }
            }
        }
        // 右下角浮动按钮层：底部距离 = 输入栏高度 + margin（与右侧 margin 相同）。
        ChatFloatingActionsMaterial(
            bottomButtonVisible = canScrollForward,
            jumpVisible = hasJumpTargets,
            bottomOffsetPx = composerBarHeightPx,
            keyboardBottomInset = keyboardHeightState,
            onJumpEvent = {
                // 从最新一条（@我 或 msgBox 事件）开始，逐条向更早遍历。
                val target = jumpTargets.getOrNull(jumpTargets.lastIndex - jumpCursor)
                if (target != null) {
                    jumpCursor += 1
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
        // 表情 / 附件面板展开时，把面板底色延伸进系统导航栏（小白条）区域：
        // 面板本身在导航栏 inset 之上结束，若不补这条底色，导航栏区域会露出页面背景色，视觉上"不沉浸"。
        if (composer.panel != ChatPanel.None) {
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            if (navBottom > 0.dp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(navBottom)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                )
            }
        }
        }
    }
    }
}

@Composable
private fun ChatTopBarTitleMaterial(chat: ChatItem) {
    val subtitle = chatSubtitle(chat)
    Column {
        Text(
            text = chat.name,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 一条消息：时间分隔 + （系统消息 | 分组气泡行）。 */
@Composable
internal fun ChatMessageItemMaterial(
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
            TimeSeparatorMaterial(separator)
        }
        if (message.type == ChatMessageType.SYSTEM) {
            SystemMessageMaterial(message.content)
        } else {
            MessageRowMaterial(
                message = message,
                group = item.group,
                onReplyQuoteClick = onReplyQuoteClick,
                plusOne = message.chatRecordMsgId in plusOneMessageIds,
                onFileClick = onFileClick,
    showPlusOne: Boolean = false,
    onPlusOne: (ChatMessage) -> Unit = {},
                downloadingFileMsgId = downloadingFileMsgId,
                fileDownloadProgress = fileDownloadProgress,
                canRecallOthers = canRecallOthers,
                canGroupAtSender = canGroupAtSender,
                canAddToFavorite = canAddToFavorite,
                onAvatarClick = onAvatarClick,
                showPlusOne = showPlusOne,
                onPlusOne = onPlusOne,
                isGroupChat = isGroupChat,
                selfGroupName = selfGroupName,
                onArkGroupNoticeClick = onArkGroupNoticeClick,
            )
        }
    }
}

@Composable
private fun MessageRowMaterial(
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
    val colorScheme = MaterialTheme.colorScheme
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
            ChatAvatarSlotMaterial(
                message = message,
                group = group,
    showPlusOne: Boolean = false,
    onPlusOne: (ChatMessage) -> Unit = {},
                trailing = false,
                onAvatarClick = onAvatarClick,
            )
        }
        // 多选：自己的消息 checkbox 放整行最左侧，气泡仍贴右（用 weight 占位推开）。
        if (isMe && selection?.active == true) {
            MaterialSelectionCheckbox(
                checked = message.chatRecordMsgId in selection.selectedIds,
                onToggle = { selection.onToggle(message) },
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Spacer(Modifier.weight(1f))
        }
        Column(
            // 284dp 是气泡最大宽度；开启双击加一时右侧/左侧额外放 34dp 的 +1 圆钮。
            modifier = Modifier.widthIn(max = 284.dp + 34.dp),
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
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
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
                ReplyQuoteMaterial(message, onClick = { onReplyQuoteClick(message) })
            }
            val bubbleContent: @Composable () -> Unit = {
                // 多选模式下不挂长按菜单（点击整行只用于选择），直接渲染气泡。
                if (selectionActive) {
                    ChatBubbleMaterial(
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
                // 不再显示 +1 成功后的「+1」badge（按需求移除）。
                MessageMetaMaterial(message = message)
                } else {
                    BubbleActionMenuHost(
                        message = message,
                        canRecallOthers = canRecallOthers,
                        canAtSender = canGroupAtSender,
                        canAddToFavorite = canAddToFavorite,
                    ) {
                        ChatBubbleMaterial(
                            message = message,
                            position = group.position,
                            onFileClick = onFileClick,
                            downloadingFileMsgId = downloadingFileMsgId,
                            fileDownloadProgress = fileDownloadProgress,
                            onArkGroupNoticeClick = onArkGroupNoticeClick,
                        )
                    }
                }
            }
            if (showPlusOne && !selectionActive) {
                // 自己的气泡 +1 在左，对方气泡 +1 在右；圆钮与气泡中心线对齐（不含时间/已读行）。
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMe) {
                        PlusOneCircleMaterial(plusOne = plusOne, onClick = { onPlusOne(message) })
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(modifier = Modifier.widthIn(max = 284.dp)) { bubbleContent() }
                    if (!isMe) {
                        Spacer(Modifier.width(8.dp))
                        PlusOneCircleMaterial(plusOne = plusOne, onClick = { onPlusOne(message) })
                    }
            }
        }
        // 多选：对方的消息 checkbox 放整行最右侧，头像+气泡仍贴左。
        if (!isMe && selection?.active == true) {
            Spacer(Modifier.weight(1f))
            MaterialSelectionCheckbox(
                checked = message.chatRecordMsgId in selection.selectedIds,
                onToggle = { selection.onToggle(message) },
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
        if (isMe) {
            ChatAvatarSlotMaterial(
                message = message,
                group = group,
                trailing = true,
                onAvatarClick = onAvatarClick,
            )
        }
    }
}

@Composable
private fun MaterialSelectionCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Checkbox(
        checked = checked,
        onCheckedChange = { onToggle() },
/** 气泡右侧的「+1」圆钮（双击加一开关开启时显示）。 */
@Composable
private fun PlusOneCircleMaterial(plusOne: Boolean, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        // 双击 +1 圆钮才复读（单击不触发）。
        modifier = Modifier
            .size(26.dp)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onClick() })
            },
        shape = CircleShape,
        color = colorScheme.primaryContainer,
        contentColor = colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "+1",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
            )
        }
    }
}

        modifier = modifier,
    )
}

/** 头像只出现在分组最后一条；其余消息保留同宽占位，保证同组气泡左/右对齐。 */
@Composable
private fun ChatAvatarSlotMaterial(
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
private fun MessageMetaMaterial(message: ChatMessage, plusOne: Boolean = false) {
    val colorScheme = MaterialTheme.colorScheme
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
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
            )
        }
        if (plusOne) {
            PlusOneTagMaterial()
        }
    }
}

/**
 * 「+1」本地反馈标记：只表示本次会话里自己成功 +1 过该消息（不落库），
 * 样式小巧，贴在时间/已读旁边，不影响气泡分组布局。
 */
@Composable
private fun PlusOneTagMaterial(modifier: Modifier = Modifier) {
    Text(
        text = "+1",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

@Composable
private fun ChatBubbleMaterial(
    message: ChatMessage,
    position: ChatBubblePosition,
    onFileClick: (ChatMessage) -> Unit = {},
    downloadingFileMsgId: Long = 0L,
    fileDownloadProgress: Int = -1,
    canRecallOthers: Boolean = false,
    onArkGroupNoticeClick: (ChatMessage) -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val isMe = message.isMe
    val container = if (isMe) colorScheme.primary else colorScheme.surfaceContainerHighest
    val content = if (isMe) colorScheme.onPrimary else colorScheme.onSurface
    if (message.parts.isNotEmpty()) {
        // 多元素消息（文本+表情+图片混排）：按 parts 顺序渲染，避免只显示第一个元素丢图。
        MultiPartBubbleMaterial(message, container, content, position)
    } else when (message.type) {
        ChatMessageType.TEXT -> TextBubbleMaterial(message, container, content, position)
        ChatMessageType.IMAGE -> ImageBubbleMaterial(message, position)
        ChatMessageType.FILE -> FileBubbleMaterial(
            message = message,
            container = container,
            content = content,
            position = position,
            onClick = { onFileClick(message) },
            downloading = message.chatRecordMsgId != 0L &&
                message.chatRecordMsgId == downloadingFileMsgId,
            downloadProgress = fileDownloadProgress,
        )
        ChatMessageType.VOICE -> VoiceBubbleMaterial(message, container, content, position)
        ChatMessageType.RED_PACKET -> RedPacketBubbleMaterial(message, position)
        ChatMessageType.LINK -> LinkBubbleMaterial(message, container, content, position)
        ChatMessageType.CHAT_RECORD -> ChatRecordBubbleMaterial(message, container, content, position)
        ChatMessageType.VIDEO -> VideoBubbleMaterial(message, position)
        ChatMessageType.ARK -> ArkCardBubbleMaterial(
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
private fun TextBubbleMaterial(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    Surface(
        shape = chatBubbleShape(position, message.isMe),
        color = container,
        contentColor = content,
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val uriHandler = remember(context) { openLinkHandler(context) }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalUriHandler provides uriHandler,
        ) {
            Text(
                text = linkifyContent(
                    message.content,
                    if (message.isMe) content else MaterialTheme.colorScheme.primary,
                ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

/**
 * 多元素消息气泡：外层沿用普通气泡形状/颜色，内部按 [ChatMessage.parts] 顺序竖排。
 * 文本走 linkifyContent（链接可点），图片宽 ≤200dp / 表情图约 110dp（无气泡底），无路径时显示占位。
 */
@Composable
private fun MultiPartBubbleMaterial(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    val navigator = LocalNavigator.current
    val colorScheme = MaterialTheme.colorScheme
    val imageLabel = stringResource(R.string.chat_image_message)
    val gradient = remember(colorScheme.secondaryContainer, colorScheme.tertiaryContainer) {
        Brush.linearGradient(listOf(colorScheme.secondaryContainer, colorScheme.tertiaryContainer))
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = remember(context) { openLinkHandler(context) }
    Surface(
        shape = chatBubbleShape(position, message.isMe),
        color = container,
        contentColor = content,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalUriHandler provides uriHandler,
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
        ChatMessageType.CALL -> CallBubbleMaterial(message, container, content, position)
        ChatMessageType.SUPER_EMOJI -> SuperEmojiBubbleMaterial(message, container, content, position)
        ChatMessageType.SYS_FACE -> SystemFaceBubbleMaterial(message, container, content, position)
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                message.parts.forEach { part ->
                    when (part.kind) {
                        ChatPartKind.TEXT -> Text(
                            text = linkifyContent(
                                part.text,
                                if (message.isMe) content else colorScheme.primary,
                            ),
                            style = MaterialTheme.typography.bodyLarge,
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
/** 「文字 + QQ 小黄脸」行内气泡：表情用 InlineTextContent 与文字同样的行高显示。 */
@Composable
private fun InlinePartsBubbleMaterial(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    parts: List<ChatPart> = message.parts,
) {
    val annotated = remember(parts) {
        buildAnnotatedString {
            parts.forEachIndexed { index, part ->
                when (part.kind) {
                    ChatPartKind.TEXT -> append(part.text)
                    ChatPartKind.IMAGE ->
                        if ((part.isSystemFace && part.imagePath != null) ||
                            part.isSuperEmoji
                        ) {
                            appendInlineContent(
                                id = "face_$index",
                                alternateText = part.faceDescription.ifBlank { "[表情]" },
                            )
                        } else {
                            append("[图片]")
                        }
                }
            }
        }
    }
    val inlineContent = remember(parts) {
        parts.mapIndexedNotNull { index, part ->
            if (part.kind != ChatPartKind.IMAGE) return@mapIndexedNotNull null
            val content: (@Composable () -> Unit)? = when {
                part.isSystemFace && part.imagePath != null -> {
                    {
                        AnimatedOrStaticImage(
                            path = part.imagePath,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                part.isSuperEmoji -> {
                    {
                        SuperEmojiAnimation(
                            lottiePath = part.superEmojiLottiePath,
                            imagePath = part.superEmojiImagePath,
                            contentDescription = part.faceDescription,
                            fallbackText = "🙂",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                else -> null
            }
            if (content == null) {
                null
            } else {
                "face_$index" to InlineTextContent(
                    Placeholder(
                        width = 20.sp,
                        height = 20.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    ),
                ) { content() }
            }
        }.toMap()
    }
    Surface(
        shape = chatBubbleShape(position, message.isMe),
        color = container,
        contentColor = content,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalUriHandler provides uriHandler,
        ) {
            Text(
                text = annotated,
                inlineContent = inlineContent,
                style = MaterialTheme.typography.bodyLarge,
                color = content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

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
                                        imageVector = Icons.Filled.Image,
                                        contentDescription = imageLabel,
                                        tint = if (emojiPart) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                                        },
                                        modifier = Modifier.size(if (emojiPart) 26.dp else 38.dp),
                                    )
                                    if (!emojiPart) {
                                        Text(
                                            text = part.text.ifBlank { imageLabel },
                                            style = MaterialTheme.typography.labelMedium,
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageBubbleMaterial(message: ChatMessage, position: ChatBubblePosition) {
    // 收藏表情 / 表情包图片：不套图片气泡，按表情尺寸小图渲染（TIM 里的「表情」形态）。
    if (message.isEmojiImage) {
        EmojiImageBubbleMaterial(message)
        return
    }
    val navigator = LocalNavigator.current
    val colorScheme = MaterialTheme.colorScheme
    val gradient = remember(colorScheme.secondaryContainer, colorScheme.tertiaryContainer) {
        Brush.linearGradient(listOf(colorScheme.secondaryContainer, colorScheme.tertiaryContainer))
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
            .clip(chatBubbleShape(position, message.isMe))
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
                imageVector = Icons.Filled.Image,
                contentDescription = stringResource(R.string.chat_image_message),
                tint = colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                modifier = Modifier.size(38.dp),
            )
            Text(
                text = message.content,
                style = MaterialTheme.typography.labelMedium,
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
private fun EmojiImageBubbleMaterial(message: ChatMessage) {
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
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FileBubbleMaterial(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
    onClick: () -> Unit = {},
    downloading: Boolean = false,
    downloadProgress: Int = -1,
) {
    val colorScheme = MaterialTheme.colorScheme
    val iconBackground = if (message.isMe) {
        colorScheme.onPrimary.copy(alpha = 0.18f)
    } else {
        colorScheme.secondaryContainer
    }
    val iconTint = if (message.isMe) colorScheme.onPrimary else colorScheme.onSecondaryContainer
    // 已下载（本地文件存在）→ 打开图标；未下载/文件缺失 → 下载图标。
    val downloaded = !message.mediaPath.isNullOrBlank() && File(message.mediaPath).isFile
    Surface(
        onClick = onClick,
        shape = chatBubbleShape(position, message.isMe),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
                    imageVector = Icons.Filled.Description,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.widthIn(max = 190.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (message.fileSize.isNotEmpty()) {
                    Text(
                        text = message.fileSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = content.copy(alpha = 0.72f),
                    )
                }
                if (downloading) {
                    Spacer(Modifier.height(6.dp))
                    // M3E 大波浪：文件下载进度条高度 8dp、幅度 4dp。
                    WavyProgressBar(
                        progress = if (downloadProgress >= 0) {
                            downloadProgress / 100f
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = content,
                        trackColor = content.copy(alpha = 0.24f),
                        amplitude = 4.dp,
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
                        Icons.Filled.Download
                    },
                    contentDescription = null,
                    tint = content.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** 语音条：播放圆钮 + 可拖动进度条 + 时长；点击播放/暂停（全局同一时间只播一条）。 */
@Composable
private fun VoiceBubbleMaterial(
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
    Surface(
        onClick = {
            // 宿主播放是异步的：参数/文件问题同步返回 false；播放失败走回调 Toast。
            if (!VoicePlaybackController.toggle(message) {
                    Toast.makeText(context, playFailedText, Toast.LENGTH_SHORT).show()
                }
            ) {
                Toast.makeText(context, unavailableText, Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.widthIn(min = 160.dp),
        shape = chatBubbleShape(position, message.isMe),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.chat_voice),
                tint = content,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            // 中间常驻进度条：可拖动 seek（命中高度 24dp，视觉 10dp 大波浪）。
            DraggableWavyProgressBar(
                progress = progress,
                onSeekFinished = { fraction -> VoicePlaybackController.seekTo(message, fraction) },
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                color = content,
                trackColor = content.copy(alpha = 0.24f),
                amplitude = 4.dp,
                barHeight = 10.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.chat_voice_message, message.voiceSeconds),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
/** QQ 系统小黄脸：与「文字+表情」混排完全一致的行内渲染（单独一个表情也同高）。 */
@Composable
private fun SystemFaceBubbleMaterial(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    InlinePartsBubbleMaterial(
        message = message,
        container = container,
        content = content,
        position = position,
        uriHandler = remember(context) { openLinkHandler(context) },
        parts = listOf(
            ChatPart(
                kind = ChatPartKind.IMAGE,
                imagePath = message.imagePath,
                imageWidth = 1,
                imageHeight = 1,
                emojiImage = true,
                isSystemFace = true,
            ),
        ),
    )
}

/** 超级表情：与混排完全一致的行内渲染（单独一个表情也同高）。 */
@Composable
private fun SuperEmojiBubbleMaterial(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    InlinePartsBubbleMaterial(
        message = message,
        container = container,
        content = content,
        position = position,
        uriHandler = remember(context) { openLinkHandler(context) },
        parts = listOf(
            ChatPart(
                kind = ChatPartKind.IMAGE,
                imageWidth = 1,
                imageHeight = 1,
                emojiImage = true,
                isSuperEmoji = true,
                superEmojiLottiePath = message.superEmojiLottiePath,
                superEmojiImagePath = message.superEmojiImagePath,
                faceDescription = message.content,
            ),
        ),
    )
}

/** 通话记录气泡：语音/视频通话图标 + TIM 原生文案（如「通话时长 01:23」「对方已取消」）。 */
@Composable
private fun CallBubbleMaterial(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    Surface(
        shape = chatBubbleShape(position, message.isMe),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (message.callVideo) Icons.Filled.Videocam else Icons.Filled.Call,
                contentDescription = null,
                tint = content.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = message.content.ifBlank { if (message.callVideo) "视频通话" else "语音通话" },
                style = MaterialTheme.typography.bodyLarge,
                color = content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

    }
}

/**
 * 红包气泡：暖色卡片 + 真实祝福语/副标题/领取状态。
 * 有 bridge 数据（billNo + authkey）时可点击打开 TIM 原生领取；无数据保持旧样式且不可点。
 */
@Composable
private fun RedPacketBubbleMaterial(message: ChatMessage, position: ChatBubblePosition) {
    val context = LocalContext.current
    val canOpen = message.canOpenRedPacket()
    val grabbed = message.isRedPacketGrabbed()
    val subtitle = message.redPacketSubtitle
    // 旧样式（示例数据/无桥接字段）固定显示「红包」副标题，保持原视觉。
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
            .clip(chatBubbleShape(position, message.isMe))
            .background(MaterialTheme.colorScheme.primary)
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
            imageVector = Icons.Filled.Redeem,
            contentDescription = stringResource(R.string.chat_red_packet),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.content.ifBlank { stringResource(R.string.chat_red_packet) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitleLine.isNotEmpty()) {
                Text(
                    text = subtitleLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun LinkBubbleMaterial(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    Surface(
        shape = chatBubbleShape(position, message.isMe),
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = stringResource(R.string.chat_link_message),
                tint = content,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.widthIn(max = 200.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.chat_link_message),
                    style = MaterialTheme.typography.labelSmall,
                    color = content.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun TimeSeparatorMaterial(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun SystemMessageMaterial(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/** 输入栏：+ / 关闭附件面板、圆角输入框、表情、空态麦克风 / 非空发送按钮。 */
@Composable
private fun ChatInputBarMaterial(
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
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    // @/面板打开时收键盘 + 清焦点（标准做法），其余 IME 逻辑交给 ChatImeController。
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(
            onClick = onStartVoiceRecord,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = stringResource(R.string.chat_voice),
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }
        Surface(
            // 固定最小高度：空态占位符与有文本时输入框高度一致（多行时正常增高）。
            modifier = Modifier
                .weight(1f)
                .height(44.dp + ((lineCount.coerceIn(1, 4) - 1) * 24).dp)
                // 点击输入框区域（即使已聚焦但键盘被系统收起）重新请求键盘：不消费事件，不影响光标/长按。
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        if (!imeController.isImeVisibleNow()) {
                            imeController.requestShow("click")
                        }
                    }
                },
            shape = RoundedCornerShape(22.dp),
            color = colorScheme.surfaceContainerHighest,
            contentColor = colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = composer.text,
                    onValueChange = { newValue ->
                        // 输入 @ 唤起成员面板前先收起键盘：避免 ModalBottomSheet Dialog
                        // 抢焦点时 IME 闪烁（M3E），同时修复 Miuix overlay 不关键盘的问题。
                        val willOpenAt = isGroup &&
                            (newValue.endsWith("@") || newValue.endsWith("＠")) &&
                            newValue.length > composer.text.length
                        if (willOpenAt) {
                            // 保留 hide；同时清焦点——只 hide 不清焦点会进入
                            // 「focused 但 IME 已关」坏状态（系统 HIDE_SAME_WINDOW_FOCUSED_WITHOUT_EDITOR），
                            // 之后点击输入框无 focus 变化就不会重新请求 IME。
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            imeController.hideIme("panel")
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
        // 左侧：语音按钮（与 QQ/TIM 一致：语音在左、加号在右）。
                        } else {
                            imeController.onInputFocusLost()
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colorScheme.onSurface),
                    onTextLayout = { lineCount = it.lineCount },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    maxLines = 4,
                    cursorBrush = SolidColor(colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (composer.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.chat_input_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurfaceVariant,
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
                        tint = if (emojiOpen) colorScheme.primary else colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        // 右侧操作位固定占位（空态加号菜单 / 附件面板关闭 / 有文本发送），
        // 保证输入框宽度和输入栏高度不随文本变化。
        when {
            attachOpen -> IconButton(
                onClick = onAttachClick,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.chat_input_close),
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            composer.text.isEmpty() -> IconButton(
                onClick = onAttachClick,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.chat_more_actions),
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
            else -> FilledIconButton(
                onClick = submit,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.chat_send),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatRecordBubbleMaterial(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
) {
    val navigator = LocalNavigator.current
    // 共用点击逻辑：读取当前页 parent/root、打日志并构造 Route（聊天页 parent/root=0）。
    val onRecordClick = onChatRecordBubbleClick(message) { route -> navigator.push(route) }
    val count = message.chatRecordCount.coerceAtLeast(message.chatRecordSummary.size)
    Surface(
        shape = chatBubbleShape(position, message.isMe),
        color = container,
        contentColor = content,
        modifier = Modifier
            .widthIn(max = 252.dp)
            .clickable(onClick = onRecordClick),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = message.chatRecordTitle.ifBlank { stringResource(R.string.chat_record_title) },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            message.chatRecordSummary.take(4).forEach { line ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = stringResource(R.string.chat_record_view, count),
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.6f),
                maxLines = 1,
            )
        }
    }
}

/**
 * ARK 卡片气泡（小程序 / 结构化消息 / 分享卡片）：
 * 预览图 + 来源/标签行 + 标题 + 描述 + 页脚（prompt + 箭头）；点击跳转 arkJumpUrl。
 * 所有 ARK 字段都为空时退回普通文本气泡。
 */
@Composable
private fun ArkCardBubbleMaterial(
    message: ChatMessage,
    container: Color,
    content: Color,
    position: ChatBubblePosition,
    onArkGroupNoticeClick: (ChatMessage) -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val hasCardContent = message.arkSource.isNotBlank() ||
        message.arkTitle.isNotBlank() ||
        message.arkDesc.isNotBlank() ||
        message.arkTag.isNotBlank() ||
        message.arkPrompt.isNotBlank() ||
        message.arkJumpUrl.isNotBlank() ||
        !message.arkPreviewPath.isNullOrBlank()
    if (!hasCardContent) {
        TextBubbleMaterial(message, container, content, position)
        return
    }
    val previewBitmap = rememberLocalImage(message.arkPreviewPath, maxDimension = 720)
    val jumpUrl = message.arkJumpUrl
    val clickable = message.arkGroupNotice || jumpUrl.isNotBlank()
    Surface(
        modifier = Modifier
            .widthIn(max = 280.dp)
            .then(
                if (clickable) {
                    Modifier.clickable {
                        if (message.arkGroupNotice) {
                            // 群公告 ARK 卡片：进入本群群公告页，不按外部链接打开。
                            onArkGroupNoticeClick(message)
                        } else {
                            com.material3tim.ui.util.ExternalLink.open(context, jumpUrl)
                        }
                    }
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(16.dp),
        // 与普通气泡同色：自己=primary/onPrimary，别人=surfaceContainerHighest/onSurface。
        color = container,
        contentColor = content,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 预览图（有本地缓存才显示，不占位空白）
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
                // 来源 + 右上角标签
                if (message.arkSource.isNotBlank() || message.arkTag.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.arkSource,
                            style = MaterialTheme.typography.labelMedium,
                            color = content.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (message.arkTag.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = message.arkTag,
                                style = MaterialTheme.typography.labelSmall,
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
                        style = MaterialTheme.typography.titleSmall,
                        color = content,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (message.arkDesc.isNotBlank()) {
                    Text(
                        text = message.arkDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = content.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // 页脚：prompt（与 source 重复时用 source/app 短名）+ 箭头
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
                            style = MaterialTheme.typography.labelSmall,
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

/**
 * 视频气泡点击统一分支（M3E / Miuix 共用，行为与原先一致）：
 * 1) 本地 videoPath 是可读文件 → 本地播放；
 * 2) 无本地文件但 msgId/elementId 可查询内核 → 进播放页由 videoPlayUrl 拉在线 URL；
 * 3) 两者都没有 → Toast「视频暂无法播放」。
 */
internal fun openVideoMessage(
    context: Context,
    message: ChatMessage,
    thumbnailPath: String,
    onOpenPlayer: (Route.ChatVideo) -> Unit,
) {
    val localPath = message.videoPath
    val localPlayable = !localPath.isNullOrBlank() &&
        runCatching { File(localPath).isFile }.getOrDefault(false)
    // 记录页消息用 chatRecordMsgId 查内核；聊天页普通视频退回 msgId。
    val queryMsgId = message.chatRecordMsgId.takeIf { it != 0L } ?: message.msgId
    when {
        localPlayable -> onOpenPlayer(
            Route.ChatVideo(
                videoPath = localPath.orEmpty(),
                thumbnailPath = thumbnailPath,
                chatId = message.chatId,
                msgId = queryMsgId,
                elementId = message.elementId,
                videoWidth = message.imageWidth,
                videoHeight = message.imageHeight,
            ),
        )
        queryMsgId != 0L && message.elementId != 0L -> onOpenPlayer(
            Route.ChatVideo(
                videoPath = "",
                thumbnailPath = message.videoThumbPath.orEmpty(),
                chatId = message.chatId,
                msgId = queryMsgId,
                elementId = message.elementId,
                videoWidth = message.imageWidth,
                videoHeight = message.imageHeight,
            ),
        )
        else -> Toast.makeText(
            context,
            R.string.chat_video_cannot_play,
            Toast.LENGTH_SHORT,
        ).show()
    }
}

@Composable
private fun VideoBubbleMaterial(message: ChatMessage, position: ChatBubblePosition) {
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
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val placeholderContent = MaterialTheme.colorScheme.onSurfaceVariant
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
            .clip(chatBubbleShape(position, message.isMe))
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
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.chat_video_title),
                    tint = if (bitmap != null) Color.White else placeholderContent,
                    modifier = Modifier.size(if (hasMetadata) 28.dp else 20.dp),
                )
            }
            if (!hasMetadata) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.chat_video_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = placeholderContent,
                )
            }
        }
        if (message.videoSeconds > 0) {
            Text(
                text = formatVideoDuration(message.videoSeconds),
                style = MaterialTheme.typography.labelSmall,
                color = if (bitmap != null) Color.White else placeholderContent,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
            )
        }
        if (bitmap == null && hasMetadata && message.fileSize.isNotBlank()) {
            Text(
                text = message.fileSize,
                style = MaterialTheme.typography.labelSmall,
                color = placeholderContent,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
        }
    }
}

/** 引用预览（气泡上方）。 */
@Composable
private fun ReplyQuoteMaterial(message: ChatMessage, onClick: () -> Unit = {}) {
    val colorScheme = MaterialTheme.colorScheme
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
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 输入栏上方的「正在回复」条。 */
@Composable
private fun ReplyComposerBarMaterial(target: ChatMessage, onClear: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
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
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = target.content.ifBlank { "[消息]" },
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 长按菜单动作处理：复制/引用/删除/转发/+1 由页面实现，未识别动作保留兜底提示。 */
private fun handleMessageMenuActionMaterial(
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
private fun ChatPreviewSkeletonMaterial(isMe: Boolean) {
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
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    }
}

/** 聊天页右下角浮动按钮层：向上箭头（@我 + 事件消息，逐条向更早跳）/ 回到最底部。 */
@Composable
private fun ChatFloatingActionsMaterial(
    bottomButtonVisible: Boolean,
    jumpVisible: Boolean,
    bottomOffsetPx: Int,
    keyboardBottomInset: State<Dp>,
    onJumpEvent: () -> Unit,
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
                // 底部 inset 取「导航栏高度 / 键盘高度」的较大值，保证按钮到输入框上边缘
                // 与到屏幕右侧的间距一致（16dp）。
                val bottomInset = maxOf(keyboardBottomInset.value, navBottomDp)
                IntOffset(0, -(bottomOffsetPx + bottomInset.roundToPx() + marginPx))
            }
        }
        AnimatedVisibility(
            visible = bottomButtonVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
        ) {
            SmallFloatingActionButton(
                onClick = onBackToBottom,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.chat_back_to_bottom),
                )
            }
        }
    }
}
        AnimatedVisibility(
            visible = jumpVisible,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(y = jumpOffset),
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
        ) {
            SmallFloatingActionButton(onClick = onJumpEvent, shape = CircleShape) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.chat_jump_to_event),
                )
            }
        }
