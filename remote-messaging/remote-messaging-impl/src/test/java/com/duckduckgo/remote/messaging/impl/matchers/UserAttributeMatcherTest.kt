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

package com.duckduckgo.remote.messaging.impl.matchers

import com.duckduckgo.browser.api.UserBrowserProperties
import com.duckduckgo.common.ui.DuckDuckGoTheme
import com.duckduckgo.remote.messaging.impl.models.*
import java.util.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UserAttributeMatcherTest {

    private val userBrowserProperties: UserBrowserProperties = mock()

    private val testee = UserAttributeMatcher(userBrowserProperties)

    @Test
    fun `evaluate - app theme matches - returns match`() = runTest {
        givenBrowserProperties(appTheme = DuckDuckGoTheme.SYSTEM_DEFAULT)

        val result = testee.evaluate(
            AppTheme(value = "system_default"),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - app theme does not match - return fail`() = runTest {
        givenBrowserProperties(appTheme = DuckDuckGoTheme.SYSTEM_DEFAULT)

        val result = testee.evaluate(
            AppTheme(value = "light"),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - bookmarks matches - returns match`() = runTest {
        givenBrowserProperties(bookmarks = 10L)

        val result = testee.evaluate(
            Bookmarks(value = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - bookmarks does not match - return fail`() = runTest {
        givenBrowserProperties(bookmarks = 10L)

        val result = testee.evaluate(
            Bookmarks(value = 15),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - bookmarks equal or lower than max - return match`() = runTest {
        givenBrowserProperties(bookmarks = 10L)

        val result = testee.evaluate(
            Bookmarks(max = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - bookmarks greater than max - return fail`() = runTest {
        givenBrowserProperties(bookmarks = 15L)

        val result = testee.evaluate(
            Bookmarks(max = 10),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - bookmarks equal or greater than min - return match`() = runTest {
        givenBrowserProperties(bookmarks = 10L)

        val result = testee.evaluate(
            Bookmarks(min = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - bookmarks lower than min - return fail`() = runTest {
        givenBrowserProperties(bookmarks = 0L)

        val result = testee.evaluate(
            Bookmarks(min = 9),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - bookmarks in range - returns match`() = runTest {
        givenBrowserProperties(bookmarks = 10L)

        val result = testee.evaluate(
            Bookmarks(min = 9, max = 15),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - bookmarks not in range - return match`() = runTest {
        givenBrowserProperties(bookmarks = 10L)

        val result = testee.evaluate(
            Bookmarks(min = 3, max = 6),
        )

        assertEquals(false, result)
    }

    // Favorites
    @Test
    fun `evaluate - favorites matches - returns match`() = runTest {
        givenBrowserProperties(favorites = 10L)

        val result = testee.evaluate(
            Favorites(value = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - favorites does not match - return fail`() = runTest {
        givenBrowserProperties(favorites = 10L)

        val result = testee.evaluate(
            Favorites(value = 15),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - favorites equal or lower than max - return match`() = runTest {
        givenBrowserProperties(favorites = 10L)

        val result = testee.evaluate(
            Favorites(max = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - favorites greater than max - return fail`() = runTest {
        givenBrowserProperties(favorites = 10L)

        val result = testee.evaluate(
            Favorites(max = 5),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - favorites equal or greater than min - return match`() = runTest {
        givenBrowserProperties(favorites = 10L)

        val result = testee.evaluate(
            Favorites(min = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - favorites lower than min - return fail`() = runTest {
        givenBrowserProperties(favorites = 0L)

        val result = testee.evaluate(
            Favorites(min = 10),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - favorites in range - returns match`() = runTest {
        givenBrowserProperties(favorites = 10L)

        val result = testee.evaluate(
            Favorites(min = 9, max = 15),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - favorites not in range - return match`() = runTest {
        givenBrowserProperties(favorites = 10L)

        val result = testee.evaluate(
            Favorites(min = 3, max = 6),
        )

        assertEquals(false, result)
    }

    // DaysSinceInstalled
    @Test
    fun `evaluate - days since installed equal or lower than max - return match`() = runTest {
        givenBrowserProperties(daysSinceInstalled = 10L)

        val result = testee.evaluate(
            DaysSinceInstalled(max = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - days since installed greater than max - return fail`() = runTest {
        givenBrowserProperties(daysSinceInstalled = 10L)

        val result = testee.evaluate(
            DaysSinceInstalled(max = 5),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - days since installed equal or greater than min - return match`() = runTest {
        givenBrowserProperties(daysSinceInstalled = 10L)

        val result = testee.evaluate(
            DaysSinceInstalled(min = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - days since installed lower than min - return fail`() = runTest {
        givenBrowserProperties(daysSinceInstalled = 1L)

        val result = testee.evaluate(
            DaysSinceInstalled(min = 10),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - days since installed in range - return match`() = runTest {
        givenBrowserProperties(daysSinceInstalled = 10L)

        val result = testee.evaluate(
            DaysSinceInstalled(min = 9, max = 15),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - days since installed not in range - return match`() = runTest {
        givenBrowserProperties(daysSinceInstalled = 10L)

        val result = testee.evaluate(
            DaysSinceInstalled(min = 3, max = 6),
        )

        assertEquals(false, result)
    }

    // DaysUsedSince
    @Test
    fun `evaluate - days used since matches - returns match`() = runTest {
        givenBrowserProperties(daysUsedSince = 10L)

        val result = testee.evaluate(
            DaysUsedSince(since = Date(), value = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - days used since does not match - return fail`() = runTest {
        givenBrowserProperties(daysUsedSince = 10L)

        val result = testee.evaluate(
            DaysUsedSince(since = Date(), value = 8),
        )

        assertEquals(false, result)
    }

    // DefaultBrowser
    @Test
    fun `evaluate - default browser matches - returns match`() = runTest {
        givenBrowserProperties(defaultBrowser = true)

        val result = testee.evaluate(
            DefaultBrowser(value = true),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - default browser does not match - return fail`() = runTest {
        givenBrowserProperties(defaultBrowser = false)

        val result = testee.evaluate(
            DefaultBrowser(value = true),
        )

        assertEquals(false, result)
    }

    // EmailEnabled
    @Test
    fun `evaluate - email enabled matches - returns match`() = runTest {
        givenBrowserProperties(emailEnabled = true)

        val result = testee.evaluate(
            EmailEnabled(value = true),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - email enabled does not match - return fail`() = runTest {
        givenBrowserProperties(emailEnabled = false)

        val result = testee.evaluate(
            EmailEnabled(value = true),
        )

        assertEquals(false, result)
    }

    // SearchCount
    @Test
    fun `evaluate - search count matches - returns match`() = runTest {
        givenBrowserProperties(searchCount = 10L)

        val result = testee.evaluate(
            SearchCount(value = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - search count does not match - return fail`() = runTest {
        givenBrowserProperties(searchCount = 10L)

        val result = testee.evaluate(
            SearchCount(value = 15),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - search count equal or lower than max - returns match`() = runTest {
        givenBrowserProperties(searchCount = 10L)

        val result = testee.evaluate(
            SearchCount(max = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - search count greater than max - return fail`() = runTest {
        givenBrowserProperties(searchCount = 10L)

        val result = testee.evaluate(
            SearchCount(max = 5),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - search count equal or greater than min - return match`() = runTest {
        givenBrowserProperties(searchCount = 10L)

        val result = testee.evaluate(
            SearchCount(min = 10),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - search count lower than min - return fail`() = runTest {
        givenBrowserProperties(searchCount = 1L)

        val result = testee.evaluate(
            SearchCount(min = 10),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - search count in range - returns match`() = runTest {
        givenBrowserProperties(searchCount = 10L)

        val result = testee.evaluate(
            SearchCount(min = 10, max = 15),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - search count not in range - return match`() = runTest {
        givenBrowserProperties(searchCount = 10L)

        val result = testee.evaluate(
            SearchCount(min = 3, max = 6),
        )

        assertEquals(false, result)
    }

    // WidgetAdded
    @Test
    fun `evaluate - widget added matches - return match`() = runTest {
        givenBrowserProperties(widgetAdded = true)

        val result = testee.evaluate(
            WidgetAdded(value = true),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - widget added does not match - return fail`() = runTest {
        givenBrowserProperties(widgetAdded = false)

        val result = testee.evaluate(
            WidgetAdded(value = true),
        )

        assertEquals(false, result)
    }

    private suspend fun givenBrowserProperties(
        appTheme: DuckDuckGoTheme = DuckDuckGoTheme.SYSTEM_DEFAULT,
        bookmarks: Long = 8L,
        favorites: Long = 8L,
        daysSinceInstalled: Long = 8L,
        daysUsedSince: Long = 8L,
        defaultBrowser: Boolean = true,
        emailEnabled: Boolean = true,
        searchCount: Long = 8L,
        widgetAdded: Boolean = true,
    ) {
        whenever(userBrowserProperties.appTheme()).thenReturn(appTheme)
        whenever(userBrowserProperties.bookmarks()).thenReturn(bookmarks)
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(daysSinceInstalled)
        whenever(userBrowserProperties.daysUsedSince(any())).thenReturn(daysUsedSince)
        whenever(userBrowserProperties.defaultBrowser()).thenReturn(defaultBrowser)
        whenever(userBrowserProperties.emailEnabled()).thenReturn(emailEnabled)
        whenever(userBrowserProperties.favorites()).thenReturn(favorites)
        whenever(userBrowserProperties.searchCount()).thenReturn(searchCount)
        whenever(userBrowserProperties.widgetAdded()).thenReturn(widgetAdded)
    }
}
