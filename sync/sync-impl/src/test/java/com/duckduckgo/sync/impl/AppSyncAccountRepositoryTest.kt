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

package com.duckduckgo.sync.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.common.utils.DefaultDispatcherProvider
import com.duckduckgo.sync.TestSyncFixtures.accountCreatedFailDupUser
import com.duckduckgo.sync.TestSyncFixtures.accountCreatedSuccess
import com.duckduckgo.sync.TestSyncFixtures.accountKeys
import com.duckduckgo.sync.TestSyncFixtures.accountKeysFailed
import com.duckduckgo.sync.TestSyncFixtures.connectDeviceKeysGoneError
import com.duckduckgo.sync.TestSyncFixtures.connectDeviceKeysNotFoundError
import com.duckduckgo.sync.TestSyncFixtures.connectDeviceSuccess
import com.duckduckgo.sync.TestSyncFixtures.connectKeys
import com.duckduckgo.sync.TestSyncFixtures.connectedDevice
import com.duckduckgo.sync.TestSyncFixtures.decryptedSecretKey
import com.duckduckgo.sync.TestSyncFixtures.deleteAccountSuccess
import com.duckduckgo.sync.TestSyncFixtures.deviceFactor
import com.duckduckgo.sync.TestSyncFixtures.deviceId
import com.duckduckgo.sync.TestSyncFixtures.deviceName
import com.duckduckgo.sync.TestSyncFixtures.deviceType
import com.duckduckgo.sync.TestSyncFixtures.encryptedRecoveryCode
import com.duckduckgo.sync.TestSyncFixtures.failedLoginKeys
import com.duckduckgo.sync.TestSyncFixtures.getDevicesError
import com.duckduckgo.sync.TestSyncFixtures.getDevicesSuccess
import com.duckduckgo.sync.TestSyncFixtures.hashedPassword
import com.duckduckgo.sync.TestSyncFixtures.invalidDecryptedSecretKey
import com.duckduckgo.sync.TestSyncFixtures.jsonConnectKeyEncoded
import com.duckduckgo.sync.TestSyncFixtures.jsonRecoveryKey
import com.duckduckgo.sync.TestSyncFixtures.jsonRecoveryKeyEncoded
import com.duckduckgo.sync.TestSyncFixtures.listOfConnectedDevices
import com.duckduckgo.sync.TestSyncFixtures.loginFailed
import com.duckduckgo.sync.TestSyncFixtures.loginSuccess
import com.duckduckgo.sync.TestSyncFixtures.logoutSuccess
import com.duckduckgo.sync.TestSyncFixtures.primaryKey
import com.duckduckgo.sync.TestSyncFixtures.protectedEncryptionKey
import com.duckduckgo.sync.TestSyncFixtures.secretKey
import com.duckduckgo.sync.TestSyncFixtures.stretchedPrimaryKey
import com.duckduckgo.sync.TestSyncFixtures.token
import com.duckduckgo.sync.TestSyncFixtures.userId
import com.duckduckgo.sync.TestSyncFixtures.validLoginKeys
import com.duckduckgo.sync.api.engine.SyncEngine
import com.duckduckgo.sync.crypto.DecryptResult
import com.duckduckgo.sync.crypto.EncryptResult
import com.duckduckgo.sync.crypto.SyncLib
import com.duckduckgo.sync.impl.AccountErrorCodes.ALREADY_SIGNED_IN
import com.duckduckgo.sync.impl.AccountErrorCodes.CONNECT_FAILED
import com.duckduckgo.sync.impl.AccountErrorCodes.CREATE_ACCOUNT_FAILED
import com.duckduckgo.sync.impl.AccountErrorCodes.GENERIC_ERROR
import com.duckduckgo.sync.impl.AccountErrorCodes.INVALID_CODE
import com.duckduckgo.sync.impl.AccountErrorCodes.LOGIN_FAILED
import com.duckduckgo.sync.impl.Result.Error
import com.duckduckgo.sync.impl.Result.Success
import com.duckduckgo.sync.impl.pixels.*
import com.duckduckgo.sync.store.SyncStore
import java.lang.RuntimeException
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppSyncAccountRepositoryTest {

    private var nativeLib: SyncLib = mock()
    private var syncDeviceIds: SyncDeviceIds = mock()
    private var syncApi: SyncApi = mock()
    private var syncStore: SyncStore = mock()
    private var syncEngine: SyncEngine = mock()
    private var syncPixels: SyncPixels = mock()

    private lateinit var syncRepo: SyncAccountRepository

    @Before
    fun before() {
        syncRepo =
            AppSyncAccountRepository(syncDeviceIds, nativeLib, syncApi, syncStore, syncEngine, syncPixels, TestScope(), DefaultDispatcherProvider())
    }

    @Test
    fun `createAccount - store credentials persisted`() {
        prepareToProvideDeviceIds()
        prepareForCreateAccountSuccess()

        val result = syncRepo.createAccount()

        assertEquals(Result.Success(true), result)
        verify(syncStore).storeCredentials(
            userId = userId,
            deviceId = deviceId,
            deviceName = deviceName,
            primaryKey = primaryKey,
            secretKey = secretKey,
            token = token,
        )
    }

    @Test
    fun `createAccount - user signed in - already signed in error`() {
        whenever(syncStore.isSignedIn()).thenReturn(true)

        val result = syncRepo.createAccount() as Error

        assertEquals(ALREADY_SIGNED_IN.code, result.code)
    }

    @Test
    fun `createAccount - fails - return create account error`() {
        prepareToProvideDeviceIds()
        prepareForEncryption()
        whenever(nativeLib.generateAccountKeys(userId = anyString(), password = anyString())).thenReturn(accountKeys)
        whenever(syncApi.createAccount(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(accountCreatedFailDupUser)

        val result = syncRepo.createAccount() as Error

        assertEquals(CREATE_ACCOUNT_FAILED.code, result.code)
    }

    @Test
    fun `createAccount - generate keys fails - return create account error`() {
        prepareToProvideDeviceIds()
        whenever(nativeLib.generateAccountKeys(userId = anyString(), password = anyString())).thenReturn(accountKeysFailed)
        whenever(syncApi.createAccount(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(accountCreatedSuccess)

        val result = syncRepo.createAccount() as Error

        assertEquals(CREATE_ACCOUNT_FAILED.code, result.code)
        verifyNoInteractions(syncApi)
    }

    @Test
    fun `getAccountInfo - account exists - returns data`() {
        givenAuthenticatedDevice()

        val result = syncRepo.getAccountInfo()

        assertEquals(userId, result.userId)
        assertEquals(deviceId, result.deviceId)
        assertEquals(deviceName, result.deviceName)
        assertTrue(result.isSignedIn)
    }

    @Test
    fun `getAccountInfo - account not created - empty`() {
        whenever(syncStore.primaryKey).thenReturn("")

        val result = syncRepo.getAccountInfo()

        assertEquals("", result.userId)
        assertEquals("", result.deviceId)
        assertEquals("", result.deviceName)
        assertFalse(result.isSignedIn)
    }

    @Test
    fun `logout - succeeds - return success and remove data`() {
        givenAuthenticatedDevice()
        whenever(syncApi.logout(token, deviceId)).thenReturn(logoutSuccess)

        val result = syncRepo.logout(deviceId)

        assertTrue(result is Result.Success)
        verify(syncStore).clearAll()
    }

    @Test
    fun `logout - succeeds - return success but do not remove local data`() {
        whenever(syncStore.deviceId).thenReturn(deviceId)
        whenever(syncStore.token).thenReturn(token)
        whenever(syncApi.logout(eq(token), anyString())).thenReturn(logoutSuccess)

        val result = syncRepo.logout("randomDeviceId")

        assertTrue(result is Success)
        verify(syncStore, times(0)).clearAll()
    }

    @Test
    fun `deleteAccount - succeeds - return success and remove data`() {
        givenAuthenticatedDevice()
        whenever(syncApi.deleteAccount(token)).thenReturn(deleteAccountSuccess)

        val result = syncRepo.deleteAccount()

        assertTrue(result is Result.Success)
        verify(syncStore).clearAll()
    }

    @Test
    fun `processJsonRecoveryCode - account persisted`() {
        prepareForLoginSuccess()

        val result = syncRepo.processCode(jsonRecoveryKeyEncoded)

        assertEquals(Result.Success(true), result)
        verify(syncStore).storeCredentials(
            userId = userId,
            deviceId = deviceId,
            deviceName = deviceName,
            primaryKey = primaryKey,
            secretKey = secretKey,
            token = token,
        )
    }

    @Test
    fun `generateKeysFromRecoveryCode - fails - return login failed error`() {
        prepareToProvideDeviceIds()
        whenever(nativeLib.prepareForLogin(primaryKey = primaryKey)).thenReturn(failedLoginKeys)

        val result = syncRepo.processCode(jsonRecoveryKeyEncoded) as Error

        assertEquals(LOGIN_FAILED.code, result.code)
    }

    @Test
    fun `processCode - login fails - return login failed error`() {
        prepareToProvideDeviceIds()
        prepareForEncryption()
        whenever(nativeLib.prepareForLogin(primaryKey = primaryKey)).thenReturn(validLoginKeys)
        whenever(syncApi.login(userId, hashedPassword, deviceId, deviceName, deviceFactor)).thenReturn(loginFailed)

        val result = syncRepo.processCode(jsonRecoveryKeyEncoded) as Error

        assertEquals(LOGIN_FAILED.code, result.code)
    }

    @Test
    fun `processRecoveryKey - decryption fails - login failed error`() {
        prepareToProvideDeviceIds()
        prepareForEncryption()
        whenever(nativeLib.prepareForLogin(primaryKey = primaryKey)).thenReturn(validLoginKeys)
        whenever(nativeLib.decrypt(encryptedData = protectedEncryptionKey, secretKey = stretchedPrimaryKey)).thenReturn(invalidDecryptedSecretKey)
        whenever(syncApi.login(userId, hashedPassword, deviceId, deviceName, deviceFactor)).thenReturn(loginSuccess)

        val result = syncRepo.processCode(jsonRecoveryKeyEncoded) as Error

        assertEquals(LOGIN_FAILED.code, result.code)
    }

    @Test
    fun `processCode - invalid code - invalid code error`() {
        val result = syncRepo.processCode("invalidCode") as Error

        assertEquals(INVALID_CODE.code, result.code)
    }

    @Test
    fun `getConnectedDevices - succeeds - returns success`() {
        whenever(syncStore.token).thenReturn(token)
        whenever(syncStore.primaryKey).thenReturn(primaryKey)
        whenever(syncStore.deviceId).thenReturn(deviceId)
        prepareForEncryption()
        whenever(syncApi.getDevices(anyString())).thenReturn(getDevicesSuccess)

        val result = syncRepo.getConnectedDevices() as Success

        assertEquals(listOfConnectedDevices, result.data)
    }

    @Test
    fun `getConnectedDevices - returns list with local device in first position`() {
        givenAuthenticatedDevice()
        prepareForEncryption()
        val thisDevice = Device(deviceId = deviceId, deviceName = deviceName, jwIat = "", deviceType = deviceFactor)
        val anotherDevice = Device(deviceId = "anotherDeviceId", deviceName = deviceName, jwIat = "", deviceType = deviceFactor)
        val anotherRemoteDevice = Device(deviceId = "anotherRemoteDeviceId", deviceName = deviceName, jwIat = "", deviceType = deviceFactor)
        whenever(syncApi.getDevices(anyString())).thenReturn(Result.Success(listOf(anotherDevice, anotherRemoteDevice, thisDevice)))

        val result = syncRepo.getConnectedDevices() as Success

        assertTrue(result.data.first().thisDevice)
    }

    @Test
    fun `getConnectedDevices - fails - return generic error`() {
        whenever(syncStore.token).thenReturn(token)
        whenever(syncStore.deviceId).thenReturn(deviceId)
        whenever(syncStore.primaryKey).thenReturn(primaryKey)
        whenever(syncApi.getDevices(anyString())).thenReturn(getDevicesError)

        val result = syncRepo.getConnectedDevices() as Error

        assertEquals(GENERIC_ERROR.code, result.code)
    }

    @Test
    fun `getConnectedDevices - decryption fails - logout device`() {
        givenAuthenticatedDevice()
        prepareForEncryption()
        val thisDevice = Device(deviceId = deviceId, deviceName = deviceName, jwIat = "", deviceType = deviceFactor)
        val otherDevice = Device(deviceId = "otherDeviceId", deviceName = "otherDeviceName", jwIat = "", deviceType = "otherDeviceType")
        whenever(syncStore.token).thenReturn(token)
        whenever(syncStore.deviceId).thenReturn(deviceId)
        whenever(syncStore.primaryKey).thenReturn(primaryKey)
        whenever(nativeLib.decryptData(anyString(), anyString())).thenThrow(NegativeArraySizeException())
        whenever(syncApi.getDevices(anyString())).thenReturn(Result.Success(listOf(thisDevice, otherDevice)))
        whenever(syncApi.logout("token", "otherDeviceId")).thenReturn(Result.Success(Logout("otherDeviceId")))

        val result = syncRepo.getConnectedDevices() as Success
        verify(syncApi).logout("token", "otherDeviceId")
        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `getRecoveryCode - when generate recovery code as string then return expected json`() {
        whenever(syncStore.primaryKey).thenReturn(primaryKey)
        whenever(syncStore.userId).thenReturn(userId)

        val result = syncRepo.getRecoveryCode() as Success

        assertEquals(jsonRecoveryKeyEncoded, result.data)
    }

    @Test
    fun `getRecoveryCode - no account - generic error`() {
        val result = syncRepo.getRecoveryCode() as Error

        assertEquals(GENERIC_ERROR.code, result.code)
    }

    @Test
    fun `getConnectQR - return expected json`() {
        whenever(nativeLib.prepareForConnect()).thenReturn(connectKeys)
        prepareToProvideDeviceIds()

        val result = syncRepo.getConnectQR() as Success

        assertEquals(jsonConnectKeyEncoded, result.data)
    }

    @Test
    fun `processConnectCodeFromAuthenticatedDevice - connects device`() {
        givenAuthenticatedDevice()
        whenever(nativeLib.seal(jsonRecoveryKey, primaryKey)).thenReturn(encryptedRecoveryCode)
        whenever(syncApi.connect(token, deviceId, encryptedRecoveryCode)).thenReturn(Result.Success(true))

        val result = syncRepo.processCode(jsonConnectKeyEncoded)

        verify(syncApi).connect(token, deviceId, encryptedRecoveryCode)
        assertTrue(result is Success)
    }

    @Test
    fun `processConnectCodeFromUnauthenticatedDevice - account created and connects`() {
        whenever(syncStore.primaryKey).thenReturn(primaryKey)
        whenever(syncStore.isSignedIn()).thenReturn(false).thenReturn(true)
        whenever(syncStore.userId).thenReturn(userId)
        whenever(syncStore.deviceId).thenReturn(deviceId)
        whenever(syncStore.token).thenReturn(token)
        prepareToProvideDeviceIds()
        prepareForCreateAccountSuccess()
        whenever(nativeLib.seal(jsonRecoveryKey, primaryKey)).thenReturn(encryptedRecoveryCode)
        whenever(syncApi.connect(token, deviceId, encryptedRecoveryCode)).thenReturn(Result.Success(true))

        val result = syncRepo.processCode(jsonConnectKeyEncoded)

        verify(syncApi).connect(token, deviceId, encryptedRecoveryCode)
        assertTrue(result is Success)
    }

    @Test
    fun `pollConnectionKeys - keys found - perform login`() {
        prepareForLoginSuccess()
        whenever(syncStore.userId).thenReturn(userId)
        whenever(syncStore.primaryKey).thenReturn(primaryKey)
        whenever(syncStore.secretKey).thenReturn(secretKey)
        whenever(syncApi.connectDevice(deviceId)).thenReturn(connectDeviceSuccess)
        whenever(nativeLib.sealOpen(encryptedRecoveryCode, primaryKey, secretKey)).thenReturn(jsonRecoveryKey)

        val result = syncRepo.pollConnectionKeys()

        assertTrue(result is Success)
    }

    @Test
    fun `pollConnectionKeys - login fails - return login error`() {
        whenever(syncDeviceIds.deviceId()).thenReturn(deviceId)
        whenever(syncStore.userId).thenReturn(userId)
        whenever(syncStore.primaryKey).thenReturn(primaryKey)
        whenever(syncStore.secretKey).thenReturn(secretKey)
        whenever(syncApi.connectDevice(deviceId)).thenReturn(connectDeviceSuccess)
        whenever(nativeLib.sealOpen(encryptedRecoveryCode, primaryKey, secretKey)).thenReturn(jsonRecoveryKey)
        whenever(syncApi.login(userId, hashedPassword, deviceId, deviceName, deviceFactor)).thenReturn(loginFailed)

        val result = syncRepo.pollConnectionKeys() as Error

        assertEquals(LOGIN_FAILED.code, result.code)
    }

    @Test
    fun `pollConnectionKeys - keys not found - return success false`() {
        whenever(syncDeviceIds.deviceId()).thenReturn(deviceId)
        whenever(syncStore.userId).thenReturn(userId)
        whenever(syncStore.primaryKey).thenReturn(primaryKey)
        whenever(syncStore.secretKey).thenReturn(secretKey)
        whenever(syncApi.connectDevice(deviceId)).thenReturn(connectDeviceKeysNotFoundError)

        val result = syncRepo.pollConnectionKeys() as Success

        assertFalse(result.data)
    }

    @Test
    fun `pollConnectionKeys - seal open fails - return connect error`() {
        whenever(syncDeviceIds.deviceId()).thenReturn(deviceId)
        whenever(syncStore.userId).thenReturn(userId)
        whenever(syncStore.primaryKey).thenReturn(primaryKey)
        whenever(syncStore.secretKey).thenReturn(secretKey)
        whenever(syncApi.connectDevice(deviceId)).thenReturn(connectDeviceSuccess)
        whenever(nativeLib.sealOpen(encryptedRecoveryCode, primaryKey, secretKey)).thenThrow(RuntimeException())

        val result = syncRepo.pollConnectionKeys() as Error

        assertEquals(CONNECT_FAILED.code, result.code)
    }

    @Test
    fun `pollConnectionKeys - polling connection and keys expired - connect failed error`() {
        whenever(syncDeviceIds.deviceId()).thenReturn(deviceId)
        whenever(syncStore.userId).thenReturn(userId)
        whenever(syncStore.primaryKey).thenReturn(primaryKey)
        whenever(syncStore.secretKey).thenReturn(secretKey)
        whenever(syncApi.connectDevice(deviceId)).thenReturn(connectDeviceKeysGoneError)

        val result = syncRepo.pollConnectionKeys() as Error

        assertEquals(CONNECT_FAILED.code, result.code)
    }

    @Test
    fun `getThisConnectedDevice - when get this connected device then return expected device`() {
        givenAuthenticatedDevice()
        whenever(syncStore.deviceId).thenReturn(deviceId)
        whenever(syncStore.deviceName).thenReturn(deviceName)
        whenever(syncDeviceIds.deviceType()).thenReturn(deviceType)

        val result = syncRepo.getThisConnectedDevice()!!

        assertEquals(deviceId, result?.deviceId)
        assertEquals(deviceName, result?.deviceName)
        assertEquals(deviceType, result?.deviceType)
    }

    @Test
    fun `getThisConnectedDevice - not authenticated - null`() {
        val result = syncRepo.getThisConnectedDevice()

        assertNull(result)
    }

    @Test
    fun `renameDevice - unauthenticated - return error`() {
        val result = syncRepo.renameDevice(connectedDevice)

        assertTrue(result is Error)
    }

    @Test
    fun `renameDevice - success - return success`() {
        givenAuthenticatedDevice()
        prepareForLoginSuccess()

        val result = syncRepo.renameDevice(connectedDevice)

        verify(syncApi).login(anyString(), anyString(), eq(connectedDevice.deviceId), anyString(), anyString())
        assertTrue(result is Result.Success)
    }

    @Test
    fun `isSyncSupported - encryption not supported - not supported`() {
        whenever(syncStore.isEncryptionSupported()).thenReturn(false)

        val result = syncRepo.isSyncSupported()

        assertFalse(result)
    }

    private fun prepareForLoginSuccess() {
        prepareForEncryption()
        whenever(syncDeviceIds.deviceId()).thenReturn(deviceId)
        whenever(syncDeviceIds.deviceName()).thenReturn(deviceName)
        whenever(syncDeviceIds.deviceType()).thenReturn(deviceType)
        whenever(nativeLib.prepareForLogin(primaryKey = primaryKey)).thenReturn(validLoginKeys)
        whenever(syncApi.login(userId, hashedPassword, deviceId, deviceName, deviceFactor)).thenReturn(loginSuccess)
    }

    private fun givenAuthenticatedDevice() {
        whenever(syncStore.userId).thenReturn(userId)
        whenever(syncStore.deviceId).thenReturn(deviceId)
        whenever(syncStore.deviceName).thenReturn(deviceName)
        whenever(syncStore.primaryKey).thenReturn(primaryKey)
        whenever(syncStore.secretKey).thenReturn(secretKey)
        whenever(syncStore.token).thenReturn(token)
        whenever(syncStore.isSignedIn()).thenReturn(true)
    }

    private fun prepareToProvideDeviceIds() {
        whenever(syncDeviceIds.userId()).thenReturn(userId)
        whenever(syncDeviceIds.deviceId()).thenReturn(deviceId)
        whenever(syncDeviceIds.deviceName()).thenReturn(deviceName)
        whenever(syncDeviceIds.deviceType()).thenReturn(deviceType)
    }

    private fun prepareForCreateAccountSuccess() {
        prepareForEncryption()
        whenever(nativeLib.generateAccountKeys(userId = anyString(), password = anyString())).thenReturn(accountKeys)
        whenever(syncApi.createAccount(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Result.Success(AccountCreatedResponse(userId, token)))
    }

    private fun prepareForEncryption() {
        whenever(nativeLib.decrypt(encryptedData = protectedEncryptionKey, secretKey = stretchedPrimaryKey)).thenReturn(decryptedSecretKey)
        whenever(nativeLib.decryptData(anyString(), primaryKey = eq(primaryKey))).thenAnswer {
            DecryptResult(0, it.arguments.first() as String)
        }
        whenever(nativeLib.encryptData(anyString(), primaryKey = eq(primaryKey))).thenAnswer {
            EncryptResult(0, it.arguments.first() as String)
        }
    }
}
