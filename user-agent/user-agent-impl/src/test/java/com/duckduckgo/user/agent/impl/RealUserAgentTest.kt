/*
 * Copyright (c) 2024 DuckDuckGo
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

package com.duckduckgo.user.agent.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.feature.toggles.api.FeatureExceptions.FeatureException
import com.duckduckgo.privacy.config.api.UnprotectedTemporary
import com.duckduckgo.user.agent.store.UserAgentRepository
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class RealUserAgentTest {
    private val mockUserAgentRepository: UserAgentRepository = mock()
    private val mockUnprotectedTemporary: UnprotectedTemporary = mock()
    lateinit var testee: RealUserAgent

    @Before
    fun before() {
        testee = RealUserAgent(mockUserAgentRepository, mockUnprotectedTemporary)
    }

    @Test
    fun `isException - domain listed in exceptions list - returns true`() {
        givenThereAreExceptions()

        assertTrue(testee.isException("http://www.example.com"))
    }

    @Test
    fun `isException - subdomain and domain listed in exceptions list - returns true`() {
        givenThereAreExceptions()

        assertTrue(testee.isException("http://test.example.com"))
    }

    @Test
    fun `isException - domain not listed in exceptions list - returns false`() {
        whenever(mockUserAgentRepository.exceptions).thenReturn(CopyOnWriteArrayList())

        assertFalse(testee.isException("http://test.example.com"))
    }

    @Test
    fun `isException - is listed in unprotected temporary list`() {
        val url = "http://example.com"
        whenever(mockUnprotectedTemporary.isAnException(url)).thenReturn(true)
        whenever(mockUserAgentRepository.exceptions).thenReturn(CopyOnWriteArrayList())

        assertTrue(testee.isException(url))
    }

    @Test
    fun `useLegacyUserAgent - site in legacy sites list`() {
        val url = "http://duckduckgo.com"

        assertTrue(testee.useLegacyUserAgent(url))
    }

    @Test
    fun `whenSubdomainInLegacySitesList - use legacy user agent`() {
        val url = "http://test.duckduckgo.com"

        assertTrue(testee.useLegacyUserAgent(url))
    }

    @Test
    fun `when site not in legacy sites list - do not use legacy user agent`() {
        val url = "http://example.com"

        assertFalse(testee.useLegacyUserAgent(url))
    }

    private fun givenThereAreExceptions() {
        val exceptions =
            CopyOnWriteArrayList<FeatureException>().apply {
                add(FeatureException("example.com", "my reason here"))
            }
        whenever(mockUserAgentRepository.exceptions).thenReturn(exceptions)
    }
}
