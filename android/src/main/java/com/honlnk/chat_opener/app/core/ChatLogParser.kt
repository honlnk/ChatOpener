package com.honlnk.chat_opener.app.core

import com.honlnk.chat_opener.app.model.ChatHeader
import com.honlnk.chat_opener.app.model.ChatLog
import com.honlnk.chat_opener.app.model.ChatMessage
import com.honlnk.chat_opener.app.model.ChatStats
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 酒馆系（SillyTavern / Tavo）JSONL 聊天记录解析器。
 *
 * 格式（每行一个 JSON 对象）：
 * - 首行为 header：{"user_name":"鸿影","character_name":"武当艳情","create_date":"...","chat_metadata":{}}
 * - 之后每行一条消息：{"name":"鸿影","is_user":true,"is_system":false,"send_date":"...","mes":"正文",...}
 *
 * 容错策略：逐行独立解析，坏行跳过不中断；mes 缺失时回退 swipes[0]；
 * send_date 支持毫秒/秒时间戳、ISO 形态与 ST 英文形态，解析失败置 null。
 *
 * 纯 Kotlin + kotlinx-serialization JsonElement API，不触碰 Android 类，
 * JVM 单测可直接运行（org.json 在单测环境是 stub）。
 */
object ChatLogParser {

    private val json = Json { ignoreUnknownKeys = true }

    private val months = mapOf(
        "january" to 1, "february" to 2, "march" to 3, "april" to 4, "may" to 5, "june" to 6,
        "july" to 7, "august" to 8, "september" to 9, "october" to 10, "november" to 11, "december" to 12
    )

    /** 无有效消息（不是聊天记录文件）返回 null */
    fun parse(text: String): ChatLog? {
        var header: ChatHeader? = null
        val messages = ArrayList<ChatMessage>()
        var userMsg = 0
        var aiMsg = 0
        var sysMsg = 0
        var userChars = 0L
        var aiChars = 0L
        var firstTs: Long? = null
        var lastTs: Long? = null

        // SimpleDateFormat 非线程安全；parse() 单线程串行使用，方法内复用实例
        val timeFmt = SimpleDateFormat("HH:mm", Locale.CHINA)
        val dayFmt = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || !line.startsWith("{")) continue

            val obj = try {
                json.parseToJsonElement(line).jsonObject
            } catch (_: Exception) {
                continue
            }

            val mes = obj.str("mes")?.takeIf { it.isNotEmpty() }
                ?: obj.swipesFirst()

            if (mes != null) {
                val isUser = obj.bool("is_user") ?: false
                val isSystem = (obj.bool("is_system") ?: false) || (obj.bool("is_small_system") ?: false)
                val ts = parseTimestamp(obj.str("send_date"))
                if (ts != null) {
                    if (firstTs == null || ts < firstTs) firstTs = ts
                    if (lastTs == null || ts > lastTs) lastTs = ts
                }
                if (isSystem) sysMsg++ else if (isUser) {
                    userMsg++
                    userChars += mes.length
                } else {
                    aiMsg++
                    aiChars += mes.length
                }
                messages += ChatMessage(
                    index = messages.size,
                    sender = obj.str("name"),
                    isUser = isUser,
                    isSystem = isSystem,
                    text = mes,
                    ts = ts,
                    timeText = ts?.let { synchronized(timeFmt) { timeFmt.format(it) } },
                    dayText = ts?.let { synchronized(dayFmt) { dayFmt.format(it) } }
                )
            } else if (header == null && (obj.containsKey("user_name") || obj.containsKey("character_name"))) {
                header = ChatHeader(obj.str("user_name"), obj.str("character_name"), obj.str("create_date"))
            }
        }

        if (messages.isEmpty()) return null

        // header 固定在首行，补齐缺名消息的发送者显示名
        val h = header
        if (h != null) {
            for (i in messages.indices) {
                val m = messages[i]
                if (m.sender == null) {
                    val fallback = if (m.isUser) h.userName else h.characterName
                    if (fallback != null) messages[i] = m.copy(sender = fallback)
                }
            }
        }

        val fullFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        val stats = ChatStats(
            totalMessages = messages.size,
            userMessages = userMsg,
            aiMessages = aiMsg,
            systemMessages = sysMsg,
            userChars = userChars,
            aiChars = aiChars,
            firstTime = firstTs?.let { synchronized(fullFmt) { fullFmt.format(it) } },
            lastTime = lastTs?.let { synchronized(fullFmt) { fullFmt.format(it) } }
        )
        return ChatLog(h, messages, stats)
    }

    /** send_date 容错解析：毫秒/秒时间戳 → ISO（Tavo）→ ST 英文形态；失败返回 null */
    fun parseTimestamp(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val s = raw.trim()

        if (s.all { it.isDigit() }) {
            val n = s.toLongOrNull() ?: return null
            return when {
                s.length >= 13 -> n
                s.length in 10..12 -> n * 1000
                else -> null
            }
        }

        // 2026-05-31T02:30:00.000 或 2026-05-31 02:30:00
        Regex("""^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?""").find(s)?.let { m ->
            val y = m.groupValues[1].toInt()
            val mo = m.groupValues[2].toInt()
            val d = m.groupValues[3].toInt()
            val h = m.groupValues[4].toInt()
            val mi = m.groupValues[5].toInt()
            val sec = m.groupValues[6].ifEmpty { "0" }.toInt()
            if (mo in 1..12 && d in 1..31 && h in 0..23 && mi in 0..59 && sec in 0..59) {
                return cal(y, mo, d, h, mi, sec)
            }
            return null
        }

        // October 11, 2023 5:34:00pm / March 5, 2024 3:04pm
        Regex("""^([A-Za-z]+)\s+(\d{1,2}),\s+(\d{4})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?\s*([ap])\.?m\.?""",
            RegexOption.IGNORE_CASE
        ).find(s)?.let { m ->
            val mo = months[m.groupValues[1].lowercase()] ?: return null
            val d = m.groupValues[2].toInt()
            val y = m.groupValues[3].toInt()
            var h = m.groupValues[4].toInt()
            val mi = m.groupValues[5].toInt()
            val sec = m.groupValues[6].ifEmpty { "0" }.toInt()
            val pm = m.groupValues[7].equals("p", ignoreCase = true)
            if (pm && h < 12) h += 12
            if (!pm && h == 12) h = 0
            if (d in 1..31 && h in 0..23 && mi in 0..59 && sec in 0..59) {
                return cal(y, mo, d, h, mi, sec)
            }
            return null
        }

        return null
    }

    private fun cal(y: Int, mo: Int, d: Int, h: Int, mi: Int, sec: Int): Long {
        val c = Calendar.getInstance()
        c.clear()
        c.set(y, mo - 1, d, h, mi, sec)
        return c.timeInMillis
    }

    private fun JsonObject.str(key: String): String? =
        (get(key) as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull

    private fun JsonObject.bool(key: String): Boolean? =
        (get(key) as? JsonPrimitive)?.booleanOrNull

    private fun JsonObject.swipesFirst(): String? =
        (get("swipes") as? JsonArray)?.firstOrNull()?.let { it as? JsonPrimitive }
            ?.takeIf { it !is JsonNull }?.contentOrNull
}
