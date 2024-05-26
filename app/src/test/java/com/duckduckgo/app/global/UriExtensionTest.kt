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

import android.net.Uri
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.common.utils.absoluteString
import com.duckduckgo.common.utils.baseHost
import com.duckduckgo.common.utils.domain
import com.duckduckgo.common.utils.extractDomain
import com.duckduckgo.common.utils.faviconLocation
import com.duckduckgo.common.utils.hasIpHost
import com.duckduckgo.common.utils.isHttp
import com.duckduckgo.common.utils.isHttps
import com.duckduckgo.common.utils.isHttpsVersionOfUri
import com.duckduckgo.common.utils.isMobileSite
import com.duckduckgo.common.utils.toDesktopUri
import com.duckduckgo.common.utils.toStringDropScheme
import com.duckduckgo.common.utils.withScheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UriExtensionTest {

    @Test
    fun `withScheme - uri does not have a scheme - with http`() {
        val url = "someurl"
        assertEquals("http://$url", Uri.parse(url).withScheme().toString())
    }

    @Test
    fun `parseUri - with scheme has no effect`() {
        val url = "http://someurl"
        assertEquals(url, Uri.parse(url).withScheme().toString())
    }

    @Test
    fun `parse - uri begins with www - base host returns without www`() {
        val url = "http://www.example.com"
        assertEquals("example.com", Uri.parse(url).baseHost)
    }

    @Test
    fun `parseUri - uri does not begin with www - base host returns same host`() {
        val url = "http://example.com"
        assertEquals("example.com", Uri.parse(url).baseHost)
    }

    @Test
    fun `parseUri - no scheme - base host still resolves host`() {
        val url = "www.example.com"
        assertEquals("example.com", Uri.parse(url).baseHost)
    }

    @Test
    fun `parse - uri contains invalid host - base host is null`() {
        val url = "about:blank"
        assertNull(Uri.parse(url).baseHost)
    }

    @Test
    fun `parseUri - http uri - is http`() {
        assertTrue(Uri.parse("http://example.com").isHttp)
        assertTrue(Uri.parse("HTTP://example.com").isHttp)
    }

    @Test
    fun `parseUri - https - is http false`() {
        assertFalse(Uri.parse("https://example.com").isHttp)
    }

    @Test
    fun `parseUri - malformed uri - is http false`() {
        assertFalse(Uri.parse("[example com]").isHttp)
    }

    @Test
    fun `parseUri - https - is true`() {
        assertTrue(Uri.parse("https://example.com").isHttps)
        assertTrue(Uri.parse("HTTPS://example.com").isHttps)
    }

    @Test
    fun `parseUri - http uri - is https false`() {
        assertFalse(Uri.parse("http://example.com").isHttps)
    }

    @Test
    fun `isHttpsVersionOf - identical uris - is true`() {
        val uri = Uri.parse("https://example.com")
        val other = Uri.parse("http://example.com")
        assertTrue(uri.isHttpsVersionOfUri(other))
    }

    @Test
    fun `isHttpsVersionOfUri - not identical - is false`() {
        val uri = Uri.parse("https://example.com")
        val other = Uri.parse("http://example.com/path")
        assertFalse(uri.isHttpsVersionOfUri(other))
    }

    @Test
    fun `whenUriIsHttpThenIsHttpsVersionOfOtherIsFalse - is https version of uri false`() {
        val uri = Uri.parse("http://example.com")
        val other = Uri.parse("http://example.com")
        assertFalse(uri.isHttpsVersionOfUri(other))
    }

    @Test
    fun `isHttpsVersionOfUri - same protocol - is false`() {
        val uri = Uri.parse("https://example.com")
        val other = Uri.parse("https://example.com")
        assertFalse(uri.isHttpsVersionOfUri(other))
    }

    @Test
    fun `parseUri - malformed uri - is https false`() {
        assertFalse(Uri.parse("[example com]").isHttps)
    }

    @Test
    fun `when Uri has IpHost is true - has IpHost`() {
        assertTrue(Uri.parse("https://54.229.105.203/something").hasIpHost)
        assertTrue(Uri.parse("54.229.105.203/something").hasIpHost)
    }

    @Test
    fun `whenUriHasPortThenHasIpHostIsTrue - has ip host`() {
        assertTrue(Uri.parse("https://54.229.105.203:999/something").hasIpHost)
        assertTrue(Uri.parse("54.229.105.203:999/something").hasIpHost)
    }

    @Test
    fun `parseUri - port number parsed successfully`() {
        assertEquals(999, Uri.parse("https://54.229.105.203:999/something").port)
    }

    @Test
    fun `parse - valid ip address with port parsed with scheme - port number parsed successfully`() {
        assertEquals(999, Uri.parse("121.33.2.11:999").withScheme().port)
    }

    @Test
    fun `when parsing standard uri then has ip host is false - has ip host is false`() {
        assertFalse(Uri.parse("http://example.com").hasIpHost)
    }

    @Test
    fun `parseUri - url starts with m dot - identified as mobile site`() {
        assertTrue(Uri.parse("https://m.example.com").isMobileSite)
    }

    @Test
    fun `parseUrl - starts with mobile dot - identified as mobile site`() {
        assertTrue(Uri.parse("https://mobile.example.com").isMobileSite)
    }

    @Test
    fun `parseUri - subdomain ends with m - not mobile site`() {
        assertFalse(Uri.parse("https://adam.example.com").isMobileSite)
    }

    @Test
    fun `parseUri - does not start with m dot - not identified as mobile site`() {
        assertFalse(Uri.parse("https://example.com").isMobileSite)
    }

    @Test
    fun `toDesktopUri - mobile site to desktop site - short mobile prefix stripped`() {
        val converted = Uri.parse("https://m.example.com").toDesktopUri()
        assertEquals("https://example.com", converted.toString())
    }

    @Test
    fun `toDesktopUri - long mobile prefix stripped`() {
        val converted = Uri.parse("https://mobile.example.com").toDesktopUri()
        assertEquals("https://example.com", converted.toString())
    }

    @Test
    fun `toDesktopUri - mobile site to desktop site - multiple mobile prefixes stripped`() {
        val converted = Uri.parse("https://mobile.m.example.com").toDesktopUri()
        assertEquals("https://example.com", converted.toString())
    }

    @Test
    fun `toDesktopUri - url unchanged`() {
        val converted = Uri.parse("https://example.com").toDesktopUri()
        assertEquals("https://example.com", converted.toString())
    }

    @Test
    fun `getAbsoluteString - do not return query parameters`() {
        val absoluteString = Uri.parse("https://example.com/test?q=example/#1/anotherrandomcode").absoluteString
        assertEquals("https://example.com/test", absoluteString)
    }

    @Test
    fun `whenNullUrl - null favicon url`() {
        assertNull("".toUri().faviconLocation())
    }

    @Test
    fun `whenHttpRequest - favicon location also http`() {
        val favicon = "http://example.com".toUri().faviconLocation()
        assertTrue(favicon!!.isHttp)
    }

    @Test
    fun `whenHttpsRequest - favicon location also https`() {
        val favicon = "https://example.com".toUri().faviconLocation()
        assertTrue(favicon!!.isHttps)
    }

    @Test
    fun `whenUrlContainsASubdomain - favicon returned`() {
        val favicon = "https://sub.example.com".toUri().faviconLocation()
        assertEquals("https://sub.example.com/favicon.ico", favicon.toString())
    }

    @Test
    fun `whenUrlIsIpAddress - ip returned in favicon url`() {
        val favicon = "https://192.168.1.0".toUri().faviconLocation()
        assertEquals("https://192.168.1.0/favicon.ico", favicon.toString())
    }

    @Test
    fun `toUri - no scheme - returns null`() {
        assertNull("www.example.com".toUri().domain())
    }

    @Test
    fun `toUri - has scheme - returns domain`() {
        assertEquals("www.example.com", "http://www.example.com".toUri().domain())
    }

    @Test
    fun `toUri - has resource name - drop scheme return resource name`() {
        assertEquals("www.foo.com", "https://www.foo.com".toUri().toStringDropScheme())
        assertEquals("www.foo.com", "http://www.foo.com".toUri().toStringDropScheme())
    }

    @Test
    fun `toUri - has resource name and path - drops scheme`() {
        assertEquals("www.foo.com/path/to/foo", "https://www.foo.com/path/to/foo".toUri().toStringDropScheme())
        assertEquals("www.foo.com/path/to/foo", "http://www.foo.com/path/to/foo".toUri().toStringDropScheme())
    }

    @Test
    fun `toUri - has resource name path and params - drop scheme return resource name path and params`() {
        assertEquals("www.foo.com/path/to/foo?key=value", "https://www.foo.com/path/to/foo?key=value".toUri().toStringDropScheme())
        assertEquals("www.foo.com/path/to/foo?key=value", "http://www.foo.com/path/to/foo?key=value".toUri().toStringDropScheme())
    }

    @Test
    fun `whenUriExtractDomainThenReturnDomainOnly - return domain only`() {
        assertEquals("www.foo.com", "https://www.foo.com/path/to/foo?key=value".extractDomain())
        assertEquals("www.foo.com", "www.foo.com/path/to/foo?key=value".extractDomain())
        assertEquals("foo.com", "foo.com/path/to/foo?key=value".extractDomain())
        assertEquals("foo.com", "http://foo.com/path/to/foo?key=value".extractDomain())
    }
}
