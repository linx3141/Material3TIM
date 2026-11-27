package com.material3tim.ui.screen.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.CompositionLocalProvider
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.CardItem
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.component.miuix.hyper.HyperTopBarIconButton
import com.material3tim.ui.component.miuix.hyper.groupedCardItems

import android.widget.Toast
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.ContactItem
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.SearchStatus
import com.material3tim.ui.component.miuix.SearchBarFake
import com.material3tim.ui.component.miuix.SearchBox
import com.material3tim.ui.component.miuix.SearchPager
import com.material3tim.ui.component.statustag.StatusTag
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddCircle
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Pin
import top.yukonga.miuix.kmp.icon.extended.Promotions
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private data class ContactQuickEntryMiuix(
    val titleRes: Int,
    val icon: ImageVector,
    val route: Route,
)

@Composable
private fun rememberMiuixQuickEntries(): List<ContactQuickEntryMiuix> = listOf(
    ContactQuickEntryMiuix(R.string.contacts_new_friends, MiuixIcons.Contacts, Route.NewFriends),
    ContactQuickEntryMiuix(R.string.contacts_group_chats, MiuixIcons.Community, Route.GroupChats),
    ContactQuickEntryMiuix(R.string.contacts_tags, MiuixIcons.Pin, Route.Tags),
    ContactQuickEntryMiuix(R.string.contacts_official, MiuixIcons.Promotions, Route.OfficialAccounts),
)

/**
 * 联系人页 Miuix 实现：快捷入口卡片 + 字母分组 + Card 联系人行，
 * 与 KernelSU 超级用户列表的 Miuix 形式保持一致。
 */
@Composable
fun ContactsPagerMiuix(
    allContacts: List<ContactItem>,
    groups: List<Pair<Char, List<ContactItem>>>,
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    bottomInnerPadding: Dp,
    onAddFriend: () -> Unit,
) {
    val enableBlur = LocalEnableBlur.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val navigator = LocalNavigator.current

    val scrollBehavior = MiuixScrollBehavior()
    val dynamicTopPadding by remember {
        derivedStateOf { 12.dp * (1f - scrollBehavior.state.collapsedFraction) }
    }

    // Phase 2b：页面级 Glass 采样 backdrop；关闭模糊/无 runtime shader 时 null → 纯色回退。
    val backdrop = rememberPageBackdrop(enableBlur)
    val lazyListState = rememberLazyListState()

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
    Scaffold(
        topBar = {
            HyperTopAppBar(
                title = stringResource(R.string.contacts),
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                actions = {
                    // 圆形 Glass 按钮（玻璃模式）/ 普通 IconButton（纯色回退）。
                    HyperTopBarIconButton(
                        onClick = onAddFriend,
                        contentDescription = stringResource(R.string.contacts_add_friend),
                        icon = MiuixIcons.AddCircle,
                    )
                },
                bottomContent = {
                            Box(
                                modifier = Modifier
                                    .alpha(if (searchStatus.isCollapsed()) 1f else 0f)
                                    .onGloballyPositioned { coordinates ->
                                        with(density) {
                                            val newOffsetY = coordinates.positionInWindow().y.toDp()
                                            if (searchStatus.offsetY != newOffsetY) {
                                                onSearchStatusChange(searchStatus.copy(offsetY = newOffsetY))
                                            }
                                        }
                                    }
                                    .then(
                                        if (searchStatus.isCollapsed()) {
                                            Modifier.pointerInput(Unit) {
                                                detectTapGestures {
                                                    onSearchStatusChange(
                                                        searchStatus.copy(current = SearchStatus.Status.EXPANDING)
                                                    )
                                                }
                                            }
                                        } else {
                                            Modifier
                                        },
                                    ),
                            ) {
                                SearchBarFake(
                                    label = stringResource(R.string.contacts_search_hint),
                                    searchBarTopPadding = dynamicTopPadding,
                                )
                            }
                },
            )
        },
        popupHost = {
            val searchResults = filterContacts(allContacts, searchStatus.searchText)
            searchStatus.SearchPager(
                onSearchStatusChange = onSearchStatusChange,
                defaultResult = {},
                searchBarTopPadding = dynamicTopPadding,
            ) {
                val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .overScrollVertical(),
                ) {
                    item {
                        Spacer(Modifier.height(6.dp))
                    }
                    if (searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.contacts_search_empty),
                                    color = colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                    items(searchResults, key = { it.id }, contentType = { "contact" }) { contact ->
                        ContactCardMiuix(
                            contact = contact,
                            onClick = {
                                onSearchStatusChange(
                                    searchStatus.copy(
                                        searchText = "",
                                        current = SearchStatus.Status.COLLAPSING,
                                    )
                                )
                                navigator.push(Route.ContactDetail(contact.id))
                            },
                        )
                    }
                    item {
                        Spacer(Modifier.height(maxOf(bottomInnerPadding, imeBottomPadding)))
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        searchStatus.SearchBox {
            Box {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .scrollEndHaptic()
                        .overScrollVertical()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        // 采样层要早于 inset/padding：不透明底 + 列表内容（不含玻璃顶栏自身）。
                        .then(
                            if (backdrop != null) {
                                Modifier
                                    .layerBackdrop(backdrop)
                                    .background(colorScheme.surface)
                            } else {
                                Modifier
                            },
                        ),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 6.dp,
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                    ),
                    overscrollEffect = null,
                ) {
                    item(key = "quick_entries") {
                        ContactsQuickEntriesMiuix(onClick = { route -> navigator.push(route) })
                    }
                    groups.forEach { (letter, contacts) ->
                        item(key = "letter_$letter") {
                            ContactsLetterHeaderMiuix(letter)
                        }
                        // HyperOS4 分组卡片：同一字母分组内的联系人合并为 squircle 分段卡片。
                        groupedCardItems(
                            keyPrefix = "contacts_$letter",
                            outerTopPadding = 0.dp,
                            outerBottomPadding = 6.dp,
                            items = contacts.map { contact ->
                                CardItem(contact.id) {
                                    ContactRowItemMiuix(
                                        contact = contact,
                                        onClick = { navigator.push(Route.ContactDetail(contact.id)) },
                                    )
                                }
                            },
                        )
                    }
                    item {
                        Spacer(Modifier.height(bottomInnerPadding + 6.dp))
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ContactsQuickEntriesMiuix(onClick: (Route) -> Unit) {
    val entries = rememberMiuixQuickEntries()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 6.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        entries.forEach { entry ->
            Card(
                modifier = Modifier.weight(1f),
                onClick = { onClick(entry.route) },
                showIndication = true,
                insideMargin = PaddingValues(horizontal = 2.dp, vertical = 10.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = entry.icon,
                        tint = colorScheme.primary,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(entry.titleRes),
                        fontSize = 11.sp,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 联系人页字母分组头（@ 成员 Sheet 复用同一视觉）。 */
@Composable
internal fun ContactsLetterHeaderMiuix(letter: Char) {
    Text(
        text = letter.toString(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 6.dp),
    )
}

/** 单条联系人卡片：48dp 头像 + 昵称 + 签名 + VIP 标签。 */
@Composable
private fun ContactRowContentMiuix(contact: ContactItem, modifier: Modifier = Modifier) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            ChatAvatar(
                name = contact.name,
                seed = contact.avatarSeed,
                isOnline = contact.isOnline,
                avatarPath = contact.avatarPath,
                modifier = Modifier.padding(end = 10.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    modifier = Modifier.basicMarquee(),
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                )
                if (contact.signature.isNotBlank()) {
                    Text(
                        text = contact.signature,
                        modifier = Modifier.basicMarquee(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight(550),
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            if (contact.isVip) {
                StatusTag(
                    label = stringResource(R.string.contacts_vip),
                    backgroundColor = colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                    contentColor = colorScheme.onTertiaryContainer,
                )
            }
        }
}

@Composable
private fun ContactRowItemMiuix(contact: ContactItem, onClick: () -> Unit) {
    ContactRowContentMiuix(
        contact = contact,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
private fun ContactCardMiuix(contact: ContactItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 6.dp),
        onClick = onClick,
        showIndication = true,
        insideMargin = PaddingValues(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        ContactRowContentMiuix(contact)
    }
}
