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

import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.networkprotection.impl.configuration.WgServerDebugProvider
import com.duckduckgo.networkprotection.store.NetPGeoswitchingRepository
import com.duckduckgo.networkprotection.store.NetPGeoswitchingRepository.UserPreferredLocation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class RealDisplayablePreferredLocationProviderTest {
    @get:Rule
    var coroutineRule = CoroutineTestRule()
    private lateinit var testee: RealDisplayablePreferredLocationProvider

    @Mock
    private lateinit var netPGeoswitchingRepository: NetPGeoswitchingRepository

    @Mock
    private lateinit var weServerDebugProvider: WgServerDebugProvider

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testee = RealDisplayablePreferredLocationProvider(
            netPGeoswitchingRepository,
            weServerDebugProvider,
            coroutineRule.testDispatcherProvider,
        )
    }

    @Test
    fun `getDisplayablePreferredLocation - no internal server override and only country preferred - return country only`() = runTest {
        whenever(weServerDebugProvider.getSelectedServerName()).thenReturn(null)
        whenever(netPGeoswitchingRepository.getUserPreferredLocation()).thenReturn(UserPreferredLocation(countryCode = "us"))

        assertEquals("United States", testee.getDisplayablePreferredLocation())
    }

    @Test
    fun `getDisplayablePreferredLocation - no internal server override and no preferred - return null`() = runTest {
        whenever(weServerDebugProvider.getSelectedServerName()).thenReturn(null)
        whenever(netPGeoswitchingRepository.getUserPreferredLocation()).thenReturn(UserPreferredLocation())

        assertNull(testee.getDisplayablePreferredLocation())
    }

    @Test
    fun `getDisplayablePreferredLocation - no internal server override and city country preferred - return city and country`() = runTest {
        whenever(weServerDebugProvider.getSelectedServerName()).thenReturn(null)
        whenever(netPGeoswitchingRepository.getUserPreferredLocation()).thenReturn(UserPreferredLocation(countryCode = "us", cityName = "El Segundo"))

        assertEquals("El Segundo, United States", testee.getDisplayablePreferredLocation())
    }

    @Test
    fun `getDisplayablePreferredLocation - internal server set with country - return server name and location`() = runTest {
        whenever(weServerDebugProvider.getSelectedServerName()).thenReturn("test server")
        whenever(netPGeoswitchingRepository.getUserPreferredLocation()).thenReturn(UserPreferredLocation(countryCode = "se", cityName = "Stockholm"))

        assertEquals("test server (Stockholm, Sweden)", testee.getDisplayablePreferredLocation())
    }

    @Test
    fun `getDisplayablePreferredLocation - internal server set without country - return server name only`() = runTest {
        whenever(weServerDebugProvider.getSelectedServerName()).thenReturn("test server")
        whenever(netPGeoswitchingRepository.getUserPreferredLocation()).thenReturn(UserPreferredLocation())

        assertEquals("test server", testee.getDisplayablePreferredLocation())
    }
}
