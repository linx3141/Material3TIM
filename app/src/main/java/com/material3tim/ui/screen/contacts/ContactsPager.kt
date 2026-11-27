package com.material3tim.ui.screen.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import com.material3tim.data.model.ContactItem
import com.material3tim.data.repository.ChatRepository
import com.material3tim.data.repository.LiveContacts
import com.material3tim.data.source.AppDataSources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.component.SearchStatus
import com.material3tim.ui.navigation3.Navigator

/**
 * 联系人页入口：按 [LocalUiMode] 分发到 Material / Miuix 两套列表形式。
 * 搜索状态提升到这里，切换页面时自动收起 Miuix 搜索浮层。
 */
@Composable
fun ContactsPager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val liveSource = AppDataSources.contactSource
    var allContacts by remember { mutableStateOf(if (liveSource != null) emptyList() else ChatRepository.sampleContacts) }
    var groups by remember {
        mutableStateOf(if (liveSource != null) emptyList() else ChatRepository.groupedContacts())
    }

    // 宿主里异步拉取真实好友列表，并订阅后续刷新。
    LaunchedEffect(liveSource) {
        if (liveSource == null) return@LaunchedEffect
        withContext(Dispatchers.IO) { LiveContacts.refresh() }
        LiveContacts.flow.collect { contacts ->
            if (contacts != null) {
                allContacts = contacts
                groups = LiveContacts.grouped(contacts)
            }
        }
    }

    var materialSearchText by remember { mutableStateOf("") }
    var miuixSearchStatus by remember { mutableStateOf(SearchStatus(label = "")) }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            if (!miuixSearchStatus.isCollapsed()) {
                miuixSearchStatus = miuixSearchStatus.copy(
                    searchText = "",
                    current = SearchStatus.Status.COLLAPSED,
                )
            }
            materialSearchText = ""
        }
    }

    when (LocalUiMode.current) {
        UiMode.Miuix -> ContactsPagerMiuix(
            allContacts = allContacts,
            groups = groups,
            searchStatus = miuixSearchStatus,
            onSearchStatusChange = { status ->
                miuixSearchStatus = status
                materialSearchText = status.searchText
            },
            bottomInnerPadding = bottomInnerPadding,
            onAddFriend = { navigator.push(com.material3tim.ui.navigation3.Route.AddFriend) },
        )

        UiMode.Material -> ContactsPagerMaterial(
            groups = groups,
            searchText = materialSearchText,
            onSearchTextChange = { materialSearchText = it },
            bottomInnerPadding = bottomInnerPadding,
            onAddFriend = { navigator.push(com.material3tim.ui.navigation3.Route.AddFriend) },
        )
    }
}

/** 按名称 / 个性签名过滤联系人（大小写不敏感）。 */
internal fun filterContacts(contacts: List<ContactItem>, query: String): List<ContactItem> {
    if (query.isBlank()) return contacts
    return contacts.filter { contact ->
        contact.name.contains(query, ignoreCase = true) ||
            contact.signature.contains(query, ignoreCase = true)
    }
}
