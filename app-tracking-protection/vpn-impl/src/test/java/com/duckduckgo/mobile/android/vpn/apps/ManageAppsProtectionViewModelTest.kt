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

package com.duckduckgo.mobile.android.vpn.apps

import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.mobile.android.vpn.R.string
import com.duckduckgo.mobile.android.vpn.apps.AppsProtectionType.AppInfoType
import com.duckduckgo.mobile.android.vpn.apps.AppsProtectionType.FilterType
import com.duckduckgo.mobile.android.vpn.apps.AppsProtectionType.InfoPanelType
import com.duckduckgo.mobile.android.vpn.apps.BannerContent.ALL_OR_PROTECTED_APPS
import com.duckduckgo.mobile.android.vpn.apps.BannerContent.CUSTOMISED_PROTECTION
import com.duckduckgo.mobile.android.vpn.apps.BannerContent.UNPROTECTED_APPS
import com.duckduckgo.mobile.android.vpn.apps.ui.TrackingProtectionExclusionListActivity
import com.duckduckgo.mobile.android.vpn.breakage.ReportBreakageScreen
import com.duckduckgo.mobile.android.vpn.exclusion.AppCategory
import com.duckduckgo.mobile.android.vpn.pixels.DeviceShieldPixels
import com.duckduckgo.mobile.android.vpn.stats.AppTrackerBlockingStatsRepository
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalTime
@RunWith(AndroidJUnit4::class)
class ManageAppsProtectionViewModelTest {

    @get:Rule
    @Suppress("unused")
    val coroutineRule = CoroutineTestRule()

    private val trackingProtectionAppsRepository = mock<TrackingProtectionAppsRepository>()
    private val appTrackersRepository = mock<AppTrackerBlockingStatsRepository>()
    private val deviceShieldPixels = mock<DeviceShieldPixels>()
    private val manuallyExcludedApps = Channel<List<Pair<String, Boolean>>>(1, BufferOverflow.DROP_OLDEST)

    private lateinit var viewModel: ManageAppsProtectionViewModel

    @Before
    fun setup() {
        whenever(trackingProtectionAppsRepository.manuallyExcludedApps()).thenReturn(manuallyExcludedApps.consumeAsFlow())

        viewModel = ManageAppsProtectionViewModel(
            trackingProtectionAppsRepository,
            appTrackersRepository,
            deviceShieldPixels,
            coroutineRule.testDispatcherProvider,
            emptyList(),
            coroutineRule.testScope,
            { false },
        )
    }

    @Test
    fun `onResume - app is manually excluded - user made changes returns true`() = runTest {
        manuallyExcludedApps.send(listOf())

        viewModel.onResume(TestLifecycleOwner())

        manuallyExcludedApps.send(listOf("package.com" to true))

        assertTrue(viewModel.userMadeChanges())
    }

    @Test
    fun `whenNoManuallyExcludedAppsThenUserMadeChangesReturnsFalse - user made changes`() = runTest {
        manuallyExcludedApps.send(listOf())

        assertFalse(viewModel.userMadeChanges())
    }

    @Test
    fun `onAppProtectionDisabled - package name excluded - manually excludes app`() = runTest {
        val packageName = "com.package.name"
        val appName = "App"
        val report = true

        viewModel.onAppProtectionDisabled(appName, packageName, report)

        verify(trackingProtectionAppsRepository).manuallyExcludeApp(packageName)
    }

    @Test
    fun `onAppProtection - app protection submitted and report skipped - skip pixel sent`() = runTest {
        val packageName = "com.package.name"
        val appName = "App"
        val report = false

        viewModel.onAppProtectionDisabled(appName, packageName, report)

        verify(deviceShieldPixels).didSkipManuallyDisableAppProtectionDialog()
    }

    @Test
    fun `onAppProtection - app protection submitted and report sent - submit pixel is sent`() = runTest {
        val packageName = "com.package.name"
        val appName = "App"
        val report = true

        viewModel.onAppProtectionDisabled(appName, packageName, report)

        verify(deviceShieldPixels).didSubmitManuallyDisableAppProtectionDialog()
    }

    @Test
    fun `onAppProtectionisSubmitted - report sent - report command is sent`() = runTest {
        val packageName = "com.package.name"
        val appName = "App"
        val report = true

        viewModel.commands().test {
            viewModel.onAppProtectionDisabled(appName, packageName, report)

            assertEquals(Command.LaunchFeedback(ReportBreakageScreen.IssueDescriptionForm("apptp", emptyList(), appName, packageName)), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppProtectionEnabled - package name enabled - protected apps enables it`() = runTest {
        val packageName = "com.package.name"
        viewModel.onAppProtectionEnabled(packageName)

        verify(deviceShieldPixels).didEnableAppProtectionFromApps()
        verify(trackingProtectionAppsRepository).manuallyEnabledApp(packageName)
    }

    @Test
    fun `onAppProtectionEnabled - package name enabled - sends pixel`() = runTest {
        val packageName = "com.package.name"
        viewModel.onAppProtectionEnabled(packageName)

        verify(trackingProtectionAppsRepository).manuallyEnabledApp(packageName)
    }

    @Test
    fun `commands - restore default - restart vpn and restore lists`() = runTest {
        viewModel.commands().test {
            viewModel.restoreProtectedApps()
            assertEquals(Command.RestartVpn, awaitItem())
            verify(trackingProtectionAppsRepository).restoreDefaultProtectedList()
            verify(deviceShieldPixels).restoreDefaultProtectionList()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `restoreProtectedApps - do not restart vpn on leaving screen`() = runTest {
        viewModel.commands().test {
            viewModel.restoreProtectedApps()
            assertEquals(Command.RestartVpn, awaitItem())
            viewModel.onPause(mock())
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onPause - vpn restarted`() = runTest {
        viewModel.commands().test {
            manuallyExcludedApps.send(listOf())
            viewModel.onResume(TestLifecycleOwner())
            manuallyExcludedApps.send(listOf("com.package.name" to true))
            viewModel.onPause(TestLifecycleOwner())
            assertEquals(Command.RestartVpn, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onPause - no changes made - vpn not restarted`() = runTest {
        viewModel.commands().test {
            manuallyExcludedApps.send(listOf())
            viewModel.onResume(TestLifecycleOwner())
            viewModel.onPause(TestLifecycleOwner())
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppProtectionChanged - enable protection dialog shown`() = runTest {
        viewModel.commands().test {
            viewModel.onAppProtectionChanged(appWithKnownIssues, 0, true)
            assertEquals(Command.ShowEnableProtectionDialog(appWithKnownIssues, 0), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppProtectionChanged - app with known issues disabled - no dialog shown`() = runTest {
        viewModel.commands().test {
            viewModel.onAppProtectionChanged(appWithKnownIssues, 0, false)
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppProtectionChanged - app loads websites enabled - enable protection dialog shown`() = runTest {
        viewModel.commands().test {
            viewModel.onAppProtectionChanged(appLoadsWebsites, 0, true)
            assertEquals(Command.ShowEnableProtectionDialog(appLoadsWebsites, 0), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppProtectionChanged - app loads websites disabled - no dialog shown`() = runTest {
        viewModel.commands().test {
            viewModel.onAppProtectionChanged(appLoadsWebsites, 0, false)
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppProtectionChanged - app with no issues - no dialog shown`() = runTest {
        viewModel.commands().test {
            viewModel.onAppProtectionChanged(appWithoutIssues, 0, true)
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppProtectionChanged - app with no issues - disabled dialog shown`() = runTest {
        viewModel.commands().test {
            viewModel.onAppProtectionChanged(appWithoutIssues, 0, false)
            assertEquals(Command.ShowDisableProtectionDialog(appWithoutIssues), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppProtectionChanged - app manually disabled is enabled - no dialog shown`() = runTest {
        viewModel.commands().test {
            viewModel.onAppProtectionChanged(appManuallyExcluded, 0, true)
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onAppProtectionChanged - app manually disabled - disable dialog shown`() = runTest {
        viewModel.commands().test {
            viewModel.onAppProtectionChanged(appManuallyExcluded, 0, false)
            assertEquals(Command.ShowDisableProtectionDialog(appManuallyExcluded), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `applyAppsFilter - get protected apps called - returns protected and unprotected apps`() = runTest {
        val protectedApps = listOf(appWithoutIssues)
        val unprotectedApps = listOf(appWithKnownIssues, appLoadsWebsites, appManuallyExcluded)
        val allApps = protectedApps + unprotectedApps

        val panelType = InfoPanelType(ALL_OR_PROTECTED_APPS)
        val filterType = FilterType(string.atp_ExcludedAppsFilterAllLabel, allApps.size)
        val appsList = listOf(panelType, filterType).plus(
            listOf(
                appInfoWithoutIssues,
                appInfoWithKnownIssues,
                appInfoLoadsWebsites,
                appInfoManuallyExcluded,
            ),
        )

        whenever(trackingProtectionAppsRepository.getAppsAndProtectionInfo()).thenReturn(
            flowOf(
                allApps,
            ),
        )
        viewModel.applyAppsFilter(TrackingProtectionExclusionListActivity.Companion.AppsFilter.ALL)

        viewModel.getProtectedApps().test {
            assertEquals(ViewState(appsList), awaitItem())
        }
    }

    @Test
    fun `getProtectedApps - when protected only filter applied - returns only protected apps`() = runTest {
        val protectedApps = listOf(appWithoutIssues)
        val unprotectedApps = listOf(appWithKnownIssues, appLoadsWebsites, appManuallyExcluded)
        val allApps = protectedApps + unprotectedApps

        val panelType = InfoPanelType(ALL_OR_PROTECTED_APPS)
        val filterType = FilterType(string.atp_ExcludedAppsFilterProtectedLabel, protectedApps.size)
        val appsList = listOf(panelType, filterType).plus(
            listOf(
                appInfoWithoutIssues,
            ),
        )

        whenever(trackingProtectionAppsRepository.getAppsAndProtectionInfo()).thenReturn(
            flowOf(
                allApps,
            ),
        )
        viewModel.applyAppsFilter(TrackingProtectionExclusionListActivity.Companion.AppsFilter.PROTECTED_ONLY)

        viewModel.getProtectedApps().test {
            assertEquals(
                ViewState(appsList),
                awaitItem(),
            )
        }
    }

    @Test
    fun `applyAppsFilter - get protected apps called - only header items returned`() = runTest {
        val protectedApps = emptyList<TrackingProtectionAppInfo>()
        val unprotectedApps = listOf(appWithKnownIssues, appLoadsWebsites, appManuallyExcluded)
        val allApps = protectedApps + unprotectedApps
        val panelType = InfoPanelType(ALL_OR_PROTECTED_APPS)
        val filterType = FilterType(string.atp_ExcludedAppsFilterProtectedLabel, 0)
        val appsList = listOf(panelType, filterType)

        whenever(trackingProtectionAppsRepository.getAppsAndProtectionInfo()).thenReturn(
            flowOf(
                allApps,
            ),
        )
        viewModel.applyAppsFilter(TrackingProtectionExclusionListActivity.Companion.AppsFilter.PROTECTED_ONLY)

        viewModel.getProtectedApps().test {
            assertEquals(ViewState(appsList), awaitItem())
        }
    }

    @Test
    fun `getProtectedApps - when unprotected only filter applied and all apps are protected - headers returned`() = runTest {
        val protectedApps = listOf(appWithoutIssues)
        val unprotectedApps = emptyList<TrackingProtectionAppInfo>()
        val allApps = protectedApps + unprotectedApps
        val panelType = InfoPanelType(UNPROTECTED_APPS)
        val filterType = FilterType(string.atp_ExcludedAppsFilterUnprotectedLabel, unprotectedApps.size)
        val appsList = listOf(panelType, filterType)

        whenever(trackingProtectionAppsRepository.getAppsAndProtectionInfo()).thenReturn(
            flowOf(
                allApps,
            ),
        )
        viewModel.applyAppsFilter(TrackingProtectionExclusionListActivity.Companion.AppsFilter.UNPROTECTED_ONLY)

        viewModel.getProtectedApps().test {
            assertEquals(
                ViewState(appsList),
                awaitItem(),
            )
        }
    }

    @Test
    fun `applyAppsFilter - get protected apps called - only unprotected apps returned and unprotected apps banner shown`() =
        runTest {
            val protectedApps = listOf(appWithoutIssues)
            val unprotectedApps = listOf(appWithKnownIssues, appLoadsWebsites, appManuallyExcluded)
            val allApps = protectedApps + unprotectedApps
            val panelType = InfoPanelType(UNPROTECTED_APPS)
            val filterType = FilterType(string.atp_ExcludedAppsFilterUnprotectedLabel, unprotectedApps.size)
            val appsList = listOf(panelType, filterType).plus(listOf(appInfoWithKnownIssues, appInfoLoadsWebsites, appInfoManuallyExcluded))

            whenever(trackingProtectionAppsRepository.getAppsAndProtectionInfo()).thenReturn(
                flowOf(
                    allApps,
                ),
            )
            viewModel.applyAppsFilter(TrackingProtectionExclusionListActivity.Companion.AppsFilter.UNPROTECTED_ONLY)

            viewModel.getProtectedApps().test {
                assertEquals(
                    ViewState(appsList),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `getProtectedApps - at least one app manually excluded - show all or protected banner content`() = runTest {
        val allApps = listOf(appWithKnownIssues, appManuallyExcluded)
        val panelType = InfoPanelType(ALL_OR_PROTECTED_APPS)
        val filterType = FilterType(string.atp_ExcludedAppsFilterAllLabel, allApps.size)
        val appsList = listOf(panelType, filterType).plus(listOf(appInfoWithKnownIssues, appInfoManuallyExcluded))

        whenever(trackingProtectionAppsRepository.getAppsAndProtectionInfo()).thenReturn(
            flowOf(
                allApps,
            ),
        )
        viewModel.applyAppsFilter(TrackingProtectionExclusionListActivity.Companion.AppsFilter.ALL)

        viewModel.getProtectedApps().test {
            assertEquals(ViewState(appsList), awaitItem())
        }
    }

    @Test
    fun `getProtectedApps - at least one app problematic not excluded - show custom banner content`() = runTest {
        val allApps = listOf(appWithKnownIssues, appProblematicNotExcluded)

        val panelType = InfoPanelType(CUSTOMISED_PROTECTION)
        val filterType = FilterType(string.atp_ExcludedAppsFilterAllLabel, allApps.size)
        val appsList = listOf(panelType, filterType).plus(listOf(appInfoWithKnownIssues, appInfoProblematicNotExcluded))

        whenever(trackingProtectionAppsRepository.getAppsAndProtectionInfo()).thenReturn(
            flowOf(
                allApps,
            ),
        )
        viewModel.applyAppsFilter(TrackingProtectionExclusionListActivity.Companion.AppsFilter.ALL)

        viewModel.getProtectedApps().test {
            assertEquals(ViewState(appsList), awaitItem())
        }
    }

    private val appWithKnownIssues =
        TrackingProtectionAppInfo(
            packageName = "com.package.name",
            name = "App",
            category = AppCategory.Undefined,
            isExcluded = true,
            knownProblem = TrackingProtectionAppInfo.KNOWN_ISSUES_EXCLUSION_REASON,
            userModified = false,
        )
    private val appInfoWithKnownIssues = AppInfoType(appWithKnownIssues)

    private val appLoadsWebsites =
        TrackingProtectionAppInfo(
            packageName = "com.package.name",
            name = "App",
            category = AppCategory.Undefined,
            isExcluded = true,
            knownProblem = TrackingProtectionAppInfo.LOADS_WEBSITES_EXCLUSION_REASON,
            userModified = false,
        )
    private val appInfoLoadsWebsites = AppInfoType(appLoadsWebsites)

    private val appManuallyExcluded =
        TrackingProtectionAppInfo(
            packageName = "com.package.name",
            name = "App",
            category = AppCategory.Undefined,
            isExcluded = true,
            knownProblem = TrackingProtectionAppInfo.NO_ISSUES,
            userModified = true,
        )
    private val appInfoManuallyExcluded = AppInfoType(appManuallyExcluded)

    private val appWithoutIssues =
        TrackingProtectionAppInfo(
            packageName = "com.package.name",
            name = "App",
            category = AppCategory.Undefined,
            isExcluded = false,
            knownProblem = TrackingProtectionAppInfo.NO_ISSUES,
            userModified = false,
        )
    private val appInfoWithoutIssues = AppInfoType(appWithoutIssues)

    private val appProblematicNotExcluded =
        TrackingProtectionAppInfo(
            packageName = "com.package.name",
            name = "App",
            category = AppCategory.Undefined,
            isExcluded = false,
            knownProblem = TrackingProtectionAppInfo.KNOWN_ISSUES_EXCLUSION_REASON,
            userModified = true,
        )
    private val appInfoProblematicNotExcluded = AppInfoType(appProblematicNotExcluded)
}
