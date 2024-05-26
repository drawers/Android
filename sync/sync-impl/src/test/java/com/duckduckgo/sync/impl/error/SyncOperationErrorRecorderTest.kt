/*
 * Copyright (c) 2024 DuckDuckGo
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

package com.duckduckgo.sync.impl.error

import com.duckduckgo.sync.impl.pixels.SyncPixels
import com.duckduckgo.sync.store.model.SyncOperationErrorType
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

internal class SyncOperationErrorRecorderTest {

    private val syncPixels: SyncPixels = mock()
    private val repository: SyncOperationErrorRepository = mock()

    private val recorder = RealSyncOperationErrorRecorder(syncPixels, repository)

    @Test
    fun `record - encrypt error reported - adds error to repository`() {
        val error = SyncOperationErrorType.DATA_ENCRYPT

        recorder.record(error)

        verify(repository).addError(error)
    }

    @Test
    fun `record - repository adds error`() {
        val error = SyncOperationErrorType.DATA_DECRYPT

        recorder.record(error)

        verify(repository).addError(error)
    }
}
