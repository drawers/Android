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

package com.duckduckgo.autofill.sync.persister

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.store.CredentialsSyncMetadataEntity
import com.duckduckgo.autofill.sync.CredentialsFixtures.amazonCredentials
import com.duckduckgo.autofill.sync.CredentialsFixtures.spotifyCredentials
import com.duckduckgo.autofill.sync.CredentialsFixtures.toLoginCredentialEntryResponse
import com.duckduckgo.autofill.sync.CredentialsFixtures.toWebsiteLoginCredentials
import com.duckduckgo.autofill.sync.CredentialsFixtures.twitterCredentials
import com.duckduckgo.autofill.sync.CredentialsSync
import com.duckduckgo.autofill.sync.CredentialsSyncMapper
import com.duckduckgo.autofill.sync.CredentialsSyncMetadata
import com.duckduckgo.autofill.sync.FakeCredentialsSyncLocalValidationFeature
import com.duckduckgo.autofill.sync.FakeCredentialsSyncStore
import com.duckduckgo.autofill.sync.FakeCrypto
import com.duckduckgo.autofill.sync.FakeSecureStorage
import com.duckduckgo.autofill.sync.credentialsSyncEntries
import com.duckduckgo.autofill.sync.inMemoryAutofillDatabase
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.sync.api.engine.SyncMergeResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class CredentialsDedupStrategyTest {

    @get:Rule
    @Suppress("unused")
    val coroutineRule = CoroutineTestRule()

    private val db = inMemoryAutofillDatabase()
    private val secureStorage = FakeSecureStorage()
    private val credentialsSyncStore = FakeCredentialsSyncStore()
    private val credentialsSyncMetadata = CredentialsSyncMetadata(db.credentialsSyncDao())
    private val credentialsSync = CredentialsSync(
        secureStorage,
        credentialsSyncStore,
        credentialsSyncMetadata,
        FakeCrypto(),
        FakeCredentialsSyncLocalValidationFeature(),
    )

    @After fun after() = runBlocking {
        db.close()
    }

    private val testee = CredentialsDedupStrategy(
        credentialsSync = credentialsSync,
        credentialsSyncMapper = CredentialsSyncMapper(FakeCrypto()),
        dispatchers = coroutineRule.testDispatcherProvider,
    )

    @Test
    fun `processEntries - no local entities - all remote entities stored`() = runTest {
        givenLocalCredentials()
        val remoteCredentials = credentialsSyncEntries(
            entries = listOf(
                twitterCredentials.toLoginCredentialEntryResponse(),
                spotifyCredentials.toLoginCredentialEntryResponse(),
            ),
            last_modified = "2022-08-30T00:00:00Z",
        )

        val result = testee.processEntries(remoteCredentials, "2022-08-30T00:00:00Z")

        assertTrue(result is SyncMergeResult.Success)
        val storedValues = secureStorage.websiteLoginDetailsWithCredentials().first()
        assertEquals(2, storedValues.count())
        assertTrue(credentialsSyncMetadata.getLocalId("1") == twitterCredentials.id)
        assertTrue(credentialsSyncMetadata.getLocalId("2") == spotifyCredentials.id)
    }

    @Test
    fun `processEntries - duplicates exist and remote are more recent - de-duplicate and store most recent`() = runTest {
        givenLocalCredentials(
            twitterCredentials,
            spotifyCredentials,
        )

        val remoteCredentials = credentialsSyncEntries(
            entries = listOf(
                twitterCredentials.toLoginCredentialEntryResponse().copy(id = "1a"),
                spotifyCredentials.toLoginCredentialEntryResponse().copy(id = "2a"),
            ),
            last_modified = "2022-08-30T00:00:00Z",
        )
        val result = testee.processEntries(remoteCredentials, "2022-08-30T00:00:00Z")

        assertTrue(result is SyncMergeResult.Success)
        val storedValues = secureStorage.websiteLoginDetailsWithCredentials().first()
        assertEquals(2, storedValues.count())

        assertTrue(credentialsSyncMetadata.getLocalId("1") == null)
        assertTrue(credentialsSyncMetadata.getLocalId("2") == null)
        assertTrue(credentialsSyncMetadata.getLocalId("1a") == twitterCredentials.id)
        assertTrue(credentialsSyncMetadata.getLocalId("2a") == spotifyCredentials.id)
    }

    @Test
    fun `processEntries - duplicates exist and local are more recent - de-duplicate and store most recent`() = runTest {
        givenLocalCredentials(
            twitterCredentials.copy(lastUpdatedMillis = 1689592358516),
            spotifyCredentials.copy(lastUpdatedMillis = 1689592358516),
        )

        val remoteCredentials = credentialsSyncEntries(
            entries = listOf(
                twitterCredentials.toLoginCredentialEntryResponse().copy(id = "1a", title = "newTitle"),
                spotifyCredentials.toLoginCredentialEntryResponse().copy(id = "2a", title = "newTitle"),
            ),
            last_modified = "2022-08-30T00:00:00Z",
        )
        val result = testee.processEntries(remoteCredentials, "2022-08-30T00:00:00Z")

        assertTrue(result is SyncMergeResult.Success)
        val storedValues = secureStorage.websiteLoginDetailsWithCredentials().first()
        assertEquals(2, storedValues.count())

        assertTrue(credentialsSyncMetadata.getLocalId("1") == null)
        assertTrue(credentialsSyncMetadata.getLocalId("2") == null)
        assertTrue(credentialsSyncMetadata.getLocalId("1a") == twitterCredentials.id)
        assertTrue(credentialsSyncMetadata.getLocalId("2a") == spotifyCredentials.id)
        assertTrue(credentialsSync.getCredentialWithSyncId("1a")?.domainTitle == twitterCredentials.domainTitle)
        assertTrue(credentialsSync.getCredentialWithSyncId("2a")?.domainTitle == spotifyCredentials.domainTitle)
    }

    @Test
    fun `processEntries - local has duplicates and no duplicates - dedup and keep the rest`() = runTest {
        givenLocalCredentials(
            twitterCredentials,
            spotifyCredentials,
            amazonCredentials,
        )
        credentialsSyncMetadata.addOrUpdate(CredentialsSyncMetadataEntity("1", twitterCredentials.id!!, null, null))
        credentialsSyncMetadata.addOrUpdate(CredentialsSyncMetadataEntity("2", spotifyCredentials.id!!, null, null))
        credentialsSyncMetadata.addOrUpdate(CredentialsSyncMetadataEntity("3", amazonCredentials.id!!, null, null))

        val remoteCredentials = credentialsSyncEntries(
            entries = listOf(
                twitterCredentials.toLoginCredentialEntryResponse(),
                spotifyCredentials.toLoginCredentialEntryResponse(),
            ),
            last_modified = "2022-08-30T00:00:00Z",
        )

        val result = testee.processEntries(remoteCredentials, "2022-08-30T00:00:00Z")

        assertTrue(result is SyncMergeResult.Success)
        val storedValues = secureStorage.websiteLoginDetailsWithCredentials().first()
        assertEquals(3, storedValues.count())
        assertTrue(credentialsSyncMetadata.getLocalId("1") == 1L)
        assertTrue(credentialsSyncMetadata.getLocalId("2") == 2L)
        assertTrue(credentialsSyncMetadata.getLocalId("3") == 3L)
    }

    @Test
    fun `processEntries - remote has duplicates and new credentials - dedup and insert new`() = runTest {
        givenLocalCredentials(
            twitterCredentials,
            spotifyCredentials,
        )
        val remoteCredentials = credentialsSyncEntries(
            entries = listOf(
                twitterCredentials.toLoginCredentialEntryResponse(),
                spotifyCredentials.toLoginCredentialEntryResponse(),
                amazonCredentials.toLoginCredentialEntryResponse(),
            ),
            last_modified = "2022-08-30T00:00:00Z",
        )

        val result = testee.processEntries(remoteCredentials, "2022-08-30T00:00:00Z")

        assertTrue(result is SyncMergeResult.Success)
        val storedValues = secureStorage.websiteLoginDetailsWithCredentials().first()
        assertEquals(3, storedValues.count())
        assertTrue(credentialsSyncMetadata.getLocalId("1") == 1L)
        assertTrue(credentialsSyncMetadata.getLocalId("2") == 2L)
        assertTrue(credentialsSyncMetadata.getLocalId("3") == 3L)
    }

    @Test
    fun `processEntries - remote is empty - no changes`() = runTest {
        givenLocalCredentials(
            twitterCredentials,
            spotifyCredentials,
        )
        val remoteCredentials = credentialsSyncEntries(
            entries = emptyList(),
            last_modified = "2022-08-30T00:00:00Z",
        )

        val result = testee.processEntries(remoteCredentials, "2022-08-30T00:00:00Z")

        assertTrue(result is SyncMergeResult.Success)
        val storedValues = secureStorage.websiteLoginDetailsWithCredentials().first()
        assertEquals(2, storedValues.count())
        assertTrue(credentialsSyncMetadata.getLocalId("1") == 1L)
        assertTrue(credentialsSyncMetadata.getLocalId("2") == 2L)
    }

    private suspend fun givenLocalCredentials(vararg credentials: LoginCredentials) {
        credentials.forEach {
            secureStorage.addWebsiteLoginDetailsWithCredentials(it.toWebsiteLoginCredentials())
            credentialsSyncMetadata.addOrUpdate(CredentialsSyncMetadataEntity(it.id!!.toString(), it.id!!, null, null))
        }
    }
}
