package com.honlnk.chat_opener.app.model

import android.net.Uri

/** 聊天记录首行的元信息（Tavo / SillyTavern 导出格式） */
data class ChatHeader(
    val userName: String?,        // 用户扮演的角色名
    val characterName: String?,   // AI 角色名
    val createDate: String?
)

/** 一条聊天消息 */
data class ChatMessage(
    val index: Int,               // 在完整消息序列中的稳定序号（过滤系统消息后仍不变）
    val sender: String?,          // 发送者名字（来自 name 字段）
    val isUser: Boolean,
    val isSystem: Boolean,
    val text: String,             // 正文（mes 字段，缺失时回退 swipes[0]）
    val ts: Long?,                // 解析后的时间戳（毫秒），无法解析为 null
    val timeText: String?,        // "HH:mm" 展示用
    val dayText: String?          // "yyyy年M月d日" 日期分隔用
)

/** 整份记录的统计 */
data class ChatStats(
    val totalMessages: Int,
    val userMessages: Int,
    val aiMessages: Int,
    val systemMessages: Int,
    val userChars: Long,
    val aiChars: Long,
    val firstTime: String?,       // 最早消息时间（可解析的前提下）
    val lastTime: String?
) {
    val totalChars: Long get() = userChars + aiChars
}

data class ChatLog(
    val header: ChatHeader?,
    val messages: List<ChatMessage>,
    val stats: ChatStats
)

/** 当前打开的文档状态（对齐 MD Opener 的 OpenedFile 模式） */
data class OpenedDoc(
    val uri: Uri?,
    val name: String,
    val log: ChatLog?,
    val loading: Boolean,
    val error: Boolean,
    val parseError: Boolean = false,
    val external: Boolean
)
