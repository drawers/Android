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
    fun `withScheme - uri does not have a scheme - appends http`() {
        val url = "someurl"
        assertEquals("http://$url", Uri.parse(url).withScheme().toString())
    }

    @Test
    fun `withScheme - uri has a scheme - no effect`() {
        val url = "http://someurl"
        assertEquals(url, Uri.parse(url).withScheme().toString())
    }

    @Test
    fun `baseHost - uri begins with www - returns without www`() {
        val url = "http://www.example.com"
        assertEquals("example.com", Uri.parse(url).baseHost)
    }

    @Test
    fun `baseHost - uri does not begin with www - returns same host`() {
        val url = "http://example.com"
        assertEquals("example.com", Uri.parse(url).baseHost)
    }

    @Test
    fun `baseHost - uri without scheme - resolves host`() {
        val url = "www.example.com"
        assertEquals("example.com", Uri.parse(url).baseHost)
    }

    @Test
    fun `baseHost - invalid host in URI - is null`() {
        val url = "about:blank"
        assertNull(Uri.parse(url).baseHost)
    }

    @Test
    fun `isHttp - uri is http irrespective of case - true`() {
        assertTrue(Uri.parse("http://example.com").isHttp)
        assertTrue(Uri.parse("HTTP://example.com").isHttp)
    }

    @Test
    fun `isHttp - uri is https - false`() {
        assertFalse(Uri.parse("https://example.com").isHttp)
    }

    @Test
    fun `isHttp - malformed URI - is false`() {
        assertFalse(Uri.parse("[example com]").isHttp)
    }

    @Test
    fun `isHttps - uri is https irrespective of case - true`() {
        assertTrue(Uri.parse("https://example.com").isHttps)
        assertTrue(Uri.parse("HTTPS://example.com").isHttps)
    }

    @Test
    fun `isHttps - uri is http - false`() {
        assertFalse(Uri.parse("http://example.com").isHttps)
    }

    @Test
    fun `isHttpsVersionOfUri - https and http identical - true`() {
        val uri = Uri.parse("https://example.com")
        val other = Uri.parse("http://example.com")
        assertTrue(uri.isHttpsVersionOfUri(other))
    }

    @Test
    fun `isHttpsVersionOfUri - https and http but not identical - false`() {
        val uri = Uri.parse("https://example.com")
        val other = Uri.parse("http://example.com/path")
        assertFalse(uri.isHttpsVersionOfUri(other))
    }

    @Test
    fun `isHttpsVersionOfUri - http uri - false`() {
        val uri = Uri.parse("http://example.com")
        val other = Uri.parse("http://example.com")
        assertFalse(uri.isHttpsVersionOfUri(other))
    }

    @Test
    fun `isHttpsVersionOfUri - both URIs are https - false`() {
        val uri = Uri.parse("https://example.com")
        val other = Uri.parse("https://example.com")
        assertFalse(uri.isHttpsVersionOfUri(other))
    }

    @Test
    fun `isHttps - malformed URI - false`() {
        assertFalse(Uri.parse("[example com]").isHttps)
    }

    @Test
    fun `hasIpHost - IP URI - true`() {
        assertTrue(Uri.parse("https://54.229.105.203/something").hasIpHost)
        assertTrue(Uri.parse("54.229.105.203/something").hasIpHost)
    }

    @Test
    fun `hasIpHost - IP with port URI - true`() {
        assertTrue(Uri.parse("https://54.229.105.203:999/something").hasIpHost)
        assertTrue(Uri.parse("54.229.105.203:999/something").hasIpHost)
    }

    @Test
    fun `parse - IP with port URI - port number parsed successfully`() {
        assertEquals(999, Uri.parse("https://54.229.105.203:999/something").port)
    }

    @Test
    fun `withScheme - valid IP address with port - port number parsed successfully`() {
        assertEquals(999, Uri.parse("121.33.2.11:999").withScheme().port)
    }

    @Test
    fun `hasIpHost - standard URI - false`() {
        assertFalse(Uri.parse("http://example.com").hasIpHost)
    }

    @Test
    fun `isMobileSite - url starts with m dot - identified as mobile site`() {
        assertTrue(Uri.parse("https://m.example.com").isMobileSite)
    }

    @Test
    fun `isMobileSite - url starts with mobile dot - identified as mobile site`() {
        assertTrue(Uri.parse("https://mobile.example.com").isMobileSite)
    }

    @Test
    fun `isMobileSite - url subdomain ends with m - not identified as mobile site`() {
        assertFalse(Uri.parse("https://adam.example.com").isMobileSite)
    }

    @Test
    fun `isMobileSite - url does not start with m dot - not identified as mobile site`() {
        assertFalse(Uri.parse("https://example.com").isMobileSite)
    }

    @Test
    fun `toDesktopUri - mobile site - short mobile prefix stripped`() {
        val converted = Uri.parse("https://m.example.com").toDesktopUri()
        assertEquals("https://example.com", converted.toString())
    }

    @Test
    fun `toDesktopUri - mobile site - long mobile prefix stripped`() {
        val converted = Uri.parse("https://mobile.example.com").toDesktopUri()
        assertEquals("https://example.com", converted.toString())
    }

    @Test
    fun `toDesktopUri - mobile site - multiple mobile prefixes stripped`() {
        val converted = Uri.parse("https://mobile.m.example.com").toDesktopUri()
        assertEquals("https://example.com", converted.toString())
    }

    @Test
    fun `toDesktopUri - desktop site - url unchanged`() {
        val converted = Uri.parse("https://example.com").toDesktopUri()
        assertEquals("https://example.com", converted.toString())
    }

    @Test
    fun `absoluteString - query parameters - not returned`() {
        val absoluteString = Uri.parse("https://example.com/test?q=example/#1/anotherrandomcode").absoluteString
        assertEquals("https://example.com/test", absoluteString)
    }

    @Test
    fun `faviconLocation - null URL - null favicon URL`() {
        assertNull("".toUri().faviconLocation())
    }

    @Test
    fun `faviconLocation - http request - also http`() {
        val favicon = "http://example.com".toUri().faviconLocation()
        assertTrue(favicon!!.isHttp)
    }

    @Test
    fun `faviconLocation - https request - also https`() {
        val favicon = "https://example.com".toUri().faviconLocation()
        assertTrue(favicon!!.isHttps)
    }

    @Test
    fun `faviconLocation - url contains subdomain - subdomain returned`() {
        val favicon = "https://sub.example.com".toUri().faviconLocation()
        assertEquals("https://sub.example.com/favicon.ico", favicon.toString())
    }

    @Test
    fun `faviconLocation - URL is IP address - IP returned in favicon URL`() {
        val favicon = "https://192.168.1.0".toUri().faviconLocation()
        assertEquals("https://192.168.1.0/favicon.ico", favicon.toString())
    }

    @Test
    fun `domain - url does not have scheme - return null`() {
        assertNull("www.example.com".toUri().domain())
    }

    @Test
    fun `domain - url has scheme - return domain`() {
        assertEquals("www.example.com", "http://www.example.com".toUri().domain())
    }

    @Test
    fun `toStringDropScheme - uri has resource name - return resource name`() {
        assertEquals("www.foo.com", "https://www.foo.com".toUri().toStringDropScheme())
        assertEquals("www.foo.com", "http://www.foo.com".toUri().toStringDropScheme())
    }

    @Test
    fun `toStringDropScheme - uri has resource name and path - return resource name and path`() {
        assertEquals("www.foo.com/path/to/foo", "https://www.foo.com/path/to/foo".toUri().toStringDropScheme())
        assertEquals("www.foo.com/path/to/foo", "http://www.foo.com/path/to/foo".toUri().toStringDropScheme())
    }

    @Test
    fun `toStringDropScheme - uri has resource name path and params - return resource name path and params`() {
        assertEquals("www.foo.com/path/to/foo?key=value", "https://www.foo.com/path/to/foo?key=value".toUri().toStringDropScheme())
        assertEquals("www.foo.com/path/to/foo?key=value", "http://www.foo.com/path/to/foo?key=value".toUri().toStringDropScheme())
    }

    @Test
    fun `extractDomain - return domain only`() {
        assertEquals("www.foo.com", "https://www.foo.com/path/to/foo?key=value".extractDomain())
        assertEquals("www.foo.com", "www.foo.com/path/to/foo?key=value".extractDomain())
        assertEquals("foo.com", "foo.com/path/to/foo?key=value".extractDomain())
        assertEquals("foo.com", "http://foo.com/path/to/foo?key=value".extractDomain())
    }
}
