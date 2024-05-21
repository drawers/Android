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

package com.duckduckgo.adclick.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.adclick.api.AdClickManager
import com.duckduckgo.adclick.impl.pixels.AdClickPixelName
import com.duckduckgo.adclick.impl.pixels.AdClickPixels
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DuckDuckGoAdClickManagerTest {

    private val mockAdClickData: AdClickData = mock()
    private val mockAdClickAttribution: AdClickAttribution = mock()
    private val mockAdClickPixels: AdClickPixels = mock()
    private lateinit var testee: AdClickManager

    @Before
    fun before() {
        testee = DuckDuckGoAdClickManager(mockAdClickData, mockAdClickAttribution, mockAdClickPixels)
    }

    @Test
    fun `detectAdClick - null url - return`() {
        testee.detectAdClick(url = null, isMainFrame = false)

        verifyNoInteractions(mockAdClickData)
    }

    @Test
    fun `detectAdClick - not main frame - return`() {
        testee.detectAdClick(url = "url", isMainFrame = false)

        verifyNoInteractions(mockAdClickData)
    }

    @Test
    fun `detectAdClick - ad click called for ad url and is main frame and only heuristic detection enabled - update ad domain with empty string`() {
        whenever(mockAdClickAttribution.isAdClick(any())).thenReturn(Pair(true, null))
        whenever(mockAdClickAttribution.isDomainDetectionEnabled()).thenReturn(false)
        whenever(mockAdClickAttribution.isHeuristicDetectionEnabled()).thenReturn(true)
        val url = "ad_url.com"

        testee.detectAdClick(url = url, isMainFrame = true)

        verify(mockAdClickData).setAdDomainTldPlusOne("")
    }

    @Test
    fun `detectAdClick - ad domain detection enabled - update ad domain`() {
        whenever(mockAdClickAttribution.isAdClick(any())).thenReturn(Pair(true, "domain.com"))
        whenever(mockAdClickAttribution.isDomainDetectionEnabled()).thenReturn(true)
        whenever(mockAdClickAttribution.isHeuristicDetectionEnabled()).thenReturn(false)
        val url = "https://ad_url?ad_domain=domain.com"

        testee.detectAdClick(url = url, isMainFrame = true)

        verify(mockAdClickData).setAdDomainTldPlusOne("domain.com")
    }

    @Test
    fun `detectAdClick - ad domain present - update ad domain value`() {
        whenever(mockAdClickAttribution.isAdClick(any())).thenReturn(Pair(true, "domain.com"))
        whenever(mockAdClickAttribution.isDomainDetectionEnabled()).thenReturn(true)
        whenever(mockAdClickAttribution.isHeuristicDetectionEnabled()).thenReturn(true)
        val url = "https://ad_url?ad_domain=domain.com"

        testee.detectAdClick(url = url, isMainFrame = true)

        verify(mockAdClickData).setAdDomainTldPlusOne("domain.com")
    }

    @Test
    fun `detectAdClick - ad click detected - update ad domain with empty string`() {
        whenever(mockAdClickAttribution.isAdClick(any())).thenReturn(Pair(true, null))
        whenever(mockAdClickAttribution.isDomainDetectionEnabled()).thenReturn(true)
        whenever(mockAdClickAttribution.isHeuristicDetectionEnabled()).thenReturn(true)
        val url = "https://ad_url.com?other_param=value"

        testee.detectAdClick(url = url, isMainFrame = true)

        verify(mockAdClickData).setAdDomainTldPlusOne("")
    }

    @Test
    fun `detectAdClick - non ad url and exemption expired - update exemptions map`() {
        whenever(mockAdClickAttribution.isAdClick(any())).thenReturn(Pair(false, null))
        whenever(mockAdClickData.getExemption()).thenReturn(expired("host.com"))
        val url = "non_ad_url.com"

        testee.detectAdClick(url = url, isMainFrame = true)

        verify(mockAdClickData, never()).setAdDomainTldPlusOne(any())
        verify(mockAdClickData).removeExemption()
    }

    @Test
    fun `detectAdClick - non ad url and exemption not expired - update exemptions map`() {
        whenever(mockAdClickAttribution.isAdClick(any())).thenReturn(Pair(false, null))
        whenever(mockAdClickData.getExemption()).thenReturn(notExpired("host.com"))
        val url = "non_ad_url.com"

        testee.detectAdClick(url = url, isMainFrame = true)

        verify(mockAdClickData, never()).setAdDomainTldPlusOne(any())
        verify(mockAdClickData).addExemption(any())
    }

    @Test
    fun `setActiveTabId - no source tab id - set active tab`() {
        val tabId = "tab_id"

        testee.setActiveTabId(tabId = tabId, url = "url", sourceTabId = null, sourceTabUrl = null)

        verify(mockAdClickData).setActiveTab(tabId)
        verify(mockAdClickData, never()).addExemption(tabId = any(), exemption = any())
    }

    @Test
    fun `setActiveTabId - set active tab and propagate exemption`() {
        val tabId = "tab_id"
        val url = "https://asos.com/"
        val sourceTabId = "source_tab_id"
        val sourceTabUrl = "source_url"
        whenever(mockAdClickData.getExemption(sourceTabId)).thenReturn(notExpired("asos.com"))

        testee.setActiveTabId(tabId = tabId, url = url, sourceTabId = sourceTabId, sourceTabUrl = sourceTabUrl)

        verify(mockAdClickData).setActiveTab(tabId)
        verify(mockAdClickData).addExemption(tabId = any(), exemption = any())
        verify(mockAdClickPixels).updateCountPixel(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION)
    }

    @Test
    fun `setActiveTabId - set active tab and do not propagate exemption from source - does not add exemption or update count pixel`() {
        val tabId = "tab_id"
        val url = "https://asos.com/"
        val sourceTabId = "source_tab_id"
        val sourceTabUrl = "source_url"
        whenever(mockAdClickData.getExemption(sourceTabId)).thenReturn(notExpired("asos.com"))
        whenever(mockAdClickData.getExemption()).thenReturn(notExpired("onbuy.com"))

        testee.setActiveTabId(tabId = tabId, url = url, sourceTabId = sourceTabId, sourceTabUrl = sourceTabUrl)

        verify(mockAdClickData).setActiveTab(tabId)
        verify(mockAdClickData, never()).addExemption(tabId = any(), exemption = any())
        verify(mockAdClickPixels, never()).updateCountPixel(AdClickPixelName.AD_CLICK_PAGELOADS_WITH_AD_ATTRIBUTION)
    }

    @Test
    fun `detectAdDomain - no ad url - remove ad domain`() {
        testee.detectAdDomain(url = "https://no_ad.com/")

        verify(mockAdClickData).removeAdDomain()
        verify(mockAdClickData, never()).addExemption(any())
    }

    @Test
    fun `detectAdDomain - duckduckgo url - return`() {
        testee.detectAdDomain(url = "https://duckduckgo.com")

        verify(mockAdClickData, never()).addExemption(any())
    }

    @Test
    fun `detectAdDomain - ad domain detected - add exemption and remove ad domain`() {
        whenever(mockAdClickData.getAdDomainTldPlusOne()).thenReturn("landing_page.com")

        testee.detectAdDomain(url = "https://landing_page.com/")

        verify(mockAdClickData).addExemption(any())
        verify(mockAdClickData).removeAdDomain()
    }

    @Test
    fun `detectAdDomain - ad domain matches - domain detection pixel sent with matched`() {
        whenever(mockAdClickData.getAdDomainTldPlusOne()).thenReturn("landing_page.com")

        testee.detectAdDomain(url = "https://landing_page.com/")

        verify(mockAdClickPixels).fireAdClickDetectedPixel(
            savedAdDomain = "landing_page.com",
            urlAdDomain = "landing_page.com",
            heuristicEnabled = false,
            domainEnabled = false,
        )
    }

    @Test
    fun `detectAdDomain - ad domain detection pixel sent with mismatch`() {
        whenever(mockAdClickData.getAdDomainTldPlusOne()).thenReturn("landing_page.com")

        testee.detectAdDomain(url = "https://other_landing_page.com/")

        verify(mockAdClickPixels).fireAdClickDetectedPixel(
            savedAdDomain = "landing_page.com",
            urlAdDomain = "other_landing_page.com",
            heuristicEnabled = false,
            domainEnabled = false,
        )
    }

    @Test
    fun `detectAdDomain - serp only - domain detection pixel sent`() {
        whenever(mockAdClickData.getAdDomainTldPlusOne()).thenReturn("landing_page")

        testee.detectAdDomain(url = "https://")

        verify(mockAdClickPixels).fireAdClickDetectedPixel(
            savedAdDomain = "landing_page",
            urlAdDomain = "",
            heuristicEnabled = false,
            domainEnabled = false,
        )
    }

    @Test
    fun `detectAdDomain - no serp domain and url domain - send domain detection pixel with heuristic only`() {
        whenever(mockAdClickData.getAdDomainTldPlusOne()).thenReturn("")

        testee.detectAdDomain(url = "https://landing_page.com/")

        verify(mockAdClickPixels).fireAdClickDetectedPixel(
            savedAdDomain = "",
            urlAdDomain = "landing_page.com",
            heuristicEnabled = false,
            domainEnabled = false,
        )
    }

    @Test
    fun `detectAdDomain - no serp domain and empty url domain - domain detection pixel sent with none`() {
        whenever(mockAdClickData.getAdDomainTldPlusOne()).thenReturn("")

        testee.detectAdDomain(url = "https://")

        verify(mockAdClickPixels).fireAdClickDetectedPixel(
            savedAdDomain = "",
            urlAdDomain = "",
            heuristicEnabled = false,
            domainEnabled = false,
        )
    }

    @Test
    fun `detectAdDomain - heuristics enabled - domain detection pixel sent with heuristics to true`() {
        whenever(mockAdClickData.getAdDomainTldPlusOne()).thenReturn("")
        whenever(mockAdClickAttribution.isHeuristicDetectionEnabled()).thenReturn(true)

        testee.detectAdDomain(url = "https://")

        verify(mockAdClickPixels).fireAdClickDetectedPixel(
            savedAdDomain = "",
            urlAdDomain = "",
            heuristicEnabled = true,
            domainEnabled = false,
        )
    }

    @Test
    fun `detectAdDomain - domain detection enabled - sends domain detection pixel with to true`() {
        whenever(mockAdClickData.getAdDomainTldPlusOne()).thenReturn("")
        whenever(mockAdClickAttribution.isDomainDetectionEnabled()).thenReturn(true)

        testee.detectAdDomain(url = "https://")

        verify(mockAdClickPixels).fireAdClickDetectedPixel(
            savedAdDomain = "",
            urlAdDomain = "",
            heuristicEnabled = false,
            domainEnabled = true,
        )
    }

    @Test
    fun `clearTabId - remove data for tab`() {
        val tabId = "tab_id"

        testee.clearTabId(tabId)

        verify(mockAdClickData).remove(tabId)
    }

    @Test
    fun `clearAll - remove all data`() {
        testee.clearAll()

        verify(mockAdClickData).removeAll()
    }

    @Test
    fun `clearAllExpired - remove all expired data`() {
        testee.clearAllExpiredAsync()

        verify(mockAdClickData).removeAllExpired()
    }

    @Test
    fun `isExemption - duckduckgo document url - returns false`() {
        val documentUrl = "https://duckduckgo.com"
        val url = "https://tracker.com"

        val result = testee.isExemption(documentUrl = documentUrl, url = url)
        verify(mockAdClickPixels, never()).fireAdClickActivePixel(any())
        assertFalse(result)
    }

    @Test
    fun `isExemption - is exemption called with url matching expired exemption - returns false`() {
        val documentUrl = "https://asos.com"
        val documentUrlHost = "asos.com"
        val url = "https://tracker.com"

        whenever(mockAdClickData.isHostExempted(documentUrlHost)).thenReturn(true)
        whenever(mockAdClickData.getExemption()).thenReturn(expired(documentUrlHost))

        val result = testee.isExemption(documentUrl = documentUrl, url = url)

        verify(mockAdClickData).removeExemption()
        verify(mockAdClickPixels, never()).fireAdClickActivePixel(any())
        assertFalse(result)
    }

    @Test
    fun `isExemption - is exemption called with url not matching expired exemption not matching tracker - returns false`() {
        val documentUrl = "https://asos.com"
        val documentUrlHost = "asos.com"
        val url = "https://tracker.com"

        whenever(mockAdClickData.isHostExempted(documentUrlHost)).thenReturn(true)
        whenever(mockAdClickData.getExemption()).thenReturn(notExpired(documentUrlHost))
        whenever(mockAdClickAttribution.isAllowed(url)).thenReturn(false)

        val result = testee.isExemption(documentUrl = documentUrl, url = url)

        verify(mockAdClickData, never()).removeExemption()
        verify(mockAdClickPixels, never()).fireAdClickActivePixel(any())
        assertFalse(result)
    }

    @Test
    fun `isExemption - called with url not matching expired exemption and matching tracker - send pixel and return true`() {
        val documentUrl = "https://asos.com"
        val documentUrlHost = "asos.com"
        val url = "https://bat.bing.com"

        whenever(mockAdClickData.isHostExempted(documentUrlHost)).thenReturn(true)
        whenever(mockAdClickData.getExemption()).thenReturn(notExpired(documentUrlHost))
        whenever(mockAdClickAttribution.isAllowed(url)).thenReturn(true)

        val result = testee.isExemption(documentUrl = documentUrl, url = url)

        verify(mockAdClickData, never()).removeExemption()
        verify(mockAdClickPixels).fireAdClickActivePixel(any())
        assertTrue(result)
    }

    private fun expired(hostTldPlusOne: String) = Exemption(
        hostTldPlusOne = hostTldPlusOne,
        navigationExemptionDeadline = 0L,
        exemptionDeadline = 0L,
    )

    private fun notExpired(hostTldPlusOne: String) = Exemption(
        hostTldPlusOne = hostTldPlusOne,
        navigationExemptionDeadline = Exemption.NO_EXPIRY,
        exemptionDeadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10),
    )
}
