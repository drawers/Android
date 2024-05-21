/*
 * Copyright (c) 2020 DuckDuckGo
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
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.downloads.api.FileDownloader.PendingFileDownload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class UriUtilsFilenameExtractorTest {

    private val mockedPixel: Pixel = mock()
    private val testee: FilenameExtractor = FilenameExtractor(mockedPixel)

    @Test
    fun `extract - url ends with filename as jpg no mime or content disposition - extracted`() {
        val url = "https://example.com/realFilename.jpg"
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted
        assertEquals("realFilename.jpg", extractionResult.filename)
    }

    @Test
    fun `extract - url contains filename but additional path segments - extracted`() {
        val url = "https://foo.example.com/path/images/b/b1/realFilename.jpg/other/stuff"
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted
        assertEquals("realFilename.jpg", extractionResult.filename)
    }

    @Test
    fun `extract - url contains filename but additional path segments and query params - extracted`() {
        val url = "https://foo.example.com/path/images/b/b1/realFilename.jpg/other/stuff?cb=123"
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted
        assertEquals("realFilename.jpg", extractionResult.filename)
    }

    @Test
    fun `extract - url contains filename but path segments look like a filename - extracted`() {
        val url = "https://foo.example.com/path/dotted.path/b/b1/realFilename.jpg"
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted
        assertEquals("realFilename.jpg", extractionResult.filename)
    }

    @Test
    fun `extract - url contains ambiguous filename - extracted`() {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("jpg", "image/jpeg")

        val url = "https://foo.example.com/path/dotted.path/b/b1/realFilename"
        val mimeType = "image/jpeg"
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted
        assertEquals("realFilename.jpg", extractionResult.filename)
    }

    @Test
    fun `extract - url contains filename and multiple path segments - extracted filename`() {
        val url = "https://foo.example.com/path/images/b/b1/realFilename.jpg"
        val mimeType = "image/jpeg"
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted
        assertEquals("realFilename.jpg", extractionResult.filename)
    }

    @Test
    fun `extract - url contains filename but additional path segments and query params which look like a filename - extracted`() {
        val url = "https://foo.example.com/path/images/b/b1/realFilename.jpg/other/stuff?cb=123.com"
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted
        assertEquals("realFilename.jpg", extractionResult.filename)
    }

    @Test
    fun `extract - url contains filename but content disposition says otherwise - extracted from content disposition`() {
        val url = "https://example.com/filename.jpg"
        val mimeType: String? = null
        val contentDisposition = "Content-Disposition: attachment; filename=fromDisposition.jpg"

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted
        assertEquals("fromDisposition.jpg", extractionResult.filename)
    }

    @Test
    fun `extract - filename ends in bin - extracted`() {
        val url = "https://example.com/realFilename.bin"
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted

        assertEquals("realFilename.bin", extractionResult.filename)
    }

    @Test
    fun `extract - url contains no file name but lots of path segments - first segment name used`() {
        val url = "https://example.com/foo/bar/files"
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Guess)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Guess
        assertEquals("foo", extractionResult.bestGuess)
    }

    @Test
    fun `extract - filename ends in bin with a slash - extracted`() {
        val url = "https://example.com/realFilename.bin/"
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted

        assertEquals("realFilename.bin", extractionResult.filename)
    }

    @Test
    fun `extract - filename contains bin - extracted`() {
        val url = "https://example.com/realFilename.bin/foo/bar"
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted

        assertEquals("realFilename.bin", extractionResult.filename)
    }

    @Test
    fun `extract - url empty string and no other data provided - default name filetype returned`() {
        val url = ""
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Guess)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Guess

        assertEquals("downloadfile", extractionResult.bestGuess)
    }

    @Test
    fun `extract - url empty string and mime type provided - default name and filetype from mime returned`() {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("jpg", "image/jpeg")

        val url = ""
        val mimeType = "image/jpeg"
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted

        assertEquals("downloadfile.jpg", extractionResult.filename)
    }

    @Test
    fun `extract - url empty string and content disposition provided - extracted`() {
        val url = ""
        val mimeType: String? = null
        val contentDisposition = "Content-Disposition: attachment; filename=fromDisposition.jpg"

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted

        assertEquals("fromDisposition.jpg", extractionResult.filename)
    }

    @Test
    fun `extract - no filename and no path segments - domain name returned`() {
        val url = "http://example.com"
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted

        assertEquals("example.com", extractionResult.filename)
    }

    @Test
    fun `extract - no filename and path segments - returns path name file`() {
        val url = "http://example.com/cat/600/400"
        val mimeType: String? = null
        val contentDisposition: String? = null

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Guess)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Guess

        assertEquals("cat", extractionResult.bestGuess)
    }

    @Test
    fun `extract - url contains filename and mime type text x python 3 and content disposition empty - filename returned`() {
        val url = """
            https://ddg-name-test-ubsgiobgibsdgsbklsdjgm.netlify.app/uploads/qwertyuiopasdfghjklzxcvbnm1234567890qwertyuiopasdfghjklzxcvbnm/bat.py
        """.trimIndent()
        val mimeType = "text/x-python3; charset=UTF-8"
        val contentDisposition = ""

        val extractionResult = testee.extract(buildPendingDownload(url, contentDisposition, mimeType))
        assertTrue(extractionResult is FilenameExtractor.FilenameExtractionResult.Extracted)

        extractionResult as FilenameExtractor.FilenameExtractionResult.Extracted

        assertEquals("bat.py", extractionResult.filename)
    }

    private fun buildPendingDownload(
        url: String,
        contentDisposition: String?,
        mimeType: String?,
    ): PendingFileDownload {
        return PendingFileDownload(
            url = url,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            subfolder = "aFolder",
        )
    }
}
