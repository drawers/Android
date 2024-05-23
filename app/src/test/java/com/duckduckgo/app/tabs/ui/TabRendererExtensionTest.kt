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

package com.duckduckgo.app.tabs.ui

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.tabs.model.TabEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class TabRendererExtensionTest {

    private val context: Context = mock()

    @Test
    fun `displayTitle - tab is blank - DuckDuckGo`() {
        whenever(context.getString(R.string.homeTab)).thenReturn("DuckDuckGo")
        assertEquals("DuckDuckGo", TabEntity("", position = 0).displayTitle(context))
    }

    @Test
    fun `displayTitle - tab has title - same`() {
        assertEquals(TITLE, TabEntity("", URL, TITLE, position = 0).displayTitle(context))
    }

    @Test
    fun `displayTitle - tab does not have title - is url host`() {
        assertEquals("example.com", TabEntity("", URL, null, position = 0).displayTitle(context))
    }

    @Test
    fun `displayTitle - tab without title and invalid URL - title is blank`() {
        assertEquals("", TabEntity("", INVALID_URL, null, position = 0).displayTitle(context))
    }

    @Test
    fun `displayUrl - tab is blank - url is DuckDuckGo`() {
        assertEquals("https://duckduckgo.com", TabEntity("", position = 0).displayUrl())
    }

    @Test
    fun `displayUrl - tab has URL - same URL`() {
        assertEquals(URL, TabEntity("", URL, TITLE, position = 0).displayUrl())
    }

    @Test
    fun `displayUrl - tab does not have a URL - blank`() {
        assertEquals("", TabEntity("", null, TITLE, position = 0).displayUrl())
    }

    companion object {
        private const val TITLE = "Title"
        private const val URL = "https://example.com"
        private const val INVALID_URL = "notaurl"
    }
}
