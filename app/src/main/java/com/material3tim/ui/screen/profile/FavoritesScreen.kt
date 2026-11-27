package com.material3tim.ui.screen.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.material3tim.R
import com.material3tim.data.model.FavoriteItem
import com.material3tim.data.model.FavoriteType
import com.material3tim.ui.LocalUiMode
import com.material3tim.ui.UiMode
import com.material3tim.ui.navigation3.LocalNavigator

/**
 * 「我的收藏」页：按 [LocalUiMode] 分发 Material / Miuix 两种界面风格。
 */
@Composable
fun FavoritesScreen() {
    val navigator = LocalNavigator.current
    val native = com.material3tim.data.source.AppDataSources.personalLibrary
    if (native != null) {
        OpenNativeLibraryPage(onOpen = native::openFavorites, onReturn = { navigator.pop() })
        return
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> FavoritesMiuix(onBack = { navigator.pop() })
        UiMode.Material -> FavoritesMaterial(onBack = { navigator.pop() })
    }
}

/** 收藏筛选条件：全部 + 五种收藏类型。 */
internal enum class FavoriteFilter(val labelRes: Int, val type: FavoriteType?) {
    ALL(R.string.favorites_all, null),
    TEXT(R.string.favorites_text, FavoriteType.TEXT),
    LINK(R.string.favorites_link, FavoriteType.LINK),
    IMAGE(R.string.favorites_image, FavoriteType.IMAGE),
    FILE(R.string.favorites_file, FavoriteType.FILE),
    VOICE(R.string.favorites_voice, FavoriteType.VOICE),
}

/** 收藏类型 → 图标（两种界面风格共用）。 */
internal fun favoriteTypeIcon(type: FavoriteType): ImageVector = when (type) {
    FavoriteType.TEXT -> Icons.Rounded.Notes
    FavoriteType.LINK -> Icons.Rounded.Link
    FavoriteType.IMAGE -> Icons.Rounded.Image
    FavoriteType.FILE -> Icons.Rounded.InsertDriveFile
    FavoriteType.VOICE -> Icons.Rounded.GraphicEq
}

/**
 * 收藏条目的副标题信息：来源 · 时间（· 文件大小 / 语音时长）。
 * [voiceLabel] 由调用方通过 `voice_duration` 资源格式化后传入。
 */
internal fun favoriteMeta(item: FavoriteItem, voiceLabel: String?): String = buildList {
    if (item.source.isNotBlank()) add(item.source)
    if (item.timeLabel.isNotBlank()) add(item.timeLabel)
    when (item.type) {
        FavoriteType.FILE -> if (item.fileSize.isNotBlank()) add(item.fileSize)
        FavoriteType.VOICE -> voiceLabel?.let { add(it) }
        else -> Unit
    }
}.joinToString(" · ")
