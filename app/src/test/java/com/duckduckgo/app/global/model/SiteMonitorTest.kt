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

package com.duckduckgo.app.global.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.browser.certificates.BypassedSSLCertificatesRepository
import com.duckduckgo.app.global.model.PrivacyShield.PROTECTED
import com.duckduckgo.app.global.model.PrivacyShield.UNKNOWN
import com.duckduckgo.app.global.model.PrivacyShield.UNPROTECTED
import com.duckduckgo.app.privacy.db.UserAllowListRepository
import com.duckduckgo.app.privacy.model.HttpsStatus
import com.duckduckgo.app.privacy.model.TestingEntity
import com.duckduckgo.app.surrogates.SurrogateResponse
import com.duckduckgo.app.trackerdetection.model.Entity
import com.duckduckgo.app.trackerdetection.model.TrackerStatus
import com.duckduckgo.app.trackerdetection.model.TrackerType
import com.duckduckgo.app.trackerdetection.model.TrackingEvent
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.privacy.config.api.ContentBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SiteMonitorTest {
    @get:Rule
    var coroutineRule = CoroutineTestRule()

    companion object {
        private const val document = "http://example.com"
        private const val httpDocument = document
        private const val httpsDocument = "https://example.com"
        private const val malformedDocument = "[example com]"

        private const val trackerA = "http://standalonetrackerA.com/script.js"
        private const val trackerB = "http://standalonetrackerB.com/script.js"
        private const val trackerC = "http://standalonetrackerC.com/script.js"
        private const val trackerD = "http://standalonetrackerD.com/script.js"
        private const val trackerE = "http://standalonetrackerE.com/script.js"
        private const val trackerF = "http://standalonetrackerF.com/script.js"

        private const val majorNetworkTracker = "http://majorNetworkTracker.com/script.js"

        private val network = TestingEntity("Network", "Network", 1.0)
        private val majorNetwork = TestingEntity("MajorNetwork", "MajorNetwork", Entity.MAJOR_NETWORK_PREVALENCE + 1)
    }

    private val mockAllowListRepository: UserAllowListRepository = mock()

    private val mockContentBlocking: ContentBlocking = mock()

    private val mockBypassedSSLCertificatesRepository: BypassedSSLCertificatesRepository = mock()

    @Test
    fun `whenUrlIsHttpsThenHttpsStatusIsSecure - https status secure`() {
        val testee = SiteMonitor(
            url = httpsDocument,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        assertEquals(HttpsStatus.SECURE, testee.https)
    }

    @Test
    fun `whenUrlIsHttpThenHttpsStatusIsNone - https status none`() {
        val testee = SiteMonitor(
            url = httpDocument,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        assertEquals(HttpsStatus.NONE, testee.https)
    }

    @Test
    fun `whenUrlIsHttpsWithHttpResourcesThenHttpsStatusIsMixed - has http resources - mixed`() {
        val testee = SiteMonitor(
            url = httpsDocument,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        testee.hasHttpResources = true
        assertEquals(HttpsStatus.MIXED, testee.https)
    }

    @Test
    fun `whenUrlIsMalformedThenHttpsStatusIsNone - https status none`() {
        val testee = SiteMonitor(
            url = malformedDocument,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        assertEquals(HttpsStatus.NONE, testee.https)
    }

    @Test
    fun `SiteMonitor - url is correct`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        assertEquals(document, testee.url)
    }

    @Test
    fun `whenSiteMonitorCreated - tracker count is zero`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        assertEquals(0, testee.trackerCount)
    }

    @Test
    fun `trackerDetected - tracker count incremented`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerA,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerB,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.USER_ALLOWED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerC,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.ALLOWED,
                type = TrackerType.OTHER,
            ),
        )
        assertEquals(1, testee.trackerCount)
    }

    @Test
    fun `trackerDetected - all trackers blocked - true`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerA,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerB,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerC,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.ALLOWED,
                type = TrackerType.OTHER,
            ),
        )
        Assert.assertTrue(testee.allTrackersBlocked)
    }

    @Test
    fun `trackerDetected - at least one tracker allowed by user - all trackers blocked is false`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerA,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerB,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.USER_ALLOWED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerC,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.ALLOWED,
                type = TrackerType.OTHER,
            ),
        )
        assertFalse(testee.allTrackersBlocked)
    }

    @Test
    fun `trackerDetected - major network count zero`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerA,
                categories = null,
                entity = network,
                surrogateId = null,
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
            ),
        )
        assertEquals(0, testee.majorNetworkCount)
    }

    @Test
    fun `trackerDetected - major network tracker detected - one`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = majorNetworkTracker,
                categories = null,
                entity = majorNetwork,
                surrogateId = null,
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
            ),
        )
        assertEquals(1, testee.majorNetworkCount)
    }

    @Test
    fun `trackerDetected - duplicate major network detected - still one`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerA,
                categories = null,
                entity = majorNetwork,
                surrogateId = null,
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerB,
                categories = null,
                entity = majorNetwork,
                surrogateId = null,
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
            ),
        )
        assertEquals(1, testee.majorNetworkCount)
    }

    @Test
    fun `whenSiteCreated - site monitor upgraded https is false`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        assertFalse(testee.upgradedHttps)
    }

    @Test
    fun `whenSiteCreatedThenSurrogatesSizeIsZero - site created - size is zero`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        assertEquals(0, testee.surrogates.size)
    }

    @Test
    fun `surrogateDetected - surrogates list incremented`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        testee.surrogateDetected(SurrogateResponse())
        assertEquals(1, testee.surrogates.size)
    }

    @Test
    fun `trackerDetected - other domains loaded count incremented`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerA,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.ALLOWED,
                type = TrackerType.AD,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerB,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.ALLOWED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerC,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
            ),
        )
        assertEquals(2, testee.otherDomainsLoadedCount)
    }

    @Test
    fun `trackerDetected - special domains loaded count incremented`() {
        val testee = SiteMonitor(
            url = document,
            title = null,
            userAllowListRepository = mockAllowListRepository,
            contentBlocking = mockContentBlocking,
            bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
            appCoroutineScope = coroutineRule.testScope,
            dispatcherProvider = coroutineRule.testDispatcherProvider,
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerA,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.AD_ALLOWED,
                type = TrackerType.AD,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerB,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.SITE_BREAKAGE_ALLOWED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerC,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.SAME_ENTITY_ALLOWED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerD,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.USER_ALLOWED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerE,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
            ),
        )
        testee.trackerDetected(
            TrackingEvent(
                documentUrl = document,
                trackerUrl = trackerF,
                categories = null,
                entity = null,
                surrogateId = null,
                status = TrackerStatus.ALLOWED,
                type = TrackerType.OTHER,
            ),
        )
        assertEquals(4, testee.specialDomainsLoadedCount)
    }

    @Test
    fun `whenSiteBelongsToUserAllowList - privacy shield unprotected`() {
        val testee = givenASiteMonitor(url = document)
        whenever(mockAllowListRepository.isDomainInUserAllowList(document)).thenReturn(true)

        assertEquals(UNPROTECTED, testee.privacyProtection())
    }

    @Test
    fun `givenASiteMonitor - site is httpt - privacy shield unprotected`() {
        val testee = givenASiteMonitor(url = httpDocument)

        assertEquals(UNPROTECTED, testee.privacyProtection())
    }

    @Test
    fun `whenSiteIsNotExceptionOrHttpButFullDetailsNotAvailableThenReturnUnknown - privacy protection unknown`() {
        val testee = givenASiteMonitor(url = httpsDocument)

        assertEquals(UNKNOWN, testee.privacyProtection())
    }

    @Test
    fun `updatePrivacyData - site is major network - privacy shield protected`() {
        val testee = givenASiteMonitor(url = httpsDocument)

        testee.updatePrivacyData(givenSitePrivacyData(entity = majorNetwork))

        assertEquals(PROTECTED, testee.privacyProtection())
    }

    @Test
    fun `updatePrivacyData - privacy issues not found - privacy shield protected`() {
        val testee = givenASiteMonitor(url = httpsDocument)

        testee.updatePrivacyData(givenSitePrivacyData(entity = network))

        assertEquals(PROTECTED, testee.privacyProtection())
    }

    @Test
    fun `whenUserBypassedSslCertificate - privacy shield unprotected`() {
        val testee = givenASiteMonitor(url = document)
        whenever(mockBypassedSSLCertificatesRepository.contains(document)).thenReturn(false)

        assertEquals(UNPROTECTED, testee.privacyProtection())
    }

    fun givenASiteMonitor(
        url: String = document,
        title: String? = null,
        upgradedHttps: Boolean = false,
    ) = SiteMonitor(
        url = url,
        title = title,
        upgradedHttps = upgradedHttps,
        userAllowListRepository = mockAllowListRepository,
        contentBlocking = mockContentBlocking,
        bypassedSSLCertificatesRepository = mockBypassedSSLCertificatesRepository,
        appCoroutineScope = coroutineRule.testScope,
        dispatcherProvider = coroutineRule.testDispatcherProvider,
    )

    fun givenSitePrivacyData(
        url: String = document,
        entity: Entity? = null,
        prevalence: Double? = null,
    ) = SitePrivacyData(
        url = url,
        entity = entity,
        prevalence = null,
    )
}
