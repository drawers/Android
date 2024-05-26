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

package com.duckduckgo.common.ui.notifyme

import android.os.Build
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.common.ui.store.notifyme.NotifyMeDataStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class NotifyMeViewModelTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private val mockAppBuildConfig = mock<AppBuildConfig>()
    private val mockNotifyMeDataStore = mock<NotifyMeDataStore>()
    private val mockLifecycleOwner = mock<LifecycleOwner>()

    private val testee: NotifyMeViewModel by lazy {
        NotifyMeViewModel(mockAppBuildConfig, mockNotifyMeDataStore)
    }

    @Before
    fun setup() {
        testee.init("shared_prefs_key")
    }

    @Test
    fun `viewState - notifications not allowed and dismiss not called and view not dismissed - view is visible`() = runTest {
        setup(
            notificationsAllowed = false,
            dismissCalled = false,
            viewDismissed = false,
        )

        testee.viewState.test {
            val state = awaitItem()
            assertTrue(state.visible)
        }
    }

    @Test
    fun `viewState - notifications not allowed and view dismissed - view is not visible`() = runTest {
        setup(
            notificationsAllowed = false,
            dismissCalled = false,
            viewDismissed = true,
        )

        testee.viewState.test {
            val state = awaitItem()
            assertFalse(state.visible)
        }
    }

    @Test
    fun `viewState - notifications not allowed and dismiss called and view not dismissed - view not visible`() = runTest {
        setup(
            notificationsAllowed = false,
            dismissCalled = true,
            viewDismissed = false,
        )

        testee.viewState.test {
            val state = awaitItem()
            assertFalse(state.visible)
        }
    }

    @Test
    fun `viewState - notifications not allowed and dismiss called and view dismissed - view is not visible`() = runTest {
        setup(
            notificationsAllowed = false,
            dismissCalled = true,
            viewDismissed = true,
        )

        testee.viewState.test {
            val state = awaitItem()
            assertFalse(state.visible)
        }
    }

    @Test
    fun `viewState - notifications allowed and dismiss not called and view not dismissed - view not visible`() = runTest {
        setup(
            notificationsAllowed = true,
            dismissCalled = false,
            viewDismissed = false,
        )

        testee.viewState.test {
            val state = awaitItem()
            assertFalse(state.visible)
        }
    }

    @Test
    fun `viewState - notifications allowed and view dismissed - view is not visible`() = runTest {
        setup(
            notificationsAllowed = true,
            dismissCalled = false,
            viewDismissed = true,
        )

        testee.viewState.test {
            val state = awaitItem()
            assertFalse(state.visible)
        }
    }

    @Test
    fun `viewState - notifications allowed and dismiss called and view not dismissed - view not visible`() = runTest {
        setup(
            notificationsAllowed = true,
            dismissCalled = true,
            viewDismissed = false,
        )

        testee.viewState.test {
            val state = awaitItem()
            assertFalse(state.visible)
        }
    }

    @Test
    fun whenNotificationsAllowedAndDismissCalledAndViewIsDismissedThenViewIsNotVisible() = runTest {
        setup(
            notificationsAllowed = true,
            dismissCalled = true,
            viewDismissed = true,
        )

        testee.viewState.test {
            val state = awaitItem()
            assertFalse(state.visible)
        }
    }

    @Test
    fun `onResume - check permissions command sent`() = runTest {
        testee.onResume(mockLifecycleOwner)

        testee.commands().test {
            assertEquals(
                NotifyMeViewModel.Command.UpdateNotificationsState,
                awaitItem(),
            )
        }
    }

    @Test
    fun `onNotifyMeButtonClicked - Android 13 - check permission rationale command sent`() = runTest {
        whenever(mockAppBuildConfig.sdkInt).thenReturn(Build.VERSION_CODES.TIRAMISU)
        testee.onNotifyMeButtonClicked()

        testee.commands().test {
            assertEquals(
                NotifyMeViewModel.Command.CheckPermissionRationale,
                awaitItem(),
            )
        }
    }

    @Test
    fun `onNotifyMeButtonClicked - Android 8 - open settings command sent`() = runTest {
        whenever(mockAppBuildConfig.sdkInt).thenReturn(Build.VERSION_CODES.O)

        testee.onNotifyMeButtonClicked()

        testee.commands().test {
            assertEquals(
                NotifyMeViewModel.Command.OpenSettingsOnAndroid8Plus,
                awaitItem(),
            )
        }
    }

    @Test
    fun `onCloseButtonClicked - close command sent and set dismissed called`() = runTest {
        testee.onCloseButtonClicked()

        testee.commands().test {
            assertEquals(
                NotifyMeViewModel.Command.DismissComponent,
                awaitItem(),
            )
            verify(mockNotifyMeDataStore).setComponentDismissed(anyString())
        }
    }

    @Test
    fun `handleRequestPermissionRationale - should show rationale true - show permission rationale sent`() = runTest {
        val shouldShowRationale = true

        testee.handleRequestPermissionRationale(shouldShowRationale)

        testee.commands().test {
            assertEquals(
                NotifyMeViewModel.Command.ShowPermissionRationale,
                awaitItem(),
            )
        }
    }

    @Test
    fun `handleRequestPermissionRationale - Android 8 with shouldShowRationale false - open settings command sent`() = runTest {
        whenever(mockAppBuildConfig.sdkInt).thenReturn(Build.VERSION_CODES.O)
        val shouldShowRationale = false

        testee.handleRequestPermissionRationale(shouldShowRationale)

        testee.commands().test {
            assertEquals(
                NotifyMeViewModel.Command.OpenSettingsOnAndroid8Plus,
                awaitItem(),
            )
        }
    }

    @Test
    fun `onResume - Android 13+ - update notifications state command sent`() = runTest {
        whenever(mockAppBuildConfig.sdkInt).thenReturn(Build.VERSION_CODES.TIRAMISU)

        testee.onResume(mockLifecycleOwner)

        testee.commands().test {
            assertEquals(
                NotifyMeViewModel.Command.UpdateNotificationsStateOnAndroid13Plus,
                awaitItem(),
            )
        }
    }

    @Test
    fun `onResume - Android 12 or below - update notifications state command sent`() = runTest {
        whenever(mockAppBuildConfig.sdkInt).thenReturn(Build.VERSION_CODES.S)

        testee.onResume(mockLifecycleOwner)

        testee.commands().test {
            assertEquals(
                NotifyMeViewModel.Command.UpdateNotificationsState,
                awaitItem(),
            )
        }
    }

    private fun setup(notificationsAllowed: Boolean, dismissCalled: Boolean, viewDismissed: Boolean) {
        testee.updateNotificationsPermissions(notificationsAllowed)
        if (dismissCalled) {
            testee.onCloseButtonClicked()
        }
        whenever(mockNotifyMeDataStore.isComponentDismissed(anyString(), anyBoolean())).thenReturn(viewDismissed)
    }
}
