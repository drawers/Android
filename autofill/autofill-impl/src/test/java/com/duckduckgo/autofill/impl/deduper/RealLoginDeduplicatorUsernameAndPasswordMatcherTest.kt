package com.duckduckgo.autofill.impl.deduper

import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import org.junit.Assert.*
import org.junit.Test

class RealLoginDeduplicatorUsernameAndPasswordMatcherTest {
    private val testee = RealAutofillDeduplicationUsernameAndPasswordMatcher()

    @Test
    fun `groupDuplicateCredentials - empty list in - empty list out`() {
        val input = emptyList<LoginCredentials>()
        val output = testee.groupDuplicateCredentials(input)
        assertTrue(output.isEmpty())
    }

    @Test
    fun `groupDuplicateCredentials - single entry - single entry out`() {
        val input = listOf(
            creds("username", "password"),
        )
        val output = testee.groupDuplicateCredentials(input)
        assertEquals(1, output.size)
    }

    @Test
    fun `groupDuplicateCredentials - no duplication - number of groups matches number of entries`() {
        val input = listOf(
            creds("username_a", "password_x"),
            creds("username_b", "password_y"),
            creds("username_c", "password_z"),
        )
        val output = testee.groupDuplicateCredentials(input)
        assertEquals(3, output.size)
    }

    @Test
    fun `groupDuplicateCredentials - entries match on username but not password - not grouped`() {
        val input = listOf(
            creds("username", "password_x"),
            creds("username", "password_y"),
        )
        val output = testee.groupDuplicateCredentials(input)
        assertEquals(2, output.size)
    }

    @Test
    fun `groupDuplicateCredentials - entries match on password but not username - not grouped`() {
        val input = listOf(
            creds("username_a", "password"),
            creds("username_b", "password"),
        )
        val output = testee.groupDuplicateCredentials(input)
        assertEquals(2, output.size)
    }

    @Test
    fun `groupDuplicateCredentials - entries match on username and password - grouped`() {
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
