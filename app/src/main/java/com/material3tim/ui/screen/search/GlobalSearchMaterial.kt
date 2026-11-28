package com.material3tim.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.ChatItem
import com.material3tim.data.model.ContactItem
import com.material3tim.data.model.GlobalSearchMessage
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.SegmentedItem
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors

/**
 * 全局搜索页（Material 3 Expressive）：
 * 顶栏返回 + 自动聚焦搜索框（可清空、IME 搜索）+ 消息/联系人/群聊分组结果，
 * 以及空闲/加载/无结果/失败/仅宿主可用状态。搜索逻辑与状态由 [GlobalSearchScreen] 统一维护。
 */
@Composable
internal fun GlobalSearchMaterial(
    keyword: String,
    state: GlobalSearchUiState,
    onKeywordChange: (String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenMessage: (GlobalSearchMessage) -> Unit,
    onOpenContact: (ContactItem) -> Unit,
    onOpenGroup: (ChatItem) -> Unit,
) {
    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.global_search_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    TopBarBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.chat_detail_back),
                    )
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            GlobalSearchFieldMaterial(
                keyword = keyword,
                onKeywordChange = onKeywordChange,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when (state) {
                    GlobalSearchUiState.Idle -> GlobalSearchHintMaterial(
                        stringResource(R.string.global_search_idle),
                    )
                    GlobalSearchUiState.HostOnly -> GlobalSearchHintMaterial(
                        stringResource(R.string.global_search_host_only),
                    )
                    GlobalSearchUiState.Loading -> GlobalSearchLoadingMaterial()
                    GlobalSearchUiState.Failed -> GlobalSearchFailedMaterial(onRetry = onRetry)
                    GlobalSearchUiState.Empty -> GlobalSearchHintMaterial(
                        stringResource(R.string.global_search_empty),
                    )
                    is GlobalSearchUiState.Results -> GlobalSearchResultsMaterial(
                        result = state.result,
                        onOpenMessage = onOpenMessage,
                        onOpenContact = onOpenContact,
                        onOpenGroup = onOpenGroup,
                    )
                }
            }
        }
    }
}

@Composable
private fun GlobalSearchFieldMaterial(
    keyword: String,
    onKeywordChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(44.dp),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                ),
                decorationBox = { innerTextField ->
                    if (keyword.isEmpty()) {
                        Text(
                            text = stringResource(R.string.global_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                },
            )
            if (keyword.isNotEmpty()) {
                IconButton(
                    onClick = { onKeywordChange("") },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** 全局搜索结果分区列表（消息/联系人/群）；首页 SearchAppBar 搜索展开层与全局搜索页共用。 */
@Composable
internal fun GlobalSearchResultsMaterial(
    result: com.material3tim.data.model.GlobalSearchResult,
    onOpenMessage: (GlobalSearchMessage) -> Unit,
    onOpenContact: (ContactItem) -> Unit,
    onOpenGroup: (ChatItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        // 同一类别的搜索结果为一张 segmented 卡片（2dp 分段间距），与项目其它列表一致。
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (result.messages.isNotEmpty()) {
            item(key = "section_messages") {
                GlobalSearchSectionHeaderMaterial(
                    text = stringResource(R.string.global_search_section_messages),
                    count = result.messages.size,
                )
            }
            itemsIndexed(
                result.messages,
                key = { _, item -> "msg_${item.chatId}_${item.msgId}_${item.msgSeq}_${item.timestampMs}" },
            ) { index, message ->
                GlobalSearchMessageRowMaterial(
                    message = message,
                    index = index,
                    count = result.messages.size,
                    onClick = { onOpenMessage(message) },
                )
            }
        }
        if (result.contacts.isNotEmpty()) {
            item(key = "section_contacts") {
                GlobalSearchSectionHeaderMaterial(
                    text = stringResource(R.string.global_search_section_contacts),
                    count = result.contacts.size,
                )
            }
            itemsIndexed(result.contacts, key = { _, item -> "contact_${item.id}" }) { index, contact ->
                GlobalSearchContactRowMaterial(
                    contact = contact,
                    index = index,
                    count = result.contacts.size,
                    onClick = { onOpenContact(contact) },
                )
            }
        }
        if (result.groups.isNotEmpty()) {
            item(key = "section_groups") {
                GlobalSearchSectionHeaderMaterial(
                    text = stringResource(R.string.global_search_section_groups),
                    count = result.groups.size,
                )
            }
            itemsIndexed(result.groups, key = { _, item -> "group_${item.id}" }) { index, group ->
                GlobalSearchGroupRowMaterial(
                    group = group,
                    index = index,
                    count = result.groups.size,
                    onClick = { onOpenGroup(group) },
                )
            }
        }
    }
}

@Composable
private fun GlobalSearchSectionHeaderMaterial(text: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$text ($count)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun GlobalSearchMessageRowMaterial(
    message: GlobalSearchMessage,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val trailing: (@Composable () -> Unit)? = if (message.timeLabel.isNotBlank()) {
        {
            Text(
                text = message.timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    } else {
        null
    }
    SegmentedItem(index = index, count = count) {
        SegmentedListItem(
            onClick = onClick,
            leadingContent = {
                ChatAvatar(
                    name = message.chatName,
                    seed = message.chatName.hashCode(),
                    size = 44.dp,
                    isGroup = message.isGroup,
                )
            },
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.chatName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (message.isGroup) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.global_search_tag_group),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            },
            supportingContent = {
                Column {
                    if (message.senderName.isNotBlank()) {
                        Text(
                            text = message.senderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = message.preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            trailingContent = trailing,
        )
    }
}

@Composable
private fun GlobalSearchContactRowMaterial(
    contact: ContactItem,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val secondary = contactSearchSecondary(contact)
    val supporting: (@Composable () -> Unit)? = if (secondary.isNotBlank()) {
        {
            Text(
                text = secondary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        null
    }
    SegmentedItem(index = index, count = count) {
        SegmentedListItem(
            onClick = onClick,
            leadingContent = {
                ChatAvatar(
                    name = contact.name,
                    seed = contact.avatarSeed,
                    size = 44.dp,
                    isOnline = contact.isOnline,
                    avatarPath = contact.avatarPath,
                )
            },
            headlineContent = {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = supporting,
        )
    }
}

@Composable
private fun GlobalSearchGroupRowMaterial(
    group: ChatItem,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val groupCode = groupCodeOfChatIdForSearch(group.id)
    val secondary = buildString {
        append(stringResource(R.string.chat_member_count, group.memberCount))
        if (groupCode.isNotBlank()) {
            append(" · ")
            append(groupCode)
        }
    }
    SegmentedItem(index = index, count = count) {
        SegmentedListItem(
            onClick = onClick,
            leadingContent = { GlobalSearchChatAvatar(chat = group, modifier = Modifier.size(44.dp)) },
            headlineContent = {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
    }
}

@Composable
private fun GlobalSearchHintMaterial(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GlobalSearchLoadingMaterial() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GlobalSearchFailedMaterial(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.global_search_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.global_search_retry))
        }
    }
}
