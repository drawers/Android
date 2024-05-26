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

package com.duckduckgo.autofill.sync

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.duckduckgo.autofill.store.AutofillDatabase
import com.duckduckgo.autofill.store.CredentialsSyncMetadataEntity
import com.duckduckgo.common.test.CoroutineTestRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class CredentialsSyncMetadataTest {

    @get:Rule
    @Suppress("unused")
    val coroutineRule = CoroutineTestRule()

    private val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AutofillDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dao = db.credentialsSyncDao()
    private val testee = CredentialsSyncMetadata(dao)

    @After
    fun after() {
        db.close()
    }

    @Test
    fun `addOrUpdate - new entity - entity inserted`() {
        assertNull(dao.getLocalId("syncId"))

        testee.addOrUpdate(CredentialsSyncMetadataEntity("syncId", 123L, null, null))

        assertEquals(123L, dao.getLocalId("syncId"))
    }

    @Test
    fun `addOrUpdate - existing entity - entity updated`() {
        dao.insert(CredentialsSyncMetadataEntity("syncId", 123L, null, null))

        testee.addOrUpdate(CredentialsSyncMetadataEntity("syncId", 456L, null, null))

        assertEquals(456L, dao.getLocalId("syncId"))
    }

    @Test
    fun `addOrUpdate - update existing entity by local id - entity updated`() {
        dao.insert(CredentialsSyncMetadataEntity("syncId", 123L, null, null))

        testee.addOrUpdate(CredentialsSyncMetadataEntity("syncId2", 123L, null, null))

        assertEquals("syncId2", dao.getSyncMetadata(123L)?.syncId)
    }

    @Test
    fun `getSyncMetadata - autofill ID not found - return null`() {
        val syncId = testee.getSyncMetadata(123L)

        assertNull(syncId)
    }

    @Test
    fun `getSyncMetadata - loginId exists - return sync metadata`() {
        val loginId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = loginId, null, null))

        val result = testee.getSyncMetadata(loginId)?.syncId

        assertEquals(syncId, result)
    }

    @Test
    fun `createSyncId - non-existing id - return new sync id`() {
        val syncId = testee.createSyncId(123L)

        assertNotNull(syncId)
    }

    @Test
    fun `createSyncId - existing id - return existing syncId`() {
        val loginId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = loginId, null, null))

        val result = testee.createSyncId(loginId)

        assertEquals(syncId, result)
        assertEquals(syncId, dao.getSyncMetadata(loginId)?.syncId)
    }

    @Test
    fun `getLocalId - local id not found - return null`() {
        val localId = testee.getLocalId("syncId")

        assertNull(localId)
    }

    @Test
    fun `getLocalId - localId exists - return localId`() {
        val localId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = localId, null, null))

        val result = testee.getLocalId(syncId)

        assertEquals(localId, result)
    }

    @Test
    fun `getRemovedEntitiesSince - entities with deleted_at - return entities`() {
        val loginId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = loginId, deleted_at = "2023-07-21T20:21:40.552Z", null))

        val result = testee.getRemovedEntitiesSince("2023-07-21T20:21:39.000Z")

        assertEquals(1, result.size)
        assertEquals(loginId, result[0].localId)
        assertNotNull(result[0].deleted_at)
    }

    @Test
    fun `getRemovedEntitiesSince - entities previous to since - do not return`() {
        val loginId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = loginId, deleted_at = "2021-08-30T00:39:00Z", null))

        val result = testee.getRemovedEntitiesSince("2022-08-30T00:40:00Z")

        assertEquals(0, result.size)
    }

    @Test
    fun `onEntityRemoved - update deletedAt if exists`() {
        val loginId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = loginId, null, null))

        testee.onEntityRemoved(loginId)

        val result = dao.getSyncMetadata(loginId)!!
        assertNotNull(result.deleted_at)
    }

    @Test
    fun `onEntitiesRemoved - entities removed - update deleted_at if exists`() {
        val loginId1 = 1L
        val syncId1 = "syncId_1"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId1, localId = loginId1, null, null))

        val loginId2 = 2L
        val syncId2 = "syncId_2"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId2, localId = loginId2, null, null))

        testee.onEntitiesRemoved(listOf(loginId1, loginId2))

        assertNotNull(dao.getSyncMetadata(loginId1)!!.deleted_at)
        assertNotNull(dao.getSyncMetadata(loginId2)!!.deleted_at)
    }

    @Test
    fun `removeDeletedEntities - delete entities before date`() {
        val loginId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = loginId, deleted_at = "2022-08-30T00:00:00Z", null))

        testee.removeDeletedEntities("2022-08-30T00:00:00Z")

        assertNull(dao.getSyncMetadata(loginId))
    }

    @Test
    fun `removeDeletedEntities - keep entities after date`() {
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = 123L, deleted_at = "2022-08-30T00:00:00Z", null))
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = 345L, deleted_at = "2022-09-30T00:00:00Z", null))

        testee.removeDeletedEntities("2022-08-30T00:00:00Z")

        assertNull(dao.getSyncMetadata(123L))
        assertNotNull(dao.getSyncMetadata(345L))
    }

    @Test
    fun `removeEntityWith - localId - remove entity`() {
        val loginId = 123L
        dao.insert(CredentialsSyncMetadataEntity(syncId = "syncId", localId = loginId, null, null))

        testee.removeEntityWith(loginId)

        assertNull(dao.getSyncMetadata(loginId))
    }

    @Test
    fun `removeEntityWith - syncId - remove entity`() {
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = 123L, null, null))

        testee.removeEntityWith(syncId)

        assertNull(dao.getSyncMetadata(123L))
    }

    @Test
    fun `onEntityChanged - update modified_at`() {
        val loginId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = loginId, null, null))

        testee.onEntityChanged(loginId)

        val result = dao.getSyncMetadata(loginId)!!
        assertNotNull(result.modified_at)
    }

    @Test
    fun `onEntitiesChanged - entity in list - update modified_at`() {
        val loginId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = loginId, null, null))

        testee.onEntitiesChanged(listOf(loginId))

        val result = dao.getSyncMetadata(loginId)!!
        assertNotNull(result.modified_at)
    }

    @Test
    fun `onEntityChanged - entity does not exist - inserted with modifiedAt`() {
        val loginId = 123L

        testee.onEntityChanged(loginId)

        val result = dao.getSyncMetadata(loginId)!!
        assertNotNull(result.modified_at)
    }

    @Test
    fun `onEntitiesChanged - entity does not exist - inserted with modifiedAt`() {
        val loginId = 123L

        testee.onEntitiesChanged(listOf(loginId))

        val result = dao.getSyncMetadata(loginId)!!
        assertNotNull(result.modified_at)
    }

    @Test
    fun `getChangesSince - return changes`() {
        val loginId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = loginId, null, "2022-08-30T00:40:00Z"))

        val result = testee.getChangesSince("2022-08-30T00:30:00Z")

        assertEquals(1, result.size)
        assertEquals(loginId, result[0].localId)
    }

    @Test
    fun `getAllCredentials - returns all`() {
        val loginId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = loginId, null, null))

        val result = testee.getAllCredentials()

        assertEquals(1, result.size)
        assertEquals(loginId, result[0].localId)
    }

    @Test
    fun `clearAll - remove all`() {
        val loginId = 123L
        val syncId = "syncId"
        dao.insert(CredentialsSyncMetadataEntity(syncId = syncId, localId = loginId, null, null))

        testee.clearAll()

        assertEquals(0, dao.getAll().size)
    }
}
