package it.matteogrechi.gymassystant

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.matteogrechi.gymassystant.app.MainActivity
import it.matteogrechi.gymassystant.ui.GymColors
import it.matteogrechi.gymassystant.ui.GymTheme
import it.matteogrechi.gymassystant.ui.LoadingScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

// GYM-007/013/022/025: verify real Compose placement/palette, without delaying production startup.
@RunWith(AndroidJUnit4::class)
class LaunchScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun loadingBrandIsCenteredWithoutNavigationInBothThemes() {
        val theme = mutableStateOf("light")
        compose.runOnUiThread {
            compose.activity.setContent {
                GymTheme(theme.value) {
                    Surface(Modifier.fillMaxSize(), color = GymColors.surface) { LoadingScreen() }
                }
            }
        }
        for ((mode, expected) in listOf("light" to 0xFFFFFAF2.toInt(), "dark" to 0xFF142729.toInt())) {
            compose.runOnUiThread { theme.value = mode }
            compose.waitForIdle()
            val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
            val brand = compose.onNodeWithContentDescription("gym assystant").fetchSemanticsNode().boundsInRoot
            assertTrue(abs(brand.center.x - root.center.x) < 1f)
            assertTrue(abs(brand.center.y - root.center.y) < root.height * 0.05f)
            compose.onNodeWithText("Loading your data…").assertIsDisplayed()
            compose.onNodeWithText("Train").assertDoesNotExist()
            compose.onNodeWithContentDescription("More options").assertDoesNotExist()
            assertEquals(expected, compose.onRoot().captureToImage().toPixelMap()[2, 100].toArgb())
        }
    }
}
