package com.material3tim.ui.screen.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.material3tim.R
import com.material3tim.data.model.ChatItem
import com.material3tim.data.model.ContactItem
import com.material3tim.data.model.GlobalSearchMessage
import com.material3tim.data.model.GlobalSearchResult
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** 全局搜索页的 UI 状态。 */
internal sealed interface GlobalSearchUiState {
    /** 还没有输入关键词。 */
    data object Idle : GlobalSearchUiState

    /** 正在搜索。 */
    data object Loading : GlobalSearchUiState

    /** 搜索成功但消息/联系人/群聊都没有结果。 */
    data object Empty : GlobalSearchUiState

    /** 搜索失败（宿主数据源返回 null 或抛异常）。 */
    data object Failed : GlobalSearchUiState

    /** 独立 App / 宿主未注册搜索数据源。 */
    data object HostOnly : GlobalSearchUiState

    /** 搜索成功的结果。 */
    data class Results(val result: GlobalSearchResult) : GlobalSearchUiState
}

/** 输入防抖时长：连续输入时只搜索最后一次关键词。 */
private const val GlobalSearchDebounceMillis = 300L

/** 单次搜索最多返回的消息条数（联系人/群聊由宿主数据源限制）。 */
private const val GlobalSearchMessageLimit = 50

/**
 * 全局搜索页入口（从消息页顶部搜索框进入，`Route.GlobalSearch`）。
 *
 * 搜索框关键词、防抖与三组结果的状态在这里维护，再按 [LocalUiMode] 分发两套纯展示实现。
 * 数据来自 [AppDataSources.searchSource]（宿主里非空；独立 App 为 null → 显示仅宿主可用）。
 */
@Composable
fun GlobalSearchScreen() {
    val navigator = LocalNavigator.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var keyword by rememberSaveable { mutableStateOf("") }
    var retryTick by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<GlobalSearchUiState>(GlobalSearchUiState.Idle) }

    LaunchedEffect(keyword, retryTick) {
        val query = keyword.trim()
        if (query.isEmpty()) {
            state = GlobalSearchUiState.Idle
            return@LaunchedEffect
        }
        // 防抖：关键词继续变化时本协程被取消，只有停手 300ms 后才真正搜索。
        delay(GlobalSearchDebounceMillis)
        state = GlobalSearchUiState.Loading
        val source = AppDataSources.searchSource
        if (source == null) {
            state = GlobalSearchUiState.HostOnly
            return@LaunchedEffect
        }
        // 数据源是阻塞调用，必须放 IO 线程；异常/失败都归到 Failed 状态。
        val raw = withContext(Dispatchers.IO) {
            runCatching { source.searchAll(query, GlobalSearchMessageLimit) }.getOrNull()
        }
        state = when {
            raw == null -> GlobalSearchUiState.Failed
            raw.messages.isEmpty() && raw.contacts.isEmpty() && raw.groups.isEmpty() ->
                GlobalSearchUiState.Empty
            else -> GlobalSearchUiState.Results(raw)
        }
    }

    val onBack: () -> Unit = { navigator.pop() }
    val onKeywordChange: (String) -> Unit = { keyword = it }
    val onRetry: () -> Unit = { retryTick += 1 }
    // 点击消息：进聊天详情页并用 jumpToSeq/jumpToMsgId 定位到该条消息。
    val onOpenMessage: (GlobalSearchMessage) -> Unit = { message ->
        keyboardController?.hide()
        navigator.push(
            Route.ChatDetail(
                chatId = message.chatId,
                jumpToSeq = message.msgSeq,
                jumpToMsgId = message.msgId,
            ),
        )
    }
    val onOpenContact: (ContactItem) -> Unit = { contact ->
        keyboardController?.hide()
        navigator.push(Route.ContactDetail(contactId = contact.id))
    }
    val onOpenGroup: (ChatItem) -> Unit = { group ->
        keyboardController?.hide()
        navigator.push(Route.ChatDetail(chatId = group.id))
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> GlobalSearchMiuix(
            keyword = keyword,
            state = state,
            onKeywordChange = onKeywordChange,
            onBack = onBack,
            onRetry = onRetry,
            onOpenMessage = onOpenMessage,
            onOpenContact = onOpenContact,
            onOpenGroup = onOpenGroup,
        )
        UiMode.Material -> GlobalSearchMaterial(
            keyword = keyword,
            state = state,
            onKeywordChange = onKeywordChange,
            onBack = onBack,
            onRetry = onRetry,
            onOpenMessage = onOpenMessage,
            onOpenContact = onOpenContact,
            onOpenGroup = onOpenGroup,
        )
    }
}


/**
 * 搜索页「群聊/会话」行头像：与消息列表同一组件/尺寸（48dp）——
 * [com.material3tim.ui.component.ChatAvatar] 内部优先加载 [ChatItem.avatarPath] 本地真实头像，
 * 为空时回退 [ChatItem.avatarSeed] 渐变 + 群标，且组件内部已做圆形裁剪。
 */
@Composable
internal fun GlobalSearchChatAvatar(chat: ChatItem, modifier: Modifier = Modifier) {
    ChatAvatar(
        name = chat.name,
        seed = chat.avatarSeed,
        modifier = modifier,
        size = 48.dp,
        isOnline = chat.isOnline,
        isGroup = chat.isGroup,
        avatarPath = chat.avatarPath,
    )
}

/** 联系人副标题：优先个性签名，其次从 id 里解析出的 QQ/uid 数字部分（宿主 id 形如 tim_friend_xxx）。 */
internal fun contactSearchSecondary(contact: ContactItem): String {
    if (contact.signature.isNotBlank()) return contact.signature
    val raw = contact.id.removePrefix("tim_friend_")
    return raw.takeIf { value -> value.isNotBlank() && value != contact.id } ?: ""
}

/** 群号：从 `tim_2_<群号>` 里解析；非群 id 返回空串。 */
internal fun groupCodeOfChatIdForSearch(chatId: String): String {
    val code = chatId.removePrefix("tim_2_")
    return code.takeIf { it != chatId && it.isNotBlank() && it.all(Char::isDigit) }.orEmpty()
}
