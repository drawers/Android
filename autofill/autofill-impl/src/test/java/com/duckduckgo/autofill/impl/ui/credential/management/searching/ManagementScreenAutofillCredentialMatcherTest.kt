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

class ManagementScreenAutofillCredentialMatcherTest {

    private val testee = ManagementScreenAutofillCredentialMatcher()

    @Test
    fun `matches - empty query - matches credential`() = runTest {
        assertTrue(testee.matches(creds(), ""))
    }

    @Test
    fun `matches - query matches username - matches credential`() {
        val creds = creds(username = "username")
        assertTrue(testee.matches(creds, "username"))
        assertTrue(EXPECT_PREFIXES, testee.matches(creds, "user"))
        assertTrue(EXPECT_IN_THE_MIDDLE, testee.matches(creds, "erna"))
        assertTrue(EXPECT_SUFFIXES, testee.matches(creds, "name"))
        assertTrue(EXPECT_CASE_INSENSITIVE, testee.matches(creds, "USERNAME"))
    }

    @Test
    fun `matches - query matches password - not a match`() {
        val creds = creds(password = "password")
        assertFalse(testee.matches(creds, "password"))
    }

    @Test
    fun `matches - query matches title - matches credential`() {
        val creds = creds(title = "title")
        assertTrue(testee.matches(creds, "title"))
        assertTrue(EXPECT_PREFIXES, testee.matches(creds, "ti"))
        assertTrue(EXPECT_IN_THE_MIDDLE, testee.matches(creds, "itl"))
        assertTrue(EXPECT_SUFFIXES, testee.matches(creds, "le"))
        assertTrue(EXPECT_CASE_INSENSITIVE, testee.matches(creds, "TITLE"))
    }

    @Test
    fun `matches - query matches notes - matches credential`() {
        val creds = creds(notes = "notes")
        assertTrue(testee.matches(creds, "notes"))
        assertTrue(EXPECT_PREFIXES, testee.matches(creds, "no"))
        assertTrue(EXPECT_IN_THE_MIDDLE, testee.matches(creds, "ote"))
        assertTrue(EXPECT_SUFFIXES, testee.matches(creds, "es"))
        assertTrue(EXPECT_CASE_INSENSITIVE, testee.matches(creds, "NOTES"))
    }

    @Test
    fun `matches - query matches domain - credential matches`() {
        val creds = creds(domain = "example.com")
        assertTrue(testee.matches(creds, "example.com"))
        assertTrue(EXPECT_PREFIXES, testee.matches(creds, "exa"))
        assertTrue(EXPECT_IN_THE_MIDDLE, testee.matches(creds, "ample.c"))
        assertTrue(EXPECT_SUFFIXES, testee.matches(creds, ".com"))
        assertTrue(EXPECT_CASE_INSENSITIVE, testee.matches(creds, "EXAMPLE.com"))
    }

    @Test
    fun `matches - query matches multiple fields - matches credential`() {
        val creds = creds(domain = "example.com", username = "example", title = "example", notes = "example")
        assertTrue(testee.matches(creds, "example"))
    }

    private fun creds(
        id: Long = 0,
        username: String? = null,
        password: String? = null,
        domain: String? = null,
        title: String? = null,
        notes: String? = null,
    ): LoginCredentials {
        return LoginCredentials(
            id = id,
            domain = domain,
            username = username,
            password = password,
            domainTitle = title,
            notes = notes,
        )
    }

    private companion object {
        private const val EXPECT_PREFIXES = "Should partially match prefixes"
        private const val EXPECT_IN_THE_MIDDLE = "Should partially match in the middle"
        private const val EXPECT_SUFFIXES = "Should partially match suffixes"
        private const val EXPECT_CASE_INSENSITIVE = "Should case insensitive match"
    }
}
