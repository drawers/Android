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

package com.duckduckgo.networkprotection.impl.notification

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.mobile.android.app.tracking.AppTrackingProtection
import com.duckduckgo.mobile.android.vpn.service.VpnEnabledNotificationContentPlugin
import com.duckduckgo.networkprotection.api.NetworkProtectionState
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class NetPEnabledNotificationContentPluginTest {

    @get:Rule
    @Suppress("unused")
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Mock
    private lateinit var appTrackingProtection: AppTrackingProtection

    @Mock
    private lateinit var networkProtectionState: NetworkProtectionState

    @Mock
    private lateinit var notificationActions: NetPNotificationActions

    private lateinit var plugin: NetPEnabledNotificationContentPlugin

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        plugin = NetPEnabledNotificationContentPlugin(
            context.resources,
            networkProtectionState,
            appTrackingProtection,
        ) { null }
    }

    @Test
    fun `getInitialContent - net p disabled - returns null`() = runTest {
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        whenever(networkProtectionState.isEnabled()).thenReturn(false)
        val content = plugin.getInitialContent()

        assertNull(content)
    }

    @Test
    fun `getInitialContent - netP enabled - return content`() = runTest {
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        whenever(networkProtectionState.isEnabled()).thenReturn(true)
        val content = plugin.getInitialContent()

        assertNotNull(content)
        content!!.assertTitleEquals("VPN is connected.")
    }

    @Test
    fun `getUpdatedContent - net p disabled - returns null`() = runTest {
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        whenever(networkProtectionState.isEnabled()).thenReturn(false)
        plugin.getUpdatedContent().test {
            assertNull(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getUpdatedContent - netP enabled no server location - initial content`() = runTest {
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        whenever(networkProtectionState.isEnabled()).thenReturn(true)
        whenever(networkProtectionState.serverLocation()).thenReturn(null)
        plugin.getUpdatedContent().test {
            val item = awaitItem()

            assertNotNull(item)
            item!!.assertTitleEquals("VPN is connected.")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getUpdatedContent - netP enabled - return content`() = runTest {
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        whenever(networkProtectionState.isEnabled()).thenReturn(true)
        whenever(networkProtectionState.serverLocation()).thenReturn("Stockholm, SE")
        plugin.getUpdatedContent().test {
            val item = awaitItem()

            assertNotNull(item)
            item!!.assertTitleEquals("VPN is connected and routing device traffic through Stockholm, SE.")

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getPriority - returns high`() {
        assertEquals(VpnEnabledNotificationContentPlugin.VpnEnabledNotificationPriority.HIGH, plugin.getPriority())
    }

    @Test
    fun `isActive - net p disabled - false`() = runTest {
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        whenever(networkProtectionState.isEnabled()).thenReturn(false)
        assertFalse(plugin.isActive())
    }

    @Test
    fun `isActive - net p enabled - true`() = runTest {
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        whenever(networkProtectionState.isEnabled()).thenReturn(true)
        assertTrue(plugin.isActive())
    }

    @Test
    fun `isActive - net P enabled app TP enabled - return false`() = runTest {
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)
        whenever(networkProtectionState.isEnabled()).thenReturn(true)
        assertFalse(plugin.isActive())
    }
}

private fun VpnEnabledNotificationContentPlugin.VpnEnabledNotificationContent.assertTitleEquals(expected: String) {
    Assert.assertEquals(expected, this.text.toString())
}
