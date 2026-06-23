package io.github.jeiel85.rxscan

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device runtime smoke test: launches the app and navigates the screens that
 * run from the synthetic preview, asserting the app does not crash. Catches
 * startup, theme, navigation, Compose, and CameraX-binding failures that the JVM
 * unit tests cannot. Uses synthetic data only.
 */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val cameraPermission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesToHomeWithoutCrash() {
        composeRule.onNodeWithText("RxScan").assertIsDisplayed()
        composeRule.onNodeWithText("약봉지 촬영").assertIsDisplayed()
    }

    @Test
    fun navigatesToSyntheticReviewWithoutCrash() {
        composeRule.onNodeWithText("검토 화면 미리보기", substring = true).performClick()
        composeRule.waitForIdle()
        // Review screen composed (mandatory-review title + finalize action).
        composeRule.onNodeWithText("약 확인").assertIsDisplayed()
        composeRule.onNodeWithText("확인한 약으로 계속").assertExists()
    }

    @Test
    fun navigatesIntoCameraScreenWithoutCrash() {
        // Home "약봉지 촬영" button -> scan intro.
        composeRule.onNode(hasText("약봉지 촬영").and(hasClickAction())).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("지원 범위 보기").assertIsDisplayed()
        // Intro "약봉지 촬영" button (disambiguated from the heading) -> live camera; CameraX must bind without crash.
        composeRule.onNode(hasText("약봉지 촬영").and(hasClickAction())).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("그래도 촬영").assertExists()
    }
}
