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

package com.duckduckgo.downloads.impl

import android.webkit.MimeTypeMap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.downloads.impl.DataUriParser.ParseResult.Invalid
import com.duckduckgo.downloads.impl.DataUriParser.ParseResult.ParsedDataUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class DataUriParserTest {

    private lateinit var testee: DataUriParser

    @Before
    fun setup() {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("jpg", "image/jpeg")
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("png", "image/png")
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("txt", "text/plain")
        testee = DataUriParser()
    }

    @Test
    fun `generate - mimeType image·png - png suffix generated`() {
        val parsed = testee.generate("data:image/png;base64,AAAA") as ParsedDataUri
        assertEquals("png", parsed.filename.fileType)
    }

    @Test
    fun `generate - mime type image·jpeg - jpg suffix generated`() {
        val parsed = testee.generate("data:image/jpeg;base64,AAAA") as ParsedDataUri
        assertEquals("jpg", parsed.filename.fileType)
    }

    @Test
    fun `generate - arbitrary image type - no suffix generated`() {
        val parsed = testee.generate("data:image/foo;base64,AAAA") as ParsedDataUri
        assertEquals("", parsed.filename.fileType)
    }

    @Test
    fun `generate - mime type not provided - no suffix added`() {
        val parsed = testee.generate("data:,AAAA") as ParsedDataUri
        assertEquals("", parsed.filename.fileType)
    }

    @Test
    fun `generate - invalid data URI provided - invalid type returned`() {
        val parsed = testee.generate("AAAA")
        assertTrue(parsed === Invalid)
    }

    @Test
    fun `generate - invalid data URI containing comma - invalid type returned`() {
        val parsed = testee.generate("data:,")
        assertTrue(parsed === Invalid)
    }

    @Test
    fun `generate - known non-image mime type - suffix generated`() {
        val parsed = testee.generate("data:text/plain;base64,AAAA") as ParsedDataUri
        assertEquals("txt", parsed.filename.fileType)
    }

    @Test
    fun `generate - mime type not provided - no suffix added in toString`() {
        val filename = testee.generate("data:,AAAA") as ParsedDataUri
        assertFalse(filename.toString().contains("."))
    }

    @Test
    fun `generate - text·plain base64 data is png - suffix is png`() {
        val parsed = testee.generate("data:text/plain;base64,iVBORw0KGgo") as ParsedDataUri
        assertEquals("png", parsed.filename.fileType)
    }

    @Test
    fun `generate - mime type is text plain and data is base64 and is jfif - suffix is jpg`() {
        val parsed = testee.generate("data:text/plain;base64,/9j/4AAQSkZJRgABAQ") as ParsedDataUri
        assertEquals("jpg", parsed.filename.fileType)
    }

    @Test
    fun `generate - text·plain base64 SVG data - suffix is svg`() {
        val parsed = testee.generate("data:text/plain;base64,PHN2ZyB2ZXJzaW9uPSIxLjIiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy") as ParsedDataUri
        assertEquals("svg", parsed.filename.fileType)
    }

    @Test
    fun `generate - text plain base64 gif data - suffix is gif`() {
        val parsed = testee.generate("data:text/plain;base64,R0lGODlhAAXQAocAAP") as ParsedDataUri
        assertEquals("gif", parsed.filename.fileType)
    }

    @Test
    fun `generate - text·plain base64 PDF data - suffix is pdf`() {
        val parsed = testee.generate("data:text/plain;base64,JVBERi0xLjEKMSAwIG9iag") as ParsedDataUri
        assertEquals("pdf", parsed.filename.fileType)
    }

    @Test
    fun `generate - text·plain base64 webp data - suffix is webp`() {
        val parsed = testee.generate("data:text/plain;base64,UklGRs4IAABXRUJQVlA4WAo") as ParsedDataUri
        assertEquals("webp", parsed.filename.fileType)
    }

    @Test
    fun `generate - text·plain base64 data - suffix is bmp`() {
        val parsed = testee.generate("data:text/plain;base64,Qk1AwgEA") as ParsedDataUri
        assertEquals("bmp", parsed.filename.fileType)
    }

    @Test
    fun `generate - text·plain base64 unknown - suffix is txt`() {
        val parsed = testee.generate("data:text/plain;base64,RUJQVlA4WAo") as ParsedDataUri
        assertEquals("txt", parsed.filename.fileType)
    }

    @Test
    fun `generate - mime type is image·jpeg - suffix is jpg`() {
        val parsed = testee.generate("data:image/jpeg;base64,RUJQVlA4WAo") as ParsedDataUri
        assertEquals("jpg", parsed.filename.fileType)
    }
}
