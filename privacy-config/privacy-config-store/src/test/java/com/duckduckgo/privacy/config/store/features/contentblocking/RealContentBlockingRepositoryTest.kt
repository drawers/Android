/*
 * Copyright (c) 2021 DuckDuckGo
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

package com.duckduckgo.privacy.config.store.features.contentblocking

import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.privacy.config.store.ContentBlockingExceptionEntity
import com.duckduckgo.privacy.config.store.PrivacyConfigDatabase
import com.duckduckgo.privacy.config.store.toFeatureException
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RealContentBlockingRepositoryTest {

    @get:Rule var coroutineRule = CoroutineTestRule()

    lateinit var testee: RealContentBlockingRepository

    private val mockDatabase: PrivacyConfigDatabase = mock()
    private val mockContentBlockingDao: ContentBlockingDao = mock()

    @Before
    fun before() {
        whenever(mockDatabase.contentBlockingDao()).thenReturn(mockContentBlockingDao)
    }

    @Test
    fun `given - repository created - exceptions loaded into memory`() {
        givenContentBlockingDaoContainsExceptions()

        testee =
            RealContentBlockingRepository(
                mockDatabase,
                TestScope(),
                coroutineRule.testDispatcherProvider,
                true,
            )

        assertEquals(
            contentBlockingException.toFeatureException(),
            testee.exceptions.first(),
        )
    }

    @Test
    fun `updateAll - update all called`() =
        runTest {
            testee =
                RealContentBlockingRepository(
                    mockDatabase,
                    TestScope(),
                    coroutineRule.testDispatcherProvider,
                    true,
                )

            testee.updateAll(listOf())

            verify(mockContentBlockingDao).updateAll(anyList())
        }

    @Test
    fun `updateAll - previous exceptions cleared`() =
        runTest {
            givenContentBlockingDaoContainsExceptions()
            testee =
                RealContentBlockingRepository(
                    mockDatabase,
                    TestScope(),
                    coroutineRule.testDispatcherProvider,
                    true,
                )
            assertEquals(1, testee.exceptions.size)
            reset(mockContentBlockingDao)

            testee.updateAll(listOf())

            assertEquals(0, testee.exceptions.size)
        }

    private fun givenContentBlockingDaoContainsExceptions() {
        whenever(mockContentBlockingDao.getAll()).thenReturn(listOf(contentBlockingException))
    }

    companion object {
        val contentBlockingException =
            ContentBlockingExceptionEntity("example.com", "my reason here")
    }
}
