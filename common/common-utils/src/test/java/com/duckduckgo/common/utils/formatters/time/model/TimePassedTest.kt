/*
 * Copyright (c) 2023 DuckDuckGo
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

package com.duckduckgo.common.utils.formatters.time.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimePassedTest {

    private val resources = getApplicationContext<Context>().resources

    @Test
    fun `format - only hours passed - proper time`() {
        val timePassed = TimePassed(1, 0, 0)
        assertEquals("1 hr 0 min 0 sec", timePassed.format(resources = resources))
    }

    @Test
    fun `format - only minutes passed - proper time`() {
        val timePassed = TimePassed(0, 10, 0)
        assertEquals("0 hr 10 min 0 sec", timePassed.format(resources = resources))
    }

    @Test
    fun `format - only seconds passed - proper time`() {
        val timePassed = TimePassed(0, 0, 25)
        assertEquals("0 hr 0 min 25 sec", timePassed.format(resources = resources))
    }

    @Test
    fun `format - hours and minutes passed - proper time`() {
        val timePassed = TimePassed(1, 10, 0)
        assertEquals("1 hr 10 min 0 sec", timePassed.format(resources = resources))
    }

    @Test
    fun `format - hours and seconds passed - proper time`() {
        val timePassed = TimePassed(1, 0, 30)
        assertEquals("1 hr 0 min 30 sec", timePassed.format(resources = resources))
    }

    @Test
    fun `format - minutes and seconds passed - proper time`() {
        val timePassed = TimePassed(0, 10, 10)
        assertEquals("0 hr 10 min 10 sec", timePassed.format(resources = resources))
    }

    @Test
    fun `shortFormat - only hours passed - proper time`() {
        val timePassed = TimePassed(1, 0, 0)
        assertEquals("1h ago", timePassed.shortFormat(resources))
    }

    @Test
    fun `shortFormat - only minutes passed - proper time`() {
        val timePassed = TimePassed(0, 10, 0)
        assertEquals("10m ago", timePassed.shortFormat(resources))
    }

    @Test
    fun `shortFormat - only seconds passed - proper time`() {
        val timePassed = TimePassed(0, 0, 45)
        assertEquals("Just Now", timePassed.shortFormat(resources))
    }

    @Test
    fun `shortFormat - few seconds passed - proper time`() {
        val timePassed = TimePassed(0, 0, 25)
        assertEquals("Just Now", timePassed.shortFormat(resources))
    }

    @Test
    fun `shortFormat - hours and minutes passed - proper time`() {
        val timePassed = TimePassed(1, 10, 0)
        assertEquals("1h ago", timePassed.shortFormat(resources))
    }

    @Test
    fun `shortFormat - hours and seconds passed - proper time`() {
        val timePassed = TimePassed(1, 0, 30)
        assertEquals("1h ago", timePassed.shortFormat(resources))
    }

    @Test
    fun `shortFormat - minutes and seconds passed - formats proper time`() {
        val timePassed = TimePassed(0, 10, 10)
        assertEquals("10m ago", timePassed.shortFormat(resources))
    }
}
