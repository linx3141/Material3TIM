package com.material3tim.data.model

/**
 * TIM「我的手机 / 我的电脑 / 我的平板」DataLine 设备会话。
 *
 * 这些会话在最近列表里 chatType=8，peerUid 使用 TIM PeerUid 的固定 DATALINE_* 常量；
 * 它们没有真实 QQ 号，不能按普通用户资料展示（否则会显示 u_Wcc5... 乱文字）。
 */
object DataLine {

    const val PHONE_UID = "u_Wcc5rknRRqRO8y5gxMD6sA"
    const val PC_UID = "u_rK7NMsbv2ZjEGPdCuOiCfw"
    const val PAD_UID = "u_l7jpPIZxQo0mzJwoEt-SKw"

    const val KIND_NONE = 0
    const val KIND_PHONE = 1
    const val KIND_PC = 2
    const val KIND_PAD = 3
    /** 未知设备类型（TIM 将来新增设备时兜底）。 */
    const val KIND_OTHER = 4

    /** 设备会话的展示名。 */
    fun title(kind: Int): String = when (kind) {
        KIND_PHONE -> "我的手机"
        KIND_PC -> "我的电脑"
        KIND_PAD -> "我的平板"
        KIND_OTHER -> "我的设备"
        else -> ""
    }

    /** 设备会话统一说明（聊天页副标题 / 聊天信息 / 用户资料页共用）。 */
    const val DESCRIPTION = "跨设备文件传输"

    /** kind -> 设备 peerUid（点击设备会话消息头像时，用会话本身的设备类型打开资料页）。 */
    fun uid(kind: Int): String = when (kind) {
        KIND_PHONE -> PHONE_UID
        KIND_PC -> PC_UID
        KIND_PAD -> PAD_UID
        else -> ""
    }

    fun kindOf(uid: String?): Int = when (uid) {
        PHONE_UID -> KIND_PHONE
        PC_UID -> KIND_PC
        PAD_UID -> KIND_PAD
        else -> KIND_NONE
    }
}
