package com.material3tim.ui.screen.chat

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.material3tim.R
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.model.ChatMessageType
import com.material3tim.data.model.ForwardedFile
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 当前聊天记录页面的 msgId：作为「嵌套记录卡片再次打开」时的 parentMsgId。
 * 聊天页默认 0；记录页内由 [ChatRecordScreen] 提供当前页 msgId。
 */
internal val LocalChatRecordParentMsgId = compositionLocalOf<Long> { 0L }

/**
 * 当前聊天记录页面的「最外层根记录 msgId」：作为下一层嵌套记录打开时的 rootMsgId。
 * 聊天页默认 0；记录页内由 [ChatRecordScreen] 提供（顶层页为自身 msgId，嵌套页沿用传入的 root）。
 */
internal val LocalChatRecordRootMsgId = compositionLocalOf<Long> { 0L }

/** 聊天记录页加载状态（M3E / Miuix 共用）。 */
internal enum class ChatRecordUiState {
    Loading,
    Success,
    Failed,
    Empty,
}

// 加载/重试/媒体轮询参数：集中管理，避免魔法数字散落在 Composable 里。
private const val LogTag = "Material3Tim"
private const val RECORD_LOAD_MAX_ATTEMPTS = 3
private const val RECORD_LOAD_RETRY_DELAY_MS = 500L
private const val MEDIA_POLL_MAX_ATTEMPTS = 20
private const val MEDIA_POLL_INTERVAL_MS = 400L

/**
 * 聊天记录气泡点击统一逻辑（M3E / Miuix 共用）：
 * 读取当前页的 parent/root CompositionLocal → 打诊断日志 → 构造待 push 的 Route。
 * 聊天页内 parent/root 为 0；记录页内嵌卡片为当前页 msgId / 最外层 root。
 */
@Composable
internal fun onChatRecordBubbleClick(
    message: ChatMessage,
    onOpen: (Route.ChatRecord) -> Unit,
): () -> Unit {
    val parentMsgId = LocalChatRecordParentMsgId.current
    val rootMsgId = LocalChatRecordRootMsgId.current
    return {
        Log.i(
            LogTag,
            "chatRecord: open from bubble chat=${message.chatId} " +
                "msgId=${message.chatRecordMsgId} elementId=${message.chatRecordElementId} " +
                "parent=$parentMsgId root=$rootMsgId",
        )
        onOpen(
            Route.ChatRecord(
                chatId = message.chatId,
                msgId = message.chatRecordMsgId,
                elementId = message.chatRecordElementId,
                parentMsgId = parentMsgId,
                rootMsgId = rootMsgId,
            ),
        )
    }
}

/** 取一次聊天记录（IO）；失败/异常返回 null。 */
private suspend fun loadChatRecordOnce(
    chatId: String,
    msgId: Long,
    elementId: Long,
    parentMsgId: Long,
    rootMsgId: Long,
): List<ChatMessage>? = withContext(Dispatchers.IO) {
    runCatching {
        AppDataSources.messageSource?.loadChatRecord(chatId, msgId, elementId, parentMsgId, rootMsgId)
    }.getOrNull()
}

/** 首次加载带短重试：宿主偶发返回 null，最多 [RECORD_LOAD_MAX_ATTEMPTS] 次。 */
private suspend fun loadChatRecordWithRetry(
    chatId: String,
    msgId: Long,
    elementId: Long,
    parentMsgId: Long,
    rootMsgId: Long,
): List<ChatMessage>? {
    var result: List<ChatMessage>? = null
    var attempts = 0
    while (attempts < RECORD_LOAD_MAX_ATTEMPTS && result == null) {
        result = loadChatRecordOnce(chatId, msgId, elementId, parentMsgId, rootMsgId)
        attempts += 1
        if (result == null && attempts < RECORD_LOAD_MAX_ATTEMPTS) {
            delay(RECORD_LOAD_RETRY_DELAY_MS)
        }
    }
    return result
}

/** 是否有媒体路径待内核下载（决定是否进入媒体轮询）。 */
private fun List<ChatMessage>.hasMissingMedia(): Boolean = any { item ->
    (item.type == ChatMessageType.IMAGE && item.imagePath.isNullOrBlank()) ||
        (item.type == ChatMessageType.VIDEO && item.videoThumbPath.isNullOrBlank())
}

/**
 * 图片/视频缩略图下载轮询：只刷新内容，不改变 Success 状态、不阻塞已加载消息显示。
 * 间隔 400ms × 最多 20 次（约 8s），首轮即等 400ms；无缺失媒体立即停止。
 * 日志降噪：轮询过程不打印 UI 日志（桥接侧有 richmedia 日志），避免刷屏。
 */
private suspend fun pollRecordMissingMedia(
    chatId: String,
    msgId: Long,
    elementId: Long,
    parentMsgId: Long,
    rootMsgId: Long,
    currentMessages: () -> List<ChatMessage>,
    onUpdated: (List<ChatMessage>) -> Unit,
) {
    var attempt = 0
    while (attempt < MEDIA_POLL_MAX_ATTEMPTS) {
        if (!currentMessages().hasMissingMedia()) return
        delay(MEDIA_POLL_INTERVAL_MS)
        val refreshed = loadChatRecordOnce(chatId, msgId, elementId, parentMsgId, rootMsgId)
        if (refreshed != null && refreshed.isNotEmpty() && refreshed != currentMessages()) {
            onUpdated(refreshed)
        }
        attempt += 1
    }
}

/**
 * 聊天记录页：展示一条合并转发消息里的全部消息，
 * 行样式与聊天页面完全一致（复用 ChatMessageItem*）。
 *
 * 主 Composable 只做状态分发；加载/重试/轮询由上面的独立函数负责。
 * 状态明确区分 Loading / Success / Failed / Empty，避免加载失败/空记录时看起来是空白页。
 */
@Composable
fun ChatRecordScreen(
    chatId: String,
    msgId: Long,
    elementId: Long,
    parentMsgId: Long = 0L,
    rootMsgId: Long = 0L,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val filePaths = remember(chatId, msgId, elementId, parentMsgId, rootMsgId) {
        mutableStateMapOf<String, String>()
    }
    var openingFile by remember(chatId, msgId, elementId, parentMsgId, rootMsgId) {
        mutableStateOf<String?>(null)
    }
    var messages by remember(chatId, msgId, elementId, parentMsgId, rootMsgId) {
        mutableStateOf<List<ChatMessage>>(emptyList())
    }
    var state by remember(chatId, msgId, elementId, parentMsgId, rootMsgId) {
        mutableStateOf(ChatRecordUiState.Loading)
    }
    var retryTick by remember(chatId, msgId, elementId, parentMsgId, rootMsgId) {
        mutableStateOf(0)
    }

    LaunchedEffect(chatId, msgId, elementId, parentMsgId, rootMsgId, retryTick) {
        Log.i(
            LogTag,
            "chatRecord: screen enter chat=$chatId msgId=$msgId elementId=$elementId " +
                "parent=$parentMsgId root=$rootMsgId retry=$retryTick",
        )
        state = ChatRecordUiState.Loading
        val loaded = loadChatRecordWithRetry(chatId, msgId, elementId, parentMsgId, rootMsgId)
        if (loaded == null) {
            state = ChatRecordUiState.Failed
            Log.i(
                LogTag,
                "chatRecord: screen loaded chat=$chatId msgId=$msgId count=0 loaded=false",
            )
            return@LaunchedEffect
        }
        messages = loaded
        state = if (loaded.isEmpty()) ChatRecordUiState.Empty else ChatRecordUiState.Success
        Log.i(
            LogTag,
            "chatRecord: screen loaded chat=$chatId msgId=$msgId count=${loaded.size} loaded=true",
        )
        if (loaded.isNotEmpty()) {
            pollRecordMissingMedia(
                chatId = chatId,
                msgId = msgId,
                elementId = elementId,
                parentMsgId = parentMsgId,
                rootMsgId = rootMsgId,
                currentMessages = { messages },
                onUpdated = { refreshed -> messages = refreshed },
            )
        }
    }

    val onFileClick: (ChatMessage) -> Unit = { message ->
        if (openingFile == null) {
            openingFile = message.id
            scope.launch {
                try {
                    val source = AppDataSources.messageSource
                    val knownPath = filePaths[message.id] ?: message.mediaPath
                    val local = withContext(Dispatchers.IO) {
                        knownPath?.takeIf { File(it).let { file -> file.isFile && file.length() > 0L } }
                    }
                    val path = local ?: run {
                        Toast.makeText(context, context.getString(R.string.chat_file_downloading), Toast.LENGTH_SHORT).show()
                        withContext(Dispatchers.IO) {
                            source?.downloadForwardedFile(
                                ForwardedFile(
                                    chatId = chatId,
                                    recordMsgId = msgId,
                                    recordElementId = elementId,
                                    parentMsgId = parentMsgId,
                                    rootMsgId = rootMsgId,
                                    messageId = message.msgId,
                                    elementId = message.elementId,
                                    rowId = message.id,
                                ),
                            )
                        }
                    }
                    if (path != null) filePaths[message.id] = path
                    val error = when {
                        path == null -> R.string.chat_file_download_failed
                        !withContext(Dispatchers.IO) { source?.openLocalFile(path) == true } ->
                            R.string.chat_file_open_failed
                        else -> null
                    }
                    if (error != null) Toast.makeText(context, context.getString(error), Toast.LENGTH_SHORT).show()
                } finally {
                    openingFile = null
                }
            }
        }
    }
    val paths = filePaths.toMap()
    val visibleMessages = remember(messages, paths) {
        if (paths.isEmpty()) messages else messages.map { message ->
            paths[message.id]?.let { message.copy(mediaPath = it) } ?: message
        }
    }
    val onBack: () -> Unit = { navigator.pop() }
    val onRetry: () -> Unit = {
        messages = emptyList()
        retryTick += 1
    }
    // 向子树提供当前页 msgId 与最外层 root：记录页内嵌的「聊天记录」卡片点开时作为 parent/root。
    // 顶层页 root = 自身 msgId；嵌套页沿用外层传入的 root。
    val providedRootMsgId = if (rootMsgId != 0L) rootMsgId else msgId
    CompositionLocalProvider(
        LocalChatRecordParentMsgId provides msgId,
        LocalChatRecordRootMsgId provides providedRootMsgId,
        LocalChatImageMessages provides visibleMessages,
    ) {
        when (LocalUiMode.current) {
            UiMode.Miuix -> ChatRecordMiuix(
                messages = visibleMessages,
                state = state,
                onBack = onBack,
                onRetry = onRetry,
                onFileClick = onFileClick,
                openingFile = openingFile,
            )
            UiMode.Material -> ChatRecordMaterial(
                messages = visibleMessages,
                state = state,
                onBack = onBack,
                onRetry = onRetry,
                onFileClick = onFileClick,
                openingFile = openingFile,
            )
        }
    }
}

/** 视频时长显示：mm:ss */
internal fun formatVideoDuration(seconds: Int): String {
    val total = seconds.coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}
