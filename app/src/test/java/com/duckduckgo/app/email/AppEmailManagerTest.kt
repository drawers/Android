/*
 * Copyright (c) 2020 DuckDuckGo
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

package com.duckduckgo.app.email

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.*
import com.duckduckgo.app.email.AppEmailManager.Companion.DUCK_EMAIL_DOMAIN
import com.duckduckgo.app.email.AppEmailManager.Companion.UNKNOWN_COHORT
import com.duckduckgo.app.email.api.EmailAlias
import com.duckduckgo.app.email.api.EmailService
import com.duckduckgo.app.email.db.EmailDataStore
import com.duckduckgo.app.email.sync.*
import com.duckduckgo.app.pixels.AppPixelName.EMAIL_DISABLED
import com.duckduckgo.app.pixels.AppPixelName.EMAIL_ENABLED
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.sync.settings.api.SyncSettingsListener
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@FlowPreview
@RunWith(AndroidJUnit4::class)
class AppEmailManagerTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mockEmailService: EmailService = mock()
    private val mockEmailDataStore: EmailDataStore = FakeEmailDataStore()
    private val mockSyncSettingsListener = mock<SyncSettingsListener>()
    private val emailSyncableSetting = EmailSync(mockEmailDataStore, mockSyncSettingsListener, mock())
    private val mockPixel: Pixel = mock()
    lateinit var testee: AppEmailManager

    @Before
    fun setup() {
        testee = AppEmailManager(
            mockEmailService,
            mockEmailDataStore,
            emailSyncableSetting,
            coroutineRule.testDispatcherProvider,
            TestScope(),
            mockPixel,
        )
    }

    @Test
    fun `getAlias - fetch alias from service - store alias adding duck domain`() = runTest {
        mockEmailDataStore.emailToken = "token"
        whenever(mockEmailService.newAlias(any())).thenReturn(EmailAlias("test"))
        testee.getAlias()

        assertEquals("test$DUCK_EMAIL_DOMAIN", mockEmailDataStore.nextAlias)
    }

    @Test
    fun `getAlias - token does not exist - do nothing`() = runTest {
        mockEmailDataStore.emailToken = null
        testee.getAlias()

        verify(mockEmailService, never()).newAlias(any())
    }

    @Test
    fun `getAlias - address is blank - store null`() = runTest {
        mockEmailDataStore.emailToken = "token"
        whenever(mockEmailService.newAlias(any())).thenReturn(EmailAlias(""))
        testee.getAlias()

        assertNull(mockEmailDataStore.nextAlias)
    }

    @Test
    fun `getAlias - next alias exists - returns next alias`() = runTest {
        givenNextAliasExists()

        assertEquals("alias", testee.getAlias())
    }

    @Test
    fun `getAlias - next alias does not exist - return null`() {
        assertNull(testee.getAlias())
    }

    @Test
    fun `getAlias - clear next alias`() {
        testee.getAlias()

        assertNull(mockEmailDataStore.nextAlias)
    }

    @Test
    fun `isSignedIn - token does not exist - return false`() {
        mockEmailDataStore.emailUsername = "username"
        mockEmailDataStore.nextAlias = "alias"

        assertFalse(testee.isSignedIn())
    }

    @Test
    fun `isSignedIn - username does not exist - return false`() {
        mockEmailDataStore.emailToken = "token"
        mockEmailDataStore.nextAlias = "alias"

        assertFalse(testee.isSignedIn())
    }

    @Test
    fun `isSignedIn - token and username exist - return true`() {
        mockEmailDataStore.emailToken = "token"
        mockEmailDataStore.emailUsername = "username"

        assertTrue(testee.isSignedIn())
    }

    @Test
    fun `storeCredentials - generate new alias`() = runTest {
        mockEmailDataStore.emailToken = "token"
        whenever(mockEmailService.newAlias(any())).thenReturn(EmailAlias(""))

        testee.storeCredentials("token", "username", "cohort")

        verify(mockEmailService).newAlias(any())
    }

    @Test
    fun `storeCredentials - notify syncable setting`() = runTest {
        mockEmailDataStore.emailToken = "token"
        whenever(mockEmailService.newAlias(any())).thenReturn(EmailAlias(""))

        testee.storeCredentials("token", "username", "cohort")

        verify(mockSyncSettingsListener).onSettingChanged(emailSyncableSetting.key)
    }

    @Test
    fun `storeCredentials - send pixel`() = runTest {
        mockEmailDataStore.emailToken = "token"
        whenever(mockEmailService.newAlias(any())).thenReturn(EmailAlias(""))

        testee.storeCredentials("token", "username", "cohort")

        verify(mockPixel).fire(EMAIL_ENABLED)
    }

    @Test
    fun `storeCredentials - credentials are stored in data store`() {
        testee.storeCredentials("token", "username", "cohort")

        assertEquals("username", mockEmailDataStore.emailUsername)
        assertEquals("token", mockEmailDataStore.emailToken)
        assertEquals("cohort", mockEmailDataStore.cohort)
    }

    @Test
    fun `storeCredentials - credentials correctly stored - isSignedInChannel sends true`() = runTest {
        testee.storeCredentials("token", "username", "cohort")

        assertTrue(testee.signedInFlow().first())
    }

    @Test
    fun `storeCredentials - credentials are blank - sends false`() = runTest {
        testee.storeCredentials("", "", "cohort")

        assertFalse(testee.signedInFlow().first())
    }

    @Test
    fun `signedOut - clear email data - alias is null`() {
        testee.signOut()

        assertNull(mockEmailDataStore.emailUsername)
        assertNull(mockEmailDataStore.emailToken)
        assertNull(mockEmailDataStore.nextAlias)

        assertNull(testee.getAlias())
    }

    @Test
    fun `signedOut - notify syncable setting`() {
        testee.signOut()

        verify(mockSyncSettingsListener).onSettingChanged(emailSyncableSetting.key)
    }

    @Test
    fun `signedOut - send pixel`() {
        testee.signOut()

        verify(mockPixel).fire(EMAIL_DISABLED)
    }

    @Test
    fun `signOut - isSignedInChannel sends false`() = runTest {
        testee.signOut()

        assertFalse(testee.signedInFlow().first())
    }

    @Test
    fun `getEmailAddress - duck email domain is appended`() {
        mockEmailDataStore.emailUsername = "username"

        assertEquals("username$DUCK_EMAIL_DOMAIN", testee.getEmailAddress())
    }

    @Test
    fun `getCohort - return cohort`() {
        mockEmailDataStore.cohort = "cohort"

        assertEquals("cohort", testee.getCohort())
    }

    @Test
    fun `getCohort - cohort is null - return unknown`() {
        mockEmailDataStore.cohort = null

        assertEquals(UNKNOWN_COHORT, testee.getCohort())
    }

    @Test
    fun `getCohort - cohort is empty - return unknown`() {
        mockEmailDataStore.cohort = ""

        assertEquals(UNKNOWN_COHORT, testee.getCohort())
    }

    @Test
    fun `isEmailFeatureSupported - encryption can be used - return true`() {
        (mockEmailDataStore as FakeEmailDataStore).canUseEncryption = true

        assertTrue(testee.isEmailFeatureSupported())
    }

    @Test
    fun `getLastUsedDate - null - return empty`() {
        assertEquals("", testee.getLastUsedDate())
    }

    @Test
    fun `getLastUsedDate - not null - return value from store`() {
        mockEmailDataStore.lastUsedDate = "2021-01-01"
        assertEquals("2021-01-01", testee.getLastUsedDate())
    }

    @Test
    fun `isEmailFeatureSupported - encryption cannot be used - return false`() {
        (mockEmailDataStore as FakeEmailDataStore).canUseEncryption = false

        assertFalse(testee.isEmailFeatureSupported())
    }

    @Test
    fun `getUserData - data received correctly`() {
        val expected = JSONObject().apply {
            put(AppEmailManager.TOKEN, "token")
            put(AppEmailManager.USERNAME, "user")
            put(AppEmailManager.NEXT_ALIAS, "nextAlias")
        }.toString()

        mockEmailDataStore.emailToken = "token"
        mockEmailDataStore.emailUsername = "user"
        mockEmailDataStore.nextAlias = "nextAlias@duck.com"

        assertEquals(expected, testee.getUserData())
    }

    @Test
    fun `signedInFlow - syncable setting notifies change - refresh email state`() = runTest {
        testee.signedInFlow().test {
            assertFalse(awaitItem())
            emailSyncableSetting.save("{\"username\":\"email\",\"personal_access_token\":\"token\"}")
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun givenNextAliasExists() {
        mockEmailDataStore.nextAlias = "alias"
    }

    class TestEmailService : EmailService {
        override suspend fun newAlias(authorization: String): EmailAlias = EmailAlias("alias")
    }
}

class FakeEmailDataStore : EmailDataStore {
    override var emailToken: String? = null
    override var nextAlias: String? = null
    override var emailUsername: String? = null
    override var cohort: String? = null
    override var lastUsedDate: String? = null

    var canUseEncryption: Boolean = false
    override fun canUseEncryption(): Boolean = canUseEncryption
}
