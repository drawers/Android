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

package com.duckduckgo.fingerprintprotection

import androidx.lifecycle.LifecycleOwner
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import androidx.work.WorkManager
import com.duckduckgo.fingerprintprotection.impl.FingerprintProtectionSeedWorkerScheduler
import com.duckduckgo.fingerprintprotection.store.seed.FingerprintProtectionSeedRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class FingerprintProtectionSeedWorkerSchedulerTest {

    private val fingerprintProtectionSeedRepository: FingerprintProtectionSeedRepository = mock()
    private val mockWorkManager: WorkManager = mock()
    private val mockOwner: LifecycleOwner = mock()

    lateinit var fingerprintProtectionSeedWorkerScheduler: FingerprintProtectionSeedWorkerScheduler

    @Before
    fun before() {
        fingerprintProtectionSeedWorkerScheduler = FingerprintProtectionSeedWorkerScheduler(mockWorkManager, fingerprintProtectionSeedRepository)
    }

    @Test
    fun `onStop - store new seed and enqueue work with replace policy`() {
        fingerprintProtectionSeedWorkerScheduler.onStop(mockOwner)

        verify(mockWorkManager).enqueueUniquePeriodicWork(any(), eq(REPLACE), any())
        verify(fingerprintProtectionSeedRepository).storeNewSeed()
    }

    @Test
    fun `onStart - enqueue work with keep policy`() {
        fingerprintProtectionSeedWorkerScheduler.onStart(mockOwner)

        verify(mockWorkManager).enqueueUniquePeriodicWork(any(), eq(KEEP), any())
    }
}
