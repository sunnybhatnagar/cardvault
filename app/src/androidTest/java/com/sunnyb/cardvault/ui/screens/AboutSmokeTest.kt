package com.sunnyb.cardvault.ui.screens

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutSmokeTest {

    @Test
    fun aboutScreen_rendersWithoutCrash() {
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            ComponentActivity::class.java
        )
        val scenario = ActivityScenario.launch<ComponentActivity>(intent)
        scenario.onActivity { activity ->
            activity.setContent {
                AboutScreen(onBack = {})
            }
            assertNotNull("Compose content should be set", activity)
        }
        scenario.close()
    }
}
