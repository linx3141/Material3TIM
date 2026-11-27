package com.material3tim.ui.screen.chat

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.ContentValues
import android.content.Intent
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.ChatInfoData
import com.material3tim.data.model.ChatItem
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.model.ChatMember
import com.material3tim.data.model.CustomEmoji
import com.material3tim.data.model.ChatMessageType
import com.material3tim.data.model.ChatPartKind
import com.material3tim.data.repository.ChatRepository
import com.material3tim.data.repository.LiveChats
import com.material3tim.data.repository.MessageRepository
import com.material3tim.data.source.AppDataSources
import com.material3tim.data.source.AtTarget
import com.material3tim.data.source.DataUpdate
import com.material3tim.data.source.LiveEvents
import com.material3tim.ui.component.ime.ChatImeController
import com.material3tim.ui.component.VoicePlaybackController
import com.material3tim.ui.screen.chat.ChatAttachAction
import java.io.File
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 聊天详情页入口：先解析会话（最近会话命中 → 直接渲染；否则异步 loadChatInfoFast/loadChatInfo
 * 构造临时 ChatItem，覆盖群助手二级页里不在最近会话列表的群），再交给 [ChatDetailContent]。
 *
 * 导航约定：MainActivity 通过 `Route.ChatDetail(chatId)` 调用，签名保持不变。
 */
@Composable
fun ChatDetailScreen(
    chatId: String,
    jumpToSeq: Long = 0L,
    jumpToMsgId: Long = 0L,
) {
    val navigator = LocalNavigator.current
    var chat by remember(chatId) { mutableStateOf(ChatRepository.chatById(chatId)) }
    var loading by remember(chatId) { mutableStateOf(chat == null) }
    var failed by remember(chatId) { mutableStateOf(false) }
    var retryTick by remember(chatId) { mutableIntStateOf(0) }

    LaunchedEffect(chatId, retryTick) {
        if (chat != null) return@LaunchedEffect
        loading = true
        failed = false
        val source = AppDataSources.chatSource
        if (source == null) {
            failed = true
            loading = false
            return@LaunchedEffect
        }
        // 先快路径（毫秒级缓存），再全量兜底；群助手群可能只在这条路径可见。
        var info = withContext(Dispatchers.IO) {
            runCatching { source.loadChatInfoFast(chatId) }.getOrNull()
        }
        if (info == null) {
            info = withContext(Dispatchers.IO) {
                runCatching { source.loadChatInfo(chatId) }.getOrNull()
            }
        }
        val resolved = info?.toTemporaryChatItem(chatId)
        if (resolved != null) {
            chat = resolved
            failed = false
        } else {
            failed = true
        }
        loading = false
    }

    val resolvedChat = chat
    if (resolvedChat == null) {
        ChatDetailPlaceholder(
            loading = loading,
            failed = failed,
            onBack = { navigator.pop() },
            onRetry = { retryTick += 1 },
        )
        return
    }
    ChatDetailContent(
        chat = resolvedChat,
        chatId = chatId,
        jumpToSeq = jumpToSeq,
        jumpToMsgId = jumpToMsgId,
    )
}

/** 临时 ChatItem：chat 不在最近会话列表（如群助手页的群）时，用 loadChatInfo 结果构造。 */
private fun ChatInfoData.toTemporaryChatItem(chatId: String): ChatItem = ChatItem(
    id = chatId,
    name = chat.name,
    lastMessage = "",
    timeLabel = "",
    unreadCount = 0,
    isPinned = false,
    isMuted = chat.isMuted,
    isGroup = chat.isGroup,
    avatarSeed = chat.avatarSeed,
    memberCount = effectiveMemberCount(this),
    isOnline = chat.isOnline,
    avatarPath = chat.avatarPath,
)

/** 聊天详情页正文（会话已解析）。 */
@Composable
private fun ChatDetailContent(
    chat: ChatItem,
    chatId: String,
    jumpToSeq: Long,
    jumpToMsgId: Long,
) {
    val navigator = LocalNavigator.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val focusManager = LocalFocusManager.current
    val view = LocalView.current
    // IME 补丁统一收敛到 ChatImeController（行为基线 v0.5.188），页面只做 attach/clearFocus/通知。
    val imeController = remember { ChatImeController(context) }
    androidx.compose.runtime.SideEffect { imeController.attach(view) }
    fun clearInputFocus() {
        focusManager.clearFocus(force = true)
    }
    // 标题返回：清焦点 + 收起 IME 后 pop（系统返回手势由系统先收键盘，不特殊处理）。
    val onBack: () -> Unit = {
        clearInputFocus()
        imeController.hideIme("title-back")
        navigator.pop()
    }

    val meName = stringResource(R.string.chat_sender_me)
    val liveMessages = AppDataSources.hasLiveMessages
    val scope = rememberCoroutineScope()
    // 进入聊天页/切换会话：先清掉上一个页面或会话残留的输入焦点与输入连接，
    // 避免「A 聊天输入后退出→进 B 点输入框无法输入」。进入不自动弹键盘（show 逻辑只在点击/焦点时触发）。
    LaunchedEffect(chat.id) {
        // 进入/切换会话：清焦点 + 通知 controller 切到新会话（重置首次焦点重绑标志）；
        // 进入页面不主动 show，用户点输入框才由 controller 走 show 阶梯。
        clearInputFocus()
        imeController.noteChatChanged(chat.id)
    }
    // 横滑退出/任何 pop：本页不再是返回栈栈顶时立即收键盘（早于 dispose，覆盖横滑 dismiss 过渡动画），
    // 标题返回/系统返回同样经过此路径；再次压栈（如打开聊天信息页）时也会收起键盘，行为符合预期。
    val currentTopRoute = navigator.current()
    val isTopChatRoute = currentTopRoute is Route.ChatDetail && currentTopRoute.chatId == chatId
    LaunchedEffect(isTopChatRoute) {
        if (!isTopChatRoute) {
            clearInputFocus()
            imeController.onRouteLeaving()
        }
    }
    // 页面被压入返回栈/后台（ON_PAUSE）时也清焦点收 IME，避免回到旧聊天时残留焦点/输入连接。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, chat.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                clearInputFocus()
                imeController.hideIme("on-pause")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // 记录当前会话（后台提醒用它判断：正在看的会话不再发通知）；离开/切会话时停止语音播放并收 IME。
    DisposableEffect(chat.id) {
        com.material3tim.data.source.AppUiState.currentChatId = chat.id
        onDispose {
            if (com.material3tim.data.source.AppUiState.currentChatId == chat.id) {
                com.material3tim.data.source.AppUiState.currentChatId = null
            }
            VoicePlaybackController.stop()
            // 兜底退出路径：页面销毁时清焦点并取消 controller 内部协程。
            clearInputFocus()
            imeController.onScreenDisposed()
        }
    }
    var baseMessages by remember(chat.id) {
        // 同步读内存预览缓存（零 IO）：导航转场第一帧就能渲染上次的消息气泡。
        mutableStateOf(
            if (liveMessages) {
                ChatMessagePreviewCache.get(chat.id)
            } else {
                MessageRepository.messagesFor(chat)
            },
        )
    }
    var localMessages by remember(chat.id) { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var localSeq by remember(chat.id) { mutableIntStateOf(0) }

    // 「加载更早消息」：olderMessages 为已加载的历史（升序），与最新一页合并去重。
    var olderMessages by remember(chat.id) { mutableStateOf<List<ChatMessage>>(emptyList()) }
    // 「查找聊天记录」跳转要等首屏消息加载完再定位，否则会被首屏「滚到底部」覆盖。
    var initialMessagesLoaded by remember(chat.id) { mutableStateOf(false) }
    var isLoadingOlder by remember(chat.id) { mutableStateOf(false) }
    var hasMoreOlder by remember(chat.id) { mutableStateOf(true) }
    val messages = remember(baseMessages, localMessages, olderMessages) {
        mergeHistoryMessages(olderMessages, baseMessages + localMessages)
    }

    // 静默加载一页更早历史（不经过下拉刷新 UI，不产生下拉动画）。
    suspend fun loadOlderPage(quiet: Boolean): Boolean {
        if (!liveMessages || isLoadingOlder || !hasMoreOlder) return false
        isLoadingOlder = true
        val older = withContext(Dispatchers.IO) {
            AppDataSources.messageSource?.loadOlderMessages(chat.id)
        }
        val grew = older != null && older.isNotEmpty() && older.size > olderMessages.size
        when {
            older == null -> hasMoreOlder = false
            older.isEmpty() || older.size == olderMessages.size -> {
                hasMoreOlder = false
                if (!quiet && olderMessages.isNotEmpty()) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.chat_load_older_empty),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
            else -> olderMessages = older
        }
        isLoadingOlder = false
        return grew
    }

    val onLoadOlder: () -> Unit = {
        if (liveMessages && !isLoadingOlder && hasMoreOlder) {
            scope.launch { loadOlderPage(quiet = false) }
        }
    }

    // 点击引用条：静默加载历史直到找到被引用消息，再让聊天页滚动定位。
    var pendingJumpMessage by remember(chat.id) { mutableStateOf<ChatMessage?>(null) }

    // 从「查找聊天记录」点进来：按 seq/msgId 直接取目标附近一段历史并定位。
    androidx.compose.runtime.LaunchedEffect(chat.id, jumpToSeq, jumpToMsgId, initialMessagesLoaded) {
        if ((jumpToSeq <= 0L && jumpToMsgId <= 0L) || !liveMessages) return@LaunchedEffect
        // 等首屏消息先加载完，避免跳转后被「进入聊天滚到底部」覆盖。
        if (!initialMessagesLoaded) return@LaunchedEffect
        val chunk = withContext(Dispatchers.IO) {
            if (jumpToSeq > 0L) {
                AppDataSources.messageSource?.loadHistoryAround(chat.id, jumpToSeq, 80)
            } else {
                null
            }
        }.orEmpty()
        val target = chunk.firstOrNull {
            (jumpToSeq > 0L && it.seq == jumpToSeq) ||
                (jumpToMsgId > 0L && it.chatRecordMsgId == jumpToMsgId)
        } ?: (baseMessages + olderMessages).firstOrNull {
            (jumpToSeq > 0L && it.seq == jumpToSeq) ||
                (jumpToMsgId > 0L && it.chatRecordMsgId == jumpToMsgId)
        }
        if (target != null) {
            if (chunk.isNotEmpty()) {
                olderMessages = (olderMessages + chunk)
                    .distinctBy { it.chatRecordMsgId }
                    .sortedBy { it.timestampMs }
            }
            pendingJumpMessage = target
        } else {
            android.widget.Toast.makeText(
                context,
                "没有找到该聊天记录",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // ---------- 多选 / 转发 ----------
    var selectionMode by remember(chat.id) { mutableStateOf(false) }
    var selectedMessageIds by remember(chat.id) { mutableStateOf<Set<Long>>(emptySet()) }
    val onToggleSelectMessage: (ChatMessage) -> Unit = { message ->
        val id = message.chatRecordMsgId
        if (id != 0L) {
            selectedMessageIds = if (id in selectedMessageIds) {
                selectedMessageIds - id
            } else {
                selectedMessageIds + id
            }
        }
    }
    val onExitSelection: () -> Unit = {
        selectionMode = false
        selectedMessageIds = emptySet()
    }
    val onEnterSelection: (ChatMessage) -> Unit = { message ->
        selectionMode = true
        val id = message.chatRecordMsgId
        if (id != 0L) selectedMessageIds = selectedMessageIds + id
    }
    val onForwardSelected: () -> Unit = {
        val ids = selectedMessageIds.toList()
        if (ids.isNotEmpty()) {
            onExitSelection()
            navigator.push(Route.Forward(chat.id, ids))
        }
    }

    // ---------- 长按菜单：单条转发 / +1 ----------
    // 单条消息转发：携带来源摘要进入转发页，顶部预览卡展示「转发 1 条消息 + 摘要首行」。
    val forwardImageLabel = stringResource(R.string.chat_image_message)
    val forwardVideoLabel = "[" + stringResource(R.string.chat_video_title) + "]"
    val forwardVoiceLabel = "[" + stringResource(R.string.chat_voice) + "]"
    val forwardFileLabel = stringResource(R.string.chat_file_message)
    val forwardRedPacketLabel = "[" + stringResource(R.string.chat_red_packet) + "]"
    val forwardLinkLabel = stringResource(R.string.chat_link_message)
    val forwardRecordLabel = "[" + stringResource(R.string.chat_record_title) + "]"
    val forwardUnsupportedLabel = stringResource(R.string.chat_forward_unsupported)
    val forwardPreviewOf: (ChatMessage) -> String = { message ->
        message.content.trim().ifEmpty {
            when (message.type) {
                ChatMessageType.IMAGE -> forwardImageLabel
                ChatMessageType.VIDEO -> forwardVideoLabel
                ChatMessageType.VOICE -> forwardVoiceLabel
                ChatMessageType.FILE -> forwardFileLabel
                ChatMessageType.RED_PACKET -> forwardRedPacketLabel
                ChatMessageType.LINK -> forwardLinkLabel
                ChatMessageType.CHAT_RECORD -> forwardRecordLabel
                else -> ""
            }
        }.take(200)
    }
    val onForwardMessage: (ChatMessage) -> Unit = { message ->
        val id = message.chatRecordMsgId
        if (id != 0L) {
            navigator.push(Route.Forward(chat.id, listOf(id), preview = forwardPreviewOf(message)))
        } else {
            android.widget.Toast.makeText(
                context,
                forwardUnsupportedLabel,
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // +1（表情回应）本地反馈：只记录"自己在本页成功 +1 过的消息 id"，用于气泡旁的小「+1」标记；
    // 不落库，退出页面即失效；内核调用是阻塞的，统一放 IO，成功后才点亮标记，避免假反馈。
    var plusOneMessageIds by remember(chat.id) { mutableStateOf<Set<Long>>(emptySet()) }
    var plusOneInFlight by remember(chat.id) { mutableStateOf<Set<Long>>(emptySet()) }
    val plusOneDoneLabel = stringResource(R.string.chat_plus_one_done)
    val plusOneAlreadyLabel = stringResource(R.string.chat_plus_one_already)
    val plusOneUnsupportedLabel = stringResource(R.string.chat_plus_one_unsupported)
    val plusOneUnavailableLabel = stringResource(R.string.chat_plus_one_unavailable)
    val plusOneFailedLabel = stringResource(R.string.chat_plus_one_failed)
    val showToast: (String) -> Unit = { text ->
        android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
    }
    val onPlusOne: (ChatMessage) -> Unit = { message ->
        val msgId = message.chatRecordMsgId
        val source = AppDataSources.messageSource
        when {
            msgId == 0L -> showToast(plusOneUnsupportedLabel)
            msgId in plusOneMessageIds -> showToast(plusOneAlreadyLabel)
            // 同一消息的请求还在飞：忽略重复点击，避免并发重复 +1。
            msgId in plusOneInFlight -> Unit
            source == null -> showToast(plusOneUnavailableLabel)
            else -> {
                plusOneInFlight = plusOneInFlight + msgId
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching { source.sendPlusOne(chat.id, msgId) }.getOrDefault(false)
                    }
                    plusOneInFlight = plusOneInFlight - msgId
                    if (ok) {
                        plusOneMessageIds = plusOneMessageIds + msgId
                        showToast(plusOneDoneLabel)
                    } else {
                        showToast(plusOneFailedLabel)
                    }
                }
            }
        }
    }
    var jumpInProgress by remember(chat.id) { mutableStateOf(false) }
    val onReplyQuoteClick: (ChatMessage) -> Unit = { source ->
        val targetId = source.replyToMsgId
        val targetSeq = source.replyToSeq
        if (targetId == 0L && targetSeq == 0L) {
            android.widget.Toast.makeText(context, "无法定位原消息", android.widget.Toast.LENGTH_SHORT).show()
        } else if (jumpInProgress) {
            // 已有跳转在进行中，忽略重复点击。
        } else {
            jumpInProgress = true
            scope.launch {
                fun List<ChatMessage>.containsTarget(): Boolean = any {
                    (targetId != 0L && it.chatRecordMsgId == targetId) ||
                        (targetSeq > 0L && it.seq == targetSeq)
                }
                // 先看已加载的
                var allOlder = olderMessages
                var found = baseMessages.containsTarget() || allOlder.containsTarget()
                if (!found && targetSeq > 0L) {
                    // 按引用里的 seq 直接取目标附近一段历史（快，不用逐页翻）。
                    val chunk = withContext(Dispatchers.IO) {
                        AppDataSources.messageSource?.loadHistoryAround(chat.id, targetSeq, 80)
                    }.orEmpty()
                    if (chunk.isNotEmpty() && chunk.containsTarget()) {
                        found = true
                        allOlder = (allOlder + chunk)
                            .distinctBy { it.chatRecordMsgId }
                            .sortedBy { it.timestampMs }
                    }
                }
                if (!found) {
                    android.widget.Toast.makeText(
                        context,
                        "正在定位原消息…",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                    // 兜底：静默逐页向上取历史（中间不更新 UI，避免列表反复跳动/下拉动画）。
                    var exhausted = false
                    var guard = 0
                    val deadline = System.currentTimeMillis() + 8000L
                    while (!found && !exhausted && guard < 200 &&
                        System.currentTimeMillis() < deadline
                    ) {
                        val next = withContext(Dispatchers.IO) {
                            AppDataSources.messageSource?.loadOlderMessages(chat.id, count = 100)
                        }
                        when {
                            next == null || next.isEmpty() || next.size == allOlder.size -> exhausted = true
                            else -> allOlder = next
                        }
                        found = allOlder.containsTarget() || baseMessages.containsTarget()
                        guard++
                    }
                    if (!found && exhausted) hasMoreOlder = false
                }
                if (found) {
                    // 只有找到目标时才更新列表并跳转，避免找不到时把列表顶上去。
                    if (allOlder.isNotEmpty()) olderMessages = allOlder
                    pendingJumpMessage = source
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "没有找到这条引用消息（可能已被删除）",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
                jumpInProgress = false
            }
        }
    }

    // 进入聊天即把该会话标记为已读（TIM 原生行为）：内核置已读，同时刷新会话列表角标。
    androidx.compose.runtime.LaunchedEffect(chat.id, liveMessages) {
        if (!liveMessages) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            AppDataSources.chatSettingSource?.setChatUnread(chat.id, false)
        }
        withContext(Dispatchers.IO) { LiveChats.refresh() }
    }

    // 宿主(TIM)里异步拉取真实消息；历史图片需要内核下载缩略图，下载后轮询刷新。
    androidx.compose.runtime.LaunchedEffect(chat.id) {
        if (!liveMessages) return@LaunchedEffect
        val loaded = ChatMessagePreviewCache.awaitOrLoad(chat.id)
        if (loaded != null) {
            // 与缓存预览内容相同则不重复替换，避免同一列表刷新造成滚动跳动。
            if (loaded != baseMessages) baseMessages = loaded
        }
        initialMessagesLoaded = true
        // 图片/头像若还在下载，后台会陆续写入本地路径：最多轮询约 14 秒。
        var attempt = 0
        while (attempt < 12) {
            val missingImages = baseMessages.any { message ->
                // 多元素消息里的图片元素也可能后回填路径（bridge 下载完成后触发刷新）。
                message.parts.any { part ->
                    part.kind == ChatPartKind.IMAGE && part.imagePath.isNullOrBlank()
                } ||
                    (message.type == ChatMessageType.IMAGE && message.imagePath.isNullOrBlank()) ||
                    (message.type == ChatMessageType.VIDEO && message.videoThumbPath.isNullOrBlank()) ||
                    (message.type == ChatMessageType.VOICE && message.mediaPath.isNullOrBlank()) ||
                    // ARK 卡片预览图由 bridge 后台下载，下载完成后 reload 刷新。
                    (
                        message.type == ChatMessageType.ARK &&
                            message.arkPreviewUrl.isNotBlank() &&
                            message.arkPreviewPath.isNullOrBlank()
                        )
            }
            val missingAvatars = baseMessages.any {
                !it.isMe && it.senderAvatarPath.isNullOrBlank()
            }
            if (!missingImages && !missingAvatars) break
            delay(1200)
            val refreshed = withContext(Dispatchers.IO) {
                AppDataSources.messageSource?.loadMessages(chat.id)
            }
            if (refreshed != null && refreshed.isNotEmpty()) {
                ChatMessagePreviewCache.save(chat.id, refreshed)
                if (refreshed != baseMessages) baseMessages = refreshed
            }
            attempt += 1
        }
    }

    // 宿主(TIM)实时推送：会话有新消息 / 更新 / 删除 / 撤回时重新拉取最新一页。
    // 内核回调由 [TimBridge] 转成 [LiveEvents.messageEvents]；chatId 为 null 表示未能解析出会话，
    // 保守起见也刷新当前页。消息列表的自动跟随由 ChatDetailMaterial/Miuix 里
    // 「消息数量增长且原本在底部时自动滚动」的逻辑处理。
    androidx.compose.runtime.LaunchedEffect(chat.id, liveMessages) {
        if (!liveMessages) return@LaunchedEffect
        LiveEvents.messageEvents
            .filter { it.chatId == null || it.chatId == chat.id }
            .conflate()
            .collect {
                val latest = withContext(Dispatchers.IO) {
                    AppDataSources.messageSource?.loadMessages(chat.id)
                }
                if (latest != null && latest.isNotEmpty()) {
                    ChatMessagePreviewCache.save(chat.id, latest)
                    if (latest != baseMessages) baseMessages = latest
                }
                // 正在查看该会话时收到新消息：同步标记已读，避免退出去列表还显示未读。
                withContext(Dispatchers.IO) {
                    AppDataSources.chatSettingSource?.setChatUnread(chat.id, false)
                }
            }
    }

    // ---------- 加号菜单：照片 / 拍照 / 文件（调用系统原生选择器与相机） ----------
    var pendingCameraUri by remember(chat.id) { mutableStateOf<Uri?>(null) }
    var pendingCameraVideoUri by remember(chat.id) { mutableStateOf<Uri?>(null) }

    fun appendLocalMessage(message: ChatMessage) {
        localMessages = localMessages + message
    }

    fun sendPickedMedia(kind: String, file: File, imageWidth: Int = 0, imageHeight: Int = 0) {
        val localId = "local_media_${System.currentTimeMillis()}"
        val isImage = imageWidth > 0 || imageHeight > 0 || kind == "image"
        appendLocalMessage(
            ChatMessage(
                id = localId,
                chatId = chat.id,
                senderName = meName,
                senderSeed = ChatRepository.profile.avatarSeed,
                isMe = true,
                type = if (isImage) ChatMessageType.IMAGE else ChatMessageType.FILE,
                content = if (isImage) "[图片]" else file.name,
                timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                isRead = true,
                fileSize = if (isImage) "" else formatMediaSize(file.length()),
                imagePath = if (isImage) file.absolutePath else null,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                mediaPath = file.absolutePath,
            ),
        )
        if (!liveMessages) return
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val source = AppDataSources.messageSource
                if (isImage) {
                    source?.sendImage(chat.id, file.absolutePath, imageWidth, imageHeight) ?: false
                } else {
                    source?.sendFile(chat.id, file.absolutePath, file.name, file.length()) ?: false
                }
            }
            if (ok) {
                // 乐观气泡已显示：服务端回显通常 <1s，600ms 后 reload 替换（原 1500ms 纯等待）。
                delay(600)
                val reloaded = withContext(Dispatchers.IO) {
                    AppDataSources.messageSource?.loadMessages(chat.id)
                }
                if (reloaded != null) {
                    baseMessages = reloaded
                    localMessages = emptyList()
                }
                withContext(Dispatchers.IO) {
                    com.material3tim.data.repository.LiveChats.refresh()
                }
            } else {
                localMessages = localMessages.filterNot { it.id == localId }
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.chat_attach_send_failed),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    /**
     * 单张图片的复制 + 解码 + 回显发送（suspend 版本，供多选顺序循环直接 await）。
     * copyUriToCache / decodeImageSize 是阻塞 IO/CPU，放 IO 线程；sendPickedMedia 内部要改 Compose 状态，
     * 留在主线程（withContext 返回后回到 Compose scope 的 Main）。
     */
    suspend fun copyAndSendPickedImage(
        uri: Uri,
        fallbackName: String = "image_${System.currentTimeMillis()}.jpg",
    ) {
        val file = withContext(Dispatchers.IO) { copyUriToCache(context, uri, fallbackName) } ?: return
        val size = withContext(Dispatchers.IO) { decodeImageSize(file) }
        sendPickedMedia("image", file, size.first, size.second)
    }

    /**
     * 相册里选到视频：
     * 1) 按 MIME/显示名推断扩展名复制到 cache；
     * 2) IO 用 MediaMetadataRetriever 读时长/宽高（处理旋转）并抽首帧 jpg；
     * 3) 先插入一条乐观 VIDEO 气泡（本地文件路径直接可预览/播放）；
     * 4) 后台 messageSource.sendVideo(...)，成功 delay + reload 换成服务端消息；
     * 失败 Toast 并移除乐观消息。
     * 元数据读取失败时跳过该项（Toast），仅首帧失败仍照常发送（thumbPath=null）。
     */
    suspend fun copyAndSendPickedVideo(uri: Uri, index: Int, mime: String) {
        val file = withContext(Dispatchers.IO) {
            copyPickedVideoToCache(context, uri, index, mime)
        }
        if (file == null) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.chat_attach_send_failed),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            return
        }
        val thumbFileName = "pickedVideo_${System.currentTimeMillis()}_$index.jpg"
        val probe = withContext(Dispatchers.IO) {
            probePickedVideo(context, file.absolutePath, thumbFileName)
        }
        if (probe == null) {
            // 探测完全失败也 best-effort 发送：duration/宽高给 0、thumbPath=null，由 bridge 再兜底探测。
            Log.w(
                MediaLogTag,
                "video probe returned null, send best-effort: path=${file.absolutePath} " +
                    "size=${file.length()}",
            )
        } else {
            Log.i(
                MediaLogTag,
                "video probe: path=${file.absolutePath} durMs=${probe.durationMs} " +
                    "${probe.width}x${probe.height} thumb=${probe.thumbnailPath}",
            )
        }
        val durationMs = probe?.durationMs ?: 0L
        val videoWidth = probe?.width ?: 0
        val videoHeight = probe?.height ?: 0
        val thumbPath = probe?.thumbnailPath
        val localId = "local_video_${System.currentTimeMillis()}"
        appendLocalMessage(
            ChatMessage(
                id = localId,
                chatId = chat.id,
                senderName = meName,
                senderSeed = ChatRepository.profile.avatarSeed,
                isMe = true,
                type = ChatMessageType.VIDEO,
                content = "[" + context.getString(R.string.chat_video_title) + "]",
                timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                isRead = true,
                fileSize = formatMediaSize(file.length()),
                mediaPath = file.absolutePath,
                videoPath = file.absolutePath,
                videoThumbPath = thumbPath,
                videoSeconds = (durationMs / 1000L).toInt(),
                imageWidth = videoWidth,
                imageHeight = videoHeight,
            ),
        )
        if (!liveMessages) return
        Log.i(
            MediaLogTag,
            "sendVideo request: chat=${chat.id} path=${file.absolutePath} size=${file.length()} " +
                "durMs=$durationMs ${videoWidth}x$videoHeight thumb=$thumbPath",
        )
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                AppDataSources.messageSource?.sendVideo(
                    chatId = chat.id,
                    filePath = file.absolutePath,
                    thumbPath = thumbPath,
                    durationMs = durationMs,
                    width = videoWidth,
                    height = videoHeight,
                ) ?: false
            }
            Log.i(MediaLogTag, "sendVideo result=$ok chat=${chat.id}")
            if (ok) {
                // 乐观视频气泡已显示：600ms 后 reload 替换（原 1500ms）。
                delay(600)
                val reloaded = withContext(Dispatchers.IO) {
                    AppDataSources.messageSource?.loadMessages(chat.id)
                }
                if (reloaded != null) {
                    baseMessages = reloaded
                    localMessages = emptyList()
                }
                withContext(Dispatchers.IO) {
                    com.material3tim.data.repository.LiveChats.refresh()
                }
            } else {
                localMessages = localMessages.filterNot { it.id == localId }
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.chat_attach_send_failed),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    /** 单张图片入口（拍照回调等）：内部切后台复制/解码，主线程调用点无需改动。 */
    fun handlePickedImage(uri: Uri) {
        scope.launch {
            copyAndSendPickedImage(uri)
        }
    }

    fun handlePickedFile(uri: Uri) {
        val file = copyUriToCache(context, uri, "file_${System.currentTimeMillis()}") ?: return
        sendPickedMedia("file", file)
    }
    /** 点击收藏表情：先本地回显一条表情图片消息，再通过宿主内核以「表情」形态发送。 */
    fun sendCustomEmoji(emoji: CustomEmoji) {
        val path = emoji.imagePath
        if (path.isNullOrBlank() || !File(path).isFile) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.chat_attach_send_failed),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
            return
        }
        val size = decodeImageSize(File(path))
        val localId = "local_emoji_${System.currentTimeMillis()}"
        appendLocalMessage(
            ChatMessage(
                id = localId,
                chatId = chat.id,
                senderName = meName,
                senderSeed = ChatRepository.profile.avatarSeed,
                isMe = true,
                type = ChatMessageType.IMAGE,
                content = "[表情]",
                timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                isRead = true,
                fileSize = "",
                imagePath = path,
                imageWidth = size.first,
                imageHeight = size.second,
                isEmojiImage = true,
                mediaPath = path,
            ),
        )
        if (!liveMessages) return
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                AppDataSources.messageSource?.sendCustomEmoji(chat.id, path) ?: false
            }
            if (ok) {
                // 本地表情气泡已显示：600ms 后 reload 替换（原 1200ms）。
                delay(600)
                val reloaded = withContext(Dispatchers.IO) {
                    AppDataSources.messageSource?.loadMessages(chat.id)
                }
                if (reloaded != null) {
                    baseMessages = reloaded
                    localMessages = emptyList()
                }
                withContext(Dispatchers.IO) {
                    com.material3tim.data.repository.LiveChats.refresh()
                }
            } else {
                localMessages = localMessages.filterNot { it.id == localId }
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.chat_attach_send_failed),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    // 「相册」用系统媒体选择器多选（最多 9 项，图片 + 视频）：
    // 图片仍走「复制到缓存 → 解码尺寸 → 后台发送」；视频先复制到缓存并**临时按文件消息发送**，
    // 等 bridge 的 sendVideo 接入后再换成真正的视频消息（后续任务）。
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK_IMAGES),
    ) { uris ->
        // 多选：协程内按选择顺序逐项处理（每项 await 完再下一项，本地回显顺序=选择顺序）；
        // 复制/解码在 IO 线程，避免选多张云端图/大视频时主线程卡顿/ANR。
        scope.launch {
            uris.forEachIndexed { index, uri ->
                // MIME 优先；部分 provider 拿不到 MIME 时用显示名扩展名兜底判断视频。
                val (mime, displayName) = withContext(Dispatchers.IO) {
                    val type = runCatching { context.contentResolver.getType(uri) }
                        .getOrNull()
                        .orEmpty()
                    type to queryDisplayName(context, uri)
                }
                val isVideo = mime.lowercase().startsWith("video/") ||
                    isVideoDisplayName(displayName)
                if (isVideo) {
                    copyAndSendPickedVideo(uri, index, mime)
                } else {
                    // 文件名带 nanoTime + 索引，避免多张图同一毫秒被覆盖
                    //（fallbackName 仅在没有显示名时生效）。
                    copyAndSendPickedImage(uri, "image_${System.nanoTime()}_$index.jpg")
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { handlePickedFile(it) } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) handlePickedImage(uri)
    }

    // 录像：MediaStore 预创建输出 uri → CaptureVideo；成功走既有 copyAndSendPickedVideo 发送链路。
    val cameraVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CaptureVideo(),
    ) { success ->
        val uri = pendingCameraVideoUri
        pendingCameraVideoUri = null
        if (success && uri != null) {
            scope.launch { copyAndSendPickedVideo(uri, 0, "video/mp4") }
        } else if (uri != null) {
            // 取消/失败：清理 MediaStore 里预创建的条目，避免留下空文件。
            runCatching { context.contentResolver.delete(uri, null, null) }
        }
    }

    // 附件面板相机项：false=拍照（原流程），true=录像。
    val onCameraAction: (Boolean) -> Unit = { isVideo ->
        if (isVideo) {
            val uri = createCameraVideoUri(context)
            if (uri != null) {
                pendingCameraVideoUri = uri
                cameraVideoLauncher.launch(uri)
            } else {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.chat_attach_send_failed),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        } else {
            val uri = createCameraImageUri(context)
            if (uri != null) {
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.chat_attach_send_failed),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    // 语音/视频通话：直接拉起 TIM 原版通话界面（bridge CallSource），不自写通话页/权限逻辑。
    // 独立 App（无 callSource）提示仅宿主可用；拉起失败 Toast「无法发起通话」。
    val startTimCall: (Boolean) -> Unit = { voice ->
        scope.launch {
            val source = AppDataSources.callSource
            if (source == null) {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.chat_call_host_only),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            } else {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        if (voice) source.startVoiceCall(chat.id) else source.startVideoCall(chat.id)
                    }.getOrDefault(false)
                }
                if (!ok) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.chat_call_failed),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    val onAttachAction: (ChatAttachAction) -> Unit = { action ->
        when (action) {
            ChatAttachAction.Photo -> photoPickerLauncher.launch(
                // 相册同时可选图片和视频（多选上限 MAX_PICK_IMAGES）。
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
            )

            ChatAttachAction.Camera -> {
                val uri = createCameraImageUri(context)
                if (uri != null) {
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                } else {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.chat_attach_send_failed),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }

            ChatAttachAction.Files -> filePickerLauncher.launch(arrayOf("*/*"))
            ChatAttachAction.VoiceCall -> startTimCall(true)
            ChatAttachAction.VideoCall -> startTimCall(false)
            else -> android.widget.Toast.makeText(
                context,
                context.getString(R.string.feature_coming_soon),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // 输入文字 / 当前展开的面板在两种 UI 模式之间共享。
    val composer = rememberChatComposerState()
    // 左滑引用：当前正在回复的消息（null 表示普通发送）
    var replyTarget by remember { mutableStateOf<ChatMessage?>(null) }

    val onOpenInfo: () -> Unit = { navigator.push(Route.ChatInfo(chat.id)) }
    // 群公告类型 ARK 卡片：进入当前会话的群公告页（不按外部链接打开）。
    val onArkGroupNoticeClick: (ChatMessage) -> Unit = { message ->
        navigator.push(Route.ChatNotice(message.chatId.ifBlank { chat.id }))
    }
    // 点消息头像进入统一「用户资料」页；系统消息/无发送者 uid 的消息不响应。
    val onAvatarClick: (ChatMessage) -> Unit = { message ->
        if (message.type != ChatMessageType.SYSTEM && message.senderUid.isNotBlank()) {
            navigator.push(
                Route.ContactDetail(
                    contactId = "",
                    chatId = chat.id,
                    uid = message.senderUid,
                    uin = message.senderUin,
                ),
            )
        }
    }

    // 长按菜单 - 删除消息
    val onDeleteMessage: (ChatMessage) -> Unit = { message ->
        val msgId = message.chatRecordMsgId
        if (msgId == 0L) {
            android.widget.Toast.makeText(context, "无法删除这条消息", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    AppDataSources.messageSource?.deleteMessage(chat.id, msgId) ?: false
                }
                android.widget.Toast.makeText(
                    context,
                    if (ok) "已删除" else "删除失败（可能已超时或没有权限）",
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
                if (ok) {
                    localMessages = localMessages.filterNot { it.chatRecordMsgId == msgId }
                    // 内核删除后只需极短补偿：200ms 后 reload（原 400ms）。
                    delay(200)
                    val reloaded = withContext(Dispatchers.IO) {
                        AppDataSources.messageSource?.loadMessages(chat.id)
                    }
                    if (reloaded != null) baseMessages = reloaded
                }
            }
        }
    }
    // 长按菜单 - 撤回消息（菜单只在自己发送且 2 分钟内的消息上显示该项）
    val onRecallMessage: (ChatMessage) -> Unit = { message ->
        val msgId = message.chatRecordMsgId
        if (msgId == 0L) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.chat_recall_unsupported),
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        } else {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    AppDataSources.messageSource?.recallMessage(chat.id, msgId) ?: false
                }
                android.widget.Toast.makeText(
                    context,
                    context.getString(
                        if (ok) R.string.chat_recall_success else R.string.chat_recall_failed,
                    ),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
                if (ok) {
                    // 内核撤回后稍等再刷新，确保消息列表反映「已撤回」状态（500→250ms）。
                    delay(250)
                    val reloaded = withContext(Dispatchers.IO) {
                        AppDataSources.messageSource?.loadMessages(chat.id)
                    }
                    if (reloaded != null) {
                        baseMessages = reloaded
                        localMessages = emptyList()
                    }
                    withContext(Dispatchers.IO) {
                        com.material3tim.data.repository.LiveChats.refresh()
                    }
                }
            }
        }
    }
    // 文件下载进度：同一时间只跟踪一个下载中的文件（按 msgId 匹配 FILE 气泡显示进度条）。
    var downloadingFileMsgId by remember { mutableStateOf(0L) }
    var fileDownloadProgress by remember { mutableIntStateOf(-1) }

    // 点击文件消息：已下载 → 系统打开；未下载 → 启动内核下载并轮询进度，下载完成后打开。
    val onFileClick: (ChatMessage) -> Unit = { message ->
        scope.launch {
            val path = message.mediaPath
            val downloaded = !path.isNullOrBlank() && File(path).isFile
            if (downloaded) {
                val ok = withContext(Dispatchers.IO) {
                    AppDataSources.messageSource?.openLocalFile(path) ?: false
                }
                if (!ok) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.chat_file_open_failed),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            } else {
                val msgId = message.chatRecordMsgId
                val elementId = message.elementId
                // 同一时间只允许一个文件在下载（进度条只有一份状态）；重复点击直接忽略。
                if (downloadingFileMsgId != 0L || msgId == 0L) return@launch
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.chat_file_downloading),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
                val started = withContext(Dispatchers.IO) {
                    AppDataSources.messageSource?.startFileDownload(chat.id, msgId, elementId)
                        ?: false
                }
                if (!started) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.chat_file_download_failed),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                    return@launch
                }
                downloadingFileMsgId = msgId
                // 进度未知时 bridge 返回 -1 → UI 显示不确定态进度条（不要固定 0%）。
                fileDownloadProgress = -1
                // 轮询下载进度：最多 180s，每 300ms 一次；拿到本地路径后打开。
                val deadline = System.currentTimeMillis() + 180_000L
                var opened = false
                while (System.currentTimeMillis() < deadline) {
                    delay(300)
                    val snapshot = withContext(Dispatchers.IO) {
                        val progress = AppDataSources.messageSource
                            ?.fileDownloadProgress(chat.id, msgId, elementId) ?: -1
                        val downloadedPath = AppDataSources.messageSource
                            ?.downloadedFilePath(chat.id, msgId, elementId)
                        progress to downloadedPath
                    }
                    val (progress, downloadedPath) = snapshot
                    // -1 表示进度未知 → 气泡显示不确定态进度条（null）。
                    fileDownloadProgress = progress
                    if (!downloadedPath.isNullOrBlank()) {
                        opened = withContext(Dispatchers.IO) {
                            AppDataSources.messageSource?.openLocalFile(downloadedPath) ?: false
                        }
                        break
                    }
                }
                if (!opened) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.chat_file_download_failed),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
                downloadingFileMsgId = 0L
                fileDownloadProgress = -1
                // 下载后 mediaPath/downloadProgress 可能回填，稍等刷新一次气泡状态（300→200ms）。
                delay(200)
                val reloaded = withContext(Dispatchers.IO) {
                    AppDataSources.messageSource?.loadMessages(chat.id)
                }
                if (reloaded != null) baseMessages = reloaded
            }
        }
    }
    // 群聊 @ 成员面板数据 + 群主/管理员撤回他人消息权限（宿主里动态加载；独立 App 为空/false）。
    var atMembers by remember(chat.id) { mutableStateOf<List<ChatMember>>(emptyList()) }
    var atMembersLoading by remember(chat.id) { mutableStateOf(false) }
    var atMembersFailed by remember(chat.id) { mutableStateOf(false) }
    var canRecallOthers by remember(chat.id) { mutableStateOf(false) }
    // @ 面板按需强制拉全量成员（loadGroupMembers），避免必须先打开群成员页。
    val onRequestAtMembers: () -> Unit = {
        if (chat.isGroup && !atMembersLoading) {
            atMembersLoading = true
            atMembersFailed = false
            scope.launch {
                val loaded = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.chatSource?.loadGroupMembers(chat.id)
                    }.getOrNull()
                }
                if (loaded != null) {
                    atMembers = loaded
                    atMembersFailed = false
                } else {
                    atMembersFailed = true
                }
                atMembersLoading = false
            }
        }
    }
    // @ 成员改为**按需加载**：进入聊天不预取（避免与首屏消息渲染抢 IO/主线程）；
    // 用户打开 @ 面板时由 onRequestAtMembers 加载全量成员并显示 loading。
    // 撤回权限查询较轻量，仍进入即查（长按菜单需要，内核缓存命中时毫秒级）。
    LaunchedEffect(chat.id) {
        if (!chat.isGroup) return@LaunchedEffect
        canRecallOthers = withContext(Dispatchers.IO) {
            AppDataSources.messageSource?.canRecallOthers(chat.id) ?: false
        }
    }
    // 群成员缓存刷新（LiveEvents MEMBERS）时同步 @ 面板成员。
    LaunchedEffect(chat.id) {
        if (!chat.isGroup || !AppDataSources.hasLiveDataSource) return@LaunchedEffect
        LiveEvents.dataUpdates.collect { update ->
            if (update.chatId == chat.id && update.kind == DataUpdate.Kind.MEMBERS) {
                val refreshed = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.chatSource?.loadGroupMembers(chat.id)
                    }.getOrNull()
                }
                if (!refreshed.isNullOrEmpty()) {
                    atMembers = refreshed
                    atMembersFailed = false
                }
            }
        }
    }
    // 长按「at对方」：把 @昵称 追加到输入框（不清空草稿），走现有 @ 元素发送逻辑。
    val onAtSender: (ChatMessage) -> Unit = { message ->
        val name = message.senderName.ifBlank {
            message.senderUin.ifBlank { "对方" }
        }
        composer.selectAts(
            listOf(AtTarget(display = "@$name", uid = message.senderUid)),
        )
    }
    // 语音：拉起系统录音机（bridge 负责录完转 silk 并发送）；简单防抖，避免重复拉起/在途提示。
    var voiceRecordRequesting by remember { mutableStateOf(false) }
    val onStartVoiceRecord: () -> Unit = {
        if (voiceRecordRequesting) {
            android.widget.Toast.makeText(
                context,
                R.string.chat_voice_record_in_progress,
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        } else {
            voiceRecordRequesting = true
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.messageSource?.startSystemVoiceRecord(chat.id)
                    }.getOrNull() ?: false
                }
                voiceRecordRequesting = false
                val messageRes = when {
                    AppDataSources.messageSource == null -> R.string.chat_voice_record_unavailable
                    ok -> R.string.chat_voice_record_started
                    else -> R.string.chat_voice_record_unavailable
                }
                android.widget.Toast.makeText(context, messageRes, android.widget.Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    // 表情包详情发送：走 MarketFace 接口（bridge 失败时内部回退图片表情），Toast 反馈。
    val onSendStickerPackEmoji: (packId: String, eId: String) -> Unit = { packId, eId ->
        scope.launch {
            val source = AppDataSources.emojiSource
            val ok = if (source == null) {
                false
            } else {
                withContext(Dispatchers.IO) {
                    runCatching {
                        source.sendStickerPackEmoji(chat.id, packId, eId)
                    }.getOrDefault(false)
                }
            }
            android.widget.Toast.makeText(
                context,
                if (ok) R.string.emoji_sticker_sent else R.string.emoji_sticker_send_failed,
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }
    // 长按图片/表情「添加到表情」：IO 调 emojiSource，Toast 反馈。
    val onAddToFavorite: (ChatMessage) -> Unit = { message ->
        scope.launch {
            val source = AppDataSources.emojiSource
            val elementId = message.elementId.takeIf { it != 0L }
                ?: message.parts.firstOrNull {
                    it.kind == com.material3tim.data.model.ChatPartKind.IMAGE
                }?.elementId ?: 0L
            val ok = if (source == null) {
                false
            } else {
                withContext(Dispatchers.IO) {
                    runCatching {
                        source.addMessageToFavorites(chat.id, message.msgId, elementId)
                    }.getOrDefault(false)
                }
            }
            android.widget.Toast.makeText(
                context,
                if (ok) R.string.emoji_added_to_favorites else R.string.emoji_add_to_favorites_failed,
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }
    val onSend: (String) -> Unit = { raw ->
        val text = raw.trim()
        if (text.isNotEmpty()) {
            val target = replyTarget
            // 只发仍出现在文本里的 @ 片段（用户手动删掉 @昵称 后不再发送 at 元素）。
            val atTargets = composer.atTargets.filter { text.contains(it.display) }
            // 一按下发送就收起引用条（不等网络真正发出去），UI 无延迟
            replyTarget = null
            localSeq += 1
            localMessages = localMessages + ChatMessage(
                id = "local_${localSeq}",
                chatId = chat.id,
                senderName = meName,
                senderSeed = ChatRepository.profile.avatarSeed,
                isMe = true,
                type = ChatMessageType.TEXT,
                content = text,
                timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                isRead = true,
                replyToName = target?.senderName.orEmpty(),
                replyToContent = target?.content.orEmpty(),
                replyToMsgId = target?.chatRecordMsgId ?: 0L,
                replyToSenderUid = target?.senderUid.orEmpty(),
                timestampMs = System.currentTimeMillis(),
            )
            if (liveMessages) {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        val source = AppDataSources.messageSource
                        when {
                            // 带 @：一次把 at 元素 + 引用一起发出去；没有 @ 时走原逻辑。
                            atTargets.isNotEmpty() -> source?.sendTextWithAt(
                                chatId = chat.id,
                                text = text,
                                atTargets = atTargets,
                                replyToMsgId = target?.chatRecordMsgId ?: 0L,
                                replySenderUid = target?.senderUid.orEmpty(),
                                replySourceText = (target?.content ?: "").take(200),
                            ) ?: false
                            target != null && target.chatRecordMsgId != 0L -> {
                                source?.sendTextWithReply(
                                    chatId = chat.id,
                                    text = text,
                                    replyToMsgId = target.chatRecordMsgId,
                                    replySenderUid = target.senderUid,
                                    replySourceText = target.content.take(200),
                                ) ?: false
                            }
                            else -> source?.sendText(chat.id, text) ?: false
                        }
                    }
                    if (ok) {
                        // 乐观文本气泡已显示：300ms 后 reload 替换（原 600ms）。
                        delay(300)
                        val reloaded = withContext(Dispatchers.IO) {
                            AppDataSources.messageSource?.loadMessages(chat.id)
                        }
                        if (reloaded != null) {
                            baseMessages = reloaded
                            localMessages = emptyList()
                        }
                        withContext(Dispatchers.IO) {
                            com.material3tim.data.repository.LiveChats.refresh()
                        }
                    }
                }
            }
            // 发送已提交：清空 @ 目标，避免下一条消息重复带 at 元素。
            composer.clearAtTargets()
        }
    }

    // 切换会话时强制销毁旧页面子树、重建 Composition（remember/focus/输入状态全部重置），
    // 避免旧 BasicTextField 的焦点目标/IME 输入连接被复用；key 只包裹页面内容，不影响转场与返回栈。
    key(chat.id) {
        LaunchedEffect(Unit) {
            Log.d("Material3Tim", "ime: chat detail keyed chat=${chat.id}")
        }
        val chatListScrollState = remember { ChatListScrollState() }
        CompositionLocalProvider(
            LocalChatListScrollState provides chatListScrollState,
            LocalChatImageMessages provides messages,
        ) {
        when (LocalUiMode.current) {
            UiMode.Miuix -> ChatDetailMiuix(
            chat = chat,
            messages = messages,
            composer = composer,
            onBack = onBack,
            onOpenInfo = onOpenInfo,
            onSend = onSend,
            onAvatarClick = onAvatarClick,
            onArkGroupNoticeClick = onArkGroupNoticeClick,
            onAtSender = onAtSender,
            onAddToFavorite = onAddToFavorite,
            onStartVoiceRecord = onStartVoiceRecord,
            initialMessagesLoaded = initialMessagesLoaded,
            replyTarget = replyTarget,
            onReplyRequest = { replyTarget = it },
            onClearReply = { replyTarget = null },
            onReplyQuoteClick = onReplyQuoteClick,
            jumpTarget = pendingJumpMessage,
            onJumpConsumed = { pendingJumpMessage = null },
            selectionMode = selectionMode,
            selectedMessageIds = selectedMessageIds,
            onToggleSelectMessage = onToggleSelectMessage,
            onEnterSelection = onEnterSelection,
            onExitSelection = onExitSelection,
            onForwardSelected = onForwardSelected,
            onForwardMessage = onForwardMessage,
            onPlusOne = onPlusOne,
            plusOneMessageIds = plusOneMessageIds,
            onDeleteMessage = onDeleteMessage,
            onRecallMessage = onRecallMessage,
            onFileClick = onFileClick,
            downloadingFileMsgId = downloadingFileMsgId,
            fileDownloadProgress = fileDownloadProgress,
            atMembers = atMembers,
            atMembersLoading = atMembersLoading,
            atMembersFailed = atMembersFailed,
            onAtMembersRequested = onRequestAtMembers,
            atSheetVisible = composer.atPickerOpen,
            onAtConfirm = { targets -> composer.selectAts(targets) },
            onAtDismiss = composer::dismissAtPicker,
            canRecallOthers = canRecallOthers,
            onLoadOlder = onLoadOlder,
            isLoadingOlder = isLoadingOlder,
            hasMoreOlder = hasMoreOlder,
            onAttachAction = onAttachAction,
            onCameraAction = onCameraAction,
            onSendCustomEmoji = ::sendCustomEmoji,
            onSendStickerPackEmoji = onSendStickerPackEmoji,
            imeController = imeController,
        )

        UiMode.Material -> ChatDetailMaterial(
            chat = chat,
            messages = messages,
            composer = composer,
            onBack = onBack,
            onOpenInfo = onOpenInfo,
            onSend = onSend,
            onAvatarClick = onAvatarClick,
            onArkGroupNoticeClick = onArkGroupNoticeClick,
            onAtSender = onAtSender,
            onAddToFavorite = onAddToFavorite,
            onStartVoiceRecord = onStartVoiceRecord,
            initialMessagesLoaded = initialMessagesLoaded,
            replyTarget = replyTarget,
            onReplyRequest = { replyTarget = it },
            onClearReply = { replyTarget = null },
            onReplyQuoteClick = onReplyQuoteClick,
            jumpTarget = pendingJumpMessage,
            onJumpConsumed = { pendingJumpMessage = null },
            selectionMode = selectionMode,
            selectedMessageIds = selectedMessageIds,
            onToggleSelectMessage = onToggleSelectMessage,
            onEnterSelection = onEnterSelection,
            onExitSelection = onExitSelection,
            onForwardSelected = onForwardSelected,
            onForwardMessage = onForwardMessage,
            onPlusOne = onPlusOne,
            plusOneMessageIds = plusOneMessageIds,
            onDeleteMessage = onDeleteMessage,
            onRecallMessage = onRecallMessage,
            onFileClick = onFileClick,
            downloadingFileMsgId = downloadingFileMsgId,
            fileDownloadProgress = fileDownloadProgress,
            atMembers = atMembers,
            atMembersLoading = atMembersLoading,
            atMembersFailed = atMembersFailed,
            onAtMembersRequested = onRequestAtMembers,
            atSheetVisible = composer.atPickerOpen,
            onAtConfirm = { targets -> composer.selectAts(targets) },
            onAtDismiss = composer::dismissAtPicker,
            canRecallOthers = canRecallOthers,
            onLoadOlder = onLoadOlder,
            isLoadingOlder = isLoadingOlder,
            hasMoreOlder = hasMoreOlder,
            onAttachAction = onAttachAction,
            onCameraAction = onCameraAction,
            onSendCustomEmoji = ::sendCustomEmoji,
            onSendStickerPackEmoji = onSendStickerPackEmoji,
            imeController = imeController,
        )
        }
        }
    }
}

/**
 * 聊天消息列表滚动状态（CompositionLocal 下发）：
 * 滚动中暂停动图/延迟新图解码，降低 fling 期间的解码与绘制压力。
 * 由 ChatDetailMaterial/Miuix 用 snapshotFlow(listState.isScrollInProgress) 写入，气泡读取。
 */
internal class ChatListScrollState {
    var isScrolling: Boolean by androidx.compose.runtime.mutableStateOf(false)
}

internal val LocalChatListScrollState = compositionLocalOf<ChatListScrollState?> { null }

/** 聊天列表滚动锚点（用于加载更早消息时保持视口）。 */
internal data class ChatScrollAnchor(
    val count: Int,
    val lastId: String?,
    val atBottom: Boolean,
)

/**
 * 合并「已加载的更早消息」与「最新一页 + 本地发送」：按 id 去重并保持升序，
 * 然后用 timestampMs 重新计算时间分隔条（跨批次衔接正确）。
 */
internal fun mergeHistoryMessages(
    older: List<ChatMessage>,
    latest: List<ChatMessage>,
): List<ChatMessage> {
    if (older.isEmpty() && latest.none { it.id.startsWith("local_") }) return latest
    val realMessages = latest.filterNot { it.id.startsWith("local_") }
    val pendingLocal = latest.filter { it.id.startsWith("local_") }
    // 真实消息（含引用）已经出现在服务端列表里后，同内容的本地区气泡要去掉，避免双气泡。
    val activeLocal = pendingLocal.filter { local ->
        realMessages.none { real ->
            real.isMe == local.isMe &&
                real.type == local.type &&
                real.content == local.content &&
                (local.timestampMs <= 0L ||
                    kotlin.math.abs(real.timestampMs - local.timestampMs) < 15_000L)
        }
    }
    val all = latest.filterNot { it.id.startsWith("local_") } + activeLocal
    val seen = HashSet<String>(older.size + all.size)
    val merged = ArrayList<ChatMessage>(older.size + all.size)
    older.forEach { if (seen.add(it.id)) merged.add(it) }
    all.forEach { if (seen.add(it.id)) merged.add(it) }

    var previousTime = 0L
    return merged.map { message ->
        val time = message.timestampMs
        if (time <= 0L) {
            message
        } else {
            val needSeparator = previousTime == 0L || time - previousTime >= 5 * 60 * 1000L
            previousTime = time
            if (needSeparator) {
                message.copy(timeSeparator = formatHistorySeparator(time))
            } else {
                message.copy(timeSeparator = null)
            }
        }
    }
}

/** 历史消息时间分隔条文案（今天/昨天/M月d日 + HH:mm）。 */
internal fun formatHistorySeparator(timeMillis: Long): String {
    val now = java.util.Calendar.getInstance()
    val target = java.util.Calendar.getInstance().apply { this.timeInMillis = timeMillis }
    val sameYear = now.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR)
    val sameDay = sameYear &&
        now.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR)
    val yesterday = run {
        val y = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        sameYear && y.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR)
    }
    val clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMillis))
    return when {
        sameDay -> "今天 $clock"
        yesterday -> "昨天 $clock"
        sameYear -> "${target.get(java.util.Calendar.MONTH) + 1}月${target.get(java.util.Calendar.DAY_OF_MONTH)}日 $clock"
        else -> SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault()).format(Date(timeMillis))
    }
}

/** 聊天详情加载占位：标题「群聊」+ 骨架/失败重试（群助手群解析期间不显示「聊天」空页）。 */
@Composable
private fun ChatDetailPlaceholder(
    loading: Boolean,
    failed: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val color: Color
    val container: Color
    when (LocalUiMode.current) {
        UiMode.Material -> {
            color = MaterialTheme.colorScheme.onSurface
            container = MaterialTheme.colorScheme.surfaceContainer
        }

        UiMode.Miuix -> {
            color = MiuixTheme.colorScheme.onSurface
            container = MiuixTheme.colorScheme.background
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(container)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.chat_detail_back),
                tint = color,
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.chat_group),
                color = color,
            )
            Spacer(Modifier.height(12.dp))
            if (failed) {
                Text(
                    text = stringResource(R.string.group_files_load_failed),
                    color = color,
                )
                TextButton(onClick = onRetry) {
                    Text(stringResource(R.string.group_files_retry))
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = color,
                )
            }
        }
    }
}

/** 会话不存在时的兜底页（正常导航链路不会走到这里）。 */
@Composable
internal fun MissingPage(title: String, onBack: () -> Unit) {
    val color: Color
    val container: Color
    when (LocalUiMode.current) {
        UiMode.Material -> {
            color = MaterialTheme.colorScheme.onSurface
            container = MaterialTheme.colorScheme.surfaceContainer
        }

        UiMode.Miuix -> {
            color = MiuixTheme.colorScheme.onSurface
            container = MiuixTheme.colorScheme.background
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(container)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.chat_detail_back),
                tint = color,
            )
        }
        Text(
            text = title,
            color = color,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

/** 单聊资料页里展示的示例 TIM 号。 */
internal fun sampleTimId(avatarSeed: Int): String =
    (avatarSeed.toLong() * 1111L + 31415926L).toString()

/** 群聊/单聊顶栏副标题。 */
@Composable
internal fun chatSubtitle(chat: ChatItem): String? = when {
    chat.isGroup && chat.memberCount > 0 -> stringResource(R.string.chat_member_count, chat.memberCount)
    chat.isOnline -> stringResource(R.string.chat_online)
    else -> null
}

/** 聊天页通用视觉常量（两种 UI 模式共用）。 */

/** 分组末尾头像下移量：让头像底边对齐气泡底边（跳过气泡下方的时间 / 已读行）。 */
internal val ChatAvatarMetaLift = 18.dp

/** 聊天加号「照片」多选上限（系统 Photo Picker 单次最多可选张数）。 */
private const val MAX_PICK_IMAGES = 9

/** 与 bridge 一致的日志 tag：视频/图片选择与发送链路。 */
private const val MediaLogTag = "Material3Tim"

/** 创建系统相机拍照输出用的 MediaStore Uri（无需 FileProvider）。 */
/**
 * ARK 卡片点击跳转（M3E/Miuix 共用）：
 * 1) 宿主要求优先走 TIM 的 JumpActivity（classLoader + 显式组件名两种方式）；
 * 2) 失败回退系统 ACTION_VIEW；
 * 3) 都失败 Toast 提示，不影响其它消息。
 */
internal fun openArkJump(context: Context, url: String) {
    if (url.isBlank()) return
    val uri = Uri.parse(url)
    // 普通 http(s) 链接直接交给系统浏览器/对应 App；TIM JumpActivity 对 m.q.qq.com
    // 之类的小程序通用链接会立刻销毁、什么都不打开，所以 http(s) 不走它。
    if (uri.scheme?.startsWith("http", ignoreCase = true) == true) {
        val opened = runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.isSuccess
        if (opened) return
    }
    val jumpActivityName = "com.tencent.mobileqq.activity.JumpActivity"
    val byClassLoader = runCatching {
        val clazz = context.classLoader.loadClass(jumpActivityName)
        context.startActivity(
            Intent(context, clazz).apply {
                data = uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.isSuccess
    if (byClassLoader) return
    val byComponent = runCatching {
        context.startActivity(
            Intent().apply {
                setClassName(context.packageName, jumpActivityName)
                data = uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.isSuccess
    if (byComponent) return
    val byActionView = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }.isSuccess
    if (!byActionView) {
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.feature_coming_soon),
            android.widget.Toast.LENGTH_SHORT,
        ).show()
    }
}

/** 红包已领取状态在 bridge 里的 grabState 值（暂定 1，构建后按实测调整）。 */
private const val RedPacketGrabStateGrabbed = 1

/** 红包是否带可领取数据（billNo + authkey 都由 bridge 解析出来时才能点击打开原生领取）。 */
internal fun ChatMessage.canOpenRedPacket(): Boolean =
    redPacketBillNo.isNotBlank() && redPacketAuthkey.isNotBlank()

/** 红包是否已领取：已领取金额 > 0 或 grabState 表示已领取。 */
internal fun ChatMessage.isRedPacketGrabbed(): Boolean =
    redPacketGrabbedAmount > 0L || redPacketGrabState == RedPacketGrabStateGrabbed

/** 录像输出 uri：MediaStore.Video 预创建 M3TIM_<ts>.mp4（video/mp4），供 CaptureVideo 写入。 */
private fun createCameraVideoUri(context: Context): Uri? = runCatching {
    val values = ContentValues().apply {
        put(
            MediaStore.Video.Media.DISPLAY_NAME,
            "M3TIM_${System.currentTimeMillis()}.mp4",
        )
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
    }
    context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
}.getOrNull()

private fun createCameraImageUri(context: Context): Uri? = runCatching {
    val values = ContentValues().apply {
        put(
            MediaStore.Images.Media.DISPLAY_NAME,
            "M3TIM_${System.currentTimeMillis()}.jpg",
        )
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }
    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}.getOrNull()

/** 把选择器返回的 Uri 复制到 cacheDir，返回本地文件（TIM 内核发送需要本地路径）。 */
private fun copyUriToCache(context: Context, uri: Uri, fallbackName: String): File? = runCatching {
    val displayName = queryDisplayName(context, uri)?.takeIf { it.isNotBlank() } ?: fallbackName
    val safeName = displayName.replace('/', '_').replace('\\', '_')
    // 每个文件单独一个子目录，文件名保持原始名字：
    // TIM 的 createFileElement(path) 会直接用文件名当消息里的文件名，前缀会显示到对方/PC 上。
    // 目录带 nanoTime：多选同一毫秒拷贝多张图时也不会互相覆盖（文件名仍保留原显示名）。
    val dir = File(
        context.cacheDir,
        "chat_media/${System.currentTimeMillis()}_${System.nanoTime()}",
    ).apply { mkdirs() }
    val target = File(dir, safeName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        target.outputStream().use { output -> input.copyTo(output) }
    } ?: return@runCatching null
    target
}.getOrNull()

/**
 * 相册选中的视频复制到缓存：文件名保留原显示名的 base + 按 MIME 推断的正确扩展名，
 * 绝不把视频写成 .jpg（copyAndSendPickedImage 的命名只用于图片）。
 */
private fun copyPickedVideoToCache(context: Context, uri: Uri, index: Int, mime: String): File? {
    val displayName = queryDisplayName(context, uri)
    val extension = videoExtensionFor(mime, displayName)
    val baseName = displayName
        ?.substringBeforeLast('.')
        ?.replace('/', '_')
        ?.replace('\\', '_')
        ?.takeIf { it.isNotBlank() }
        ?: "video_${System.nanoTime()}_$index"
    val dir = File(
        context.cacheDir,
        "chat_media/${System.currentTimeMillis()}_${System.nanoTime()}",
    ).apply { mkdirs() }
    val target = File(dir, baseName + extension)
    Log.i(
        MediaLogTag,
        "copyVideo start: uri=$uri mime=$mime displayName=$displayName ext=$extension " +
            "target=${target.absolutePath}",
    )
    val copied = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return@runCatching null
        target
    }.onFailure {
        Log.w(MediaLogTag, "copyVideo failed: uri=$uri target=${target.absolutePath}", it)
    }.getOrNull()
    Log.i(
        MediaLogTag,
        "copyVideo done: path=${copied?.absolutePath} size=${copied?.length() ?: -1L}",
    )
    return copied
}

/** 视频常见扩展名（MIME 为空时的兜底判断）。 */
private val VideoFileExtensions = setOf("mp4", "mov", "3gp", "3gpp", "webm", "mkv", "avi", "m4v")

/** 显示名是否像视频文件（扩展名判断）。 */
private fun isVideoDisplayName(displayName: String?): Boolean {
    val ext = displayName?.substringAfterLast('.', "")?.lowercase().orEmpty()
    return ext in VideoFileExtensions
}

/** 视频缓存文件扩展名：优先 MIME，其次显示名自带扩展名，兜底 .mp4。 */
private fun videoExtensionFor(mime: String, displayName: String?): String {
    val lower = mime.lowercase()
    when {
        lower.contains("mp4") -> return ".mp4"
        lower.contains("quicktime") || lower.contains("mov") -> return ".mov"
        lower.contains("3gpp") -> return ".3gp"
        lower.contains("webm") -> return ".webm"
        lower.contains("matroska") || lower.contains("mkv") -> return ".mkv"
        lower.contains("avi") -> return ".avi"
    }
    val nameExt = displayName
        ?.substringAfterLast('.', "")
        ?.takeIf { ext -> ext.length in 2..5 && ext.all { ch -> ch.isLetterOrDigit() } }
    return if (nameExt != null) ".$nameExt" else ".mp4"
}

private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}.getOrNull()

private fun decodeImageSize(file: File): Pair<Int, Int> = runCatching {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, options)
    options.outWidth to options.outHeight
}.getOrDefault(0 to 0)

internal fun formatMediaSize(bytes: Long): String = when {
    bytes <= 0L -> ""
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L ->
        String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.getDefault(), "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
}
