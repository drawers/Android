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

package com.duckduckgo.remote.messaging.impl.matchers

import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.remote.messaging.api.JsonMatchingAttribute
import com.duckduckgo.remote.messaging.api.RemoteMessagingRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class InteractedWithMessageMatcherTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val remoteMessagingRepository: RemoteMessagingRepository = mock()

    @Test
    fun `map - map key interacted with message - matching attribute`() = runTest {
        val matcher = InteractedWithMessageMatcher(remoteMessagingRepository)
        val jsonMatchingAttribute = JsonMatchingAttribute(value = listOf("1", "2", "3"))
        val result = matcher.map("interactedWithMessage", jsonMatchingAttribute)
        assertTrue(result is InteractedWithMessageMatchingAttribute)
        assertEquals(listOf("1", "2", "3"), (result as InteractedWithMessageMatchingAttribute).messageIds)
    }

    @Test
    fun `map - json matching attribute value is null - return null`() = runTest {
        val matcher = InteractedWithMessageMatcher(remoteMessagingRepository)
        val jsonMatchingAttribute = JsonMatchingAttribute(value = null)
        val result = matcher.map("interactedWithMessage", jsonMatchingAttribute)
        assertNull(result)
    }

    @Test
    fun `map - json matching attribute value is empty - return null`() = runTest {
        val matcher = InteractedWithMessageMatcher(remoteMessagingRepository)
        val jsonMatchingAttribute = JsonMatchingAttribute(value = emptyList<String>())
        val result = matcher.map("interactedWithMessage", jsonMatchingAttribute)
        assertNull(result)
    }

    @Test
    fun `map - json matching attribute value not list - return null`() = runTest {
        val matcher = InteractedWithMessageMatcher(remoteMessagingRepository)
        val jsonMatchingAttribute = JsonMatchingAttribute(value = 1)
        val result = matcher.map("interactedWithMessage", jsonMatchingAttribute)
        assertNull(result)
    }

    @Test
    fun `evaluate - message id matches - returns true`() = runTest {
        givenMessageIdDismissed(listOf("1", "2", "3"))
        val matcher = InteractedWithMessageMatcher(remoteMessagingRepository)
        val matchingAttribute = InteractedWithMessageMatchingAttribute(listOf("1", "2", "3"))
        val result = matcher.evaluate(matchingAttribute)!!
        assertTrue(result)
    }

    @Test
    fun `evaluate - one dismissed message id matches - returns true`() = runTest {
        givenMessageIdDismissed(listOf("1", "2", "3"))
        val matcher = InteractedWithMessageMatcher(remoteMessagingRepository)
        val matchingAttribute = InteractedWithMessageMatchingAttribute(listOf("1", "4", "5"))
        val result = matcher.evaluate(matchingAttribute)!!
        assertTrue(result)
    }

    @Test
    fun `evaluate - no dismissed message id matches - returns false`() = runTest {
        givenMessageIdDismissed(listOf("1", "2", "3"))
        val matcher = InteractedWithMessageMatcher(remoteMessagingRepository)
        val matchingAttribute = InteractedWithMessageMatchingAttribute(listOf("4", "5"))
        val result = matcher.evaluate(matchingAttribute)!!
        assertFalse(result)
    }

    @Test(expected = AssertionError::class)
    fun `givenMessageIdDismissed - empty list in matching attribute - return exception`() = runTest {
        givenMessageIdDismissed(listOf("1", "2", "3"))
        val matcher = InteractedWithMessageMatcher(remoteMessagingRepository)
        val matchingAttribute = InteractedWithMessageMatchingAttribute(emptyList())
        matcher.evaluate(matchingAttribute)
    }

    private fun givenMessageIdDismissed(listOf: List<String>) {
        whenever(remoteMessagingRepository.dismissedMessages()).thenReturn(listOf)
    }
}
