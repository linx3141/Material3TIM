package com.material3tim.data.source

import com.material3tim.data.model.ChatInfoData
import com.material3tim.data.model.GroupAssistantData
import com.material3tim.data.model.GroupNotice
import com.material3tim.data.model.ChatItem
import com.material3tim.data.model.ChatMessage
import com.material3tim.data.model.ChatMember
import com.material3tim.data.model.ContactDetail
import com.material3tim.data.model.ContactItem
import com.material3tim.data.model.AddFriendGroup
import com.material3tim.data.model.AddFriendSearchResult
import com.material3tim.data.model.AddFriendUser
import com.material3tim.data.model.AddFriendSettingInfo
import com.material3tim.data.model.CustomEmoji
import com.material3tim.data.model.GroupFileDownload
import com.material3tim.data.model.GroupFileEntry
import com.material3tim.data.model.FriendRequest
import com.material3tim.data.model.GlobalSearchResult
import com.material3tim.data.model.StickerPack
import com.material3tim.data.model.GroupFilePage
import com.material3tim.data.model.UserProfile

/**
 * 会话数据源。独立 App 里为 null（用示例数据）；
 * Xposed 宿主（TIM 进程）里由模块替换成真实数据源。
 */
interface ChatSource {
    /** 阻塞加载会话列表；失败返回 null。必须在后台线程调用。 */
    fun loadChats(): List<ChatItem>?

    /** 阻塞加载聊天信息页数据（群资料/成员列表/单聊资料）；失败返回 null。 */
    fun loadChatInfo(chatId: String): ChatInfoData? = null

    /** 清掉群资料/成员列表缓存（管理操作后强制重新拉取）；默认无操作。 */
    fun invalidateChatInfoCache(chatId: String) {}

    /**
     * 毫秒级快速路径：只读内存/本地缓存合成 ChatInfoData（不等待网络/SSO/内核列表），
     * 无缓存或非宿主返回 null。慢数据由后台刷新完成，通过 LiveEvents.dataUpdates 通知 UI。
     */
    fun loadChatInfoFast(chatId: String): ChatInfoData? = null

    /** 阻塞加载群公告历史（按时间倒序）；失败返回 null。 */
    fun loadGroupNotices(chatId: String): List<GroupNotice>? = null

    /**
     * 群助手（TIM「群助手」二级页数据）：收进群助手的群列表 + 是否显示列表入口卡片。
     * 独立 App 默认 null（安全）。
     */
    fun loadGroupAssistant(): GroupAssistantData? = null

    /**
     * @ 面板用：阻塞强制拉取群全量成员（内核 getAllMemberListV2，≤8s），成功后写成员缓存。
     * 独立 App 默认 null；失败 null。IO 线程调用。
     */
    fun loadGroupMembers(chatId: String): List<ChatMember>? = null
}

/** @ 目标：[isAll]=true 表示 @全体成员（[uid] 为空）。[display] 是插入到文本里的「@昵称 」片段。 */
data class AtTarget(val display: String, val uid: String, val isAll: Boolean = false)

/** 消息数据源：拉取会话消息 / 发送文本。宿主里由 TIM Kernel 实现。 */
/**
 * 群文件数据源：列表/下载/管理（上传、新建文件夹、重命名、移动、删除）。
 * 宿主里由 TIM 内核实现；独立 App 恒为 null / 默认失败。
 */
interface GroupFileSource {
    /** 当前账号在该群是否有管理权限（群主/管理员）；非群/查询失败返回 false。 */
    fun canManageGroupFiles(chatId: String): Boolean = false

    /** 拉取指定文件夹下的文件+子文件夹列表；[parentFolderId] 为空表示根目录。阻塞，IO 线程调用。 */
    fun listGroupFiles(
        chatId: String,
        parentFolderId: String,
        startIndex: Int = 0,
        count: Int = 50,
    ): GroupFilePage? = null

    /**
     * 搜索群文件（文件名关键字）；失败返回 null。阻塞，IO 线程调用。
     * 结果可能来自群的任意文件夹（[GroupFileEntry.parentFolderId] 为 ""）。
     */
    fun searchGroupFiles(
        chatId: String,
        keyword: String,
        count: Int = 50,
    ): List<com.material3tim.data.model.GroupFileEntry>? = null

    /**
     * 下载群文件；[onProgress] 会在下载过程中回调（可能在其他线程），完成后 [GroupFileDownload.done]=true。
     * 阻塞直到完成/失败，IO 线程调用。
     */
    fun downloadGroupFile(
        chatId: String,
        entry: GroupFileEntry,
        onProgress: (GroupFileDownload) -> Unit = {},
    ): GroupFileDownload? = null

    /** 上传本地文件到群文件（[parentFolderId] 为空=根目录）。阻塞，IO 线程调用；成功返回 true。 */
    fun uploadGroupFile(
        chatId: String,
        parentFolderId: String,
        localPath: String,
        fileName: String,
    ): Boolean = false

    /** 在群文件根目录新建文件夹；成功返回 true。 */
    fun createGroupFolder(chatId: String, folderName: String): Boolean = false

    /** 重命名文件/文件夹；成功返回 true。 */
    fun renameGroupFile(chatId: String, entry: GroupFileEntry, newName: String): Boolean = false

    /** 移动文件/文件夹到目标文件夹（[destFolderId] 为空=根目录）；成功返回 true。 */
    fun moveGroupFiles(
        chatId: String,
        entries: List<GroupFileEntry>,
        destFolderId: String,
    ): Boolean = false

    /** 删除文件/文件夹；成功返回 true。 */
    fun deleteGroupFiles(chatId: String, entries: List<GroupFileEntry>): Boolean = false

    /** 用系统应用打开已下载的本地文件。 */
    fun openGroupFile(path: String): Boolean = false
}

interface MessageSource {
    fun loadMessages(chatId: String): List<ChatMessage>?

    /**
     * 图片查看器用：取一条图片消息的**原图**本地路径（阻塞，IO 线程调用）。
     * 优先已缓存原图 → 内核原图下载 → originImageUrl HTTP 兜底；失败 null。
     */
    fun loadChatImageOriginal(chatId: String, msgId: Long, elementId: Long): String? = null

    /**
     * 用系统录音机录音并作为语音消息发送到 [chatId]：拉起 MediaStore.RECORD_SOUND，
    /**
     * 发送 QQ 超级表情（FaceElement 带 stickerType/packId/stickerId）；失败返回 false。
     * 阻塞，需在 IO 线程调用。
     */
    fun sendSuperEmoji(
        chatId: String,
        serverId: Int,
        desc: String,
        packId: String,
        stickerId: String,
        stickerType: Int = 1,
    ): Boolean = false

    /** 发送 QQ 系统小黄脸（普通 FaceElement）。阻塞，需在 IO 线程调用。 */
    fun sendSystemFace(chatId: String, serverId: Int, desc: String): Boolean = false

     * 结果经 TimUiHook.onActivityResult → 桥接复制/转码 → TIM 语音发送链路。
     * 返回 false 表示没有前台 Activity / 已有记录进行中（独立 App 默认 false）。
     */
    fun startSystemVoiceRecord(chatId: String): Boolean = false

    /** 加载更早的一页消息；返回累计已加载的更早消息（升序），null 表示失败。 */
    fun loadOlderMessages(chatId: String, count: Int = 40): List<ChatMessage>?

    /** 直接按 seq 取目标消息附近的一段历史（用于点击引用跳转到很早的消息）。 */
    fun loadHistoryAround(chatId: String, seq: Long, count: Int = 60): List<ChatMessage>? = null

    fun sendText(chatId: String, text: String): Boolean

    /** 删除一条消息；失败返回 false。 */
    fun deleteMessage(chatId: String, msgId: Long): Boolean = false

    /** 撤回一条消息（仅自己的消息、时限内）；成功返回 true。阻塞，需在 IO 线程调用。 */
    fun recallMessage(chatId: String, msgId: Long): Boolean = false

    /** 转发消息到若干会话；失败返回 false。 */
    fun forwardMessages(srcChatId: String, msgIds: List<Long>, dstChatIds: List<String>): Boolean = false

    /**
     * 给一条消息 +1（TIM 的点赞/表情回应）；成功返回 true。
     * 宿主里由 TIM 内核实现，独立 App 恒为 false（UI 提示需在宿主内使用）。
     */
    fun sendPlusOne(chatId: String, msgId: Long): Boolean = false

    /** 发送带引用的文本消息；默认退化为普通文本。 */
    fun sendTextWithReply(
        chatId: String,
        text: String,
        replyToMsgId: Long,
        replySenderUid: String,
        replySourceText: String,
    ): Boolean = sendText(chatId, text)

    /** 发送带 @ 的文本（可同时引用）；[atTargets] 为空时退化为普通/引用文本。阻塞，IO 线程调用。 */
    fun sendTextWithAt(
        chatId: String,
        text: String,
        atTargets: List<AtTarget>,
        replyToMsgId: Long = 0L,
        replySenderUid: String = "",
        replySourceText: String = "",
    ): Boolean = false

    /** 当前账号在该群是否群主或管理员（可撤回其他成员消息；非群/查询失败返回 false）。 */
    fun canRecallOthers(chatId: String): Boolean = false

    /** 发送本地图片（相册选择 / 拍照得到的文件）。 */
    fun sendImage(chatId: String, filePath: String, width: Int, height: Int): Boolean

    /** 发送本地文件（系统文件选择器得到的文件）。 */
    fun sendFile(chatId: String, filePath: String, fileName: String, fileSize: Long): Boolean

    /**
     * 发送本地视频（TIM 短消息视频，elementType=5）；[thumbPath] 为空时由桥层截首帧生成；
     * [durationMs] 毫秒、[width]/[height] 为视频/缩略图尺寸。成功返回 true。阻塞，IO 线程调用。
     */
    fun sendVideo(
        chatId: String,
        filePath: String,
        thumbPath: String?,
        durationMs: Long,
        width: Int,
        height: Int,
    ): Boolean = false

    /**
     * 发送用户收藏的表情（以「表情」形态发送，QQ 渲染为表情而不是普通图片）。
     * 宿主里由 TIM 内核实现；独立 App 恒为 false。
     */
    fun sendCustomEmoji(chatId: String, filePath: String): Boolean = false

    /**
     * 打开 TIM 原生红包领取/详情：按消息 id 取 MsgRecord，走 TIM 自己的 GrabHbViewModel
     * （含 pskey + hb_pre_grap 预领取），这是和原生 AIO 点击完全一致的路径。
     */
    fun openRedPacketMsg(chatId: String, msgId: Long): Boolean = false

    /**
     * 打开 TIM 原生红包领取/详情（构造 PayBridgeActivity 的 graphb 请求）。
     * 字段来自 WalletElement：billNo/authkey/channel/senderUin/grabState。成功启动返回 true。
     */
    fun openRedPacket(
        chatId: String,
        billNo: String,
        authkey: String,
        channel: Int,
        senderUin: Long,
        grabState: Int,
    ): Boolean = false

    /** 加载一条「聊天记录（合并转发）」里的消息；失败返回 null。 */
    fun loadChatRecord(chatId: String, msgId: Long, elementId: Long): List<ChatMessage>? = null

    /**
     * 合并转发聊天记录（含嵌套记录）：parentMsgId = 外层记录消息 msgId（0=未知）。
     * 默认实现委托 3 参版本，独立 App/旧实现安全。
     */
    fun loadChatRecord(
        chatId: String,
        msgId: Long,
        elementId: Long,
        parentMsgId: Long = 0L,
    ): List<ChatMessage>? = loadChatRecord(chatId, msgId, elementId)

    /**
     * 合并转发聊天记录（更深嵌套）：parentMsgId = 直接外层记录 msgId，rootMsgId = 最外层记录 msgId。
     * 默认实现委托 4 参版本。
     */
    fun loadChatRecord(
        chatId: String,
        msgId: Long,
        elementId: Long,
        parentMsgId: Long,
        rootMsgId: Long,
    ): List<ChatMessage>? = loadChatRecord(chatId, msgId, elementId, parentMsgId)

    /** 获取视频在线播放地址（未下载时使用）；失败返回 null。 */
    fun videoPlayUrl(chatId: String, msgId: Long, elementId: Long): String? = null

    /**
     * 下载并打开文件消息（未下载时先触发内核 richmedia 下载再交给系统应用打开）；阻塞，需在 IO 线程调用。
     * [knownPath] 为消息里已带出的本地路径（可空）。
     */
    fun downloadAndOpenFile(
        chatId: String,
        msgId: Long,
        elementId: Long,
        knownPath: String?,
    ): Boolean = false

    /** 用系统应用通过 TIM FileProvider content:// 打开本地文件；成功拉起返回 true。 */
    fun openLocalFile(path: String): Boolean = false

    /** 打开 ARK 卡片（TIM JumpActivity / JumpAction 分发；http(s) 兜底 ACTION_VIEW）；已拉起返回 true。 */
    fun openArkUrl(url: String): Boolean = false

    /** 触发文件下载（非阻塞，仅发内核 richmedia 请求）；返回是否已发出。 */
    fun startFileDownload(chatId: String, msgId: Long, elementId: Long): Boolean = false

    /** 查询文件下载进度：0..100，-1=未知（未下载/无进度）。已下载完成返回 100。 */
    fun fileDownloadProgress(chatId: String, msgId: Long, elementId: Long): Int = -1

    /** 已下载文件本地路径（可直接打开）；未完成/未知返回 null。 */
    fun downloadedFilePath(chatId: String, msgId: Long, elementId: Long): String? = null
}

/** 联系人数据源：好友列表 + 联系人详情。 */
interface ContactSource {
    fun loadContacts(): List<ContactItem>?

    fun loadContactDetail(contactId: String): ContactDetail?

    /**
     * 按聊天上下文加载任意用户资料（群聊成员/私聊对方，含非好友）；[uid]/[uin] 二选一可用。
     * 返回 [UserProfile]（好友关系、群角色、群成员信息）；失败返回 null。阻塞，需在 IO 线程调用。
     */
    fun loadUserProfile(chatId: String, uid: String, uin: String): UserProfile? = null
}

/** 好友/会话管理 + 群资料编辑数据源（失败返回 false/null，不抛出）。 */
interface FriendManageSource {
    /** 清空某会话的全部聊天记录（内核 clearMsgRecords）。阻塞，IO 线程调用。 */
    fun clearChatHistory(chatId: String): Boolean = false

    /** 删除好友；[uid] 为空时尝试从 chatId 解析。 */
    fun deleteFriend(chatId: String, uid: String): Boolean = false

    /** 好友申请列表（「新的朋友」）；失败返回 null。阻塞，IO 线程调用。 */
    fun loadFriendRequests(): List<FriendRequest>? = null

    /** 同意/拒绝好友申请；[uin] 可空（部分接口只需 uid）。 */
    fun approveFriendRequest(uid: String, uin: String, accept: Boolean): Boolean = false

    /** 设置好友备注（空字符串=清除）。 */
    fun setFriendRemark(uid: String, remark: String): Boolean = false

    /** 修改群简介（kernel modifyGroupDetailInfoV2，失败回退旧版 OIDB 0x89a_0）。 */
    fun setGroupDescription(chatId: String, description: String): Boolean = false

    /** 打开 TIM 原生群资料/群头像编辑入口；失败返回 false。 */
    fun openGroupAvatarEditor(chatId: String): Boolean = false
}

/** 语音/视频通话数据源：拉起 TIM 原版通话 UI。 */
interface CallSource {
    /** 发起语音通话；返回是否成功拉起。阻塞可短，IO/主线程均可（内部会切到宿主 Activity）。 */
    fun startVoiceCall(chatId: String): Boolean = false

    /** 发起视频通话。 */
    fun startVideoCall(chatId: String): Boolean = false
}

/** 全局搜索数据源（跨会话消息 + 联系人 + 群聊）。 */
interface SearchSource {
    /** 阻塞搜索；失败返回 null。必须在 IO 线程调用。 */
    fun searchAll(keyword: String, messageLimit: Int = 50): GlobalSearchResult? = null
}

/** 收藏表情数据源：用户「添加到表情」收集的单张表情图片。 */
interface EmojiSource {
    /** 阻塞加载收藏表情；失败返回 null。必须在后台线程调用。 */
    fun loadFavoriteEmojis(): List<CustomEmoji>?

    /**
     * 打开收藏表情页时调用：检查并触发 TIM 漫游收藏同步（30s 节流），返回同步后的最新列表。
     * 阻塞，IO 线程；失败 null。
     */
    fun refreshFavoriteEmojis(): List<CustomEmoji>? = loadFavoriteEmojis()

    /**
    /** 阻塞加载 QQ 超级表情列表；失败/宿主未就绪返回 null。必须在后台线程调用。 */
    fun loadSuperEmojis(): List<com.material3tim.data.model.SuperEmojiItem>? = null

    /** 阻塞加载 QQ 系统小黄脸列表（静态内置表情）；失败返回 null。必须在后台线程调用。 */
    fun loadSystemFaces(): List<com.material3tim.data.model.SystemFaceItem>? = null

     * 长按图片/表情消息「添加到表情」：按 elementId 找 PicElement/MarketFaceElement，
     * 必要时先下载原图，再走 TIM 收藏 API。成功（已触发/已添加）返回 true。
     */
    fun addMessageToFavorites(chatId: String, msgId: Long, elementId: Long): Boolean = false

    /** 真实表情包列表（TIM 已安装 tab 表情包）；不可用/失败返回 null。 */
    fun loadStickerPacks(): List<StickerPack>? = null

    /**
     * 某个表情包内的表情列表（阻塞 IO，默认 null；同包内存缓存）。
     * 返回 [CustomEmoji]：id=eId，imagePath 只给真实存在的本地文件（缺失时尝试 TIM 下载/HTTP 补齐）。
     */
    fun loadStickerPackEmojis(packId: String): List<CustomEmoji>? = null

    /**
     * 发送表情包内表情（epId + eId）：优先 MarketFace 贴纸消息（对方可见贴纸），
     * 失败回退为图片表情发送。阻塞，IO 线程；成功返回 true。
     */
    fun sendStickerPackEmoji(chatId: String, epId: String, eId: String): Boolean = false

    /** 把收藏表情移到收藏列表最前（TIM 漫游排序 + 本地 DB 顺序持久化）；阻塞 IO。 */
    fun moveFavoriteEmojiToTop(emojiId: String): Boolean = false

    /** 删除收藏表情（服务端漫游删除 + 本地 DB 删除）；阻塞 IO。 */
    fun deleteFavoriteEmoji(emojiId: String): Boolean = false
}

/** 会话设置 / 聊天记录搜索数据源（置顶、消息免打扰、查找聊天记录）。 */
interface ChatSettingSource {
    /** 设置会话置顶；成功返回 true。 */
    fun setChatPinned(chatId: String, pinned: Boolean): Boolean = false

    /** 设置会话消息免打扰；成功返回 true。 */
    fun setChatMuted(chatId: String, muted: Boolean): Boolean = false

    /** 设置会话未读状态（true=标为未读，false=标为已读）；成功返回 true。阻塞。 */
    fun setChatUnread(chatId: String, unread: Boolean): Boolean = false

    /** 删除会话（仅删除会话列表条目，不删除聊天记录）；成功返回 true。阻塞。 */
    fun deleteChat(chatId: String): Boolean = false

    /** 在指定会话里搜索聊天记录（关键字）；失败返回 null。阻塞，需在 IO 线程调用。 */
    fun searchChatMessages(chatId: String, keyword: String, limit: Int = 50): List<ChatMessage>? = null
}

/** 加好友/群：按 QQ 号 / 手机号 / 群号搜索，及加好友/加群/扫一扫入口。 */
/**
 * 群管理数据源：踢人 / 禁言 / 管理员 / 群名片 / 群名 / 群公告 / 退群 / 解散。
 * 宿主里由 TIM 内核 `IKernelGroupService` 实现；独立 App 恒为默认失败。
 */
interface GroupManageSource {
    /** 踢出群成员；[uids] 为内核 uid（ChatMember.uid，如 `u_xxx`）。 */
    fun kickGroupMembers(chatId: String, uids: List<String>): Boolean = false

    /** 设置/解除单成员禁言；[muted]=false 时忽略 [seconds]。成功返回 true。 */
    fun setGroupMemberMuted(chatId: String, uid: String, muted: Boolean, seconds: Int = 0): Boolean = false

    /** 设置/取消管理员。成功返回 true。 */
    fun setGroupMemberAdmin(chatId: String, uid: String, admin: Boolean): Boolean = false

    /** 修改成员群名片（[card] 为空表示清除）。成功返回 true。 */
    fun setGroupMemberCard(chatId: String, uid: String, card: String): Boolean = false

    /** 修改群名称；成功返回 true。 */
    fun setGroupName(chatId: String, name: String): Boolean = false

    /** 全员禁言开关；成功返回 true。 */
    fun setGroupAllMuted(chatId: String, muted: Boolean): Boolean = false

    /** 发布群公告（[content] 纯文本，[feedsId] 传已存在公告 id 表示编辑）；成功返回 true。 */
    fun publishGroupNotice(chatId: String, content: String, feedsId: String = ""): Boolean = false

    /** 主动退群；成功返回 true。 */
    fun quitGroup(chatId: String): Boolean = false

    /** 解散群（仅群主）；成功返回 true。 */
    fun destroyGroup(chatId: String): Boolean = false
}

interface AddFriendSource {
    /** 按关键字搜索个人账号与群聊；失败返回 null。阻塞，需在 IO 线程调用。 */
    fun search(keyword: String): AddFriendSearchResult?

    /** 查询目标用户的加好友设置（friend_setting + 验证问题）；失败返回 null。阻塞，需在 IO 线程调用。 */
    fun queryFriendSetting(user: AddFriendUser): AddFriendSettingInfo? = null

    /** 发送带验证消息的加好友请求（原生加好友验证流程）；成功返回 true。阻塞。 */
    fun requestAddFriend(user: AddFriendUser, message: String): Boolean = false

    /** requestAddFriend 失败后，服务端/风控返回的可读错误文案（可能为 null）。 */
    fun lastAddFriendError(): String? = null

    /** 申请加入群聊；成功返回 true。阻塞。 */
    fun joinGroup(group: AddFriendGroup): Boolean = false

}

/** 扫一扫结果处理：QQ 功能性二维码（mqqapi://、加好友/加群卡片等）交给 TIM 原生跳转。 */
interface QrActionSource {
    /**
     * 用 TIM 原生扫码结果分发（DispatchScanResultActivity）打开二维码内容；成功拉起返回 true。
     * 普通 http(s) 链接由 UI 层用 ExternalLink 打开，不需要走这里。
     */
    fun openQrContent(text: String): Boolean = false
}

/** 创建群聊：用选中的联系人创建群。 */
interface GroupCreateSource {
    /**
     * 创建群聊；[members] 为选中的联系人（ContactItem）。成功返回新群的 chatId（tim_2_<群号>），失败返回 null。
     * 阻塞，需在 IO 线程调用。
     */
    fun createGroup(members: List<com.material3tim.data.model.ContactItem>): String?
}

/** 语音播放数据源：交给宿主 TIM 的语音播放器（支持 silk/amr）。 */
interface VoicePlaybackSource {
    /** 播放本地语音文件；[offsetMs] 为起始偏移（毫秒）。成功开始播放返回 true。阻塞，需在 IO 线程调用。 */
    fun playVoice(path: String, offsetMs: Int = 0): Boolean = false

    /** 停止当前语音播放。 */
    fun stopVoice() {}

    /** 是否正在播放（UI 高亮/切换用）。 */
    fun isVoicePlaying(): Boolean = false

    /** 当前语音播放是否走扬声器（false=听筒，默认）。 */
    fun isSpeakerEnabled(): Boolean = false

    /** 切换语音播放扬声器/听筒（true=扬声器）；立即生效并用于后续语音。 */
    fun setSpeakerEnabled(enabled: Boolean): Boolean = false

    /** 当前播放位置（毫秒），未播放返回 0。 */
    fun playbackPositionMs(): Int = 0

    /** 当前语音总时长（毫秒），未播放返回 0。 */
    fun playbackDurationMs(): Int = 0

    /** 从指定毫秒位置重新播放当前语音；未在播放/无播放器返回 false。 */
    fun seekVoice(positionMs: Int): Boolean = false

    /** 暂停当前语音（保留播放位置）；成功返回 true。 */
    fun pauseVoice(): Boolean = false

    /** 从暂停位置继续播放；无暂停状态返回 false。 */
    fun resumeVoice(): Boolean = false
}

/** 可插拔的数据源注册表（第 3 步：真实 TIM 数据接入）。 */
object AppDataSources {

    @Volatile
    var chatSource: ChatSource? = null

    @Volatile
    var messageSource: MessageSource? = null

    @Volatile
/**
 * 原生 TIM 界面切换（仅宿主模式）。
 * 我们的 Compose 界面是盖在 TIM decorView 上的覆盖层；切到原生时隐藏覆盖层。
 */
interface NativeUiSource {
    /** 切换到 TIM 原生界面；成功返回 true。 */
    fun switchToNativeUi(): Boolean = false

    /** 当前是否处于 TIM 原生界面。 */
    fun isNativeUiMode(): Boolean = false
}

    var contactSource: ContactSource? = null

    @Volatile
    var emojiSource: EmojiSource? = null

    @Volatile
    var chatSettingSource: ChatSettingSource? = null

    @Volatile
    var addFriendSource: AddFriendSource? = null

    @Volatile
    var qrActionSource: QrActionSource? = null

    @Volatile
    var groupCreateSource: GroupCreateSource? = null

    @Volatile
    var voicePlaybackSource: VoicePlaybackSource? = null

    @Volatile
    var groupFileSource: GroupFileSource? = null

    @Volatile
    var groupManageSource: GroupManageSource? = null

    @Volatile
    var searchSource: SearchSource? = null

    @Volatile
    var callSource: CallSource? = null

    @Volatile
    var friendManageSource: FriendManageSource? = null

    /** 是否运行在宿主（TIM）进程、使用真实数据。 */
    val hasLiveDataSource: Boolean
        get() = chatSource != null

    val hasLiveMessages: Boolean
        get() = messageSource != null

    val hasLiveContacts: Boolean
        get() = contactSource != null

    @Volatile
    var nativeUiSource: NativeUiSource? = null

    val hasLiveEmojis: Boolean
        get() = emojiSource != null

    val hasLiveChatSettings: Boolean
        get() = chatSettingSource != null

    val hasLiveAddFriend: Boolean
        get() = addFriendSource != null

    val hasLiveQrAction: Boolean
        get() = qrActionSource != null

    val hasLiveGroupCreate: Boolean
        get() = groupCreateSource != null

    val hasLiveVoicePlayback: Boolean
        get() = voicePlaybackSource != null

    val hasLiveGroupManage: Boolean
        get() = groupManageSource != null

    val hasLiveSearch: Boolean
        get() = searchSource != null

    val hasLiveCall: Boolean
        get() = callSource != null

    val hasLiveFriendManage: Boolean
        get() = friendManageSource != null
}
