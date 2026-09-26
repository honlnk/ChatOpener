package com.honlnk.chat_opener.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.honlnk.chat_opener.app.model.ChatLog
import com.honlnk.chat_opener.app.model.ChatMessage
import com.honlnk.chat_opener.app.model.ChatStats
import com.honlnk.chat_opener.app.model.OpenedDoc
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 展开全文交互回归测试（Robolectric + Compose UI）。
 *
 * 背景：v1.0.0–v1.2.0 真机反馈「展开全文」点击无反应（顶栏按钮正常），
 * 两轮盲修无效后补此守卫——本测试在 JVM 上跑真实的 Compose 布局与命中测试，
 * 若此处通过而真机仍无效，即可锁定为设备/窗口层触摸派发问题，排除业务逻辑。
 *
 * 覆盖两条展开路径：底部标签点击、折叠态整块气泡点按（v1.2.1 新增的大触摸区）。
 * 阈值 1200 为 ChatScreen 内 COLLAPSE_THRESHOLD，测试文本取 1300 字保证折叠。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatScreenExpandTest {

    @get:Rule
    val compose = createComposeRule()

    private val tail = "TAILMARKER-尾部标记"
    private val longText = "头".repeat(1300) + tail

    private fun launch() {
        val msg = ChatMessage(
            index = 0, sender = "测试角色", isUser = false, isSystem = false,
            text = longText, ts = null, timeText = null, dayText = null
        )
        val log = ChatLog(
            header = null, messages = listOf(msg),
            stats = ChatStats(1, 0, 1, 0, 0, longText.length.toLong(), null, null)
        )
        compose.setContent {
            MaterialTheme {
                ChatScreen(
                    doc = OpenedDoc(
                        uri = null, name = "测试.jsonl", log = log,
                        loading = false, error = false, external = false
                    ),
                    isDark = false, fontSizeSp = 17,
                    showTimestamps = false, showSystemMessages = true,
                    onRetry = {}, onClose = {}
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `标签点击-展开后可收起`() {
        launch()
        compose.onNodeWithText("展开全文", substring = true).assertExists()
        compose.onNodeWithText(tail, substring = true).assertDoesNotExist()

        compose.onNodeWithText("展开全文", substring = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(tail, substring = true).assertExists()
        compose.onNodeWithText("收起", substring = true).assertExists()

        compose.onNodeWithText("收起", substring = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(tail, substring = true).assertDoesNotExist()
    }

    @Test
    fun `折叠态-点气泡本体也能展开`() {
        launch()
        compose.onNodeWithText("头".repeat(50), substring = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(tail, substring = true).assertExists()
    }
}
