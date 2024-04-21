package com.duckduckgo.autofill.impl.deduper

import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import org.junit.Assert.*
import org.junit.Test

class RealLoginSorterForDeduplicationTest {

    private val testee = AutofillDeduplicationLoginComparator()

    @Test
    fun `whenFirstLoginIsNewerThenReturnNegative - compare first login is newer then return negative`() {
        val login1 = creds(lastUpdated = 2000, domain = null)
        val login2 = creds(lastUpdated = 1000, domain = null)
        assertTrue(testee.compare(login1, login2) < 0)
    }

    @Test
    fun `whenSecondLoginIsNewerThenReturnPositive - compare logins - positive`() {
        val login1 = creds(lastUpdated = 1000, domain = null)
        val login2 = creds(lastUpdated = 2000, domain = null)
        assertTrue(testee.compare(login1, login2) > 0)
    }

    @Test
    fun `whenFirstLoginHasNoLastModifiedTimestampThenReturnsNegative - negative`() {
        val login1 = creds(lastUpdated = null, domain = null)
        val login2 = creds(lastUpdated = 2000, domain = null)
        assertTrue(testee.compare(login1, login2) < 0)
    }

    @Test
    fun `whenSecondLoginHasNoLastModifiedTimestampThenReturnsPositive - no last modified timestamp - positive`() {
        val login1 = creds(lastUpdated = 1000, domain = null)
        val login2 = creds(lastUpdated = null, domain = null)
        assertTrue(testee.compare(login1, login2) > 0)
    }

    @Test
    fun `whenLastModifiedTimesEqualAndFirstLoginDomainShouldBeSortedFirstThenReturnsNegative - last updated equal and first domain sorted first - negative`() {
        val login1 = creds(lastUpdated = 1000, domain = "example.com")
        val login2 = creds(lastUpdated = 1000, domain = "site.com")
        assertTrue(testee.compare(login1, login2) < 0)
    }

    @Test
    fun `whenLastModifiedTimesEqualAndSecondLoginDomainShouldBeSortedFirstThenReturnsNegative - last updated equal and domain sorted first - negative`() {
        val login1 = creds(lastUpdated = 1000, domain = "site.com")
        val login2 = creds(lastUpdated = 1000, domain = "example.com")
        assertTrue(testee.compare(login1, login2) > 0)
    }

    @Test
    fun `whenLastModifiedTimesEqualAndDomainsEqualThenReturns0 - equal last modified times and domains - 0`() {
        val login1 = creds(lastUpdated = 1000, domain = "example.com")
        val login2 = creds(lastUpdated = 1000, domain = "example.com")
        assertEquals(0, testee.compare(login1, login2))
    }

    @Test
    fun `whenLastModifiedDatesMissingAndDomainMissingThenReturns0 - missing last modified dates and domain - 0`() {
        val login1 = creds(lastUpdated = null, domain = null)
        val login2 = creds(lastUpdated = null, domain = null)
        assertEquals(0, testee.compare(login1, login2))
    }

    @Test
    fun `whenLoginsSameLastUpdatedTimeThenReturn0 - same last updated time - 0`() {
        val login1 = creds(lastUpdated = 1000, domain = null)
        val login2 = creds(lastUpdated = 1000, domain = null)
        assertEquals(0, testee.compare(login1, login2))
    }

    private fun creds(
        lastUpdated: Long?,
        domain: String? = "example.com",
    ): LoginCredentials {
        return LoginCredentials(id = 0, lastUpdatedMillis = lastUpdated, domain = domain, username = "username", password = "password")
    }
}
