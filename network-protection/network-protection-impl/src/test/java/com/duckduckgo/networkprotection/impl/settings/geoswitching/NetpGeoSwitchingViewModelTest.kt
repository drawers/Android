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

package com.duckduckgo.networkprotection.impl.settings.geoswitching

import androidx.lifecycle.LifecycleOwner
import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.networkprotection.api.NetworkProtectionState
import com.duckduckgo.networkprotection.impl.configuration.WgServerDebugProvider
import com.duckduckgo.networkprotection.impl.pixels.NetworkProtectionPixels
import com.duckduckgo.networkprotection.impl.settings.geoswitching.GeoswitchingListItem.CountryItem
import com.duckduckgo.networkprotection.impl.settings.geoswitching.GeoswitchingListItem.DividerItem
import com.duckduckgo.networkprotection.impl.settings.geoswitching.GeoswitchingListItem.HeaderItem
import com.duckduckgo.networkprotection.impl.settings.geoswitching.GeoswitchingListItem.RecommendedItem
import com.duckduckgo.networkprotection.store.NetPGeoswitchingRepository
import com.duckduckgo.networkprotection.store.NetPGeoswitchingRepository.UserPreferredLocation
import com.duckduckgo.networkprotection.store.db.NetPGeoswitchingLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class NetpGeoSwitchingViewModelTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()
    private lateinit var testee: NetpGeoSwitchingViewModel

    @Mock
    private lateinit var mockLifecycleOwner: LifecycleOwner

    @Mock
    private lateinit var wgServerDebugProvider: WgServerDebugProvider

    @Mock
    private lateinit var networkProtectionState: NetworkProtectionState

    @Mock
    private lateinit var networkProtectionPixels: NetworkProtectionPixels
    private val fakeContentProvider = FakeNetpEgressServersProvider()
    private val fakeRepository = FakeNetPGeoswitchingRepository()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testee = NetpGeoSwitchingViewModel(
            fakeContentProvider,
            fakeRepository,
            coroutineRule.testDispatcherProvider,
            wgServerDebugProvider,
            networkProtectionState,
            networkProtectionPixels,
        )
    }

    @Test
    fun `onStart - view state should emit parsed list`() = runTest {
        testee.onStart(mockLifecycleOwner)
        testee.viewState().test {
            expectMostRecentItem().also {
                assertEquals(6, it.items.size)
                assertTrue(it.items[0] is HeaderItem)
                assertTrue(it.items[1] is RecommendedItem)
                assertTrue(it.items[2] is DividerItem)
                assertTrue(it.items[3] is HeaderItem)
                assertTrue(it.items[4] is CountryItem)
                assertEquals(
                    it.items[4],
                    CountryItem(
                        countryCode = "gb",
                        countryEmoji = "🇬🇧",
                        countryName = "UK",
                        cities = emptyList(),
                    ),
                )
                assertTrue(it.items[5] is CountryItem)
                assertEquals(
                    it.items[5],
                    CountryItem(
                        countryCode = "us",
                        countryEmoji = "🇺🇸",
                        countryName = "United States",
                        cities = listOf("Atlanta", "Chicago", "El Segundo", "Newark"),
                    ),
                )
            }
        }
    }

    @Test
    fun `onStart - provider has no downloaded data - view state only contains nearest available`() = runTest {
        val mockProvider = mock(NetpEgressServersProvider::class.java)
        testee = NetpGeoSwitchingViewModel(
            mockProvider,
            fakeRepository,
            coroutineRule.testDispatcherProvider,
            wgServerDebugProvider,
            networkProtectionState,
            networkProtectionPixels,
        )
        whenever(mockProvider.getServerLocations()).thenReturn(emptyList())

        testee.onStart(mockLifecycleOwner)
        testee.viewState().test {
            expectMostRecentItem().also {
                assertEquals(2, it.items.size)
                assertTrue(it.items[0] is HeaderItem)
                assertTrue(it.items[1] is RecommendedItem)
            }
        }
    }

    @Test
    fun `onCountrySelected - country and city changed - update stored country and reset city`() = runTest {
        fakeRepository.setUserPreferredLocation(UserPreferredLocation("us", "Newark"))

        testee.onCountrySelected("uk")

        fakeRepository.getUserPreferredLocation().let {
            assertEquals("uk", it.countryCode)
            assertNull(it.cityName)
        }
        verify(wgServerDebugProvider).clearSelectedServerName()
    }

    @Test
    fun `onCountrySelected - same preferred country and city - stored data same`() = runTest {
        fakeRepository.setUserPreferredLocation(UserPreferredLocation("us", "Newark"))

        testee.onCountrySelected("us")

        fakeRepository.getUserPreferredLocation().let {
            assertEquals("us", it.countryCode)
            assertEquals("Newark", it.cityName)
        }
        verifyNoInteractions(wgServerDebugProvider)
    }

    @Test
    fun `onNearestAvailableCountrySelected - stored data should be null`() = runTest {
        fakeRepository.setUserPreferredLocation(UserPreferredLocation("us", "Newark"))

        testee.onNearestAvailableCountrySelected()

        fakeRepository.getUserPreferredLocation().let {
            assertNull(it.countryCode)
            assertNull(it.cityName)
        }
        verify(wgServerDebugProvider).clearSelectedServerName()
    }

    @Test
    fun `onStart - netP not enabled - do not restart`() = runTest {
        whenever(networkProtectionState.isEnabled()).thenReturn(false)

        testee.onStart(mockLifecycleOwner)
        testee.onStop(mockLifecycleOwner)

        verify(networkProtectionState).isEnabled()
        verifyNoMoreInteractions(networkProtectionState)
    }

    @Test
    fun `onStart - netP enabled but no change in preferred location - do not restart`() = runTest {
        fakeRepository.setUserPreferredLocation(UserPreferredLocation(countryCode = "us"))
        whenever(networkProtectionState.isEnabled()).thenReturn(true)

        testee.onStart(mockLifecycleOwner)
        testee.onCountrySelected("us")
        testee.onStop(mockLifecycleOwner)

        verify(networkProtectionState).isEnabled()
        verifyNoMoreInteractions(networkProtectionState)
    }

    @Test
    fun `onStart - netPis enabled and preferred country changed - restart`() = runTest {
        whenever(networkProtectionState.isEnabled()).thenReturn(true)

        testee.onStart(mockLifecycleOwner)
        testee.onCountrySelected("us")
        testee.onStop(mockLifecycleOwner)

        verify(networkProtectionState).clearVPNConfigurationAndRestart()
    }

    @Test
    fun `onStart - netPis enabled and preferred city changed - restart`() = runTest {
        fakeRepository.setUserPreferredLocation(UserPreferredLocation(countryCode = "us", cityName = "El Segundo"))
        whenever(networkProtectionState.isEnabled()).thenReturn(true)

        testee.onStart(mockLifecycleOwner)
        fakeRepository.setUserPreferredLocation(UserPreferredLocation(countryCode = "us", cityName = "Newark"))
        testee.onStop(mockLifecycleOwner)

        verify(networkProtectionState).clearVPNConfigurationAndRestart()
    }

    @Test
    fun `onCreate - emit impression pixels`() {
        testee.onCreate(mockLifecycleOwner)

        verify(networkProtectionPixels).reportGeoswitchingScreenShown()
    }

    @Test
    fun `onStart - no countries available - emit no locations pixel`() = runTest {
        val mockProvider = mock(NetpEgressServersProvider::class.java)
        testee = NetpGeoSwitchingViewModel(
            mockProvider,
            fakeRepository,
            coroutineRule.testDispatcherProvider,
            wgServerDebugProvider,
            networkProtectionState,
            networkProtectionPixels,
        )
        whenever(mockProvider.getServerLocations()).thenReturn(emptyList())

        testee.onStart(mockLifecycleOwner)

        verify(networkProtectionPixels).reportGeoswitchingNoLocations()
    }

    @Test
    fun `onStart - netp not enabled - emit pixel for nearest`() = runTest {
        fakeRepository.setUserPreferredLocation(UserPreferredLocation(countryCode = "us", cityName = "El Segundo"))
        whenever(networkProtectionState.isEnabled()).thenReturn(false)
        testee.onStart(mockLifecycleOwner)

        fakeRepository.setUserPreferredLocation(UserPreferredLocation())
        testee.onStop(mockLifecycleOwner)

        verify(networkProtectionPixels).reportPreferredLocationSetToNearest()
    }

    @Test
    fun `onStart - netp enabled and preferred location changed to nearest - emit pixel for nearest`() = runTest {
        fakeRepository.setUserPreferredLocation(UserPreferredLocation(countryCode = "us", cityName = "El Segundo"))
        whenever(networkProtectionState.isEnabled()).thenReturn(true)
        testee.onStart(mockLifecycleOwner)

        fakeRepository.setUserPreferredLocation(UserPreferredLocation())
        testee.onStop(mockLifecycleOwner)

        verify(networkProtectionPixels).reportPreferredLocationSetToNearest()
    }

    @Test
    fun `onStart - netp not enabled - emit pixel for custom`() = runTest {
        fakeRepository.setUserPreferredLocation(UserPreferredLocation())
        whenever(networkProtectionState.isEnabled()).thenReturn(false)
        testee.onStart(mockLifecycleOwner)

        fakeRepository.setUserPreferredLocation(UserPreferredLocation(countryCode = "us"))
        testee.onStop(mockLifecycleOwner)

        verify(networkProtectionPixels).reportPreferredLocationSetToCustom()
    }

    @Test
    fun `onStart - netp enabled and preferred location changed to custom - emit pixel for custom`() = runTest {
        fakeRepository.setUserPreferredLocation(UserPreferredLocation())
        whenever(networkProtectionState.isEnabled()).thenReturn(true)
        testee.onStart(mockLifecycleOwner)

        fakeRepository.setUserPreferredLocation(UserPreferredLocation(countryCode = "us", cityName = "El Segundo"))
        testee.onStop(mockLifecycleOwner)

        verify(networkProtectionPixels).reportPreferredLocationSetToCustom()
    }

    @Test
    fun `onStart - netP enabled but no change in custom preferred location - emit no pixels`() = runTest {
        fakeRepository.setUserPreferredLocation(UserPreferredLocation(countryCode = "us"))
        whenever(networkProtectionState.isEnabled()).thenReturn(true)

        testee.onStart(mockLifecycleOwner)
        testee.onCountrySelected("us")
        testee.onStop(mockLifecycleOwner)

        verifyNoMoreInteractions(networkProtectionPixels)
    }

    @Test
    fun `onStart - netP enabled but no change in default preferred location - emit no pixels`() = runTest {
        whenever(networkProtectionState.isEnabled()).thenReturn(true)

        testee.onStart(mockLifecycleOwner)
        testee.onStop(mockLifecycleOwner)

        verifyNoMoreInteractions(networkProtectionPixels)
    }
}

class FakeNetPGeoswitchingRepository : NetPGeoswitchingRepository {
    private var _userPreferredCountry: String? = null
    private var _userPreferredCity: String? = null
    private var locations: List<NetPGeoswitchingLocation> = emptyList()

    override suspend fun getUserPreferredLocation(): UserPreferredLocation = UserPreferredLocation(_userPreferredCountry, _userPreferredCity)

    override suspend fun setUserPreferredLocation(userPreferredLocation: UserPreferredLocation) {
        _userPreferredCountry = userPreferredLocation.countryCode
        _userPreferredCity = userPreferredLocation.cityName
    }

    override fun getLocations(): List<NetPGeoswitchingLocation> = locations

    override fun getLocationsFlow(): Flow<List<NetPGeoswitchingLocation>> = TODO()

    override fun replaceLocations(locations: List<NetPGeoswitchingLocation>) {
        this.locations = locations
    }
}
