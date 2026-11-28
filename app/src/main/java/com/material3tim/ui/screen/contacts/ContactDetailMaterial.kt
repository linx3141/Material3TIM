package com.material3tim.ui.screen.contacts

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.ChatMember
import com.material3tim.data.model.ContactDetail
import com.material3tim.data.model.DataLine
import com.material3tim.data.repository.LiveChats
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.DataLineAvatar
import com.material3tim.ui.component.material.ExpressiveConfirmDialog
import com.material3tim.ui.component.material.ExpressiveInputDialog
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors
import com.material3tim.ui.screen.chat.GroupMemberRole
import com.material3tim.ui.screen.chat.MemberActionsSheetMaterial
import com.material3tim.ui.screen.chat.MemberInputDialogMaterial
import com.material3tim.ui.screen.chat.canOperateMember
import com.material3tim.ui.screen.chat.performGroupManageAction
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 联系人详情页（Material 3 Expressive 风格）。
 * 头部资料卡 + 发消息 / 通话操作 + 资料卡 + 标签 + 删除好友。
 */
@Composable
internal fun ContactDetailMaterial(
    detail: ContactDetail,
    dataLineKind: Int = 0,
    onBack: () -> Unit,
    onSendMessage: () -> Unit,
    isFriend: Boolean = true,
    isSelfProfile: Boolean = false,
    isProfileLoading: Boolean = false,
    isProfileLoadFailed: Boolean = false,
    onRetryProfile: () -> Unit = {},
    onAddFriend: () -> Unit = {},
    manageRole: GroupMemberRole? = null,
    manageTarget: ChatMember? = null,
    manageChatId: String = "",
    onProfileRefresh: () -> Unit = {},
    showRemarkEntry: Boolean = false,
    remarkUid: String = "",
    isGroupMemberProfile: Boolean = false,
    deleteFriendChatId: String = "",
    deleteFriendUid: String = "",
    onDeleteFriendSuccess: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noPermissionMessage = stringResource(R.string.group_manage_no_permission)
    val successMessage = stringResource(R.string.group_manage_success)
    val failedMessage = stringResource(R.string.group_manage_failed)
    var showManageSheet by remember { mutableStateOf(false) }
    var kickTarget by remember { mutableStateOf<ChatMember?>(null) }
    var cardTarget by remember { mutableStateOf<ChatMember?>(null) }
    var showRemarkDialog by remember { mutableStateOf(false) }
    var showDeleteFriendDialog by remember { mutableStateOf(false) }
    val remarkSuccessMessage = stringResource(R.string.contact_remark_success)
    val remarkFailedMessage = stringResource(R.string.contact_remark_failed)
    val deleteFriendSuccessMessage = stringResource(R.string.chat_info_delete_friend_success)
    val deleteFriendFailedMessage = stringResource(R.string.chat_info_delete_friend_failed)
    val hostOnlyMessage = stringResource(R.string.chat_info_host_only)
    val showManageEntry = manageRole != null && manageTarget != null &&
        canOperateMember(manageRole, manageTarget)
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val notSet = stringResource(R.string.contact_not_set)
    val loadingText = stringResource(R.string.user_profile_loading)
    val profileFailedText = stringResource(R.string.user_profile_load_failed)
    // 资料行内容：未加载 → 加载中…；失败 → 加载失败；否则真实值/未设置。
    val infoValue: (String) -> String = { raw ->
        when {
            isProfileLoading -> loadingText
            isProfileLoadFailed -> profileFailedText
            else -> raw.ifBlank { notSet }
        }
    }
    val tagChipScrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.contact_detail)) },
                navigationIcon = {
                    TopBarBackButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.back),
                    )
                },
                actions = {
                    if (showManageEntry) {
                        IconButton(onClick = { showManageSheet = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreHoriz,
                                contentDescription = stringResource(R.string.group_manage_member_actions),
                            )
                        }
                    }
                },
                colors = expressiveTopAppBarColors(),
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ContactDetailHeaderMaterial(
                    detail = detail,
                    dataLineKind = dataLineKind,
                    loading = isProfileLoading,
                    loadFailed = isProfileLoadFailed,
                )
            }
            if (isProfileLoadFailed) {
                item(key = "profile_failed") {
                    TonalCard(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = profileFailedText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onRetryProfile) {
                                Text(text = stringResource(R.string.user_profile_retry))
                            }
                        }
                    }
                }
            }
            if (!isSelfProfile && !isProfileLoading && !isProfileLoadFailed) item(key = "actions") {
                ContactDetailActionsMaterial(
                    isFriend = isFriend,
                    onSendMessage = onSendMessage,
                    onCall = showComingSoon,
                    onAddFriend = onAddFriend,
                )
            }
            if (showRemarkEntry && !isSelfProfile && !isProfileLoading && !isProfileLoadFailed) {
                item(key = "remark_action") {
                    SegmentedColumn {
                        item {
                            SegmentedListItem(
                                onClick = { showRemarkDialog = true },
                                leadingContent = {
                                    Icon(Icons.Filled.Edit, contentDescription = null)
                                },
                                headlineContent = {
                                    Text(
                                        text = stringResource(
                                            if (detail.remark.isBlank()) {
                                                R.string.contact_remark_set
                                            } else {
                                                R.string.contact_remark_edit
                                            },
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
            item(key = "info") {
                SegmentedColumn(
                    content = listOf(
                        {
                            ContactInfoRowMaterial(
                                icon = Icons.Filled.Person,
                                label = stringResource(R.string.contact_remark),
                                value = infoValue(detail.remark),
                            )
                        },
                        {
                            ContactInfoRowMaterial(
                                icon = Icons.Filled.Phone,
                                label = stringResource(R.string.contact_phone),
                                value = infoValue(detail.phone),
                            )
                        },
                        {
                            ContactInfoRowMaterial(
                                icon = Icons.Filled.Email,
                                label = stringResource(R.string.contact_email),
                                value = infoValue(detail.email),
                            )
                        },
                        {
                            ContactInfoRowMaterial(
                                icon = Icons.Filled.Place,
                                label = stringResource(R.string.contact_region),
                                value = infoValue(detail.region),
                            )
                        },
                        {
                            ContactInfoRowMaterial(
                                icon = Icons.Filled.Groups,
                                label = stringResource(R.string.contact_group),
                                value = infoValue(
                                    detail.tags.firstOrNull()
                                        ?: stringResource(R.string.contact_group_default),
                                ),
                            )
                        },
                    )
                )
            }
            item(key = "tags") {
                TonalCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.contact_tags),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        if (isProfileLoading || isProfileLoadFailed) {
                            Text(
                                text = if (isProfileLoading) loadingText else profileFailedText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else if (detail.tags.isEmpty()) {
                            Text(
                                text = notSet,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Row(
                                modifier = Modifier.horizontalScroll(tagChipScrollState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                detail.tags.forEach { tag ->
                                    SuggestionChip(
                                        onClick = showComingSoon,
                                        label = { Text(text = tag) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (isFriend && !isSelfProfile && !isGroupMemberProfile && !isProfileLoading && !isProfileLoadFailed) item(key = "danger") {
                TonalCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = {
                        if (AppDataSources.friendManageSource == null ||
                            deleteFriendChatId.isBlank() || deleteFriendUid.isBlank()
                        ) {
                            Toast.makeText(context, hostOnlyMessage, Toast.LENGTH_SHORT).show()
                        } else {
                            showDeleteFriendDialog = true
                        }
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.contact_delete),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
    if (showManageSheet && manageRole != null && manageTarget != null) {
        MemberActionsSheetMaterial(
            member = manageTarget,
            role = manageRole,
            chatId = manageChatId,
            noPermission = noPermissionMessage,
            successMessage = successMessage,
            failedMessage = failedMessage,
            onRefresh = onProfileRefresh,
            onDismiss = { showManageSheet = false },
            onKick = { kickTarget = it },
            onEditCard = { cardTarget = it },
        )
    }
    kickTarget?.let { member ->
        GroupMemberKickDialogMaterial(
            member = member,
            chatId = manageChatId,
            noPermissionMessage = noPermissionMessage,
            successMessage = successMessage,
            failedMessage = failedMessage,
            onDismiss = { kickTarget = null },
            onSuccess = onProfileRefresh,
        )
    }
    cardTarget?.let { member ->
        MemberInputDialogMaterial(
            title = stringResource(R.string.group_manage_edit_card),
            placeholder = stringResource(R.string.group_manage_card_hint),
            initial = member.name,
            confirmText = stringResource(R.string.confirm),
            onConfirm = { newCard ->
                cardTarget = null
                scope.performGroupManageAction(
                    context = context,
                    noPermissionMessage = noPermissionMessage,
                    successMessage = successMessage,
                    failedMessage = failedMessage,
                    onSuccess = onProfileRefresh,
                    call = {
                        AppDataSources.groupManageSource
                            ?.setGroupMemberCard(manageChatId, member.uid, newCard.trim()) ?: false
                    },
                )
            },
            onDismiss = { cardTarget = null },
        )
    }
    // 设置/修改好友备注（仅好友资料页显示入口）。
    ExpressiveInputDialog(
        show = showRemarkDialog,
        title = stringResource(
            if (detail.remark.isBlank()) R.string.contact_remark_set else R.string.contact_remark_edit,
        ),
        initialText = detail.remark,
        placeholder = stringResource(R.string.contact_remark_hint),
        confirmText = stringResource(R.string.confirm),
        onConfirm = { remark ->
            showRemarkDialog = false
            scope.launch {
                val ok = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        AppDataSources.friendManageSource
                            ?.setFriendRemark(remarkUid, remark.trim()) ?: false
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    if (ok) remarkSuccessMessage else remarkFailedMessage,
                    Toast.LENGTH_SHORT,
                ).show()
                if (ok) onProfileRefresh()
            }
        },
        onDismissRequest = { showRemarkDialog = false },
    )
    // 删除好友确认（好友资料页，危险色）。
    ExpressiveConfirmDialog(
        show = showDeleteFriendDialog,
        title = stringResource(R.string.contact_delete),
        message = stringResource(R.string.chat_info_delete_friend_confirm),
        confirmText = stringResource(R.string.contact_delete),
        danger = true,
        onConfirm = {
            showDeleteFriendDialog = false
            scope.launch {
                val ok = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        AppDataSources.friendManageSource
                            ?.deleteFriend(deleteFriendChatId, deleteFriendUid) ?: false
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    if (ok) deleteFriendSuccessMessage else deleteFriendFailedMessage,
                    Toast.LENGTH_SHORT,
                ).show()
                if (ok) {
                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching { LiveChats.refresh() }
                    }
                    onDeleteFriendSuccess()
                }
            }
        },
        onDismissRequest = { showDeleteFriendDialog = false },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContactDetailHeaderMaterial(
    detail: ContactDetail,
    dataLineKind: Int = 0,
    loading: Boolean = false,
    loadFailed: Boolean = false,
) {
    val contact = detail.contact
    val loadingText = stringResource(R.string.user_profile_loading)
    val failedText = stringResource(R.string.user_profile_load_failed)
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (loading) {
                // 头像尺寸占位：Expressive 加载动画，圆形裁剪，尺寸与头像一致（96dp）。
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    ContainedLoadingIndicator(modifier = Modifier.fillMaxSize())
                }
            } else {
                if (dataLineKind != 0) {
                    DataLineAvatar(kind = dataLineKind, size = 96.dp)
                } else {
                    ChatAvatar(
                        name = contact.name,
                        seed = contact.avatarSeed,
                        size = 96.dp,
                        isOnline = contact.isOnline,
                        avatarPath = contact.avatarPath,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = when {
                    loading -> loadingText
                    loadFailed && contact.name.isBlank() -> failedText
                    contact.name.isBlank() -> loadingText
                    else -> contact.name
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    dataLineKind != 0 -> stringResource(R.string.contact_dataline_session)
                    loading -> loadingText
                    loadFailed && detail.timId.isBlank() -> failedText
                    else -> stringResource(R.string.contact_tim_id, detail.timId.ifBlank { "—" })
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!loading && contact.isOnline) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.contact_online),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    dataLineKind != 0 -> DataLine.DESCRIPTION
                    loading -> loadingText
                    loadFailed && contact.signature.isBlank() -> failedText
                    else -> contact.signature.ifBlank { stringResource(R.string.contact_signature_empty) }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ContactDetailActionsMaterial(
    isFriend: Boolean,
    onSendMessage: () -> Unit,
    onCall: () -> Unit,
    onAddFriend: () -> Unit,
) {
    if (!isFriend) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onAddFriend,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.contact_add_friend),
                    maxLines = 1,
                )
            }
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onSendMessage,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.contact_send_message),
                maxLines = 1,
            )
        }
        FilledTonalButton(
            onClick = onCall,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.contact_audio_call),
                maxLines = 1,
            )
        }
        FilledTonalButton(
            onClick = onCall,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.contact_video_call),
                maxLines = 1,
            )
        }
    }
}

/** 群成员管理：移出群聊确认弹窗（复用 ChatMembers 的 groupManageSource 调用与 Toast 逻辑）。 */
@Composable
private fun GroupMemberKickDialogMaterial(
    member: ChatMember,
    chatId: String,
    noPermissionMessage: String,
    successMessage: String,
    failedMessage: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 踢人确认：Expressive 危险确认弹窗。
    ExpressiveConfirmDialog(
        show = true,
        title = stringResource(R.string.group_manage_kick),
        message = stringResource(R.string.group_manage_kick_confirm),
        confirmText = stringResource(R.string.group_manage_kick),
        danger = true,
        onConfirm = {
            onDismiss()
            scope.launch {
                val ok = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching {
                        AppDataSources.groupManageSource
                            ?.kickGroupMembers(chatId, listOf(member.uid)) ?: false
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    if (ok) successMessage else failedMessage,
                    Toast.LENGTH_SHORT,
                ).show()
                if (ok) onSuccess()
            }
        },
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun ContactInfoRowMaterial(
    icon: ImageVector,
    label: String,
    value: String,
) {
    SegmentedListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        },
        headlineContent = { Text(text = label) },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 190.dp),
            )
        },
    )
}
