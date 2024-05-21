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
import com.duckduckgo.app.location.GeoLocationPermissions
import com.duckduckgo.app.location.data.LocationPermissionEntity
import com.duckduckgo.app.location.data.LocationPermissionType
import com.duckduckgo.app.location.data.LocationPermissionsRepository
import com.duckduckgo.app.settings.db.SettingsDataStore
import com.duckduckgo.app.sitepermissions.SitePermissionsViewModel.Command.LaunchWebsiteAllowed
import com.duckduckgo.app.sitepermissions.SitePermissionsViewModel.Command.ShowRemovedAllConfirmationSnackbar
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.site.permissions.impl.SitePermissionsRepository
import com.duckduckgo.site.permissions.store.sitepermissions.SitePermissionsEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SitePermissionsViewModelTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private val mockSitePermissionsRepository: SitePermissionsRepository = mock()
    private val mockLocationPermissionsRepository: LocationPermissionsRepository = mock()
    private val mockGeoLocationPermissions: GeoLocationPermissions = mock()
    private val mockSettingsDataStore: SettingsDataStore = mock()

    private val viewModel = SitePermissionsViewModel(
        sitePermissionsRepository = mockSitePermissionsRepository,
        locationPermissionsRepository = mockLocationPermissionsRepository,
        geolocationPermissions = mockGeoLocationPermissions,
        settingsDataStore = mockSettingsDataStore,
        dispatcherProvider = coroutineRule.testDispatcherProvider,
    )

    @Before
    fun before() {
        loadPermissionsSettings()
        loadWebsites()
        viewModel.allowedSites()
    }

    @Test
    fun `whenAllowedSitesLoaded - view state emitted websites`() = runTest {
        viewModel.viewState.test {
            val sitePermissions = awaitItem().sitesPermissionsAllowed
            assertEquals(2, sitePermissions.size)
        }
    }

    @Test
    fun `whenAllowedSitesLoaded - viewState emitted location websites`() = runTest {
        viewModel.viewState.test {
            val sitePermissions = awaitItem().locationPermissionsAllowed
            assertEquals(1, sitePermissions.size)
        }
    }

    @Test
    fun `removeAllSites - clear all location websites is called`() = runTest {
        viewModel.removeAllSitesSelected()

        verify(mockGeoLocationPermissions).clearAll()
    }

    @Test
    fun `removeAllSites - delete all site permissions called`() = runTest {
        viewModel.removeAllSitesSelected()

        verify(mockSitePermissionsRepository).deleteAll()
    }

    @Test
    fun `removeAll - show remove all snackbar`() = runTest {
        viewModel.removeAllSitesSelected()

        viewModel.commands.test {
            assertTrue(awaitItem() is ShowRemovedAllConfirmationSnackbar)
        }
    }

    @Test
    fun `loadPermissionsSettings - view state emitted for ask location enabled - not enabled`() = runTest {
        loadPermissionsSettings(locationEnabled = false)
        viewModel.viewState.test {
            val askLocationEnabled = awaitItem().askLocationEnabled
            assertFalse(askLocationEnabled)
        }
    }

    @Test
    fun `loadPermissionsSettings - view state emitted for ask camera enabled - not enabled`() = runTest {
        loadPermissionsSettings(cameraEnabled = false)

        viewModel.viewState.test {
            val askCameraEnabled = awaitItem().askCameraEnabled
            assertFalse(askCameraEnabled)
        }
    }

    @Test
    fun `loadPermissionsSettings - view state emitted for ask mic enabled - not asked`() = runTest {
        loadPermissionsSettings(micEnabled = false)

        viewModel.viewState.test {
            val askMicEnabled = awaitItem().askMicEnabled
            assertFalse(askMicEnabled)
        }
    }

    @Test
    fun `toggleOffAskForLocation - view state emitted`() = runTest {
        viewModel.permissionToggleSelected(false, R.string.sitePermissionsSettingsLocation)

        viewModel.viewState.test {
            val locationEnabled = awaitItem().askLocationEnabled
            assertFalse(locationEnabled)
        }
    }

    @Test
    fun `toggleOffAskForCamera - view state emitted`() = runTest {
        viewModel.permissionToggleSelected(false, R.string.sitePermissionsSettingsCamera)

        viewModel.viewState.test {
            val cameraEnabled = awaitItem().askCameraEnabled
            assertFalse(cameraEnabled)
        }
    }

    @Test
    fun `toggleOffAskForMic - view state emitted`() = runTest {
        viewModel.permissionToggleSelected(false, R.string.sitePermissionsSettingsMicrophone)

        viewModel.viewState.test {
            val micEnabled = awaitItem().askMicEnabled
            assertFalse(micEnabled)
        }
    }

    @Test
    fun `whenWebsiteIsTapped - navigate to permissions per website screen`() = runTest {
        val testDomain = "website1.com"
        viewModel.allowedSiteSelected(testDomain)

        viewModel.commands.test {
            assertTrue(awaitItem() is LaunchWebsiteAllowed)
        }
    }

    private fun loadWebsites() {
        val locationPermissions = listOf(LocationPermissionEntity("www.website1.com", LocationPermissionType.ALLOW_ONCE))
        val sitePermissions = listOf(SitePermissionsEntity("www.website2.com"), SitePermissionsEntity("www.website3.com"))
        whenever(mockLocationPermissionsRepository.getLocationPermissionsFlow()).thenReturn(flowOf(locationPermissions))
        whenever(mockSitePermissionsRepository.sitePermissionsWebsitesFlow()).thenReturn(flowOf(sitePermissions))
        whenever(mockSitePermissionsRepository.sitePermissionsAllowedFlow()).thenReturn(flowOf(emptyList()))
    }

    private fun loadPermissionsSettings(micEnabled: Boolean = true, cameraEnabled: Boolean = true, locationEnabled: Boolean = true) {
        whenever(mockSettingsDataStore.appLocationPermission).thenReturn(locationEnabled)
        whenever(mockSitePermissionsRepository.askMicEnabled).thenReturn(micEnabled)
        whenever(mockSitePermissionsRepository.askCameraEnabled).thenReturn(cameraEnabled)
    }
}
