package com.material3tim.ui.screen.chat

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import com.material3tim.data.model.ChatPartKind
import com.material3tim.data.source.AppDataSources
import androidx.compose.material.icons.filled.AddReaction
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.model.ChatMessageType
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.component.material.OffsetAnchoredExpressiveMenu
import com.material3tim.ui.component.miuix.glass.GlassMenuEntry
import com.material3tim.ui.component.miuix.glass.LocalGlassMenu
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.glass.GlassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.dp

/** 长按菜单动作。 */
internal enum class MessageMenuAction(val label: String, val icon: ImageVector) {
    PLUS_ONE("+1", Icons.Filled.AddReaction),
    COPY("复制", Icons.Filled.ContentCopy),
    SAVE_TO_COLLECTION("收藏", Icons.Filled.BookmarkAdd),
    FORWARD("转发", Icons.Filled.Share),
    MULTI_SELECT("多选", Icons.Filled.Checklist),
    REPLY("引用", Icons.AutoMirrored.Filled.Reply),
    AT_SENDER("at对方", Icons.Filled.AlternateEmail),
    ADD_TO_FAVORITE("添加到表情", Icons.Filled.AddReaction),
    RECALL("撤回", Icons.AutoMirrored.Filled.Undo),
    DELETE("删除", Icons.Filled.Delete),
}

internal fun openMessageCollection(context: android.content.Context, message: ChatMessage) {
    if (AppDataSources.personalLibrary?.collect(message) != true) {
        android.widget.Toast.makeText(context, context.getString(com.material3tim.R.string.profile_collect_failed),
            android.widget.Toast.LENGTH_SHORT).show()
    }
}

/** TIM 原生撤回时限：自己发送的消息 2 分钟内可撤回。 */
private const val RECALL_TIME_WINDOW_MS = 120_000L

/**
 * 按消息状态过滤可见的长按菜单动作（顺序与枚举一致）：
 * - 默认全部可见；
 * - 「撤回」仅当消息有真实 msgId（非本地乐观消息）且在 2 分钟内可见，且满足其一：
 *   * 自己发的消息（TIM 原生时限内）；
 *   * 群主/管理员撤回他人消息（[canRecallOthers]，由页面按群权限查询后传入）。
 */
internal fun visibleMessageMenuActions(
    message: ChatMessage,
    canRecallOthers: Boolean = false,
    canAtSender: Boolean = false,
    canAddToFavorite: Boolean = false,
): List<MessageMenuAction> {
    val withinRecallWindow = message.chatRecordMsgId != 0L &&
        message.timestampMs > 0L &&
        System.currentTimeMillis() - message.timestampMs <= RECALL_TIME_WINDOW_MS
    val recallVisible = withinRecallWindow && (message.isMe || canRecallOthers)
    // 「at对方」仅群聊、非自己消息、发送者 uid 可用时可见。
    val atSenderVisible = canAtSender && !message.isMe && message.senderUid.isNotBlank()
    // 「添加到表情」仅图片/表情类消息、且宿主有 emojiSource 时可见（自己/他人均可）。
    val addToFavoriteVisible = canAddToFavorite &&
        (message.type == ChatMessageType.IMAGE || message.isEmojiImage)
    return MessageMenuAction.entries.filter { action ->
        when (action) {
            MessageMenuAction.RECALL -> recallVisible
            MessageMenuAction.SAVE_TO_COLLECTION -> AppDataSources.personalLibrary != null && when (message.type) {
                ChatMessageType.TEXT -> message.content.isNotBlank() && message.parts.none { it.kind == ChatPartKind.IMAGE }
                ChatMessageType.IMAGE -> !message.imagePath.isNullOrBlank()
                ChatMessageType.VIDEO -> !message.videoPath.isNullOrBlank()
                ChatMessageType.FILE, ChatMessageType.VOICE -> !message.mediaPath.isNullOrBlank()
                else -> false
            }
            MessageMenuAction.AT_SENDER -> atSenderVisible
            MessageMenuAction.ADD_TO_FAVORITE -> addToFavoriteVisible
            else -> true
        }
    }
}

/** 页面级提供的菜单动作处理（复制/引用/删除/转发/+1 由页面实现）。 */
internal val LocalMessageMenuAction =
    compositionLocalOf<((MessageMenuAction, ChatMessage) -> Unit)?> { null }

/**
 * 气泡容器：长按气泡才呼出菜单，带触感振动。
 * - Miuix：菜单交给根级 GlassMenuHost（miuix-glass GlassPopup），锚点 = 长按位置窗口坐标
 * - Material：保持原来“哪里长按就在哪里弹出”的 Expressive 下拉
 */
@Composable
internal fun BubbleActionMenuHost(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    canRecallOthers: Boolean = false,
    canAtSender: Boolean = false,
    canAddToFavorite: Boolean = false,
    content: @Composable () -> Unit,
) {
    val onMenuAction = LocalMessageMenuAction.current
    if (onMenuAction == null) {
        content()
        return
    }
    var show by remember(message.id) { mutableStateOf(false) }
    var pressOffset by remember(message.id) { mutableStateOf(IntOffset.Zero) }
    // 长按点（相对气泡左上角）：1dp 透明锚点 Box 放在这里，作为 morph 展开/收缩的中心。
    var pressPosition by remember(message.id) { mutableStateOf(Offset.Zero) }
    // 1x1 morph 锚点：GlassTransformPopup 从手指长按点展开，预测性返回跟手收缩回该点。
    val menuAnchor = rememberGlassPopupAnchor()
    val haptic = LocalHapticFeedback.current
    // 按消息状态过滤可见动作（如「撤回」只在这条消息可撤回时出现）；show 变化时重算，
    // 保证每次打开菜单都用当前时间判断 2 分钟撤回时限。
    val menuActions = remember(message, show, canRecallOthers, canAtSender, canAddToFavorite) {
        visibleMessageMenuActions(message, canRecallOthers, canAtSender, canAddToFavorite)
    }
    val viewConfiguration = LocalViewConfiguration.current
    Box(
        modifier = modifier
            .pointerInput(message.id) {
                // 用 Initial pass 自己检测长按：图片/视频/聊天记录等子组件自带 clickable，
                // 普通 detectTapGestures 会被子组件消费掉，这里保证所有气泡都能长按。
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    val releasedBeforeLongPress = withTimeoutOrNull(
                        viewConfiguration.longPressTimeoutMillis,
                    ) {
                        var finished = false
                        while (!finished) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                finished = true
                            } else if (
                                (change.position - down.position).getDistance() >
                                viewConfiguration.touchSlop
                            ) {
                                finished = true
                            }
                        }
                        true
                    } != null
                    if (!releasedBeforeLongPress) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        pressPosition = down.position
                        pressOffset = IntOffset(
                            down.position.x.roundToInt(),
                            down.position.y.roundToInt(),
                        )
                        show = true
                        // 长按已触发：消费后续事件，避免松手时触发子组件点击（打开图片/视频等）。
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                }
            },
    ) {
        // 1dp 透明锚点：放在手指长按位置（窗口矩形≈1x1），GlassTransformPopup 从该点展开/收缩；
        // cornerRadius 与展开面板一致（24dp，start==end）→ 不会出现负圆角。
        Box(
            modifier = Modifier
                .offset { IntOffset(pressPosition.x.roundToInt(), pressPosition.y.roundToInt()) }
                .size(1.dp)
                .glassPopupAnchor(anchor = menuAnchor, cornerRadius = 24.dp, floating = true),
        )
        content()
        when (LocalUiMode.current) {
            UiMode.Miuix -> MiuixBubbleMenu(
                message = message,
                actions = menuActions,
                show = show,
                anchor = menuAnchor,
                onDismiss = { show = false },
                onAction = { action -> show = false; onMenuAction(action, message) },
            )
            UiMode.Material -> MaterialBubbleMenu(
                message = message,
                actions = menuActions,
                show = show,
                pressOffset = pressOffset,
                onDismiss = { show = false },
                onAction = { action -> show = false; onMenuAction(action, message) },
            )
        }
    }
}

/** Material：保持“哪里长按就在哪里弹出”的 Expressive 下拉。 */
@Composable
private fun MaterialBubbleMenu(
    message: ChatMessage,
    actions: List<MessageMenuAction>,
    show: Boolean,
    pressOffset: IntOffset,
    onDismiss: () -> Unit,
    onAction: (MessageMenuAction) -> Unit,
) {
    OffsetAnchoredExpressiveMenu(
        expanded = show,
        onDismissRequest = onDismiss,
        anchorOffset = pressOffset,
    ) {
        actions.forEachIndexed { index, action ->
            SelectableDropdownMenuItem(
                text = { Text(action.label) },
                selected = false,
                leadingIcon = {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        modifier = Modifier.size(MenuDefaults.LeadingIconSize),
                    )
                },
                shapes = MenuDefaults.itemShape(
                    index = index,
                    count = actions.size,
                ),
                onClick = { onAction(action) },
            )
        }
    }
}

/**
 * Miuix：长按菜单交给根级 [com.material3tim.ui.component.miuix.glass.GlassMenuHost] 渲染。
 *
 * 锚点 = 气泡内 1dp 透明 Box（跟随手指长按点），由页面级宿主用 `GlassTransformPopup`
 * 从该点形状变换展开，预测性返回跟手收缩回手指点（几乎不覆盖气泡本身）。
 */
@Composable
private fun MiuixBubbleMenu(
    message: ChatMessage,
    actions: List<MessageMenuAction>,
    show: Boolean,
    anchor: GlassPopupAnchor,
    onDismiss: () -> Unit,
    onAction: (MessageMenuAction) -> Unit,
) {
    val glassMenu = LocalGlassMenu.current
    // 页面采样只能在页面 CompositionLocal 作用域内读，这里读到后交给根级状态保存。
    val pageBackdrop = LocalPageBackdrop.current
    // 本气泡的菜单发出方 token：气泡被移出组合（如对方撤回）时关闭残留菜单。
    val menuOwner = remember { Any() }
    val entries = remember(message.id, actions) {
        actions.map { action ->
            GlassMenuEntry(
                label = action.label,
                icon = action.icon,
                isDanger = action == MessageMenuAction.DELETE,
                onClick = { onAction(action) },
            )
        }
    }
    DisposableEffect(menuOwner) {
        onDispose { glassMenu?.hideIfOwner(menuOwner) }
    }
    // 长按后 show 变 true：先等一帧，让 1dp 锚点 Box 移动到本次长按点并完成测量上报，
    // 再交给页面级宿主用 GlassTransformPopup 从手指点展开（预测性返回跟手收缩回手指点）。
    LaunchedEffect(show) {
        if (!show) return@LaunchedEffect
        withFrameNanos { }
        glassMenu?.show(
            owner = menuOwner,
            anchor = anchor,
            entries = entries,
            backdrop = pageBackdrop,
            onDismiss = onDismiss,
        )
    }
}
