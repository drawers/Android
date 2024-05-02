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

package com.duckduckgo.autofill.impl.ui.credential.management.searching

import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock

class ManagementScreenCredentialListFilterTest {

    private val matcher: AutofillCredentialMatcher = mock()
    private val testee = ManagementScreenCredentialListFilter(matcher)

    @Test
    fun `filter - empty list and query - returns empty list`() = runTest {
        assertTrue(testee.filter(emptyList(), "").isEmpty())
    }

    @Test
    fun `filter - non empty list and empty query - unfiltered list returned`() = runTest {
        val originalList = listOf(
            creds(),
            creds(),
        )
        val results = testee.filter(originalList, "")
        assertEquals(2, results.size)
    }

    @Test
    fun `filter - empty list - empty list returned`() = runTest {
        assertTrue(testee.filter(emptyList(), "foo").isEmpty())
    }

    private fun creds(id: Long = 0): LoginCredentials {
        return LoginCredentials(id = id, domain = "example.com", username = "u", password = "p")
    }
}
