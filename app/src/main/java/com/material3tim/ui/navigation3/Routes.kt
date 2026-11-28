package com.material3tim.ui.navigation3

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * Type-safe navigation keys for miuix-nav.
 * Each destination is a NavKey (data object/data class) and can be saved/restored in the back stack.
 */
@Serializable
sealed interface Route : NavKey, Parcelable {
    @Parcelize
    @Serializable
    data object Main : Route

    // ===================== 消息 =====================
    @Parcelize
    @Serializable
    data class ChatDetail(
        val chatId: String,
        /** 从聊天记录搜索点进来时：要定位的消息 seq / msgId（0 表示普通打开）。 */
        val jumpToSeq: Long = 0L,
        val jumpToMsgId: Long = 0L,
    ) : Route

    @Parcelize
    @Serializable
    data class ChatSearch(val chatId: String) : Route

    /** 全局搜索页（消息 / 联系人 / 群聊）。 */
    @Parcelize
    @Serializable
    data object GlobalSearch : Route

    @Parcelize
    @Serializable
    data object AddFriend : Route

    /** 加好友验证页（从搜索结果点进来，填验证消息后发送请求）。 */
    @Parcelize
    @Serializable
    data class AddFriendVerify(
        val uid: String = "",
        val uin: Long = 0L,
        val name: String = "",
        val detail: String = "",
        val phone: String = "",
        val token: String = "",
    ) : Route

    /** 扫一扫：调系统相机拍照后识别二维码并跳转。 */
    @Parcelize
    @Serializable
    data object ScanQr : Route

    @Parcelize
    @Serializable
    data object CreateGroup : Route

    @Parcelize
    @Serializable
    data class ChatInfo(val chatId: String) : Route

    @Parcelize
    @Serializable
    data class ChatMembers(val chatId: String) : Route

    /** 群文件页（文件/文件夹列表、下载、管理）。 */
    @Parcelize
    @Serializable
    data class ChatFiles(
        val chatId: String,
        /** 当前打开的文件夹 id（空=根目录）。每个文件夹是一层独立页面，返回手势自然回到上一级。 */
        val folderId: String = "",
        val folderName: String = "",
    ) : Route

    @Parcelize
    @Serializable
    data class ChatNotice(val chatId: String) : Route

    @Parcelize
    @Serializable
    data class ChatNoticeDetail(val chatId: String, val noticeId: String) : Route

    /** 群助手二级页（消息列表群助手卡片进入，展示收进群助手的群列表）。 */
    @Parcelize
    @Serializable
    data object GroupAssistant : Route

    @Parcelize
    @Serializable
    data class ChatImage(
        val imagePath: String,
        /** 会话 id（宿主取原图用；空=示例/无原图回退）。 */
    /** 转发流程里的群助手：先选收进群助手的群，再把消息转发到所选群。 */
    @Parcelize
    @Serializable
    data class ForwardGroupAssistant(
        val srcChatId: String,
        val msgIds: List<Long>,
        val preview: String = "",
    ) : Route

        val chatId: String = "",
        /** 消息内核 msgId（0=未知/示例数据）。 */
        val msgId: Long = 0L,
        /** 图片元素 elementId（0=未知/示例数据）。 */
        val elementId: Long = 0L,
    ) : Route

    @Parcelize
    @Serializable
    data class ChatVideo(
        val videoPath: String,
        val thumbnailPath: String = "",
        val chatId: String = "",
        val msgId: Long = 0L,
        val elementId: Long = 0L,
        val videoWidth: Int = 0,
        val videoHeight: Int = 0,
    ) : Route

    @Parcelize
    @Serializable
    data class Forward(
        val srcChatId: String,
        val msgIds: List<Long>,
        /** 来源消息摘要（单条转发时展示在转发页顶部预览卡）；多选转发为空，仅显示条数。 */
        val preview: String = "",
    ) : Route

    @Parcelize
    @Serializable
    data class ChatRecord(
        val chatId: String,
        val msgId: Long,
        val elementId: Long,
        /** 嵌套记录的父记录 msgId（0=未知/顶层记录）。 */
        val parentMsgId: Long = 0L,
        /** 最外层根记录 msgId（0=未知/顶层记录；嵌套时用于内核 getMultiMsg 组合）。 */
        val rootMsgId: Long = 0L,
    ) : Route

    // ===================== 联系人 =====================
    @Parcelize
    @Serializable
    data class ContactDetail(
        val contactId: String,
        val chatId: String = "",
        val uid: String = "",
        val uin: String = "",
    ) : Route

    @Parcelize
    @Serializable
    data object NewFriends : Route

    @Parcelize
    @Serializable
    data object GroupChats : Route

    @Parcelize
    @Serializable
    data object Tags : Route

    @Parcelize
    @Serializable
    data object OfficialAccounts : Route

    // ===================== 我的 =====================
    @Parcelize
    @Serializable
    data object EditProfile : Route

    @Parcelize
    @Serializable
    data object Favorites : Route

    @Parcelize
    @Serializable
    data object Files : Route

    @Parcelize
    @Serializable
    data object Wallet : Route

    @Parcelize
    @Serializable
    data object Services : Route

    // ===================== 设置 =====================
    @Parcelize
    @Serializable
    data object Settings : Route

    @Parcelize
    @Serializable
    data object SupportDevelopment : Route

    @Parcelize
    @Serializable
    data object ColorPalette : Route

    @Parcelize
    @Serializable
    data object About : Route
}
