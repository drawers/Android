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

package com.duckduckgo.sync.impl.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.sync.TestSyncFixtures
import com.duckduckgo.sync.TestSyncFixtures.connectedDevice
import com.duckduckgo.sync.TestSyncFixtures.deviceId
import com.duckduckgo.sync.TestSyncFixtures.jsonRecoveryKeyEncoded
import com.duckduckgo.sync.api.SyncState
import com.duckduckgo.sync.api.SyncState.IN_PROGRESS
import com.duckduckgo.sync.api.SyncState.OFF
import com.duckduckgo.sync.api.SyncState.READY
import com.duckduckgo.sync.api.SyncStateMonitor
import com.duckduckgo.sync.api.engine.SyncEngine
import com.duckduckgo.sync.api.engine.SyncEngine.SyncTrigger.FEATURE_READ
import com.duckduckgo.sync.impl.RecoveryCodePDF
import com.duckduckgo.sync.impl.Result
import com.duckduckgo.sync.impl.Result.Success
import com.duckduckgo.sync.impl.SyncAccountRepository
import com.duckduckgo.sync.impl.SyncFeatureToggle
import com.duckduckgo.sync.impl.auth.DeviceAuthenticator
import com.duckduckgo.sync.impl.pixels.SyncPixels
import com.duckduckgo.sync.impl.ui.SyncActivityViewModel.Command
import com.duckduckgo.sync.impl.ui.SyncActivityViewModel.Command.AskTurnOffSync
import com.duckduckgo.sync.impl.ui.SyncActivityViewModel.Command.CheckIfUserHasStoragePermission
import com.duckduckgo.sync.impl.ui.SyncActivityViewModel.Command.IntroCreateAccount
import com.duckduckgo.sync.impl.ui.SyncActivityViewModel.Command.IntroRecoverSyncData
import com.duckduckgo.sync.impl.ui.SyncActivityViewModel.Command.RecoveryCodePDFSuccess
import com.duckduckgo.sync.impl.ui.SyncActivityViewModel.Command.RequestSetupAuthentication
import com.duckduckgo.sync.impl.ui.SyncActivityViewModel.SetupFlows.CreateAccountFlow
import com.duckduckgo.sync.impl.ui.SyncActivityViewModel.SetupFlows.SignInFlow
import com.duckduckgo.sync.impl.ui.SyncDeviceListItem.SyncedDevice
import java.lang.String.format
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SyncActivityViewModelTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val recoveryPDF: RecoveryCodePDF = mock()
    private val syncAccountRepository: SyncAccountRepository = mock()
    private val syncStateMonitor: SyncStateMonitor = mock()
    private val syncEngine: SyncEngine = mock()
    private val syncFeatureToggle: SyncFeatureToggle = mock()
    private val syncPixels: SyncPixels = mock()
    private val deviceAuthenticator: DeviceAuthenticator = mock()

    private val stateFlow = MutableStateFlow(SyncState.READY)

    private lateinit var testee: SyncActivityViewModel

    @Before
    fun before() {
        testee = SyncActivityViewModel(
            syncAccountRepository = syncAccountRepository,
            dispatchers = coroutineTestRule.testDispatcherProvider,
            syncStateMonitor = syncStateMonitor,
            syncEngine = syncEngine,
            recoveryCodePDF = recoveryPDF,
            syncFeatureToggle = syncFeatureToggle,
            syncPixels = syncPixels,
            deviceAuthenticator = deviceAuthenticator,
        )
        whenever(deviceAuthenticator.isAuthenticationRequired()).thenReturn(true)
        whenever(syncStateMonitor.syncState()).thenReturn(emptyFlow())
        whenever(syncAccountRepository.isSyncSupported()).thenReturn(true)
    }

    @Test
    fun `givenAuthenticatedUser - device sync view state enabled`() = runTest {
        givenAuthenticatedUser()

        testee.viewState().test {
            val viewState = expectMostRecentItem()
            assertTrue(viewState.showAccount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `whenUserSignedIn - show account`() = runTest {
        givenAuthenticatedUser()

        testee.viewState().test {
            val viewState = expectMostRecentItem()
            assertTrue(viewState.showAccount)

            verify(syncEngine).triggerSync(FEATURE_READ)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `givenAuthenticatedUser - login QR code is not null`() = runTest {
        givenAuthenticatedUser()

        testee.viewState().test {
            val viewState = expectMostRecentItem()
            assertTrue(viewState.showAccount)

            verify(syncEngine).triggerSync(FEATURE_READ)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `whenUserHasMultipleConnectedDevices - view state updated`() = runTest {
        givenAuthenticatedUser()

        val connectedDevices = listOf(connectedDevice, connectedDevice)
        whenever(syncAccountRepository.getConnectedDevices()).thenReturn(Result.Success(connectedDevices))

        testee.viewState().test {
            val initialState = expectMostRecentItem()
            assertEquals(connectedDevices.size, initialState.syncedDevices.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSyncWithAnotherDevice - emit command sync with another device`() = runTest {
        givenUserHasDeviceAuthentication(true)
        testee.onSyncWithAnotherDevice()

        testee.commands().test {
            awaitItem().assertCommandType(Command.SyncWithAnotherDevice::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSyncWithAnotherDevice - without device authentication - emit command request setup authentication`() = runTest {
        givenUserHasDeviceAuthentication(false)
        testee.onSyncWithAnotherDevice()

        testee.commands().test {
            awaitItem().assertCommandType(Command.RequestSetupAuthentication::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAddAnotherDevice - emit command add another device`() = runTest {
        givenUserHasDeviceAuthentication(true)
        testee.onAddAnotherDevice()

        testee.commands().test {
            awaitItem().assertCommandType(Command.AddAnotherDevice::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAddAnotherDevice - no device authentication - emit command request setup authentication`() = runTest {
        givenUserHasDeviceAuthentication(false)
        testee.onAddAnotherDevice()

        testee.commands().test {
            awaitItem().assertCommandType(Command.RequestSetupAuthentication::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSyncThisDevice - launch create account flow`() = runTest {
        givenUserHasDeviceAuthentication(true)
        testee.commands().test {
            testee.onSyncThisDevice()
            awaitItem().assertCommandType(IntroCreateAccount::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSyncThisDevice - no device authentication - emit command request setup authentication`() = runTest {
        givenUserHasDeviceAuthentication(false)
        testee.commands().test {
            testee.onSyncThisDevice()
            awaitItem().assertCommandType(RequestSetupAuthentication::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRecoverYourSyncedData - recover data command sent`() = runTest {
        givenUserHasDeviceAuthentication(true)
        testee.onRecoverYourSyncedData()

        testee.commands().test {
            awaitItem().assertCommandType(IntroRecoverSyncData::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRecoverYourSyncedData - recover data without device authentication - emit command request setup authentication`() = runTest {
        givenUserHasDeviceAuthentication(false)
        testee.onRecoverYourSyncedData()

        testee.commands().test {
            awaitItem().assertCommandType(RequestSetupAuthentication::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onTurnOffClicked - ask turn off command shown`() = runTest {
        givenAuthenticatedUser()

        testee.onTurnOffClicked()

        testee.commands().test {
            awaitItem().assertCommandType(AskTurnOffSync::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onTurnOffSyncConfirmed - logout local device`() = runTest {
        whenever(syncAccountRepository.getThisConnectedDevice()).thenReturn(connectedDevice)
        whenever(syncAccountRepository.logout(deviceId)).thenReturn(Result.Success(true))

        testee.onTurnOffSyncConfirmed(connectedDevice)

        verify(syncAccountRepository).logout(deviceId)
    }

    @Test
    fun `logout - view state updated`() = runTest {
        givenAuthenticatedUser()

        whenever(syncAccountRepository.logout(deviceId)).thenReturn(Result.Success(true))

        testee.viewState().test {
            var viewState = expectMostRecentItem()
            assertTrue(viewState.showAccount)
            testee.onTurnOffSyncConfirmed(connectedDevice)
            viewState = awaitItem()
            assertFalse(viewState.showAccount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onTurnOffSyncConfirmed - view state updated`() = runTest {
        givenAuthenticatedUser()

        whenever(syncAccountRepository.logout(deviceId)).thenReturn(Result.Error(reason = "error"))

        testee.onTurnOffSyncConfirmed(connectedDevice)

        testee.viewState().test {
            val viewState = awaitItem()
            assertTrue(viewState.showAccount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onTurnOffSyncCancelled - device sync view state enabled`() = runTest {
        givenAuthenticatedUser()

        testee.viewState().test {
            testee.onTurnOffSyncCancelled()
            val viewState = expectMostRecentItem()
            assertTrue(viewState.showAccount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeleteAccountClicked - ask delete account`() = runTest {
        testee.onDeleteAccountClicked()

        testee.commands().test {
            awaitItem().assertCommandType(Command.AskDeleteAccount::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeleteAccountSuccess - update view state`() = runTest {
        givenAuthenticatedUser()

        whenever(syncAccountRepository.deleteAccount()).thenReturn(Result.Success(true))

        testee.viewState().test {
            var viewState = expectMostRecentItem()
            assertTrue(viewState.showAccount)
            testee.onDeleteAccountConfirmed()
            viewState = awaitItem()
            assertFalse(viewState.showAccount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeleteAccount - error - update view state`() = runTest {
        givenAuthenticatedUser()

        whenever(syncAccountRepository.deleteAccount()).thenReturn(Result.Error(reason = "error"))

        testee.onDeleteAccountConfirmed()

        testee.viewState().test {
            val viewState = awaitItem()
            assertTrue(viewState.showAccount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeleteAccountConfirmed - delete account confirmed - delete account`() = runTest {
        whenever(syncAccountRepository.getThisConnectedDevice()).thenReturn(connectedDevice)
        whenever(syncAccountRepository.logout(deviceId)).thenReturn(Result.Success(true))

        testee.onDeleteAccountConfirmed()

        verify(syncAccountRepository).deleteAccount()
    }

    @Test
    fun `onDeleteAccountCancelled - device sync view state is enabled`() = runTest {
        givenAuthenticatedUser()

        testee.viewState().test {
            testee.onDeleteAccountCancelled()
            val viewState = expectMostRecentItem()
            assertTrue(viewState.showAccount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRemoveDeviceClicked - ask remove device`() = runTest {
        testee.onRemoveDeviceClicked(connectedDevice)

        testee.commands().test {
            awaitItem().assertCommandType(Command.AskRemoveDevice::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRemoveDeviceConfirmed - remove device`() = runTest {
        givenAuthenticatedUser()

        whenever(syncAccountRepository.logout(deviceId)).thenReturn(Result.Success(true))

        testee.onRemoveDeviceConfirmed(connectedDevice)

        verify(syncAccountRepository).logout(deviceId)
    }

    @Test
    fun `onRemoveDevice - fetch remote devices`() = runTest {
        givenAuthenticatedUser()

        whenever(syncAccountRepository.logout(deviceId)).thenReturn(Result.Success(true))

        testee.onRemoveDeviceConfirmed(connectedDevice)

        verify(syncAccountRepository).getConnectedDevices()
    }

    @Test
    fun `onRemoveDevice - viewState updated`() = runTest {
        givenAuthenticatedUser()

        whenever(syncAccountRepository.logout(deviceId)).thenReturn(Result.Success(true))

        testee.viewState().test {
            var awaitItem = expectMostRecentItem()
            assertEquals(1, awaitItem.syncedDevices.size)
            whenever(syncAccountRepository.getConnectedDevices()).thenReturn(Result.Success(listOf()))
            testee.onRemoveDeviceConfirmed(connectedDevice)
            awaitItem = awaitItem()
            assertEquals(0, awaitItem.syncedDevices.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRemoveDevice - fails - restore previous list`() = runTest {
        givenAuthenticatedUser()

        whenever(syncAccountRepository.logout(deviceId)).thenReturn(Result.Error(reason = "error"))

        testee.viewState().test {
            testee.onRemoveDeviceConfirmed(connectedDevice)
            val awaitItem = expectMostRecentItem()
            assertEquals(1, awaitItem.syncedDevices.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeviceEdited - viewState updated`() = runTest {
        givenAuthenticatedUser()

        whenever(syncAccountRepository.renameDevice(any())).thenReturn(Result.Success(true))

        testee.viewState().test {
            var awaitItem = expectMostRecentItem()
            val newDevice = connectedDevice.copy(deviceName = "newDevice")
            whenever(syncAccountRepository.getConnectedDevices()).thenReturn(Result.Success(listOf(newDevice)))
            testee.onDeviceEdited(newDevice)
            awaitItem = awaitItem()
            assertNotNull(awaitItem.syncedDevices.filterIsInstance<SyncedDevice>().first { it.device.deviceName == newDevice.deviceName })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSaveRecoveryCodeClicked - emit check if user has permission command`() = runTest {
        givenUserHasDeviceAuthentication(true)
        whenever(syncAccountRepository.getRecoveryCode()).thenReturn(Result.Success(jsonRecoveryKeyEncoded))
        testee.commands().test {
            testee.onSaveRecoveryCodeClicked()
            val command = awaitItem()
            assertTrue(command is CheckIfUserHasStoragePermission)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSaveRecoveryCodeClicked - no device authentication - emit command request setup authentication`() = runTest {
        givenUserHasDeviceAuthentication(false)
        whenever(syncAccountRepository.getRecoveryCode()).thenReturn(Result.Success(jsonRecoveryKeyEncoded))
        testee.commands().test {
            testee.onSaveRecoveryCodeClicked()
            val command = awaitItem()
            assertTrue(command is RequestSetupAuthentication)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `generateRecoveryCode - generate file and emit success command`() = runTest {
        whenever(syncAccountRepository.getRecoveryCode()).thenReturn(Result.Success(jsonRecoveryKeyEncoded))
        whenever(recoveryPDF.generateAndStoreRecoveryCodePDF(any(), eq(jsonRecoveryKeyEncoded))).thenReturn(TestSyncFixtures.pdfFile())

        testee.commands().test {
            testee.generateRecoveryCode(mock())
            val command = awaitItem()
            assertTrue(command is RecoveryCodePDFSuccess)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeviceConnected - view state updated`() = runTest {
        givenAuthenticatedUser()

        val connectedDevices = listOf(connectedDevice, connectedDevice)
        whenever(syncAccountRepository.getConnectedDevices()).thenReturn(Result.Success(connectedDevices))

        testee.viewState().test {
            testee.onDeviceConnected()

            val initialState = expectMostRecentItem()
            assertEquals(connectedDevices.size, initialState.syncedDevices.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `syncStateIsDisabled - viewState changes`() = runTest {
        givenAuthenticatedUser()

        testee.viewState().test {
            val initialState = expectMostRecentItem()
            assertEquals(initialState.showAccount, true)
            stateFlow.value = OFF
            val updatedState = awaitItem()
            assertEquals(updatedState.showAccount, false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `syncStateEnabled - viewState changes`() = runTest {
        givenAuthenticatedUser()
        stateFlow.value = SyncState.OFF

        testee.viewState().test {
            val initialState = expectMostRecentItem()
            assertEquals(initialState.showAccount, false)
            stateFlow.value = READY
            val updatedState = awaitItem()
            assertEquals(updatedState.showAccount, true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `syncStateIsInProgressEnabled - viewState does not change`() = runTest {
        givenAuthenticatedUser()

        testee.viewState().test {
            val initialState = expectMostRecentItem()
            assertEquals(initialState.showAccount, true)
            stateFlow.value = IN_PROGRESS
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `whenUserSignedAndSetupFlowsDisabled - view state - all setup flows disabled`() = runTest {
        whenever(syncFeatureToggle.allowSetupFlows()).thenReturn(false)
        whenever(syncFeatureToggle.allowCreateAccount()).thenReturn(false)
        givenAuthenticatedUser()

        testee.viewState().test {
            val viewState = expectMostRecentItem()
            assertTrue(viewState.disabledSetupFlows.contains(SignInFlow))
            assertTrue(viewState.disabledSetupFlows.contains(CreateAccountFlow))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `whenUserSignedAndCreateAccountDisabled - view state - only sign in flow disabled`() = runTest {
        whenever(syncFeatureToggle.allowSetupFlows()).thenReturn(true)
        whenever(syncFeatureToggle.allowCreateAccount()).thenReturn(false)
        givenAuthenticatedUser()

        testee.viewState().test {
            val viewState = expectMostRecentItem()
            assertFalse(viewState.disabledSetupFlows.contains(SignInFlow))
            assertTrue(viewState.disabledSetupFlows.contains(CreateAccountFlow))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `whenSetupFlowsDisabled - all setup flows disabled view state`() = runTest {
        whenever(syncFeatureToggle.allowSetupFlows()).thenReturn(false)
        whenever(syncFeatureToggle.allowCreateAccount()).thenReturn(true)

        testee.viewState().test {
            val viewState = expectMostRecentItem()
            assertTrue(viewState.disabledSetupFlows.contains(SignInFlow))
            assertTrue(viewState.disabledSetupFlows.contains(CreateAccountFlow))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `whenCreateAccountDisabledThenOnlySignInFlowDisabled - view state`() = runTest {
        whenever(syncFeatureToggle.allowSetupFlows()).thenReturn(true)
        whenever(syncFeatureToggle.allowCreateAccount()).thenReturn(false)

        testee.viewState().test {
            val viewState = expectMostRecentItem()
            assertFalse(viewState.disabledSetupFlows.contains(SignInFlow))
            assertTrue(viewState.disabledSetupFlows.contains(CreateAccountFlow))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isSyncSupported - emit command show device unsupported`() = runTest {
        whenever(syncAccountRepository.isSyncSupported()).thenReturn(false)

        testee.commands().test {
            awaitItem().assertCommandType(Command.ShowDeviceUnsupported::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun Command.assertCommandType(expectedType: KClass<out Command>) {
        assertTrue(format("Unexpected command type: %s", this::class.simpleName), this::class == expectedType)
    }

    private fun givenAuthenticatedUser() {
        whenever(syncAccountRepository.isSignedIn()).thenReturn(true)
        whenever(syncStateMonitor.syncState()).thenReturn(stateFlow.asStateFlow())
        whenever(syncAccountRepository.getRecoveryCode()).thenReturn(Result.Success(jsonRecoveryKeyEncoded))
        whenever(syncAccountRepository.getThisConnectedDevice()).thenReturn(connectedDevice)
        whenever(syncAccountRepository.getConnectedDevices()).thenReturn(Success(listOf(connectedDevice)))
    }

    private fun givenUserHasDeviceAuthentication(hasDeviceAuthentication: Boolean) {
        whenever(deviceAuthenticator.hasValidDeviceAuthentication()).thenReturn(hasDeviceAuthentication)
    }
}
