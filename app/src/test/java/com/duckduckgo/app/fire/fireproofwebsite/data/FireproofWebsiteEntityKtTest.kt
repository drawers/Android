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

package com.duckduckgo.app.fire.fireproofwebsite.data

import org.junit.Assert.*
import org.junit.Test

class FireproofWebsiteEntityKtTest {

    @Test
    fun `website - domain starts with www - drops prefix`() {
        val fireproofWebsiteEntity = FireproofWebsiteEntity("www.example.com")
        val website = fireproofWebsiteEntity.website()
        assertEquals("example.com", website)
    }

    @Test
    fun `whenDomainStartsWithWWWThenDropPrefix - domain starts with www uppercase - drops prefix`() {
        val fireproofWebsiteEntity = FireproofWebsiteEntity("WWW.example.com")
        val website = fireproofWebsiteEntity.website()
        assertEquals("example.com", website)
    }

    @Test
    fun `website - domain does not start with www - unchanged`() {
        val fireproofWebsiteEntity = FireproofWebsiteEntity("mobile.example.com")
        val website = fireproofWebsiteEntity.website()
        assertEquals("mobile.example.com", website)
    }
}
