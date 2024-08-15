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

package com.duckduckgo.app.browser

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DuckDuckGoUrlDetectorTest {

    private lateinit var testee: DuckDuckGoUrlDetector

    @Before
    fun setup() {
        testee = DuckDuckGoUrlDetectorImpl()
    }

    @Test
    fun `isDuckDuckGoUrl - search term - not DDG URL`() {
        assertFalse(testee.isDuckDuckGoUrl("foo"))
    }

    @Test
    fun `isDuckDuckGoUrl - non-DDG URL - identified as not DDG URL`() {
        assertFalse(testee.isDuckDuckGoUrl("example.com"))
    }

    @Test
    fun `isDuckDuckGoUrl - full DDG URL - identified as DDG URL`() {
        assertTrue(testee.isDuckDuckGoUrl("https://duckduckgo.com/?q=test%20search&tappv=android_0_2_0&t=ddg_android"))
    }

    @Test
    fun `isDuckDuckGoUrl - subdomain and ETL is DDG - return true`() {
        assertTrue(testee.isDuckDuckGoUrl("https://test.duckduckgo.com"))
    }

    @Test
    fun `isDuckDuckGoUrl - subdomain and ETL is not DDG - return false`() {
        assertFalse(testee.isDuckDuckGoUrl("https://test.duckduckgo.test.com"))
    }

    @Test
    fun `extractQuery - DDG URL contains query - query can be extracted`() {
        val query = testee.extractQuery("https://duckduckgo.com?q=test%20search")
        assertEquals("test search", query)
    }

    @Test
    fun `extractQuery - DDG URL does not contain query - query is null`() {
        val query = testee.extractQuery("https://duckduckgo.com")
        assertNull(query)
    }

    @Test
    fun `isDuckDuckGoQueryUrl - DDG URL contains query - query detected`() {
        assertTrue(testee.isDuckDuckGoQueryUrl("https://duckduckgo.com?q=test%20search"))
    }

    @Test
    fun `isDuckDuckGoQueryUrl - URL does not contain query - query not detected`() {
        assertFalse(testee.isDuckDuckGoQueryUrl("https://duckduckgo.com"))
    }

    @Test
    fun `isDuckDuckGoQueryUrl - non-DDG URL contains query - query is not detected`() {
        assertFalse(testee.isDuckDuckGoQueryUrl("https://example.com?q=test%20search"))
    }

    @Test
    fun `extractVertical - DDG URL contains vertical - vertical can be extracted`() {
        val vertical = testee.extractVertical("https://duckduckgo.com/?q=new+zealand+images&t=ffab&atb=v218-6&iar=images&iax=images&ia=images")
        assertEquals("images", vertical)
    }

    @Test
    fun `extractVertical - DDG URL does not contain vertical - vertical is null`() {
        val vertical = testee.extractVertical("https://duckduckgo.com")
        assertNull(vertical)
    }

    @Test
    fun `isDuckDuckGoVerticalUrl - vertical URL detected`() {
        assertTrue(testee.isDuckDuckGoVerticalUrl("https://duckduckgo.com?ia=images"))
    }

    @Test
    fun `isDuckDuckGoVerticalUrl - URL does not contain vertical - not detected`() {
        assertFalse(testee.isDuckDuckGoVerticalUrl("https://duckduckgo.com"))
    }

    @Test
    fun `isDuckDuckGoVerticalUrl - non-DDG URL - not detected`() {
        assertFalse(testee.isDuckDuckGoVerticalUrl("https://example.com?ia=images"))
    }

    @Test
    fun `isDuckDuckGoStaticUrl - settings page - static page detected`() {
        assertTrue(testee.isDuckDuckGoStaticUrl("https://duckduckgo.com/settings"))
    }

    @Test
    fun `isDuckDuckGoStaticUrl - DDG params page - static page detected`() {
        assertTrue(testee.isDuckDuckGoStaticUrl("https://duckduckgo.com/params"))
    }

    @Test
    fun `isDuckDuckGoStaticUrl - non-static page - not detected`() {
        assertFalse(testee.isDuckDuckGoStaticUrl("https://duckduckgo.com/something"))
    }

    @Test
    fun `isDuckDuckGoStaticUrl - non DDG - static page detected`() {
        assertFalse(testee.isDuckDuckGoStaticUrl("https://example.com/settings"))
    }

    @Test
    fun `isDuckDuckGoEmailUrl - not DuckDuckGo email URL - return false`() {
        assertFalse(testee.isDuckDuckGoEmailUrl("https://example.com"))
    }

    @Test
    fun `isDuckDuckGoEmailUrl - DuckDuckGo email URL - return true`() {
        assertTrue(testee.isDuckDuckGoEmailUrl("https://duckduckgo.com/email"))
    }

    @Test
    fun `isDuckDuckGoEmailUrl - url contains subdomain and is eTLD - return true`() {
        assertTrue(testee.isDuckDuckGoEmailUrl("https://test.duckduckgo.com/email"))
    }

    @Test
    fun `isDuckDuckGoEmailUrl - url contains subdomain and is not eTLD - return true`() {
        assertFalse(testee.isDuckDuckGoEmailUrl("https://test.duckduckgo.test.com/email"))
    }

    @Test
    fun `isDuckDuckGoEmailUrl - no scheme - returns false`() {
        assertFalse(testee.isDuckDuckGoEmailUrl("duckduckgo.com/email"))
    }
}
