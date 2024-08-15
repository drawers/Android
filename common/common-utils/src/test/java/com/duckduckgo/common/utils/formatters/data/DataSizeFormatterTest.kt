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

package com.duckduckgo.common.utils.formatters.data

import java.text.NumberFormat
import java.util.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DataSizeFormatterTest {

    private lateinit var testee: DataSizeFormatter

    @Before
    fun setup() {
        testee = DataSizeFormatter(NumberFormat.getNumberInstance(Locale.US).also { it.maximumFractionDigits = 1 })
    }

    @Test
    fun `format - no data - 0 bytes returned`() {
        assertEquals("0 bytes", testee.format(0))
    }

    @Test
    fun `format - less than 1Kb - bytes returned`() {
        assertEquals("100 bytes", testee.format(100))
    }

    @Test
    fun `format - exactly on 1 KB - KB returned`() {
        assertEquals("1 KB", testee.format(1000))
    }

    @Test
    fun `format - not a whole number of kilobytes - kb returned`() {
        assertEquals("1.5 KB", testee.format(1501))
    }

    @Test
    fun `format - exactly 1 megabyte - MB returned`() {
        assertEquals("1 MB", testee.format(1_000_000))
    }

    @Test
    fun `format - exactly 1 gigabyte - GB returned`() {
        assertEquals("1 GB", testee.format(1_000_000_000))
    }
}
