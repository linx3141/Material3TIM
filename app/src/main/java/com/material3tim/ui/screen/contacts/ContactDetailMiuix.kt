package com.material3tim.ui.screen.contacts

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.ChatMember
import com.material3tim.data.model.ContactDetail
import com.material3tim.data.model.DataLine
import com.material3tim.data.repository.LiveChats
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.DataLineAvatar
import com.material3tim.ui.component.miuix.glass.GlassConfirmDialog
import com.material3tim.ui.component.miuix.glass.GlassInputDialog
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.HyperTopBarIconButton
import com.material3tim.ui.theme.LocalEnableBlur
import com.material3tim.ui.screen.chat.GroupMemberRole
import com.material3tim.ui.screen.chat.MemberActionsDialogMiuix
import com.material3tim.ui.screen.chat.canOperateMember
import com.material3tim.ui.screen.chat.performGroupManageAction
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 联系人详情页（Miuix 风格）。
 * Card + BasicComponent 资料行 + TextButton 操作，与 KernelSU 的 Miuix 页面形式一致。
 */
@Composable
internal fun ContactDetailMiuix(
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
    // 群名片输入内容：用于「非空才可确认」（由 GlassInputDialog.onTextChange 同步）。
    var cardEditText by remember { mutableStateOf("") }
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
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    // Phase 2d：页面级 Glass 采样 backdrop；null（关闭模糊/无 runtime shader）时玻璃组件纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
    Scaffold(
        topBar = {
            HyperTopAppBar(
                title = stringResource(R.string.contact_detail),
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                navigationIcon = {
                    // 圆形 Glass 按钮（玻璃模式）/ 普通 IconButton（纯色回退）。
                    HyperTopBarIconButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.back),
                        icon = MiuixIcons.Back,
                    )
                },
                actions = {
                    if (showManageEntry) {
                        HyperTopBarIconButton(
                            onClick = { showManageSheet = true },
                            contentDescription = stringResource(R.string.group_manage_member_actions),
                            icon = MiuixIcons.More,
                        )
                    }
                },
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 采样层要早于 inset/padding：记录不透明底 + 页面内容（不含玻璃顶栏自身）。
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .padding(horizontal = 12.dp),
                contentPadding = innerPadding,
                overscrollEffect = null,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    ContactDetailHeaderMiuix(
                        detail = detail,
                        dataLineKind = dataLineKind,
                        loading = isProfileLoading,
                        loadFailed = isProfileLoadFailed,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (isProfileLoadFailed) {
                    item(key = "profile_failed") {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            BasicComponent(
                                title = profileFailedText,
                                summary = "",
                                endActions = {
                                    TextButton(
                                        text = stringResource(R.string.user_profile_retry),
                                        onClick = onRetryProfile,
                                    )
                                },
                            )
                        }
                    }
                }
                if (!isSelfProfile && !isProfileLoading && !isProfileLoadFailed) item(key = "actions") {
                    ContactDetailActionsMiuix(
                        isFriend = isFriend,
                        onSendMessage = onSendMessage,
                        onCall = showComingSoon,
                        onAddFriend = onAddFriend,
                    )
                }
                if (showRemarkEntry && !isSelfProfile && !isProfileLoading && !isProfileLoadFailed) {
                    item(key = "remark_action") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showRemarkDialog = true },
                            showIndication = true,
                        ) {
                            BasicComponent(
                                title = stringResource(
                                    if (detail.remark.isBlank()) {
                                        R.string.contact_remark_set
                                    } else {
                                        R.string.contact_remark_edit
                                    },
                                ),
                                summary = "",
                            )
                        }
                    }
                }
                item(key = "info") {
                    ContactInfoCardMiuix(
                        detail = detail,
                        isProfileLoading = isProfileLoading,
                        isProfileLoadFailed = isProfileLoadFailed,
                    )
                }
                item(key = "tags") {
                    ContactTagsCardMiuix(
                        tags = detail.tags,
                        isProfileLoading = isProfileLoading,
                        isProfileLoadFailed = isProfileLoadFailed,
                        infoValue = infoValue,
                        onTagClick = showComingSoon,
                    )
                }
                if (isFriend && !isSelfProfile && !isGroupMemberProfile &&
                    !isProfileLoading && !isProfileLoadFailed
                ) {
                    item(key = "danger") {
                        ContactDangerRowMiuix(
                            onClick = {
                                if (AppDataSources.friendManageSource == null ||
                                    deleteFriendChatId.isBlank() || deleteFriendUid.isBlank()
                                ) {
                                    Toast.makeText(context, hostOnlyMessage, Toast.LENGTH_SHORT).show()
                                } else {
                                    showDeleteFriendDialog = true
                                }
                            },
                        )
                    }
                }
                item(key = "bottom_padding") {
                    Spacer(
                        Modifier.height(
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                                    24.dp
                        )
                    )
                }
            }
        }
    }
    if (showManageSheet && manageRole != null && manageTarget != null) {
        MemberActionsDialogMiuix(
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
    // 踢出群成员确认：HyperOS4 玻璃确认对话框（危险项；必须常驻组合，show 控制显隐）。
    GlassConfirmDialog(
        show = kickTarget != null,
        onDismissRequest = { kickTarget = null },
        title = stringResource(R.string.group_manage_kick),
        summary = stringResource(R.string.group_manage_kick_confirm),
        confirmText = stringResource(R.string.group_manage_kick),
        cancelText = stringResource(R.string.cancel),
        destructive = true,
        onConfirm = {
            val member = kickTarget
            kickTarget = null
            if (member != null) {
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            AppDataSources.groupManageSource
                                ?.kickGroupMembers(manageChatId, listOf(member.uid))
                                ?: false
                        }.getOrDefault(false)
                    }
                    Toast.makeText(
                        context,
                        if (ok) successMessage else failedMessage,
                        Toast.LENGTH_SHORT,
                    ).show()
                    if (ok) onProfileRefresh()
                }
            }
        },
    )
    // 修改群名片：玻璃输入对话框；打开时重置为成员昵称，空白不可确认（保留原校验）。
    GlassInputDialog(
        show = cardTarget != null,
        onDismissRequest = { cardTarget = null },
        title = stringResource(R.string.group_manage_edit_card),
        summary = stringResource(R.string.group_manage_card_hint),
        initialText = cardTarget?.name.orEmpty(),
        placeholder = null,
        singleLine = true,
        confirmText = stringResource(R.string.confirm),
        cancelText = stringResource(R.string.cancel),
        confirmEnabled = cardEditText.isNotBlank(),
        onTextChange = { cardEditText = it },
        onConfirm = { text ->
            val member = cardTarget
            cardTarget = null
            if (member != null) {
                scope.performGroupManageAction(
                    context = context,
                    noPermissionMessage = noPermissionMessage,
                    successMessage = successMessage,
                    failedMessage = failedMessage,
                    onSuccess = onProfileRefresh,
                    call = {
                        AppDataSources.groupManageSource
                            ?.setGroupMemberCard(
                                manageChatId,
                                member.uid,
                                text.trim(),
                            ) ?: false
                    },
                )
            }
        },
    )
    // 设置/修改备注：玻璃输入对话框（原实现为多行 TextField，保留多行）。
    GlassInputDialog(
        show = showRemarkDialog,
        onDismissRequest = { showRemarkDialog = false },
        title = stringResource(
            if (detail.remark.isBlank()) R.string.contact_remark_set else R.string.contact_remark_edit,
        ),
        summary = stringResource(R.string.contact_remark_hint),
        initialText = detail.remark,
        placeholder = null,
        singleLine = false,
        maxLines = Int.MAX_VALUE,
        confirmText = stringResource(R.string.confirm),
        cancelText = stringResource(R.string.cancel),
        onConfirm = { text ->
            showRemarkDialog = false
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.friendManageSource
                            ?.setFriendRemark(remarkUid, text.trim()) ?: false
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
    )
    // 删除好友确认：玻璃危险确认对话框。
    GlassConfirmDialog(
        show = showDeleteFriendDialog,
        onDismissRequest = { showDeleteFriendDialog = false },
        title = stringResource(R.string.contact_delete),
        summary = stringResource(R.string.chat_info_delete_friend_confirm),
        confirmText = stringResource(R.string.contact_delete),
        cancelText = stringResource(R.string.cancel),
        destructive = true,
        onConfirm = {
            showDeleteFriendDialog = false
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.friendManageSource
                            ?.deleteFriend(deleteFriendChatId, deleteFriendUid)
                            ?: false
                    }.getOrDefault(false)
                }
                Toast.makeText(
                    context,
                    if (ok) deleteFriendSuccessMessage else deleteFriendFailedMessage,
                    Toast.LENGTH_SHORT,
                ).show()
                if (ok) {
                    withContext(Dispatchers.IO) {
                        runCatching { LiveChats.refresh() }
                    }
                    onDeleteFriendSuccess()
                }
            }
        },
    )
    }
}

@Composable
private fun ContactDetailHeaderMiuix(
    detail: ContactDetail,
    dataLineKind: Int = 0,
    loading: Boolean = false,
    loadFailed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val contact = detail.contact
    val loadingText = stringResource(R.string.user_profile_loading)
    val failedText = stringResource(R.string.user_profile_load_failed)
    Card(
        modifier = modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 22.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (loading) {
                // 头像尺寸占位：Miuix InfiniteProgressIndicator，尺寸同头像（96dp）。
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    InfiniteProgressIndicator(modifier = Modifier.size(96.dp), size = 96.dp)
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
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    dataLineKind != 0 -> stringResource(R.string.contact_dataline_session)
                    loading -> loadingText
                    loadFailed && detail.timId.isBlank() -> failedText
                    else -> stringResource(R.string.contact_tim_id, detail.timId.ifBlank { "—" })
                },
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
            )
            if (!loading && contact.isOnline) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.contact_online),
                    fontSize = 12.sp,
                    color = colorScheme.primary,
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
                fontSize = 13.sp,
                color = colorScheme.onSurfaceVariantSummary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ContactDetailActionsMiuix(
    isFriend: Boolean,
    onSendMessage: () -> Unit,
    onCall: () -> Unit,
    onAddFriend: () -> Unit,
) {
    if (!isFriend) {
        TextButton(
            text = stringResource(R.string.contact_add_friend),
            onClick = onAddFriend,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            text = stringResource(R.string.contact_send_message),
            onClick = onSendMessage,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                text = stringResource(R.string.contact_audio_call),
                onClick = onCall,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                text = stringResource(R.string.contact_video_call),
                onClick = onCall,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ContactInfoCardMiuix(
    detail: ContactDetail,
    isProfileLoading: Boolean = false,
    isProfileLoadFailed: Boolean = false,
) {
    val notSet = stringResource(R.string.contact_not_set)
    val loadingText = stringResource(R.string.user_profile_loading)
    val failedText = stringResource(R.string.user_profile_load_failed)
    val infoValue: (String) -> String = { raw ->
        when {
            isProfileLoading -> loadingText
            isProfileLoadFailed -> failedText
            else -> raw.ifBlank { notSet }
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        BasicComponent(
            title = stringResource(R.string.contact_remark),
            summary = infoValue(detail.remark),
        )
        BasicComponent(
            title = stringResource(R.string.contact_phone),
            summary = infoValue(detail.phone),
        )
        BasicComponent(
            title = stringResource(R.string.contact_email),
            summary = infoValue(detail.email),
        )
        BasicComponent(
            title = stringResource(R.string.contact_region),
            summary = infoValue(detail.region),
        )
        BasicComponent(
            title = stringResource(R.string.contact_group),
            summary = infoValue(
                detail.tags.firstOrNull() ?: stringResource(R.string.contact_group_default),
            ),
        )
    }
}

@Composable
private fun ContactTagsCardMiuix(
    tags: List<String>,
    isProfileLoading: Boolean = false,
    isProfileLoadFailed: Boolean = false,
    infoValue: (String) -> String = { it },
    onTagClick: () -> Unit,
) {
    val tagScrollState = rememberScrollState()
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.contact_tags),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            if (isProfileLoading || isProfileLoadFailed) {
                Text(
                    text = infoValue(""),
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            } else if (tags.isEmpty()) {
                Text(
                    text = stringResource(R.string.contact_not_set),
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(tagScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        Text(
                            text = tag,
                            fontSize = 12.sp,
                            color = colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .clickable { onTagClick() }
                                .background(
                                    color = colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactDangerRowMiuix(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        showIndication = true,
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
                tint = colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.contact_delete),
                fontSize = 14.sp,
                color = colorScheme.error,
            )
        }
    }
}
