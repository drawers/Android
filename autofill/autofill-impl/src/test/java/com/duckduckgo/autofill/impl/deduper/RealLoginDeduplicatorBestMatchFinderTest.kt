package com.duckduckgo.autofill.impl.deduper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.impl.encoding.TestUrlUnicodeNormalizer
import com.duckduckgo.autofill.impl.urlmatcher.AutofillDomainNameUrlMatcher
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealLoginDeduplicatorBestMatchFinderTest {

    private val urlMatcher = AutofillDomainNameUrlMatcher(TestUrlUnicodeNormalizer())
    private val matchTypeDetector = RealAutofillDeduplicationMatchTypeDetector(urlMatcher)
    private val testee = RealAutofillDeduplicationBestMatchFinder(
        urlMatcher = urlMatcher,
        matchTypeDetector = matchTypeDetector,
    )

    @Test
    fun `whenEmptyListThenNoBestMatchFound - no best match found`() {
        assertNull(testee.findBestMatch("", emptyList()))
    }

    @Test
    fun `whenSinglePerfectMatchThenThatIsReturnedAsBestMatch - single perfect match - that is returned as best match`() {
        val input = listOf(
            LoginCredentials(id = 0, domain = "example.com", username = "username", password = "password"),
        )
        val result = testee.findBestMatch("example.com", input)
        assertNotNull(result)
    }

    @Test
    fun `whenMultiplePerfectMatchesMostRecentlyModifiedIsReturned - multiple perfect matches - most recently modified is returned`() {
        val input = listOf(
            creds("example.com", 1000),
            creds("example.com", 2000),
        )
        val result = testee.findBestMatch("example.com", input)
        assertEquals(2000L, result!!.lastUpdatedMillis)
    }

    @Test
    fun `whenMultiplePartialMatchesWithSameTimestampThenDomainAlphabeticallyFirstReturned - same timestamp - domain alphabetically first returned`() {
        val input = listOf(
            creds("a.example.com", 2000),
            creds("b.example.com", 2000),
        )
        val result = testee.findBestMatch("example.com", input)
        assertEquals("a.example.com", result!!.domain)
    }

    @Test
    fun `whenSingleNonMatchThenReturnedAsBestMatch - single non match - returned as best match`() {
        val input = listOf(
            creds("not-a-match.com", 2000),
        )
        val result = testee.findBestMatch("example.com", input)
        assertEquals("not-a-match.com", result!!.domain)
    }

    @Test
    fun `whenMultipleNonMatchesThenMostRecentlyModifiedIsReturned - most recently modified is returned`() {
        val input = listOf(
            creds("not-a-match.com", 2000),
            creds("also-not-a-match.com", 1000),
        )
        val result = testee.findBestMatch("example.com", input)
        assertEquals("not-a-match.com", result!!.domain)
    }

    @Test
    fun `whenMatchesFromAllTypesThenMatchInPerfectReturnedRegardlessOfTimestamps - perfect match regardless of timestamps`() {
        val input = listOf(
            creds("perfect-match.com", 1000),
            creds("imperfect-match.com", 3000),
            creds("not-a-match.com", 2000),
        )
        val result = testee.findBestMatch("perfect-match.com", input)
        assertEquals("perfect-match.com", result!!.domain)
    }

    private fun creds(domain: String, lastModified: Long?): LoginCredentials {
        return LoginCredentials(domain = domain, lastUpdatedMillis = lastModified, username = "", password = "")
    }
}
