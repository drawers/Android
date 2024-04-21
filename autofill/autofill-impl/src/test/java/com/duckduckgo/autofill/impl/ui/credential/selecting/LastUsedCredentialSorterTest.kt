package com.duckduckgo.autofill.impl.ui.credential.selecting

import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import org.junit.Assert.assertEquals
import org.junit.Test

class LastUsedCredentialSorterTest {

    private val testee = LastUsedCredentialSorter()

    @Test
    fun `compare - timestamps are equal - 0 returned`() {
        val login1 = aLogin(lastUsedTimestamp = 1)
        val login2 = aLogin(lastUsedTimestamp = 1)
        val result = testee.compare(login1, login2)
        assertEquals(0, result)
    }

    @Test
    fun `compare - timestamps are both null - 0 returned`() {
        val login1 = aLogin(lastUsedTimestamp = null)
        val login2 = aLogin(lastUsedTimestamp = null)
        val result = testee.compare(login1, login2)
        assertEquals(0, result)
    }

    @Test
    fun `compare - login1 timestamp is lower - sorted before other login`() {
        val login1 = aLogin(lastUsedTimestamp = 1)
        val login2 = aLogin(lastUsedTimestamp = 2)
        val result = testee.compare(login1, login2)
        assertEquals(-1, result)
    }

    @Test
    fun `compare - login1 missing a timestamp - sorted before other login`() {
        val login1 = aLogin(lastUsedTimestamp = null)
        val login2 = aLogin(lastUsedTimestamp = 1)
        val result = testee.compare(login1, login2)
        assertEquals(-1, result)
    }

    @Test
    fun `compare - login2 timestamp is lower - sorted before other login`() {
        val login1 = aLogin(lastUsedTimestamp = 2)
        val login2 = aLogin(lastUsedTimestamp = 1)
        val result = testee.compare(login1, login2)
        assertEquals(1, result)
    }

    @Test
    fun `compare - login2 missing a timestamp - sorted before other login`() {
        val login1 = aLogin(lastUsedTimestamp = 1)
        val login2 = aLogin(lastUsedTimestamp = null)
        val result = testee.compare(login1, login2)
        assertEquals(1, result)
    }

    @Test
    fun `compare - login1 is null - sorted before other login`() {
        val login2 = aLogin(lastUsedTimestamp = null)
        val result = testee.compare(null, login2)
        assertEquals(-1, result)
    }

    @Test
    fun `compare - login2 is null - sorted before other login`() {
        val login1 = aLogin(lastUsedTimestamp = null)
        val result = testee.compare(login1, null)
        assertEquals(1, result)
    }

    @Test
    fun `compare - both logins are null - treated as equals`() {
        assertEquals(0, testee.compare(null, null))
    }

    private fun aLogin(lastUsedTimestamp: Long?): LoginCredentials {
        return LoginCredentials(domain = "example.com", username = "user", password = "pass", lastUsedMillis = lastUsedTimestamp)
    }
}
