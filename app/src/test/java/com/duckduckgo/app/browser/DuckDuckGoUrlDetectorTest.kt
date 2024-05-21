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
    fun `isDuckDuckGoUrl - search term checked - not DDG url`() {
        assertFalse(testee.isDuckDuckGoUrl("foo"))
    }

    @Test
    fun `isDuckDuckGoUrl - non DDG URL - not DDG URL`() {
        assertFalse(testee.isDuckDuckGoUrl("example.com"))
    }

    @Test
    fun `isDuckDuckGoUrl - checking full DDG URL - identified as DDG URL`() {
        assertTrue(testee.isDuckDuckGoUrl("https://duckduckgo.com/?q=test%20search&tappv=android_0_2_0&t=ddg_android"))
    }

    @Test
    fun `isDuckDuckGoUrl - checking subdomain and ETL dis DDC - returns true`() {
        assertTrue(testee.isDuckDuckGoUrl("https://test.duckduckgo.com"))
    }

    @Test
    fun `isDuckDuckGoUrl - checking subdomain and ETL dis not DDG - returns false`() {
        assertFalse(testee.isDuckDuckGoUrl("https://test.duckduckgo.test.com"))
    }

    @Test
    fun `extractQuery - DDG url contains query - query extracted`() {
        val query = testee.extractQuery("https://duckduckgo.com?q=test%20search")
        assertEquals("test search", query)
    }

    @Test
    fun `extractQuery - DDG url does not contain query - null`() {
        val query = testee.extractQuery("https://duckduckgo.com")
        assertNull(query)
    }

    @Test
    fun `isDuckDuckGoQueryUrl - contains query - query detected`() {
        assertTrue(testee.isDuckDuckGoQueryUrl("https://duckduckgo.com?q=test%20search"))
    }

    @Test
    fun `isDuckDuckGoQueryUrl - does not contain query - is not detected`() {
        assertFalse(testee.isDuckDuckGoQueryUrl("https://duckduckgo.com"))
    }

    @Test
    fun `isDuckDuckGoQueryUrl - non DDG URL contains query - not detected`() {
        assertFalse(testee.isDuckDuckGoQueryUrl("https://example.com?q=test%20search"))
    }

    @Test
    fun `extractVertical - DDG URL contains vertical - images`() {
        val vertical = testee.extractVertical("https://duckduckgo.com/?q=new+zealand+images&t=ffab&atb=v218-6&iar=images&iax=images&ia=images")
        assertEquals("images", vertical)
    }

    @Test
    fun `extractVertical - DDG URL does not contain vertical - null`() {
        val vertical = testee.extractVertical("https://duckduckgo.com")
        assertNull(vertical)
    }

    @Test
    fun `isDuckDuckGoVerticalUrl - contains vertical - detected`() {
        assertTrue(testee.isDuckDuckGoVerticalUrl("https://duckduckgo.com?ia=images"))
    }

    @Test
    fun `isDuckDuckGoVerticalUrl - does not contain vertical - is not detected`() {
        assertFalse(testee.isDuckDuckGoVerticalUrl("https://duckduckgo.com"))
    }

    @Test
    fun `isDuckDuckGoVerticalUrl - non DDG URL - not detected`() {
        assertFalse(testee.isDuckDuckGoVerticalUrl("https://example.com?ia=images"))
    }

    @Test
    fun `isDuckDuckGoStaticUrl - settings page - static page detected`() {
        assertTrue(testee.isDuckDuckGoStaticUrl("https://duckduckgo.com/settings"))
    }

    @Test
    fun `isDuckDuckGoStaticUrl - params page - static page detected`() {
        assertTrue(testee.isDuckDuckGoStaticUrl("https://duckduckgo.com/params"))
    }

    @Test
    fun `isDuckDuckGoStatic - not static page - is not detected`() {
        assertFalse(testee.isDuckDuckGoStaticUrl("https://duckduckgo.com/something"))
    }

    @Test
    fun `isDuckDuckGoStatic - non DDG - not detected`() {
        assertFalse(testee.isDuckDuckGoStaticUrl("https://example.com/settings"))
    }

    @Test
    fun `isDuckDuckGoEmailUrl - is not DuckDuckGo email URL - returns false`() {
        assertFalse(testee.isDuckDuckGoEmailUrl("https://example.com"))
    }

    @Test
    fun `isDuckDuckGoEmailUrl - returns true`() {
        assertTrue(testee.isDuckDuckGoEmailUrl("https://duckduckgo.com/email"))
    }

    @Test
    fun `isDuckDuckGoEmailUrl - url contains subdomain and is ETLD for DuckDuckGo email URL - returns true`() {
        assertTrue(testee.isDuckDuckGoEmailUrl("https://test.duckduckgo.com/email"))
    }

    @Test
    fun `isDuckDuckGoEmailUrl - url contains subdomain and is not ETLD for DuckDuckGo email URL - returns true`() {
        assertFalse(testee.isDuckDuckGoEmailUrl("https://test.duckduckgo.test.com/email"))
    }

    @Test
    fun `isDuckDuckGoEmailUrl - no scheme and from duckduckgo url - returns false`() {
        assertFalse(testee.isDuckDuckGoEmailUrl("duckduckgo.com/email"))
    }
}
