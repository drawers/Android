/*
 * Copyright (c) 2024 DuckDuckGo
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

package com.duckduckgo.networkprotection.impl.subscription.settings

import app.cash.turbine.test
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.common.ui.view.listitem.CheckListItem.CheckItemStatus.ENABLED
import com.duckduckgo.common.ui.view.listitem.CheckListItem.CheckItemStatus.WARNING
import com.duckduckgo.navigation.api.GlobalActivityStarter.ActivityParams
import com.duckduckgo.networkprotection.api.NetworkProtectionAccessState
import com.duckduckgo.networkprotection.api.NetworkProtectionAccessState.NetPAccessState.Locked
import com.duckduckgo.networkprotection.api.NetworkProtectionAccessState.NetPAccessState.UnLocked
import com.duckduckgo.networkprotection.api.NetworkProtectionState
import com.duckduckgo.networkprotection.api.NetworkProtectionState.ConnectionState.CONNECTED
import com.duckduckgo.networkprotection.api.NetworkProtectionState.ConnectionState.CONNECTING
import com.duckduckgo.networkprotection.api.NetworkProtectionState.ConnectionState.DISCONNECTED
import com.duckduckgo.networkprotection.impl.R
import com.duckduckgo.networkprotection.impl.pixels.NetworkProtectionPixelNames.NETP_SETTINGS_PRESSED
import com.duckduckgo.networkprotection.impl.subscription.settings.ProSettingNetPViewModel.Command
import com.duckduckgo.networkprotection.impl.subscription.settings.ProSettingNetPViewModel.NetPEntryState.Hidden
import com.duckduckgo.networkprotection.impl.subscription.settings.ProSettingNetPViewModel.NetPEntryState.Pending
import com.duckduckgo.networkprotection.impl.subscription.settings.ProSettingNetPViewModel.NetPEntryState.ShowState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProSettingNetPViewModelTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val pixel: Pixel = mock()
    private val networkProtectionState: NetworkProtectionState = mock()
    private val networkProtectionAccessState: NetworkProtectionAccessState = mock()
    private lateinit var proSettingNetPViewModel: ProSettingNetPViewModel

    @Before
    fun before() {
        proSettingNetPViewModel = ProSettingNetPViewModel(
            networkProtectionAccessState,
            networkProtectionState,
            coroutineTestRule.testDispatcherProvider,
            pixel,
        )
    }

    @Test
    fun `onNetPSettingClicked - return screen for current state`() = runTest {
        val testScreen = object : ActivityParams {}
        whenever(networkProtectionAccessState.getScreenForCurrentState()).thenReturn(testScreen)

        proSettingNetPViewModel.commands().test {
            proSettingNetPViewModel.onNetPSettingClicked()

            assertEquals(Command.OpenNetPScreen(testScreen), awaitItem())
            verify(pixel).fire(NETP_SETTINGS_PRESSED)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onStart - net p entry state shows hidden`() = runTest {
        whenever(networkProtectionState.getConnectionStateFlow()).thenReturn(flowOf(DISCONNECTED))
        whenever(networkProtectionAccessState.getStateFlow()).thenReturn(flowOf(Locked))

        proSettingNetPViewModel.onStart(mock())

        proSettingNetPViewModel.viewState.test {
            assertEquals(
                Hidden,
                expectMostRecentItem().networkProtectionEntryState,
            )
        }
    }

    @Test
    fun `onStart - net p state in beta but not accepted terms - pending`() = runTest {
        whenever(networkProtectionState.getConnectionStateFlow()).thenReturn(flowOf(DISCONNECTED))
        whenever(networkProtectionAccessState.getStateFlow()).thenReturn(flowOf(UnLocked))
        whenever(networkProtectionState.isOnboarded()).thenReturn(false)

        proSettingNetPViewModel.onStart(mock())

        proSettingNetPViewModel.viewState.test {
            assertEquals(
                Pending,
                expectMostRecentItem().networkProtectionEntryState,
            )
        }
    }

    @Test
    fun `onStart - net p state in beta and onboarded and enabled - correct show state`() = runTest {
        whenever(networkProtectionState.getConnectionStateFlow()).thenReturn(flowOf(CONNECTED))
        whenever(networkProtectionAccessState.getStateFlow()).thenReturn(flowOf(UnLocked))
        whenever(networkProtectionState.isOnboarded()).thenReturn(true)

        proSettingNetPViewModel.onStart(mock())

        proSettingNetPViewModel.viewState.test {
            assertEquals(
                ShowState(
                    icon = ENABLED,
                    subtitle = R.string.netpSubscriptionSettingsConnected,
                ),
                expectMostRecentItem().networkProtectionEntryState,
            )
        }
    }

    @Test
    fun `onStart - net p state in beta and not onboarded and enabled - correct show state`() = runTest {
        whenever(networkProtectionState.getConnectionStateFlow()).thenReturn(flowOf(CONNECTED))
        whenever(networkProtectionAccessState.getStateFlow()).thenReturn(flowOf(UnLocked))
        whenever(networkProtectionState.isOnboarded()).thenReturn(false)

        proSettingNetPViewModel.onStart(mock())

        proSettingNetPViewModel.viewState.test {
            assertEquals(
                Pending,
                expectMostRecentItem().networkProtectionEntryState,
            )
        }
    }

    @Test
    fun `onStart - net p state in beta onboarded and enabled - correct show state`() = runTest {
        whenever(networkProtectionState.getConnectionStateFlow()).thenReturn(flowOf(CONNECTED))
        whenever(networkProtectionAccessState.getStateFlow()).thenReturn(flowOf(UnLocked))
        whenever(networkProtectionState.isOnboarded()).thenReturn(true)

        proSettingNetPViewModel.onStart(mock())

        proSettingNetPViewModel.viewState.test {
            assertEquals(
                ShowState(
                    icon = ENABLED,
                    subtitle = R.string.netpSubscriptionSettingsConnected,
                ),
                expectMostRecentItem().networkProtectionEntryState,
            )
        }
    }

    @Test
    fun `onStart - net p state in beta and connecting - correct show state`() = runTest {
        whenever(networkProtectionState.getConnectionStateFlow()).thenReturn(flowOf(CONNECTING))
        whenever(networkProtectionAccessState.getStateFlow()).thenReturn(flowOf(UnLocked))
        whenever(networkProtectionState.isOnboarded()).thenReturn(true)

        proSettingNetPViewModel.onStart(mock())

        proSettingNetPViewModel.viewState.test {
            assertEquals(
                ShowState(
                    icon = ENABLED,
                    subtitle = R.string.netpSubscriptionSettingsConnecting,
                ),
                expectMostRecentItem().networkProtectionEntryState,
            )
        }
    }

    @Test
    fun `onStart - net p state in beta and disabled - show correct state`() = runTest {
        whenever(networkProtectionState.getConnectionStateFlow()).thenReturn(flowOf(DISCONNECTED))
        whenever(networkProtectionAccessState.getStateFlow()).thenReturn(flowOf(UnLocked))
        whenever(networkProtectionState.isOnboarded()).thenReturn(true)

        proSettingNetPViewModel.onStart(mock())

        proSettingNetPViewModel.viewState.test {
            assertEquals(
                ShowState(
                    icon = WARNING,
                    subtitle = R.string.netpSubscriptionSettingsDisconnected,
                ),
                expectMostRecentItem().networkProtectionEntryState,
            )
        }
    }
}
