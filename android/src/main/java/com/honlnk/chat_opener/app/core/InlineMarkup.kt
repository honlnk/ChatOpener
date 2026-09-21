package com.honlnk.chat_opener.app.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * 酒馆文轻量行内标记 → AnnotatedString：
 * - \*\*粗体\*\* → 加粗
 * - \*斜体\*（RP 动作描写）→ 斜体 + 柔色
 * - `代码` → 等宽字体 + 代码色
 * - 命中搜索词 → 高亮背景
 *
 * 解析结果由 UI 层 remember 缓存（键：消息 + 主题 + 搜索词），
 * 滚动回收后重建的成本仅为数百字符的正则扫描。
 */
object InlineMarkup {

    private val token = Regex("""\*\*[^*\n]+\*\*|\*[^*\n]+\*|`[^`\n]+`""")

    fun build(
        text: String,
        query: String?,
        italicColor: Color,
        codeColor: Color,
        highlightBg: Color
    ): AnnotatedString = buildAnnotatedString {
        var last = 0
        for (m in token.findAll(text)) {
            if (m.range.first > last) {
                appendPlain(text.substring(last, m.range.first), query, highlightBg)
            }
            val seg = m.value
            when {
                seg.startsWith("**") -> appendPlain(
                    seg.substring(2, seg.length - 2), query, highlightBg,
                    SpanStyle(fontWeight = FontWeight.Bold)
                )
                seg.startsWith("`") -> appendPlain(
                    seg.substring(1, seg.length - 1), query, highlightBg,
                    SpanStyle(fontFamily = FontFamily.Monospace, color = codeColor)
                )
                else -> appendPlain(
                    seg.substring(1, seg.length - 1), query, highlightBg,
                    SpanStyle(fontStyle = FontStyle.Italic, color = italicColor)
                )
            }
            last = m.range.last + 1
        }
        if (last < text.length) appendPlain(text.substring(last), query, highlightBg)
    }

    private fun AnnotatedString.Builder.appendPlain(
        s: String,
        query: String?,
        highlightBg: Color,
        span: SpanStyle? = null
    ) {
        if (span != null) pushStyle(span)
        if (query.isNullOrEmpty()) {
            append(s)
        } else {
            var from = 0
            while (true) {
                val i = s.indexOf(query, from, ignoreCase = true)
                if (i < 0) break
                append(s, from, i)
                pushStyle(SpanStyle(background = highlightBg))
                append(s, i, i + query.length)
                pop()
                from = i + query.length
            }
            append(s, from, s.length)
        }
        if (span != null) pop()
    }
}
