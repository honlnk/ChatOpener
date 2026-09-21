package com.honlnk.chat_opener.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 紧凑版 TopAppBar：52dp 高、titleMedium（16sp）标题。
 * statusBarsPadding 放在高度约束之外，保证内容不顶进状态栏。
 * 必须用 heightIn(max=) 而非 height()：height() 会把 minHeight 也钉成 52dp，
 * 经 Surface 传入 TopAppBarLayout 后标题槽被迫撑满且 Text 顶对齐排字。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactTopAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon ?: {},
        actions = actions,
        modifier = Modifier
            .statusBarsPadding()
            .heightIn(max = 52.dp),
        windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    )
}
