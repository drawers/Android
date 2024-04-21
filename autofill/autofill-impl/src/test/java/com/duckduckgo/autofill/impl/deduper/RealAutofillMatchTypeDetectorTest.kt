package com.duckduckgo.autofill.impl.deduper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.impl.deduper.AutofillDeduplicationMatchTypeDetector.MatchType
import com.duckduckgo.autofill.impl.encoding.TestUrlUnicodeNormalizer
import com.duckduckgo.autofill.impl.urlmatcher.AutofillDomainNameUrlMatcher
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealAutofillMatchTypeDetectorTest {
    private val testee = RealAutofillDeduplicationMatchTypeDetector(AutofillDomainNameUrlMatcher(TestUrlUnicodeNormalizer()))

    @Test
    fun `detectMatchType - exact url match - perfect match`() {
        val result = testee.detectMatchType("example.com", creds("example.com"))
        result.assertIsPerfectMatch()
    }

    @Test
    fun `detectMatchType - subdomain match on saved site - type is partial match`() {
        val result = testee.detectMatchType("example.com", creds("subdomain.example.com"))
        result.assertIsPartialMatch()
    }

    @Test
    fun `detectMatchType - subdomain match on visited site - type is partial match`() {
        val result = testee.detectMatchType("subdomain.example.com", creds("example.com"))
        result.assertIsPartialMatch()
    }

    @Test
    fun `detectMatchType - subdomain match on both visited and saved site - perfect match`() {
        val result = testee.detectMatchType("subdomain.example.com", creds("subdomain.example.com"))
        result.assertIsPerfectMatch()
    }

    @Test
    fun `detectMatchType - no eTld plus one match - not a match`() {
        val result = testee.detectMatchType("foo.com", creds("example.com"))
        result.assertNotAMatch()
    }

    private fun MatchType.assertIsPerfectMatch() = assertTrue(this is MatchType.PerfectMatch)
    private fun MatchType.assertIsPartialMatch() = assertTrue(this is MatchType.PartialMatch)
    private fun MatchType.assertNotAMatch() = assertTrue(this is MatchType.NotAMatch)

    private fun creds(domain: String): LoginCredentials {
        return LoginCredentials(domain = domain, username = "", password = "")
    }
}
