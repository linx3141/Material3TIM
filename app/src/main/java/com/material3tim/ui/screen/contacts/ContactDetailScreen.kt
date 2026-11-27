package com.material3tim.ui.screen.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.ChatMember
import com.material3tim.data.model.ContactDetail
import com.material3tim.data.model.ContactItem
import com.material3tim.data.model.GroupMemberRole as ModelGroupMemberRole
import com.material3tim.data.model.UserProfile
import com.material3tim.data.repository.ChatRepository
import com.material3tim.data.repository.LiveProfile
import com.material3tim.data.source.AppDataSources
import com.material3tim.data.source.DataUpdate
import com.material3tim.data.source.LiveEvents
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.screen.chat.GroupMemberRole
import com.material3tim.ui.screen.chat.canOperateMember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 统一「用户资料」页入口（联系人与聊天头像共用）。
 *
 * - 联系人入口：`Route.ContactDetail(contactId = ...)`，数据走 [AppDataSources.contactSource]?.loadContactDetail
 *   （独立 App 用示例数据）。
 * - 聊天头像入口：额外带 `chatId/uid/uin`，数据走 `loadUserProfile(chatId, uid, uin)`，
 *   好友显示原联系人资料功能（发消息/通话/删除好友），非好友显示「加好友」。
 * - 群聊里当前账号是群主/管理员时，页面顶栏右上角出现三点 → 群成员管理 sheet
 *   （复用 ChatMembers 的 MemberActionsSheetMaterial / MemberActionsDialogMiuix 与权限 helper）。
 */
@Composable
fun ContactDetailScreen(
    contactId: String,
    chatId: String = "",
    uid: String = "",
    uin: String = "",
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val liveSource = AppDataSources.contactSource
    val isChatProfile = chatId.isNotBlank() && (uid.isNotBlank() || uin.isNotBlank())
    var profile by remember(contactId, chatId, uid, uin) { mutableStateOf<UserProfile?>(null) }
    var detail by remember(contactId, isChatProfile) {
        mutableStateOf(
            if (!isChatProfile && liveSource == null) ChatRepository.contactDetailFor(contactId) else null,
        )
    }
    var loading by remember(contactId, isChatProfile) { mutableStateOf(true) }
    var loadFailed by remember(contactId, isChatProfile) { mutableStateOf(false) }
    var refreshTick by remember(contactId, isChatProfile) { mutableStateOf(0) }
    // 群角色 fast 兜底：loadUserProfile 未返回/未带 myRole 时，用 loadChatInfoFast 的 members 先补三点入口。
    var fastRole by remember(chatId, uid, uin) { mutableStateOf<GroupMemberRole?>(null) }
    var fastTargetMember by remember(chatId, uid, uin) { mutableStateOf<ChatMember?>(null) }

    LaunchedEffect(contactId, chatId, uid, uin, refreshTick) {
        loading = true
        loadFailed = false
        if (isChatProfile) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    AppDataSources.contactSource?.loadUserProfile(chatId, uid, uin)
                }.getOrNull()
            }
            profile = loaded
            if (loaded == null) {
                loadFailed = true
                if (!AppDataSources.hasLiveDataSource) {
                    detail = ChatRepository.contactDetailFor(uid)
                }
            }
            if (AppDataSources.hasLiveDataSource) {
                // fast 群资料（毫秒级）：给用户资料页补群角色/目标成员（三点管理入口依赖它）。
                val fastInfo = withContext(Dispatchers.IO) {
                    runCatching { AppDataSources.chatSource?.loadChatInfoFast(chatId) }.getOrNull()
                }
                if (fastInfo != null) {
                    val members = fastInfo.members
                    val me = members.firstOrNull { it.isMe }
                    val target = members.firstOrNull { member ->
                        (uid.isNotBlank() && member.uid == uid) ||
                            (uin.isNotBlank() && member.uin == uin)
                    }
                    fastRole = me?.let {
                        GroupMemberRole(isOwner = it.isOwner, isAdmin = it.isAdmin)
                    }
                    fastTargetMember = target
                }
            }
        } else {
            val loaded = if (liveSource != null) {
                withContext(Dispatchers.IO) {
                    runCatching { liveSource.loadContactDetail(contactId) }.getOrNull()
                }
            } else {
                ChatRepository.contactDetailFor(contactId)
            }
            detail = loaded
            if (loaded == null) loadFailed = true
        }
        loading = false
    }

    // 后台资料/成员/用户资料刷新完成：直接重读 profile + fast 群角色局部更新，不整页 loading、不闪白。
    LaunchedEffect(chatId, isChatProfile, uid, uin) {
        if (!isChatProfile || !AppDataSources.hasLiveDataSource) return@LaunchedEffect
        LiveEvents.dataUpdates.collect { update ->
            if (update.chatId == chatId &&
                (update.kind == DataUpdate.Kind.USER_PROFILE ||
                    update.kind == DataUpdate.Kind.MEMBERS ||
                    update.kind == DataUpdate.Kind.CHAT_INFO)
            ) {
                val fresh = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.contactSource?.loadUserProfile(chatId, uid, uin)
                    }.getOrNull()
                }
                if (fresh != null) {
                    profile = fresh
                    loadFailed = false
                }
                val fastInfo = withContext(Dispatchers.IO) {
                    runCatching { AppDataSources.chatSource?.loadChatInfoFast(chatId) }.getOrNull()
                }
                if (fastInfo != null) {
                    val members = fastInfo.members
                    val me = members.firstOrNull { it.isMe }
                    val target = members.firstOrNull { member ->
                        (uid.isNotBlank() && member.uid == uid) ||
                            (uin.isNotBlank() && member.uin == uin)
                    }
                    fastRole = me?.let {
                        GroupMemberRole(isOwner = it.isOwner, isAdmin = it.isAdmin)
                    }
                    fastTargetMember = target
                }
            }
        }
    }

    // 聊天头像入口：先用骨架数据立即出页面 UI（头像/名称/资料行先占位加载中），不整页 loading。
    val skeletonDetail = remember(chatId, uid) {
        ContactDetail(
            contact = ContactItem(id = "", name = "", letter = '#'),
            timId = "",
            chatId = chatId,
        )
    }
    // chatId/uid/uin 入口映射成与联系人资料页一致的数据结构（页面 UI 完全共用）。
    val mappedProfile: ContactDetail? = profile?.let { loaded ->
        ContactDetail(
            contact = loaded.contact,
            timId = loaded.timId.ifBlank { loaded.contact.id },
            remark = "",
            phone = "",
            email = "",
            region = "",
            tags = emptyList(),
            chatId = loaded.chatId.ifBlank { chatId },
        )
    }
    val pageDetail: ContactDetail? = when {
        isChatProfile -> mappedProfile ?: detail ?: skeletonDetail
        else -> detail
    }

    if (pageDetail == null) {
        ContactDetailLoadState(
            loading = loading,
            failed = loadFailed,
            onRetry = { refreshTick += 1 },
        )
        return
    }

    // 点自己头像：senderUin 与当前登录账号 uin 相同（或群成员标记 isMe）→ 自己的资料页，
    // 不显示「加好友/发消息/删除好友」。
    val selfProfile = profile
    val targetMember: ChatMember? = profile?.targetMember ?: fastTargetMember
    val isSelf = targetMember?.isMe == true ||
        (uin.isNotBlank() && LiveProfile.peek()?.timId == uin)
    val isFriend = if (isSelf) true else profile?.isFriend ?: true
    // 群成员资料（群聊里点成员头像进入）不显示「设置/修改备注」；仅好友资料页显示。
    val isGroupMemberProfile = isChatProfile && profile?.groupCode?.isNotBlank() == true
    // 备注接口需要 uid：聊天入口直接用 uid；联系人入口 id 形如 tim_friend_<uid>。
    val remarkUid = uid.ifBlank {
        if (contactId.startsWith("tim_friend_")) contactId.removePrefix("tim_friend_") else ""
    }
    val showRemarkEntry = !isSelf && isFriend && !isGroupMemberProfile &&
        remarkUid.isNotBlank() && AppDataSources.friendManageSource != null
    // 删除好友：好友资料页可点（群成员/自己/独立 App 不显示真实入口）。
    val deleteFriendChatId = if (isChatProfile) {
        chatId
    } else {
        pageDetail.chatId.ifBlank { "" }
    }
    val onDeleteFriendSuccess: () -> Unit = {
        navigator.popUntil { route -> route is Route.Main }
    }
    val myRole: GroupMemberRole? = profile?.myRole?.toUiGroupMemberRole() ?: fastRole
    val groupChatId = profile?.chatId?.ifBlank { chatId } ?: chatId
    // 群聊成员资料 → 发消息应打开与该成员的私聊（tim_1_<uid>）；私聊/联系人场景直接用其 chatId。
    val chatProfile = profile
    val targetChatId = if (chatProfile != null && chatProfile.groupCode.isNotBlank() && uid.isNotBlank()) {
        "tim_1_$uid"
    } else {
        pageDetail.chatId.ifBlank { chatId }.ifBlank { pageDetail.contact.id }
    }
    val onSendMessage: () -> Unit = {
        if (targetChatId.isNotBlank()) navigator.push(Route.ChatDetail(targetChatId))
    }
    val onAddFriend: () -> Unit = {
        navigator.push(
            Route.AddFriendVerify(
                uid = uid,
                uin = uin.toLongOrNull() ?: 0L,
                name = pageDetail.contact.name,
            ),
        )
    }
    val onProfileRefresh: () -> Unit = { refreshTick += 1 }
    val onOpenQZone: () -> Unit = {
        // 优先用群成员/入口传入的 QQ 号；timId 在拿不到 uin 时会退化成 uid，
        // 拿它去开空间会变成错误的人（甚至空号）。只接受纯数字 QQ 号。
        val qzoneQq = sequenceOf(
            targetMember?.uin,
            uin,
            profile?.timId,
            pageDetail.timId,
        ).filterNotNull().firstOrNull { candidate ->
            // QQ 号必须为正整数；"0" / uid 字符串都不能拿去开空间。
            (candidate.toLongOrNull() ?: 0L) > 0L
        }.orEmpty()
        com.material3tim.ui.util.openQZone(context, qzoneQq)
    }
    // 仅「聊天头像入口且资料还没拿到」时显示局部 loading/失败态；拿到后（甚至刷新中）继续显示旧数据。
    val showProfilePlaceholders = isChatProfile && profile == null
    val profileLoading = showProfilePlaceholders && loading
    val profileLoadFailed = showProfilePlaceholders && loadFailed

    when (LocalUiMode.current) {
        UiMode.Material -> ContactDetailMaterial(
            detail = pageDetail,
            onBack = { navigator.pop() },
            onSendMessage = onSendMessage,
            isFriend = isFriend,
            isSelfProfile = isSelf,
            isProfileLoading = profileLoading,
            isProfileLoadFailed = profileLoadFailed,
            onRetryProfile = { refreshTick += 1 },
            onAddFriend = onAddFriend,
            onOpenQZone = onOpenQZone,
            manageRole = myRole,
            manageTarget = targetMember,
            manageChatId = groupChatId,
            onProfileRefresh = onProfileRefresh,
            showRemarkEntry = showRemarkEntry,
            remarkUid = remarkUid,
            isGroupMemberProfile = isGroupMemberProfile,
            deleteFriendChatId = deleteFriendChatId,
            deleteFriendUid = remarkUid,
            onDeleteFriendSuccess = onDeleteFriendSuccess,
        )

        UiMode.Miuix -> ContactDetailMiuix(
            detail = pageDetail,
            onBack = { navigator.pop() },
            onSendMessage = onSendMessage,
            isFriend = isFriend,
            isSelfProfile = isSelf,
            isProfileLoading = profileLoading,
            isProfileLoadFailed = profileLoadFailed,
            onRetryProfile = { refreshTick += 1 },
            onAddFriend = onAddFriend,
            onOpenQZone = onOpenQZone,
            manageRole = myRole,
            manageTarget = targetMember,
            manageChatId = groupChatId,
            onProfileRefresh = onProfileRefresh,
            showRemarkEntry = showRemarkEntry,
            remarkUid = remarkUid,
            isGroupMemberProfile = isGroupMemberProfile,
            deleteFriendChatId = deleteFriendChatId,
            deleteFriendUid = remarkUid,
            onDeleteFriendSuccess = onDeleteFriendSuccess,
        )
    }
}

/** 用户资料页加载中 / 加载失败（带重试）的兜底页。 */
@Composable
private fun ContactDetailLoadState(
    loading: Boolean,
    failed: Boolean,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading && !failed) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.loading),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = stringResource(R.string.group_files_load_failed),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onRetry) {
                    Text(text = stringResource(R.string.group_files_retry))
                }
            }
        }
    }
}

/** bridge 模型里的群成员角色 → 群管理 helper 使用的 UI 角色。 */
internal fun ModelGroupMemberRole?.toUiGroupMemberRole(): GroupMemberRole? = when (this) {
    ModelGroupMemberRole.OWNER -> GroupMemberRole(isOwner = true)
    ModelGroupMemberRole.ADMIN -> GroupMemberRole(isAdmin = true)
    ModelGroupMemberRole.MEMBER -> GroupMemberRole()
    null -> null
}

/** 群管理权限：仅群主/管理员可见三点入口，且目标成员必须可被当前账号操作。 */
internal fun canManageGroupMember(
    role: GroupMemberRole?,
    target: ChatMember?,
): Boolean = role != null && target != null && canOperateMember(role, target)
