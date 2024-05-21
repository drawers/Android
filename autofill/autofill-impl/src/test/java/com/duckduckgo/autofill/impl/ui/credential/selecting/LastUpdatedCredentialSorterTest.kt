package com.duckduckgo.autofill.impl.ui.credential.selecting

import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import org.junit.Assert.assertEquals
import org.junit.Test

class LastUpdatedCredentialSorterTest {

    private val testee = LastUpdatedCredentialSorter()

    @Test
    fun `compare - timestamps equal - 0 returned`() {
        val login1 = aLogin(lastUpdatedTimestamp = 1)
        val login2 = aLogin(lastUpdatedTimestamp = 1)
        val result = testee.compare(login1, login2)
        assertEquals(0, result)
    }

    @Test
    fun `compare - timestamps both null - 0 returned`() {
        val login1 = aLogin(lastUpdatedTimestamp = null)
        val login2 = aLogin(lastUpdatedTimestamp = null)
        val result = testee.compare(login1, login2)
        assertEquals(0, result)
    }

    @Test
    fun `compare - login1 timestamp lower - sorted before other login`() {
        val login1 = aLogin(lastUpdatedTimestamp = 1)
        val login2 = aLogin(lastUpdatedTimestamp = 2)
        val result = testee.compare(login1, login2)
        assertEquals(-1, result)
    }

    @Test
    fun `compare - login missing timestamp - sorted before other login`() {
        val login1 = aLogin(lastUpdatedTimestamp = null)
        val login2 = aLogin(lastUpdatedTimestamp = 1)
        val result = testee.compare(login1, login2)
        assertEquals(-1, result)
    }

    @Test
    fun `compare - login timestamp lower - sorted before other login`() {
        val login1 = aLogin(lastUpdatedTimestamp = 2)
        val login2 = aLogin(lastUpdatedTimestamp = 1)
        val result = testee.compare(login1, login2)
        assertEquals(1, result)
    }

    @Test
    fun whenLogin2IsMissingATimestampThenSortedBeforeOtherLogin() {
        val login1 = aLogin(lastUpdatedTimestamp = 1)
        val login2 = aLogin(lastUpdatedTimestamp = null)
        val result = testee.compare(login1, login2)
        assertEquals(1, result)
    }

    @Test
    fun `compare - login 1 is null - sorted before other login`() {
        val login2 = aLogin(lastUpdatedTimestamp = null)
        val result = testee.compare(null, login2)
        assertEquals(-1, result)
    }

    @Test
    fun `compare - login 2 is null - sorted before other login`() {
        val login1 = aLogin(lastUpdatedTimestamp = null)
        val result = testee.compare(login1, null)
        assertEquals(1, result)
    }

    @Test
    fun `compare - both logins null - equals`() {
        assertEquals(0, testee.compare(null, null))
    }

    private fun aLogin(lastUpdatedTimestamp: Long?): LoginCredentials {
        return LoginCredentials(domain = "example.com", username = "user", password = "pass", lastUpdatedMillis = lastUpdatedTimestamp)
    }
}
