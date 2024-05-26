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

package com.duckduckgo.app.permissions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.permissions.PermissionsViewModel.Command
import com.duckduckgo.app.pixels.AppPixelName
import com.duckduckgo.app.settings.clear.AppLinkSettingType
import com.duckduckgo.app.settings.db.SettingsDataStore
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

class PermissionsViewModelTest {

    @get:Rule
    @Suppress("unused")
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var testee: PermissionsViewModel

    @Mock
    private lateinit var mockAppSettingsDataStore: SettingsDataStore

    @Mock
    private lateinit var mockPixel: Pixel

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    @Before
    fun before() {
        MockitoAnnotations.openMocks(this)

        testee = PermissionsViewModel(
            mockAppSettingsDataStore,
            mockPixel,
        )
    }

    @Test
    fun `viewState - start not called - initialised with default values`() = runTest {
        testee.viewState().test {
            val value = awaitItem()
            assertTrue(value.autoCompleteSuggestionsEnabled)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `start - notifications enabled - notifications setting subtitle set correctly`() = runTest {
        testee.start(notificationsEnabled = true)

        testee.viewState().test {
            val value = expectMostRecentItem()

            assertEquals(R.string.settingsSubtitleNotificationsEnabled, value.notificationsSettingSubtitleId)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `start - notifications disabled - notifications setting subtitle set as disabled`() = runTest {
        testee.start(notificationsEnabled = false)

        testee.viewState().test {
            val value = expectMostRecentItem()

            assertEquals(R.string.settingsSubtitleNotificationsDisabled, value.notificationsSettingSubtitleId)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppLinksSettingChanged - set to ask every time - data store updated and pixel sent`() {
        testee.onAppLinksSettingChanged(AppLinkSettingType.ASK_EVERYTIME)

        verify(mockAppSettingsDataStore).appLinksEnabled = true
        verify(mockAppSettingsDataStore).showAppLinksPrompt = true
        verify(mockPixel).fire(AppPixelName.SETTINGS_APP_LINKS_ASK_EVERY_TIME_SELECTED)
    }

    @Test
    fun `onAppLinksSettingChanged - set to always - data store updated and pixel sent`() {
        testee.onAppLinksSettingChanged(AppLinkSettingType.ALWAYS)

        verify(mockAppSettingsDataStore).appLinksEnabled = true
        verify(mockAppSettingsDataStore).showAppLinksPrompt = false
        verify(mockPixel).fire(AppPixelName.SETTINGS_APP_LINKS_ALWAYS_SELECTED)
    }

    @Test
    fun `onAppLinksSettingChanged - set to never - data store updated and pixel sent`() {
        testee.onAppLinksSettingChanged(AppLinkSettingType.NEVER)

        verify(mockAppSettingsDataStore).appLinksEnabled = false
        verify(mockAppSettingsDataStore).showAppLinksPrompt = false
        verify(mockPixel).fire(AppPixelName.SETTINGS_APP_LINKS_NEVER_SELECTED)
    }

    @Test
    fun `onSitePermissionsClicked - emit command launch location and pixel fired`() = runTest {
        testee.commands().test {
            testee.onSitePermissionsClicked()

            assertEquals(Command.LaunchLocation, awaitItem())
            verify(mockPixel).fire(AppPixelName.SETTINGS_SITE_PERMISSIONS_PRESSED)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `userRequestedToChangeNotificationsSetting - emit command and send pixel`() = runTest {
        testee.commands().test {
            testee.userRequestedToChangeNotificationsSetting()

            assertEquals(Command.LaunchNotificationsSettings, awaitItem())
            verify(mockPixel).fire(AppPixelName.SETTINGS_NOTIFICATIONS_PRESSED)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `userRequestedToChangeAppLinkSetting - emit command launch app link settings`() = runTest {
        testee.commands().test {
            testee.userRequestedToChangeAppLinkSetting()

            assertEquals(Command.LaunchAppLinkSettings(AppLinkSettingType.ASK_EVERYTIME), awaitItem())
            verify(mockPixel).fire(AppPixelName.SETTINGS_APP_LINKS_PRESSED)

            cancelAndConsumeRemainingEvents()
        }
    }
}
