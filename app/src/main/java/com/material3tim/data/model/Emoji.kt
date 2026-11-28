package com.material3tim.data.model

import androidx.compose.runtime.Immutable

/** 表情包合集（示例数据）。 */
@Immutable
data class StickerPack(
    val id: String,
    val name: String,
    /** 用于在卡片上展示的代表性 emoji（示例数据用；真实数据可为空） */
    val previewEmoji: String = "",
    /** 真实表情包封面图 URL（TIM EmoticonPackage.imageUrl / CDN 封面；示例数据为空） */
    val coverUrl: String = "",
    /** 封面本地文件路径（TIM 包目录 color/list/gray.png 或下载到 cacheDir/sticker_pack） */
    val coverPath: String? = null,
    /** 是否已下载/已添加（TIM 真实数据为已安装表情包 tab） */
    val downloaded: Boolean = true,
)

/**
 * 用户收藏的表情（TIM 漫游收藏表情：看到别人的表情后「添加到表情」收集的那类单张图片表情）。
 * 与表情包（[StickerPack]）不同，这是用户自己的图片收藏。
 */
@Immutable
data class CustomEmoji(
    /** 稳定 id（TIM 的 resid / md5）。 */
    val id: String,
    /** 本地缓存图片路径（TIM 已下载到本机时提供）。 */
    val imagePath: String?,
    /** 描述文字（TIM 的修改词/OCR 词，可能为空）。 */
    val desc: String = "",
    /** 是否商城表情（MarkFace）；自拍/图片收藏一般为 false。 */
    val isMarkFace: Boolean = false,
)

/**
 * QQ 超级表情（TIM 里「表情」页的大号动画小黄脸）。
 *
 * TIM 的实现：`FaceElement`（elementType=6）里带 stickerType/stickerId/packId，
 * 动画资源是 Lottie JSON（`QQSysAndEmojiResMgr.getAniStickerResPath()` 返回的路径）。
 */
@Immutable
data class SuperEmojiItem(
    /** 本地 id（QQSysFaceUtil 的 localId）。 */
    val localId: Int,
    /** 服务端表情 id（FaceElement.faceIndex / QSid）。 */
    val serverId: Int,
    /** 描述文字（表情名/alt 文本）。 */
    val desc: String = "",
    /** 动画资源包 id / 动画贴纸 id（用于解析 Lottie 路径）。 */
    val packId: String = "",
    val stickerId: String = "",
    /** TIM AniStickerInfo.aniStickerType（超级表情发送需要，普通大表情一般为 1）。 */
    val stickerType: Int = 1,
    /** 已下载的 Lottie JSON 本地路径；为空表示资源未下载。 */
    val lottiePath: String? = null,
    /** 静态兜底图（APNG/PNG）本地路径。 */
    val imagePath: String? = null,
    val width: Int = 0,
    val height: Int = 0,
)

/** QQ 系统小黄脸（FaceElement 普通表情，静态内置图片）。 */
@Immutable
data class SystemFaceItem(
    val localId: Int,
    val serverId: Int,
    val desc: String = "",
    /** 已解码缓存的静态 png 路径（为空表示解析失败）。 */
    val imagePath: String? = null,
)

/** 加好友/群搜索到的个人账号。 */
/** 目标用户的加好友设置（TIM 原生 friend_setting + 验证问题）。 */
data class AddFriendSettingInfo(
    /** 0=允许任何人，1=需要验证信息，2=需要正确回答问题，3=需要回答问题，4=需要回答问题并经确认。 */
    val setting: Int,
    /** 设置 3/4 时目标用户配置的验证问题（可多个）。 */
    val questions: List<String> = emptyList(),
)

data class AddFriendUser(
    val uid: String,
    val uin: Long,
    val name: String,
    val avatarPath: String? = null,
    /** 副标题（签名/QQ号等）。 */
    val detail: String = "",
    /** 手机号搜索结果带出来的手机号（用于手机号加好友）。 */
    val phone: String = "",
    /** TIM 搜索结果里的验证 token（手机号加好友时走 Verify 校验用）。 */
    val token: String = "",
)

/** 加好友/群搜索到的群聊。 */
data class AddFriendGroup(
    val groupCode: Long,
    val name: String,
    val avatarPath: String? = null,
    val detail: String = "",
)

/** 「加好友/群」搜索结果：个人账号在上、群聊在下。 */
data class AddFriendSearchResult(
    val users: List<AddFriendUser> = emptyList(),
    val groups: List<AddFriendGroup> = emptyList(),
)
