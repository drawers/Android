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

package com.duckduckgo.autofill.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.autofill.api.CredentialUpdateExistingCredentialsDialog.CredentialUpdateType
import com.duckduckgo.autofill.api.ExistingCredentialMatchDetector.ContainsCredentialsResult
import com.duckduckgo.autofill.api.ExistingCredentialMatchDetector.ContainsCredentialsResult.ExactMatch
import com.duckduckgo.autofill.api.ExistingCredentialMatchDetector.ContainsCredentialsResult.NoMatch
import com.duckduckgo.autofill.api.ExistingCredentialMatchDetector.ContainsCredentialsResult.UrlOnlyMatch
import com.duckduckgo.autofill.api.ExistingCredentialMatchDetector.ContainsCredentialsResult.UsernameMatch
import com.duckduckgo.autofill.api.ExistingCredentialMatchDetector.ContainsCredentialsResult.UsernameMissing
import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.impl.encoding.TestUrlUnicodeNormalizer
import com.duckduckgo.autofill.impl.securestorage.SecureStorage
import com.duckduckgo.autofill.impl.securestorage.WebsiteLoginDetails
import com.duckduckgo.autofill.impl.securestorage.WebsiteLoginDetailsWithCredentials
import com.duckduckgo.autofill.impl.urlmatcher.AutofillDomainNameUrlMatcher
import com.duckduckgo.autofill.impl.urlmatcher.AutofillUrlMatcher
import com.duckduckgo.autofill.store.AutofillPrefsStore
import com.duckduckgo.autofill.store.LastUpdatedTimeProvider
import com.duckduckgo.autofill.sync.CredentialsFixtures.toLoginCredentials
import com.duckduckgo.autofill.sync.CredentialsSyncMetadata
import com.duckduckgo.autofill.sync.SyncCredentialsListener
import com.duckduckgo.autofill.sync.inMemoryAutofillDatabase
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SecureStoreBackedAutofillStoreTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val lastUpdatedTimeProvider = object : LastUpdatedTimeProvider {
        override fun getInMillis(): Long = UPDATED_INITIAL_LAST_UPDATED
    }

    @Mock
    private lateinit var autofillPrefsStore: AutofillPrefsStore
    private lateinit var testee: SecureStoreBackedAutofillStore
    private lateinit var secureStore: FakeSecureStore

    private val autofillUrlMatcher: AutofillUrlMatcher = AutofillDomainNameUrlMatcher(TestUrlUnicodeNormalizer())

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(autofillPrefsStore.isEnabled).thenReturn(true)
    }

    @Test
    fun `setupTestee - autofill unavailable and prefs are all true - return true for autofill enabled`() {
        setupTestee(canAccessSecureStorage = false)

        assertTrue(testee.autofillEnabled)
    }

    @Test
    fun `autofillEnabled - prefs all false - return false`() = runTest {
        setupTesteeWithAutofillAvailable()
        whenever(autofillPrefsStore.isEnabled).thenReturn(false)

        assertFalse(testee.autofillEnabled)
    }

    @Test
    fun `setupTestee - secure storage available - return autofill available`() = runTest {
        setupTesteeWithAutofillAvailable()
        assertTrue(testee.autofillAvailable)
    }

    @Test
    fun `setupTestee - secure storage not available - return autofill available false`() {
        setupTestee(canAccessSecureStorage = false)
        assertFalse(testee.autofillAvailable)
    }

    @Test
    fun `getCredentials - autofill enabled but cannot access secure storage - returns empty`() = runTest {
        setupTestee(canAccessSecureStorage = false)
        val url = "example.com"
        storeCredentials(1, url, "username", "password")

        assertTrue(testee.getCredentials(url).isEmpty())
    }

    @Test
    fun `containsCredentials - store empty - no match`() = runTest {
        setupTesteeWithAutofillAvailable()
        val result = testee.containsCredentials("example.com", "username", "password")
        assertNotMatch(result)
    }

    @Test
    fun `containsCredentials - matching username entries but none matching URL - no match`() = runTest {
        setupTesteeWithAutofillAvailable()
        storeCredentials(1, "https://example.com", "username", "password")
        val result = testee.containsCredentials("foo.com", "username", "password")
        assertNotMatch(result)
    }

    @Test
    fun `containsCredentials - domain but different credentials - URL match`() = runTest {
        setupTesteeWithAutofillAvailable()
        storeCredentials(1, "https://example.com", "username", "password")
        val result = testee.containsCredentials("example.com", "differentUsername", "differentPassword")
        assertUrlOnlyMatch(result)
    }

    @Test
    fun `containsCredentials - site with no username - username missing`() = runTest {
        setupTesteeWithAutofillAvailable()
        storeCredentials(id = 1, domain = "https://example.com", username = null, password = "password")
        val result = testee.containsCredentials("example.com", "username", "password")
        assertUsernameMissing(result)
    }

    @Test
    fun `containsCredentials - different password exists for site with no username - url match`() = runTest {
        setupTesteeWithAutofillAvailable()
        storeCredentials(id = 1, domain = "https://example.com", username = null, password = "password")
        val result = testee.containsCredentials("example.com", "username", "not-the-same-password")
        assertUrlOnlyMatch(result)
    }

    @Test
    fun `containsCredentials - domain and username but different password`() = runTest {
        setupTesteeWithAutofillAvailable()
        storeCredentials(1, "https://example.com", "username", "password")
        val result = testee.containsCredentials("example.com", "username", "differentPassword")
        assertUsernameMatch(result)
    }

    @Test
    fun `containsCredentials - matching domain, username, and password`() = runTest {
        setupTesteeWithAutofillAvailable()
        storeCredentials(1, "https://example.com", "username", "password")
        val result = testee.containsCredentials("example.com", "username", "password")
        assertExactMatch(result)
    }

    @Test
    fun `getCredentials - no credentials for URL stored - return nothing`() = runTest {
        setupTesteeWithAutofillAvailable()
        storeCredentials(1, "url.com", "username1", "password123")

        assertEquals(0, testee.getCredentials("https://example.com").size)
    }

    @Test
    fun `getAllCredentials - no credentials saved - return nothing`() = runTest {
        setupTesteeWithAutofillAvailable()
        assertEquals(0, testee.getAllCredentials().first().size)
    }

    @Test
    fun `storeCredentials - missing username and storing a credential with no username - url only match`() = runTest {
        setupTesteeWithAutofillAvailable()
        storeCredentials(1, "https://example.com", username = null, password = "password")
        val result = testee.containsCredentials("example.com", username = null, password = "differentPassword")
        assertUrlOnlyMatch(result)
    }

    @Test
    fun `updateCredentials - url - updated only matching credential`() = runTest {
        setupTesteeWithAutofillAvailable()
        val url = "https://example.com"
        storeCredentials(1, url, "username1", "password123")
        storeCredentials(2, url, "username2", "password456")
        storeCredentials(3, url, "username3", "password789")
        val credentials = LoginCredentials(
            domain = url,
            username = "username1",
            password = "newpassword",
            id = 1,
        )

        testee.updateCredentials(url, credentials, CredentialUpdateType.Password)

        testee.getCredentials(url).run {
            this.assertHasLoginCredentials(url, "username1", "newpassword", UPDATED_INITIAL_LAST_UPDATED)
            this.assertHasLoginCredentials(url, "username2", "password456")
            this.assertHasLoginCredentials(url, "username3", "password789")
        }
    }

    @Test
    fun `updateCredentials - updated only matching credential`() = runTest {
        setupTesteeWithAutofillAvailable()
        val url = "example.com"
        storeCredentials(1, url, "username1", "password123")
        storeCredentials(2, url, "username2", "password456")
        storeCredentials(3, url, "username3", "password789")
        val credentials = LoginCredentials(
            domain = url,
            username = "username1",
            password = "newpassword",
            id = 1,
        )

        testee.updateCredentials(credentials)

        testee.getCredentials(url).run {
            this.assertHasLoginCredentials(url, "username1", "newpassword", UPDATED_INITIAL_LAST_UPDATED)
            this.assertHasLoginCredentials(url, "username2", "password456")
            this.assertHasLoginCredentials(url, "username3", "password789")
        }
    }

    @Test
    fun `updateCredentials - domain updated - clean raw URL`() = runTest {
        setupTesteeWithAutofillAvailable()
        val url = "https://example.com"
        storeCredentials(1, url, "username1", "password123")
        val credentials = LoginCredentials(
            domain = "https://www.example.com/test/path",
            username = "username1",
            password = "newpassword",
            id = 1,
        )

        testee.updateCredentials(credentials)

        testee.getCredentials(url).run {
            this.assertHasLoginCredentials("www.example.com", "username1", "newpassword", UPDATED_INITIAL_LAST_UPDATED)
        }
    }

    @Test
    fun `updateCredentials - for URL - updated only matching credential`() = runTest {
        setupTesteeWithAutofillAvailable()
        val url = "example.com"
        storeCredentials(1, url, null, "password123")
        storeCredentials(2, url, "username2", "password456")
        storeCredentials(3, url, "username3", "password789")
        val credentials = LoginCredentials(
            domain = url,
            username = "username1",
            password = "password123",
            id = 1,
        )

        testee.updateCredentials(url, credentials, CredentialUpdateType.Username)

        testee.getCredentials(url).run {
            this.assertHasLoginCredentials(url, "username1", "password123", UPDATED_INITIAL_LAST_UPDATED)
            this.assertHasLoginCredentials(url, "username2", "password456")
            this.assertHasLoginCredentials(url, "username3", "password789")
        }
    }

    @Test
    fun `updateCredentials - username missing for another URL - no updates made`() = runTest {
        setupTesteeWithAutofillAvailable()
        val urlStored = "example.com"
        val newDomain = "test.com"
        storeCredentials(1, urlStored, null, "password123")

        val credentials = LoginCredentials(domain = newDomain, username = "username1", password = "password123", id = 1)

        assertNull(testee.updateCredentials(newDomain, credentials, CredentialUpdateType.Username))

        testee.getCredentials(urlStored).run {
            this.assertHasLoginCredentials(urlStored, null, "password123")
        }
    }

    @Test
    fun `saveCredentials - return saved on get credentials`() = runTest {
        setupTesteeWithAutofillAvailable()
        val url = "example.com"
        val credentials = LoginCredentials(
            id = 1L,
            domain = url,
            username = "username1",
            password = "password",
        )
        testee.saveCredentials(url, credentials)

        assertEquals(credentials.copy(domain = "example.com", lastUpdatedMillis = UPDATED_INITIAL_LAST_UPDATED), testee.getCredentials(url)[0])
    }

    @Test
    fun `deleteCredentials - removes credential from store`() = runTest {
        setupTesteeWithAutofillAvailable()
        val url = "example.com"
        storeCredentials(1, url, "username1", "password123")
        storeCredentials(2, url, "username2", "password456")
        storeCredentials(3, url, "username3", "password789")
        testee.deleteCredentials(1)

        testee.getCredentials(url).run {
            this.assertHasNoLoginCredentials(url, "username1", "password123")
            this.assertHasLoginCredentials(url, "username2", "password456")
            this.assertHasLoginCredentials(url, "username3", "password789")
        }
    }

    @Test
    fun `getCredentialsWithId - stored credential - returns credentials`() = runTest {
        setupTesteeWithAutofillAvailable()
        val url = "example.com"
        storeCredentials(1, url, "username1", "password123")
        storeCredentials(2, url, "username2", "password456")

        assertEquals(
            LoginCredentials(
                id = 1,
                domain = url,
                username = "username1",
                password = "password123",
                lastUpdatedMillis = DEFAULT_INITIAL_LAST_UPDATED,
                notes = "notes",
            ),
            testee.getCredentialsWithId(1),
        )
    }

    @Test
    fun `getCredentialsWithId - no credential stored - null`() = runTest {
        setupTesteeWithAutofillAvailable()
        assertNull(testee.getCredentialsWithId(1))
    }

    @Test
    fun `getCredentials - exact url matches - credentials returned`() = runTest {
        setupTesteeWithAutofillAvailable()
        val savedUrl = "https://example.com"
        val visitedSite = "https://example.com"
        storeCredentials(1, savedUrl, "username1", "password123")

        val results = testee.getCredentials(visitedSite)
        assertEquals(1, results.size)
        assertEquals(1L, results[0].id)
    }

    @Test
    fun `getCredentials - exact subdomain matches - credentials returned`() = runTest {
        setupTesteeWithAutofillAvailable()
        val savedUrl = "https://subdomain.example.com"
        val visitedSite = "https://subdomain.example.com"
        storeCredentials(1, savedUrl, "username1", "password123")

        val results = testee.getCredentials(visitedSite)
        assertEquals(1, results.size)
        assertEquals(1L, results[0].id)
    }

    @Test
    fun `getCredentials - exact www subdomain matches - credentials returned`() = runTest {
        setupTesteeWithAutofillAvailable()
        val savedUrl = "https://www.example.com"
        val visitedSite = "https://www.example.com"
        storeCredentials(1, savedUrl, "username1", "password123")

        val results = testee.getCredentials(visitedSite)
        assertEquals(1, results.size)
        assertEquals(1L, results[0].id)
    }

    @Test
    fun `getCredentials - saved credential has subdomain and visited page does not - credential matched`() = runTest {
        setupTesteeWithAutofillAvailable()
        val savedUrl = "https://test.example.com"
        val visitedSite = "https://example.com"
        storeCredentials(1, savedUrl, "username1", "password123")

        val results = testee.getCredentials(visitedSite)
        assertEquals(1, results.size)
    }

    @Test
    fun `getCredentials - saved credential has no subdomain - credential not matched`() = runTest {
        setupTesteeWithAutofillAvailable()
        val savedUrl = "https://example.com"
        val visitedSite = "https://test.example.com"
        storeCredentials(1, savedUrl, "username1", "password123")

        val results = testee.getCredentials(visitedSite)
        assertEquals(1, results.size)
    }

    @Test
    fun `getCredentials - different subdomain - credential matched`() = runTest {
        setupTesteeWithAutofillAvailable()
        val savedUrl = "https://test.example.com"
        val visitedSite = "https://different.example.com"
        storeCredentials(1, savedUrl, "username1", "password123")

        val results = testee.getCredentials(visitedSite)
        assertEquals(1, results.size)
    }

    @Test
    fun `reinsertCredentials - all fields preserved`() = runTest {
        setupTesteeWithAutofillAvailable()
        val originalCredentials = LoginCredentials(
            id = 123,
            domain = "example.com",
            username = "user",
            password = "abc123",
            domainTitle = "title",
            notes = "notes",
            lastUpdatedMillis = 1000,
        )
        testee.reinsertCredentials(originalCredentials)

        val reinsertedCredentials = secureStore.getWebsiteLoginDetailsWithCredentials(123)
        assertNotNull("Failed to find credentials", reinsertedCredentials)
        assertEquals(originalCredentials.id, reinsertedCredentials!!.details.id)
        assertEquals(originalCredentials.username, reinsertedCredentials.details.username)
        assertEquals(originalCredentials.domain, reinsertedCredentials.details.domain)
        assertEquals(originalCredentials.domainTitle, reinsertedCredentials.details.domainTitle)
        assertEquals(originalCredentials.lastUpdatedMillis, reinsertedCredentials.details.lastUpdatedMillis)
        assertEquals(originalCredentials.notes, reinsertedCredentials.notes)
        assertEquals(originalCredentials.password, reinsertedCredentials.password)
    }

    @Test
    fun `updateCredentials - last updated timestamp gets updated`() = runTest {
        setupTesteeWithAutofillAvailable()
        val saved = storeCredentials(id = 1, domain = "example.com", username = "username", password = "password")
        val updated = testee.updateCredentials(saved)!!
        assertEquals(UPDATED_INITIAL_LAST_UPDATED, updated.lastUpdatedMillis)
    }

    @Test
    fun `updateCredentials - specify not to update last updated timestamp - not updated`() = runTest {
        setupTesteeWithAutofillAvailable()
        val saved = storeCredentials(id = 1, domain = "example.com", username = "username", password = "password")
        val updated = testee.updateCredentials(saved, refreshLastUpdatedTimestamp = false)!!
        assertEquals(DEFAULT_INITIAL_LAST_UPDATED, updated.lastUpdatedMillis)
    }

    private fun List<LoginCredentials>.assertHasNoLoginCredentials(
        url: String,
        username: String,
        password: String,
        lastUpdatedTimeMillis: Long = DEFAULT_INITIAL_LAST_UPDATED,
    ) {
        val result = this.filter {
            it.domain == url && it.username == username && it.password == password && it.lastUpdatedMillis == lastUpdatedTimeMillis
        }
        assertEquals(0, result.size)
    }

    private fun List<LoginCredentials>.assertHasLoginCredentials(
        url: String,
        username: String?,
        password: String,
        lastUpdatedTimeMillis: Long = DEFAULT_INITIAL_LAST_UPDATED,
    ) {
        val result = this.filter {
            it.domain == url && it.username == username && it.password == password && it.lastUpdatedMillis == lastUpdatedTimeMillis
        }
        assertEquals(1, result.size)
    }

    private fun setupTesteeWithAutofillAvailable() {
        setupTestee(canAccessSecureStorage = true)
    }

    private fun setupTestee(
        canAccessSecureStorage: Boolean,
    ) {
        secureStore = FakeSecureStore(canAccessSecureStorage)
        testee = SecureStoreBackedAutofillStore(
            secureStorage = secureStore,
            lastUpdatedTimeProvider = lastUpdatedTimeProvider,
            autofillPrefsStore = autofillPrefsStore,
            dispatcherProvider = coroutineTestRule.testDispatcherProvider,
            autofillUrlMatcher = autofillUrlMatcher,
            syncCredentialsListener = SyncCredentialsListener(
                CredentialsSyncMetadata(inMemoryAutofillDatabase().credentialsSyncDao()),
                coroutineTestRule.testDispatcherProvider,
                coroutineTestRule.testScope,
            ),
        )
    }

    private fun assertNotMatch(result: ContainsCredentialsResult) {
        assertTrue(String.format("Expected NoMatch but was %s", result.javaClass.simpleName), result is NoMatch)
    }

    private fun assertUrlOnlyMatch(result: ContainsCredentialsResult) {
        assertTrue(String.format("Expected UrlOnlyMatch but was %s", result.javaClass.simpleName), result is UrlOnlyMatch)
    }

    private fun assertUsernameMissing(result: ContainsCredentialsResult) {
        assertTrue(String.format("Expected UsernameMissing but was %s", result.javaClass.simpleName), result is UsernameMissing)
    }

    private fun assertUsernameMatch(result: ContainsCredentialsResult) {
        assertTrue(String.format("Expected UsernameMatch but was %s", result.javaClass.simpleName), result is UsernameMatch)
    }

    private fun assertExactMatch(result: ContainsCredentialsResult) {
        assertTrue(String.format("Expected ExactMatch but was %s", result.javaClass.simpleName), result is ExactMatch)
    }

    private suspend fun storeCredentials(
        id: Long,
        domain: String,
        username: String?,
        password: String,
        lastUpdatedTimeMillis: Long = DEFAULT_INITIAL_LAST_UPDATED,
        notes: String = "notes",
    ): LoginCredentials {
        val details = WebsiteLoginDetails(domain = domain, username = username, id = id, lastUpdatedMillis = lastUpdatedTimeMillis)
        val credentials = WebsiteLoginDetailsWithCredentials(details, password, notes)
        return secureStore.addWebsiteLoginDetailsWithCredentials(credentials).toLoginCredentials()
    }

    private class FakeSecureStore(val canAccessSecureStorage: Boolean) : SecureStorage {

        private val credentials = mutableListOf<WebsiteLoginDetailsWithCredentials>()

        override suspend fun addWebsiteLoginDetailsWithCredentials(
            websiteLoginDetailsWithCredentials: WebsiteLoginDetailsWithCredentials,
        ): WebsiteLoginDetailsWithCredentials {
            val id = websiteLoginDetailsWithCredentials.details.id ?: (credentials.size.toLong() + 1)
            val credentialWithId: WebsiteLoginDetailsWithCredentials = websiteLoginDetailsWithCredentials.copy(
                details = websiteLoginDetailsWithCredentials.details.copy(id = id),
            )
            credentials.add(credentialWithId)
            return credentialWithId
        }

        override suspend fun addWebsiteLoginDetailsWithCredentials(credentials: List<WebsiteLoginDetailsWithCredentials>) {
            credentials.forEach { addWebsiteLoginDetailsWithCredentials(it) }
        }

        override suspend fun websiteLoginDetailsForDomain(domain: String): Flow<List<WebsiteLoginDetails>> {
            return flow {
                emit(
                    credentials.filter {
                        it.details.domain?.contains(domain) == true
                    }.map {
                        it.details
                    },
                )
            }
        }

        override suspend fun websiteLoginDetails(): Flow<List<WebsiteLoginDetails>> {
            return flow {
                emit(credentials.map { it.details })
            }
        }

        override suspend fun getWebsiteLoginDetailsWithCredentials(id: Long): WebsiteLoginDetailsWithCredentials? {
            return credentials.firstOrNull() { it.details.id == id }
        }

        override suspend fun websiteLoginDetailsWithCredentialsForDomain(domain: String): Flow<List<WebsiteLoginDetailsWithCredentials>> {
            return flow {
                emit(
                    credentials.filter {
                        it.details.domain?.contains(domain) == true
                    },
                )
            }
        }

        override suspend fun websiteLoginDetailsWithCredentials(): Flow<List<WebsiteLoginDetailsWithCredentials>> {
            return flow {
                emit(credentials)
            }
        }

        override suspend fun updateWebsiteLoginDetailsWithCredentials(
            websiteLoginDetailsWithCredentials: WebsiteLoginDetailsWithCredentials,
        ): WebsiteLoginDetailsWithCredentials {
            credentials.indexOfFirst { it.details.id == websiteLoginDetailsWithCredentials.details.id }.also {
                credentials[it] = websiteLoginDetailsWithCredentials
            }
            return websiteLoginDetailsWithCredentials
        }

        override suspend fun deleteWebsiteLoginDetailsWithCredentials(id: Long) {
            credentials.removeAll { it.details.id == id }
        }

        override suspend fun deleteWebSiteLoginDetailsWithCredentials(ids: List<Long>) {
            credentials.removeAll { ids.contains(it.details.id) }
        }

        override suspend fun addToNeverSaveList(domain: String) {
        }

        override suspend fun clearNeverSaveList() {
        }

        override suspend fun neverSaveListCount(): Flow<Int> = emptyFlow()
        override suspend fun isInNeverSaveList(domain: String): Boolean = false

        override fun canAccessSecureStorage(): Boolean = canAccessSecureStorage
    }

    companion object {
        private const val DEFAULT_INITIAL_LAST_UPDATED = 200L
        private const val UPDATED_INITIAL_LAST_UPDATED = 10000L
    }
}
