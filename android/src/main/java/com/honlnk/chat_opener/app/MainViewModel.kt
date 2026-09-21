package com.honlnk.chat_opener.app

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.honlnk.chat_opener.app.core.ChatLogParser
import com.honlnk.chat_opener.app.core.SettingsRepository
import com.honlnk.chat_opener.app.core.UriReader
import com.honlnk.chat_opener.app.model.OpenedDoc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ChatOpenerApp
    val settings: SettingsRepository = app.settingsRepository

    val currentDoc = MutableStateFlow<OpenedDoc?>(null)

    /** 最近成功打开的记录（仅内存，随 ViewModel 销毁即失效），供主页一键重开 */
    val lastDoc = MutableStateFlow<OpenedDoc?>(null)

    fun openUri(context: Context, uri: Uri, external: Boolean = false) {
        val name = UriReader.displayName(context, uri) ?: uri.lastPathSegment ?: "chat.jsonl"
        currentDoc.value = OpenedDoc(uri, name, null, loading = true, error = false, external = external)
        viewModelScope.launch(Dispatchers.Default) {
            // 读取在 IO，解析在 Default；两步都是重活，不占用主线程
            val text = withContext(Dispatchers.IO) { UriReader.read(context, uri) }
            val doc = when {
                text == null ->
                    OpenedDoc(uri, name, null, loading = false, error = true, external = external)
                else -> {
                    val log = ChatLogParser.parse(text)
                    if (log == null) {
                        OpenedDoc(uri, name, null, loading = false, error = false, parseError = true, external = external)
                    } else {
                        OpenedDoc(uri, name, log, loading = false, error = false, external = external)
                    }
                }
            }
            currentDoc.value = doc
        }
    }

    fun closeCurrent() {
        // 只记录成功解析的记录；失败的重开只会再次报错，没有记忆价值
        currentDoc.value?.takeIf { it.log != null }?.let { lastDoc.value = it }
        currentDoc.value = null
    }

    /** 从主页重开最近记录：视为应用内打开，系统返回键退回主页而非退出 App */
    fun reopenLast() {
        lastDoc.value?.let { currentDoc.value = it.copy(external = false) }
    }
}
