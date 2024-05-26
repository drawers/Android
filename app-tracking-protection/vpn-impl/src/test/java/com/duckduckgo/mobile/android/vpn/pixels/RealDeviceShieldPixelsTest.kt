/*
 * Copyright (c) 2021 DuckDuckGo
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

package com.duckduckgo.mobile.android.vpn.pixels

import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.common.test.api.InMemorySharedPreferences
import com.duckduckgo.data.store.api.SharedPreferencesProvider
import java.util.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class RealDeviceShieldPixelsTest {

    private val pixel = mock<Pixel>()
    private val sharedPreferencesProvider = mock<SharedPreferencesProvider>()

    lateinit var deviceShieldPixels: DeviceShieldPixels

    @Before
    fun setup() {
        val prefs = InMemorySharedPreferences()
        whenever(
            sharedPreferencesProvider.getSharedPreferences(eq("com.duckduckgo.mobile.android.device.shield.pixels"), eq(true), eq(true)),
        ).thenReturn(prefs)

        deviceShieldPixels = RealDeviceShieldPixels(pixel, sharedPreferencesProvider)
    }

    @Test
    fun `whenDeviceShieldEnabledOnSearch - fire daily pixel`() {
        deviceShieldPixels.deviceShieldEnabledOnSearch()
        deviceShieldPixels.deviceShieldEnabledOnSearch()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_ENABLE_UPON_SEARCH_DAILY.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `whenDeviceShieldDisabledOnSearch - fire daily pixel`() {
        deviceShieldPixels.deviceShieldDisabledOnSearch()
        deviceShieldPixels.deviceShieldDisabledOnSearch()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_DISABLE_UPON_SEARCH_DAILY.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `reportEnable - fire unique and daily pixel`() {
        deviceShieldPixels.reportEnabled()
        deviceShieldPixels.reportEnabled()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_ENABLE_UNIQUE)
        verify(pixel).fire(DeviceShieldPixelNames.ATP_ENABLE_DAILY.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `reportDisable - fire daily pixel`() {
        deviceShieldPixels.reportDisabled()
        deviceShieldPixels.reportDisabled()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_DISABLE_DAILY.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `enableFromReminderNotification - fire unique daily and count pixels`() {
        deviceShieldPixels.enableFromReminderNotification()
        deviceShieldPixels.enableFromReminderNotification()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_REMINDER_NOTIFICATION_UNIQUE)
        verify(pixel).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_REMINDER_NOTIFICATION_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_REMINDER_NOTIFICATION.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `enableFromSettings - fire unique daily and count pixels`() {
        deviceShieldPixels.enableFromOnboarding()
        deviceShieldPixels.enableFromOnboarding()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_ONBOARDING_UNIQUE)
        verify(pixel).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_ONBOARDING_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_ONBOARDING.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `enableFromSettingsTile - fire unique daily and count pixels`() {
        deviceShieldPixels.enableFromQuickSettingsTile()
        deviceShieldPixels.enableFromQuickSettingsTile()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_SETTINGS_TILE_UNIQUE)
        verify(pixel).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_SETTINGS_TILE_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_SETTINGS_TILE.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `enableFromPrivacyReport - fire unique daily and count pixels`() {
        deviceShieldPixels.enableFromSummaryTrackerActivity()
        deviceShieldPixels.enableFromSummaryTrackerActivity()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_SUMMARY_TRACKER_ACTIVITY_UNIQUE)
        verify(pixel).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_SUMMARY_TRACKER_ACTIVITY_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_ENABLE_FROM_SUMMARY_TRACKER_ACTIVITY.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `disableFromSettingsTile - fire daily and count pixels`() {
        deviceShieldPixels.disableFromQuickSettingsTile()
        deviceShieldPixels.disableFromQuickSettingsTile()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_DISABLE_FROM_SETTINGS_TILE_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_DISABLE_FROM_SETTINGS_TILE.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `didShowDailyNotification - fire daily pixel`() {
        deviceShieldPixels.didShowDailyNotification(0)
        deviceShieldPixels.didShowDailyNotification(1)

        verify(pixel).fire(DeviceShieldPixelNames.DID_SHOW_DAILY_NOTIFICATION.notificationVariant(0))
        verify(pixel).fire(DeviceShieldPixelNames.DID_SHOW_DAILY_NOTIFICATION.notificationVariant(1))
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `didPressDailyNotification - fire daily pixel`() {
        deviceShieldPixels.didPressOnDailyNotification(0)
        deviceShieldPixels.didPressOnDailyNotification(1)

        verify(pixel).fire(DeviceShieldPixelNames.DID_PRESS_DAILY_NOTIFICATION.notificationVariant(0))
        verify(pixel).fire(DeviceShieldPixelNames.DID_PRESS_DAILY_NOTIFICATION.notificationVariant(1))
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `didShowWeeklyNotification - fire daily pixel`() {
        deviceShieldPixels.didShowWeeklyNotification(0)
        deviceShieldPixels.didShowWeeklyNotification(1)

        verify(pixel).fire(DeviceShieldPixelNames.DID_SHOW_WEEKLY_NOTIFICATION.notificationVariant(0))
        verify(pixel).fire(DeviceShieldPixelNames.DID_SHOW_WEEKLY_NOTIFICATION.notificationVariant(1))
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `didPressWeeklyNotification - fire daily pixel`() {
        deviceShieldPixels.didPressOnWeeklyNotification(0)
        deviceShieldPixels.didPressOnWeeklyNotification(1)

        verify(pixel).fire(DeviceShieldPixelNames.DID_PRESS_WEEKLY_NOTIFICATION.notificationVariant(0))
        verify(pixel).fire(DeviceShieldPixelNames.DID_PRESS_WEEKLY_NOTIFICATION.notificationVariant(1))
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `didPressOngoingNotification - fire daily and count pixels`() {
        deviceShieldPixels.didPressOngoingNotification()
        deviceShieldPixels.didPressOngoingNotification()

        verify(pixel).fire(DeviceShieldPixelNames.DID_PRESS_ONGOING_NOTIFICATION_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.DID_PRESS_ONGOING_NOTIFICATION.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `didShowReminderNotification - fire daily and count pixels`() {
        deviceShieldPixels.didShowReminderNotification()
        deviceShieldPixels.didShowReminderNotification()

        verify(pixel).fire(DeviceShieldPixelNames.DID_SHOW_REMINDER_NOTIFICATION_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.DID_SHOW_REMINDER_NOTIFICATION.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `didPressReminderNotification - fire daily and count pixels`() {
        deviceShieldPixels.didPressReminderNotification()
        deviceShieldPixels.didPressReminderNotification()

        verify(pixel).fire(DeviceShieldPixelNames.DID_PRESS_REMINDER_NOTIFICATION_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.DID_PRESS_REMINDER_NOTIFICATION.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `didShowNewTabSummary - fire unique daily and count pixels`() {
        deviceShieldPixels.didShowNewTabSummary()
        deviceShieldPixels.didShowNewTabSummary()

        verify(pixel).fire(DeviceShieldPixelNames.DID_SHOW_NEW_TAB_SUMMARY_UNIQUE)
        verify(pixel).fire(DeviceShieldPixelNames.DID_SHOW_NEW_TAB_SUMMARY_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.DID_SHOW_NEW_TAB_SUMMARY.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `didPressNewTabSummary - fire daily and count pixels`() {
        deviceShieldPixels.didPressNewTabSummary()
        deviceShieldPixels.didPressNewTabSummary()

        verify(pixel).fire(DeviceShieldPixelNames.DID_PRESS_NEW_TAB_SUMMARY_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.DID_PRESS_NEW_TAB_SUMMARY.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `didShowSummaryTrackerActivity - fire unique daily and count pixels`() {
        deviceShieldPixels.didShowSummaryTrackerActivity()
        deviceShieldPixels.didShowSummaryTrackerActivity()

        verify(pixel).fire(DeviceShieldPixelNames.DID_SHOW_SUMMARY_TRACKER_ACTIVITY_UNIQUE)
        verify(pixel).fire(DeviceShieldPixelNames.DID_SHOW_SUMMARY_TRACKER_ACTIVITY_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.DID_SHOW_SUMMARY_TRACKER_ACTIVITY.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `didShowDetailedTrackerActivity - fire unique daily and count pixels`() {
        deviceShieldPixels.didShowDetailedTrackerActivity()
        deviceShieldPixels.didShowDetailedTrackerActivity()

        verify(pixel).fire(DeviceShieldPixelNames.DID_SHOW_DETAILED_TRACKER_ACTIVITY_UNIQUE)
        verify(pixel).fire(DeviceShieldPixelNames.DID_SHOW_DETAILED_TRACKER_ACTIVITY_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.DID_SHOW_DETAILED_TRACKER_ACTIVITY.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `startError - fire daily and count pixels`() {
        deviceShieldPixels.startError()
        deviceShieldPixels.startError()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_START_ERROR_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_START_ERROR.pixelName)
        verify(pixel, times(2)).enqueueFire(DeviceShieldPixelNames.VPN_START_ATTEMPT_FAILURE.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `automaticRestart - fire daily and count pixels`() {
        deviceShieldPixels.automaticRestart()
        deviceShieldPixels.automaticRestart()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_AUTOMATIC_RESTART_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_AUTOMATIC_RESTART.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `whenSuddenKillBySystem - fire daily and count pixels`() {
        deviceShieldPixels.suddenKillBySystem()
        deviceShieldPixels.suddenKillBySystem()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_KILLED_BY_SYSTEM_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_KILLED_BY_SYSTEM.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_KILLED.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `whenSuddenKillByVpnRevoked - fire daily and count pixels`() {
        deviceShieldPixels.suddenKillByVpnRevoked()
        deviceShieldPixels.suddenKillByVpnRevoked()

        verify(pixel).fire(DeviceShieldPixelNames.ATP_KILLED_VPN_REVOKED_DAILY.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_KILLED_VPN_REVOKED.pixelName)
        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_KILLED.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `whenPrivacyReportArticleDisplayed - fire count pixel`() {
        deviceShieldPixels.privacyReportArticleDisplayed()
        deviceShieldPixels.privacyReportArticleDisplayed()

        verify(pixel, times(2)).fire(DeviceShieldPixelNames.ATP_DID_SHOW_PRIVACY_REPORT_ARTICLE.pixelName)
        verify(pixel, times(1)).fire(DeviceShieldPixelNames.ATP_DID_SHOW_PRIVACY_REPORT_ARTICLE_DAILY.pixelName)
        verifyNoMoreInteractions(pixel)
    }

    @Test
    fun `reportUnprotectedAppsBucket - fire pixels`() {
        val bucketSize = 20
        deviceShieldPixels.reportUnprotectedAppsBucket(bucketSize)

        verify(pixel).fire(DeviceShieldPixelNames.ATP_REPORT_UNPROTECTED_APPS_BUCKET.notificationVariant(bucketSize))
        verify(pixel).fire(DeviceShieldPixelNames.ATP_REPORT_UNPROTECTED_APPS_BUCKET_DAILY.notificationVariant(bucketSize))
    }

    private fun DeviceShieldPixelNames.notificationVariant(variant: Int): String {
        return String.format(Locale.US, pixelName, variant)
    }
}
