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

package com.duckduckgo.mobile.android.vpn.apps

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.mobile.android.vpn.apps.TrackingProtectionAppsRepository.ProtectionState
import com.duckduckgo.mobile.android.vpn.trackers.FakeAppTrackerRepository
import com.duckduckgo.networkprotection.api.NetworkProtectionExclusionList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class TrackingProtectionAppsRepositoryTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private val packageManager: PackageManager = mock()
    private val appTrackerRepository = FakeAppTrackerRepository()
    private val networkProtectionExclusionList: NetworkProtectionExclusionList = mock()

    private lateinit var trackingProtectionAppsRepository: TrackingProtectionAppsRepository

    @Before
    fun setup() {
        whenever(packageManager.getInstalledApplications(PackageManager.GET_META_DATA)).thenReturn(INSTALLED_APPS.asApplicationInfo())
        whenever(packageManager.getApplicationLabel(any())).thenReturn("App Name")
        appTrackerRepository.appExclusionList = EXCLUSION_LIST.toMutableMap()
        appTrackerRepository.manualExclusionList = MANUAL_EXCLUSION_LIST.toMutableMap()
        appTrackerRepository.systemAppOverrides = SYSTEM_OVERRIDE_LIST.toSet()

        trackingProtectionAppsRepository =
            RealTrackingProtectionAppsRepository(
                packageManager,
                appTrackerRepository,
                coroutineRule.testDispatcherProvider,
                networkProtectionExclusionList,
                InstrumentationRegistry.getInstrumentation().targetContext,
            )
    }

    @Test
    fun `getExclusionAppList - returns exclusion list`() = runTest {
        val exclusionList = trackingProtectionAppsRepository.getExclusionAppsList()

        assertEquals(
            listOf("com.example.app2", "com.example.app3", "com.example.app5", "com.example.system", "com.duckduckgo.mobile.android.vpn.test"),
            exclusionList,
        )
    }

    @Test
    fun `isProtectionEnabled - app disabled - returns false`() = runTest {
        whenever(packageManager.getApplicationInfo("com.example.app2", 0))
            .thenReturn(ApplicationInfo().apply { packageName = "com.example.app2" })
        whenever(networkProtectionExclusionList.isExcluded("com.example.app2")).thenReturn(false)

        val protectionState = trackingProtectionAppsRepository.getAppProtectionStatus("com.example.app2")

        assertEquals(ProtectionState.UNPROTECTED, protectionState)
    }

    @Test
    fun `getAppProtectionStatus - app enabled - protected`() = runTest {
        whenever(packageManager.getApplicationInfo("com.example.app1", 0))
            .thenReturn(ApplicationInfo().apply { packageName = "com.example.app1" })
        whenever(networkProtectionExclusionList.isExcluded("com.example.app1")).thenReturn(false)

        val protectionState = trackingProtectionAppsRepository.getAppProtectionStatus("com.example.app1")

        assertEquals(ProtectionState.PROTECTED, protectionState)
    }

    @Test
    fun `getAppProtectionStatus - excluded in netp - unprotected through netp`() = runTest {
        whenever(packageManager.getApplicationInfo("com.example.app1", 0))
            .thenReturn(ApplicationInfo().apply { packageName = "com.example.app1" })
        whenever(networkProtectionExclusionList.isExcluded("com.example.app1")).thenReturn(true)

        val protectionState = trackingProtectionAppsRepository.getAppProtectionStatus("com.example.app1")

        assertEquals(ProtectionState.UNPROTECTED_THROUGH_NETP, protectionState)
    }

    @Test
    fun `getAppProtectionStatus - game - returns unprotected`() = runTest {
        whenever(packageManager.getApplicationInfo("com.example.game", 0))
            .thenReturn(
                ApplicationInfo().apply {
                    packageName = "com.example.game"
                    category = ApplicationInfo.CATEGORY_GAME
                },
            )
        whenever(networkProtectionExclusionList.isExcluded("com.example.game")).thenReturn(false)

        val protectionState = trackingProtectionAppsRepository.getAppProtectionStatus("com.example.game")

        assertEquals(ProtectionState.PROTECTED, protectionState)
    }

    @Test
    fun `getAppProtectionStatus - ddg app - returns unprotected`() = runTest {
        whenever(packageManager.getApplicationInfo("com.duckduckgo.mobile.android.vpn.test", 0))
            .thenReturn(ApplicationInfo().apply { packageName = "com.duckduckgo.mobile.android.vpn.test" })
        whenever(networkProtectionExclusionList.isExcluded("com.duckduckgo.mobile.android.vpn.test")).thenReturn(false)

        val protectionState = trackingProtectionAppsRepository.getAppProtectionStatus("com.duckduckgo.mobile.android.vpn.test")

        assertEquals(ProtectionState.UNPROTECTED, protectionState)
    }

    @Test
    fun `getAppProtectionStatus - unknown package - return protected`() = runTest {
        whenever(packageManager.getApplicationInfo("com.example.unknown", 0))
            .thenReturn(ApplicationInfo().apply { packageName = "com.example.unknown" })
        whenever(networkProtectionExclusionList.isExcluded("com.example.unknown")).thenReturn(false)

        val protectionState = trackingProtectionAppsRepository.getAppProtectionStatus("com.example.unknown")

        assertEquals(ProtectionState.PROTECTED, protectionState)
    }

    @Test
    fun `getAppProtectionStatus - name not found exception thrown - return protected`() = runTest {
        whenever(packageManager.getApplicationInfo("com.example.unknown", 0))
            .thenThrow(NameNotFoundException())

        val protectionState = trackingProtectionAppsRepository.getAppProtectionStatus("com.example.unknown")

        assertEquals(ProtectionState.PROTECTED, protectionState)
    }

    @Test
    fun `manuallyEnabledApp - remove from exclusion list`() = runTest {
        trackingProtectionAppsRepository.manuallyEnabledApp("com.example.app2")

        val exclusionList = trackingProtectionAppsRepository.getExclusionAppsList()

        assertEquals(listOf("com.example.app3", "com.example.app5", "com.example.system", "com.duckduckgo.mobile.android.vpn.test"), exclusionList)
    }

    @Test
    fun `manuallyExcludedApps - returns excluded apps`() = runTest {
        trackingProtectionAppsRepository.manuallyExcludedApps().test {
            assertEquals(
                listOf("com.example.app1" to true, "com.example.app2" to false, "com.example.app3" to false),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `manuallyExcludeApp - add to exclusion list`() = runTest {
        trackingProtectionAppsRepository.manuallyExcludeApp("com.example.app1")

        val exclusionList = trackingProtectionAppsRepository.getExclusionAppsList()

        assertEquals(
            listOf(
                "com.example.app1",
                "com.example.app2",
                "com.example.app3",
                "com.example.app5",
                "com.example.system",
                "com.duckduckgo.mobile.android.vpn.test",
            ),
            exclusionList,
        )
    }

    @Test
    fun `restoreDefaultProtectedList - clear manual exclusion list`() = runTest {
        trackingProtectionAppsRepository.restoreDefaultProtectedList()

        val exclusionList = trackingProtectionAppsRepository.getExclusionAppsList()

        assertEquals(
            listOf("com.example.app1", "com.example.app3", "com.example.app5", "com.example.system", "com.duckduckgo.mobile.android.vpn.test"),
            exclusionList,
        )
    }

    @Test
    fun `getAppsAndProtectionInfo - return apps with protection info`() = runTest {
        whenever(networkProtectionExclusionList.isExcluded(any())).thenReturn(false)
        trackingProtectionAppsRepository.getAppsAndProtectionInfo().test {
            assertEquals(
                listOf(
                    "com.example.app1" to false,
                    "com.example.app2" to true,
                    "com.example.app3" to true,
                    "com.example.app4" to false,
                    "com.example.app5" to true,
                    "com.example.app6" to false,
                    "com.example.game" to false,
                    "com.example.system.overriden" to false,
                ),
                this.awaitItem().map { it.packageName to it.isExcluded },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAppsAndProtectionInfo - with app in netp exclusion list - returns apps with protection info`() = runTest {
        whenever(networkProtectionExclusionList.isExcluded(any())).thenReturn(true)

        trackingProtectionAppsRepository.getAppsAndProtectionInfo().test {
            assertEquals(
                listOf(
                    "com.example.app1" to true,
                    "com.example.app2" to true,
                    "com.example.app3" to true,
                    "com.example.app4" to true,
                    "com.example.app5" to true,
                    "com.example.app6" to true,
                    "com.example.game" to true,
                    "com.example.system.overriden" to true,
                ),
                this.awaitItem().map { it.packageName to it.isExcluded },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAppsAndProtectionInfo - with app in netp exclusion list - returns apps with protection info and correct known problem`() = runTest {
        whenever(networkProtectionExclusionList.isExcluded(any())).thenReturn(true)

        trackingProtectionAppsRepository.getAppsAndProtectionInfo().test {
            assertEquals(
                listOf(
                    "com.example.app1" to TrackingProtectionAppInfo.EXCLUDED_THROUGH_NETP,
                    "com.example.app2" to TrackingProtectionAppInfo.EXCLUDED_THROUGH_NETP,
                    "com.example.app3" to TrackingProtectionAppInfo.EXCLUDED_THROUGH_NETP,
                    "com.example.app4" to TrackingProtectionAppInfo.EXCLUDED_THROUGH_NETP,
                    "com.example.app5" to TrackingProtectionAppInfo.EXCLUDED_THROUGH_NETP,
                    "com.example.app6" to TrackingProtectionAppInfo.EXCLUDED_THROUGH_NETP,
                    "com.example.game" to TrackingProtectionAppInfo.EXCLUDED_THROUGH_NETP,
                    "com.example.system.overriden" to TrackingProtectionAppInfo.EXCLUDED_THROUGH_NETP,
                ),
                this.awaitItem().map { it.packageName to it.knownProblem },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun List<String>.asApplicationInfo(): List<ApplicationInfo> {
        return this.map {
            ApplicationInfo()
                .apply {
                    packageName = it
                    category = if (it == "com.example.game") ApplicationInfo.CATEGORY_GAME else ApplicationInfo.CATEGORY_UNDEFINED
                    flags = if (it.startsWith("com.example.system")) ApplicationInfo.FLAG_SYSTEM else 0
                }
        }
    }

    companion object {
        private val INSTALLED_APPS = listOf(
            "com.example.app1",
            "com.example.app2",
            "com.example.app3",
            "com.example.app4",
            "com.example.app5",
            "com.example.app6",
            "com.example.game", // it's a game and should be protected by default
            "com.example.system", // should be automatically be added to exclusion list
            "com.example.system.overriden",
            "com.duckduckgo.mobile.android.vpn.test", // should be automatically be added to exclusion list, packageName returned during test
        )
        private val EXCLUSION_LIST = mapOf(
            "com.example.app1" to "UNKNOWN",
            "com.example.app3" to "UNKNOWN",
            "com.example.app5" to "Browser",
        )
        private val MANUAL_EXCLUSION_LIST = mapOf(
            "com.example.app1" to true,
            "com.example.app2" to false,
            "com.example.app3" to false,
        )
        private val SYSTEM_OVERRIDE_LIST = listOf(
            "com.example.system.overriden",
        )
    }
}
