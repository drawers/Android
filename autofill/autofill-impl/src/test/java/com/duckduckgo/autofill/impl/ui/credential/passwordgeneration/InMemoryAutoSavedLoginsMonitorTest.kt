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

package com.duckduckgo.autofill.impl.ui.credential.passwordgeneration

import org.junit.Assert.*
import org.junit.Test

class InMemoryAutoSavedLoginsMonitorTest {
    private val testee = InMemoryAutoSavedLoginsMonitor()

    @Test
    fun `setAutoSavedLoginId - get auto saved login id returned`() {
        val loginId: Long = 1
        val tabId = "abc"
        testee.setAutoSavedLoginId(loginId, tabId)
        assertEquals(loginId, testee.getAutoSavedLoginId(tabId))
    }

    @Test
    fun `setAutoSavedLoginId - cleared - not returned from get function`() {
        val loginId: Long = 1
        val tabId = "abc"
        testee.setAutoSavedLoginId(loginId, tabId)
        testee.clearAutoSavedLoginId(tabId)
        assertNull(testee.getAutoSavedLoginId(tabId))
    }
}
