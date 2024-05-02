package com.duckduckgo.autofill.impl.deduper

import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import org.junit.Assert.*
import org.junit.Test

class RealLoginSorterForDeduplicationTest {

    private val testee = AutofillDeduplicationLoginComparator()

    @Test
    fun `compare - first login newer - return negative`() {
        val login1 = creds(lastUpdated = 2000, domain = null)
        val login2 = creds(lastUpdated = 1000, domain = null)
        assertTrue(testee.compare(login1, login2) < 0)
    }

    @Test
    fun `compare - second login newer - return positive`() {
        val login1 = creds(lastUpdated = 1000, domain = null)
        val login2 = creds(lastUpdated = 2000, domain = null)
        assertTrue(testee.compare(login1, login2) > 0)
    }

    @Test
    fun `compare - first login has no last modified timestamp - returns negative`() {
        val login1 = creds(lastUpdated = null, domain = null)
        val login2 = creds(lastUpdated = 2000, domain = null)
        assertTrue(testee.compare(login1, login2) < 0)
    }

    @Test
    fun `compare - second login has no last modified timestamp - returns positive`() {
        val login1 = creds(lastUpdated = 1000, domain = null)
        val login2 = creds(lastUpdated = null, domain = null)
        assertTrue(testee.compare(login1, login2) > 0)
    }

    @Test
    fun `compare - last modified times equal and first login domain should be sorted first then returns negative`() {
        val login1 = creds(lastUpdated = 1000, domain = "example.com")
        val login2 = creds(lastUpdated = 1000, domain = "site.com")
        assertTrue(testee.compare(login1, login2) < 0)
    }

    @Test
    fun `compare - last modified times equal and second login domain should be sorted first - returns negative`() {
        val login1 = creds(lastUpdated = 1000, domain = "site.com")
        val login2 = creds(lastUpdated = 1000, domain = "example.com")
        assertTrue(testee.compare(login1, login2) > 0)
    }

    @Test
    fun `compare - last modified times equal and domains equal - returns 0`() {
        val login1 = creds(lastUpdated = 1000, domain = "example.com")
        val login2 = creds(lastUpdated = 1000, domain = "example.com")
        assertEquals(0, testee.compare(login1, login2))
    }

    @Test
    fun `compare - last modified dates missing and domain missing - returns 0`() {
        val login1 = creds(lastUpdated = null, domain = null)
        val login2 = creds(lastUpdated = null, domain = null)
        assertEquals(0, testee.compare(login1, login2))
    }

    @Test
    fun `compare - logins same last updated time - return 0`() {
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
