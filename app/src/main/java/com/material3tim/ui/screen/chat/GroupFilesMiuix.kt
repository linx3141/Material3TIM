package com.material3tim.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.material3tim.R
import com.material3tim.data.model.GroupFileEntry
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.SideEffect
import com.material3tim.ui.component.miuix.glass.GlassConfirmDialog
import com.material3tim.ui.component.miuix.glass.GlassInputDialog
import com.material3tim.ui.component.miuix.glass.LocalPageBackdrop
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.material3tim.ui.component.miuix.hyper.HyperTopBarIconButton
import com.material3tim.ui.component.miuix.glass.rememberPageBackdrop
import com.material3tim.ui.component.miuix.hyper.HyperTopAppBar
import com.material3tim.ui.theme.LocalEnableBlur
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.icon.basic.Search
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.AddFolder
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Folder
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.UploadCloud
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * 群文件页（Miuix）：目录浏览 + 下载进度 + 管理模式（上传/新建文件夹/重命名/移动/删除）。
 */
@Composable
internal fun GroupFilesMiuix(
    state: GroupFilesState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPickUpload: () -> Unit,
    onOpenFolder: (GroupFileEntry) -> Unit = {},
) {
    val enableBlur = LocalEnableBlur.current
    val scrollBehavior = MiuixScrollBehavior()
    // Phase 2b：页面级 Glass 采样 backdrop（关闭模糊/无 runtime shader 时为 null → 纯色回退）。
    val backdrop = rememberPageBackdrop(enableBlur)

    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
    Scaffold(
        topBar = {
            HyperTopAppBar(
                title = stringResource(R.string.group_files_title),
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                navigationIcon = {
                    // 圆形 Glass 按钮（玻璃模式）/ 普通 IconButton（纯色回退）。
                    HyperTopBarIconButton(
                        onClick = onBack,
                        contentDescription = stringResource(R.string.chat_detail_back),
                        icon = MiuixIcons.Back,
                    )
                },
                actions = {
                    HyperTopBarIconButton(
                        onClick = onRefresh,
                        contentDescription = stringResource(R.string.group_files_refresh),
                        icon = MiuixIcons.Refresh,
                    )
                    if (state.canManage) {
                        if (state.manageMode) {
                            HyperTopBarIconButton(
                                onClick = onPickUpload,
                                contentDescription = stringResource(R.string.group_files_upload),
                                icon = MiuixIcons.UploadCloud,
                            )
                            HyperTopBarIconButton(
                                onClick = state::showCreateFolder,
                                contentDescription = stringResource(R.string.group_files_new_folder),
                                icon = MiuixIcons.AddFolder,
                            )
                            TextButton(
                                text = stringResource(R.string.group_files_done),
                                onClick = state::toggleManageMode,
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                            )
                        } else {
                            TextButton(
                                text = stringResource(R.string.group_files_manage),
                                onClick = state::toggleManageMode,
                                colors = ButtonDefaults.textButtonColorsPrimary(),
                            )
                        }
                    }
                },
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout)
            .only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 采样层要早于 inset/padding：记录不透明底 + 页面内容（不含玻璃顶栏自身）。
                .then(
                    if (backdrop != null) {
                        Modifier
                            .layerBackdrop(backdrop)
                            .background(colorScheme.surface)
                    } else {
                        Modifier
                    },
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                GroupFilesBreadcrumbMiuix(state = state, onBack = onBack)
                // 与首页消息列表顶部一致的 Miuix 搜索栏（InputField 胶囊）。
                InputField(
                    query = state.searchQuery,
                    onQueryChange = state::updateSearchQuery,
                    label = stringResource(R.string.group_files_search_hint),
                    leadingIcon = {
                        Icon(
                            imageVector = MiuixIcons.Basic.Search,
                            contentDescription = null,
                            tint = colorScheme.onSurfaceContainerHigh,
                            modifier = Modifier
                                .size(44.dp)
                                .padding(start = 16.dp, end = 8.dp),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 4.dp, bottom = 6.dp),
                    onSearch = { },
                    enabled = true,
                    expanded = false,
                    onExpandedChange = { },
                )
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        state.searchQuery.isNotBlank() -> GroupFilesSearchContentMiuix(state = state)
                        !state.sourceAvailable -> GroupFilesEmptyMiuix(
                            text = stringResource(R.string.group_files_host_only),
                        )
                        state.loading && state.entries.isEmpty() -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            // Miuix 标准 loading。
                            InfiniteProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                size = 48.dp,
                            )
                        }
                        state.error != null && state.entries.isEmpty() -> GroupFilesErrorMiuix(
                            message = state.error.orEmpty(),
                            onRetry = onRefresh,
                        )
                        state.entries.isEmpty() -> GroupFilesEmptyMiuix(
                            text = stringResource(R.string.group_files_empty),
                        )
                        else -> LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 4.dp,
                                bottom = 16.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(state.folders, key = { "folder_${state.entryKey(it)}" }) { entry ->
                                GroupFilesFolderRowMiuix(
                                    state = state,
                                    entry = entry,
                                    onClick = { onOpenFolder(entry) },
                                )
                            }
                            items(state.files, key = { "file_${state.entryKey(it)}" }) { entry ->
                                GroupFilesFileRowMiuix(state = state, entry = entry)
                            }
                            if (!state.isEnd) {
                                item(key = "load_more") {
                                    LaunchedEffect(state.entries.size) {
                                        state.loadMore()
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (state.loadingMore) {
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .width(96.dp)
                                                    .height(4.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (state.manageMode) {
                    GroupFilesManageBarMiuix(state = state)
                }
            }
        }
    }
    }
    GroupFilesDialogsMiuix(state = state, backdrop = backdrop)
}

/** 群文件搜索（Miuix）：结果按文件行样式显示。 */
@Composable
private fun GroupFilesSearchContentMiuix(state: GroupFilesState) {
    when {
        state.searchLoading && state.searchResults.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            InfiniteProgressIndicator(
                modifier = Modifier.size(48.dp),
                size = 48.dp,
            )
        }
        state.searchError != null && state.searchResults.isEmpty() -> GroupFilesErrorMiuix(
            message = state.searchError.orEmpty(),
            onRetry = { state.updateSearchQuery(state.searchQuery) },
        )
        state.searchResults.isEmpty() -> GroupFilesEmptyMiuix(
            text = stringResource(R.string.group_files_search_empty),
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 4.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(state.searchResults, key = { "search_${state.entryKey(it)}" }) { entry ->
                GroupFilesFileRowMiuix(state = state, entry = entry)
            }
        }
    }
}

@Composable
private fun GroupFilesBreadcrumbMiuix(state: GroupFilesState, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.breadcrumb,
            fontSize = 12.sp,
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (state.canGoUp) {
                        Modifier.clickable { onBack() }
                    } else {
                        Modifier
                    },
                ),
        )
        Text(
            text = stringResource(R.string.group_files_folder_count, state.allFileCount),
            fontSize = 11.sp,
            color = colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
        )
    }
}

@Composable
private fun GroupFilesEmptyMiuix(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, fontSize = 14.sp, color = colorScheme.onSurfaceVariantSummary)
    }
}

@Composable
private fun GroupFilesErrorMiuix(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, fontSize = 14.sp, color = colorScheme.onSurfaceVariantSummary)
        Spacer(Modifier.height(8.dp))
        TextButton(
            text = stringResource(R.string.group_files_retry),
            onClick = onRetry,
        )
    }
}

@Composable
private fun GroupFilesFolderRowMiuix(
    state: GroupFilesState,
    entry: GroupFileEntry,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            if (state.manageMode) state.toggleSelect(entry) else onClick()
        },
        showIndication = true,
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.manageMode) {
                Checkbox(
                    state = if (state.isSelected(entry)) ToggleableState.On else ToggleableState.Off,
                    onClick = { state.toggleSelect(entry) },
                )
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                imageVector = MiuixIcons.Folder,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.fileCount > 0) {
                    Text(
                        text = stringResource(R.string.group_files_folder_count, entry.fileCount),
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                }
            }
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun GroupFilesFileRowMiuix(state: GroupFilesState, entry: GroupFileEntry) {
    val download = state.downloads[entry.fileId]
    val downloading = download != null && !download.done && !download.failed
    val onClick: () -> Unit = {
        when {
            state.manageMode -> state.toggleSelect(entry)
            entry.localPath != null -> state.openFile(entry)
            else -> state.download(entry)
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        showIndication = true,
        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.manageMode) {
                Checkbox(
                    state = if (state.isSelected(entry)) ToggleableState.On else ToggleableState.Off,
                    onClick = { state.toggleSelect(entry) },
                )
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                imageVector = groupFileKindIcon(entry),
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    fontSize = 14.sp,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = groupFileMetaText(entry)
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        fontSize = 11.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                state.manageMode -> Unit
                entry.localPath != null -> TextButton(
                    text = stringResource(R.string.group_files_open),
                    onClick = { state.openFile(entry) },
                )
                downloading -> Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(64.dp),
                ) {
                    LinearProgressIndicator(
                        progress = download.progress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        height = 4.dp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${download.progress}%",
                        fontSize = 10.sp,
                        color = colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                    )
                }
                else -> IconButton(onClick = { state.download(entry) }) {
                    Icon(
                        imageVector = MiuixIcons.Download,
                        contentDescription = stringResource(R.string.group_files_download),
                        tint = colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupFilesManageBarMiuix(state: GroupFilesState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        insideMargin = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.group_files_selected_count, state.selectedKeys.size),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            TextButton(
                text = stringResource(R.string.group_files_delete),
                onClick = state::showDelete,
                enabled = state.selectedKeys.isNotEmpty(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
            TextButton(
                text = stringResource(R.string.group_files_move),
                onClick = state::showMove,
                enabled = state.selectedEntries.isNotEmpty() && state.selectedEntries.none { it.isFolder },
            )
            TextButton(
                text = stringResource(R.string.group_files_rename),
                onClick = state::showRename,
                enabled = state.selectedEntries.size == 1,
            )
        }
    }
}

@Composable
private fun GroupFilesDialogsMiuix(state: GroupFilesState, backdrop: Backdrop?) {
    // 常驻组合：记住最后一个非空 dialog，让 Glass 组件在关闭动画期间仍有内容可绘制。
    var lastDialog by remember { mutableStateOf<GroupFilesDialog?>(null) }
    SideEffect {
        if (state.dialog != null) lastDialog = state.dialog
    }
    val dialog = state.dialog ?: lastDialog
    val visible = state.dialog != null
    if (dialog == null) return

    // 页面级 backdrop 的 provider 在 Scaffold 处已结束，这里再包一层供玻璃对话框采样
    //（backdrop 为 null 时 Glass 组件自动不透明回退）。
    CompositionLocalProvider(LocalPageBackdrop provides backdrop) {
        // 文本输入内容：用于「非空才可确认」；打开时由 GlassInputDialog 重置为 initialText。
        var textInput by remember { mutableStateOf("") }

        // ---- 删除确认 ----
        GlassConfirmDialog(
            show = visible && dialog is GroupFilesDialog.Delete,
            onDismissRequest = state::dismissDialog,
            title = stringResource(R.string.group_files_delete_title),
            summary = stringResource(R.string.group_files_delete_message),
            confirmText = stringResource(R.string.group_files_delete),
            cancelText = stringResource(R.string.cancel),
            destructive = true,
            onConfirm = {
                (dialog as? GroupFilesDialog.Delete)?.let { state.delete(it.entries) }
            },
        )

        // ---- 新建文件夹 / 重命名：同一个玻璃输入对话框 ----
        val isTextInput = dialog is GroupFilesDialog.CreateFolder || dialog is GroupFilesDialog.Rename
        val textInputTitle = when (dialog) {
            GroupFilesDialog.CreateFolder -> stringResource(R.string.group_files_new_folder)
            is GroupFilesDialog.Rename -> stringResource(R.string.group_files_rename)
            else -> ""
        }
        val textInputPlaceholder = when (dialog) {
            GroupFilesDialog.CreateFolder -> stringResource(R.string.group_files_folder_hint)
            is GroupFilesDialog.Rename -> stringResource(R.string.group_files_rename_hint)
            else -> null
        }
        val textInputInitial = (dialog as? GroupFilesDialog.Rename)?.entry?.name ?: ""
        GlassInputDialog(
            show = visible && isTextInput,
            onDismissRequest = state::dismissDialog,
            title = textInputTitle,
            summary = null,
            initialText = textInputInitial,
            placeholder = textInputPlaceholder,
            singleLine = true,
            confirmText = stringResource(R.string.confirm),
            cancelText = stringResource(R.string.cancel),
            confirmEnabled = textInput.isNotBlank(),
            onTextChange = { textInput = it },
            onConfirm = { name ->
                when (val d = dialog) {
                    GroupFilesDialog.CreateFolder -> state.createFolder(name)
                    is GroupFilesDialog.Rename -> state.rename(d.entry, name)
                    else -> Unit
                }
            },
        )

        // ---- 移动到文件夹：自定义内容 GlassDialog（GlassCard 行 + 可滚动列表）----
        val moveEntries = (dialog as? GroupFilesDialog.Move)?.entries.orEmpty()
        GlassDialog(
            show = visible && dialog is GroupFilesDialog.Move,
            onDismissRequest = state::dismissDialog,
            backdrop = LocalPageBackdrop.current,
            title = stringResource(R.string.group_files_move_title),
            summary = stringResource(R.string.group_files_move_desc),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                state.moveTargets.forEach { target ->
                    // 玻璃弹层内用普通 Card 承载文件夹行（miuix 0.9.5-beta03 无 GlassCard）。
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        onClick = { state.move(moveEntries, target.id) },
                        showIndication = true,
                        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = MiuixIcons.Folder,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = target.name,
                                fontSize = 14.sp,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            GlassDialogButton(
                onClick = state::dismissDialog,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
}

