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
import com.duckduckgo.app.browser.WebNavigationStateChange.NewPage
import com.duckduckgo.app.browser.WebNavigationStateChange.Other
import com.duckduckgo.app.browser.WebNavigationStateChange.PageCleared
import com.duckduckgo.app.browser.WebNavigationStateChange.PageNavigationCleared
import com.duckduckgo.app.browser.WebNavigationStateChange.Unchanged
import com.duckduckgo.app.browser.WebNavigationStateChange.UrlUpdated
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebNavigationStateComparisonTest {

    @Test
    fun `compare - previous state and latest state same - returns unchanged`() {
        val state = buildState("http://foo.com", "http://subdomain.foo.com")
        assertEquals(Unchanged, state.compare(state))
    }

    @Test
    fun `compare - previous state and latest state equal - returns unchanged`() {
        val previousState = buildState("http://foo.com", "http://subdomain.foo.com")
        val latestState = buildState("http://foo.com", "http://subdomain.foo.com")
        assertEquals(Unchanged, latestState.compare(previousState))
    }

    @Test
    fun `compare - previous state is null and latest contains original URL, current URL, and title - returns new page with title`() {
        val previousState = null
        val latestState = buildState("http://latest.com", "http://subdomain.latest.com", "Title")
        assertEquals(NewPage("http://subdomain.latest.com", "Title"), latestState.compare(previousState))
    }

    @Test
    fun `compare - previous state is null and latest contains original URL and no title - returns new page without title`() {
        val previousState = null
        val latestState = buildState("http://latest.com", "http://subdomain.latest.com")
        assertEquals(NewPage("http://subdomain.latest.com", null), latestState.compare(previousState))
    }

    @Test
    fun `compare - previous contains no original or current URL and latest contains both - returns new page`() {
        val previousState = buildState(null, null)
        val latestState = buildState("http://latest.com", "http://subdomain.latest.com")
        assertEquals(NewPage("http://subdomain.latest.com", null), latestState.compare(previousState))
    }

    @Test
    fun `compare - previous state no original URL and current URL, latest state original and current URL - returns new page`() {
        val previousState = buildState(null, "http://subdomain.previous.com")
        val latestState = buildState("http://latest.com", "http://subdomain.latest.com")
        assertEquals(NewPage("http://subdomain.latest.com", null), latestState.compare(previousState))
    }

    @Test
    fun `compare - previous contains original URL and no current URL, latest contains original and current URL - returns new page`() {
        val previousState = buildState("http://previous.com", null)
        val latestState = buildState("http://latest.com", "http://subdomain.latest.com")
        assertEquals(NewPage("http://subdomain.latest.com", null), latestState.compare(previousState))
    }

    @Test
    fun `compare - different original url - returns new page`() {
        val previousState = buildState("http://previous.com", "http://subdomain.previous.com")
        val latestState = buildState("http://latest.com", "http://subdomain.latest.com")
        assertEquals(NewPage("http://subdomain.latest.com", null), latestState.compare(previousState))
    }

    @Test
    fun `compare - same original URL different current URL domain - returns new page`() {
        val previousState = buildState("http://same.com", "http://subdomain.previous.com")
        val latestState = buildState("http://same.com", "http://subdomain.latest.com")
        assertEquals(NewPage("http://subdomain.latest.com", null), latestState.compare(previousState))
    }

    @Test
    fun `compare - same original url and different current url with same host - url updated`() {
        val previousState = buildState("http://same.com", "http://same.com/previous")
        val latestState = buildState("http://same.com", "http://same.com/latest")
        assertEquals(UrlUpdated("http://same.com/latest"), latestState.compare(previousState))
    }

    @Test
    fun `compare - previous contains original and current URL, latest contains none - returns page cleared`() {
        val previousState = buildState("http://previous.com", "http://subdomain.previous.com")
        val latestState = buildState(null, null)
        assertEquals(PageCleared, latestState.compare(previousState))
    }

    @Test
    fun `compare - previous contains original URL and current URL, latest contains no original URL and a current URL - returns page cleared`() {
        val previousState = buildState("http://previous.com", "http://subdomain.previous.com")
        val latestState = buildState(null, "http://subdomain.latest.com")
        assertEquals(PageCleared, latestState.compare(previousState))
    }

    @Test
    fun `compare - latest state is empty navigation - page navigation cleared`() {
        val previousState = buildState("http://previous.com", "http://subdomain.previous.com")
        val latestState = EmptyNavigationState(previousState)
        assertEquals(PageNavigationCleared, latestState.compare(previousState))
    }

    @Test
    fun `compare - same original URL and no current URL - returns other`() {
        val previousState = buildState("http://same.com", "http://subdomain.previous.com")
        val latestState = buildState("http://same.com", null)
        assertEquals(Other, latestState.compare(previousState))
    }

    @Test
    fun `compare - different original URL and no current URL - returns other`() {
        val previousState = buildState("http://previous.com", "http://subdomain.previous.com")
        val latestState = buildState("http://latest.com", null)
        assertEquals(Other, latestState.compare(previousState))
    }

    private fun buildState(
        originalUrl: String?,
        currentUrl: String?,
        title: String? = null,
        newProgress: Int? = null,
    ): WebNavigationState {
        return TestNavigationState(
            originalUrl = originalUrl,
            currentUrl = currentUrl,
            title = title,
            stepsToPreviousPage = 1,
            canGoBack = true,
            canGoForward = true,
            hasNavigationHistory = true,
            progress = newProgress,
        )
    }
}
