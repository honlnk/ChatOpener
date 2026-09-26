package com.honlnk.chat_opener.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import com.honlnk.chat_opener.app.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun AppRoot(vm: MainViewModel) {
    val current by vm.currentDoc.collectAsState()
    val lastDoc by vm.lastDoc.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val themeMode by vm.settings.themeMode.collectAsState(initial = 0)
    val fontSizeSp by vm.settings.fontSizeSp.collectAsState(initial = 17)
    val showTimestamps by vm.settings.showTimestamps.collectAsState(initial = false)
    val showSystemMessages by vm.settings.showSystemMessages.collectAsState(initial = true)

    val isDark = when (themeMode) {
        0 -> isSystemInDarkTheme()
        2 -> true
        else -> false
    }

    // 系统返回键分级：设置页 → 返回上一屏；应用内打开的记录 → 主页；
    // 外部 intent 直接打开的记录不拦截，直接退出 App 回到来源应用
    val cur = current
    BackHandler(enabled = showSettings || (cur != null && !cur.external)) {
        if (showSettings) showSettings = false else vm.closeCurrent()
    }

    if (showSettings) {
        SettingsScreen(
            themeMode = themeMode,
            fontSizeSp = fontSizeSp,
            showTimestamps = showTimestamps,
            showSystemMessages = showSystemMessages,
            onThemeChange = { vm.viewModelScope.launch { vm.settings.setThemeMode(it) } },
            onFontChange = { vm.viewModelScope.launch { vm.settings.setFontSize(it) } },
            onTimestampsChange = { vm.viewModelScope.launch { vm.settings.setShowTimestamps(it) } },
            onSystemMessagesChange = { vm.viewModelScope.launch { vm.settings.setShowSystemMessages(it) } },
            onBack = { showSettings = false }
        )
    } else if (cur != null) {
        ChatScreen(
            doc = cur,
            isDark = isDark,
            fontSizeSp = fontSizeSp,
            showTimestamps = showTimestamps,
            showSystemMessages = showSystemMessages,
            onRetry = { cur.uri?.let { vm.openUri(context, it, cur.external) } },
            onClose = vm::closeCurrent
        )
    } else {
        HomeScreen(
            onOpenUri = { vm.openUri(context, it) },
            onSettings = { showSettings = true },
            hasRecent = lastDoc != null,
            onOpenRecent = vm::reopenLast
        )
    }
}
