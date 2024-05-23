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

package com.duckduckgo.networkprotection.impl.pixels

import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.common.test.api.InMemorySharedPreferences
import com.duckduckgo.data.store.api.SharedPreferencesProvider
import com.duckduckgo.networkprotection.impl.cohort.NetpCohortStore
import com.duckduckgo.networkprotection.impl.pixels.NetworkProtectionPixelNames.NETP_ENABLE_UNIQUE
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RealNetworkProtectionPixelTest {
    @Mock
    private lateinit var pixel: Pixel

    @Mock
    private lateinit var sharedPreferencesProvider: SharedPreferencesProvider

    private lateinit var fakeNetpCohortStore: FakeNetpCohortStore

    private lateinit var testee: RealNetworkProtectionPixel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        fakeNetpCohortStore = FakeNetpCohortStore().apply {
            cohortLocalDate = LocalDate.now()
        }
        val prefs = InMemorySharedPreferences()
        whenever(
            sharedPreferencesProvider.getSharedPreferences(eq("com.duckduckgo.networkprotection.pixels.v1"), eq(true), eq(false)),
        ).thenReturn(prefs)
        testee = RealNetworkProtectionPixel(
            pixel,
            sharedPreferencesProvider,
            fakeNetpCohortStore,
            object : ETTimestamp() {
                override fun formattedTimestamp(): String {
                    return "2000-01-01"
                }
            },
        )
    }

    @Test
    fun `reportErrorInRegistration - called twice - fire count pixel twice and daily pixel once`() {
        testee.reportErrorInRegistration()
        testee.reportErrorInRegistration()

        verify(pixel).enqueueFire("m_netp_ev_backend_api_error_device_registration_failed_d", mapOf("ts" to "2000-01-01"))
        verify(pixel, times(2)).enqueueFire("m_netp_ev_backend_api_error_device_registration_failed_c", mapOf("ts" to "2000-01-01"))
    }

    @Test
    fun `reportErrorWgInvalidState - called twice - fire count pixel twice and daily pixel once`() {
        testee.reportErrorWgInvalidState()
        testee.reportErrorWgInvalidState()

        verify(pixel).enqueueFire("m_netp_ev_wireguard_error_invalid_state_d", mapOf("ts" to "2000-01-01"))
        verify(pixel, times(2)).enqueueFire("m_netp_ev_wireguard_error_invalid_state_c", mapOf("ts" to "2000-01-01"))
    }

    @Test
    fun `reportErrorWgBackendCantStart - called twice - fire count pixel twice and daily pixel once`() {
        testee.reportErrorWgBackendCantStart()
        testee.reportErrorWgBackendCantStart()

        verify(pixel).enqueueFire("m_netp_ev_wireguard_error_cannot_start_wireguard_backend_d", mapOf("ts" to "2000-01-01"))
        verify(pixel, times(2)).enqueueFire("m_netp_ev_wireguard_error_cannot_start_wireguard_backend_c", mapOf("ts" to "2000-01-01"))
    }

    @Test
    fun `reportEnabled - called twice - fire daily pixel once`() {
        testee.reportEnabled()
        testee.reportEnabled()
        val baseDate = LocalDate.of(2023, 1, 1)
        val week = ChronoUnit.WEEKS.between(baseDate, fakeNetpCohortStore.cohortLocalDate!!) + 1

        verify(pixel).enqueueFire(
            "m_netp_ev_enabled_d",
            mapOf("cohort" to "week-$week", "ts" to "2000-01-01"),
        )
        verify(pixel).enqueueFire(
            NETP_ENABLE_UNIQUE,
            mapOf("cohort" to "week-$week", "ts" to "2000-01-01"),
        )
    }

    @Test
    fun `reportEnabled - send cohort from 2023-01-01`() {
        testee.reportEnabled()
        val baseDate = LocalDate.of(2023, 1, 1)
        val week = ChronoUnit.WEEKS.between(baseDate, fakeNetpCohortStore.cohortLocalDate!!) + 1

        verify(pixel).enqueueFire(
            "m_netp_ev_enabled_d",
            mapOf("cohort" to "week-$week", "ts" to "2000-01-01"),
        )
        verify(pixel).enqueueFire(
            NETP_ENABLE_UNIQUE,
            mapOf("cohort" to "week-$week", "ts" to "2000-01-01"),
        )
    }

    @Test
    fun `reportEnabled - cohort at boundary - do not coalesce`() {
        fakeNetpCohortStore.cohortLocalDate = LocalDate.now().minusWeeks(6)
        testee.reportEnabled()
        val baseDate = LocalDate.of(2023, 1, 1)
        val week = ChronoUnit.WEEKS.between(baseDate, fakeNetpCohortStore.cohortLocalDate!!) + 1

        verify(pixel).enqueueFire(
            "m_netp_ev_enabled_d",
            mapOf("cohort" to "week-$week", "ts" to "2000-01-01"),
        )
        verify(pixel).enqueueFire(
            NETP_ENABLE_UNIQUE,
            mapOf("cohort" to "week-$week", "ts" to "2000-01-01"),
        )
    }

    @Test
    fun `reportEnabled - past the week boundary - coalesce cohort`() {
        fakeNetpCohortStore.cohortLocalDate = LocalDate.now().minusWeeks(7)
        testee.reportEnabled()

        verify(pixel).enqueueFire(
            "m_netp_ev_enabled_d",
            mapOf("cohort" to "", "ts" to "2000-01-01"),
        )
        verify(pixel).enqueueFire(
            NETP_ENABLE_UNIQUE,
            mapOf("cohort" to "", "ts" to "2000-01-01"),
        )
    }

    @Test
    fun `reportDisabled - called twice - fire daily pixel once`() {
        testee.reportDisabled()
        testee.reportDisabled()

        verify(pixel).fire("m_netp_ev_disabled_d")
    }

    @Test
    fun `reportWireguardLibraryLoadFailed - called twice - fire count pixel twice and daily pixel once`() {
        testee.reportWireguardLibraryLoadFailed()
        testee.reportWireguardLibraryLoadFailed()

        verify(pixel).enqueueFire("m_netp_ev_wireguard_error_unable_to_load_wireguard_library_d", mapOf("ts" to "2000-01-01"))
        verify(pixel, times(2)).enqueueFire("m_netp_ev_wireguard_error_unable_to_load_wireguard_library_c", mapOf("ts" to "2000-01-01"))
    }

    @Test
    fun `reportRekeyCompleted - called twice - fire daily pixel once`() {
        testee.reportRekeyCompleted()
        testee.reportRekeyCompleted()

        verify(pixel).enqueueFire("m_netp_ev_rekey_completed_d", mapOf("ts" to "2000-01-01"))
        verify(pixel, times(2)).enqueueFire("m_netp_ev_rekey_completed_c", mapOf("ts" to "2000-01-01"))
    }

    @Test
    fun `reportVpnConflictDialogShown - called twice - fire daily pixel once`() {
        testee.reportVpnConflictDialogShown()
        testee.reportVpnConflictDialogShown()

        verify(pixel).fire("m_netp_imp_vpn_conflict_dialog_d")
        verify(pixel, times(2)).fire("m_netp_imp_vpn_conflict_dialog_c")
    }

    @Test
    fun `reportAlwaysOnConflictDialogShown - called twice - fire daily pixel once`() {
        testee.reportAlwaysOnConflictDialogShown()
        testee.reportAlwaysOnConflictDialogShown()

        verify(pixel).fire("m_netp_imp_always_on_conflict_dialog_d")
        verify(pixel, times(2)).fire("m_netp_imp_always_on_conflict_dialog_c")
    }

    @Test
    fun `reportAlwaysOnPromotionDialogShown - called twice - fire daily pixel once`() {
        testee.reportAlwaysOnPromotionDialogShown()
        testee.reportAlwaysOnPromotionDialogShown()

        verify(pixel).fire("m_netp_imp_always_on_promotion_dialog_d")
        verify(pixel, times(2)).fire("m_netp_imp_always_on_promotion_dialog_c")
    }

    @Test
    fun `reportAlwaysOnLockdownDialogShown - called twice - fire daily pixel once`() {
        testee.reportAlwaysOnLockdownDialogShown()
        testee.reportAlwaysOnLockdownDialogShown()

        verify(pixel).fire("m_netp_imp_always_on_lockdown_dialog_d")
        verify(pixel, times(2)).fire("m_netp_imp_always_on_lockdown_dialog_c")
    }

    @Test
    fun `reportOpenSettingsFromAlwaysOnPromotion - called twice - fire daily pixel once`() {
        testee.reportOpenSettingsFromAlwaysOnPromotion()
        testee.reportOpenSettingsFromAlwaysOnPromotion()

        verify(pixel).fire("m_netp_ev_open_settings_from_always_on_promotion_dialog_d")
        verify(pixel, times(2)).fire("m_netp_ev_open_settings_from_always_on_promotion_dialog_c")
    }

    @Test
    fun `reportOpenSettingsFromAlwaysOnLockdown - called twice - fire daily pixel once`() {
        testee.reportOpenSettingsFromAlwaysOnLockdown()
        testee.reportOpenSettingsFromAlwaysOnLockdown()

        verify(pixel).fire("m_netp_ev_open_settings_from_always_on_lockdown_dialog_d")
        verify(pixel, times(2)).fire("m_netp_ev_open_settings_from_always_on_lockdown_dialog_c")
    }

    @Test
    fun `reportExclusionListShown - called twice - fire daily pixel once`() {
        testee.reportExclusionListShown()
        testee.reportExclusionListShown()

        verify(pixel).fire("m_netp_imp_exclusion_list_d")
        verify(pixel, times(2)).fire("m_netp_imp_exclusion_list_c")
    }

    @Test
    fun `reportAppAddedToExclusionList - called twice - fire pixel twice`() {
        testee.reportAppAddedToExclusionList()
        testee.reportAppAddedToExclusionList()

        verify(pixel, times(2)).fire("m_netp_ev_exclusion_list_app_added_c")
    }

    @Test
    fun `reportAppRemovedFromExclusionList - called twice - fire pixel twice`() {
        testee.reportAppRemovedFromExclusionList()
        testee.reportAppRemovedFromExclusionList()

        verify(pixel, times(2)).fire("m_netp_ev_exclusion_list_app_removed_c")
    }

    @Test
    fun `reportSkippedReportAfterExcludingApp - called twice - fire daily pixel once`() {
        testee.reportSkippedReportAfterExcludingApp()
        testee.reportSkippedReportAfterExcludingApp()

        verify(pixel).fire("m_netp_ev_skip_report_after_excluding_app_d")
        verify(pixel, times(2)).fire("m_netp_ev_skip_report_after_excluding_app_c")
    }

    @Test
    fun `reportExclusionListRestoreDefaults - called twice - fire daily pixel once`() {
        testee.reportExclusionListRestoreDefaults()
        testee.reportExclusionListRestoreDefaults()

        verify(pixel).fire("m_netp_ev_exclusion_list_restore_defaults_d")
        verify(pixel, times(2)).fire("m_netp_ev_exclusion_list_restore_defaults_c")
    }

    @Test
    fun `reportExclusionListLaunchBreakageReport - called twice - fire daily pixel once`() {
        testee.reportExclusionListLaunchBreakageReport()
        testee.reportExclusionListLaunchBreakageReport()

        verify(pixel).fire("m_netp_ev_exclusion_list_launch_breakage_report_d")
        verify(pixel, times(2)).fire("m_netp_ev_exclusion_list_launch_breakage_report_c")
    }

    @Test
    fun `reportFaqsShown - called twice - fire daily pixel once`() {
        testee.reportFaqsShown()
        testee.reportFaqsShown()

        verify(pixel).fire("m_netp_imp_faqs_d")
        verify(pixel, times(2)).fire("m_netp_imp_faqs_c")
    }
}

private class FakeNetpCohortStore(
    override var cohortLocalDate: LocalDate? = null,
) : NetpCohortStore
