package com.honlnk.chat_opener.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ChatLogParserTest {

    private fun sampleDoc(): String {
        val sb = StringBuilder()
        sb.appendLine("""{"user_name":"鸿影","character_name":"武当艳情","create_date":"2026-05-31@02h27m39s","chat_metadata":{}}""")
        sb.appendLine("""{"name":"武当艳情","is_user":false,"is_system":false,"send_date":"2026-05-31T02:28:00.000","mes":"*雪夜之中，婴儿的啼哭声划破山门。*","original_avatar":null,"extra":{}}""")
        sb.appendLine("""{"name":"鸿影","is_user":true,"is_system":false,"send_date":"2026-05-31T02:30:00.000","mes":"师姐跟了过去"}""")
        sb.appendLine("""坏行，不是 JSON，应当被跳过""")
        sb.appendLine("""{"name":"System","is_user":false,"is_system":true,"send_date":"1748627460000","mes":"[进入山门]"}""")
        // mes 缺失但有 swipes：回退 swipes[0]
        sb.appendLine("""{"name":"武当艳情","is_user":false,"is_system":false,"send_date":"2026-05-31T03:00:00.000","mes":"","swipes":["回退的正文"]}""")
        return sb.toString()
    }

    @Test
    fun `解析基本文档-统计与身份正确`() {
        val log = ChatLogParser.parse(sampleDoc())!!
        assertEquals("鸿影", log.header?.userName)
        assertEquals("武当艳情", log.header?.characterName)
        assertEquals(4, log.stats.totalMessages)
        assertEquals(1, log.stats.userMessages)
        assertEquals(2, log.stats.aiMessages)
        assertEquals(1, log.stats.systemMessages)
        assertTrue(log.stats.aiChars > 0)
        // 坏行被跳过，消息 index 连续
        assertEquals(listOf(0, 1, 2, 3), log.messages.map { it.index })
        // mes 为空串时回退 swipes[0]
        assertEquals("回退的正文", log.messages[3].text)
    }

    @Test
    fun `发送者缺名时用 header 补齐`() {
        val doc = """
            {"user_name":"我","character_name":"角色"}
            {"name":null,"is_user":true,"mes":"用户消息"}
            {"is_user":false,"mes":"AI 消息"}
        """.trimIndent()
        val log = ChatLogParser.parse(doc)!!
        assertEquals("我", log.messages[0].sender)
        assertEquals("角色", log.messages[1].sender)
    }

    @Test
    fun `无消息返回 null`() {
        assertNull(ChatLogParser.parse(""))
        assertNull(ChatLogParser.parse("""{"foo":1}{"bar":2}"""))
    }

    @Test
    fun `时间戳解析-毫秒`() {
        val ts = ChatLogParser.parseTimestamp("1748627460000")
        assertNotNull(ts)
        val cal = Calendar.getInstance().apply { timeInMillis = ts!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
    }

    @Test
    fun `时间戳解析-秒级补千`() {
        val sec = ChatLogParser.parseTimestamp("1748627460")
        assertNotNull(sec)
        assertEquals(1748627460000L, sec)
    }

    @Test
    fun `时间戳解析-ISO 形态`() {
        val ts = ChatLogParser.parseTimestamp("2026-05-31T02:30:00.000")!!
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH))
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(2, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `时间戳解析-ST 英文形态含 pm 进位`() {
        val ts = ChatLogParser.parseTimestamp("October 11, 2023 5:34:00pm")!!
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        assertEquals(2023, cal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH))
        assertEquals(17, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `时间戳解析-非法输入返回 null`() {
        assertNull(ChatLogParser.parseTimestamp(null))
        assertNull(ChatLogParser.parseTimestamp(""))
        assertNull(ChatLogParser.parseTimestamp("not a date"))
        assertNull(ChatLogParser.parseTimestamp("123"))   // 3 位数字不是合理时间戳
    }

    @Test
    fun `无 header 文档不崩溃且统计正确`() {
        val doc = """
            {"name":"A","is_user":false,"mes":"第一条"}
            {"name":"B","is_user":true,"mes":"第二条"}
        """.trimIndent()
        val log = ChatLogParser.parse(doc)!!
        assertNull(log.header)
        assertEquals(2, log.stats.totalMessages)
        assertEquals(0, log.stats.systemMessages)
    }
}
