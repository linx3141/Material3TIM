package com.material3tim.ui.screen.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.material3tim.R
import com.material3tim.data.model.FileItem
import com.material3tim.data.model.FileKind
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator

/**
 * 「我的文件」页：按 [LocalUiMode] 分发 Material / Miuix 两种界面风格。
 */
@Composable
fun FilesScreen() {
    val navigator = LocalNavigator.current
    val native = com.material3tim.data.source.AppDataSources.personalLibrary
    if (native != null) {
        OpenNativeLibraryPage(onOpen = native::openFiles, onReturn = { navigator.pop() })
        return
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> FilesMiuix(onBack = { navigator.pop() })
        UiMode.Material -> FilesMaterial(onBack = { navigator.pop() })
    }
}

/** 文件时间分组：今天 / 昨天 / 更早。 */
internal enum class FileSection(val labelRes: Int) {
    TODAY(R.string.files_today),
    YESTERDAY(R.string.files_yesterday),
    EARLIER(R.string.files_earlier),
}

/** 根据 [FileItem.timeLabel] 前缀判断所属分组。 */
internal fun fileSectionOf(timeLabel: String, todayLabel: String, yesterdayLabel: String): FileSection =
    when {
        timeLabel.startsWith(todayLabel) -> FileSection.TODAY
        timeLabel.startsWith(yesterdayLabel) -> FileSection.YESTERDAY
        else -> FileSection.EARLIER
    }

/** 按「今天 → 昨天 → 更早」分组，保持组内原有顺序并丢弃空分组。 */
internal fun groupFiles(
    files: List<FileItem>,
    todayLabel: String,
    yesterdayLabel: String,
): List<Pair<FileSection, List<FileItem>>> = FileSection.entries.mapNotNull { section ->
    val sectionFiles = files.filter { fileSectionOf(it.timeLabel, todayLabel, yesterdayLabel) == section }
    if (sectionFiles.isEmpty()) null else section to sectionFiles
}

/** 文件类型 → 图标（两种界面风格共用）。 */
internal fun fileKindIcon(kind: FileKind): ImageVector = when (kind) {
    FileKind.DOC -> Icons.Rounded.Description
    FileKind.PDF -> Icons.Rounded.PictureAsPdf
    FileKind.IMAGE -> Icons.Rounded.Image
    FileKind.VIDEO -> Icons.Rounded.VideoFile
    FileKind.ZIP -> Icons.Rounded.FolderZip
    FileKind.APK -> Icons.Rounded.Android
    FileKind.MUSIC -> Icons.Rounded.Audiotrack
    FileKind.OTHER -> Icons.Rounded.InsertDriveFile
}

/** 文件行副标题信息：大小 · 时间 · 来源。 */
internal fun fileMeta(file: FileItem): String = buildList {
    if (file.size.isNotBlank()) add(file.size)
    if (file.timeLabel.isNotBlank()) add(file.timeLabel)
    if (file.from.isNotBlank()) add(file.from)
}.joinToString(" · ")
