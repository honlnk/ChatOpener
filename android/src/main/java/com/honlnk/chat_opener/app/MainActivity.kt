package com.honlnk.chat_opener.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.IntentCompat
import androidx.core.view.WindowCompat
import com.honlnk.chat_opener.app.ui.AppRoot
import com.honlnk.chat_opener.app.ui.theme.ChatOpenerTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleIntent(intent)
        setContent {
            ChatOpenerTheme {
                AppRoot(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let { vm.openUri(this, it, external = true) }
            Intent.ACTION_SEND -> {
                // 只接收分享来的文件流；纯文本 EXTRA_TEXT 不构成聊天记录，忽略
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.let { vm.openUri(this, it, external = true) }
            }
        }
    }
}
