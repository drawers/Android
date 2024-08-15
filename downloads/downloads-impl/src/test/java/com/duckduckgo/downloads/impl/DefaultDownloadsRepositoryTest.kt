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

package com.duckduckgo.downloads.impl

import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.downloads.api.DownloadsRepository
import com.duckduckgo.downloads.api.model.DownloadItem
import com.duckduckgo.downloads.store.DownloadEntity
import com.duckduckgo.downloads.store.DownloadStatus.FINISHED
import com.duckduckgo.downloads.store.DownloadStatus.STARTED
import com.duckduckgo.downloads.store.DownloadsDao
import com.duckduckgo.downloads.store.DownloadsDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

class DefaultDownloadsRepositoryTest {

    @get:Rule
    @Suppress("unused")
    val coroutineRule = CoroutineTestRule()

    private val mockDb: DownloadsDatabase = mock()
    private val mockDao: DownloadsDao = mock()
    private val mockUrlFileDownloadCallManager: UrlFileDownloadCallManager = mock()
    private lateinit var repository: DownloadsRepository

    @Before
    fun before() {
        whenever(mockDb.downloadsDao()).thenReturn(mockDao)

        repository = DefaultDownloadsRepository(mockDb, mockUrlFileDownloadCallManager)
    }

    @Test
    fun `insert - download item - insert called`() = runTest {
        val item = oneItem()
        val entity = oneEntity()

        repository.insert(item)

        verify(mockDb.downloadsDao()).insert(entity)
        verifyNoInteractions(mockUrlFileDownloadCallManager)
    }

    @Test
    fun `insertAll - download items - insertAll called`() = runTest {
        val firstItem = oneItem()
        val secondItem = otherItem()
        val firstEntity = oneEntity()
        val secondEntity = otherEntity()

        repository.insertAll(listOf(firstItem, secondItem))

        verify(mockDb.downloadsDao()).insertAll(listOf(firstEntity, secondEntity))
        verifyNoInteractions(mockUrlFileDownloadCallManager)
    }

    @Test
    fun `update - download status finished and content length - update called with same params`() =
        runTest {
            val item = oneItem()
            val updatedStatus = FINISHED
            val updatedContentLength = 1111111L

            repository.update(
                downloadId = item.downloadId,
                downloadStatus = updatedStatus,
                contentLength = updatedContentLength,
            )

            verify(mockDb.downloadsDao()).update(
                downloadId = item.downloadId,
                downloadStatus = updatedStatus,
                contentLength = updatedContentLength,
            )

            verify(mockUrlFileDownloadCallManager).remove(item.downloadId)
        }

    @Test
    fun `update - download item by id with status started and content length - update called with same params`() =
        runTest {
            val item = oneItem()
            val updatedStatus = STARTED
            val updatedContentLength = 1111111L

            repository.update(
                downloadId = item.downloadId,
                downloadStatus = updatedStatus,
                contentLength = updatedContentLength,
            )

            verify(mockDb.downloadsDao()).update(
                downloadId = item.downloadId,
                downloadStatus = updatedStatus,
                contentLength = updatedContentLength,
            )

            verifyNoInteractions(mockUrlFileDownloadCallManager)
        }

    @Test
    fun `update - download item by file name with finished status and content length - update called with same params`() =
        runTest {
            val item = oneItem().copy(downloadId = 0L)
            val updatedStatus = FINISHED
            val updatedContentLength = 1111111L

            repository.update(
                fileName = item.fileName,
                downloadStatus = updatedStatus,
                contentLength = updatedContentLength,
            )

            verify(mockDb.downloadsDao()).update(
                fileName = item.fileName,
                downloadStatus = updatedStatus,
                contentLength = updatedContentLength,
            )

            verifyNoInteractions(mockUrlFileDownloadCallManager)
        }

    @Test
    fun `update - download item by file name with status started and content length - update called with same params`() =
        runTest {
            val item = oneItem().copy(downloadId = 0L)
            val updatedStatus = FINISHED
            val updatedContentLength = 1111111L

            repository.update(
                fileName = item.fileName,
                downloadStatus = updatedStatus,
                contentLength = updatedContentLength,
            )

            verify(mockDb.downloadsDao()).update(
                fileName = item.fileName,
                downloadStatus = updatedStatus,
                contentLength = updatedContentLength,
            )

            verifyNoInteractions(mockUrlFileDownloadCallManager)
        }

    @Test
    fun `delete - download item - delete called`() = runTest {
        val item = oneItem()

        repository.delete(item.downloadId)

        verify(mockDb.downloadsDao()).delete(item.downloadId)
        verify(mockUrlFileDownloadCallManager).remove(item.downloadId)
    }

    @Test
    fun `delete - list of download items - delete called`() = runTest {
        val firstItem = oneItem()
        val secondItem = otherItem()

        repository.delete(listOf(firstItem.downloadId, secondItem.downloadId))

        verify(mockDb.downloadsDao()).delete(listOf(firstItem.downloadId, secondItem.downloadId))
        verify(mockUrlFileDownloadCallManager, times(2)).remove(any())
    }

    @Test
    fun `deleteAll - delete with no params called`() = runTest {
        val entity = oneEntity()
        whenever(mockDb.downloadsDao().getDownloads()).thenReturn(listOf(entity))

        repository.deleteAll()

        verify(mockDb.downloadsDao()).delete()
        verify(mockUrlFileDownloadCallManager).remove(entity.downloadId)
    }

    @Test
    fun `getDownloadItem - calls getDownloadItem`() = runTest {
        val item = oneItem()
        val entity = oneEntity()
        whenever(mockDb.downloadsDao().getDownloadItem(item.downloadId)).thenReturn(entity)

        repository.getDownloadItem(item.downloadId)

        verify(mockDb.downloadsDao()).getDownloadItem(item.downloadId)
    }

    @Test
    fun `getDownloads - getDownloadItems called - getDownloads called`() = runTest {
        whenever(mockDb.downloadsDao().getDownloads()).thenReturn(listOf(oneEntity()))

        repository.getDownloads()

        verify(mockDb.downloadsDao()).getDownloads()
    }

    private fun oneItem() = DownloadItem(
        downloadId = 10L,
        downloadStatus = STARTED,
        fileName = "file.jpg",
        contentLength = 100L,
        createdAt = "2022-02-04",
        filePath = "/",
    )

    private fun otherItem() = DownloadItem(
        downloadId = 20L,
        downloadStatus = STARTED,
        fileName = "other-file.jpg",
        contentLength = 120L,
        createdAt = "2022-02-06",
        filePath = "/",
    )

    private fun oneEntity() = DownloadEntity(
        id = 0L,
        downloadId = 10L,
        downloadStatus = STARTED,
        fileName = "file.jpg",
        contentLength = 100L,
        createdAt = "2022-02-04",
        filePath = "/",
    )

    private fun otherEntity() = DownloadEntity(
        id = 0L,
        downloadId = 20L,
        downloadStatus = STARTED,
        fileName = "other-file.jpg",
        contentLength = 120L,
        createdAt = "2022-02-06",
        filePath = "/",
    )
}
