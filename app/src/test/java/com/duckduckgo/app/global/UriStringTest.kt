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

package com.duckduckgo.app.global

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.browser.UriString.Companion.isWebUrl
import com.duckduckgo.app.browser.UriString.Companion.sameOrSubdomain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UriStringTest {

    @Test
    fun `sameOrSubdomain - same domain - true`() {
        assertTrue(sameOrSubdomain("http://example.com/index.html", "http://example.com/home.html"))
    }

    @Test
    fun whenUrlsHaveSameDomainThenSameOrSubdomainIsTrue2() {
        assertTrue(sameOrSubdomain("http://example.com/index.html".toUri(), "http://example.com/home.html"))
    }

    @Test
    fun `sameOrSubdomain - url is subdomain - true`() {
        assertTrue(sameOrSubdomain("http://subdomain.example.com/index.html", "http://example.com/home.html"))
    }

    @Test
    fun whenUrlIsSubdomainThenSameOrSubdomainIsTrue2() {
        assertTrue(sameOrSubdomain("http://subdomain.example.com/index.html".toUri(), "http://example.com/home.html"))
    }

    @Test
    fun `sameOrSubdomain - url is a parent domain - false`() {
        assertFalse(sameOrSubdomain("http://example.com/index.html", "http://parent.example.com/home.html"))
    }

    @Test
    fun `whenUrlIsAParentDomainThenSameOrSubdomainIsFalse - same or subdomain - false`() {
        assertFalse(sameOrSubdomain("http://example.com/index.html".toUri(), "http://parent.example.com/home.html"))
    }

    @Test
    fun `whenChildUrlIsMalformed - same or subdomain - false`() {
        assertFalse(sameOrSubdomain("??.example.com/index.html", "http://example.com/home.html"))
    }

    @Test
    fun `whenChildUrlIsMalformedThenSameOrSubdomainIsFalse - same or subdomain is false`() {
        assertFalse(sameOrSubdomain("??.example.com/index.html".toUri(), "http://example.com/home.html"))
    }

    @Test
    fun `whenParentUrlIsMalformedThenSameOrSubdomainIsFalse - same or subdomain - false`() {
        assertFalse(sameOrSubdomain("http://example.com/index.html", "??.example.com/home.html"))
    }

    @Test
    fun `whenParentUrlIsMalformedThenSameOrSubdomainIsFalse - same or subdomain is false`() {
        assertFalse(sameOrSubdomain("http://example.com/index.html".toUri(), "??.example.com/home.html"))
    }

    @Test
    fun whenUrlsHaveSameDomainThenSafeSameOrSubdomainIsTrue() {
        assertTrue(sameOrSubdomain("http://example.com/index.html", "http://example.com/home.html"))
    }

    @Test
    fun whenUrlsHaveSameDomainThenSafeSameOrSubdomainIsTrue2() {
        assertTrue(sameOrSubdomain("http://example.com/index.html".toUri(), "http://example.com/home.html"))
    }

    @Test
    fun whenUrlIsSubdomainThenSafeSameOrSubdomainIsTrue() {
        assertTrue(sameOrSubdomain("http://subdomain.example.com/index.html", "http://example.com/home.html"))
    }

    @Test
    fun whenUrlIsSubdomainThenSafeSameOrSubdomainIsTrue2() {
        assertTrue(sameOrSubdomain("http://subdomain.example.com/index.html".toUri(), "http://example.com/home.html"))
    }

    @Test
    fun whenUrlIsAParentDomainThenSafeSameOrSubdomainIsFalse() {
        assertFalse(sameOrSubdomain("http://example.com/index.html", "http://parent.example.com/home.html"))
    }

    @Test
    fun whenUrlIsAParentDomainThenSafeSameOrSubdomainIsFalse2() {
        assertFalse(sameOrSubdomain("http://example.com/index.html".toUri(), "http://parent.example.com/home.html"))
    }

    @Test
    fun `sameOrSubdomain - child url is malformed - false`() {
        assertFalse(sameOrSubdomain("??.example.com/index.html", "http://example.com/home.html"))
    }

    @Test
    fun `whenChildUrlIsMalformedThenSafeSameOrSubdomainIsFalse - same or subdomain - false`() {
        assertFalse(sameOrSubdomain("??.example.com/index.html".toUri(), "http://example.com/home.html"))
    }

    @Test
    fun `whenParentUrlIsMalformedThenSafeSameOrSubdomainIsFalse - same or subdomain - false`() {
        assertFalse(sameOrSubdomain("http://example.com/index.html", "??.example.com/home.html"))
    }

    @Test
    fun `whenParentUrlIsMalformedThenSafeSameOrSubdomainIsFalse - same or subdomain is false`() {
        assertFalse(sameOrSubdomain("http://example.com/index.html".toUri(), "??.example.com/home.html"))
    }

    @Test
    fun `isWebUrl - user is present - is false`() {
        val input = "http://example.com@sample.com"
        assertFalse(isWebUrl(input))
    }

    @Test
    fun `isWebUrl - given long well-formed URL - is true`() {
        val input = "http://www.veganchic.com/products/Camo-High-Top-Sneaker-by-The-Critical-Slide-Societ+80758-0180.html"
        assertTrue(isWebUrl(input))
    }

    @Test
    fun `isWebUrl - host valid - is web url true`() {
        assertTrue(isWebUrl("test.com"))
    }

    @Test
    fun `isWebUrl - host valid ip address - is true`() {
        assertTrue(isWebUrl("121.33.2.11"))
    }

    @Test
    fun `isWebUrl - host valid ip address with port - is true`() {
        assertTrue(isWebUrl("121.33.2.11:999"))
    }

    @Test
    fun `isWebUrl - host is localhost - true`() {
        assertTrue(isWebUrl("localhost"))
    }

    @Test
    fun `isWebUrl - host is invalid contains space - is false`() {
        assertFalse(isWebUrl("t est.com"))
    }

    @Test
    fun `isWebUrl - host is invalid contains exclamation mark - is false`() {
        assertFalse(isWebUrl("test!com.com"))
    }

    @Test
    fun `isWebUrl - host is invalid IP - is false`() {
        assertFalse(isWebUrl("121.33.33."))
    }

    @Test
    fun `isWebUrl - host is invalid misspelled localhost contains space - is false`() {
        assertFalse(isWebUrl("localhostt"))
    }

    @Test
    fun `isWebUrl - scheme is valid normal url - is web url true`() {
        assertTrue(isWebUrl("http://test.com"))
    }

    @Test
    fun `isWebUrl - scheme is valid ip address - true`() {
        assertTrue(isWebUrl("http://121.33.2.11"))
    }

    @Test
    fun `isWebUrl - scheme is valid ip address with port - true`() {
        assertTrue(isWebUrl("http://121.33.2.11:999"))
    }

    @Test
    fun `isWebUrl - scheme is valid localhost url - is true`() {
        assertTrue(isWebUrl("http://localhost"))
    }

    @Test
    fun `isWebUrl - scheme is invalid normal url - is false`() {
        assertFalse(isWebUrl("asdas://test.com"))
    }

    @Test
    fun `isWebUrl - scheme is invalid ip address - is false`() {
        assertFalse(isWebUrl("asdas://121.33.2.11"))
    }

    @Test
    fun `isWebUrl - scheme is invalid localhost - is false`() {
        assertFalse(isWebUrl("asdas://localhost"))
    }

    @Test
    fun `isWebUrl - text is incomplete http scheme letters only - is false`() {
        assertFalse(isWebUrl("http"))
    }

    @Test
    fun `isWebUrl - text is incomplete http scheme missing both slashes - is false`() {
        assertFalse(isWebUrl("http:"))
    }

    @Test
    fun `isWebUrl - text is incomplete http scheme missing one slash - is false`() {
        assertFalse(isWebUrl("http:/"))
    }

    @Test
    fun `isWebUrl - text is incomplete https scheme letters only - is false`() {
        assertFalse(isWebUrl("https"))
    }

    @Test
    fun `isWebUrl - text is incomplete https scheme missing both slashes - is false`() {
        assertFalse(isWebUrl("https:"))
    }

    @Test
    fun `isWebUrl - text is incomplete https scheme missing one slash - is false`() {
        assertFalse(isWebUrl("https:/"))
    }

    @Test
    fun `isWebUrl - path valid normal url - is true`() {
        assertTrue(isWebUrl("http://test.com/path"))
    }

    @Test
    fun `isWebUrl - path is valid ip address - is web url true`() {
        assertTrue(isWebUrl("http://121.33.2.11/path"))
    }

    @Test
    fun `isWebUrl - path is valid ip address with port - is true`() {
        assertTrue(isWebUrl("http://121.33.2.11:999/path"))
    }

    @Test
    fun `isWebUrl - path is valid localhost - is web url true`() {
        assertTrue(isWebUrl("http://localhost/path"))
    }

    @Test
    fun `isWebUrl - path valid missing scheme normal url - is true`() {
        assertTrue(isWebUrl("test.com/path"))
    }

    @Test
    fun `isWebUrl - path valid missing scheme ip address - is true`() {
        assertTrue(isWebUrl("121.33.2.11/path"))
    }

    @Test
    fun `isWebUrl - path valid missing scheme localhost - is true`() {
        assertTrue(isWebUrl("localhost/path"))
    }

    @Test
    fun `isWebUrl - path contains space normal url - is false`() {
        assertFalse(isWebUrl("http://test.com/pa th"))
    }

    @Test
    fun `isWebUrl - path is invalid contains space ip address - is false`() {
        assertFalse(isWebUrl("http://121.33.2.11/pa th"))
    }

    @Test
    fun `isWebUrl - path is invalid contains space localhost - is false`() {
        assertFalse(isWebUrl("http://localhost/pa th"))
    }

    @Test
    fun `isWebUrl - path contains space missing scheme normal url - is false`() {
        assertFalse(isWebUrl("test.com/pa th"))
    }

    @Test
    fun `isWebUrl - path is invalid contains space missing scheme ip address - is false`() {
        assertFalse(isWebUrl("121.33.2.11/pa th"))
    }

    @Test
    fun `isWebUrl - path is invalid contains space missing scheme localhost - is false`() {
        assertFalse(isWebUrl("localhost/pa th"))
    }

    @Test
    fun `isWebUrl - path is valid contains encoded space normal url - is true`() {
        assertTrue(isWebUrl("http://www.example.com/pa%20th"))
    }

    @Test
    fun `isWebUrl - params are valid normal URL - is true`() {
        assertTrue(isWebUrl("http://test.com?s=dafas&d=342"))
    }

    @Test
    fun `isWebUrl - params are valid ip address - is web url true`() {
        assertTrue(isWebUrl("http://121.33.2.11?s=dafas&d=342"))
    }

    @Test
    fun `isWebUrl - params are valid localhost - is web url true`() {
        assertTrue(isWebUrl("http://localhost?s=dafas&d=342"))
    }

    @Test
    fun `isWebUrl - params are valid normal url missing scheme - is true`() {
        assertTrue(isWebUrl("test.com?s=dafas&d=342"))
    }

    @Test
    fun `isWebUrl - params are valid ip address missing scheme - is true`() {
        assertTrue(isWebUrl("121.33.2.11?s=dafas&d=342"))
    }

    @Test
    fun `isWebUrl - params are valid localhost missing scheme - is true`() {
        assertTrue(isWebUrl("localhost?s=dafas&d=342"))
    }

    @Test
    fun `isWebUrl - params are valid contains encoded uri - is true`() {
        assertTrue(isWebUrl("https://m.facebook.com/?refsrc=https%3A%2F%2Fwww.facebook.com%2F&_rdr"))
    }

    @Test
    fun `isWebUrl - given simple string - is false`() {
        assertFalse(isWebUrl("randomtext"))
    }

    @Test
    fun `isWebUrl - given string with dot prefix - is false`() {
        assertFalse(isWebUrl(".randomtext"))
    }

    @Test
    fun `isWebUrl - given string with dot suffix - is false`() {
        assertFalse(isWebUrl("randomtext."))
    }

    @Test
    fun `isWebUrl - given number - is false`() {
        assertFalse(isWebUrl("33"))
    }

    @Test
    fun `isWebUrl - named local machine with scheme and port - is true`() {
        assertTrue(isWebUrl("http://raspberrypi:8080"))
    }

    @Test
    fun `isWebUrl - named local machine with no scheme and port - is false`() {
        assertFalse(isWebUrl("raspberrypi:8080"))
    }

    @Test
    fun `isWebUrl - named local machine with scheme no port - is true`() {
        assertTrue(isWebUrl("http://raspberrypi"))
    }

    @Test
    fun `isWebUrl - starts with site specific search - is false`() {
        assertFalse(isWebUrl("site:example.com"))
    }

    @Test
    fun `isWebUrl - scheme is valid ftp but not http - not`() {
        assertFalse(isWebUrl("ftp://example.com"))
    }
}
