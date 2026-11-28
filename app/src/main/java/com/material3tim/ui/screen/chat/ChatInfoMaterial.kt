package com.material3tim.ui.screen.chat

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.ChatInfoData
import com.material3tim.data.model.ChatMember
import com.material3tim.data.repository.LiveChats
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.DataLineAvatar
import com.material3tim.ui.component.material.ExpressiveAlertDialog
import com.material3tim.ui.component.material.ExpressiveConfirmDialog
import com.material3tim.ui.component.material.ExpressiveInputDialog
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.SegmentedSwitchItem
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route

/**
 * 聊天信息页（Material 3 Expressive 风格）：
 * 头部资料卡、群成员宫格、群公告、置顶/免打扰开关、若干操作行。
 * 置顶 / 消息免打扰走宿主 ChatSettingSource（独立 App 提示仅宿主可用）；
 * 其余操作暂为占位提示。
 */
@Composable
internal fun ChatInfoMaterial(
    data: ChatInfoData,
    chatId: String,
    onBack: () -> Unit,
    onViewAllMembers: () -> Unit = {},
    onOpenNotice: () -> Unit = {},
    onSearchHistory: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onExit: () -> Unit = {},
    loading: Boolean = false,
    loadFailed: Boolean = false,
    onRetry: () -> Unit = {},
) {
    val chat = data.chat
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val comingSoon = remember(context) {
        {
            Toast.makeText(context, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show()
        }
    }
    val hostOnlyMessage = stringResource(R.string.chat_info_host_only)
    val clearSuccessMessage = stringResource(R.string.chat_info_clear_success)
    val clearFailedMessage = stringResource(R.string.chat_info_clear_failed)
    val deleteFriendSuccessMessage = stringResource(R.string.chat_info_delete_friend_success)
    val deleteFriendFailedMessage = stringResource(R.string.chat_info_delete_friend_failed)
    val avatarEditorFailedMessage = stringResource(R.string.chat_info_avatar_editor_failed)
    val settingFailedMessage = stringResource(R.string.chat_info_setting_failed)
    // 用 chat.isPinned / isMuted 作为 key：宿主数据异步到达、父级用最新 ChatItem 覆盖 data.chat 时，
    // 开关状态会跟着重置为最新值，保证初始状态准确。
    var pinned by rememberSaveable(chat.id, chat.isPinned) { mutableStateOf(chat.isPinned) }
    var muted by rememberSaveable(chat.id, chat.isMuted) { mutableStateOf(chat.isMuted) }
    val onPinnedChange: (Boolean) -> Unit = { checked ->
        scope.updateChatSetting(
            context = context,
            newValue = checked,
            hostOnlyMessage = hostOnlyMessage,
            failedMessage = settingFailedMessage,
            setLocal = { pinned = it },
            sourceCall = {
                AppDataSources.chatSettingSource?.setChatPinned(chat.id, checked) ?: false
            },
        )
    }
    val onMutedChange: (Boolean) -> Unit = { checked ->
        scope.updateChatSetting(
            context = context,
            newValue = checked,
            hostOnlyMessage = hostOnlyMessage,
            failedMessage = settingFailedMessage,
            setLocal = { muted = it },
            sourceCall = {
                AppDataSources.chatSettingSource?.setChatMuted(chat.id, checked) ?: false
            },
        )
    }
    // 宿主内优先真实成员列表大小（TIM 缓存的 memberCount 可能偏大）。
    val memberCount = effectiveMemberCount(data)
    val navigator = LocalNavigator.current
    val role = remember(data.members) { currentMemberRole(data.members) }
    var showNameDialog by remember(chat.id) { mutableStateOf(false) }
    var showQuitDialog by remember(chat.id) { mutableStateOf(false) }
    var showClearHistoryDialog by remember(chat.id) { mutableStateOf(false) }
    var showDeleteFriendDialog by remember(chat.id) { mutableStateOf(false) }
    var showDescriptionDialog by remember(chat.id) { mutableStateOf(false) }
    var allMuted by rememberSaveable(chat.id) { mutableStateOf(false) }
    val noPermissionMessage = stringResource(R.string.group_manage_no_permission)
    val successMessage = stringResource(R.string.group_manage_success)
    val failedMessage = stringResource(R.string.group_manage_failed)
    val isOwner = role.isOwner
    val performManageAction: (call: () -> Boolean, onSuccess: (() -> Unit)?) -> Unit =
        { call, onSuccess ->
            if (AppDataSources.groupManageSource == null) {
                Toast.makeText(context, noPermissionMessage, Toast.LENGTH_SHORT).show()
            } else {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching(call).getOrDefault(false)
                    }
                    Toast.makeText(
                        context,
                        if (ok) successMessage else failedMessage,
                        Toast.LENGTH_SHORT,
                    ).show()
                    if (ok) onSuccess?.invoke()
                }
            }
        }
    // 清空聊天记录：确认 → 内核 clearChatHistory → Toast + 会话列表刷新 → 回消息列表。
    val onClearHistory: () -> Unit = {
        if (AppDataSources.friendManageSource == null) {
            Toast.makeText(context, hostOnlyMessage, Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.friendManageSource?.clearChatHistory(chatId) ?: false
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    if (ok) clearSuccessMessage else clearFailedMessage,
                    Toast.LENGTH_SHORT,
                ).show()
                if (ok) {
                    withContext(Dispatchers.IO) { runCatching { LiveChats.refresh() } }
                    onExit()
                }
            }
        }
    }
    // 删除好友（单聊）：确认 → deleteFriend → Toast + 刷新会话 → 回消息列表。
    val onDeleteFriend: () -> Unit = {
        if (AppDataSources.friendManageSource == null) {
            Toast.makeText(context, hostOnlyMessage, Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.friendManageSource?.deleteFriend(chatId, "") ?: false
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    if (ok) deleteFriendSuccessMessage else deleteFriendFailedMessage,
                    Toast.LENGTH_SHORT,
                ).show()
                if (ok) {
                    withContext(Dispatchers.IO) { runCatching { LiveChats.refresh() } }
                    onExit()
                }
            }
        }
    }
    // 打开 TIM 原生群头像/群资料编辑页（群主/管理员）。
    val onOpenGroupAvatarEditor: () -> Unit = {
        if (AppDataSources.friendManageSource == null) {
            Toast.makeText(context, hostOnlyMessage, Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.friendManageSource?.openGroupAvatarEditor(chatId) ?: false
                    }.getOrDefault(false)
                }
                if (!ok) {
                    Toast.makeText(context, avatarEditorFailedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.chat_info),
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
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ChatInfoHeaderMaterial(
                    data = data,
                    memberCount = memberCount,
                    onNameClick = if (role.canManage) {
                        { showNameDialog = true }
                    } else {
                        null
                    },
                    onAvatarClick = if (chat.isGroup && role.canManage) {
                        onOpenGroupAvatarEditor
                    } else {
                        null
                    },
                )
            }
            item(key = "profile") {
                ChatInfoProfileMaterial(
                    data = data,
                    onDescriptionClick = if (chat.isGroup && role.canManage) {
                        { showDescriptionDialog = true }
                    } else {
                        null
                    },
                )
            }
            if (chat.isGroup) {
                when {
                    data.members.isNotEmpty() -> item(key = "members") {
                        ChatInfoMembersMaterial(
                            members = data.members,
                            memberCount = memberCount,
                            onViewAll = onViewAllMembers,
                        )
                    }
                    loading -> item(key = "members_loading") {
                        ChatInfoMembersLoadingMaterial()
                    }
                    loadFailed -> item(key = "members_failed") {
                        ChatInfoMembersLoadFailedMaterial(onRetry = onRetry)
                    }
                }
            }
            // 群公告卡片对群聊始终显示（无公告时也能进入公告页发布）
            if (data.chat.isGroup) {
                item(key = "notice") {
                    ChatInfoNoticeMaterial(notice = data.notice, onClick = onOpenNotice)
                }
            }
            if (chat.isGroup) {
                item(key = "group_files") {
                    SegmentedColumn {
                        item {
                            SegmentedListItem(
                                onClick = { navigator.push(Route.ChatFiles(chat.id)) },
                                leadingContent = {
                                    Icon(Icons.Filled.Folder, contentDescription = null)
                                },
                                headlineContent = { Text(stringResource(R.string.group_files_title)) },
                            )
                        }
                    }
                }
            }
            item(key = "switches") {
                SegmentedColumn {
                    item {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.PushPin,
                            title = stringResource(R.string.chat_info_pin),
                            checked = pinned,
                            onCheckedChange = onPinnedChange,
                        )
                    }
                    item {
                        SegmentedSwitchItem(
                            icon = Icons.Filled.NotificationsOff,
                            title = stringResource(R.string.chat_info_mute),
                            checked = muted,
                            onCheckedChange = onMutedChange,
                        )
                    }
                    if (chat.isGroup && role.canManage) {
                        item {
                            SegmentedSwitchItem(
                                icon = Icons.AutoMirrored.Filled.VolumeOff,
                                title = stringResource(R.string.group_manage_all_mute),
                                checked = allMuted,
                                onCheckedChange = { checked ->
                                    val previous = allMuted
                                    allMuted = checked
                                    performManageAction(
                                        {
                                            AppDataSources.groupManageSource
                                                ?.setGroupAllMuted(chatId, checked) ?: false
                                        },
                                        { onRefresh() },
                                    )
                                    if (AppDataSources.groupManageSource == null) {
                                        allMuted = previous
                                    }
                                },
                            )
                        }
                    }
                }
            }
            item(key = "actions") {
                SegmentedColumn {
                    item {
                        SegmentedListItem(
                            onClick = onSearchHistory,
                            leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                            headlineContent = { Text(stringResource(R.string.chat_info_search_history)) },
                        )
                    }
                    item {
                        SegmentedListItem(
                            onClick = { showClearHistoryDialog = true },
                            leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            headlineContent = { Text(stringResource(R.string.chat_info_clear)) },
                        )
                    }
                    item {
                        SegmentedListItem(
                            onClick = comingSoon,
                            leadingContent = { Icon(Icons.Filled.Report, contentDescription = null) },
                            headlineContent = { Text(stringResource(R.string.chat_info_report)) },
                        )
                    }
                    item {
                        SegmentedListItem(
                            onClick = {
                                if (chat.isGroup) {
                                    showQuitDialog = true
                                } else {
                                    showDeleteFriendDialog = true
                                }
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = if (chat.isGroup) Icons.AutoMirrored.Filled.ExitToApp else Icons.Filled.PersonRemove,
                                    contentDescription = null,
                                    tint = colorScheme.error,
                                )
                            },
                            headlineContent = {
                                Text(
                                    text = stringResource(
                                        when {
                                            !chat.isGroup -> R.string.chat_info_delete_friend
                                            isOwner -> R.string.group_manage_destroy
                                            else -> R.string.chat_info_quit_group
                                        },
                                    ),
                                    color = colorScheme.error,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
    // 群名称编辑：方案 A Expressive 居中弹窗（自绘遮罩 + 缩放淡入动画）。
    ExpressiveInputDialog(
        show = showNameDialog,
        title = stringResource(R.string.group_manage_group_name),
        initialText = chat.name,
        placeholder = stringResource(R.string.group_manage_group_name_hint),
        confirmText = stringResource(R.string.confirm),
        onConfirm = { name ->
            showNameDialog = false
            performManageAction(
                {
                    AppDataSources.groupManageSource
                        ?.setGroupName(chatId, name.trim()) ?: false
                },
                { onRefresh() },
            )
        },
        onDismissRequest = { showNameDialog = false },
    )
    // 退出/解散群：Expressive 危险确认弹窗。
    val quitTitle = stringResource(
        if (isOwner) R.string.group_manage_destroy else R.string.chat_info_quit_group,
    )
    ExpressiveConfirmDialog(
        show = showQuitDialog,
        title = quitTitle,
        message = stringResource(
            if (isOwner) R.string.group_manage_destroy_confirm else R.string.group_manage_quit_confirm,
        ),
        confirmText = quitTitle,
        danger = true,
        onConfirm = {
            showQuitDialog = false
            performManageAction(
                {
                    if (isOwner) {
                        AppDataSources.groupManageSource?.destroyGroup(chatId) ?: false
                    } else {
                        AppDataSources.groupManageSource?.quitGroup(chatId) ?: false
                    }
                },
                { onExit() },
            )
        },
        onDismissRequest = { showQuitDialog = false },
    )
    // 清空聊天记录确认。
    ExpressiveConfirmDialog(
        show = showClearHistoryDialog,
        title = stringResource(R.string.chat_info_clear),
        message = stringResource(R.string.chat_info_clear_confirm),
        confirmText = stringResource(R.string.chat_info_clear),
        danger = true,
        onConfirm = {
            showClearHistoryDialog = false
            onClearHistory()
        },
        onDismissRequest = { showClearHistoryDialog = false },
    )
    // 删除好友确认（单聊）。
    ExpressiveConfirmDialog(
        show = showDeleteFriendDialog,
        title = stringResource(R.string.chat_info_delete_friend),
        message = stringResource(R.string.chat_info_delete_friend_confirm),
        confirmText = stringResource(R.string.chat_info_delete_friend),
        danger = true,
        onConfirm = {
            showDeleteFriendDialog = false
            onDeleteFriend()
        },
        onDismissRequest = { showDeleteFriendDialog = false },
    )
    // 修改群简介（群主/管理员）。
    ExpressiveInputDialog(
        show = showDescriptionDialog,
        title = stringResource(R.string.chat_info_edit_group_desc),
        initialText = data.description,
        placeholder = stringResource(R.string.chat_info_group_desc_hint),
        confirmText = stringResource(R.string.confirm),
        singleLine = false,
        maxLines = 4,
        minHeight = 100.dp,
        onConfirm = { description ->
            showDescriptionDialog = false
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.friendManageSource
                            ?.setGroupDescription(chatId, description.trim()) ?: false
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    if (ok) successMessage else failedMessage,
                    Toast.LENGTH_SHORT,
                ).show()
                if (ok) onRefresh()
            }
        },
        onDismissRequest = { showDescriptionDialog = false },
    )
}

@Composable
private fun ChatInfoHeaderMaterial(
    data: ChatInfoData,
    memberCount: Int,
    onNameClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null,
) {
    val chat = data.chat
    TonalCard(containerColor = MaterialTheme.colorScheme.surfaceBright) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (chat.dataLineKind != 0) {
                DataLineAvatar(
                    kind = chat.dataLineKind,
                    modifier = if (onAvatarClick != null) {
                        Modifier.clip(CircleShape).clickable(onClick = onAvatarClick)
                    } else {
                        Modifier
                    },
                    size = 64.dp,
                )
            } else {
                ChatAvatar(
                    name = chat.name,
                    seed = chat.avatarSeed,
                    modifier = if (onAvatarClick != null) {
                        Modifier.clip(CircleShape).clickable(onClick = onAvatarClick)
                    } else {
                        Modifier
                    },
                    size = 64.dp,
                    isOnline = chat.isOnline,
                    isGroup = chat.isGroup,
                    avatarPath = chat.avatarPath,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.then(
                        if (onNameClick != null) Modifier.clickable(onClick = onNameClick) else Modifier,
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (chat.isGroup) {
                        stringResource(R.string.chat_member_count, memberCount)
                    } else {
                        stringResource(
                            R.string.chat_info_qq_number,
                            data.timId.ifBlank { "—" },
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChatInfoMembersMaterial(
    members: List<ChatMember>,
    memberCount: Int,
    onViewAll: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    TonalCard(
        containerColor = MaterialTheme.colorScheme.surfaceBright,
        onClick = onViewAll,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.chat_info_member_count, memberCount),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                members.take(12).chunked(4).forEach { rowMembers ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowMembers.forEach { member ->
                            MemberItemMaterial(member = member, modifier = Modifier.weight(1f))
                        }
                        repeat(4 - rowMembers.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.chat_info_view_members, memberCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun MemberItemMaterial(member: ChatMember, modifier: Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ChatAvatar(
            name = member.name,
            seed = member.avatarSeed,
            size = 44.dp,
            avatarPath = member.avatarPath,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (member.isOwner) {
            Text(
                text = stringResource(R.string.chat_info_owner),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ChatInfoNoticeMaterial(notice: String, onClick: () -> Unit) {
    // 整张卡（含「暂无群公告」空态）都显式绑定点击：进入 Route.ChatNotice(chatId)。
    TonalCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        containerColor = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.chat_info_notice),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = notice.ifBlank { stringResource(R.string.chat_info_notice_empty) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ChatInfoProfileMaterial(
    data: ChatInfoData,
    onDescriptionClick: (() -> Unit)? = null,
) {
    val chat = data.chat
    val colorScheme = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val timId = data.timId.ifBlank { "—" }
    TonalCard(containerColor = MaterialTheme.colorScheme.surfaceBright) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // 设备会话（我的手机/电脑/平板）没有 QQ 号，不显示号码行。
            if (chat.dataLineKind == 0) {
                Text(
                    text = stringResource(
                        if (chat.isGroup) R.string.chat_info_group_number else R.string.chat_info_qq_number,
                        timId,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            clipboard.setText(AnnotatedString(timId))
                            Toast.makeText(context, R.string.chat_info_copied, Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 4.dp),
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(
                text = stringResource(
                    when {
                        chat.dataLineKind != 0 -> R.string.chat_dataline_description
                        chat.isGroup -> R.string.chat_info_group_intro
                        else -> R.string.chat_info_signature
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = data.description.ifBlank { stringResource(R.string.chat_info_no_desc) },
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = if (onDescriptionClick != null) {
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDescriptionClick)
                        .padding(vertical = 4.dp)
                } else {
                    Modifier
                },
            )
        }
    }
}

/** 成员区局部 loading（M3E Expressive ContainedLoadingIndicator），高度与成员卡片接近。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatInfoMembersLoadingMaterial() {
    TonalCard(containerColor = MaterialTheme.colorScheme.surfaceBright) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            contentAlignment = Alignment.Center,
        ) {
            ContainedLoadingIndicator()
        }
    }
}

/** 成员区加载失败 + 重试（页面其它部分保持可见）。 */
@Composable
private fun ChatInfoMembersLoadFailedMaterial(onRetry: () -> Unit) {
    TonalCard(containerColor = MaterialTheme.colorScheme.surfaceBright) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.group_files_load_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.group_files_retry))
            }
        }
    }
}
