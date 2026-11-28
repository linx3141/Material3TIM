package com.material3tim.ui.screen.forward

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.material3tim.R
import com.material3tim.data.model.ChatItem
import com.material3tim.data.repository.LiveChats
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.screen.chat.GroupAssistantChatId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 转发页：会话列表（与主页消息列表同构），点会话直接转发；
 * 顶栏「多选」开启后，卡片内部右侧出现 checkbox，可一次转发到多个会话。
 *
 * 顶部固定展示来源预览卡：「转发 N 条消息」+ 单条消息摘要首行（多选转发时 preview 为空，只显示条数）。
 * 数据分支：宿主订阅 [LiveChats.liveFlow] 保持目标会话实时；独立 App 回落示例会话。
 *
 * @param preview 单条转发的来源消息摘要（多选转发为空字符串）。
 */
@Composable
fun ForwardScreen(srcChatId: String, msgIds: List<Long>, preview: String = "") {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    // null = 首次加载尚未结束；加载完成（含兜底）后写入列表，用于驱动 loading 态。
    var initialChats by remember { mutableStateOf<List<ChatItem>?>(null) }
    var multiSelect by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var sending by remember { mutableStateOf(false) }

    val liveChats by LiveChats.liveFlow.collectAsStateWithLifecycle()

    LaunchedEffect(srcChatId) {
        // 宿主(TIM 进程)：先拉真实会话列表；独立 App(无数据源)或拉取失败时回落
        // [LiveChats.current]（内部已含 sampleChats），保证转发页始终有可选的目标会话。
        val live = withContext(Dispatchers.IO) {
            runCatching { AppDataSources.chatSource?.loadChats() }.getOrNull()
        }
        initialChats = live ?: LiveChats.current
    }

    // 宿主：目标会话跟随 LiveChats 实时刷新（内核推送新会话后转发页同步）；
    // 独立 App：liveFlow 恒为 null，用首次加载的示例会话列表。
    val chats = if (AppDataSources.hasLiveDataSource) {
        liveChats ?: initialChats.orEmpty()
    } else {
        initialChats.orEmpty()
    }
    val loading = initialChats == null && chats.isEmpty()

    // 独立 App(无宿主消息数据源)里转发能力不可用：必须给出明确提示，不能点击后静默无反应。
    val forwardUnavailableMessage = stringResource(R.string.forward_unavailable)
    val sendTo: (List<String>) -> Unit = { targets ->
        if (!sending && targets.isNotEmpty()) {
            val messageSource = AppDataSources.messageSource
            if (messageSource == null) {
                android.widget.Toast.makeText(
                    context,
                    forwardUnavailableMessage,
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
            } else {
                sending = true
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            messageSource.forwardMessages(srcChatId, msgIds, targets)
                        }.getOrDefault(false)
                    }
                    android.widget.Toast.makeText(
                        context,
                        if (ok) {
                            context.getString(R.string.forward_success_format, targets.size)
                        } else {
                            context.getString(R.string.forward_failed)
                        },
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                    sending = false
                    if (ok) navigator.pop()
                }
            }
        }
    }

    val onClickChat: (ChatItem) -> Unit = { chat ->
        if (chat.id == GroupAssistantChatId) {
            // 群助手是伪会话：非多选时进入群助手群列表选择目标群；
            // 多选转发时不可选（与 TIM 原生一致）。
            if (!multiSelect) {
                navigator.push(Route.ForwardGroupAssistant(srcChatId, msgIds, preview))
            }
        } else if (multiSelect) {
            selected = if (chat.id in selected) selected - chat.id else selected + chat.id
        } else {
            sendTo(listOf(chat.id))
        }
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> MiuixForwardPage(
            chats = chats,
            messageCount = msgIds.size,
            preview = preview,
            loading = loading,
            multiSelect = multiSelect,
            selected = selected,
            enabled = !sending,
            onBack = { navigator.pop() },
            onToggleMultiSelect = {
                multiSelect = !multiSelect
                if (!multiSelect) selected = emptySet()
            },
            onSendSelected = { sendTo(selected.toList()) },
            onChatClick = onClickChat,
        )

        UiMode.Material -> MaterialForwardPage(
            chats = chats,
            messageCount = msgIds.size,
            preview = preview,
            loading = loading,
            multiSelect = multiSelect,
            selected = selected,
            enabled = !sending,
            onBack = { navigator.pop() },
            onToggleMultiSelect = {
                multiSelect = !multiSelect
                if (!multiSelect) selected = emptySet()
            },
            onSendSelected = { sendTo(selected.toList()) },
            onChatClick = onClickChat,
        )
    }
}

@Composable
private fun MiuixForwardPage(
    chats: List<ChatItem>,
    messageCount: Int,
    preview: String,
    loading: Boolean,
    multiSelect: Boolean,
    selected: Set<String>,
    enabled: Boolean,
    onBack: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onSendSelected: () -> Unit,
    onChatClick: (ChatItem) -> Unit,
) {
    val colorScheme = MiuixTheme.colorScheme
    MiuixScaffold(
        topBar = {
            SmallTopAppBar(
                title = if (multiSelect) "已选 ${selected.size} 个会话" else "转发",
                navigationIcon = {
                    Box(
                        modifier = Modifier.padding(start = 12.dp),
                    ) {
                        MiuixIcon(
                            imageVector = MiuixIcons.Close,
                            contentDescription = "关闭",
                            tint = colorScheme.onSurface,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable(onClick = onBack),
                        )
                    }
                },
                actions = {
                    if (multiSelect) {
                        MiuixTextButton(
                            text = "发送",
                            onClick = onSendSelected,
                            enabled = selected.isNotEmpty(),
                        )
                    } else {
                        MiuixTextButton(text = "多选", onClick = onToggleMultiSelect)
                    }
                },
            )
        },
    ) { padding ->
        if (loading) {
            // 目标会话首次加载中：居中 loading，避免先闪一下空态。
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ForwardListContent(
                chats = chats,
                messageCount = messageCount,
                preview = preview,
                multiSelect = multiSelect,
                selected = selected,
                enabled = enabled,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                onChatClick = onChatClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialForwardPage(
    chats: List<ChatItem>,
    messageCount: Int,
    preview: String,
    loading: Boolean,
    multiSelect: Boolean,
    selected: Set<String>,
    enabled: Boolean,
    onBack: () -> Unit,
    onToggleMultiSelect: () -> Unit,
    onSendSelected: () -> Unit,
    onChatClick: (ChatItem) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (multiSelect) "已选 ${selected.size} 个会话" else "转发") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                },
                actions = {
                    if (multiSelect) {
                        TextButton(onClick = onSendSelected, enabled = selected.isNotEmpty()) {
                            Text("发送")
                        }
                    } else {
                        TextButton(onClick = onToggleMultiSelect) {
                            Text("多选")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (loading) {
            // 目标会话首次加载中：居中 loading，避免先闪一下空态。
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            ForwardListContent(
                chats = chats,
                messageCount = messageCount,
                preview = preview,
                multiSelect = multiSelect,
                selected = selected,
                enabled = enabled,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
                onChatClick = onChatClick,
            )
        }
    }
}

/**
 * 转发页列表内容（两种风格共用）：来源预览卡 + 目标会话列表 / 空态。
 * 卡片行样式由 [ForwardChatCard] 内部按当前 UI 风格着色。
 */
@Composable
private fun ForwardListContent(
    chats: List<ChatItem>,
    messageCount: Int,
    preview: String,
    multiSelect: Boolean,
    selected: Set<String>,
    enabled: Boolean,
    contentPadding: PaddingValues,
    onChatClick: (ChatItem) -> Unit,
) {
    val uiMode = LocalUiMode.current
    val emptyColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.onSurfaceVariant
        UiMode.Miuix -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "preview") {
            ForwardPreviewCard(preview = preview, messageCount = messageCount)
        }
        if (chats.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.forward_empty),
                        fontSize = 14.sp,
                        color = emptyColor,
                    )
                }
            }
        } else {
            items(chats, key = { it.id }) { chat ->
                ForwardChatCard(
                    chat = chat,
                    multiSelect = multiSelect,
                    checked = chat.id in selected,
                    selectable = chat.id != GroupAssistantChatId,
                    onClick = { if (enabled) onChatClick(chat) },
                )
            }
        }
    }
}

/** 来源预览卡：转发条数 + 单条消息摘要首行（摘要为空时只显示条数）。 */
@Composable
private fun ForwardPreviewCard(
    preview: String,
    messageCount: Int,
    modifier: Modifier = Modifier,
) {
    val uiMode = LocalUiMode.current
    val container = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surfaceContainerHigh
        UiMode.Miuix -> MiuixTheme.colorScheme.surfaceContainerHigh
    }
    val titleColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.onSurface
        UiMode.Miuix -> MiuixTheme.colorScheme.onSurface
    }
    val bodyColor = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.onSurfaceVariant
        UiMode.Miuix -> MiuixTheme.colorScheme.onSurfaceVariantSummary
    }
    val summary = preview.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.forward_preview_count, messageCount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (summary.isNotEmpty()) {
            Text(
                text = summary,
                fontSize = 13.sp,
                color = bodyColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ForwardChatCard(
    chat: ChatItem,
    multiSelect: Boolean,
    checked: Boolean,
    selectable: Boolean = true,
    onClick: () -> Unit,
) {
    // 多选时群助手不可选：整行置灰且不响应点击，也不显示 checkbox。
    val rowAlpha = if (multiSelect && !selectable) 0.45f else 1f
    val uiMode = LocalUiMode.current
    val container = when (uiMode) {
        UiMode.Material -> MaterialTheme.colorScheme.surfaceContainerHigh
        UiMode.Miuix -> MiuixTheme.colorScheme.surfaceContainerHigh
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(container)
            .alpha(rowAlpha)
            .clickable(enabled = !(multiSelect && !selectable), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAvatar(
            name = chat.name,
            seed = chat.avatarSeed,
            size = 48.dp,
            isGroup = chat.isGroup,
            avatarPath = chat.avatarPath,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = chat.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = when (uiMode) {
                    UiMode.Material -> MaterialTheme.colorScheme.onSurface
                    UiMode.Miuix -> MiuixTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = chat.displaySummary,
                fontSize = 13.sp,
                color = when (uiMode) {
                    UiMode.Material -> MaterialTheme.colorScheme.onSurfaceVariant
                    UiMode.Miuix -> MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (multiSelect && selectable) {
            Spacer(Modifier.width(8.dp))
            when (uiMode) {
                UiMode.Material -> Checkbox(
                    checked = checked,
                    onCheckedChange = { onClick() },
                )
                UiMode.Miuix -> MiuixCheckbox(
                    state = if (checked) ToggleableState.On else ToggleableState.Off,
                    onClick = onClick,
                )
            }
        }
    }
}
