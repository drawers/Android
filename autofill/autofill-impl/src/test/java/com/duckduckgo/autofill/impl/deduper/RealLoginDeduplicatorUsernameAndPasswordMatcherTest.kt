package com.duckduckgo.autofill.impl.deduper

import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import org.junit.Assert.*
import org.junit.Test

class RealLoginDeduplicatorUsernameAndPasswordMatcherTest {
    private val testee = RealAutofillDeduplicationUsernameAndPasswordMatcher()

    @Test
    fun `whenEmptyListInThenEmptyListOut - empty list in - empty list out`() {
        val input = emptyList<LoginCredentials>()
        val output = testee.groupDuplicateCredentials(input)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `whenSingleEntryInThenSingleEntryOut - single entry in - single entry out`() {
        val input = listOf(
            creds("username", "password"),
        )
        val output = testee.groupDuplicateCredentials(input)
        assertEquals(1, output.size)
    }

    @Test
    fun `whenMultipleEntriesWithNoDuplicationAtAllThenNumberOfGroupsReturnedMatchesNumberOfEntriesInputted - no duplication - 3 groups`() {
        val input = listOf(
            creds("username_a", "password_x"),
            creds("username_b", "password_y"),
            creds("username_c", "password_z"),
        )
        val output = testee.groupDuplicateCredentials(input)
        assertEquals(3, output.size)
    }

    @Test
    fun `whenEntriesMatchOnUsernameButNotPasswordThenNotGrouped - not grouped`() {
        val input = listOf(
            creds("username", "password_x"),
            creds("username", "password_y"),
        )
        val output = testee.groupDuplicateCredentials(input)
        assertEquals(2, output.size)
    }

    @Test
    fun `whenEntriesMatchOnPasswordButNotUsernameThenNotGrouped - not grouped`() {
        val input = listOf(
            creds("username_a", "password"),
            creds("username_b", "password"),
        )
        val output = testee.groupDuplicateCredentials(input)
        assertEquals(2, output.size)
    }

    @Test
    fun `whenEntriesMatchOnUsernameAndPasswordThenGrouped - grouped`() {
        val input = listOf(
            creds("username", "password"),
            creds("username", "password"),
        )
        val output = testee.groupDuplicateCredentials(input)
        assertEquals(1, output.size)
    }

    private fun creds(username: String, password: String): LoginCredentials {
        return LoginCredentials(username = username, password = password, domain = "domain")
    }
}
