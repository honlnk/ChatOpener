package com.honlnk.chat_opener.app

import android.app.Application
import com.honlnk.chat_opener.app.core.SettingsRepository

class ChatOpenerApp : Application() {
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
