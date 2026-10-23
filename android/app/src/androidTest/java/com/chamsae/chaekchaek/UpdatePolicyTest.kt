package com.chamsae.chaekchaek

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chaekchaek.app.config.*
import com.chaekchaek.app.ui.common.UpdatePolicyHost
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdatePolicyTest {
    @get:Rule val compose = createComposeRule()

    @Test fun requiredCannotBeDismissedBySystemBackAndKeepsDraft() {
        val controller = MobilePolicyController(InstalledApp(10))
        val draft = mutableStateOf("보존할 감상 초안")
        controller.overrideRequirement(UpdateRequirement.Required)
        compose.setContent { ChaekchaekTheme { Text(draft.value); UpdatePolicyHost(controller) {} } }
        compose.onNodeWithText("나중에").assertDoesNotExist()
        compose.onNodeWithText("업데이트").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        compose.waitForIdle()
        compose.onNodeWithText("업데이트가 필요해요").assertIsDisplayed()
        compose.runOnIdle { controller.overrideRequirement(null) }
        compose.onNodeWithText("보존할 감상 초안").assertIsDisplayed()
    }

    @Test fun recommendedDefersWithoutRecreatingUnderlyingState() {
        val controller = MobilePolicyController(InstalledApp(10))
        controller.overrideRequirement(UpdateRequirement.Recommended)
        var actions = 0
        compose.setContent { ChaekchaekTheme { Text("입력 유지"); UpdatePolicyHost(controller) { actions++ } } }
        compose.onNodeWithText("나중에").performClick()
        compose.onNodeWithText("새 버전이 있어요").assertDoesNotExist()
        compose.onNodeWithText("입력 유지").assertIsDisplayed()
        assertEquals(0, actions)
    }
}
