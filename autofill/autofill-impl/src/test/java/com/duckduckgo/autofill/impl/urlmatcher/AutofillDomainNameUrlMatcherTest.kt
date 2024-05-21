/*
 * Copyright (c) 2022 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.autofill.impl.urlmatcher

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.autofill.impl.encoding.TestUrlUnicodeNormalizer
import com.duckduckgo.autofill.impl.urlmatcher.AutofillUrlMatcher.ExtractedUrlParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutofillDomainNameUrlMatcherTest {

    private val testee = AutofillDomainNameUrlMatcher(TestUrlUnicodeNormalizer())

    @Test
    fun `extractUrlPartsForAutofill - basic domain - same returned for eTLD plus 1`() {
        val inputUrl = "duckduckgo.com"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertEquals(inputUrl, result.eTldPlus1)
    }

    @Test
    fun `extractUrlPartsForAutofill - url is invalid missing tld - etld plus one null`() {
        val inputUrl = "duckduckgo"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertNull(result.eTldPlus1)
    }

    @Test
    fun `extractUrlPartsForAutofill - url is invalid missing tld - subdomain is null`() {
        val inputUrl = "duckduckgo"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertNull(result.subdomain)
    }

    @Test
    fun `extractUrlPartsForAutofill - contains http scheme - etld plus 1 returned`() {
        val inputUrl = "http://duckduckgo.com"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertEquals("duckduckgo.com", result.eTldPlus1)
    }

    @Test
    fun `extractUrlPartsForAutofill - contains https scheme - eTldPlus1 returned`() {
        val inputUrl = "https://duckduckgo.com"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertEquals("duckduckgo.com", result.eTldPlus1)
    }

    @Test
    fun `extractUrlPartsForAutofill - contains www subdomain - etld plus 1 returned`() {
        val inputUrl = "www.duckduckgo.com"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertEquals("duckduckgo.com", result.eTldPlus1)
    }

    @Test
    fun `extractUrlPartsForAutofill - contains another subdomain - eTLD plus 1 returned`() {
        val inputUrl = "foo.duckduckgo.com"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertEquals("duckduckgo.com", result.eTldPlus1)
    }

    @Test
    fun `extractUrlPartsForAutofill - no subdomain - etld plus 1 returned`() {
        val inputUrl = "duckduckgo.co.uk"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertEquals("duckduckgo.co.uk", result.eTldPlus1)
    }

    @Test
    fun `extractUrlPartsForAutofill - two part tld with subdomain - etld plus 1 returned`() {
        val inputUrl = "www.duckduckgo.co.uk"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertEquals("duckduckgo.co.uk", result.eTldPlus1)
    }

    @Test
    fun `extractUrlPartsForAutofill - subdomain www - correctly identified as subdomain`() {
        val inputUrl = "www.duckduckgo.co.uk"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertEquals("www", result.subdomain)
    }

    @Test
    fun `extractUrlPartsForAutofill - subdomain present but not www - correctly identified as subdomain`() {
        val inputUrl = "test.duckduckgo.co.uk"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertEquals("test", result.subdomain)
    }

    @Test
    fun `extractUrlPartsForAutofill - subdomain has two levels - correctly identified as subdomain`() {
        val inputUrl = "foo.bar.duckduckgo.co.uk"
        val result = testee.extractUrlPartsForAutofill(inputUrl)
        assertEquals("foo.bar", result.subdomain)
    }

    @Test
    fun `extractUrlPartsForAutofill - matching for autofill`() {
        val savedSite = testee.extractUrlPartsForAutofill("https://example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("https://example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun `extractUrlPartsForAutofill - matching for autofill - true`() {
        val savedSite = testee.extractUrlPartsForAutofill("https://example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("https://EXAMPLE.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun whenUrlsAreIdenticalExceptForUppercaseSavedSiteThenMatchingForAutofill() {
        val savedSite = testee.extractUrlPartsForAutofill("https://EXAMPLE.com")
        val visitedSite = testee.extractUrlPartsForAutofill("https://example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun whenBothUrlsContainSameSubdomainThenMatchingForAutofill() {
        val savedSite = testee.extractUrlPartsForAutofill("test.example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("test.example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun whenBothUrlsContainWwwSubdomainThenMatchingForAutofill() {
        val savedSite = testee.extractUrlPartsForAutofill("www.example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("www.example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun whenSavedSiteContainsSubdomainAndVisitedSiteDoesNotThenMatchingForAutofill() {
        val savedSite = testee.extractUrlPartsForAutofill("foo.example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun whenSavedSiteDoesNotContainSubdomainAndVisitedSiteDoesThenMatchingForAutofill() {
        val savedSite = testee.extractUrlPartsForAutofill("example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("foo.example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun whenUrlsHaveDifferentSubdomainsThenMatchingForAutofill() {
        val savedSite = testee.extractUrlPartsForAutofill("bar.example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("foo.example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun whenSavedSiteContainsWwwSubdomainAndVisitedSiteDoesNotThenMatchingForAutofill() {
        val savedSite = testee.extractUrlPartsForAutofill("www.example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun `matchingForAutofill - saved site matches visited except for port - not matching for autofill`() {
        val savedSite = ExtractedUrlParts(eTldPlus1 = "example.com", userFacingETldPlus1 = "example.com", subdomain = null, port = 8000)
        val visitedSite = ExtractedUrlParts(eTldPlus1 = "example.com", userFacingETldPlus1 = "example.com", subdomain = null, port = 1000)
        assertFalse(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun `matchingForAutofill - saved site matches visited and equal ports - matching for autofill`() {
        val savedSite = ExtractedUrlParts(eTldPlus1 = "example.com", userFacingETldPlus1 = "example.com", subdomain = null, port = 8000)
        val visitedSite = ExtractedUrlParts(eTldPlus1 = "example.com", userFacingETldPlus1 = "example.com", subdomain = null, port = 8000)
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun `matchingForAutofill - saved site missing port - not matching for autofill`() {
        val savedSite = ExtractedUrlParts(eTldPlus1 = "example.com", userFacingETldPlus1 = "example.com", subdomain = null, port = null)
        val visitedSite = ExtractedUrlParts(eTldPlus1 = "example.com", userFacingETldPlus1 = "example.com", subdomain = null, port = 8000)
        assertFalse(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun `matchingForAutofill - saved site matches visited and visited site missing port - not matching for autofill`() {
        val savedSite = ExtractedUrlParts(eTldPlus1 = "example.com", userFacingETldPlus1 = "example.com", subdomain = null, port = 8000)
        val visitedSite = ExtractedUrlParts(eTldPlus1 = "example.com", userFacingETldPlus1 = "example.com", subdomain = null, port = null)
        assertFalse(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun `extractUrlPartsForAutofill - site does not match - matching for autofill`() {
        val savedSite = testee.extractUrlPartsForAutofill("WWW.example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun whenSavedSiteDoesNotContainSubdomainAndVisitedSiteDoesContainWwwSubdomainThenMatchingForAutofill() {
        val savedSite = testee.extractUrlPartsForAutofill("example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("www.example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun `extractUrlPartsForAutofill - site does not contain subdomain and site contains uppercase www subdomain - matching for autofill`() {
        val savedSite = testee.extractUrlPartsForAutofill("example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("WWW.example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun whenSavedSiteContainNestedSubdomainsAndVisitedSiteContainsMatchingRootSubdomainThenMatchingForAutofill() {
        val savedSite = testee.extractUrlPartsForAutofill("a.b.example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("b.example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun whenSavedSiteContainSubdomainAndVisitedSiteContainsNestedSubdomainsThenMatchingForAutofill() {
        val savedSite = testee.extractUrlPartsForAutofill("b.example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("a.b.example.com")
        assertTrue(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun `extractUrlPartsForAutofill - no matching for autofill`() {
        val savedSite = testee.extractUrlPartsForAutofill("example.com")
        val visitedSite = testee.extractUrlPartsForAutofill("example.com.malicious.com")
        assertFalse(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun `extractUrlPartsForAutofill - matching for autofill - no match`() {
        val savedSite = testee.extractUrlPartsForAutofill("example.com.malicious.com")
        val visitedSite = testee.extractUrlPartsForAutofill("example.com")
        assertFalse(testee.matchingForAutofill(visitedSite, savedSite))
    }

    @Test
    fun `cleanRawUrl - return only scheme and domain`() {
        assertEquals("www.foo.com", testee.cleanRawUrl("https://www.foo.com/path/to/foo?key=value"))
        assertEquals("www.fuu.foo.com", testee.cleanRawUrl("https://www.fuu.foo.com/path/to/foo?key=value"))
        assertEquals("foo.com", testee.cleanRawUrl("http://foo.com/path/to/foo?key=value"))
        assertEquals("fuu.foo.com", testee.cleanRawUrl("http://fuu.foo.com/path/to/foo?key=value"))
        assertEquals("foo.com:9000", testee.cleanRawUrl("http://foo.com:9000/path/to/foo?key=value"))
        assertEquals("fuu.foo.com:9000", testee.cleanRawUrl("http://fuu.foo.com:9000/path/to/foo?key=value"))
        assertEquals("faa.fuu.foo.com:9000", testee.cleanRawUrl("http://faa.fuu.foo.com:9000/path/to/foo?key=value"))
        assertEquals("foo.com", testee.cleanRawUrl("foo.com/path/to/foo"))
        assertEquals("www.foo.com", testee.cleanRawUrl("www.foo.com/path/to/foo"))
        assertEquals("foo.com", testee.cleanRawUrl("foo.com"))
        assertEquals("foo.com:9000", testee.cleanRawUrl("foo.com:9000"))
        assertEquals("fuu.foo.com", testee.cleanRawUrl("fuu.foo.com"))
        assertEquals("192.168.0.1", testee.cleanRawUrl("192.168.0.1"))
        assertEquals("192.168.0.1:9000", testee.cleanRawUrl("192.168.0.1:9000"))
        assertEquals("192.168.0.1", testee.cleanRawUrl("http://192.168.0.1"))
        assertEquals("192.168.0.1:9000", testee.cleanRawUrl("http://192.168.0.1:9000"))
        assertEquals("fuu.foo.com:9000", testee.cleanRawUrl("fuu.foo.com:9000"))
        assertEquals("RandomText", testee.cleanRawUrl("thisIs@RandomText"))
    }

    @Test
    fun `extractUrlPartsForAutofill - domain contains valid non ascii characters - eTLD plus one punycode encoded`() {
        assertEquals("xn--a-5fa.com", testee.extractUrlPartsForAutofill("ça.com").eTldPlus1)
        assertEquals("xn--a-5fa.com", testee.extractUrlPartsForAutofill("https://ça.com").eTldPlus1)
    }
}
