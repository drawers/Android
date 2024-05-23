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

package com.duckduckgo.app.brokensite

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.browser.certificates.BypassedSSLCertificatesRepository
import com.duckduckgo.app.global.model.Site
import com.duckduckgo.app.global.model.SiteMonitor
import com.duckduckgo.app.privacy.db.UserAllowListRepository
import com.duckduckgo.app.surrogates.SurrogateResponse
import com.duckduckgo.app.trackerdetection.model.TrackerStatus
import com.duckduckgo.app.trackerdetection.model.TrackerType
import com.duckduckgo.app.trackerdetection.model.TrackingEvent
import com.duckduckgo.browser.api.brokensite.BrokenSiteData
import com.duckduckgo.browser.api.brokensite.BrokenSiteData.ReportFlow.MENU
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.privacy.config.api.ContentBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class BrokenSiteDataTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private val mockAllowListRepository: UserAllowListRepository = mock()

    private val mockContentBlocking: ContentBlocking = mock()
    private val mockBypassedSSLCertificatesRepository: BypassedSSLCertificatesRepository = mock()

    @Test
    fun `fromSite - site is null - data is empty and upgraded is false`() {
        val data = BrokenSiteData.fromSite(null, reportFlow = MENU)
        assertTrue(data.url.isEmpty())
        assertTrue(data.blockedTrackers.isEmpty())
        assertTrue(data.surrogates.isEmpty())
        assertFalse(data.upgradedToHttps)
    }

    @Test
    fun `fromSite - site exists - data contains url`() {
        val site = buildSite(SITE_URL)
        val data = BrokenSiteData.fromSite(site, reportFlow = MENU)
        assertEquals(SITE_URL, data.url)
    }

    @Test
    fun `fromSite - site upgraded - https upgraded is true`() {
        val site = buildSite(SITE_URL, httpsUpgraded = true)
        val data = BrokenSiteData.fromSite(site, reportFlow = MENU)
        assertTrue(data.upgradedToHttps)
    }

    @Test
    fun `fromSite - site not upgraded - https upgraded is false`() {
        val site = buildSite(SITE_URL, httpsUpgraded = false)
        val data = BrokenSiteData.fromSite(site, reportFlow = MENU)
        assertFalse(data.upgradedToHttps)
    }

    @Test
    fun `fromSite - url parameters removed - true`() {
        val site = buildSite(SITE_URL)
        site.urlParametersRemoved = true
        val data = BrokenSiteData.fromSite(site, reportFlow = MENU)
        assertTrue(data.urlParametersRemoved)
    }

    @Test
    fun `fromSite - url parameters not removed - url parameters removed is false`() {
        val site = buildSite(SITE_URL)
        val data = BrokenSiteData.fromSite(site, reportFlow = MENU)
        assertFalse(data.urlParametersRemoved)
    }

    @Test
    fun `fromSite - site has no trackers - blocked trackers is empty`() {
        val site = buildSite(SITE_URL)
        val data = BrokenSiteData.fromSite(site, reportFlow = MENU)
        assertTrue(data.blockedTrackers.isEmpty())
    }

    @Test
    fun `fromSite - site has blocked trackers - blocked trackers exist`() {
        val site = buildSite(SITE_URL)
        val event = TrackingEvent(
            documentUrl = "http://www.example.com",
            trackerUrl = "http://www.tracker.com/tracker.js",
            categories = emptyList(),
            entity = null,
            surrogateId = null,
            status = TrackerStatus.BLOCKED,
            type = TrackerType.OTHER,
        )
        val anotherEvent = TrackingEvent(
            documentUrl = "http://www.example.com/test",
            trackerUrl = "http://www.anothertracker.com/tracker.js",
            categories = emptyList(),
            entity = null,
            surrogateId = null,
            status = TrackerStatus.ALLOWED,
            type = TrackerType.OTHER,
        )
        site.trackerDetected(event)
        site.trackerDetected(anotherEvent)
        assertEquals("tracker.com", BrokenSiteData.fromSite(site, reportFlow = MENU).blockedTrackers)
    }

    @Test
    fun `fromSite - same host blocked trackers - only unique trackers included`() {
        val site = buildSite(SITE_URL)
        val event = TrackingEvent(
            documentUrl = "http://www.example.com",
            trackerUrl = "http://www.tracker.com/tracker.js",
            categories = emptyList(),
            entity = null,
            surrogateId = null,
            status = TrackerStatus.BLOCKED,
            type = TrackerType.OTHER,
        )
        val anotherEvent = TrackingEvent(
            documentUrl = "http://www.example.com/test",
            trackerUrl = "http://www.tracker.com/tracker2.js",
            categories = emptyList(),
            entity = null,
            surrogateId = null,
            status = TrackerStatus.BLOCKED,
            type = TrackerType.OTHER,
        )
        site.trackerDetected(event)
        site.trackerDetected(anotherEvent)
        assertEquals("tracker.com", BrokenSiteData.fromSite(site, reportFlow = MENU).blockedTrackers)
    }

    @Test
    fun `fromSite - site has blocked CNAMEd trackers - blocked trackers exist`() {
        val site = buildSite(SITE_URL)
        val event = TrackingEvent(
            documentUrl = "http://www.example.com",
            trackerUrl = ".tracker.com/tracker.js",
            categories = emptyList(),
            entity = null,
            surrogateId = null,
            status = TrackerStatus.BLOCKED,
            type = TrackerType.OTHER,
        )
        site.trackerDetected(event)
        assertEquals(".tracker.com", BrokenSiteData.fromSite(site, reportFlow = MENU).blockedTrackers)
    }

    @Test
    fun `fromSite - site has no surrogates - surrogates is empty`() {
        val site = buildSite(SITE_URL)
        val data = BrokenSiteData.fromSite(site, reportFlow = MENU)
        assertTrue(data.surrogates.isEmpty())
    }

    @Test
    fun `fromSite - site has surrogates - surrogates exist`() {
        val surrogate = SurrogateResponse("test.js", true, "surrogate.com/test.js", "", "")
        val anotherSurrogate = SurrogateResponse("test.js", true, "anothersurrogate.com/test.js", "", "")
        val site = buildSite(SITE_URL)
        site.surrogateDetected(surrogate)
        site.surrogateDetected(anotherSurrogate)
        assertEquals("surrogate.com,anothersurrogate.com", BrokenSiteData.fromSite(site, reportFlow = MENU).surrogates)
    }

    @Test
    fun `fromSite - same host surrogates - only unique surrogate included`() {
        val surrogate = SurrogateResponse("test.js", true, "surrogate.com/test.js", "", "")
        val anotherSurrogate = SurrogateResponse("test.js", true, "surrogate.com/test2.js", "", "")
        val site = buildSite(SITE_URL)
        site.surrogateDetected(surrogate)
        site.surrogateDetected(anotherSurrogate)
        assertEquals("surrogate.com", BrokenSiteData.fromSite(site, reportFlow = MENU).surrogates)
    }

    private fun buildSite(
        url: String,
        httpsUpgraded: Boolean = false,
        sslError: Boolean = false,
    ): Site {
        return SiteMonitor(
            url,
            "",
            upgradedHttps = httpsUpgraded,
            mockAllowListRepository,
            mockContentBlocking,
            mockBypassedSSLCertificatesRepository,
            coroutineRule.testScope,
            coroutineRule.testDispatcherProvider,
        )
    }

    companion object {
        private const val SITE_URL = "foo.com"
    }
}
