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

package com.duckduckgo.app.browser.filechooser

import android.content.ClipData
import android.content.ClipData.Item
import android.content.ClipDescription
import android.content.ClipDescription.MIMETYPE_TEXT_URILIST
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileChooserIntentBuilderTest {

    private lateinit var testee: FileChooserIntentBuilder

    @Before
    fun setup() {
        testee = FileChooserIntentBuilder()
    }

    @Test
    fun `intent - built - read uri permission flag set`() {
        val output = testee.intent(emptyArray())
        assertTrue("Intent.FLAG_GRANT_READ_URI_PERMISSION flag not set on intent", output.hasFlagSet(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    @Test
    fun `intent - built - accept type set to all`() {
        val output = testee.intent(emptyArray())
        assertEquals("*/*", output.type)
    }

    @Test
    fun `intent - multiple mode disabled - returns false`() {
        val output = testee.intent(emptyArray(), canChooseMultiple = false)
        assertFalse(output.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
    }

    @Test
    fun `intent - requested types missing - should not add mime type extra`() {
        val output = testee.intent(emptyArray())
        assertFalse(output.hasExtra(Intent.EXTRA_MIME_TYPES))
    }

    @Test
    fun `intent - requested types present - should add mime type extra`() {
        val output = testee.intent(arrayOf("image/png", "image/gif"))
        assertTrue(output.hasExtra(Intent.EXTRA_MIME_TYPES))
    }

    @Test
    fun `intent - upper case types given - normalised to lowercase`() {
        val output = testee.intent(arrayOf("ImAgE/PnG"))
        assertEquals("image/png", output.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)!![0])
    }

    @Test
    fun `intent - empty types given - not included in output`() {
        val output = testee.intent(arrayOf("image/png", "", " ", "image/gif"))
        val mimeTypes = output.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)
        assertEquals(2, mimeTypes!!.size)
        assertEquals("image/png", mimeTypes[0])
        assertEquals("image/gif", mimeTypes[1])
    }

    @Test
    fun `intent - multiple mode enabled - returns true`() {
        val output = testee.intent(emptyArray(), canChooseMultiple = true)
        assertTrue(output.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
    }

    @Test
    fun `extractSelectedFileUris - single URI empty clip - single URI returned`() {
        val intent = buildIntent("a")
        val extractedUris = testee.extractSelectedFileUris(intent)

        assertEquals(1, extractedUris!!.size)
        assertEquals("a", extractedUris.first().toString())
    }

    @Test
    fun `extractSelectedFileUris - single URI non-empty clip - URI returned`() {
        val intent = buildIntent("a", listOf("b"))
        val extractedUris = testee.extractSelectedFileUris(intent)

        assertEquals(1, extractedUris!!.size)
        assertEquals("b", extractedUris.first().toString())
    }

    @Test
    fun `extractSelectedFileUris - multiple clip items - correct URIs returned`() {
        val intent = buildIntent("a", listOf("b", "c"))
        val extractedUris = testee.extractSelectedFileUris(intent)

        assertEquals(2, extractedUris!!.size)
        assertEquals("b", extractedUris[0].toString())
        assertEquals("c", extractedUris[1].toString())
    }

    @Test
    fun `extractSelectedFileUris - single URI missing but clip data available - URI returned from clip`() {
        val intent = buildIntent(clipData = listOf("b"))
        val extractedUris = testee.extractSelectedFileUris(intent)

        assertEquals(1, extractedUris!!.size)
        assertEquals("b", extractedUris.first().toString())
    }

    @Test
    fun `extractSelectedFileUris - no data or clip data - null uri returned`() {
        val intent = buildIntent(data = null, clipData = null)
        val extractedUris = testee.extractSelectedFileUris(intent)

        assertNull(extractedUris)
    }

    /**
     * Helper function to build an `Intent` which contains one or more of `data` and `clipData` values.
     *
     * This is a bit messy but the Intent APIs are messy themselves; at least this contains the mess to this one helper function
     */

    private fun buildIntent(
        data: String? = null,
        clipData: List<String>? = null,
    ): Intent {
        return Intent().also {
            if (data != null) {
                it.data = data.toUri()
            }

            if (clipData != null && clipData.isNotEmpty()) {
                val clipDescription = ClipDescription("", arrayOf(MIMETYPE_TEXT_URILIST))
                it.clipData = ClipData(clipDescription, Item(Uri.parse(clipData.first())))

                for (i in 1 until clipData.size) {
                    it.clipData?.addItem(Item(Uri.parse(clipData[i])))
                }
            }
        }
    }

    private fun Intent.hasFlagSet(expectedFlag: Int): Boolean {
        val actual = flags and expectedFlag
        return expectedFlag == actual
    }
}
