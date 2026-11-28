package com.material3tim.ui.screen.chat

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.ChatInfoData
import com.material3tim.data.model.ChatMember
import com.material3tim.data.repository.LiveChats
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.DataLineAvatar
import com.material3tim.ui.component.miuix.glass.GlassConfirmDialog
import com.material3tim.ui.component.miuix.glass.GlassInputDialog
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarIconButton
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.theme.LocalEnableBlur
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Pin
import top.yukonga.miuix.kmp.icon.extended.RemoveContact
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.VolumeOff
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 聊天信息页（Miuix 风格）：
 * Card 分组（头部资料 / 群成员 / 群公告 / 置顶与免打扰开关 / 操作行）。
 * 置顶 / 消息免打扰走宿主 ChatSettingSource（独立 App 提示仅宿主可用）；
 * 其余操作行统一弹「后续版本实现」。
 */
@Composable
internal fun ChatInfoMiuix(
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
    val enableBlur = LocalEnableBlur.current
    val scrollBehavior = MiuixScrollBehavior()
    // Phase 2b：页面级 Glass 采样 backdrop（关闭模糊/无 runtime shader 时为 null → 纯色回退）。
    val backdrop = rememberPageBackdrop(enableBlur)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val comingSoon = remember(context) {
        {
            Toast.makeText(context, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show()
        }
    }
    val hostOnlyMessage = stringResource(R.string.chat_info_host_only)
    val settingFailedMessage = stringResource(R.string.chat_info_setting_failed)
    val clearSuccessMessage = stringResource(R.string.chat_info_clear_success)
    val clearFailedMessage = stringResource(R.string.chat_info_clear_failed)
    val deleteFriendSuccessMessage = stringResource(R.string.chat_info_delete_friend_success)
    val deleteFriendFailedMessage = stringResource(R.string.chat_info_delete_friend_failed)
    val avatarEditorFailedMessage = stringResource(R.string.chat_info_avatar_editor_failed)
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
    val layoutDirection = LocalLayoutDirection.current
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navigator = LocalNavigator.current
    val role = remember(data.members) { currentMemberRole(data.members) }
    var showNameDialog by remember(chat.id) { mutableStateOf(false) }
    var showQuitDialog by remember(chat.id) { mutableStateOf(false) }
    var showClearHistoryDialog by remember(chat.id) { mutableStateOf(false) }
    var showDeleteFriendDialog by remember(chat.id) { mutableStateOf(false) }
    var showDescriptionDialog by remember(chat.id) { mutableStateOf(false) }
    // 群名输入内容：用于「非空才可确认」（由 GlassInputDialog.onTextChange 同步）。
    var nameEditText by remember { mutableStateOf("") }
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
    // 清空聊天记录：确认 → clearChatHistory → Toast + 刷新会话 → 回消息列表。
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
    // 删除好友（单聊）。
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
    // 打开 TIM 原生群头像/群资料编辑页。
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

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
    Scaffold(
        topBar = {
            HyperTopAppBar(
                title = stringResource(R.string.chat_info),
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                navigationIcon = {
                    // 圆形 Glass 按钮（玻璃模式）/ 普通 IconButton（纯色回退）。
                    HyperTopBarIconButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.chat_detail_back),
                        icon = MiuixIcons.Back,
                    )
                },
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    .fillMaxSize()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
                    end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = navigationBarPadding + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                overscrollEffect = null,
            ) {
                item(key = "header") {
                    ChatInfoHeaderMiuix(
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
                    ChatInfoProfileMiuix(
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
                            ChatInfoMembersMiuix(
                                members = data.members,
                                memberCount = memberCount,
                                onViewAll = onViewAllMembers,
                            )
                        }
                        loading -> item(key = "members_loading") {
                            ChatInfoMembersLoadingMiuix()
                        }
                        loadFailed -> item(key = "members_failed") {
                            ChatInfoMembersLoadFailedMiuix(onRetry = onRetry)
                        }
                    }
                }
                // 群公告卡片对群聊始终显示（无公告时也能进入公告页发布）
                if (data.chat.isGroup) {
                    item(key = "notice") {
                        ChatInfoNoticeMiuix(notice = data.notice, onClick = onOpenNotice)
                    }
                }
                if (chat.isGroup) {
                    item(key = "group_files") {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            ArrowPreference(
                                title = stringResource(R.string.group_files_title),
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.Folder,
                                        contentDescription = null,
                                        tint = colorScheme.onBackground,
                                        modifier = Modifier.padding(end = 6.dp),
                                    )
                                },
                                onClick = { navigator.push(Route.ChatFiles(chat.id)) },
                            )
                        }
                    }
                }
                item(key = "switches") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchPreference(
                            title = stringResource(R.string.chat_info_pin),
                            startAction = {
                                Icon(
                                    imageVector = MiuixIcons.Pin,
                                    contentDescription = null,
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            checked = pinned,
                            onCheckedChange = onPinnedChange,
                        )
                        SwitchPreference(
                            title = stringResource(R.string.chat_info_mute),
                            startAction = {
                                Icon(
                                    imageVector = MiuixIcons.VolumeOff,
                                    contentDescription = null,
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            checked = muted,
                            onCheckedChange = onMutedChange,
                        )
                        if (chat.isGroup && role.canManage) {
                            SwitchPreference(
                                title = stringResource(R.string.group_manage_all_mute),
                                startAction = {
                                    Icon(
                                        imageVector = MiuixIcons.VolumeOff,
                                        contentDescription = null,
                                        tint = colorScheme.onBackground,
                                        modifier = Modifier.padding(end = 6.dp),
                                    )
                                },
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
                item(key = "actions") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ArrowPreference(
                            title = stringResource(R.string.chat_info_search_history),
                            startAction = {
                                Icon(
                                    imageVector = MiuixIcons.Basic.Search,
                                    contentDescription = null,
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            onClick = onSearchHistory,
                        )
                        ArrowPreference(
                            title = stringResource(R.string.chat_info_clear),
                            startAction = {
                                Icon(
                                    imageVector = MiuixIcons.Delete,
                                    contentDescription = null,
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            onClick = { showClearHistoryDialog = true },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.chat_info_report),
                            startAction = {
                                Icon(
                                    imageVector = MiuixIcons.Report,
                                    contentDescription = null,
                                    tint = colorScheme.onBackground,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            onClick = comingSoon,
                        )
                        ArrowPreference(
                            title = stringResource(
                                when {
                                    !chat.isGroup -> R.string.chat_info_delete_friend
                                    isOwner -> R.string.group_manage_destroy
                                    else -> R.string.chat_info_quit_group
                                },
                            ),
                            startAction = {
                                Icon(
                                    imageVector = MiuixIcons.RemoveContact,
                                    contentDescription = null,
                                    tint = colorScheme.error,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            },
                            onClick = {
                                if (chat.isGroup) showQuitDialog = true else showDeleteFriendDialog = true
                            },
                        )
                    }
                }
            }
        }
    }
    }
    // 页面级 backdrop 的 provider 在 Scaffold 处已结束，这里再包一层供玻璃对话框采样
    //（页面未提供 backdrop 时 Glass 组件自动不透明回退）。
    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
    // 修改群名：HyperOS4 玻璃输入对话框（常驻组合，show 控制显隐；空白不可确认）。
    GlassInputDialog(
        show = showNameDialog,
        onDismissRequest = { showNameDialog = false },
        title = stringResource(R.string.group_manage_group_name),
        summary = stringResource(R.string.group_manage_group_name_hint),
        initialText = chat.name,
        singleLine = true,
        confirmText = stringResource(R.string.confirm),
        cancelText = stringResource(R.string.cancel),
        confirmEnabled = nameEditText.isNotBlank(),
        onTextChange = { nameEditText = it },
        onConfirm = { text ->
            showNameDialog = false
            performManageAction(
                { AppDataSources.groupManageSource?.setGroupName(chatId, text.trim()) ?: false },
                { onRefresh() },
            )
        },
    )
    // 退群/解散群：玻璃危险确认对话框。
    GlassConfirmDialog(
        show = showQuitDialog,
        onDismissRequest = { showQuitDialog = false },
        title = stringResource(
            if (isOwner) R.string.group_manage_destroy else R.string.chat_info_quit_group,
        ),
        summary = stringResource(
            if (isOwner) R.string.group_manage_destroy_confirm else R.string.group_manage_quit_confirm,
        ),
        confirmText = stringResource(
            if (isOwner) R.string.group_manage_destroy else R.string.chat_info_quit_group,
        ),
        cancelText = stringResource(R.string.cancel),
        destructive = true,
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
    )
    // 清空聊天记录：玻璃危险确认对话框。
    GlassConfirmDialog(
        show = showClearHistoryDialog,
        onDismissRequest = { showClearHistoryDialog = false },
        title = stringResource(R.string.chat_info_clear),
        summary = stringResource(R.string.chat_info_clear_confirm),
        confirmText = stringResource(R.string.chat_info_clear),
        cancelText = stringResource(R.string.cancel),
        destructive = true,
        onConfirm = {
            showClearHistoryDialog = false
            onClearHistory()
        },
    )
    // 删除好友（单聊）：玻璃危险确认对话框。
    GlassConfirmDialog(
        show = showDeleteFriendDialog,
        onDismissRequest = { showDeleteFriendDialog = false },
        title = stringResource(R.string.chat_info_delete_friend),
        summary = stringResource(R.string.chat_info_delete_friend_confirm),
        confirmText = stringResource(R.string.chat_info_delete_friend),
        cancelText = stringResource(R.string.cancel),
        destructive = true,
        onConfirm = {
            showDeleteFriendDialog = false
            onDeleteFriend()
        },
    )
    // 修改群简介：玻璃输入对话框（多行）。
    GlassInputDialog(
        show = showDescriptionDialog,
        onDismissRequest = { showDescriptionDialog = false },
        title = stringResource(R.string.chat_info_edit_group_desc),
        summary = stringResource(R.string.chat_info_group_desc_hint),
        initialText = data.description,
        singleLine = false,
        maxLines = Int.MAX_VALUE,
        confirmText = stringResource(R.string.confirm),
        cancelText = stringResource(R.string.cancel),
        onConfirm = { text ->
            showDescriptionDialog = false
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        AppDataSources.friendManageSource
                            ?.setGroupDescription(chatId, text.trim()) ?: false
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
    )
    }
}

@Composable
private fun ChatInfoHeaderMiuix(
    data: ChatInfoData,
    memberCount: Int,
    onNameClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null,
) {
    val chat = data.chat
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
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
                    fontSize = 19.sp,
                    fontWeight = FontWeight(600),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
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
                    fontSize = 13.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChatInfoMembersMiuix(
    members: List<ChatMember>,
    memberCount: Int,
    onViewAll: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.chat_info_member_count, memberCount),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                members.take(12).chunked(4).forEach { rowMembers ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        rowMembers.forEach { member ->
                            MemberItemMiuix(member = member, modifier = Modifier.weight(1f))
                        }
                        repeat(4 - rowMembers.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            ArrowPreference(
                title = stringResource(R.string.chat_info_view_members, memberCount),
                onClick = onViewAll,
            )
        }
    }
}

@Composable
private fun MemberItemMiuix(member: ChatMember, modifier: Modifier) {
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
            fontSize = 11.sp,
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (member.isOwner) {
            Text(
                text = stringResource(R.string.chat_info_owner),
                fontSize = 10.sp,
                color = colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ChatInfoNoticeMiuix(notice: String, onClick: () -> Unit) {
    // 整张卡（含「暂无群公告」空态）都显式绑定点击：进入 Route.ChatNotice(chatId)。
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        insideMargin = PaddingValues(0.dp),
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = MiuixIcons.ChevronForward,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariantSummary,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = notice.ifBlank { stringResource(R.string.chat_info_notice_empty) },
                fontSize = 14.sp,
                color = colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ChatInfoProfileMiuix(
    data: ChatInfoData,
    onDescriptionClick: (() -> Unit)? = null,
) {
    val chat = data.chat
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val timId = data.timId.ifBlank { "—" }
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
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
                    fontSize = 15.sp,
                    color = colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = data.description.ifBlank { stringResource(R.string.chat_info_no_desc) },
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariantSummary,
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

/** 成员区局部 loading（Miuix InfiniteProgressIndicator），高度与成员卡片接近。 */
@Composable
private fun ChatInfoMembersLoadingMiuix() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            contentAlignment = Alignment.Center,
        ) {
            InfiniteProgressIndicator()
        }
    }
}

/** 成员区加载失败 + 重试（页面其它部分保持可见）。 */
@Composable
private fun ChatInfoMembersLoadFailedMiuix(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.group_files_load_failed),
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.height(6.dp))
            TextButton(
                text = stringResource(R.string.group_files_retry),
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    }
}
