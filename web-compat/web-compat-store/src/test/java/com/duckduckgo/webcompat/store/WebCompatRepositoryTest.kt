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

package com.duckduckgo.webcompat.store

import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class WebCompatRepositoryTest {
    @get:Rule var coroutineRule = CoroutineTestRule()

    private lateinit var testee: RealWebCompatRepository

    private val mockDatabase: WebCompatDatabase = mock()
    private val mockWebCompatDao: WebCompatDao = mock()

    @Before
    fun before() {
        whenever(mockWebCompatDao.get()).thenReturn(null)
        whenever(mockDatabase.webCompatDao()).thenReturn(mockWebCompatDao)
    }

    @Test
    fun `getWebCompatEntity - initialized and no stored value - load empty json to memory`() =
        runTest {
            testee =
                RealWebCompatRepository(
                    mockDatabase,
                    TestScope(),
                    coroutineRule.testDispatcherProvider,
                    isMainProcess = true,
                )

            verify(mockWebCompatDao).get()
            assertEquals("{}", testee.getWebCompatEntity().json)
        }

    @Test
    fun `getWebCompatEntity - initialized and has stored value - loads stored json to memory`() =
        runTest {
            whenever(mockWebCompatDao.get()).thenReturn(webCompatEntity)
            testee =
                RealWebCompatRepository(
                    mockDatabase,
                    TestScope(),
                    coroutineRule.testDispatcherProvider,
                    isMainProcess = true,
                )

            verify(mockWebCompatDao).get()
            assertEquals(webCompatEntity.json, testee.getWebCompatEntity().json)
        }

    @Test
    fun `updateAll - update all called`() =
        runTest {
            testee =
                RealWebCompatRepository(
                    mockDatabase,
                    TestScope(),
                    coroutineRule.testDispatcherProvider,
                    isMainProcess = true,
                )

            testee.updateAll(webCompatEntity)

            verify(mockWebCompatDao).updateAll(webCompatEntity)
        }

    companion object {
        val webCompatEntity = WebCompatEntity(json = "{\"key\":\"value\"}")
    }
}
