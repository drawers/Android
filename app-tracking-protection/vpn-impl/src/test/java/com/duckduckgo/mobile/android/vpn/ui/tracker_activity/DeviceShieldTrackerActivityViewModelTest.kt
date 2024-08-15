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

package com.duckduckgo.mobile.android.vpn.ui.tracker_activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.mobile.android.vpn.AppTpVpnFeature
import com.duckduckgo.mobile.android.vpn.feature.removal.VpnFeatureRemover
import com.duckduckgo.mobile.android.vpn.network.ExternalVpnDetector
import com.duckduckgo.mobile.android.vpn.pixels.DeviceShieldPixels
import com.duckduckgo.mobile.android.vpn.state.VpnStateMonitor
import com.duckduckgo.mobile.android.vpn.stats.AppTrackerBlockingStatsRepository
import com.duckduckgo.mobile.android.vpn.ui.onboarding.VpnStore
import com.duckduckgo.mobile.android.vpn.ui.tracker_activity.DeviceShieldTrackerActivityViewModel.BannerState
import com.duckduckgo.mobile.android.vpn.ui.tracker_activity.DeviceShieldTrackerActivityViewModel.ViewEvent
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalTime
class DeviceShieldTrackerActivityViewModelTest {

    @get:Rule
    @Suppress("unused")
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    @Suppress("unused")
    val coroutineRule = CoroutineTestRule()

    private lateinit var viewModel: DeviceShieldTrackerActivityViewModel

    private val appTrackerBlockingStatsRepository = mock<AppTrackerBlockingStatsRepository>()
    private val deviceShieldPixels = mock<DeviceShieldPixels>()
    private val vpnDetector = mock<ExternalVpnDetector>()
    private val vpnStateMonitor = mock<VpnStateMonitor>()
    private val vpnFeatureRemover = mock<VpnFeatureRemover>()
    private val vpnStore = mock<VpnStore>()

    @Before
    fun setup() {
        viewModel = DeviceShieldTrackerActivityViewModel(
            deviceShieldPixels,
            appTrackerBlockingStatsRepository,
            vpnStateMonitor,
            vpnDetector,
            vpnFeatureRemover,
            vpnStore,
            coroutineRule.testDispatcherProvider,
        )
    }

    @Test
    fun `getRunningState - return running state`() = runTest {
        whenever(vpnStateMonitor.getStateFlow(AppTpVpnFeature.APPTP_VPN)).thenReturn(
            flow { emit(VpnStateMonitor.VpnState(VpnStateMonitor.VpnRunningState.ENABLED)) },
        )

        viewModel.getRunningState().test {
            assertEquals(VpnStateMonitor.VpnState(VpnStateMonitor.VpnRunningState.ENABLED), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onViewEvent - launch app trackers view event - command is launch app trackers`() = runBlocking {
        viewModel.commands().test {
            viewModel.onViewEvent(ViewEvent.LaunchAppTrackersFAQ)

            assertEquals(DeviceShieldTrackerActivityViewModel.Command.LaunchAppTrackersFAQ, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onViewEvent - launch device shield FAQ - command is launch device shield FAQ`() = runBlocking {
        viewModel.commands().test {
            viewModel.onViewEvent(ViewEvent.LaunchDeviceShieldFAQ)

            assertEquals(DeviceShieldTrackerActivityViewModel.Command.LaunchDeviceShieldFAQ, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onViewEvent - launch excluded apps - command is launch excluded apps`() = runBlocking {
        viewModel.commands().test {
            viewModel.onViewEvent(ViewEvent.LaunchExcludedApps)

            assertEquals(DeviceShieldTrackerActivityViewModel.Command.LaunchManageAppsProtection, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onViewEvent - launch most recent activity - command is launch most recent activity`() = runBlocking {
        viewModel.commands().test {
            viewModel.onViewEvent(ViewEvent.LaunchMostRecentActivity)

            assertEquals(DeviceShieldTrackerActivityViewModel.Command.LaunchMostRecentActivity, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppTPToggleSwitched - other VPN disabled - tracking protection enabled`() = runBlocking {
        whenever(vpnDetector.isExternalVpnDetected()).thenReturn(false)
        viewModel.commands().test {
            viewModel.onAppTPToggleSwitched(true)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.CheckVPNPermission, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppTPToggleSwitched - other VPN disabled - shows confirmation dialog`() = runBlocking {
        whenever(vpnDetector.isExternalVpnDetected()).thenReturn(false)
        viewModel.commands().test {
            viewModel.onAppTPToggleSwitched(false)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.ShowDisableVpnConfirmationDialog, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onVPNPermissionNeeded - vpn permission needed - vpn permission request launched`() = runBlocking {
        viewModel.commands().test {
            val permissionIntent = Intent()
            viewModel.onVPNPermissionNeeded(permissionIntent)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.RequestVPNPermission(permissionIntent), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onVPNPermissionResult - result is OK - VPN is launched`() = runBlocking {
        viewModel.commands().test {
            viewModel.onVPNPermissionResult(AppCompatActivity.RESULT_OK)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.LaunchVPN, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onVPNPermissionResult - always on disabled and system killed app - show always on promotion`() = runBlocking {
        whenever(vpnStateMonitor.isAlwaysOnEnabled()).thenReturn(false)
        whenever(vpnStateMonitor.vpnLastDisabledByAndroid()).thenReturn(true)

        viewModel.commands().test {
            viewModel.onVPNPermissionResult(AppCompatActivity.RESULT_OK)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.LaunchVPN, awaitItem())
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.ShowAlwaysOnPromotionDialog, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onVPNPermissionResult - always on disabled and system did not kill app - do not show always on promotion`() = runBlocking {
        whenever(vpnStateMonitor.isAlwaysOnEnabled()).thenReturn(false)
        whenever(vpnStateMonitor.vpnLastDisabledByAndroid()).thenReturn(false)

        viewModel.commands().test {
            viewModel.onVPNPermissionResult(AppCompatActivity.RESULT_OK)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.LaunchVPN, expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onVPNPermissionResult - VPN in always on mode - does not send promote always on dialog command`() = runBlocking {
        whenever(vpnStateMonitor.isAlwaysOnEnabled()).thenReturn(true)
        whenever(vpnStateMonitor.vpnLastDisabledByAndroid()).thenReturn(true)

        viewModel.commands().test {
            viewModel.onVPNPermissionResult(AppCompatActivity.RESULT_OK)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.LaunchVPN, expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onVPNPermissionResult - permission denied and request time too short - show VPN conflict dialog`() = runBlocking {
        viewModel.commands().test {
            val permissionIntent = Intent()
            viewModel.onVPNPermissionNeeded(permissionIntent)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.RequestVPNPermission(permissionIntent), awaitItem())

            viewModel.onVPNPermissionResult(AppCompatActivity.RESULT_CANCELED)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.ShowVpnAlwaysOnConflictDialog, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onVPNPermissionResult - request time higher than needed - VPN is stopped`() = runBlocking {
        viewModel.commands().test {
            val permissionIntent = Intent()
            viewModel.onVPNPermissionNeeded(permissionIntent)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.RequestVPNPermission(permissionIntent), awaitItem())

            delay(2000)
            viewModel.onVPNPermissionResult(AppCompatActivity.RESULT_CANCELED)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.VPNPermissionNotGranted, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppTPToggleSwitched - other VPN enabled - shows VPN conflict dialog`() = runBlocking {
        whenever(vpnDetector.isExternalVpnDetected()).thenReturn(true)
        viewModel.commands().test {
            viewModel.onAppTPToggleSwitched(true)
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.ShowVpnConflictDialog, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppTpManuallyDisabled - tracking protection stopped`() = runBlocking {
        viewModel.commands().test {
            viewModel.onAppTpManuallyDisabled()

            verify(deviceShieldPixels).disableFromSummaryTrackerActivity()
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.StopVPN, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onViewEvent - ask to remove feature - dialog is shown`() = runBlocking {
        viewModel.commands().test {
            viewModel.onViewEvent(ViewEvent.AskToRemoveFeature)

            assertEquals(DeviceShieldTrackerActivityViewModel.Command.ShowRemoveFeatureConfirmationDialog, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onViewEvent - remove feature - feature removed and VPN and screen closed`() = runBlocking {
        viewModel.commands().test {
            viewModel.onViewEvent(ViewEvent.RemoveFeature)

            verify(deviceShieldPixels).didChooseToRemoveTrackingProtectionFeature()
            verify(vpnFeatureRemover).manuallyRemoveFeature()
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.StopVPN, awaitItem())
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.CloseScreen, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onViewEvent - promote always on open settings selected - command is sent`() = runBlocking {
        viewModel.commands().test {
            viewModel.onViewEvent(ViewEvent.PromoteAlwaysOnOpenSettings)

            verify(deviceShieldPixels).didChooseToOpenSettingsFromPromoteAlwaysOnDialog()
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.OpenVpnSettings, awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `showAppTpEnabledCtaIfNeeded - AppTp enabled CTA shown - no command sent`() = runBlocking {
        whenever(vpnStore.didShowAppTpEnabledCta()).thenReturn(true)

        viewModel.commands().test {
            viewModel.showAppTpEnabledCtaIfNeeded()

            expectNoEvents()
            verify(vpnStore, never()).appTpEnabledCtaDidShow()
        }
    }

    @Test
    fun `showAppTpEnabledCtaIfNeeded - AppTp enabled CTA not shown and list shown - command sent`() = runBlocking {
        whenever(vpnStore.didShowAppTpEnabledCta()).thenReturn(false)

        viewModel.commands().test {
            viewModel.showAppTpEnabledCtaIfNeeded()

            verify(vpnStore).appTpEnabledCtaDidShow()
            assertEquals(DeviceShieldTrackerActivityViewModel.Command.ShowAppTpEnabledCta, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `bannerState - called outside onboarding session - return next session banner`() {
        whenever(vpnStore.getAndSetOnboardingSession()).thenReturn(false)

        val bannerState = viewModel.bannerState()

        assertEquals(BannerState.NextSessionBanner, bannerState)
    }

    @Test
    fun `bannerState - during onboarding session - return onboarding banner`() {
        whenever(vpnStore.getAndSetOnboardingSession()).thenReturn(true)

        val bannerState = viewModel.bannerState()

        assertEquals(BannerState.OnboardingBanner, bannerState)
    }
}
