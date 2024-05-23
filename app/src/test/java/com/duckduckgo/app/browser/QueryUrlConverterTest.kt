/*
 * Copyright (c) 2021 DuckDuckGo
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

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.browser.omnibar.QueryOrigin
import com.duckduckgo.app.browser.omnibar.QueryUrlConverter
import com.duckduckgo.app.referral.AppReferrerDataStore
import com.duckduckgo.app.statistics.store.StatisticsDataStore
import com.duckduckgo.experiments.api.VariantManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class QueryUrlConverterTest {

    private var mockStatisticsStore: StatisticsDataStore = mock()
    private val variantManager: VariantManager = mock()
    private val mockAppReferrerDataStore: AppReferrerDataStore = mock()
    private val requestRewriter = DuckDuckGoRequestRewriter(
        DuckDuckGoUrlDetectorImpl(),
        mockStatisticsStore,
        variantManager,
        mockAppReferrerDataStore,
    )
    private val testee: QueryUrlConverter = QueryUrlConverter(requestRewriter)

    @Before
    fun setup() {
        whenever(variantManager.getVariantKey()).thenReturn("")
    }

    @Test
    fun `convertQueryToUrl - single word - search query built`() {
        val input = "foo"
        val result = testee.convertQueryToUrl(input)
        assertDuckDuckGoSearchQuery("foo", result)
    }

    @Test
    fun `convertQueryToUrl - invalid URL - encoded search query built`() {
        val input = "http://test .com"
        val expected = "http%3A%2F%2Ftest%20.com"
        val result = testee.convertQueryToUrl(input)
        assertDuckDuckGoSearchQuery(expected, result)
    }

    @Test
    fun `convertQueryToUrl - query with symbols - properly encoded`() {
        val input = "test \"%-.<>\\^_`{|~"
        val expected = "test%20%22%25-.%3C%3E%5C%5E_%60%7B%7C~"
        val result = testee.convertQueryToUrl(input)
        assertDuckDuckGoSearchQuery(expected, result)
    }

    @Test
    fun `convertQueryToUrl - invalid characters - appends encoded version`() {
        val input = "43 + 5"
        val expected = "43%20%2B%205"
        val result = testee.convertQueryToUrl(input)
        assertDuckDuckGoSearchQuery(expected, result)
    }

    @Test
    fun `convertQueryToUrl - missing scheme - http added`() {
        val input = "example.com"
        val expected = "http://$input"
        val result = testee.convertQueryToUrl(input)
        assertEquals(expected, result)
    }

    @Test
    fun `convertQueryToUrl - query origin from user - search query built`() {
        val input = "foo"
        val result = testee.convertQueryToUrl(input, queryOrigin = QueryOrigin.FromUser)
        assertDuckDuckGoSearchQuery("foo", result)
    }

    @Test
    fun `convertQueryToUrl - query origin from user and is URL - URL returned`() {
        val input = "http://example.com"
        val result = testee.convertQueryToUrl(input, queryOrigin = QueryOrigin.FromUser)
        assertEquals(input, result)
    }

    @Test
    fun `convertQueryToUrl - query origin from autocomplete and isNav false - search query built`() {
        val input = "example.com"
        val result = testee.convertQueryToUrl(input, queryOrigin = QueryOrigin.FromAutocomplete(isNav = false))
        assertDuckDuckGoSearchQuery("example.com", result)
    }

    @Test
    fun `convertQueryToUrl - query origin from autocomplete and isNav true - url returned`() {
        val input = "http://example.com"
        val result = testee.convertQueryToUrl(input, queryOrigin = QueryOrigin.FromAutocomplete(isNav = true))
        assertEquals(input, result)
    }

    @Test
    fun `convertQueryToUrl - query origin from autocomplete and isNav is null and is not url - search query built`() {
        val input = "foo"
        val result = testee.convertQueryToUrl(input, queryOrigin = QueryOrigin.FromAutocomplete(isNav = null))
        assertDuckDuckGoSearchQuery("foo", result)
    }

    @Test
    fun `convertQueryToUrl - contains a major vertical - vertical added to url`() {
        val input = "foo"
        val vertical = QueryUrlConverter.majorVerticals.random()
        val result = testee.convertQueryToUrl(input, vertical = vertical, queryOrigin = QueryOrigin.FromUser)
        assertTrue(result.contains("iar=$vertical"))
    }

    @Test
    fun `convertQueryToUrl - non-major vertical - vertical not added to url`() {
        val input = "foo"
        val vertical = "nonMajor"
        val result = testee.convertQueryToUrl(input, vertical = vertical, queryOrigin = QueryOrigin.FromUser)
        assertFalse(result.contains("iar=$vertical"))
    }

    private fun assertDuckDuckGoSearchQuery(
        query: String,
        url: String,
    ) {
        val uri = Uri.parse(url)
        assertEquals("duckduckgo.com", uri.host)
        assertEquals("https", uri.scheme)
        assertEquals("", uri.path)
        assertEquals("ddg_android", uri.getQueryParameter("t"))
        val encodedQuery = uri.encodedQuery
        assertNotNull(encodedQuery)
        assertTrue("Query string doesn't match. Expected `q=$query` somewhere in query ${uri.encodedQuery}", encodedQuery!!.contains("q=$query"))
    }
}
