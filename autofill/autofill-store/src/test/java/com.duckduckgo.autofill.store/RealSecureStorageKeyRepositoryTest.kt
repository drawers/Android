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

package com.duckduckgo.autofill.store

import com.duckduckgo.securestorage.store.RealSecureStorageKeyRepository
import com.duckduckgo.securestorage.store.keys.SecureStorageKeyStore
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RealSecureStorageKeyRepositoryTest {
    @Mock
    private lateinit var keyStore: SecureStorageKeyStore
    private lateinit var testee: RealSecureStorageKeyRepository
    private val testValue = "TestValue".toByteArray()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testee = RealSecureStorageKeyRepository(keyStore)
    }

    @Test
    fun `password - set - update key in key store`() {
        testee.password = testValue

        verify(keyStore).updateKey("KEY_GENERATED_PASSWORD", testValue)
    }

    @Test
    fun `password - get key for password in key store`() {
        whenever(keyStore.getKey("KEY_GENERATED_PASSWORD")).thenReturn(testValue)

        assertEquals(testValue, testee.password)
    }

    @Test
    fun `l1Key - set - update key in keyStore`() {
        testee.l1Key = testValue

        verify(keyStore).updateKey("KEY_L1KEY", testValue)
    }

    @Test
    fun `l1Key - get key for L1 key in key store`() {
        whenever(keyStore.getKey("KEY_L1KEY")).thenReturn(testValue)

        assertEquals(testValue, testee.l1Key)
    }

    @Test
    fun `passwordSalt - set - update key in keyStore`() {
        testee.passwordSalt = testValue

        verify(keyStore).updateKey("KEY_PASSWORD_SALT", testValue)
    }

    @Test
    fun `passwordSalt - get key for password salt key in key store`() {
        whenever(keyStore.getKey("KEY_PASSWORD_SALT")).thenReturn(testValue)

        assertEquals(testValue, testee.passwordSalt)
    }

    @Test
    fun `encryptedL2Key - set - update key in keyStore`() {
        testee.encryptedL2Key = testValue

        verify(keyStore).updateKey("KEY_ENCRYPTED_L2KEY", testValue)
    }

    @Test
    fun `encryptedL2Key - get key for encrypted L2 key in key store`() {
        whenever(keyStore.getKey("KEY_ENCRYPTED_L2KEY")).thenReturn(testValue)

        assertEquals(testValue, testee.encryptedL2Key)
    }

    @Test
    fun `encryptedL2KeyIV - update key in keyStore`() {
        testee.encryptedL2KeyIV = testValue

        verify(keyStore).updateKey("KEY_ENCRYPTED_L2KEY_IV", testValue)
    }

    @Test
    fun `encryptedL2KeyIV - get key from keystore`() {
        whenever(keyStore.getKey("KEY_ENCRYPTED_L2KEY_IV")).thenReturn(testValue)

        assertEquals(testValue, testee.encryptedL2KeyIV)
    }
}
