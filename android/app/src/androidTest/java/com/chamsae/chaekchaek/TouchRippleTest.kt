package com.chamsae.chaekchaek

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chaekchaek.app.ui.common.ChaekDropdown
import com.chaekchaek.app.ui.common.ChaekFilterChip
import com.chaekchaek.app.ui.theme.ChaekchaekTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TouchRippleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lightThemeDoesNotDrawRipples() = assertNoRipples(darkTheme = false)

    @Test fun darkThemeDoesNotDrawRipples() = assertNoRipples(darkTheme = true)

    @Test
    fun pixelComparisonDetectsDefaultMaterialRipple() {
        compose.setContent {
            MaterialTheme {
                Surface(onClick = {}, modifier = Modifier.size(120.dp, 48.dp).testTag("default")) {
                    Text("Default")
                }
            }
        }
        assertTrue("기본 Material 리플을 감지해야 비교 검증이 유효하다", pressedPixelChanges("default") > 0)
    }

    @Test
    fun keyboardFocusAndPointerHoverStateLayersRemainVisible() {
        lateinit var focusInteractions: MutableInteractionSource
        compose.setContent {
            ChaekchaekTheme {
                focusInteractions = remember { MutableInteractionSource() }
                Column {
                    Surface(
                        onClick = {},
                        modifier = Modifier.size(120.dp, 48.dp).testTag("focus"),
                        interactionSource = focusInteractions,
                    ) {
                        Text("Focus")
                    }
                    Surface(onClick = {}, modifier = Modifier.size(120.dp, 48.dp).testTag("hover")) {
                        Text("Hover")
                    }
                }
            }
        }

        assertTrue("키보드 포커스 상태층은 보여야 한다", focusedPixelChanges("focus", focusInteractions) > 0)
        assertTrue("포인터 hover 상태층은 보여야 한다", hoveredPixelChanges("hover") > 0)
    }

    @Test
    fun clickLongClickSelectionAndAccessibilityRemainAvailable() {
        var clicks = 0
        var longClicks = 0
        var disabledClicks = 0
        compose.setContent {
            ChaekchaekTheme {
                Column {
                    Box(Modifier.size(48.dp).testTag("click").combinedClickable(
                        role = Role.Button,
                        onClick = { clicks++ },
                        onLongClick = { longClicks++ },
                    ))
                    Button(onClick = { disabledClicks++ }, enabled = false, modifier = Modifier.testTag("disabled")) {
                        Text("Disabled")
                    }
                    var selected by remember { mutableStateOf(false) }
                    ChaekFilterChip("Filter", selected, { selected = true }, Modifier.testTag("filter"))
                    var checked by remember { mutableStateOf(false) }
                    Box(Modifier.size(48.dp).testTag("toggle").toggleable(
                        value = checked, role = Role.Checkbox, onValueChange = { checked = it },
                    ))
                    var option by remember { mutableStateOf("Recent") }
                    ChaekDropdown(option, listOf("Recent", "Old"), { it }, { option = it }, "Sort order")
                }
            }
        }
        val click = compose.onNodeWithTag("click")
        click.assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        click.performTouchInput { click() }
        click.performClick() // 접근성 OnClick 액션도 유지한다.
        click.performTouchInput { longClick() }
        compose.onNodeWithTag("disabled").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithTag("filter").performClick().assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        compose.onNodeWithTag("toggle").assertIsOff().performClick().assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        compose.onNodeWithText("Recent").performClick()
        assertEquals("팝업 메뉴도 리플 없이 선택되어야 한다", 0, pressedPixelChanges(compose.onNodeWithText("Old")))
        compose.onNodeWithText("Old").assertExists()
        compose.runOnIdle {
            assertEquals(2, clicks)
            assertEquals(1, longClicks)
            assertEquals(0, disabledClicks)
        }
    }

    private fun assertNoRipples(darkTheme: Boolean) {
        var clicks = 0
        compose.setContent {
            ChaekchaekTheme(darkTheme = darkTheme) {
                RippleControls { clicks++ }
            }
        }
        for (tag in listOf("foundation", "surface", "button", "textButton", "iconButton", "menu", "filter", "toggle")) {
            assertEquals("$tag 눌림 중 픽셀이 바뀌면 안 된다", 0, pressedPixelChanges(tag))
        }
        compose.runOnIdle { assertEquals(8, clicks) }
    }

    private fun pressedPixelChanges(tag: String): Int = pressedPixelChanges(compose.onNodeWithTag(tag))

    private fun focusedPixelChanges(tag: String, interactions: MutableInteractionSource): Int {
        val node = compose.onNodeWithTag(tag)
        val before = node.captureToImage()
        compose.runOnIdle { interactions.tryEmit(FocusInteraction.Focus()) }
        Thread.sleep(180)
        return differentPixels(before, node.captureToImage())
    }

    private fun hoveredPixelChanges(tag: String): Int {
        val node = compose.onNodeWithTag(tag)
        val before = node.captureToImage()
        node.performMouseInput { enter(center) }
        Thread.sleep(180)
        val hovered = node.captureToImage()
        node.performMouseInput { exit(center) }
        return differentPixels(before, hovered)
    }

    private fun pressedPixelChanges(node: SemanticsNodeInteraction): Int {
        val before = node.captureToImage()
        node.performTouchInput { down(center) }
        // Android RenderThread의 리플도 충분히 그려진 뒤 비교한다.
        Thread.sleep(180)
        val pressed = node.captureToImage()
        node.performTouchInput { up() }
        return differentPixels(before, pressed)
    }

    private fun differentPixels(before: ImageBitmap, after: ImageBitmap): Int {
        assertEquals(before.width, after.width)
        assertEquals(before.height, after.height)
        val first = before.toPixelMap()
        val second = after.toPixelMap()
        var changed = 0
        for (y in 0 until before.height) for (x in 0 until before.width) {
            if (first[x, y] != second[x, y]) changed++
        }
        return changed
    }
}

@Composable
private fun RippleControls(onClick: () -> Unit) {
    Column {
        Box(Modifier.size(120.dp, 48.dp).testTag("foundation").clickable(
            role = Role.Button, onClick = onClick,
        )) { Text("Foundation") }
        Surface(onClick = onClick, modifier = Modifier.size(120.dp, 48.dp).testTag("surface")) { Text("Surface") }
        Button(onClick = onClick, modifier = Modifier.testTag("button")) { Text("Button") }
        TextButton(onClick = onClick, modifier = Modifier.testTag("textButton")) { Text("Text") }
        IconButton(onClick = onClick, modifier = Modifier.testTag("iconButton")) { Text("X") }
        DropdownMenuItem(text = { Text("Menu") }, onClick = onClick, modifier = Modifier.testTag("menu"))
        ChaekFilterChip("Filter", false, onClick, Modifier.testTag("filter"))
        Box(Modifier.size(120.dp, 48.dp).testTag("toggle").toggleable(
            value = false, role = Role.Checkbox, onValueChange = { onClick() },
        )) { Text("Toggle") }
    }
}
