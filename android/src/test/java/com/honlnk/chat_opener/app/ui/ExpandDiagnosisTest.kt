package com.honlnk.chat_opener.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 展开无反应 bug 的阶梯诊断（临时，定位后删除）：
 * ChatScreenExpandTest 已证明真机症状可在 JVM 复现——点击后状态未生效。
 * 此处以 A0→A→B 三级最小复现逐级逼近 Bubble 结构，CI 打印语义树辅助定位。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpandDiagnosisTest {

    @get:Rule
    val compose = createComposeRule()

    private fun dump(tag: String) {
        println("DIAG $tag >>> ${compose.onRoot().printToString()}")
    }

    @Test
    fun `A0-纯remember`() {
        compose.setContent {
            MaterialTheme {
                var expanded by remember { mutableStateOf(false) }
                val shown = remember(expanded) { if (expanded) "FULL" else "PREFIX" }
                Column {
                    Text(shown)
                    Text("toggle", Modifier.clickable { expanded = !expanded })
                }
            }
        }
        compose.waitForIdle()
        dump("A0-before")
        compose.onNodeWithText("toggle").performClick()
        compose.waitForIdle()
        dump("A0-after")
        compose.onNodeWithText("FULL").assertExists()
    }

    @Test
    fun `A1-rememberSaveable`() {
        compose.setContent {
            MaterialTheme {
                var expanded by rememberSaveable { mutableStateOf(false) }
                val shown = remember(expanded) { if (expanded) "FULL" else "PREFIX" }
                Column {
                    Text(shown)
                    Text("toggle", Modifier.clickable { expanded = !expanded })
                }
            }
        }
        compose.waitForIdle()
        dump("A1-before")
        compose.onNodeWithText("toggle").performClick()
        compose.waitForIdle()
        dump("A1-after")
        compose.onNodeWithText("FULL").assertExists()
    }

    @Test
    fun `B-Bubble同构`() {
        compose.setContent {
            MaterialTheme {
                val idx = 0
                val queryHit = false
                var expanded by rememberSaveable(idx) { mutableStateOf(false) }
                val effExpanded = expanded || queryHit
                val displayText = if (!effExpanded) "PREFIX" else "FULLTEXT"
                val annotated = remember(idx, effExpanded) { displayText }
                Surface {
                    Column(
                        Modifier.then(
                            if (!effExpanded) Modifier.clickable(role = Role.Button) { expanded = true }
                            else Modifier
                        )
                    ) {
                        Text(annotated)
                        Text(
                            "toggle",
                            Modifier.clickable(role = Role.Button) { expanded = !expanded }
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        dump("B-before")
        compose.onNodeWithText("toggle").performClick()
        compose.waitForIdle()
        dump("B-after")
        compose.onNodeWithText("FULLTEXT").assertExists()
    }
}
