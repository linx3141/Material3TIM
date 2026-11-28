/*
 * Ported from AOSP Launcher3 widget picker (Apache License 2.0):
 *   packages/apps/Launcher3/modules/widgetpicker/.../ui/components/ScrollableFloatingToolbar.kt
 *   packages/apps/Launcher3/modules/widgetpicker/.../ui/components/LeadingIconToolbarTab.kt
 *
 * 改动：颜色/字体改用本项目 MaterialTheme；新增未读角标；选中项自动滚到可见区域居中。
 */
package com.material3tim.ui.component.bottombar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Launcher3 微件页同款的「可横向滚动的悬浮工具条」。 */
@Composable
private fun ScrollableFloatingToolbar(
    tabs: List<@Composable () -> Unit>,
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    edgePadding: Dp = 8.dp,
    maxWidth: Dp = 348.dp,
    minTabWidth: Dp = 96.dp,
    shadowElevation: Dp = 3.dp,
) {
    val scrollState = rememberScrollState()
    Surface(
        // 与 Launcher3 WidgetPickerColors.toolbarBackground 一致。
        color = MaterialTheme.colorScheme.surfaceBright,
        shadowElevation = shadowElevation,
        shape = shape,
        modifier = modifier.wrapContentSize(align = Alignment.Center),
    ) {
        ScrollableTabsLayout(
            tabs = tabs,
            scrollState = scrollState,
            maxWidth = maxWidth,
            edgePadding = edgePadding,
            minTabWidth = minTabWidth,
            selectedTabIndex = selectedTabIndex,
        )
    }
}

@Composable
private fun ScrollableTabsLayout(
    tabs: List<@Composable () -> Unit>,
    scrollState: ScrollState,
    maxWidth: Dp,
    edgePadding: Dp,
    minTabWidth: Dp,
    selectedTabIndex: Int,
) {
    val coroutineScope = rememberCoroutineScope()
    val tabsScrollState = remember(scrollState, coroutineScope) {
        TabsScrollState(scrollState = scrollState, coroutineScope = coroutineScope)
    }
    val tabsContent: @Composable () -> Unit = { tabs.forEach { it() } }
    Layout(
        contents = listOf(tabsContent),
        modifier = Modifier
            .widthIn(max = maxWidth)
            .wrapContentSize(align = Alignment.Center)
            .padding(vertical = 8.dp)
            .horizontalScroll(scrollState)
            .selectableGroup()
            .clipToBounds(),
    ) { (tabMeasurables), constraints ->
        val edgePaddingPx = edgePadding.roundToPx()
        val minTabWidthPx = minTabWidth.roundToPx()
        val layoutHeightPx = tabMeasurables.maxIntrinsicHeight()
        val tabConstraints = constraints.copy(
            minWidth = minTabWidthPx,
            minHeight = layoutHeightPx,
            maxHeight = layoutHeightPx,
        )
        val tabPlaceables = tabMeasurables.fastMap { it.measure(tabConstraints) }
        val (accumulatedWidthPx, tabPositions) =
            tabPlaceables.fold(Pair(edgePaddingPx, mutableListOf<TabPosition>())) { acc, placeable ->
                val (accumulated, positions) = acc
                val tabWidthPx = maxOf(minTabWidthPx, placeable.width)
                positions.add(TabPosition(leftPx = accumulated, widthPx = tabWidthPx))
                Pair(accumulated + tabWidthPx, positions)
            }
        val totalLayoutWidthPx = accumulatedWidthPx + edgePaddingPx
        layout(totalLayoutWidthPx, layoutHeightPx) {
            tabPlaceables.fastForEachIndexed { index, placeable ->
                placeable.placeRelative(x = tabPositions[index].leftPx, y = 0)
            }
            tabsScrollState.scrollToSelectedTab(
                tabPositions = tabPositions,
                selectedTab = selectedTabIndex,
                totalWidthPx = totalLayoutWidthPx,
            )
        }
    }
}

private data class TabPosition(val leftPx: Int, val widthPx: Int)

private class TabsScrollState(
    private val scrollState: ScrollState,
    private val coroutineScope: CoroutineScope,
) {
    private var selectedTab: Int? = null

    fun scrollToSelectedTab(tabPositions: List<TabPosition>, selectedTab: Int, totalWidthPx: Int) {
        if (this.selectedTab != selectedTab) {
            this.selectedTab = selectedTab
            tabPositions.getOrNull(selectedTab)?.let { selectedTabPosition ->
                val visibleRightOffset = scrollState.maxValue
                val visibleToolbarWidth = totalWidthPx - visibleRightOffset
                val visibleToolbarCenter = visibleToolbarWidth / 2
                val tabHalfWidth = selectedTabPosition.widthPx / 2
                val targetScrollOffset =
                    (selectedTabPosition.leftPx - (visibleToolbarCenter - tabHalfWidth))
                        .coerceIn(0, visibleRightOffset.coerceAtLeast(0))
                if (scrollState.value != targetScrollOffset) {
                    coroutineScope.launch { scrollState.animateScrollTo(targetScrollOffset) }
                }
            }
        }
    }
}

private fun List<Measurable>.maxIntrinsicHeight(): Int =
    fastFold(initial = 0) { curr, measurable ->
        maxOf(curr, measurable.maxIntrinsicHeight(Constraints.Infinity))
    }

/** 微件页同款带图标 tab：图标只在选中时显示。 */
@Composable
private fun LeadingIconToolbarTab(
    leadingIcon: ImageVector,
    label: String,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    iconEnterTransition: EnterTransition = fadeIn() + expandIn(expandFrom = Alignment.Center),
    iconExitTransition: ExitTransition = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center),
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    // 与 Launcher3 WidgetPickerColors 的 toolbar 颜色 token 一致。
    val backgroundColor =
        if (selected) colorScheme.secondaryContainer else Color.Transparent
    val contentColor =
        if (selected) colorScheme.onSecondaryContainer else colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .selectable(
                selected = selected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onClick()
                },
            )
            .background(color = backgroundColor)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = iconEnterTransition,
            exit = iconExitTransition,
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
                tint = contentColor,
            )
        }
        Text(
            text = label,
            style = if (selected) {
                MaterialTheme.typography.labelLargeEmphasized
            } else {
                MaterialTheme.typography.labelLarge
            },
            color = contentColor,
            maxLines = 1,
        )
        if (badgeCount > 0) {
            Box(modifier = Modifier.padding(start = 6.dp)) {
                Badge(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

/**
 * 首页悬浮底栏：Launcher3 微件页同款（可横向滚动、选中项显示图标、选中项自动滚到中间）。
 * 开启后替换普通底栏，适用于 M3E / Miuix 两种模式。
 */
@Composable
fun LauncherFloatingBottomBar(
    navigationBadge: NavigationBadgeState,
    selectedPage: Int,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = BottomBarDestination.entries.mapIndexed { index, destination ->
        @Composable {
            LeadingIconToolbarTab(
                leadingIcon = destination.icon,
                label = stringResource(destination.label),
                contentDescription = stringResource(destination.label),
                selected = index == selectedPage,
                onClick = { onSelectPage(index) },
                badgeCount = if (index == BottomBarDestination.Chat.ordinal) {
                    navigationBadge.unreadChatCount
                } else {
                    0
                },
            )
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        ScrollableFloatingToolbar(tabs = tabs, selectedTabIndex = selectedPage)
    }
}
