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
    fun whenUrlIsAParentDomainThenSameOrSubdomainIsFalse2() {
        assertFalse(sameOrSubdomain("http://example.com/index.html".toUri(), "http://parent.example.com/home.html"))
    }

    @Test
    fun `sameOrSubdomain - child URL malformed - false`() {
        assertFalse(sameOrSubdomain("??.example.com/index.html", "http://example.com/home.html"))
    }

    @Test
    fun `sameOrSubdomain - malformed child URL - false`() {
        assertFalse(sameOrSubdomain("??.example.com/index.html".toUri(), "http://example.com/home.html"))
    }

    @Test
    fun `sameOrSubdomain - parent URL is malformed - false`() {
        assertFalse(sameOrSubdomain("http://example.com/index.html", "??.example.com/home.html"))
    }

    @Test
    fun `sameOrSubdomain - malformed parent URL - false`() {
        assertFalse(sameOrSubdomain("http://example.com/index.html".toUri(), "??.example.com/home.html"))
    }

    @Test
    fun `sameOrSubdomain - urls have same domain - true`() {
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
    fun `sameOrSubdomain - child URL is malformed - false`() {
        assertFalse(sameOrSubdomain("??.example.com/index.html", "http://example.com/home.html"))
    }

    @Test
    fun whenChildUrlIsMalformedThenSafeSameOrSubdomainIsFalse2() {
        assertFalse(sameOrSubdomain("??.example.com/index.html".toUri(), "http://example.com/home.html"))
    }

    @Test
    fun whenParentUrlIsMalformedThenSafeSameOrSubdomainIsFalse() {
        assertFalse(sameOrSubdomain("http://example.com/index.html", "??.example.com/home.html"))
    }

    @Test
    fun `sameOrSubdomain - parent URL malformed - false`() {
        assertFalse(sameOrSubdomain("http://example.com/index.html".toUri(), "??.example.com/home.html"))
    }

    @Test
    fun `isWebUrl - user is present - false`() {
        val input = "http://example.com@sample.com"
        assertFalse(isWebUrl(input))
    }

    @Test
    fun `isWebUrl - long well-formed URL - true`() {
        val input = "http://www.veganchic.com/products/Camo-High-Top-Sneaker-by-The-Critical-Slide-Societ+80758-0180.html"
        assertTrue(isWebUrl(input))
    }

    @Test
    fun `isWebUrl - valid host - true`() {
        assertTrue(isWebUrl("test.com"))
    }

    @Test
    fun `isWebUrl - valid IP address - true`() {
        assertTrue(isWebUrl("121.33.2.11"))
    }

    @Test
    fun `isWebUrl - valid IP address with port - true`() {
        assertTrue(isWebUrl("121.33.2.11:999"))
    }

    @Test
    fun `isWebUrl - localhost - true`() {
        assertTrue(isWebUrl("localhost"))
    }

    @Test
    fun `isWebUrl - host contains space - false`() {
        assertFalse(isWebUrl("t est.com"))
    }

    @Test
    fun `isWebUrl - invalid host with exclamation mark - false`() {
        assertFalse(isWebUrl("test!com.com"))
    }

    @Test
    fun `isWebUrl - invalid IP - false`() {
        assertFalse(isWebUrl("121.33.33."))
    }

    @Test
    fun `isWebUrl - invalid host - false`() {
        assertFalse(isWebUrl("localhostt"))
    }

    @Test
    fun `isWebUrl - valid normal URL - true`() {
        assertTrue(isWebUrl("http://test.com"))
    }

    @Test
    fun whenSchemeIsValidIpAddressThenIsWebUrlIsTrue() {
        assertTrue(isWebUrl("http://121.33.2.11"))
    }

    @Test
    fun whenSchemeIsValidIpAddressWithPortThenIsWebUrlIsTrue() {
        assertTrue(isWebUrl("http://121.33.2.11:999"))
    }

    @Test
    fun `isWebUrl - valid localhost URL - true`() {
        assertTrue(isWebUrl("http://localhost"))
    }

    @Test
    fun `isWebUrl - invalid normal url - false`() {
        assertFalse(isWebUrl("asdas://test.com"))
    }

    @Test
    fun `isWebUrl - invalid IP address scheme - false`() {
        assertFalse(isWebUrl("asdas://121.33.2.11"))
    }

    @Test
    fun `isWebUrl - invalid localhost scheme - false`() {
        assertFalse(isWebUrl("asdas://localhost"))
    }

    @Test
    fun `isWebUrl - incomplete http scheme letters only - false`() {
        assertFalse(isWebUrl("http"))
    }

    @Test
    fun `isWebUrl - incomplete http scheme missing both slashes - false`() {
        assertFalse(isWebUrl("http:"))
    }

    @Test
    fun `isWebUrl - incomplete HTTP scheme missing one slash - false`() {
        assertFalse(isWebUrl("http:/"))
    }

    @Test
    fun `isWebUrl - incomplete https scheme letters only - false`() {
        assertFalse(isWebUrl("https"))
    }

    @Test
    fun `isWebUrl - incomplete https scheme missing slashes - false`() {
        assertFalse(isWebUrl("https:"))
    }

    @Test
    fun `isWebUrl - incomplete https scheme missing one slash - false`() {
        assertFalse(isWebUrl("https:/"))
    }

    @Test
    fun whenPathIsValidNormalUrlThenIsWebUrlIsTrue() {
        assertTrue(isWebUrl("http://test.com/path"))
    }

    @Test
    fun whenPathIsValidIpAddressThenIsWebUrlIsTrue() {
        assertTrue(isWebUrl("http://121.33.2.11/path"))
    }

    @Test
    fun whenPathIsValidIpAddressWithPortThenIsWebUrlIsTrue() {
        assertTrue(isWebUrl("http://121.33.2.11:999/path"))
    }

    @Test
    fun `isWebUrl - valid localhost path - true`() {
        assertTrue(isWebUrl("http://localhost/path"))
    }

    @Test
    fun `isWebUrl - valid missing scheme normal URL - true`() {
        assertTrue(isWebUrl("test.com/path"))
    }

    @Test
    fun `isWebUrl - valid missing scheme IP address - true`() {
        assertTrue(isWebUrl("121.33.2.11/path"))
    }

    @Test
    fun `isWebUrl - valid missing scheme localhost - true`() {
        assertTrue(isWebUrl("localhost/path"))
    }

    @Test
    fun `isWebUrl - invalid path with space - false`() {
        assertFalse(isWebUrl("http://test.com/pa th"))
    }

    @Test
    fun `isWebUrl - invalid path with space in IP address - false`() {
        assertFalse(isWebUrl("http://121.33.2.11/pa th"))
    }

    @Test
    fun `isWebUrl - invalid path with space in localhost - false`() {
        assertFalse(isWebUrl("http://localhost/pa th"))
    }

    @Test
    fun `isWebUrl - invalid path with space and missing scheme - false`() {
        assertFalse(isWebUrl("test.com/pa th"))
    }

    @Test
    fun whenPathIsInvalidContainsSpaceMissingSchemeIpAddressThenIsWebUrlIsFalse() {
        assertFalse(isWebUrl("121.33.2.11/pa th"))
    }

    @Test
    fun whenPathIsInvalidContainsSpaceMissingSchemeLocalhostThenIsWebUrlIsFalse() {
        assertFalse(isWebUrl("localhost/pa th"))
    }

    @Test
    fun `isWebUrl - valid URL with encoded space - true`() {
        assertTrue(isWebUrl("http://www.example.com/pa%20th"))
    }

    @Test
    fun whenParamsAreValidNormalUrlThenIsWebUrlIsTrue() {
        assertTrue(isWebUrl("http://test.com?s=dafas&d=342"))
    }

    @Test
    fun whenParamsAreValidIpAddressThenIsWebUrlIsTrue() {
        assertTrue(isWebUrl("http://121.33.2.11?s=dafas&d=342"))
    }

    @Test
    fun `isWebUrl - valid localhost params - true`() {
        assertTrue(isWebUrl("http://localhost?s=dafas&d=342"))
    }

    @Test
    fun `isWebUrl - valid normal URL missing scheme - true`() {
        assertTrue(isWebUrl("test.com?s=dafas&d=342"))
    }

    @Test
    fun `isWebUrl - valid IP address missing scheme - true`() {
        assertTrue(isWebUrl("121.33.2.11?s=dafas&d=342"))
    }

    @Test
    fun `isWebUrl - valid localhost missing scheme - true`() {
        assertTrue(isWebUrl("localhost?s=dafas&d=342"))
    }

    @Test
    fun `isWebUrl - valid params with encoded URI - true`() {
        assertTrue(isWebUrl("https://m.facebook.com/?refsrc=https%3A%2F%2Fwww.facebook.com%2F&_rdr"))
    }

    @Test
    fun `isWebUrl - simple string - false`() {
        assertFalse(isWebUrl("randomtext"))
    }

    @Test
    fun `isWebUrl - string with dot prefix - false`() {
        assertFalse(isWebUrl(".randomtext"))
    }

    @Test
    fun `isWebUrl - string with dot suffix - false`() {
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
    fun `isWebUrl - site specific search - is false`() {
        assertFalse(isWebUrl("site:example.com"))
    }

    @Test
    fun `isWebUrl - valid FTP scheme - not HTTP`() {
        assertFalse(isWebUrl("ftp://example.com"))
    }
}
