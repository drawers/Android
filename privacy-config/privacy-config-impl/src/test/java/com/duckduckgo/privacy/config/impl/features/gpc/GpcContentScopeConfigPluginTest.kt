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

package com.duckduckgo.privacy.config.impl.features.gpc

import com.duckduckgo.privacy.config.store.features.gpc.GpcRepository
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GpcContentScopeConfigPluginTest {

    lateinit var testee: GpcContentScopeConfigPlugin

    private val mockGpcRepository: GpcRepository = mock()

    @Before
    fun before() {
        testee = GpcContentScopeConfigPlugin(mockGpcRepository)
    }

    @Test
    fun `config - return correctly formatted json`() {
        whenever(mockGpcRepository.gpcContentScopeConfig).thenReturn(config)
        assertEquals("\"gpc\":$config", testee.config())
    }

    @Test
    fun `preferences - GPC enabled - correctly formatted JSON`() {
        whenever(mockGpcRepository.isGpcEnabled()).thenReturn(true)
        assertEquals(testee.preferences(), "\"globalPrivacyControlValue\":true")
    }

    @Test
    fun `preferences - GPC disabled - correctly formatted JSON`() {
        whenever(mockGpcRepository.isGpcEnabled()).thenReturn(false)
        assertEquals(testee.preferences(), "\"globalPrivacyControlValue\":false")
    }

    companion object {
        const val config = "{\"exceptions\":[{\"domain\":\"example.com\"}]," +
            "\"settings\":{\"gpcHeaderEnabledSites\":[\"foo.com\"]}," +
            "\"state\":\"enabled\"}"
    }
}
