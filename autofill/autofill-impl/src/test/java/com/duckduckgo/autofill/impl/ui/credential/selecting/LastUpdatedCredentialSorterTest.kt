package com.duckduckgo.autofill.impl.ui.credential.selecting

import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import org.junit.Assert.assertEquals
import org.junit.Test

class LastUpdatedCredentialSorterTest {

    private val testee = LastUpdatedCredentialSorter()

    @Test
    fun `whenTimestampsAreEqualThen0Returned - equal timestamps - 0 returned`() {
        val login1 = aLogin(lastUpdatedTimestamp = 1)
        val login2 = aLogin(lastUpdatedTimestamp = 1)
        val result = testee.compare(login1, login2)
        assertEquals(0, result)
    }

    @Test
    fun `whenTimestampsAreBothNullThen0Returned - null timestamps - 0 returned`() {
        val login1 = aLogin(lastUpdatedTimestamp = null)
        val login2 = aLogin(lastUpdatedTimestamp = null)
        val result = testee.compare(login1, login2)
        assertEquals(0, result)
    }

    @Test
    fun `whenLogin1TimestampIsLowerThenSortedBeforeOtherLogin - sorted before other login`() {
        val login1 = aLogin(lastUpdatedTimestamp = 1)
        val login2 = aLogin(lastUpdatedTimestamp = 2)
        val result = testee.compare(login1, login2)
        assertEquals(-1, result)
    }

    @Test
    fun `whenLogin1IsMissingATimestampThenSortedBeforeOtherLogin - sorted before other login - null timestamp`() {
        val login1 = aLogin(lastUpdatedTimestamp = null)
        val login2 = aLogin(lastUpdatedTimestamp = 1)
        val result = testee.compare(login1, login2)
        assertEquals(-1, result)
    }

    @Test
    fun `whenLogin2TimestampIsLowerThenSortedBeforeOtherLogin - sorted before other login`() {
        val login1 = aLogin(lastUpdatedTimestamp = 2)
        val login2 = aLogin(lastUpdatedTimestamp = 1)
        val result = testee.compare(login1, login2)
        assertEquals(1, result)
    }

    @Test
    fun `whenLogin2IsMissingATimestampThenSortedBeforeOtherLogin - sorted before other login - timestamp missing`() {
        val login1 = aLogin(lastUpdatedTimestamp = 1)
        val login2 = aLogin(lastUpdatedTimestamp = null)
        val result = testee.compare(login1, login2)
        assertEquals(1, result)
    }

    @Test
    fun `whenLogin1IsNullThenSortedBeforeOtherLogin - sorted before other login`() {
        val login2 = aLogin(lastUpdatedTimestamp = null)
        val result = testee.compare(null, login2)
        assertEquals(-1, result)
    }

    @Test
    fun `whenLogin2IsNullThenSortedBeforeOtherLogin - sorted before other login`() {
        val login1 = aLogin(lastUpdatedTimestamp = null)
        val result = testee.compare(login1, null)
        assertEquals(1, result)
    }

    @Test
    fun `whenBothLoginsAreNullThenTreatedAsEquals - treated as equals`() {
        assertEquals(0, testee.compare(null, null))
    }

    private fun aLogin(lastUpdatedTimestamp: Long?): LoginCredentials {
        return LoginCredentials(domain = "example.com", username = "user", password = "pass", lastUpdatedMillis = lastUpdatedTimestamp)
    }
}
