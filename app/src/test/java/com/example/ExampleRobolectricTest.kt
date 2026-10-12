package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.insights.BpClassification
import com.example.insights.ClinicalInsightsEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches PulseStep`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PulseStep", appName)
    }

    @Test
    fun `classify blood pressure follows AHA guidelines`() {
        val normal = ClinicalInsightsEngine.classifyBloodPressure(118, 76)
        assertEquals(BpClassification.NORMAL, normal)

        val elevated = ClinicalInsightsEngine.classifyBloodPressure(124, 78)
        assertEquals(BpClassification.ELEVATED, elevated)

        val stage1 = ClinicalInsightsEngine.classifyBloodPressure(135, 82)
        assertEquals(BpClassification.STAGE_1, stage1)

        val stage2 = ClinicalInsightsEngine.classifyBloodPressure(145, 92)
        assertEquals(BpClassification.STAGE_2, stage2)

        val crisis = ClinicalInsightsEngine.classifyBloodPressure(185, 125)
        assertEquals(BpClassification.CRISIS, crisis)
    }

    @Test
    fun `classify heart rate zones`() {
        assertEquals("Athletic / Low Resting", ClinicalInsightsEngine.classifyHeartRate(55))
        assertEquals("Normal Resting", ClinicalInsightsEngine.classifyHeartRate(72))
        assertEquals("Elevated / Active", ClinicalInsightsEngine.classifyHeartRate(110))
    }
}
