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

package com.duckduckgo.sync.impl.ui.qrcode

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.sync.impl.ui.qrcode.SquareDecoratedBarcodeViewModel.Command.CheckCameraAvailable
import com.duckduckgo.sync.impl.ui.qrcode.SquareDecoratedBarcodeViewModel.Command.CheckPermissions
import com.duckduckgo.sync.impl.ui.qrcode.SquareDecoratedBarcodeViewModel.Command.OpenSettings
import com.duckduckgo.sync.impl.ui.qrcode.SquareDecoratedBarcodeViewModel.Command.RequestPermissions
import com.duckduckgo.sync.impl.ui.qrcode.SquareDecoratedBarcodeViewModel.ViewState.CameraUnavailable
import com.duckduckgo.sync.impl.ui.qrcode.SquareDecoratedBarcodeViewModel.ViewState.PermissionsGranted
import com.duckduckgo.sync.impl.ui.qrcode.SquareDecoratedBarcodeViewModel.ViewState.PermissionsNotGranted
import com.duckduckgo.sync.impl.ui.qrcode.SquareDecoratedBarcodeViewModel.ViewState.Unknown
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncBarcodeViewModelTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private val permissionDeniedWrapper = PermissionDeniedWrapper()
    private val fakeLifecycleOwner = {
        object : Lifecycle() {
            override fun addObserver(observer: LifecycleObserver) {
                TODO("Not yet implemented")
            }

            override fun removeObserver(observer: LifecycleObserver) {
                TODO("Not yet implemented")
            }

            override fun getCurrentState(): State {
                TODO("Not yet implemented")
            }
        }
    }

    private val testee: SquareDecoratedBarcodeViewModel by lazy {
        SquareDecoratedBarcodeViewModel(permissionDeniedWrapper)
    }

    @Test
    fun `handleCameraAvailability - camera unavailable - viewState camera unavailable`() = runTest {
        testee.handleCameraAvailability(false)

        testee.viewState.test {
            val state = awaitItem()
            Assert.assertEquals(CameraUnavailable, state)
        }
    }

    @Test
    fun `handlePermissions - permission granted - viewState permissions granted`() = runTest {
        testee.handlePermissions(true)

        testee.viewState.test {
            val state = awaitItem()
            Assert.assertEquals(PermissionsGranted, state)
        }
    }

    @Test
    fun `handlePermissions - permission not granted and not denied yet - request permissions`() = runTest {
        permissionDeniedWrapper.permissionAlreadyDenied = false
        testee.handlePermissions(false)

        testee.commands().test {
            val state = awaitItem()
            Assert.assertEquals(RequestPermissions, state)
        }
    }

    @Test
    fun `handlePermissions - permission not granted - viewState unknown`() = runTest {
        testee.handlePermissions(false)

        testee.viewState.test {
            val state = awaitItem()
            Assert.assertEquals(Unknown, state)
        }
    }

    @Test
    fun `handlePermissions - permission not granted and permission already denied - viewState permissions not granted`() = runTest {
        permissionDeniedWrapper.permissionAlreadyDenied = true
        testee.handlePermissions(false)

        testee.viewState.test {
            val state = awaitItem()
            Assert.assertEquals(PermissionsNotGranted, state)
        }
    }

    @Test
    fun `goToSettings - command open settings`() = runTest {
        testee.goToSettings()

        testee.commands().test {
            val state = awaitItem()
            Assert.assertEquals(OpenSettings, state)
        }
    }

    @Test
    fun `goToSettings - viewState unknown`() = runTest {
        testee.goToSettings()

        testee.viewState.test {
            val state = awaitItem()
            Assert.assertEquals(Unknown, state)
        }
    }

    @Test
    fun `onResume - command check camera available`() = runTest {
        testee.onResume(fakeLifecycleOwner)

        testee.commands().test {
            val state = awaitItem()
            Assert.assertEquals(CheckCameraAvailable, state)
        }
    }

    @Test
    fun `onResume - viewState unknown`() = runTest {
        testee.onResume(fakeLifecycleOwner)

        testee.viewState.test {
            val state = awaitItem()
            Assert.assertEquals(Unknown, state)
        }
    }

    @Test
    fun `handleCameraAvailability - camera available - command check permissions`() = runTest {
        testee.handleCameraAvailability(true)

        testee.commands().test {
            val state = awaitItem()
            Assert.assertEquals(CheckPermissions, state)
        }
    }

    @Test
    fun `handleCameraAvailability - camera available - viewState unknown`() = runTest {
        testee.handleCameraAvailability(true)

        testee.viewState.test {
            val state = awaitItem()
            Assert.assertEquals(Unknown, state)
        }
    }

    @Test
    fun `handlePermissions - permission not granted and not denied yet - permission already denied true`() = runTest {
        permissionDeniedWrapper.permissionAlreadyDenied = false

        testee.handlePermissions(false)

        Assert.assertTrue(permissionDeniedWrapper.permissionAlreadyDenied)
    }
}
