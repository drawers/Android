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

package com.duckduckgo.fingerprintprotection.store.features.fingerprintingcanvas

import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.fingerprintprotection.store.FingerprintProtectionDatabase
import com.duckduckgo.fingerprintprotection.store.FingerprintingCanvasEntity
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RealFingerprintingCanvasRepositoryTest {
    @get:Rule var coroutineRule = CoroutineTestRule()

    private lateinit var testee: RealFingerprintingCanvasRepository

    private val mockDatabase: FingerprintProtectionDatabase = mock()
    private val mockFingerprintingCanvasDao: FingerprintingCanvasDao = mock()

    @Before
    fun before() {
        whenever(mockFingerprintingCanvasDao.get()).thenReturn(null)
        whenever(mockDatabase.fingerprintingCanvasDao()).thenReturn(mockFingerprintingCanvasDao)
    }

    @Test
    fun `loadEmptyJson - initialized and does not have stored value - load empty json to memory`() =
        runTest {
            testee =
                RealFingerprintingCanvasRepository(
                    mockDatabase,
                    TestScope(),
                    coroutineRule.testDispatcherProvider,
                    isMainProcess = true,
                )

            verify(mockFingerprintingCanvasDao).get()
            assertEquals("{}", testee.fingerprintingCanvasEntity.json)
        }

    @Test
    fun `whenInitializedAndHasStoredValueThenLoadStoredJsonToMemory - load stored json to memory`() =
        runTest {
            whenever(mockFingerprintingCanvasDao.get()).thenReturn(fingerprintingCanvasEntity)
            testee =
                RealFingerprintingCanvasRepository(
                    mockDatabase,
                    TestScope(),
                    coroutineRule.testDispatcherProvider,
                    isMainProcess = true,
                )

            verify(mockFingerprintingCanvasDao).get()
            assertEquals(fingerprintingCanvasEntity.json, testee.fingerprintingCanvasEntity.json)
        }

    @Test
    fun `updateAll - update all called`() =
        runTest {
            testee =
                RealFingerprintingCanvasRepository(
                    mockDatabase,
                    TestScope(),
                    coroutineRule.testDispatcherProvider,
                    isMainProcess = true,
                )

            testee.updateAll(fingerprintingCanvasEntity)

            verify(mockFingerprintingCanvasDao).updateAll(fingerprintingCanvasEntity)
        }

    companion object {
        val fingerprintingCanvasEntity = FingerprintingCanvasEntity(json = "{\"key\":\"value\"}")
    }
}
