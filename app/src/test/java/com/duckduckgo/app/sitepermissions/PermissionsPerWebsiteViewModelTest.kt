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

package com.duckduckgo.app.sitepermissions

import app.cash.turbine.test
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.location.data.LocationPermissionEntity
import com.duckduckgo.app.location.data.LocationPermissionType
import com.duckduckgo.app.location.data.LocationPermissionsRepository
import com.duckduckgo.app.settings.db.SettingsDataStore
import com.duckduckgo.app.sitepermissions.permissionsperwebsite.PermissionsPerWebsiteViewModel
import com.duckduckgo.app.sitepermissions.permissionsperwebsite.PermissionsPerWebsiteViewModel.Command.GoBackToSitePermissions
import com.duckduckgo.app.sitepermissions.permissionsperwebsite.PermissionsPerWebsiteViewModel.Command.ShowPermissionSettingSelectionDialog
import com.duckduckgo.app.sitepermissions.permissionsperwebsite.WebsitePermissionSetting
import com.duckduckgo.app.sitepermissions.permissionsperwebsite.WebsitePermissionSettingOption
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.site.permissions.impl.SitePermissionsRepository
import com.duckduckgo.site.permissions.store.sitepermissions.SitePermissionAskSettingType
import com.duckduckgo.site.permissions.store.sitepermissions.SitePermissionsEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PermissionsPerWebsiteViewModelTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private val mockSitePermissionsRepository: SitePermissionsRepository = mock()
    private val mockLocationPermissionsRepository: LocationPermissionsRepository = mock()
    private val mockSettingsDataStore: SettingsDataStore = mock()

    private val viewModel = PermissionsPerWebsiteViewModel(
        sitePermissionsRepository = mockSitePermissionsRepository,
        locationPermissionsRepository = mockLocationPermissionsRepository,
        settingsDataStore = mockSettingsDataStore,
    )

    private val domain = "domain.com"

    @Test
    fun `loadWebsitePermissions - view state emitted settings`() = runTest {
        loadAskForPermissionsPrefs()
        loadWebsitePermissionsSettings()

        viewModel.websitePermissionSettings(domain)

        viewModel.viewState.test {
            val permissions = awaitItem().websitePermissions
            assertEquals(4, permissions.size)
        }
    }

    @Test
    fun `loadWebsitePermissions - site permission is allow always - show setting as allow`() = runTest {
        loadAskForPermissionsPrefs()
        loadWebsitePermissionsSettings(cameraSetting = SitePermissionAskSettingType.ALLOW_ALWAYS.name)

        viewModel.websitePermissionSettings(domain)

        viewModel.viewState.test {
            val cameraSetting = awaitItem().websitePermissions[1]
            assertEquals(WebsitePermissionSettingOption.ALLOW, cameraSetting.setting)
        }
    }

    @Test
    fun `loadWebsitePermissions - site permission ask every time - show setting as ask`() = runTest {
        loadAskForPermissionsPrefs()
        loadWebsitePermissionsSettings(cameraSetting = SitePermissionAskSettingType.ASK_EVERY_TIME.name)

        viewModel.websitePermissionSettings(domain)

        viewModel.viewState.test {
            val cameraSetting = awaitItem().websitePermissions[1]
            assertEquals(WebsitePermissionSettingOption.ASK, cameraSetting.setting)
        }
    }

    @Test
    fun `loadWebsitePermissions - site permission is deny always time - show setting as deny`() = runTest {
        loadAskForPermissionsPrefs()
        loadWebsitePermissionsSettings(cameraSetting = SitePermissionAskSettingType.DENY_ALWAYS.name)

        viewModel.websitePermissionSettings(domain)

        viewModel.viewState.test {
            val cameraSetting = awaitItem().websitePermissions[1]
            assertEquals(WebsitePermissionSettingOption.DENY, cameraSetting.setting)
        }
    }

    @Test
    fun `loadAskForSitePermissionPrefs - view state updated - ask disabled`() = runTest {
        loadAskForPermissionsPrefs(cameraEnabled = false)
        loadWebsitePermissionsSettings()

        viewModel.websitePermissionSettings(domain)

        viewModel.viewState.test {
            val cameraSetting = awaitItem().websitePermissions[1]
            assertEquals(WebsitePermissionSettingOption.ASK_DISABLED, cameraSetting.setting)
        }
    }

    @Test
    fun `loadAskForSitePermissionPrefs - prefs disabled and setting deny - show setting as deny`() = runTest {
        loadAskForPermissionsPrefs(cameraEnabled = false)
        loadWebsitePermissionsSettings(cameraSetting = SitePermissionAskSettingType.DENY_ALWAYS.name)

        viewModel.websitePermissionSettings(domain)

        viewModel.viewState.test {
            val cameraSetting = awaitItem().websitePermissions[1]
            assertEquals(WebsitePermissionSettingOption.DENY, cameraSetting.setting)
        }
    }

    @Test
    fun `loadAskForSitePermissionPrefs - website permission settings - show setting as allow`() = runTest {
        loadAskForPermissionsPrefs(cameraEnabled = false)
        loadWebsitePermissionsSettings(cameraSetting = SitePermissionAskSettingType.ALLOW_ALWAYS.name)

        viewModel.websitePermissionSettings(domain)

        viewModel.viewState.test {
            val cameraSetting = awaitItem().websitePermissions[1]
            assertEquals(WebsitePermissionSettingOption.ALLOW, cameraSetting.setting)
        }
    }

    @Test
    fun `whenPermissionIsTapped - show settings selection dialog`() = runTest {
        loadAskForPermissionsPrefs()
        loadWebsitePermissionsSettings()

        viewModel.websitePermissionSettings(domain)
        val websitePermissionSetting =
            WebsitePermissionSetting(R.drawable.ic_video_24, R.string.sitePermissionsSettingsCamera, WebsitePermissionSettingOption.ASK)
        viewModel.permissionSettingSelected(websitePermissionSetting)

        viewModel.commands.test {
            assertTrue(awaitItem() is ShowPermissionSettingSelectionDialog)
        }
    }

    @Test
    fun `onPermissionSettingSelected - save permission setting`() = runTest {
        loadAskForPermissionsPrefs()
        loadWebsitePermissionsSettings()

        viewModel.websitePermissionSettings(domain)
        val websitePermissionSetting =
            WebsitePermissionSetting(R.drawable.ic_video_24, R.string.sitePermissionsSettingsCamera, WebsitePermissionSettingOption.ASK)
        val sitePermissionSetting =
            SitePermissionsEntity(
                domain,
                websitePermissionSetting.setting.toSitePermissionSettingEntityType().name,
                websitePermissionSetting.setting.toSitePermissionSettingEntityType().name,
            )
        viewModel.onPermissionSettingSelected(websitePermissionSetting, domain)

        verify(mockSitePermissionsRepository).savePermission(sitePermissionSetting)
    }

    @Test
    fun `websitePermissionsAreRemoved - delete from DB`() = runTest {
        loadAskForPermissionsPrefs()
        loadWebsitePermissionsSettings()

        viewModel.websitePermissionSettings(domain)
        viewModel.removeWebsitePermissionsSettings(domain)

        verify(mockSitePermissionsRepository).deletePermissionsForSite(domain)
    }

    @Test
    fun `whenRemoveWebsitePermissionsThenNavigateBack - website permissions removed - navigate back to site permissions screen`() = runTest {
        loadAskForPermissionsPrefs()
        loadWebsitePermissionsSettings()

        viewModel.websitePermissionSettings(domain)
        viewModel.removeWebsitePermissionsSettings(domain)

        viewModel.commands.test {
            assertTrue(awaitItem() is GoBackToSitePermissions)
        }
    }

    private fun loadAskForPermissionsPrefs(
        micEnabled: Boolean = true,
        cameraEnabled: Boolean = true,
        locationEnabled: Boolean = true,
    ) {
        whenever(mockSettingsDataStore.appLocationPermission).thenReturn(locationEnabled)
        whenever(mockSitePermissionsRepository.askMicEnabled).thenReturn(micEnabled)
        whenever(mockSitePermissionsRepository.askCameraEnabled).thenReturn(cameraEnabled)
    }

    private fun loadWebsitePermissionsSettings(
        cameraSetting: String = SitePermissionAskSettingType.ASK_EVERY_TIME.name,
        micSetting: String = SitePermissionAskSettingType.ASK_EVERY_TIME.name,
        locationSetting: LocationPermissionType = LocationPermissionType.ALLOW_ALWAYS,
    ) {
        val testLocationEntity = LocationPermissionEntity(domain, locationSetting)
        val testSitePermissionEntity = SitePermissionsEntity(domain, cameraSetting, micSetting)
        mockSitePermissionsRepository.stub { onBlocking { getSitePermissionsForWebsite(domain) }.thenReturn(testSitePermissionEntity) }
        mockLocationPermissionsRepository.stub { onBlocking { getDomainPermission(domain) }.thenReturn(testLocationEntity) }
    }
}
