/*
 * Copyright (c) 2022 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.adclick.impl.pixels

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.adclick.impl.Exemption
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.app.statistics.pixels.Pixel.PixelType.COUNT
import com.duckduckgo.common.test.api.InMemorySharedPreferences
import java.time.Instant
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class RealAdClickPixelsTest {

    private lateinit var testee: AdClickPixels
    private lateinit var prefs: SharedPreferences

    private val mockPixel: Pixel = mock()
    private val mockContext: Context = mock()

    @Before
    fun setup() {
        prefs = InMemorySharedPreferences()
        whenever(mockContext.getSharedPreferences("com.duckduckgo.adclick.impl.pixels", 0)).thenReturn(prefs)
        testee = RealAdClickPixels(mockPixel, mockContext)
    }

    @Test
    fun `fireAdClickActivePixel - null exemption - return false`() {
        val exemption = null

        val result = testee.fireAdClickActivePixel(exemption)

        assertFalse(result)
    }

    @Test
    fun `fireAdClickActivePixel - non-null exemption and pixel already fired - return false`() {
        val exemption = Exemption(
            hostTldPlusOne = "ad_domain",
            navigationExemptionDeadline = 0L,
            exemptionDeadline = 0L,
            adClickActivePixelFired = true,
        )

        val result = testee.fireAdClickActivePixel(exemption)

        assertFalse(result)
    }

    @Test
    fun `fireAdClickActivePixel - non-null exemption and pixel not already fired - fire pixel and return true`() {
        val exemption = Exemption(
            hostTldPlusOne = "ad_domain",
            navigationExemptionDeadline = 0L,
            exemptionDeadline = 0L,
            adClickActivePixelFired = false,
        )

        val result = testee.fireAdClickActivePixel(exemption)

        verify(mockPixel).fire(AdClickPixelName.AD_CLICK_ACTIVE)
        assertTrue(result)
    }

    @Test
    fun `fireAdClickDetectedPixel - saved ad domain same as url ad domain - pixel sent with matched param`() {
        val savedAdDomain = "ad_domain"
        val urlAdDomain = "ad_domain"
        val heuristicEnabled = true
        val domainEnabled = true

        testee.fireAdClickDetectedPixel(savedAdDomain, urlAdDomain, heuristicEnabled, domainEnabled)

        verify(mockPixel).fire(
            AdClickPixelName.AD_CLICK_DETECTED,
            mapOf(
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION to AdClickPixelValues.AD_CLICK_DETECTED_MATCHED,
                AdClickPixelParameters.AD_CLICK_HEURISTIC_DETECTION to "true",
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION_ENABLED to "true",
            ),
        )
    }

    @Test
    fun `fireAdClickDetectedPixel - saved ad domain different than URL ad domain - pixel sent with mismatch param`() {
        val savedAdDomain = "ad_domain"
        val urlAdDomain = "other_domain"
        val heuristicEnabled = true
        val domainEnabled = true

        testee.fireAdClickDetectedPixel(savedAdDomain, urlAdDomain, heuristicEnabled, domainEnabled)

        verify(mockPixel).fire(
            AdClickPixelName.AD_CLICK_DETECTED,
            mapOf(
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION to AdClickPixelValues.AD_CLICK_DETECTED_MISMATCH,
                AdClickPixelParameters.AD_CLICK_HEURISTIC_DETECTION to "true",
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION_ENABLED to "true",
            ),
        )
    }

    @Test
    fun `fireAdClickDetectedPixel - no url ad domain - pixel sent with serp only param`() {
        val savedAdDomain = "ad_domain"
        val urlAdDomain = ""
        val heuristicEnabled = true
        val domainEnabled = true

        testee.fireAdClickDetectedPixel(savedAdDomain, urlAdDomain, heuristicEnabled, domainEnabled)

        verify(mockPixel).fire(
            AdClickPixelName.AD_CLICK_DETECTED,
            mapOf(
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION to AdClickPixelValues.AD_CLICK_DETECTED_SERP_ONLY,
                AdClickPixelParameters.AD_CLICK_HEURISTIC_DETECTION to "true",
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION_ENABLED to "true",
            ),
        )
    }

    @Test
    fun `fireAdClickDetectedPixel - no saved ad domain and url ad domain - pixel sent with heuristic only param`() {
        val savedAdDomain = ""
        val urlAdDomain = "ad_domain"
        val heuristicEnabled = true
        val domainEnabled = true

        testee.fireAdClickDetectedPixel(savedAdDomain, urlAdDomain, heuristicEnabled, domainEnabled)

        verify(mockPixel).fire(
            AdClickPixelName.AD_CLICK_DETECTED,
            mapOf(
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION to AdClickPixelValues.AD_CLICK_DETECTED_HEURISTIC_ONLY,
                AdClickPixelParameters.AD_CLICK_HEURISTIC_DETECTION to "true",
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION_ENABLED to "true",
            ),
        )
    }

    @Test
    fun `fireAdClickDetectedPixel - no saved ad domain and no url ad domain - pixel sent with none param`() {
        val savedAdDomain = ""
        val urlAdDomain = ""
        val heuristicEnabled = true
        val domainEnabled = true

        testee.fireAdClickDetectedPixel(savedAdDomain, urlAdDomain, heuristicEnabled, domainEnabled)

        verify(mockPixel).fire(
            AdClickPixelName.AD_CLICK_DETECTED,
            mapOf(
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION to AdClickPixelValues.AD_CLICK_DETECTED_NONE,
                AdClickPixelParameters.AD_CLICK_HEURISTIC_DETECTION to "true",
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION_ENABLED to "true",
            ),
        )
    }

    @Test
    fun `fireAdClickDetectedPixel - disabled heuristic detection - pixel sent with correct param`() {
        val savedAdDomain = ""
        val urlAdDomain = ""
        val heuristicEnabled = false
        val domainEnabled = true

        testee.fireAdClickDetectedPixel(savedAdDomain, urlAdDomain, heuristicEnabled, domainEnabled)

        verify(mockPixel).fire(
            AdClickPixelName.AD_CLICK_DETECTED,
            mapOf(
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION to AdClickPixelValues.AD_CLICK_DETECTED_NONE,
                AdClickPixelParameters.AD_CLICK_HEURISTIC_DETECTION to "false",
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION_ENABLED to "true",
            ),
        )
    }

    @Test
    fun `fireAdClickDetectedPixel - disabled domain detection - pixel sent with correct param`() {
        val savedAdDomain = ""
        val urlAdDomain = ""
        val heuristicEnabled = true
        val domainEnabled = false

        testee.fireAdClickDetectedPixel(savedAdDomain, urlAdDomain, heuristicEnabled, domainEnabled)

        verify(mockPixel).fire(
            AdClickPixelName.AD_CLICK_DETECTED,
            mapOf(
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION to AdClickPixelValues.AD_CLICK_DETECTED_NONE,
                AdClickPixelParameters.AD_CLICK_HEURISTIC_DETECTION to "true",
                AdClickPixelParameters.AD_CLICK_DOMAIN_DETECTION_ENABLED to "false",
            ),
        )
    }

    @Test
    fun `updateCountPixel - sharedPref updated`() {
        val key = "${AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION.pixelName}_count"
        assertEquals(0, prefs.getInt(key, 0))

        testee.updateCountPixel(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION)
        testee.updateCountPixel(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION)

        assertEquals(2, prefs.getInt(key, 0))
    }

    @Test
    fun `fireCountPixel - zero count - pixel not sent`() {
        val key = "${AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION.pixelName}_count"
        assertEquals(0, prefs.getInt(key, 0))

        testee.fireCountPixel(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION)

        verify(mockPixel, never()).fire(
            pixel = eq(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION),
            parameters = any(),
            encodedParameters = any(),
            type = eq(COUNT),
        )
    }

    @Test
    fun `fireCountPixel - non-zero count and current time not set - pixel sent`() {
        val key = "${AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION.pixelName}_count"
        testee.updateCountPixel(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION)
        assertEquals(1, prefs.getInt(key, 0))

        testee.fireCountPixel(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION)

        verify(mockPixel).fire(
            pixel = eq(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION),
            parameters = eq(mapOf(AdClickPixelParameters.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION_COUNT to "1")),
            encodedParameters = any(),
            type = eq(COUNT),
        )
    }

    @Test
    fun `fireCountPixel - non-zero count and time before timestamp - pixel not sent`() {
        val key = "${AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION.pixelName}_count"
        val timestampKey = "${AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION.pixelName}_timestamp"
        val now = Instant.now().toEpochMilli()
        testee.updateCountPixel(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION)
        assertEquals(1, prefs.getInt(key, 0))
        prefs.edit { putLong(timestampKey, now.plus(TimeUnit.HOURS.toMillis(1))) }

        testee.fireCountPixel(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION)

        verify(mockPixel, never()).fire(
            pixel = eq(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION),
            parameters = any(),
            encodedParameters = any(),
            type = eq(COUNT),
        )
    }

    @Test
    fun `fireCountPixel - non-zero count and time after timestamp - pixel sent`() {
        val key = "${AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION.pixelName}_count"
        val timestampKey = "${AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION.pixelName}_timestamp"
        val now = Instant.now().toEpochMilli()
        testee.updateCountPixel(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION)
        assertEquals(1, prefs.getInt(key, 0))
        prefs.edit { putLong(timestampKey, now.minus(TimeUnit.HOURS.toMillis(1))) }

        testee.fireCountPixel(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION)

        verify(mockPixel).fire(
            pixel = eq(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION),
            parameters = eq(mapOf(AdClickPixelParameters.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION_COUNT to "1")),
            encodedParameters = any(),
            type = eq(COUNT),
        )
    }
}
