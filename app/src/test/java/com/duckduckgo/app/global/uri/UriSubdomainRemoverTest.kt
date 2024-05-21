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

package com.duckduckgo.app.global.uri

import android.net.Uri
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UriSubdomainRemoverTest {

    @Test
    fun `removeSubdomain - two available subdomains - one returned`() {
        val converted = Uri.parse("https://a.example.com").removeSubdomain()
        assertEquals("https://example.com", converted)
    }

    @Test
    fun `removeSubdomain - five available - four returned`() {
        val converted = Uri.parse("https://a.b.c.d.example.com").removeSubdomain()
        assertEquals("https://b.c.d.example.com", converted)
    }

    @Test
    fun `removeSubdomain - multiple subdomains removed - keeps calling`() {
        val converted = Uri.parse("https://a.b.c.d.example.com")
            .removeSubdomain()!!
            .toUri().removeSubdomain()!!
            .toUri().removeSubdomain()
        assertEquals("https://d.example.com", converted)
    }

    @Test
    fun `removeSubdomain - only one subdomain exists - returns null`() {
        val converted = Uri.parse("https://example.com").removeSubdomain()
        assertNull(converted)
    }

    @Test
    fun `removeSubdomain - only one subdomain exists but has multipart TLD co·uk - returns null`() {
        val converted = Uri.parse("https://co.uk").removeSubdomain()
        assertNull(converted)
    }

    @Test
    fun `removeSubdomain - only one exists but has multipart TLD - returns multipart TLD`() {
        val converted = Uri.parse("https://co.za").removeSubdomain()
        assertNull(converted)
    }

    @Test
    fun `removeSubdomain - recent TLD - returns null`() {
        val converted = Uri.parse("https://example.dev").removeSubdomain()
        assertNull(converted)
    }

    @Test
    fun `removeSubdomain - unknown TLD - returns null`() {
        val converted = Uri.parse("https://example.nonexistent").removeSubdomain()
        assertNull(converted)
    }

    @Test
    fun `removeSubdomain - unknown TLD - returns non-existent TLD`() {
        val converted = Uri.parse("https://foo.example.nonexistent").removeSubdomain()
        assertEquals("https://example.nonexistent", converted)
    }

    @Test
    fun `parse - uri ip address removed - returns null`() {
        val converted = Uri.parse("127.0.0.1").removeSubdomain()
        assertNull(converted)
    }
}
