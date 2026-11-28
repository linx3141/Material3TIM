package com.material3tim.ui.screen.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.material3tim.R
import com.material3tim.data.model.GroupFileDownload
import com.material3tim.data.model.GroupFileEntry
import com.material3tim.data.source.AppDataSources
import com.material3tim.data.source.GroupFileSource
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator
import com.material3tim.ui.navigation3.Route
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 群文件目录栈中的一层（根目录 id 为空字符串）。 */
internal data class GroupFilesFolder(val id: String, val name: String)

/** 群文件页当前弹出的对话框。 */
internal sealed interface GroupFilesDialog {
    data object CreateFolder : GroupFilesDialog
    data class Rename(val entry: GroupFileEntry) : GroupFilesDialog
    data class Move(val entries: List<GroupFileEntry>) : GroupFilesDialog
    data class Delete(val entries: List<GroupFileEntry>) : GroupFilesDialog
}

/** 上传前从系统文件选择器拷到 cache 的临时文件。 */
private data class GroupFilesUpload(val path: String, val name: String)

/**
 * 群文件页状态机（M3E/Miuix 共用）：
 * 目录栈、分页列表、下载进度、管理/多选、上传/新建/重命名/移动/删除。
 *
 * 所有 [GroupFileSource] 调用都切到 IO 线程；下载进度回调可能来自其它线程，
 * 统一用 [scope].launch 切回主线程更新 Compose 状态。
 */
@Stable
internal class GroupFilesState(
    private val context: Context,
    val chatId: String,
    private val scope: CoroutineScope,
    private val rootLabel: String,
    initialFolderId: String = "",
    initialFolderName: String = "",
) {
    private val source: GroupFileSource? = AppDataSources.groupFileSource

    val entries = mutableStateListOf<GroupFileEntry>()
    val downloads = mutableStateMapOf<String, GroupFileDownload>()

    var pathStack by mutableStateOf(
        if (initialFolderId.isBlank()) {
            listOf(GroupFilesFolder("", rootLabel))
        } else {
            listOf(
                GroupFilesFolder("", rootLabel),
                GroupFilesFolder(initialFolderId, initialFolderName),
            )
        },
    )
        private set
    var loading by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var isEnd by mutableStateOf(true)
        private set
    var allFileCount by mutableStateOf(0)
        private set
    var canManage by mutableStateOf(false)
        private set
    var manageMode by mutableStateOf(false)
        private set
    var selectedKeys by mutableStateOf<Set<String>>(emptySet())
        private set
    var dialog by mutableStateOf<GroupFilesDialog?>(null)
    var busy by mutableStateOf(false)
        private set

    // 搜索（文件名关键字）：搜索结果替换当前列表显示，不改变目录栈。
    var searchMode by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var searchResults by mutableStateOf<List<GroupFileEntry>>(emptyList())
        private set
    var searchLoading by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set
    private var searchJob: Job? = null

    private var nextIndex = 0
    private var requestId = 0

    val sourceAvailable: Boolean get() = source != null
    val canGoUp: Boolean get() = pathStack.size > 1
    val currentFolderId: String get() = pathStack.last().id
    val currentFolderName: String get() = pathStack.last().name
    val breadcrumb: String get() = pathStack.joinToString(" / ") { it.name }
    val folders: List<GroupFileEntry> get() = entries.filter { it.isFolder }
    val files: List<GroupFileEntry> get() = entries.filterNot { it.isFolder }
    val selectedEntries: List<GroupFileEntry> get() = entries.filter { entryKey(it) in selectedKeys }
    val moveTargets: List<GroupFilesFolder>
        get() = listOf(GroupFilesFolder("", rootLabel)) +
            folders.map { GroupFilesFolder(it.folderId, it.name) }

    fun entryKey(entry: GroupFileEntry): String =
        entry.fileId.ifBlank { entry.folderId.ifBlank { entry.name } }

    /** 首次进入：查询管理权限并加载根目录。 */
    fun start() {
        // 先刷新列表（尽快出内容），再异步查管理权限；权限查询慢也不影响首屏。
        refresh()
        scope.launch {
            canManage = withContext(Dispatchers.IO) {
                runCatching { source?.canManageGroupFiles(chatId) ?: false }.getOrDefault(false)
            }
        }
    }

    fun refresh() {
        loadPage(append = false)
    }

    /** 操作成功后刷新：立即刷一次，再短补偿 600ms 补一次（服务器/内核状态可能稍有延迟；原 1200ms）。 */
    private fun refreshAfterAction() {
        refresh()
        scope.launch {
            delay(600)
            refresh()
        }
    }

    fun loadMore() {
        if (loading || loadingMore || isEnd || !sourceAvailable) return
        loadPage(append = true)
    }

    private fun loadPage(append: Boolean) {
        val src = source ?: run {
            loading = false
            error = null
            return
        }
        val currentRequest = ++requestId
        if (append) loadingMore = true else loading = true
        if (!append) error = null
        val folderId = currentFolderId
        val startIndex = if (append) nextIndex else 0
        scope.launch {
            val page = withContext(Dispatchers.IO) {
                runCatching {
                    src.listGroupFiles(chatId, folderId, startIndex, PAGE_SIZE)
                }.getOrNull()
            }
            if (currentRequest != requestId) return@launch
            // 目录已经切换（理论上 requestId 已挡住，这里再做一次防御）：丢弃旧目录的结果。
            if (folderId != currentFolderId) return@launch
            if (page == null) {
                if (!append) {
                    entries.clear()
                    error = context.getString(R.string.group_files_load_failed)
                }
            } else {
                Log.i(
                    "Material3Tim",
                    "groupFile: folder loaded folderId=${folderId.ifBlank { "/" }} " +
                        "entries=${page.entries.size} isEnd=${page.isEnd}",
                )
                val merged = if (append) entries + page.entries else page.entries
                entries.clear()
                entries.addAll(
                    merged.distinctBy { it.fileId.ifBlank { it.folderId.ifBlank { it.name } } }
                        .sortedWith(
                            compareByDescending<GroupFileEntry> { it.isFolder }
                                .thenByDescending { it.uploadTime }
                                .thenBy { it.name },
                        ),
                )
                isEnd = page.isEnd
                nextIndex = page.nextIndex
                allFileCount = page.allFileCount
                error = page.error
            }
            loading = false
            loadingMore = false
        }
    }

    fun openFolder(entry: GroupFileEntry) {
        if (!entry.isFolder) return
        // 加载中/连续快速点击时忽略：否则会叠出 A/A/A 目录栈。
        if (loading || loadingMore) return
        Log.i(
            "Material3Tim",
            "groupFile: openFolder name=${entry.name} folderId=${entry.folderId} " +
                "parent=${entry.parentFolderId} fileCount=${entry.fileCount}",
        )
        // 立即清空旧目录内容并进入 loading，避免打开空目录时短暂显示父目录文件。
        entries.clear()
        nextIndex = 0
        isEnd = true
        allFileCount = 0
        pathStack = pathStack + GroupFilesFolder(entry.folderId, entry.name)
        exitManageMode()
        refresh()
    }

    fun goUp() {
        if (!canGoUp) return
        if (loading || loadingMore) return
        entries.clear()
        nextIndex = 0
        isEnd = true
        allFileCount = 0
        pathStack = pathStack.dropLast(1)
        exitManageMode()
        refresh()
    }

    // ------------------------------------------------------------------
    // 搜索
    // ------------------------------------------------------------------

    fun openSearch() {
        searchMode = true
        searchQuery = ""
        searchResults = emptyList()
        searchError = null
        searchLoading = false
    }

    fun closeSearch() {
        searchJob?.cancel()
        searchMode = false
        searchQuery = ""
        searchResults = emptyList()
        searchError = null
        searchLoading = false
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        searchMode = query.isNotBlank()
        searchJob?.cancel()
        if (query.isBlank()) {
            searchResults = emptyList()
            searchError = null
            searchLoading = false
            return
        }
        val src = source
        if (src == null) {
            searchResults = emptyList()
            searchError = context.getString(R.string.group_files_host_only)
            searchLoading = false
            return
        }
        searchLoading = true
        searchError = null
        val keyword = query.trim()
        searchJob = scope.launch {
            delay(300) // 防抖：连续输入只在停止后查一次
            val results = withContext(Dispatchers.IO) {
                runCatching { src.searchGroupFiles(chatId, keyword) }.getOrNull()
            }
            // 查询期间用户又改了关键字/关闭搜索：丢弃旧结果。
            if (searchQuery.trim() != keyword || !searchMode) return@launch
            searchResults = results.orEmpty()
            searchError = if (results == null) {
                context.getString(R.string.group_files_search_failed)
            } else {
                null
            }
            searchLoading = false
        }
    }

    fun toggleManageMode() {
        manageMode = !manageMode
        if (!manageMode) selectedKeys = emptySet()
    }

    private fun exitManageMode() {
        manageMode = false
        selectedKeys = emptySet()
    }

    fun toggleSelect(entry: GroupFileEntry) {
        val key = entryKey(entry)
        selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
    }

    fun isSelected(entry: GroupFileEntry): Boolean = entryKey(entry) in selectedKeys

    fun download(entry: GroupFileEntry) {
        val src = source ?: return
        val fileId = entry.fileId
        if (fileId.isBlank()) return
        val current = downloads[fileId]
        if (current != null && !current.done && !current.failed) return
        downloads[fileId] = GroupFileDownload(fileId = fileId, progress = 0)
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    src.downloadGroupFile(chatId, entry) { progress ->
                        // 回调可能来自其它线程：切回主线程更新 Compose 状态。
                        scope.launch {
                            downloads[progress.fileId.ifBlank { fileId }] = progress
                        }
                    }
                }.getOrNull()
            }
            if (result == null) {
                downloads[fileId] = GroupFileDownload(
                    fileId = fileId,
                    failed = true,
                    message = context.getString(R.string.group_files_download_failed),
                )
                toast(context.getString(R.string.group_files_download_failed))
                return@launch
            }
            downloads[result.fileId.ifBlank { fileId }] = result
            if (result.done && !result.path.isNullOrBlank()) {
                val path = result.path
                val index = entries.indexOfFirst { it.fileId == fileId }
                if (index >= 0) {
                    entries[index] = entries[index].copy(localPath = path)
                }
                toast(context.getString(R.string.group_files_download_done))
            } else {
                toast(result.message ?: context.getString(R.string.group_files_download_failed))
            }
        }
    }

    fun openFile(entry: GroupFileEntry) {
        val src = source ?: return
        val path = entry.localPath
            ?: downloads[entry.fileId]?.takeIf { it.done }?.path
            ?: return
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching { src.openGroupFile(path) }.getOrDefault(false)
            }
            if (!ok) toast(context.getString(R.string.group_files_open_failed))
        }
    }

    fun upload(uri: Uri) {
        val src = source ?: return
        scope.launch {
            busy = true
            val cached = withContext(Dispatchers.IO) { copyPickedFileToCache(context, uri) }
            if (cached == null) {
                busy = false
                toast(context.getString(R.string.group_files_upload_failed))
                return@launch
            }
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    src.uploadGroupFile(chatId, currentFolderId, cached.path, cached.name)
                }.getOrDefault(false)
            }
            busy = false
            if (ok) {
                toast(context.getString(R.string.group_files_upload_success))
                refreshAfterAction()
            } else {
                toast(context.getString(R.string.group_files_upload_failed))
            }
        }
    }

    fun createFolder(name: String) {
        val src = source ?: return
        val folderName = name.trim()
        if (folderName.isEmpty()) return
        dialog = null
        scope.launch {
            busy = true
            val ok = withContext(Dispatchers.IO) {
                runCatching { src.createGroupFolder(chatId, folderName) }.getOrDefault(false)
            }
            busy = false
            if (ok) {
                toast(context.getString(R.string.group_files_action_success))
                refreshAfterAction()
            } else {
                toast(context.getString(R.string.group_files_action_failed))
            }
        }
    }

    fun rename(entry: GroupFileEntry, newName: String) {
        val src = source ?: return
        val name = newName.trim()
        if (name.isEmpty()) return
        dialog = null
        scope.launch {
            busy = true
            val ok = withContext(Dispatchers.IO) {
                runCatching { src.renameGroupFile(chatId, entry, name) }.getOrDefault(false)
            }
            busy = false
            if (ok) {
                toast(context.getString(R.string.group_files_action_success))
                selectedKeys = emptySet()
                refreshAfterAction()
            } else {
                toast(context.getString(R.string.group_files_action_failed))
            }
        }
    }

    fun move(entriesToMove: List<GroupFileEntry>, destFolderId: String) {
        val src = source ?: return
        if (entriesToMove.isEmpty()) return
        dialog = null
        scope.launch {
            busy = true
            val ok = withContext(Dispatchers.IO) {
                runCatching { src.moveGroupFiles(chatId, entriesToMove, destFolderId) }
                    .getOrDefault(false)
            }
            busy = false
            if (ok) {
                toast(context.getString(R.string.group_files_action_success))
                selectedKeys = emptySet()
                refreshAfterAction()
            } else {
                toast(context.getString(R.string.group_files_action_failed))
            }
        }
    }

    fun delete(entriesToDelete: List<GroupFileEntry>) {
        val src = source ?: return
        if (entriesToDelete.isEmpty()) return
        dialog = null
        scope.launch {
            busy = true
            val ok = withContext(Dispatchers.IO) {
                runCatching { src.deleteGroupFiles(chatId, entriesToDelete) }
                    .getOrDefault(false)
            }
            busy = false
            if (ok) {
                toast(context.getString(R.string.group_files_action_success))
                selectedKeys = emptySet()
                refreshAfterAction()
            } else {
                toast(context.getString(R.string.group_files_action_failed))
            }
        }
    }

    fun showCreateFolder() {
        dialog = GroupFilesDialog.CreateFolder
    }

    fun showRename() {
        val entry = selectedEntries.singleOrNull() ?: return
        dialog = GroupFilesDialog.Rename(entry)
    }

    fun showMove() {
        val targets = selectedEntries.filterNot { it.isFolder }
        if (targets.isEmpty()) return
        dialog = GroupFilesDialog.Move(targets)
    }

    fun showDelete() {
        val targets = selectedEntries
        if (targets.isEmpty()) return
        dialog = GroupFilesDialog.Delete(targets)
    }

    fun dismissDialog() {
        dialog = null
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}

/**
 * 群文件页入口：按 [LocalUiMode] 分发 M3E / Miuix 两套实现，共享同一个 [GroupFilesState]。
 */
@Composable
fun GroupFilesScreen(
    chatId: String,
    initialFolderId: String = "",
    initialFolderName: String = "",
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val rootLabel = androidx.compose.ui.res.stringResource(R.string.group_files_all_files)
    val state = remember(chatId, initialFolderId) {
        GroupFilesState(
            context = context,
            chatId = chatId,
            scope = scope,
            rootLabel = rootLabel,
            initialFolderId = initialFolderId,
            initialFolderName = initialFolderName,
        )
    }
    val uploadPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(state::upload)
    }
    LaunchedEffect(chatId, initialFolderId) {
        state.start()
    }
    // 每个文件夹是独立页面：返回/上滑直接 pop 回上一级（根目录再 pop 回聊天页）。
    val onBack: () -> Unit = { navigator.pop() }
    val onOpenFolder: (GroupFileEntry) -> Unit = { entry ->
        if (entry.isFolder) {
            navigator.push(Route.ChatFiles(chatId, entry.folderId, entry.name))
        }
    }
    val onPickUpload: () -> Unit = {
        uploadPicker.launch(arrayOf("*/*"))
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> GroupFilesMiuix(
            state = state,
            onBack = onBack,
            onRefresh = state::refresh,
            onPickUpload = onPickUpload,
            onOpenFolder = onOpenFolder,
        )
        UiMode.Material -> GroupFilesMaterial(
            state = state,
            onBack = onBack,
            onRefresh = state::refresh,
            onPickUpload = onPickUpload,
            onOpenFolder = onOpenFolder,
        )
    }
}

/** 文件类型 → 图标（两套 UI 共用；Miuix 侧也直接用 Material ImageVector）。 */
internal fun groupFileKindIcon(entry: GroupFileEntry): ImageVector {
    if (entry.isFolder) return Icons.Filled.Folder
    val ext = entry.name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> Icons.Rounded.PictureAsPdf
        "zip", "rar", "7z", "tar", "gz" -> Icons.Rounded.FolderZip
        "png", "jpg", "jpeg", "gif", "webp", "bmp", "heic" -> Icons.Rounded.Image
        "mp4", "mov", "mkv", "avi", "webm", "3gp", "ts" -> Icons.Rounded.VideoFile
        "apk" -> Icons.Rounded.Android
        "doc", "docx", "wps", "txt", "md" -> Icons.Rounded.Description
        "xls", "xlsx", "csv", "et" -> Icons.Rounded.TableChart
        "ppt", "pptx", "dps" -> Icons.Rounded.Slideshow
        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
    }
}

/** 文件大小文本（B/KB/MB/GB）。 */
internal fun groupFileSizeText(bytes: Long): String {
    if (bytes <= 0L) return ""
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale.getDefault(), "%.1f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.getDefault(), "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.getDefault(), "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

/** 上传时间文本（秒级时间戳 → MM-dd HH:mm；0 表示未知）。 */
internal fun groupFileTimeText(seconds: Long): String {
    if (seconds <= 0L) return ""
    val millis = if (seconds > 10_000_000_000L) seconds else seconds * 1000L
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
}

/** 文件行副标题：大小 · 上传者 · 时间。 */
internal fun groupFileMetaText(entry: GroupFileEntry): String = buildList {
    groupFileSizeText(entry.fileSize).takeIf { it.isNotBlank() }?.let(::add)
    entry.uploaderName.takeIf { it.isNotBlank() }?.let(::add)
    groupFileTimeText(entry.uploadTime).takeIf { it.isNotBlank() }?.let(::add)
}.joinToString(" · ")

/** 系统选择器选中的文件复制到 cache（保留显示名）；失败返回 null。 */
private fun copyPickedFileToCache(context: Context, uri: Uri): GroupFilesUpload? {
    val displayName = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "upload_${System.currentTimeMillis()}"
    val safeName = displayName.replace('/', '_').replace('\\', '_')
    val dir = File(
        context.cacheDir,
        "group_files/${System.currentTimeMillis()}_${System.nanoTime()}",
    ).apply { mkdirs() }
    val target = File(dir, safeName)
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return@runCatching null
        if (!target.isFile || target.length() <= 0L) return@runCatching null
        GroupFilesUpload(path = target.absolutePath, name = safeName)
    }.getOrNull()
}
