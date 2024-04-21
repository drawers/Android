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

package com.duckduckgo.autofill.impl.securestorage

import app.cash.turbine.test
import com.duckduckgo.autofill.impl.securestorage.encryption.EncryptionHelper.EncryptedString
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.securestorage.store.SecureStorageRepository
import com.duckduckgo.securestorage.store.SecureStorageRepository.Factory
import com.duckduckgo.securestorage.store.db.WebsiteLoginCredentialsEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class RealSecureStorageTest {
    private lateinit var testee: RealSecureStorage

    @get:Rule
    @Suppress("unused")
    val coroutineRule = CoroutineTestRule()

    @Mock
    private lateinit var secureStorageRepository: SecureStorageRepository

    @Mock
    private lateinit var l2DataTransformer: L2DataTransformer
    private val testCredentials = WebsiteLoginDetailsWithCredentials(
        details = WebsiteLoginDetails(
            domain = "test.com",
            username = "user@test.com",
            id = 1,
            domainTitle = "test",
            lastUpdatedMillis = 1000L,
        ),
        password = expectedDecryptedData,
        notes = expectedDecryptedData,
    )

    private val testEntity = WebsiteLoginCredentialsEntity(
        id = 1,
        domain = "test.com",
        username = "user@test.com",
        password = expectedEncryptedData,
        passwordIv = expectedEncryptedIv,
        notes = expectedEncryptedData,
        notesIv = expectedEncryptedIv,
        domainTitle = "test",
        lastUpdatedInMillis = 1000L,
        lastUsedInMillis = null,
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(l2DataTransformer.decrypt(any(), any())).thenReturn(expectedDecryptedData)
        whenever(l2DataTransformer.encrypt(any())).thenReturn(
            EncryptedString(expectedEncryptedData, expectedEncryptedIv),
        )
        testee = RealSecureStorage(
            object : Factory {
                override fun get(): SecureStorageRepository = secureStorageRepository
            },
            coroutineRule.testDispatcherProvider,
            l2DataTransformer,
        )
    }

    @Test
    fun `addWebsiteLoginDetailsWithCredentials - add entity to repository with encrypted password and IV`() = runTest {
        testee.addWebsiteLoginDetailsWithCredentials(testCredentials)

        verify(secureStorageRepository).addWebsiteLoginCredential(testEntity)
    }

    @Test
    fun `canAccessSecureStorage - return can process data value`() {
        whenever(l2DataTransformer.canProcessData()).thenReturn(true)

        assertTrue(testee.canAccessSecureStorage())
    }

    @Test
    fun `deleteWebsiteLoginDetailsWithCredentials - deletes entity with Id from secure storage repository`() = runTest {
        testee.deleteWebsiteLoginDetailsWithCredentials(1)

        verify(secureStorageRepository).deleteWebsiteLoginCredentials(1)
    }

    @Test
    fun `updateWebsiteLoginDetailsWithCredentials - update entity in secure storage repository`() = runTest {
        testee.updateWebsiteLoginDetailsWithCredentials(testCredentials)

        verify(secureStorageRepository).updateWebsiteLoginCredentials(testEntity)
    }

    @Test
    fun `getWebsiteLoginDetailsWithCredentials - get entity with Id from secure storage repository`() = runTest {
        whenever(secureStorageRepository.getWebsiteLoginCredentialsForId(1)).thenReturn(testEntity)

        val result = testee.getWebsiteLoginDetailsWithCredentials(1)

        assertEquals(testCredentials, result)
    }

    @Test
    fun `websiteLoginDetails - all details requested - get all entities and return details only`() = runTest {
        whenever(secureStorageRepository.websiteLoginCredentials()).thenReturn(
            MutableStateFlow(listOf(testEntity)),
        )

        val result = testee.websiteLoginDetails().first()

        assertEquals(listOf(testCredentials.details), result)
    }

    @Test
    fun `websiteLoginDetailsForDomain - requested - get entity for domain and return details only`() = runTest {
        whenever(secureStorageRepository.websiteLoginCredentialsForDomain("test.com")).thenReturn(
            MutableStateFlow(listOf(testEntity)),
        )

        val result = testee.websiteLoginDetailsForDomain("test.com").first()

        assertEquals(listOf(testCredentials.details), result)
    }

    @Test
    fun `websiteLoginDetailsWithCredentials - get all entities and return including decrypted password`() = runTest {
        whenever(secureStorageRepository.websiteLoginCredentials()).thenReturn(
            MutableStateFlow(listOf(testEntity)),
        )

        val result = testee.websiteLoginDetailsWithCredentials().first()

        assertEquals(listOf(testCredentials), result)
    }

    @Test
    fun `websiteLoginDetailsWithCredentialsForDomain - get entity for domain and return including decrypted password`() = runTest {
        whenever(secureStorageRepository.websiteLoginCredentialsForDomain("test.com")).thenReturn(
            MutableStateFlow(listOf(testEntity)),
        )

        val result = testee.websiteLoginDetailsWithCredentialsForDomain("test.com").first()

        assertEquals(listOf(testCredentials), result)
    }

    @Test
    fun `setUpNoSecureStorageRepository - can access secure storage false`() {
        setUpNoSecureStorageRepository()

        assertFalse(testee.canAccessSecureStorage())
    }

    @Test
    fun `addWebsiteLoginDetailsWithCredentials - no secure storage repository - do nothing`() = runTest {
        setUpNoSecureStorageRepository()

        testee.addWebsiteLoginDetailsWithCredentials(testCredentials)

        verifyNoInteractions(secureStorageRepository)
    }

    @Test
    fun `getWebsiteLoginDetailsWithCredentials - no secure storage repository - null`() = runTest {
        setUpNoSecureStorageRepository()

        assertNull(testee.getWebsiteLoginDetailsWithCredentials(1))
    }

    @Test
    fun `updateWebsiteLoginDetailsWithCredentials - no secure storage repository - do nothing`() = runTest {
        setUpNoSecureStorageRepository()

        testee.updateWebsiteLoginDetailsWithCredentials(testCredentials)

        verifyNoInteractions(secureStorageRepository)
    }

    @Test
    fun `deleteWebsiteLoginDetailsWithCredentials - no secure storage repository - do nothing`() = runTest {
        setUpNoSecureStorageRepository()

        testee.deleteWebsiteLoginDetailsWithCredentials(1)

        verifyNoInteractions(secureStorageRepository)
    }

    @Test
    fun `websiteLoginDetailsForDomain - no secure storage repository - flow returns nothing`() = runTest {
        setUpNoSecureStorageRepository()

        testee.websiteLoginDetailsForDomain("test").test {
            this.awaitComplete()
        }
    }

    @Test
    fun `getWebsiteLoginDetails - no secure storage repository - flow returns nothing`() = runTest {
        setUpNoSecureStorageRepository()

        testee.websiteLoginDetails().test {
            this.awaitComplete()
        }
    }

    @Test
    fun `getWebsiteCredentialsForDomain - no secure storage repository - flow returns empty list`() = runTest {
        setUpNoSecureStorageRepository()

        testee.websiteLoginDetailsWithCredentialsForDomain("test").test {
            assertTrue(this.awaitItem().isEmpty())
            this.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getWebsiteCredentials - no secure storage repository - empty list`() = runTest {
        setUpNoSecureStorageRepository()

        testee.websiteLoginDetailsWithCredentials().test {
            assertTrue(this.awaitItem().isEmpty())
            this.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteWebSiteLoginDetailsWithCredentials - bulk deletion function on secure storage repo - used`() = runTest {
        val idsToDelete = listOf(1L, 2L, 3L)
        testee.deleteWebSiteLoginDetailsWithCredentials(idsToDelete)
        verify(secureStorageRepository).deleteWebsiteLoginCredentials(idsToDelete)
    }

    private fun setUpNoSecureStorageRepository() {
        testee = RealSecureStorage(
            object : Factory {
                override fun get(): SecureStorageRepository? = null
            },
            coroutineRule.testDispatcherProvider,
            l2DataTransformer,
        )
    }

    companion object {
        private const val expectedEncryptedData: String = "ZXhwZWN0ZWRFbmNyeXB0ZWREYXRh"
        private const val expectedEncryptedIv: String = "ZXhwZWN0ZWRFbmNyeXB0ZWRJVg=="
        private const val expectedDecryptedData: String = "ZXhwZWN0ZWREZWNyeXB0ZWREYXRh"
    }
}
