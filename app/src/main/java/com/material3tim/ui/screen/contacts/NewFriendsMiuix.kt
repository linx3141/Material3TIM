package com.material3tim.ui.screen.contacts

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.FriendRequest
import com.material3tim.data.model.FriendRequestState
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.groupedCardItems
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopBarGlassButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 新的朋友页（Miuix 风格）：
 * 添加好友入口 + 两段申请 HyperOS4 分段卡片分组（收到 / 发出）；仅收到的 pending 申请显示接受/拒绝。
 */
@Composable
fun NewFriendsMiuix(
    requests: List<FriendRequest>,
    loading: Boolean = false,
    loadFailed: Boolean = false,
    onRetry: () -> Unit = {},
    onAccept: (FriendRequest) -> Unit = {},
    onReject: (FriendRequest) -> Unit = {},
    onAddFriend: () -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberPageBackdrop(enableBlur)
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }
    val incomingRequests = requests.filter { it.isIncoming }
    val outgoingRequests = requests.filterNot { it.isIncoming }

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        Scaffold(
            topBar = {
                HyperTopAppBar(
                    title = stringResource(R.string.new_friends_title),
                    isContentScrolled = listState.canScrollBackward,
                    backdrop = backdrop,
                    scrollBehavior = scrollBehavior,
                    navigationIcon = { HyperBackIcon(onClick = { navigator.pop() }) },
                )
            },
            popupHost = { },
            contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal),
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .scrollEndHaptic()
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    // 采样层要早于 inset/padding：记录不透明底 + 列表内容（不含玻璃顶栏自身）。
                    .then(
                        if (backdrop != null) {
                            Modifier
                                .layerBackdrop(backdrop)
                                .background(colorScheme.surface)
                        } else {
                            Modifier
                        },
                    ),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                // HyperOS4 分组卡片：添加好友入口 + 收到/发出申请各成一组 squircle 分段卡片。
                groupedCardItems(
                    keyPrefix = "addFriend",
                    outerTopPadding = 12.dp,
                    outerBottomPadding = 6.dp,
                    items = listOf(
                        CardItem("add") {
                            BasicComponent(
                                title = stringResource(R.string.new_friends_add),
                                summary = stringResource(R.string.new_friends_search),
                                startAction = {
                                    Icon(
                                        imageVector = Icons.Filled.PersonAdd,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.padding(end = 6.dp),
                                    )
                                },
                                onClick = onAddFriend,
                            )
                        },
                    ),
                )
                when {
                    loading && requests.isEmpty() -> item(key = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 72.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            InfiniteProgressIndicator()
                        }
                    }
                    loadFailed && requests.isEmpty() -> item(key = "failed") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 56.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.new_friends_load_failed),
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                                Spacer(Modifier.height(6.dp))
                                TextButton(
                                    text = stringResource(R.string.new_friends_retry),
                                    onClick = onRetry,
                                    colors = ButtonDefaults.textButtonColorsPrimary(),
                                )
                            }
                        }
                    }
                    requests.isEmpty() -> item(key = "empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 72.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.new_friends_empty),
                                fontSize = 14.sp,
                                color = colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    else -> {
                        // 上：收到的申请（对方加我）。空分组隐藏。
                        if (incomingRequests.isNotEmpty()) {
                            item(key = "incoming_title") {
                                NewFriendsGroupTitleMiuix(stringResource(R.string.new_friends_incoming))
                            }
                            groupedCardItems(
                                keyPrefix = "incoming",
                                outerTopPadding = 0.dp,
                                outerBottomPadding = 6.dp,
                                items = incomingRequests.map { request ->
                                    CardItem(request.id) {
                                        NewFriendIncomingRowMiuix(
                                            request = request,
                                            onAccept = { onAccept(request) },
                                            onReject = { onReject(request) },
                                        )
                                    }
                                },
                            )
                        }
                        // 下：我发出的申请（不能同意/拒绝，只显示状态）。
                        if (outgoingRequests.isNotEmpty()) {
                            item(key = "outgoing_title") {
                                NewFriendsGroupTitleMiuix(stringResource(R.string.new_friends_outgoing))
                            }
                            groupedCardItems(
                                keyPrefix = "outgoing",
                                outerTopPadding = 0.dp,
                                outerBottomPadding = 6.dp,
                                items = outgoingRequests.map { request ->
                                    CardItem(request.id) {
                                        NewFriendOutgoingRowMiuix(request = request)
                                    }
                                },
                            )
                        }
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
}

/** 顶栏返回按钮：跟随顶栏风格（HyperTopBarGlassButton → Glass 圆钮 / Original 普通 IconButton）。 */
@Composable
private fun HyperBackIcon(onClick: () -> Unit) {
    HyperTopBarGlassButton(onClick = onClick) {
        Icon(
            imageVector = MiuixIcons.Back,
            contentDescription = stringResource(R.string.back),
            tint = colorScheme.onSurface,
        )
    }
}

@Composable
private fun NewFriendsGroupTitleMiuix(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = colorScheme.primary,
        modifier = Modifier.padding(start = 18.dp, top = 4.dp, bottom = 6.dp),
    )
}

/** 收到的申请：pending 显示接受/拒绝；已处理只显示状态。 */
@Composable
private fun NewFriendIncomingRowMiuix(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAvatar(
            name = request.name,
            seed = request.avatarSeed,
            avatarPath = request.avatarPath,
            size = 48.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = request.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = request.timeLabel,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = request.message,
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        when (request.state) {
            FriendRequestState.PENDING -> Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    text = stringResource(R.string.new_friends_reject),
                    onClick = onReject,
                )
                Spacer(Modifier.width(4.dp))
                TextButton(
                    text = stringResource(R.string.new_friends_accept),
                    onClick = onAccept,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
            FriendRequestState.ACCEPTED, FriendRequestState.REJECTED -> Text(
                text = stringResource(
                    if (request.state == FriendRequestState.ACCEPTED) {
                        R.string.new_friends_accepted
                    } else {
                        R.string.new_friends_rejected
                    },
                ),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** 我发出的申请：无接受/拒绝按钮，只显示状态。 */
@Composable
private fun NewFriendOutgoingRowMiuix(request: FriendRequest) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatAvatar(
            name = request.name,
            seed = request.avatarSeed,
            avatarPath = request.avatarPath,
            size = 48.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = request.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = request.timeLabel,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = request.message,
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(
                when (request.state) {
                    FriendRequestState.PENDING -> R.string.new_friends_waiting
                    FriendRequestState.ACCEPTED -> R.string.new_friends_passed
                    FriendRequestState.REJECTED -> R.string.new_friends_declined
                },
            ),
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariantSummary,
        )
    }
}
