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

package com.duckduckgo.app.cta.ui

import android.content.res.Resources
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.duckduckgo.app.global.install.AppInstallStore
import com.duckduckgo.app.global.model.Site
import com.duckduckgo.app.global.model.orderedTrackerBlockedEntities
import com.duckduckgo.app.onboarding.store.OnboardingStore
import com.duckduckgo.app.privacy.model.HttpsStatus
import com.duckduckgo.app.privacy.model.TestingEntity
import com.duckduckgo.app.statistics.pixels.Pixel.PixelParameter.CTA_SHOWN
import com.duckduckgo.app.survey.model.Survey
import com.duckduckgo.app.trackerdetection.model.Entity
import com.duckduckgo.app.trackerdetection.model.TrackerStatus
import com.duckduckgo.app.trackerdetection.model.TrackerType
import com.duckduckgo.app.trackerdetection.model.TrackingEvent
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CtaTest {

    @Mock
    private lateinit var mockOnboardingStore: OnboardingStore

    @Mock
    private lateinit var mockAppInstallStore: AppInstallStore

    @Mock
    private lateinit var mockActivity: FragmentActivity

    @Mock
    private lateinit var mockResources: Resources

    @Before
    fun before() {
        MockitoAnnotations.openMocks(this)

        whenever(mockActivity.resources).thenReturn(mockResources)
        whenever(mockResources.getQuantityString(any(), any())).thenReturn("withZero")
        whenever(mockResources.getQuantityString(any(), any(), any())).thenReturn("withMultiple")
    }

    @Test
    fun `pixelOkParameters - cta is survey return empty ok parameters`() {
        val testee = HomePanelCta.Survey(Survey("abc", "http://example.com", 1, Survey.Status.SCHEDULED))
        assertTrue(testee.pixelOkParameters().isEmpty())
    }

    @Test
    fun `pixelCancelParameters - cta is survey return empty - returns empty`() {
        val testee = HomePanelCta.Survey(Survey("abc", "http://example.com", 1, Survey.Status.SCHEDULED))
        assertTrue(testee.pixelCancelParameters().isEmpty())
    }

    @Test
    fun `pixelShownParameters - cta is survey return empty shown parameters`() {
        val testee = HomePanelCta.Survey(Survey("abc", "http://example.com", 1, Survey.Status.SCHEDULED))
        assertTrue(testee.pixelShownParameters().isEmpty())
    }

    @Test
    fun `pixelOkParameters - auto return empty`() {
        val testee = HomePanelCta.AddWidgetAuto
        assertTrue(testee.pixelOkParameters().isEmpty())
    }

    @Test
    fun `pixelCancelParameters - auto return empty`() {
        val testee = HomePanelCta.AddWidgetAuto
        assertTrue(testee.pixelCancelParameters().isEmpty())
    }

    @Test
    fun `pixelShownParameters - auto return empty - is empty`() {
        val testee = HomePanelCta.AddWidgetAuto
        assertTrue(testee.pixelShownParameters().isEmpty())
    }

    @Test
    fun `pixelOkParameters - empty ok parameters`() {
        val testee = HomePanelCta.AddWidgetInstructions
        assertTrue(testee.pixelOkParameters().isEmpty())
    }

    @Test
    fun `pixelCancelParameters - add widget instructions return empty - empty`() {
        val testee = HomePanelCta.AddWidgetInstructions
        assertTrue(testee.pixelCancelParameters().isEmpty())
    }

    @Test
    fun `pixelShownParameters - empty shown parameters`() {
        val testee = HomePanelCta.AddWidgetInstructions
        assertTrue(testee.pixelShownParameters().isEmpty())
    }

    @Test
    fun `pixelCancelParameters - cta is bubble type - correct cancel parameters`() {
        val testee = DaxBubbleCta.DaxIntroCta(mockOnboardingStore, mockAppInstallStore)
        val value = testee.pixelCancelParameters()

        assertEquals(1, value.size)
        assertTrue(value.containsKey(CTA_SHOWN))
        assertEquals(testee.ctaPixelParam, value[CTA_SHOWN])
    }

    @Test
    fun `pixelOkParameters - cta is bubble type - correct ok parameters`() {
        val testee = DaxBubbleCta.DaxIntroCta(mockOnboardingStore, mockAppInstallStore)
        val value = testee.pixelOkParameters()

        assertEquals(1, value.size)
        assertTrue(value.containsKey(CTA_SHOWN))
        assertEquals(testee.ctaPixelParam, value[CTA_SHOWN])
    }

    @Test
    fun `pixelShownParameters - cta is bubble type - correct shown parameters`() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(null)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis())

        val testee = DaxBubbleCta.DaxIntroCta(mockOnboardingStore, mockAppInstallStore)
        val expectedValue = "${testee.ctaPixelParam}:0"

        val value = testee.pixelShownParameters()
        assertEquals(1, value.size)
        assertTrue(value.containsKey(CTA_SHOWN))
        assertEquals(expectedValue, value[CTA_SHOWN])
    }

    @Test
    fun `addCtaToHistory - return correct value`() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(null)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis())

        val testee = DaxBubbleCta.DaxEndCta(mockOnboardingStore, mockAppInstallStore)
        val value = testee.addCtaToHistory("test")
        assertEquals("test:0", value)
    }

    @Test
    fun `addCtaToHistory - day 3 - returns correct value`() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(null)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3))

        val testee = DaxBubbleCta.DaxEndCta(mockOnboardingStore, mockAppInstallStore)
        val value = testee.addCtaToHistory("test")
        assertEquals("test:3", value)
    }

    @Test
    fun `addCtaToHistory - day 4 - return 3 as day value`() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(null)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4))

        val testee = DaxBubbleCta.DaxEndCta(mockOnboardingStore, mockAppInstallStore)
        val value = testee.addCtaToHistory("test")
        assertEquals("test:3", value)
    }

    @Test
    fun `addCtaToHistory - contains history - concatenate new value`() {
        val ctaHistory = "s:0-t:1"
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(ctaHistory)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))

        val testee = DaxBubbleCta.DaxEndCta(mockOnboardingStore, mockAppInstallStore)
        val value = testee.addCtaToHistory("test")
        val expectedValue = "$ctaHistory-test:1"

        assertEquals(expectedValue, value)
    }

    @Test
    fun `pixelShownParameters - cta is bubble type - concatenate journey stored value in pixel`() {
        val existingJourney = "s:0-t:1"
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(existingJourney)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))
        val testee = DaxBubbleCta.DaxEndCta(mockOnboardingStore, mockAppInstallStore)
        val expectedValue = "$existingJourney-${testee.ctaPixelParam}:1"

        val value = testee.pixelShownParameters()
        assertEquals(expectedValue, value[CTA_SHOWN])
    }

    @Test
    fun `canSendPixel - onboarding dialog journey s·0 - returns true`() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn("s:0")
        val testee = DaxBubbleCta.DaxEndCta(mockOnboardingStore, mockAppInstallStore)
        assertTrue(testee.canSendShownPixel())
    }

    @Test
    fun `canSend - onboarding dialog journey substring - returns true`() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn("s:0-te:0")
        val testee = DaxBubbleCta.DaxEndCta(mockOnboardingStore, mockAppInstallStore)
        assertTrue(testee.canSendShownPixel())
    }

    @Test
    fun `canSendShownPixel - onboarding dialog journey - returns false`() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn("i:0-e:0-s:0")

        val testee = DaxBubbleCta.DaxEndCta(mockOnboardingStore, mockAppInstallStore)
        assertFalse(testee.canSendShownPixel())
    }

    @Test
    fun `pixelCancelParameters - cta is dialog type - correct cancel parameters`() {
        val testee = DaxDialogCta.DaxSerpCta(mockOnboardingStore, mockAppInstallStore)

        val value = testee.pixelCancelParameters()
        assertEquals(1, value.size)
        assertTrue(value.containsKey(CTA_SHOWN))
        assertEquals(testee.ctaPixelParam, value[CTA_SHOWN])
    }

    @Test
    fun `pixelOkParameters - cta is dialog type - correct ok parameters`() {
        val testee = DaxDialogCta.DaxSerpCta(mockOnboardingStore, mockAppInstallStore)

        val value = testee.pixelOkParameters()
        assertEquals(1, value.size)
        assertTrue(value.containsKey(CTA_SHOWN))
        assertEquals(testee.ctaPixelParam, value[CTA_SHOWN])
    }

    @Test
    fun `pixelShownParameters - cta is dialog type - correct shown parameters`() {
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(null)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis())
        val testee = DaxDialogCta.DaxSerpCta(mockOnboardingStore, mockAppInstallStore)
        val expectedValue = "${testee.ctaPixelParam}:0"

        val value = testee.pixelShownParameters()
        assertEquals(1, value.size)
        assertTrue(value.containsKey(CTA_SHOWN))
        assertEquals(expectedValue, value[CTA_SHOWN])
    }

    @Test
    fun `pixelShownParameters - cta is dialog type - concatenates journey stored value in pixel`() {
        val existingJourney = "s:0-t:1"
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(existingJourney)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))
        val testee = DaxDialogCta.DaxSerpCta(mockOnboardingStore, mockAppInstallStore)
        val expectedValue = "$existingJourney-${testee.ctaPixelParam}:1"

        val value = testee.pixelShownParameters()
        assertEquals(expectedValue, value[CTA_SHOWN])
    }

    @Test
    fun `getDaxText - more than two trackers blocked - first two with multiple string`() {
        val trackers = listOf(
            TestingEntity("Facebook", "Facebook", 9.0),
            TestingEntity("Other", "Other", 9.0),
            TestingEntity("Amazon", "Amazon", 9.0),
        )

        val testee = DaxDialogCta.DaxTrackersBlockedCta(mockOnboardingStore, mockAppInstallStore, trackers, "http://www.trackers.com")
        val value = testee.getDaxText(mockActivity)

        assertEquals("<b>Facebook, Other</b>withMultiple", value)
    }

    @Test
    fun `getDaxText - two trackers blocked - with zero string`() {
        val trackers = listOf(
            TestingEntity("Facebook", "Facebook", 9.0),
            TestingEntity("Other", "Other", 9.0),
        )

        val testee = DaxDialogCta.DaxTrackersBlockedCta(mockOnboardingStore, mockAppInstallStore, trackers, "http://www.trackers.com")
        val value = testee.getDaxText(mockActivity)

        assertEquals("<b>Facebook, Other</b>withZero", value)
    }

    @Test
    fun `getDaxText - trackers blocked - returns them sorting by prevalence`() {
        val trackers = listOf(
            TrackingEvent(
                documentUrl = "facebook.com",
                trackerUrl = "facebook.com",
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
                entity = TestingEntity("Facebook", "Facebook", 3.0),
                categories = null,
                surrogateId = null,
            ),
            TrackingEvent(
                documentUrl = "other.com",
                trackerUrl = "other.com",
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
                entity = TestingEntity("Other", "Other", 9.0),
                categories = null,
                surrogateId = null,
            ),
        )
        val site = site(events = trackers)

        val testee =
            DaxDialogCta.DaxTrackersBlockedCta(
                mockOnboardingStore,
                mockAppInstallStore,
                site.orderedTrackerBlockedEntities(),
                "http://www.trackers.com",
            )
        val value = testee.getDaxText(mockActivity)

        assertEquals("<b>Other, Facebook</b>withZero", value)
    }

    @Test
    fun `getDaxText - trackers blocked - only display name`() {
        val trackers = listOf(
            TrackingEvent(
                documentUrl = "facebook.com",
                trackerUrl = "facebook.com",
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
                entity = TestingEntity("Facebook", "Facebook", 3.0),
                categories = null,
                surrogateId = null,
            ),
            TrackingEvent(
                documentUrl = "other.com",
                trackerUrl = "other.com",
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
                entity = TestingEntity("Other", "", 9.0),
                categories = null,
                surrogateId = null,
            ),
        )
        val site = site(events = trackers)

        val testee = DaxDialogCta.DaxTrackersBlockedCta(
            mockOnboardingStore,
            mockAppInstallStore,
            site.orderedTrackerBlockedEntities(),
            "http://www.trackers.com",
        )
        val value = testee.getDaxText(mockActivity)

        assertEquals("<b>Facebook</b>withZero", value)
    }

    @Test
    fun `getDaxText - trackers blocked - only trackers blocked`() {
        val trackers = listOf(
            TrackingEvent(
                documentUrl = "facebook.com",
                trackerUrl = "facebook.com",
                status = TrackerStatus.ALLOWED,
                type = TrackerType.OTHER,
                entity = TestingEntity("Facebook", "Facebook", 3.0),
                categories = null,
                surrogateId = null,
            ),
            TrackingEvent(
                documentUrl = "other.com",
                trackerUrl = "other.com",
                status = TrackerStatus.BLOCKED,
                type = TrackerType.OTHER,
                entity = TestingEntity("Other", "Other", 9.0),
                categories = null,
                surrogateId = null,
            ),
        )
        val site = site(events = trackers)

        val testee = DaxDialogCta.DaxTrackersBlockedCta(
            mockOnboardingStore,
            mockAppInstallStore,
            site.orderedTrackerBlockedEntities(),
            "http://www.trackers.com",
        )
        val value = testee.getDaxText(mockActivity)

        assertEquals("<b>Other</b>withZero", value)
    }

    @Test
    fun `getDaxText - multiple trackers from same network blocked - returns one with zero string`() {
        val trackers = listOf(
            TestingEntity("Facebook", "Facebook", 9.0),
            TestingEntity("Facebook", "Facebook", 9.0),
            TestingEntity("Facebook", "Facebook", 9.0),
        )

        val testee = DaxDialogCta.DaxTrackersBlockedCta(mockOnboardingStore, mockAppInstallStore, trackers, "http://www.trackers.com")
        val value = testee.getDaxText(mockActivity)

        assertEquals("<b>Facebook</b>withZero", value)
    }

    @Test
    fun `pixelShown - try clear data cta shown - concatenate journey stored value in pixel`() {
        val existingJourney = "s:0-t:1"
        whenever(mockOnboardingStore.onboardingDialogJourney).thenReturn(existingJourney)
        whenever(mockAppInstallStore.installTimestamp).thenReturn(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))
        val testee = DaxFireDialogCta.TryClearDataCta(mockOnboardingStore, mockAppInstallStore)
        val expectedValue = "$existingJourney-${testee.ctaPixelParam}:1"

        val value = testee.pixelShownParameters()

        assertEquals(expectedValue, value[CTA_SHOWN])
    }

    private fun site(
        url: String = "http://www.test.com",
        uri: Uri? = Uri.parse(url),
        https: HttpsStatus = HttpsStatus.SECURE,
        trackerCount: Int = 0,
        events: List<TrackingEvent> = emptyList(),
        majorNetworkCount: Int = 0,
        allTrackersBlocked: Boolean = true,
        entity: Entity? = null,
    ): Site {
        val site: Site = mock()
        whenever(site.url).thenReturn(url)
        whenever(site.uri).thenReturn(uri)
        whenever(site.https).thenReturn(https)
        whenever(site.entity).thenReturn(entity)
        whenever(site.trackingEvents).thenReturn(events)
        whenever(site.trackerCount).thenReturn(trackerCount)
        whenever(site.majorNetworkCount).thenReturn(majorNetworkCount)
        whenever(site.allTrackersBlocked).thenReturn(allTrackersBlocked)
        return site
    }
}
