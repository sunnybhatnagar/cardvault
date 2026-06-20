package com.sunnyb.cardvault.ui.screens

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sunnyb.cardvault.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun mainActivity_launchesWithoutCrash() {
        hiltRule.inject()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        var isResumed = false
        scenario.onActivity {
            isResumed = true
        }
        assertTrue("Activity should reach resumed state", isResumed)
        scenario.close()
    }
}

@RunWith(AndroidJUnit4::class)
class AboutScreenSmokeTest {

    @Test
    fun aboutScreen_rendersWithoutCrash() {
        val intent = Intent(
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
            ComponentActivity::class.java
        )
        val scenario = ActivityScenario.launch<ComponentActivity>(intent)
        scenario.onActivity { activity ->
            activity.setContent {
                AboutScreen(onBack = {})
            }
        }
        scenario.close()
    }
}
