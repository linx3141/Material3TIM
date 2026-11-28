package com.material3tim.ui.screen.contacts

import android.widget.Toast
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.material3tim.R
import com.material3tim.data.model.ContactItem
import com.material3tim.ui.component.ChatAvatar
import com.material3tim.ui.component.material.ExpressiveScaffold
import com.material3tim.ui.component.material.SearchAppBar
import com.material3tim.ui.component.material.SegmentedItem
import com.material3tim.ui.component.material.SegmentedListItem
import com.material3tim.ui.component.material.TonalCard
import com.material3tim.ui.component.statustag.StatusTag
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route

private data class ContactQuickEntry(
    val titleRes: Int,
    val icon: ImageVector,
    val route: Route,
)

private val materialQuickEntries = listOf(
    ContactQuickEntry(R.string.contacts_new_friends, Icons.Filled.PersonAdd, Route.NewFriends),
    ContactQuickEntry(R.string.contacts_group_chats, Icons.Filled.Groups, Route.GroupChats),
    ContactQuickEntry(R.string.contacts_tags, Icons.AutoMirrored.Filled.Label, Route.Tags),
    ContactQuickEntry(R.string.contacts_official, Icons.Filled.Campaign, Route.OfficialAccounts),
)

/**
 * 联系人页 Material 3 Expressive 实现：
 * 快捷入口卡片 + 字母分组标题 + SegmentedListItem 联系人行。
 */
@Composable
fun ContactsPagerMaterial(
    groups: List<Pair<Char, List<ContactItem>>>,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val onFeatureComingSoon: () -> Unit = {
        Toast.makeText(context, R.string.feature_coming_soon, Toast.LENGTH_SHORT).show()
    }
    val searchResults = if (searchText.isBlank()) {
        emptyList()
    } else {
        groups.flatMap { (_, contacts) -> contacts }.let { contacts -> filterContacts(contacts, searchText) }
    }

    ExpressiveScaffold(
        topBar = {
            SearchAppBar(
                snackbarHostState = snackbarHostState,
                title = { Text(stringResource(R.string.contacts)) },
                searchText = searchText,
                onSearchTextChange = onSearchTextChange,
                onClearClick = { onSearchTextChange("") },
                actions = {
                    IconButton(onClick = onFeatureComingSoon) {
                        Icon(
                            imageVector = Icons.Filled.PersonAdd,
                            contentDescription = stringResource(R.string.contacts_add_friend),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                defaultContent = { _, _ -> },
                alwaysCollapsedTopBar = true,
                searchContent = { bottomPadding, closeSearch ->
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp + bottomPadding + bottomInnerPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.contacts_search_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyColumn(
                            state = searchListState,
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp + bottomPadding + bottomInnerPadding,
                            ),
                        ) {
                            itemsIndexed(searchResults, key = { _, contact -> contact.id }) { index, contact ->
                                SegmentedItem(index = index, count = searchResults.size) {
                                    ContactListRowMaterial(contact = contact) {
                                        closeSearch()
                                        navigator.push(Route.ContactDetail(contact.id))
                                    }
                                }
                            }
                        }
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = 16.dp + bottomInnerPadding,
            ),
        ) {
            item(key = "quick_entries") {
                ContactsQuickEntriesMaterial(onClick = { route -> navigator.push(route) })
            }
            groups.forEach { (letter, contacts) ->
                item(key = "letter_$letter") {
                    ContactsLetterHeaderMaterial(letter)
                }
                itemsIndexed(contacts, key = { _, contact -> contact.id }) { index, contact ->
                    SegmentedItem(index = index, count = contacts.size) {
                        ContactListRowMaterial(
                            contact = contact,
                            onClick = { navigator.push(Route.ContactDetail(contact.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactsQuickEntriesMaterial(onClick: (Route) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        materialQuickEntries.forEach { entry ->
            TonalCard(
                modifier = Modifier.weight(1f),
                onClick = { onClick(entry.route) },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(entry.titleRes),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 联系人页字母分组头（@ 成员 Sheet 复用同一视觉）。 */
@Composable
internal fun ContactsLetterHeaderMaterial(letter: Char) {
    Text(
        text = letter.toString(),
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
    )
}

/** 单条联系人行：44dp 头像 + 昵称 + 签名 + VIP 标签。 */
@Composable
internal fun ContactListRowMaterial(contact: ContactItem, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val supporting: (@Composable () -> Unit)? = if (contact.signature.isNotBlank()) {
        {
            Text(
                text = contact.signature,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        null
    }
    val trailing: (@Composable () -> Unit)? = if (contact.isVip) {
        {
            StatusTag(
                label = stringResource(R.string.contacts_vip),
                backgroundColor = colorScheme.tertiaryContainer,
                contentColor = colorScheme.onTertiaryContainer,
            )
        }
    } else {
        null
    }

    SegmentedListItem(
        onClick = onClick,
        leadingContent = {
            ChatAvatar(
                name = contact.name,
                seed = contact.avatarSeed,
                size = 44.dp,
                isOnline = contact.isOnline,
                avatarPath = contact.avatarPath,
            )
        },
        headlineContent = {
            Text(
                text = contact.name,
                modifier = Modifier.basicMarquee(),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = supporting,
        trailingContent = trailing,
    )
}
