package com.duckduckgo.autofill.impl.deduper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.impl.encoding.TestUrlUnicodeNormalizer
import com.duckduckgo.autofill.impl.urlmatcher.AutofillDomainNameUrlMatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealAutofillLoginDeduplicatorTest {
    private val urlMatcher = AutofillDomainNameUrlMatcher(TestUrlUnicodeNormalizer())
    private val matchTypeDetector = RealAutofillDeduplicationMatchTypeDetector(urlMatcher)
    private val testee = RealAutofillLoginDeduplicator(
        usernamePasswordMatcher = RealAutofillDeduplicationUsernameAndPasswordMatcher(),
        bestMatchFinder = RealAutofillDeduplicationBestMatchFinder(
            urlMatcher = urlMatcher,
            matchTypeDetector = matchTypeDetector,
        ),
    )

    @Test
    fun `deduplicate - empty list in - empty list out`() = runTest {
        val result = testee.deduplicate("example.com", emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `deduplicate - single entry in - single entry returned`() {
        val inputList = listOf(
            aLogin("domain", "username", "password"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(1, result.size)
    }

    @Test
    fun `deduplicate - entries completely unrelated - no deduplication`() {
        val inputList = listOf(
            aLogin("domain_A", "username_A", "password_A"),
            aLogin("domain_B", "username_B", "password_B"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(2, result.size)
        assertNotNull(result.find { it.domain == "domain_A" })
        assertNotNull(result.find { it.domain == "domain_B" })
    }

    @Test
    fun `deduplicate - entries share username and password but not domain - deduped`() {
        val inputList = listOf(
            aLogin("foo.com", "username", "password"),
            aLogin("bar.com", "username", "password"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(1, result.size)
    }

    @Test
    fun `deduplicate - entries share domain and username but not password - no deduplication`() {
        val inputList = listOf(
            aLogin("example.com", "username", "123"),
            aLogin("example.com", "username", "xyz"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(2, result.size)
        assertNotNull(result.find { it.password == "123" })
        assertNotNull(result.find { it.password == "xyz" })
    }

    @Test
    fun `deduplicate - entries share domain and password but not username - no deduplication`() {
        val inputList = listOf(
            aLogin("example.com", "user_A", "password"),
            aLogin("example.com", "user_B", "password"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(2, result.size)
        assertNotNull(result.find { it.username == "user_A" })
        assertNotNull(result.find { it.username == "user_B" })
    }

    @Test
    fun `deduplicate - multiple credentials with perfect domain matches - deduped`() {
        val inputList = listOf(
            aLogin("example.com", "username", "password"),
            aLogin("example.com", "username", "password"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(1, result.size)
    }

    @Test
    fun `deduplicate - multiple partial domain matches - deduped`() {
        val inputList = listOf(
            aLogin("a.example.com", "username", "password"),
            aLogin("b.example.com", "username", "password"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(1, result.size)
    }

    @Test
    fun `deduplicate - multiple credentials not domain matches - deduped`() {
        val inputList = listOf(
            aLogin("foo.com", "username", "password"),
            aLogin("bar.com", "username", "password"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(1, result.size)
    }

    @Test
    fun `deduplicate - entries share credentials across perfect and partial matches - deduped to perfect match`() {
        val inputList = listOf(
            aLogin("example.com", "username", "password"),
            aLogin("a.example.com", "username", "password"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(1, result.size)
        assertNotNull(result.find { it.domain == "example.com" })
    }

    @Test
    fun `deduplicate - entries share credentials across perfect and non-domain matches - deduped to perfect match`() {
        val inputList = listOf(
            aLogin("example.com", "username", "password"),
            aLogin("bar.com", "username", "password"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(1, result.size)
        assertNotNull(result.find { it.domain == "example.com" })
    }

    @Test
    fun `deduplicate - entries share credentials across partial and non-domain matches - deduped to perfect match`() {
        val inputList = listOf(
            aLogin("a.example.com", "username", "password"),
            aLogin("bar.com", "username", "password"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(1, result.size)
        assertNotNull(result.find { it.domain == "a.example.com" })
    }

    @Test
    fun `deduplicate - entries share credentials across perfect and partial and non-domain matches - deduped to perfect match`() {
        val inputList = listOf(
            aLogin("a.example.com", "username", "password"),
            aLogin("example.com", "username", "password"),
            aLogin("bar.com", "username", "password"),
        )
        val result = testee.deduplicate("example.com", inputList)
        assertEquals(1, result.size)
        assertNotNull(result.find { it.domain == "example.com" })
    }

    private fun aLogin(domain: String, username: String, password: String): LoginCredentials {
        return LoginCredentials(username = username, password = password, domain = domain)
    }
}
