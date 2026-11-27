package com.material3tim.ui.screen.contacts

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.material3tim.R
import com.material3tim.data.model.FriendRequest
import com.material3tim.data.model.FriendRequestState
import com.material3tim.data.repository.ContentRepository
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.UiMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 新的朋友页入口：按 [LocalUiMode] 分发到 Material / Miuix 两套实现。
 *
 * 宿主（TIM 进程）里从 [AppDataSources.friendManageSource]?.loadFriendRequests 拉真实好友申请，
 * 接受/拒绝走 approveFriendRequest（成功后重新拉列表）；独立 App 保持示例数据。
 */
@Composable
fun NewFriendsScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val friendManageSource = AppDataSources.friendManageSource
    val isHost = friendManageSource != null
    val scope = rememberCoroutineScope()
    val failedMessage = stringResource(R.string.group_manage_failed)
    var requests by remember {
        mutableStateOf(if (isHost) emptyList() else ContentRepository.friendRequests)
    }
    var loading by remember { mutableStateOf(isHost) }
    var loadFailed by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var actionInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTick) {
        if (!isHost) {
            requests = ContentRepository.friendRequests
            loading = false
            loadFailed = false
            return@LaunchedEffect
        }
        loading = requests.isEmpty()
        loadFailed = false
        val loaded = withContext(Dispatchers.IO) {
            runCatching { friendManageSource?.loadFriendRequests() }.getOrNull()
        }
        if (loaded != null) {
            requests = loaded
            loadFailed = false
        } else if (requests.isEmpty()) {
            loadFailed = true
        }
        loading = false
    }

    val onRespond: (FriendRequest, Boolean) -> Unit = { request, accept ->
        if (!isHost) {
            // 独立 App：仅本地更新状态，不发网络请求。
            requests = requests.map { item ->
                if (item.id == request.id) {
                    item.copy(
                        state = if (accept) FriendRequestState.ACCEPTED else FriendRequestState.REJECTED,
                    )
                } else {
                    item
                }
            }
        } else if (!actionInProgress) {
            actionInProgress = true
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        friendManageSource?.approveFriendRequest(
                            uid = request.uid,
                            uin = request.uin,
                            accept = accept,
                        ) ?: false
                    }.getOrDefault(false)
                }
                actionInProgress = false
                if (ok) {
                    refreshTick += 1
                } else {
                    Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    when (LocalUiMode.current) {
        UiMode.Material -> NewFriendsMaterial(
            requests = requests,
            loading = loading,
            loadFailed = loadFailed,
            onRetry = { refreshTick += 1 },
            onAccept = { onRespond(it, true) },
            onReject = { onRespond(it, false) },
            onAddFriend = { navigator.push(Route.AddFriend) },
        )

        UiMode.Miuix -> NewFriendsMiuix(
            requests = requests,
            loading = loading,
            loadFailed = loadFailed,
            onRetry = { refreshTick += 1 },
            onAccept = { onRespond(it, true) },
            onReject = { onRespond(it, false) },
            onAddFriend = { navigator.push(Route.AddFriend) },
        )
    }
}
