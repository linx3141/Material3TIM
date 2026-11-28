package com.material3tim.data.model

import androidx.compose.runtime.Immutable

/** 消息内容片段类型（多元素混排消息用）。 */
enum class ChatPartKind { TEXT, IMAGE }

/** 一条消息内的内容片段（用于「文本+表情+图片」等多元素混排消息，按消息内顺序排列）。 */
@Immutable
data class ChatPart(
    val kind: ChatPartKind,
    /** TEXT 片段的文本（FACE/MARKET_FACE 已合成为「[表情]」）。 */
    val text: String = "",
    /** IMAGE 片段本地路径（可能为 null，等待下载）。 */
    val imagePath: String? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    /** 表情图片（PicElement.picSubType == 1）。 */
    val emojiImage: Boolean = false,
    /** 对应 MsgElement 的 elementId（图片下载/重新拉取用）。 */
    val elementId: Long = 0L,
    /** QQ 系统小黄脸：文本气泡内按行内小表情渲染（与文字同高）。 */
    val isSystemFace: Boolean = false,
    /** QQ 超级表情：文本气泡内按行内小表情渲染。 */
    val isSuperEmoji: Boolean = false,
    val superEmojiLottiePath: String? = null,
    val superEmojiImagePath: String? = null,
    /** 表情含义（contentDescription/无资源时的兜底文本）。 */
    val faceDescription: String = "",
)

/** 聊天消息类型。 */
enum class ChatMessageType {
    TEXT,
    IMAGE,
    FILE,
    VOICE,
    RED_PACKET,
    LINK,
    SYSTEM,
    /** 聊天记录（合并转发） */
    CHAT_RECORD,
    /** 视频消息 */
    VIDEO,
    /** ARK 卡片（小程序/结构化消息/分享卡片等） */
    ARK,
    /** 语音/视频通话记录（AVRecordElement，elementType=21） */
    CALL,
    /** QQ 超级表情（FaceElement 带 stickerType/packId，Lottie 动画大表情） */
    SUPER_EMOJI,
    /** QQ 系统小黄脸（FaceElement 普通静态表情，渲染为小尺寸图片） */
    SYS_FACE,
}

/**
 * 一条聊天消息（示例数据）。
 * 第 1 步只做 UI，消息不落库、不发送。
 */
@Immutable
data class ChatMessage(
    val id: String,
    val chatId: String,
    val senderName: String,
    val senderSeed: Int,
    val isMe: Boolean = false,
    /** 这条消息是否 @ 了我（QQ NT MsgConstant.ATTYPEME=4；@全体不算） */
    val atMe: Boolean = false,
    val type: ChatMessageType = ChatMessageType.TEXT,
    val content: String = "",
    /** 气泡下方/时间分隔条上的时间文本 */
    val timeLabel: String = "",
    /** 对方是否已读（仅自己发出的消息） */
    val isRead: Boolean = true,
    /** 语音时长（秒） */
    val voiceSeconds: Int = 0,
    /** 文件大小文本，如 "2.4 MB" */
    val fileSize: String = "",
    /** 群聊中是否显示发送者昵称（连续同人消息可省略） */
    val showSenderName: Boolean = false,
    /** 发送者真实头像本地路径 */
    val senderAvatarPath: String? = null,
    /** 图片消息的本地文件路径（TIM 已缓存的图片） */
    val imagePath: String? = null,
    /** 图片原始宽高（用于气泡比例） */
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    /** 是否是「表情图片」（收藏表情/表情包图片，PicElement.picSubType == 1）：以表情尺寸渲染而不是大图气泡。 */
    val isEmojiImage: Boolean = false,
    /** 语音/文件的本地文件路径（后续点击播放/打开用） */
    val mediaPath: String? = null,
    /** 超级表情：Lottie JSON 动画本地路径（TIM 已下载时）。 */
    val superEmojiLottiePath: String? = null,
    /** 超级表情：静态兜底图（APNG/PNG）本地路径。 */
    val superEmojiImagePath: String? = null,
    /** 超级表情原始尺寸（用于按比例放大显示；0 表示未知）。 */
    val superEmojiWidth: Int = 0,
    val superEmojiHeight: Int = 0,
    /** 是否显示时间分隔条（例如 "今天 14:32"） */
    val timeSeparator: String? = null,
    /** 消息时间戳（毫秒，真实消息用；示例数据为 0）。加载更早消息后用它重排时间分隔条。 */
    val timestampMs: Long = 0,
    /** 聊天记录气泡：标题（如「群聊的聊天记录」） */
    val chatRecordTitle: String = "",
    /** 聊天记录气泡：预览行 */
    val chatRecordSummary: List<String> = emptyList(),
    /** 聊天记录气泡：被点击消息的 msgId */
    val chatRecordMsgId: Long = 0,
    /** 聊天记录气泡：合并转发元素 elementId */
    val chatRecordElementId: Long = 0,
    /** 聊天记录气泡：消息条数 */
    val chatRecordCount: Int = 0,
    /** 视频消息：时长（秒） */
    val videoSeconds: Int = 0,
    /** 视频消息：缩略图本地路径 */
    val videoThumbPath: String? = null,
    /** 视频消息：视频文件本地路径（可能未下载） */
    val videoPath: String? = null,
    /** 视频/富媒体消息对应的元素 id */
    val elementId: Long = 0,
    /** 发送者 uid（引用消息时需要带回源消息的 uid） */
    val senderUid: String = "",
    /** 发送者 QQ 号（数字字符串，可能为空；点开头像拉用户资料时用） */
    val senderUin: String = "",
    /** 引用消息：被引用消息的发送者昵称 */
    val replyToName: String = "",
    /** 引用消息：被引用消息的文本摘要 */
    val replyToContent: String = "",
    /** 引用消息：被引用消息的 msgId */
    val replyToMsgId: Long = 0L,
    /** 引用消息：被引用消息的 seq（msgId 缺失时用于从内核补拉/定位） */
    val replyToSeq: Long = 0L,
    /** 消息自身的 seq（用于按 seq 定位引用目标） */
    /** 通话记录：true=视频通话，false=语音通话 */
    val callVideo: Boolean = false,
    val seq: Long = 0L,
    /** 是否有引用（即使预览信息缺失也应该显示引用条） */
    val hasReply: Boolean = false,
    /** 引用消息：被引用消息发送者 uid */
    val replyToSenderUid: String = "",
    /** 文件下载进度（0..100；-1=未下载/未知）。仅文件消息有意义。 */
    val downloadProgress: Int = -1,
    /** FileElement.transferStatus（文件传输状态；-1=未知）。UI 可判断暂停/进行中/完成。 */
    val transferStatus: Int = -1,
    /** 多元素混排消息的片段（文本+表情+图片等）；空列表表示按单元素（type/content/imagePath）渲染。 */
    val parts: List<ChatPart> = emptyList(),
    /** ARK 卡片：来源应用 id（如 com.tencent.miniapp_01 / com.tencent.structmsg） */
    val arkApp: String = "",
    /** ARK 卡片：显示来源名（如「哔哩哔哩」「QQ小程序」），来自 JSON prompt/desc 推断 */
    val arkSource: String = "",
    /** ARK 卡片：标题 */
    val arkTitle: String = "",
    /** ARK 卡片：描述/摘要 */
    val arkDesc: String = "",
    /** ARK 卡片：标签（右上角小标签，可能为空） */
    val arkTag: String = "",
    /** ARK 卡片：预览图远程 URL（bridge 下载用；空表示无预览图） */
    val arkPreviewUrl: String = "",
    /** ARK 卡片：预览图本地缓存路径（远程 URL 由 bridge 下载） */
    val arkPreviewPath: String? = null,
    /** ARK 卡片：点击跳转 URL / schema（可能为空） */
    val arkJumpUrl: String = "",
    /** ARK 卡片：原始 prompt（如「[QQ小程序]哔哩哔哩」），可作页脚 */
    val arkPrompt: String = "",
    /** ARK 卡片：是否「群公告」卡片（UI 点击时应打开 Route.ChatNotice(chatId)，而不是外部链接） */
    val arkGroupNotice: Boolean = false,
    /** 红包：listid / billNo（打开/领取需要） */
    val redPacketBillNo: String = "",
    /** 红包：authkey（打开/领取需要） */
    val redPacketAuthkey: String = "",
    /** 红包：redChannel */
    val redPacketChannel: Int = 0,
    /** 红包：发送者 uin（打开/领取需要） */
    val redPacketSenderUin: Long = 0L,
    /** 红包：副标题/祝福语补充（WalletAio title/subTitle 等） */
    val redPacketSubtitle: String = "",
    /** 红包：领取状态（WalletElement.grabState；-1 未知） */
    val redPacketGrabState: Int = -1,
    /** 红包：自己已领取金额（grabState 表示已领取时有意义） */
    val redPacketGrabbedAmount: Long = 0L,
    /** 红包：TIM 本地红包状态文案（QWallet INewQWalletApi.getHbStateText，如「已领取」「已领完」） */
    val redPacketStateText: String = "",
    /** 内核 msgId（图片查看器取原图、按 msgId 回查等用；示例数据为 0）。 */
    val msgId: Long = 0L,
)

/** 聊天信息页的成员项。 */
@Immutable
data class ChatMember(
    val name: String,
    val avatarSeed: Int,
    val isOwner: Boolean = false,
    /** 群名片/昵称等显示名之外，成员的真实头像本地路径 */
    val avatarPath: String? = null,
    /** 成员 QQ 号（没有则为 uid） */
    val uin: String = "",
    /** 内核 uid（@ 元素发送用，如 u_xxx） */
    val uid: String = "",
    val isAdmin: Boolean = false,
    val isMe: Boolean = false,
)

/** 一条群公告（来自 TIM 的群公告 feeds 缓存，含历史公告）。 */
@Immutable
data class GroupNotice(
    val id: String,
    val title: String,
    val content: String,
    val timeMillis: Long = 0L,
)

/** 聊天信息页需要的附加数据。 */
@Immutable
data class ChatInfoData(
    val chat: ChatItem,
    val members: List<ChatMember> = emptyList(),
    val notice: String = "",
    /** 群号 / 对方 QQ 号 */
    val timId: String = "",
    /** 群简介 / 个性签名 */
    val description: String = "",
    /** 群成员人数（群资料里的准确人数） */
    val memberCount: Int = 0,
)

/** 群成员角色（统一用户资料页用；与 ChatMember.isOwner/isAdmin 对应）。 */
enum class GroupMemberRole { OWNER, ADMIN, MEMBER }

/**
 * 统一「用户资料」页数据：
 * - 从联系人页进来 = 好友资料（[isFriend]=true）；
 * - 从聊天头像进来 = 任意用户（群成员/私聊对方，可能非好友，非好友时 UI 显示「加好友」）；
 * - 群聊上下文里 [myRole] 是我的角色、[targetMember] 是目标成员（群名片/uin/角色）。
 *
 * 兼容旧「我的资料」页字段（name/timId/signature/avatarSeed/isOnline/avatarPath）；
 * 统一资料页请优先使用 [contact]/[isFriend]/[chatId]/[groupCode]/[myRole]/[targetMember]。
 */
@Immutable
data class UserProfile(
    val name: String = "",
    val timId: String = "",
    val signature: String = "",
    val avatarSeed: Int = 0,
    val isOnline: Boolean = true,
    val avatarPath: String? = null,
    val contact: ContactItem = ContactItem(id = "", name = "", letter = '#'),
    val isFriend: Boolean = false,
    val chatId: String = "",
    val groupCode: String = "",
    val myRole: GroupMemberRole? = null,
    val targetMember: ChatMember? = null,
)

/** 全局搜索：一条跨会话命中的消息。 */
@Immutable
data class GlobalSearchMessage(
    val chatId: String,
    val chatName: String,
    val isGroup: Boolean,
    val senderName: String,
    val msgId: Long,
    val msgSeq: Long,
    val preview: String,
    val timeLabel: String = "",
    /** 消息时间（毫秒；排序/分组用，UI 可忽略）。 */
    val timestampMs: Long = 0L,
)

/** 全局搜索结果（消息/联系人/群聊分组）。 */
@Immutable
data class GlobalSearchResult(
    val messages: List<GlobalSearchMessage> = emptyList(),
    val contacts: List<ContactItem> = emptyList(),
    val groups: List<ChatItem> = emptyList(),
)

/** 联系人详情页展示的数据。 */
@Immutable
data class ContactDetail(
    val contact: ContactItem,
    val timId: String,
    val remark: String = "",
    val phone: String = "",
    val email: String = "",
    val region: String = "",
    val tags: List<String> = emptyList(),
    val chatId: String,
)
