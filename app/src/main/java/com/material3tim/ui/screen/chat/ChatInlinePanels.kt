package com.material3tim.ui.screen.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.material3tim.ui.component.ime.ChatImeController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** 只挂在文本框上，避免点两侧图标也唤起 IME；面板打开时由文本框获取焦点后接管。 */
internal fun Modifier.chatTextFieldIme(composer: ChatComposerState, ime: ChatImeController): Modifier =
    pointerInput(composer, ime) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            if (!composer.isPanelOpen && !ime.isImeVisibleNow()) ime.requestShow("click")
        }
    }

/** 记录切换瞬间输入栏距窗口底部的距离（含导航栏），而不是猜测某个输入法的高度。 */
internal class ChatPanelHandoff(
    private val composer: ChatComposerState,
    private val keyboard: State<Dp>,
    private val navBar: State<Dp>,
) {
    private var heldBottom by mutableStateOf(0.dp)
    var returningToIme by mutableStateOf(false)
        private set

    fun captureKeyboard() {
        val bottom = keyboard.value.takeIf { it > navBar.value } ?: 0.dp
        heldBottom = if (returningToIme) maxOf(heldBottom, bottom) else bottom
        returningToIme = false
    }

    fun returnToKeyboard() { returningToIme = heldBottom > 0.dp }
    fun finishReturn() { returningToIme = false }
    val keyboardTookOver: Boolean get() = keyboard.value >= heldBottom

    val panelSpace: Dp get() = if (composer.isPanelOpen || returningToIme) {
        (heldBottom - navBar.value).coerceAtLeast(0.dp)
    } else 0.dp

    // 现有布局会另加 IME 平移，所以要扣掉已由面板占用的部分，不能叠加两次。
    val remainingImeInset = derivedStateOf { (keyboard.value - panelSpace).coerceAtLeast(0.dp) }
}

@Composable
internal fun rememberChatPanelHandoff(
    composer: ChatComposerState,
    keyboard: State<Dp>,
    navigationBarBottom: Dp,
): ChatPanelHandoff {
    val navBar = rememberUpdatedState(navigationBarBottom)
    val handoff = remember(composer, keyboard) { ChatPanelHandoff(composer, keyboard, navBar) }
    LaunchedEffect(handoff, handoff.returningToIme) {
        if (handoff.returningToIme) {
            // 键盘可能被系统拒绝、变矮或改成浮动；不能因此永久留下空白占位。
            withTimeoutOrNull(900) { snapshotFlow { handoff.keyboardTookOver }.first { it } }
            if (handoff.returningToIme) handoff.finishReturn()
        }
    }
    return handoff
}

@Composable
internal fun rememberChatPanelToggle(
    composer: ChatComposerState,
    ime: ChatImeController,
    keyboardHeight: State<Dp>,
    input: FocusRequester,
    handoff: ChatPanelHandoff,
): (ChatPanel) -> Unit {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var restoreIme by remember(composer) { mutableStateOf(false) }
    return { target ->
        // 只在从输入区进入面板时记录；表情和附件互切不能覆盖原来的键盘状态。
        if (!composer.isPanelOpen) {
            restoreIme = handoff.returningToIme || ime.isImeVisibleNow() || keyboardHeight.value > 0.dp
            handoff.captureKeyboard()
        }
        composer.dismissAtPicker()
        composer.togglePanel(target)
        if (composer.isPanelOpen) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            ime.hideIme("panel")
        } else if (restoreIme) {
            restoreIme = false
            handoff.returnToKeyboard()
            input.requestFocus()
            ime.requestShow("panel-closed")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChatInlinePanelHost(
    listState: LazyListState,
    panel: ChatPanel,
    handoff: ChatPanelHandoff,
    keyboardHeight: State<Dp>,
    emoji: @Composable () -> Unit,
    attach: @Composable () -> Unit,
) {
    // hide 是异步的，宿主有时也只上报高度。等 IME 不再占位才展开面板，
    // 切回键盘时则立即撤掉面板，不让退出动画和 IME 同时占用底部。
    val keyboardHasHeight by remember(keyboardHeight) { derivedStateOf { keyboardHeight.value > 0.dp } }
    val followPanelHeight: (IntSize) -> Unit = remember(listState) {
        var previousHeight: Int? = null
        { size ->
            val delta = size.height - (previousHeight ?: size.height)
            previousHeight = size.height
            // Column 先测面板，再测占剩余空间的消息列表。用高度差调整首项偏移，
            // 让当前消息随输入栏移动；不跳到末条，也不打断用户正在进行的滚动。
            if (delta != 0 && !listState.isScrollInProgress && listState.layoutInfo.totalItemsCount > 0) {
                listState.requestScrollToItem(
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset + delta,
                )
            }
        }
    }
    val showContent = panel != ChatPanel.None && !WindowInsets.isImeVisible && !keyboardHasHeight
    val panelContent: @Composable () -> Unit = {
        when (panel) {
            ChatPanel.Emoji -> emoji()
            ChatPanel.Attach -> attach()
            ChatPanel.None -> Unit
        }
    }
    val reserved = handoff.panelSpace
    val size = if (reserved > 0.dp) Modifier.height(reserved) else Modifier
    // 外层常驻以观测关闭时的 0 高度。键盘交接时先占位，IME 消失后直接替换内容，
    // 不再从 0 播第二次展开动画；从未打开键盘时仍保留原来的面板动效。
    Box(Modifier.onSizeChanged(followPanelHeight).then(size), propagateMinConstraints = true) {
        if (reserved > 0.dp) {
            if (showContent) panelContent()
        } else {
            AnimatedVisibility(
                visible = showContent,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = ExitTransition.None,
            ) { panelContent() }
        }
    }
}
