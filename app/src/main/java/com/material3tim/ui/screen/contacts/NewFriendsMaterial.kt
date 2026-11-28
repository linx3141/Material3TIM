package com.material3tim.ui.screen.contacts

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.FriendRequest
import com.material3tim.data.model.FriendRequestState
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SegmentedColumn
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.material.TopBarBackButton
import com.material3tim.ui.component.material.expressiveTopAppBarColors
import com.material3tim.ui.navigation3.LocalNavigator

/**
 * 新的朋友页（Material 3 Expressive 风格）：
 * 添加好友入口 + 两段申请列表（收到 / 发出）；仅收到的 pending 申请显示接受/拒绝。
 */
@Composable
fun NewFriendsMaterial(
    requests: List<FriendRequest>,
    loading: Boolean = false,
    loadFailed: Boolean = false,
    onRetry: () -> Unit = {},
    onAccept: (FriendRequest) -> Unit = {},
    onReject: (FriendRequest) -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val comingSoon = stringResource(R.string.feature_coming_soon)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val showComingSoon: () -> Unit = {
        Toast.makeText(context, comingSoon, Toast.LENGTH_SHORT).show()
    }
    val incomingRequests = requests.filter { it.isIncoming }
    val outgoingRequests = requests.filterNot { it.isIncoming }

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.new_friends_title)) },
                navigationIcon = {
                    TopBarBackButton(
                        onClick = { navigator.pop() },
                        contentDescription = stringResource(R.string.back),
                    )
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "add_friend") {
                NewFriendsAddEntryMaterial(onClick = showComingSoon)
            }
            when {
                loading && requests.isEmpty() -> item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 72.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                loadFailed && requests.isEmpty() -> item(key = "failed") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.new_friends_load_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.new_friends_retry))
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    // 上：收到的申请（对方加我）。空分组隐藏。
                    if (incomingRequests.isNotEmpty()) {
                        item(key = "incoming") {
                            SegmentedColumn(title = stringResource(R.string.new_friends_incoming)) {
                                incomingRequests.forEachIndexed { index, request ->
                                    item(key = "in_${index}_${request.id}") {
                                        NewFriendIncomingItemMaterial(
                                            request = request,
                                            onAccept = { onAccept(request) },
                                            onReject = { onReject(request) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // 下：我发出的申请（不能同意/拒绝，只显示状态）。
                    if (outgoingRequests.isNotEmpty()) {
                        item(key = "outgoing") {
                            SegmentedColumn(title = stringResource(R.string.new_friends_outgoing)) {
                                outgoingRequests.forEachIndexed { index, request ->
                                    item(key = "out_${index}_${request.id}") {
                                        NewFriendOutgoingItemMaterial(request = request)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewFriendsAddEntryMaterial(onClick: () -> Unit) {
    TonalCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.PersonAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.new_friends_add),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.new_friends_search),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 收到的申请：pending 显示接受/拒绝；已处理只显示状态。 */
@Composable
private fun NewFriendIncomingItemMaterial(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    SegmentedListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = request.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = request.timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
        supportingContent = {
            Text(
                text = request.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            ChatAvatar(
                name = request.name,
                seed = request.avatarSeed,
                avatarPath = request.avatarPath,
                size = 48.dp,
            )
        },
        trailingContent = {
            when (request.state) {
                FriendRequestState.PENDING -> Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onReject) {
                        Text(
                            text = stringResource(R.string.new_friends_reject),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        onClick = onAccept,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.new_friends_accept),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                FriendRequestState.ACCEPTED, FriendRequestState.REJECTED -> FriendRequestStateLabelMaterial(
                    text = stringResource(
                        if (request.state == FriendRequestState.ACCEPTED) {
                            R.string.new_friends_accepted
                        } else {
                            R.string.new_friends_rejected
                        },
                    ),
                )
            }
        },
    )
}

/** 我发出的申请：无接受/拒绝按钮，只显示状态。 */
@Composable
private fun NewFriendOutgoingItemMaterial(request: FriendRequest) {
    SegmentedListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = request.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = request.timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        },
        supportingContent = {
            Text(
                text = request.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            ChatAvatar(
                name = request.name,
                seed = request.avatarSeed,
                avatarPath = request.avatarPath,
                size = 48.dp,
            )
        },
        trailingContent = {
            FriendRequestStateLabelMaterial(
                text = stringResource(
                    when (request.state) {
                        FriendRequestState.PENDING -> R.string.new_friends_waiting
                        FriendRequestState.ACCEPTED -> R.string.new_friends_passed
                        FriendRequestState.REJECTED -> R.string.new_friends_declined
                    },
                ),
            )
        },
    )
}

@Composable
private fun FriendRequestStateLabelMaterial(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}
