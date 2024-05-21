/*
 * Copyright (c) 2023 DuckDuckGo
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

package com.duckduckgo.privacyprotectionspopup.impl

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.duckduckgo.app.browser.DuckDuckGoUrlDetector
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.common.utils.AppUrl
import com.duckduckgo.common.utils.extractDomain
import com.duckduckgo.feature.toggles.api.Toggle
import com.duckduckgo.feature.toggles.api.Toggle.State
import com.duckduckgo.privacyprotectionspopup.api.PrivacyProtectionsPopupUiEvent.DISABLE_PROTECTIONS_CLICKED
import com.duckduckgo.privacyprotectionspopup.api.PrivacyProtectionsPopupUiEvent.DISMISSED
import com.duckduckgo.privacyprotectionspopup.api.PrivacyProtectionsPopupUiEvent.DISMISS_CLICKED
import com.duckduckgo.privacyprotectionspopup.api.PrivacyProtectionsPopupUiEvent.DONT_SHOW_AGAIN_CLICKED
import com.duckduckgo.privacyprotectionspopup.api.PrivacyProtectionsPopupUiEvent.PRIVACY_DASHBOARD_CLICKED
import com.duckduckgo.privacyprotectionspopup.api.PrivacyProtectionsPopupViewState
import com.duckduckgo.privacyprotectionspopup.impl.PrivacyProtectionsPopupExperimentVariant.CONTROL
import com.duckduckgo.privacyprotectionspopup.impl.PrivacyProtectionsPopupExperimentVariant.TEST
import com.duckduckgo.privacyprotectionspopup.impl.db.PopupDismissDomainRepository
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class PrivacyProtectionsPopupManagerImplTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val featureFlag = FakePrivacyProtectionsPopupFeature()

    private val protectionsStateProvider = FakeProtectionsStateProvider()

    private val timeProvider = FakeTimeProvider()

    private val popupDismissDomainRepository = FakePopupDismissDomainRepository()

    private val userAllowListRepository = FakeUserAllowlistRepository()

    private val dataStore = FakePrivacyProtectionsPopupDataStore()

    private val duckDuckGoUrlDetector = FakeDuckDuckGoUrlDetector()

    private val variantRandomizer = FakePrivacyProtectionsPopupExperimentVariantRandomizer()

    private val pixels: PrivacyProtectionsPopupPixels = mock()

    private val subject = PrivacyProtectionsPopupManagerImpl(
        appCoroutineScope = coroutineRule.testScope,
        featureFlag = featureFlag,
        dataProvider = PrivacyProtectionsPopupManagerDataProviderImpl(
            protectionsStateProvider = protectionsStateProvider,
            popupDismissDomainRepository = popupDismissDomainRepository,
            dataStore = dataStore,
        ),
        timeProvider = timeProvider,
        popupDismissDomainRepository = popupDismissDomainRepository,
        userAllowListRepository = userAllowListRepository,
        dataStore = dataStore,
        duckDuckGoUrlDetector = duckDuckGoUrlDetector,
        variantRandomizer = variantRandomizer,
        pixels = pixels,
    )

    @Test
    fun `onPageRefreshTriggeredByUser - view state updated - emits update to show popup`() = runTest {
        val toggleUsedAt = timeProvider.time - Duration.ofDays(32)
        dataStore.setToggleUsageTimestamp(toggleUsedAt)

        subject.viewState.test {
            assertEquals(PrivacyProtectionsPopupViewState.Gone, awaitItem())
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            expectNoEvents()
            subject.onPageRefreshTriggeredByUser()
            assertTrue(awaitItem() is PrivacyProtectionsPopupViewState.Visible)
            expectNoEvents()
        }
    }

    @Test
    fun `onPageRefreshTriggeredByUser - view state updated - popup is shown`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = true)
        }
    }

    @Test
    fun `onPageLoaded - popup not shown - not visible`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://duckduckgo.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)
        }
    }

    @Test
    fun `onPageLoaded - feature disabled - popup not shown`() = runTest {
        featureFlag.enabled = false
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)
        }
    }

    @Test
    fun `onPageLoaded - protections disabled - popup not shown`() = runTest {
        protectionsStateProvider.protectionsEnabled = false

        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)
        }
    }

    @Test
    fun `onPageLoaded - popup not shown`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)
        }
    }

    @Test
    fun `onPageLoaded - page loaded with http error - popup not shown`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = listOf(500), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)
        }
    }

    @Test
    fun `onPageLoaded - page loaded with browser error - popup not shown`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = true)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)
        }
    }

    @Test
    fun `onPageChanged - popup not dismissed`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = true)

            subject.onPageLoaded(url = "https://www.example2.com", httpErrorCodes = emptyList(), hasBrowserError = false)

            expectNoEvents()
        }
    }

    @Test
    fun `onUiEvent - view state updated - popup dismissed`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = true)

            subject.onUiEvent(DISMISSED)

            assertPopupVisible(visible = false)
        }
    }

    @Test
    fun `onUiEvent - popup dismissed`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = true)

            subject.onUiEvent(DISMISS_CLICKED)

            assertPopupVisible(visible = false)
            assertStoredPopupDismissTimestamp(url = "https://www.example.com", expectedTimestamp = timeProvider.time)
        }
    }

    @Test
    fun `onDisableProtectionsClicked - popup dismissed`() = runTest {
        subject.viewState.test {
            assertEquals(PrivacyProtectionsPopupViewState.Gone, awaitItem())
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertTrue(awaitItem() is PrivacyProtectionsPopupViewState.Visible)

            subject.onUiEvent(DISABLE_PROTECTIONS_CLICKED)

            assertEquals(PrivacyProtectionsPopupViewState.Gone, awaitItem())
            assertStoredPopupDismissTimestamp(url = "https://www.example.com", expectedTimestamp = timeProvider.time)
        }
    }

    @Test
    fun `onDisableProtectionsClicked - view state updated - domain added to user allowlist`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            assertFalse(userAllowListRepository.isUrlInUserAllowList("https://www.example.com"))
            assertPopupVisible(visible = true)

            subject.onUiEvent(DISABLE_PROTECTIONS_CLICKED)

            assertPopupVisible(visible = false)
            assertTrue(userAllowListRepository.isUrlInUserAllowList("https://www.example.com"))
        }
    }

    @Test
    fun `onPageRefresh - popup dismissed recently - not shown on refresh`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            subject.onUiEvent(DISMISSED)
            assertStoredPopupDismissTimestamp(url = "https://www.example.com", expectedTimestamp = timeProvider.time)

            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)
            assertStoredPopupDismissTimestamp(url = "https://www.example.com", expectedTimestamp = timeProvider.time)
        }
    }

    @Test
    fun `onPageRefresh - popup dismissed more than 24 hours ago for same domain - shown again`() = runTest {
        subject.viewState.test {
            timeProvider.time = Instant.parse("2023-11-29T10:15:30.000Z")
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            subject.onUiEvent(DISMISSED)
            assertStoredPopupDismissTimestamp(url = "https://www.example.com", expectedTimestamp = timeProvider.time)
            timeProvider.time += Duration.ofDays(2)

            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = true)
        }
    }

    @Test
    fun `onPageLoaded - popup dismissed recently - not shown for same domain but will be for other domains`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            subject.onUiEvent(DISMISSED)

            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)

            subject.onPageLoaded(url = "https://www.example2.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = true)

            subject.onUiEvent(DISMISSED)
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)
        }
    }

    @Test
    fun `onPageRefreshTriggeredByUser - view state - popup not shown`() = runTest {
        val protectionsEnabledFlow = MutableSharedFlow<Boolean>()
        protectionsStateProvider.overrideProtectionsEnabledFlow(protectionsEnabledFlow)

        subject.viewState.test {
            assertPopupVisible(visible = false)
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            protectionsEnabledFlow.emit(true)
            expectNoEvents()
        }
    }

    @Test
    fun `onPageRefreshTriggeredByUser - view state updated - popup not shown`() = runTest {
        val protectionsEnabledFlow = MutableSharedFlow<Boolean>()
        protectionsStateProvider.overrideProtectionsEnabledFlow(protectionsEnabledFlow)

        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            timeProvider.time += Duration.ofSeconds(5)
            protectionsEnabledFlow.emit(true)
            assertPopupVisible(visible = false)
        }
    }

    @Test
    fun `onPageRefreshTriggeredByUser - toggle used in last 2 weeks - popup is not shown on refresh`() = runTest {
        val toggleUsedAt = timeProvider.time - Duration.ofDays(10)
        dataStore.setToggleUsageTimestamp(toggleUsedAt)

        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)
        }
    }

    @Test
    fun `onPageRefreshTriggeredByUser - toggle not used in last 2 weeks - popup is shown on refresh`() = runTest {
        val toggleUsedAt = timeProvider.time - Duration.ofDays(32)
        dataStore.setToggleUsageTimestamp(toggleUsedAt)

        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = true)
        }
    }

    @Test
    fun `onPageRefresh - page reloads on refresh with http error - popup is not dismissed`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = true)

            timeProvider.time += Duration.ofSeconds(2)
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = listOf(500), hasBrowserError = false)

            expectNoEvents()
        }
    }

    @Test
    fun `onPageLoaded - popup shown - trigger count incremented`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = true)
            assertEquals(1, dataStore.getPopupTriggerCount())

            subject.onUiEvent(DISMISSED)

            assertPopupVisible(visible = false)

            subject.onPageLoaded(url = "https://www.example2.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = true)
            assertEquals(2, dataStore.getPopupTriggerCount())
        }
    }

    @Test
    fun `onPageLoaded - popup trigger count zero - do not show again option not available`() = runTest {
        dataStore.setPopupTriggerCount(0)

        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertEquals(PrivacyProtectionsPopupViewState.Visible(doNotShowAgainOptionAvailable = false), expectMostRecentItem())
        }
    }

    @Test
    fun `onPageLoaded - popup trigger count greater than zero - do not show again option available`() = runTest {
        dataStore.setPopupTriggerCount(1)

        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertEquals(PrivacyProtectionsPopupViewState.Visible(doNotShowAgainOptionAvailable = true), expectMostRecentItem())
        }
    }

    @Test
    fun `onUiEvent - do not show again clicked - popup is not shown again`() = runTest {
        dataStore.setPopupTriggerCount(1)

        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertEquals(PrivacyProtectionsPopupViewState.Visible(doNotShowAgainOptionAvailable = true), expectMostRecentItem())

            subject.onUiEvent(DONT_SHOW_AGAIN_CLICKED)

            assertPopupVisible(visible = false)
            assertTrue(dataStore.getDoNotShowAgainClicked())

            subject.onPageLoaded(url = "https://www.example2.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            expectNoEvents()
        }
    }

    @Test
    fun `onPageLoaded - popup conditions met and experiment variant is control - not shown`() = runTest {
        dataStore.setExperimentVariant(CONTROL)
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)
        }
    }

    @Test
    fun `onPageLoaded - popup conditions met - initializes variant with random value`() = runTest {
        variantRandomizer.variant = CONTROL
        assertNull(dataStore.getExperimentVariant())

        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = false)
            assertEquals(CONTROL, dataStore.getExperimentVariant())
        }
    }

    @Test
    fun `onPageLoaded - experiment variant assigned - pixel sent`() = runTest {
        variantRandomizer.variant = CONTROL
        assertNull(dataStore.getExperimentVariant())
        var variantIncludedInPixel: PrivacyProtectionsPopupExperimentVariant? = null
        whenever(pixels.reportExperimentVariantAssigned()) doAnswer {
            variantIncludedInPixel = runBlocking { dataStore.getExperimentVariant() }
        }

        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            cancelAndIgnoreRemainingEvents()

            verify(pixels).reportExperimentVariantAssigned()
            assertEquals(CONTROL, variantIncludedInPixel) // Verify that pixel is sent AFTER assigned variant is stored.
        }
    }

    @Test
    fun `onPageLoaded - variant already assigned - pixel not sent`() = runTest {
        dataStore.setExperimentVariant(TEST)
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            assertPopupVisible(visible = true)

            verify(pixels, never()).reportExperimentVariantAssigned()
        }
    }

    @Test
    fun `onPageRefreshTriggeredByUser - popup triggered - pixel sent`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            assertPopupVisible(visible = true)

            verify(pixels).reportPopupTriggered()
        }
    }

    @Test
    fun `onUiEvent - privacy protections disable button clicked - pixel sent`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            assertPopupVisible(visible = true)

            subject.onUiEvent(DISABLE_PROTECTIONS_CLICKED)

            verify(pixels).reportProtectionsDisabled()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onUiEvent - dismiss button clicked - pixel sent`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            assertPopupVisible(visible = true)

            subject.onUiEvent(DISMISS_CLICKED)

            verify(pixels).reportPopupDismissedViaButton()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onUiEvent - popup dismissed via click outside - pixel sent`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            assertPopupVisible(visible = true)

            subject.onUiEvent(DISMISSED)

            verify(pixels).reportPopupDismissedViaClickOutside()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onUiEvent - do not show again button clicked - pixel sent`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            assertPopupVisible(visible = true)

            subject.onUiEvent(DONT_SHOW_AGAIN_CLICKED)

            verify(pixels).reportDoNotShowAgainClicked()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onUiEvent - privacy dashboard opened - pixel sent`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()
            assertPopupVisible(visible = true)

            subject.onUiEvent(PRIVACY_DASHBOARD_CLICKED)

            verify(pixels).reportPrivacyDashboardOpened()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPageRefresh - view state updated - pixel sent`() = runTest {
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            verify(pixels).reportPageRefreshOnPossibleBreakage()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onPageRefresh - feature disabled - no experiment variant - pixel not sent`() = runTest {
        featureFlag.enabled = false
        subject.viewState.test {
            subject.onPageLoaded(url = "https://www.example.com", httpErrorCodes = emptyList(), hasBrowserError = false)
            subject.onPageRefreshTriggeredByUser()

            verify(pixels).reportPageRefreshOnPossibleBreakage()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun ReceiveTurbine<PrivacyProtectionsPopupViewState>.assertPopupVisible(visible: Boolean) {
        if (visible) {
            assertTrue(expectMostRecentItem() is PrivacyProtectionsPopupViewState.Visible)
        } else {
            assertEquals(PrivacyProtectionsPopupViewState.Gone, expectMostRecentItem())
        }
    }

    private suspend fun assertStoredPopupDismissTimestamp(url: String, expectedTimestamp: Instant?) {
        val dismissedAt = popupDismissDomainRepository.getPopupDismissTime(url.extractDomain()!!).first()
        assertEquals(expectedTimestamp, dismissedAt)
    }
}

private class FakePrivacyProtectionsPopupFeature : PrivacyProtectionsPopupFeature {

    var enabled = true

    override fun self(): Toggle = object : Toggle {
        override fun isEnabled(): Boolean = enabled
        override fun setEnabled(state: State) = throw UnsupportedOperationException()
        override fun getRawStoredState(): State? = throw UnsupportedOperationException()
    }
}

private class FakeProtectionsStateProvider : ProtectionsStateProvider {

    private var _protectionsEnabled = MutableStateFlow(true)

    private var protectionsEnabledOverride: Flow<Boolean>? = null

    var protectionsEnabled: Boolean
        set(value) {
            check(protectionsEnabledOverride == null)
            _protectionsEnabled.value = value
        }
        get() = _protectionsEnabled.value

    fun overrideProtectionsEnabledFlow(flow: Flow<Boolean>) {
        protectionsEnabledOverride = flow
    }

    override fun areProtectionsEnabled(domain: String): Flow<Boolean> =
        protectionsEnabledOverride ?: _protectionsEnabled.asStateFlow()
}

private class FakePopupDismissDomainRepository : PopupDismissDomainRepository {

    private val data = MutableStateFlow(emptyMap<String, Instant>())

    override fun getPopupDismissTime(domain: String): Flow<Instant?> =
        data.map { it[domain] }.distinctUntilChanged()

    override suspend fun setPopupDismissTime(
        domain: String,
        time: Instant,
    ) {
        data.update { it + (domain to time) }
    }

    override suspend fun removeEntriesOlderThan(time: Instant) =
        throw UnsupportedOperationException()

    override suspend fun removeAllEntries() =
        throw UnsupportedOperationException()
}

private class FakeDuckDuckGoUrlDetector : DuckDuckGoUrlDetector {
    override fun isDuckDuckGoUrl(url: String): Boolean = AppUrl.Url.HOST == Uri.parse(url).host

    override fun isDuckDuckGoEmailUrl(url: String): Boolean = throw UnsupportedOperationException()
    override fun isDuckDuckGoQueryUrl(uri: String): Boolean = throw UnsupportedOperationException()
    override fun isDuckDuckGoStaticUrl(uri: String): Boolean = throw UnsupportedOperationException()
    override fun extractQuery(uriString: String): String? = throw UnsupportedOperationException()
    override fun isDuckDuckGoVerticalUrl(uri: String): Boolean = throw UnsupportedOperationException()
    override fun extractVertical(uriString: String): String? = throw UnsupportedOperationException()
}

private class FakePrivacyProtectionsPopupExperimentVariantRandomizer : PrivacyProtectionsPopupExperimentVariantRandomizer {
    var variant = TEST

    override fun getRandomVariant(): PrivacyProtectionsPopupExperimentVariant = variant
}
