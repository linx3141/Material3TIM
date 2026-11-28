package com.material3tim.data.model

import androidx.compose.runtime.Immutable

/** 消息页聊天卡片摘要前的提醒标签（TIM 风格，如 [有人@我] / [有新文件]）。 */
enum class ChatPreviewTag {
    /** [有人@我] */
    AtMe,
    /** [@全体成员] */
    AtAll,
    /** [有新文件] */
    NewFile,
    /** [新的作业] */
    NewHomework,
    /** [群公告] */
    GroupNotice,
    /** [@我的红包] */
    MyRedPacket,
    /** [@我的礼物] */
    MyGift,
    /** [QQ红包] */
    QQRedPacket,
    /** [有人回复我] */
    ReplyMe,
    /** [特别关心] */
    SpecialCare,
}

/**
 * 一条聊天会话（消息页列表项）。第一阶段使用实例数据填充。
 */
@Immutable
data class ChatItem(
    val id: String,
    /** 会话名称：好友昵称 / 群名称 / 服务号名称 */
    val name: String,
    /** 最后一条消息摘要 */
    val lastMessage: String,
    /** 最后消息时间的显示文本，例如 “14:32” / “昨天” / “3月5日” */
    val timeLabel: String,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isGroup: Boolean = false,
    /** 头像渐变配色的种子，0..n */
    val avatarSeed: Int = 0,
    /** 群成员数（仅群聊显示） */
    val memberCount: Int = 0,
    /** 在线状态（好友头像右下角的绿点） */
    val isOnline: Boolean = false,
    /** 草稿内容（显示在摘要前） */
    val draft: String? = null,
    /** 真实头像本地路径（TIM 进程里由 Kernel 提供，独立 App 为 null → 用首字渐变） */
    val avatarPath: String? = null,
    /** 摘要前的提醒标签（@我 / 有新文件 / 群公告等，仅未读时由数据层给出） */
    val previewTag: ChatPreviewTag? = null,
    /**
     * 「我的手机 / 我的电脑 / 我的平板」这类 DataLine 设备会话：0=普通会话，
     * 1=手机，2=电脑，3=平板；UI 据此显示设备图标/名称，而不是「QQ用户(0)」。
     */
    val dataLineKind: Int = 0,
    /**
     * 该会话所有 msgBox 事件消息的 seq（如 [有新文件]/[有人回复我]/[群公告] 等，可能多条）。
     * 聊天页「向上箭头」按钮据此与 @我 消息合并，从新到旧逐条跳转。
     */
    val previewEventSeqs: List<Long> = emptyList(),
) {
    val displaySummary: String
        get() = if (draft != null) "[草稿] $draft" else lastMessage

    /** 排序权重：置顶优先，其次按未读、时间（示例数据里时间顺序即列表顺序） */
    val sortKey: Int
        get() = if (isPinned) 0 else 1
}

/** 联系人列表项。 */
@Immutable
data class ContactItem(
    val id: String,
    val name: String,
    /** 首字母（A-Z / #），用于分组 */
    val letter: Char,
    val signature: String = "",
    val avatarSeed: Int = 0,
    val isOnline: Boolean = false,
    val isVip: Boolean = false,
    /** 真实头像本地路径 */
    val avatarPath: String? = null,
)
