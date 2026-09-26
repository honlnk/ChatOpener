package com.honlnk.chat_opener.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.honlnk.chat_opener.app.R
import com.honlnk.chat_opener.app.core.InlineMarkup
import com.honlnk.chat_opener.app.model.ChatLog
import com.honlnk.chat_opener.app.model.ChatMessage
import com.honlnk.chat_opener.app.model.OpenedDoc
import com.honlnk.chat_opener.app.ui.components.CompactTopAppBar
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

/** 气泡配色（亮/暗 × 用户/AI 四套），标记与高亮色随气泡底色适配。
 *  字段用 Long：0xFFxxxxxx 形态的字面量超过 Int.MAX_VALUE，本身就是 Long */
private data class BubblePalette(
    val userBg: Long, val userContent: Long, val userItalic: Long, val userCode: Long, val userHi: Long,
    val aiBg: Long, val aiContent: Long, val aiItalic: Long, val aiCode: Long, val aiHi: Long
)

private val LightPalette = BubblePalette(
    userBg = 0xFFA65A2E, userContent = 0xFFFFFFFF, userItalic = 0xE6FFFFFF, userCode = 0xFFF1DCBE, userHi = 0x40FFFFFF,
    aiBg = 0xFFF1EBDD, aiContent = 0xFF33302A, aiItalic = 0xFF6A655A, aiCode = 0xFF5B4636, aiHi = 0x59E8C170
)

private val DarkPalette = BubblePalette(
    userBg = 0xFFD6A878, userContent = 0xFF2E1D0E, userItalic = 0xB32E1D0E, userCode = 0xFF7A4E1E, userHi = 0x33261A0F,
    aiBg = 0xFF26221C, aiContent = 0xFFD9D3C4, aiItalic = 0xFFA89F8C, aiCode = 0xFFE0C9A6, aiHi = 0x4DD6A878
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    doc: OpenedDoc,
    isDark: Boolean,
    fontSizeSp: Int,
    showTimestamps: Boolean,
    showSystemMessages: Boolean,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    val log = doc.log
    when {
        doc.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        doc.error || doc.parseError || log == null -> ErrorView(doc, onRetry, onClose)
        else -> ChatList(doc, log, isDark, fontSizeSp, showTimestamps, showSystemMessages, onClose)
    }
}

@Composable
private fun ErrorView(doc: OpenedDoc, onRetry: () -> Unit, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.ErrorOutline, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(if (doc.parseError) R.string.parse_error else R.string.read_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row {
                if (doc.uri != null) {
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                    Spacer(Modifier.size(4.dp))
                }
                TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatList(
    doc: OpenedDoc,
    log: ChatLog,
    isDark: Boolean,
    fontSizeSp: Int,
    showTimestamps: Boolean,
    showSystemMessages: Boolean,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var hitPos by remember { mutableStateOf(-1) }
    var showStats by remember { mutableStateOf(false) }
    var showJump by remember { mutableStateOf(false) }

    val renderList = remember(log, showSystemMessages) {
        if (showSystemMessages) log.messages else log.messages.filter { !it.isSystem }
    }

    val trimmedQuery = remember(query) { query.trim() }
    val hits = remember(trimmedQuery, renderList) {
        if (trimmedQuery.isEmpty()) emptyList()
        else renderList.mapNotNull { m -> if (m.text.contains(trimmedQuery, ignoreCase = true)) m.index else null }
    }

    fun jumpTo(msgIndex: Int) {
        val pos = renderList.indexOfFirst { it.index == msgIndex }
        if (pos >= 0) {
            val cur = listState.firstVisibleItemIndex
            scope.launch {
                // 远距离直接跳（animate 跨越数千条会长时间占帧），近距离带动画
                if (abs(pos - cur) > 30) listState.scrollToItem(pos)
                else listState.animateScrollToItem(pos)
            }
        }
    }

    fun navHit(delta: Int) {
        if (hits.isEmpty()) return
        hitPos = if (hitPos < 0) 0 else (hitPos + delta + hits.size) % hits.size
        jumpTo(hits[hitPos])
    }

    val title = log.header?.characterName ?: doc.name.removeSuffix(".jsonl").removeSuffix(".JSONL")
    val subtitle = remember(log) {
        "${log.stats.totalMessages} 条 · ${formatChars(log.stats.totalChars)}"
    }
    val firstVisible by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val positionText = "第 ${firstVisible + 1}/${renderList.size} 条"
    val nearBottom by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= renderList.size - 5 }
    }

    Scaffold(
        topBar = {
            Column {
                CompactTopAppBar(
                    title = {
                        Column {
                            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "$subtitle · $positionText",
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Filled.Close, stringResource(R.string.close))
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchActive = !searchActive; if (!searchActive) { query = ""; hitPos = -1 } }) {
                            Icon(Icons.Filled.Search, stringResource(R.string.search))
                        }
                        IconButton(onClick = { showStats = true }) {
                            Icon(Icons.Filled.Info, stringResource(R.string.stats))
                        }
                        // 设置入口只在首页；预览页这个位置留给条数跳转
                        IconButton(onClick = { showJump = true }) {
                            Icon(Icons.Filled.GpsFixed, stringResource(R.string.jump_to))
                        }
                    }
                )
                AnimatedVisibility(visible = searchActive) {
                    SearchRow(
                        query = query,
                        hitPos = hitPos,
                        hitCount = hits.size,
                        onQueryChange = { query = it; hitPos = -1 },
                        onPrev = { navHit(-1) },
                        onNext = { navHit(1) },
                        onClose = { searchActive = false; query = ""; hitPos = -1 }
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = !nearBottom) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            listState.scrollToItem(renderList.size - 1)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.KeyboardDoubleArrowDown, stringResource(R.string.jump_bottom))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 96.dp)
        ) {
            itemsIndexed(renderList, key = { _, m -> "m${m.index}" }, contentType = { _, _ -> "msg" }) { idx, msg ->
                MessageRow(
                    msg = msg,
                    number = idx + 1,
                    prevDayText = if (idx > 0) renderList[idx - 1].dayText else null,
                    query = trimmedQuery,
                    isDark = isDark,
                    fontSizeSp = fontSizeSp,
                    showTimestamps = showTimestamps
                )
            }
        }
    }

    if (showStats) {
        StatsDialog(log = log, fileName = doc.name, onDismiss = { showStats = false })
    }

    if (showJump) {
        // 编号口径与顶栏「第 X/N 条」一致：当前渲染列表（系统消息可能已被过滤）的 1-based 位置
        JumpDialog(
            total = renderList.size,
            onJump = { n -> showJump = false; jumpTo(renderList[n - 1].index) },
            onDismiss = { showJump = false }
        )
    }
}

@Composable
private fun JumpDialog(total: Int, onJump: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val n = text.toIntOrNull()
    val valid = n != null && n in 1..total
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.jump_to)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { raw -> text = raw.filter { it.isDigit() }.take(6) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.jump_input_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text(stringResource(R.string.jump_range, total, total)) }
            )
        },
        confirmButton = {
            TextButton(onClick = { if (n != null) onJump(n) }, enabled = valid) {
                Text(stringResource(R.string.jump_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun SearchRow(
    query: String,
    hitPos: Int,
    hitCount: Int,
    onQueryChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.bodySmall) },
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            trailingIcon = if (query.isNotEmpty()) {
                { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, null, Modifier.size(16.dp)) } }
            } else null
        )
        Text(
            text = if (hitCount == 0) stringResource(R.string.search_no_hit)
            else "${if (hitPos >= 0) hitPos + 1 else 0}/$hitCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        IconButton(onClick = onPrev, enabled = hitCount > 0 && hitPos > 0) {
            Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.search))
        }
        IconButton(onClick = onNext, enabled = hitCount > 0) {
            Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.search))
        }
        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
    }
}

@Composable
private fun MessageRow(
    msg: ChatMessage,
    number: Int,
    prevDayText: String?,
    query: String,
    isDark: Boolean,
    fontSizeSp: Int,
    showTimestamps: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val day = msg.dayText
        if (day != null && day != prevDayText) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                ) {
                    Text(day, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                }
            }
        }

        if (msg.isSystem) {
            Text(
                // 系统消息也编号：内联前缀，保证可见序列连续不断档
                "#$number · ${msg.text}",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 4.dp)
            )
        } else {
            val alignEnd = msg.isUser
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
            ) {
                Column(
                    horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
                    modifier = Modifier.fillMaxWidth(0.86f)
                ) {
                    val meta = buildString {
                        append("#$number · ")
                        append(msg.sender ?: if (msg.isUser) "我" else "AI")
                        if (showTimestamps && msg.timeText != null) append(" · ${msg.timeText}")
                    }
                    Text(
                        meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 2.dp)
                    )
                    Bubble(
                        msg = msg,
                        query = query,
                        isDark = isDark,
                        fontSizeSp = fontSizeSp
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Bubble(msg: ChatMessage, query: String, isDark: Boolean, fontSizeSp: Int) {
    val palette = if (isDark) DarkPalette else LightPalette
    val user = msg.isUser

    val annotated: AnnotatedString = remember(msg.index, isDark, query) {
        if (user) InlineMarkup.build(msg.text, query, palette.userItalic.c(), palette.userCode.c(), palette.userHi.c())
        else InlineMarkup.build(msg.text, query, palette.aiItalic.c(), palette.aiCode.c(), palette.aiHi.c())
    }

    Surface(
        color = (if (user) palette.userBg else palette.aiBg).c(),
        contentColor = (if (user) palette.userContent else palette.aiContent).c(),
        shape = RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp,
            bottomStart = if (user) 16.dp else 4.dp,
            bottomEnd = if (user) 4.dp else 16.dp
        )
    ) {
        // 消息全量展示（v1.3.0 起折叠功能整体移除，用户拍板）
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                annotated,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.5f).sp
            )
        }
    }
}

@Composable
private fun StatsDialog(log: ChatLog, fileName: String, onDismiss: () -> Unit) {
    val s = log.stats
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.stats_title)) },
        text = {
            Column {
                StatsRow(stringResource(R.string.stats_character), log.header?.characterName ?: "—")
                StatsRow(stringResource(R.string.stats_user), log.header?.userName ?: "—")
                StatsRow(
                    stringResource(R.string.stats_messages),
                    "共 ${s.totalMessages}（用户 ${s.userMessages} · AI ${s.aiMessages} · 系统 ${s.systemMessages}）"
                )
                StatsRow(
                    stringResource(R.string.stats_chars),
                    "共 ${formatChars(s.totalChars)}（用户 ${formatChars(s.userChars)} · AI ${formatChars(s.aiChars)}）"
                )
                StatsRow(stringResource(R.string.stats_range), s.firstTime?.let { "$it → ${s.lastTime ?: ""}" } ?: "—")
                StatsRow(stringResource(R.string.stats_file), fileName)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.25f))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatChars(n: Long): String =
    if (n >= 10000) String.format(Locale.CHINA, "%.1f 万字", n / 10000.0) else "$n 字"

private fun Long.c() = androidx.compose.ui.graphics.Color(toInt())
