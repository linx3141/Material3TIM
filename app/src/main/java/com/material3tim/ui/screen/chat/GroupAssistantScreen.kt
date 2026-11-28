package com.material3tim.ui.screen.chat

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.material3tim.R
import com.material3tim.data.model.ChatItem
import com.material3tim.data.model.GroupAssistantData
import com.material3tim.data.source.AppDataSources
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 消息列表里「群助手」伪会话的固定 id（与 bridge 映射一致）。 */
internal const val GroupAssistantChatId = "tim_troop_assistant"

/**
 * 群助手二级页入口（消息列表群助手卡片点击进入）：
 * 数据来自 [AppDataSources.chatSource]?.loadGroupAssistant（阻塞 IO），按 [LocalUiMode] 分发两套实现。
 */
@Composable
fun GroupAssistantScreen(
    forwardSrcChatId: String? = null,
    forwardMsgIds: List<Long> = emptyList(),
    forwardPreview: String = "",
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val forwardMode = forwardSrcChatId != null
    var sending by remember { mutableStateOf(false) }
    var data by remember { mutableStateOf<GroupAssistantData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var refreshTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTick) {
        loading = true
        failed = false
        val source = AppDataSources.chatSource
        if (source == null) {
            // 独立 App / 宿主未就绪：无群助手数据 → 空态。
            data = null
            loading = false
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            runCatching { source.loadGroupAssistant() }.getOrNull()
        }
        data = loaded
        failed = loaded == null
        loading = false
    }

    val groups = data?.groups.orEmpty()
    val onBack: () -> Unit = { navigator.pop() }
    val onOpenGroup: (ChatItem) -> Unit = if (forwardMode) {
        { group ->
            if (!sending) {
                val messageSource = AppDataSources.messageSource
                if (messageSource == null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.forward_unavailable),
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    sending = true
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching {
                                messageSource.forwardMessages(
                                    forwardSrcChatId!!,
                                    forwardMsgIds,
                                    listOf(group.id),
                                )
                            }.getOrDefault(false)
                        }
                        Toast.makeText(
                            context,
                            if (ok) {
                                context.getString(R.string.forward_success_format, 1)
                            } else {
                                context.getString(R.string.forward_failed)
                            },
                            Toast.LENGTH_SHORT,
                        ).show()
                        sending = false
                        if (ok) {
                            // 关掉转发页/群助手选择页，回到来源聊天页。
                            navigator.popUntil { it is Route.ChatDetail }
                        }
                    }
                }
            }
        }
    } else {
        { group -> navigator.push(Route.ChatDetail(group.id)) }
    }
    val onRetry: () -> Unit = { refreshTick += 1 }

    when (LocalUiMode.current) {
        UiMode.Material -> GroupAssistantMaterial(
            groups = groups,
            loading = loading,
            failed = failed,
            onBack = onBack,
            onRetry = onRetry,
            onOpenGroup = onOpenGroup,
            title = if (forwardMode) androidx.compose.ui.res.stringResource(R.string.forward_group_assistant_title) else null,
        )

        UiMode.Miuix -> GroupAssistantMiuix(
            groups = groups,
            loading = loading,
            failed = failed,
            onBack = onBack,
            onRetry = onRetry,
            onOpenGroup = onOpenGroup,
            title = if (forwardMode) androidx.compose.ui.res.stringResource(R.string.forward_group_assistant_title) else null,
        )
    }
}
