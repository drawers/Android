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

package com.duckduckgo.common.utils.formatters.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseDateFormatterTest {

    @Test
    fun `bucketByHour - only hourly precision is used`() {
        val formatted = DatabaseDateFormatter.bucketByHour(fixedTime())
        assertEquals("2020-12-25T13:00:00", formatted)
    }

    @Test
    fun `timestamp - fixed time - only seconds precision used`() {
        val formatted = DatabaseDateFormatter.timestamp(fixedTime())
        assertEquals("2020-12-25T13:14:15", formatted)
    }

    @Test
    fun `iso8601 - date is formatted`() {
        val formatted = DatabaseDateFormatter.iso8601(fixedUTCTime())
        assertEquals("2020-12-25T13:14:15.000000016Z", formatted)
    }

    @Test
    fun `parseMillisIso8601 - iso8601 parsed - date is correct`() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.MILLIS) // SystemClock returns time with higher precision on JVM
        val format = DatabaseDateFormatter.iso8601(now)
        val offsetDateMillis = DatabaseDateFormatter.millisIso8601(now)
        val formatted = DatabaseDateFormatter.parseMillisIso8601(offsetDateMillis)

        assertEquals(format, formatted)
    }

    @Test
    fun `parseMillisIso8601 - system time millis - string represents expected date time`() {
        val timeInMillisNow = System.currentTimeMillis()

        val formattedMillis = DatabaseDateFormatter.parseMillisIso8601(timeInMillisNow)
        val formattedIso8601Now = DatabaseDateFormatter.iso8601()

        // truncate on minutes just to avoid seconds/millis difference
        val isoParsed = Instant.parse(formattedIso8601Now).atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES)
        val millisParsed = Instant.parse(formattedMillis).atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES)

        assertEquals(isoParsed, millisParsed)
    }

    @Test
    fun `parseIso8601ToMillis - represents expected dateTime`() {
        val timeInMillisNow = System.currentTimeMillis()
        val iso8601String = DatabaseDateFormatter.parseMillisIso8601(timeInMillisNow)

        val millisParsed = DatabaseDateFormatter.parseIso8601ToMillis(iso8601String)

        assertEquals(timeInMillisNow, millisParsed)
    }

    private fun fixedTime(): LocalDateTime {
        return LocalDateTime.of(2020, 12, 25, 13, 14, 15, 16)
    }
    private fun fixedUTCTime(): OffsetDateTime {
        return OffsetDateTime.of(2020, 12, 25, 13, 14, 15, 16, ZoneOffset.UTC)
    }
}
