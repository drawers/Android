/*
 * Copyright (c) 2021 DuckDuckGo
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.privacy.db.UserAllowListRepository
import com.duckduckgo.feature.toggles.api.FeatureToggle
import com.duckduckgo.privacy.config.api.GpcException
import com.duckduckgo.privacy.config.api.GpcHeaderEnabledSite
import com.duckduckgo.privacy.config.api.PrivacyFeatureName
import com.duckduckgo.privacy.config.api.UnprotectedTemporary
import com.duckduckgo.privacy.config.impl.features.gpc.RealGpc.Companion.GPC_HEADER
import com.duckduckgo.privacy.config.impl.features.gpc.RealGpc.Companion.GPC_HEADER_VALUE
import com.duckduckgo.privacy.config.store.features.gpc.GpcRepository
import java.util.concurrent.CopyOnWriteArrayList
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class RealGpcTest {
    private val mockGpcRepository: GpcRepository = mock()
    private val mockFeatureToggle: FeatureToggle = mock()
    private val mockUnprotectedTemporary: UnprotectedTemporary = mock()
    private val mockUserAllowListRepository: UserAllowListRepository = mock()
    lateinit var testee: RealGpc

    @Before
    fun setup() {
        val exceptions =
            CopyOnWriteArrayList<GpcException>().apply { add(GpcException(EXCEPTION_URL)) }
        val headers =
            CopyOnWriteArrayList<GpcHeaderEnabledSite>().apply { add(GpcHeaderEnabledSite(VALID_CONSUMER_URL)) }
        whenever(mockGpcRepository.exceptions).thenReturn(exceptions)
        whenever(mockGpcRepository.headerEnabledSites).thenReturn(headers)

        testee =
            RealGpc(mockFeatureToggle, mockGpcRepository, mockUnprotectedTemporary, mockUserAllowListRepository)
    }

    @Test
    fun `isEnabled - is GPC enabled called`() {
        testee.isEnabled()
        verify(mockGpcRepository).isGpcEnabled()
    }

    @Test
    fun `enableGpc - enable gpc called`() {
        testee.enableGpc()
        verify(mockGpcRepository).enableGpc()
    }

    @Test
    fun `disableGpc - disable gpc called`() {
        testee.disableGpc()
        verify(mockGpcRepository).disableGpc()
    }

    @Test
    fun `getHeaders - feature and gpc enabled - url in exceptions - return empty map`() {
        givenFeatureAndGpcAreEnabled()

        val result = testee.getHeaders(EXCEPTION_URL)

        assertEquals(0, result.size)
    }

    @Test
    fun `getHeaders - feature and GPC enabled - returns map with headers`() {
        givenFeatureAndGpcAreEnabled()

        val result = testee.getHeaders("test.com")

        assertTrue(result.containsKey(GPC_HEADER))
        assertEquals(GPC_HEADER_VALUE, result[GPC_HEADER])
    }

    @Test
    fun `getHeaders - feature enabled - gpc not enabled - url not in exceptions - empty map`() {
        givenFeatureIsEnabledButGpcIsNot()

        val result = testee.getHeaders("test.com")

        assertEquals(0, result.size)
    }

    @Test
    fun `getHeaders - feature not enabled and GPC is enabled - return empty map`() {
        givenFeatureIsNotEnabledButGpcIsEnabled()

        val result = testee.getHeaders("test.com")

        assertEquals(0, result.size)
    }

    @Test
    fun `canUrlAddHeaders - feature and gpc enabled - url in exceptions - returns false`() {
        givenFeatureAndGpcAreEnabled()

        assertFalse(testee.canUrlAddHeaders(EXCEPTION_URL, emptyMap()))
    }

    @Test
    fun `canUrlAddHeaders - feature and gpc enabled - header already exists - returns false`() {
        givenFeatureAndGpcAreEnabled()

        assertFalse(
            testee.canUrlAddHeaders(VALID_CONSUMER_URL, mapOf(GPC_HEADER to GPC_HEADER_VALUE)),
        )
    }

    @Test
    fun `canUrlAddHeaders - feature and GPC enabled - returns true`() {
        givenFeatureAndGpcAreEnabled()

        assertTrue(testee.canUrlAddHeaders(VALID_CONSUMER_URL, emptyMap()))
    }

    @Test
    fun `canUrlAddHeaders - feature and GPC enabled - URL in not in consumers list - return false`() {
        givenFeatureAndGpcAreEnabled()

        assertFalse(testee.canUrlAddHeaders("test.com", emptyMap()))
    }

    @Test
    fun `canUrlAddHeaders - feature and gpc enabled - url in exceptions list - returns false`() {
        val exceptions =
            CopyOnWriteArrayList<GpcException>().apply { add(GpcException(VALID_CONSUMER_URL)) }
        whenever(mockGpcRepository.exceptions).thenReturn(exceptions)
        givenFeatureAndGpcAreEnabled()

        assertFalse(testee.canUrlAddHeaders(VALID_CONSUMER_URL, emptyMap()))
    }

    @Test
    fun `canUrlAddHeaders - feature not enabled and GPC is enabled and URL in consumers list and header does not exist - returns false`() {
        givenFeatureIsNotEnabledButGpcIsEnabled()

        assertFalse(testee.canUrlAddHeaders(VALID_CONSUMER_URL, emptyMap()))
    }

    @Test
    fun `canUrlAddHeaders - feature enabled gpc not enabled url in consumers list header does not exist - returns false`() {
        givenFeatureIsEnabledButGpcIsNot()

        assertFalse(testee.canUrlAddHeaders(VALID_CONSUMER_URL, emptyMap()))
    }

    @Test
    fun `canGpcBeUsedByUrl - feature and gpc enabled - returns true`() {
        givenFeatureAndGpcAreEnabled()

        assertTrue(testee.canGpcBeUsedByUrl("test.com"))
    }

    @Test
    fun `canGpcBeUsedByUrl - feature and gpc enabled - returns false`() {
        givenFeatureAndGpcAreEnabled()

        assertFalse(testee.canGpcBeUsedByUrl(EXCEPTION_URL))
    }

    @Test
    fun `canGpcBeUsedByUrl - feature enabled gpc not enabled url not exception - returns false`() {
        givenFeatureIsEnabledButGpcIsNot()

        assertFalse(testee.canGpcBeUsedByUrl("test.com"))
    }

    @Test
    fun `canGpcBeUsedByUrl - feature not enabled and GPC is enabled - returns false`() {
        givenFeatureIsNotEnabledButGpcIsEnabled()

        assertFalse(testee.canGpcBeUsedByUrl("test.com"))
    }

    @Test
    fun `canGpcBeUsedByUrl - url in unprotected temporary - returns false`() {
        givenFeatureAndGpcAreEnabled()
        whenever(mockUnprotectedTemporary.isAnException(VALID_CONSUMER_URL)).thenReturn(true)

        assertFalse(testee.canGpcBeUsedByUrl(VALID_CONSUMER_URL))
    }

    @Test
    fun `isAnException - is domain in user allow list - returns true`() {
        whenever(mockUserAllowListRepository.isUrlInUserAllowList(anyString())).thenReturn(true)
        assertTrue(testee.isAnException("test.com"))
    }

    private fun givenFeatureAndGpcAreEnabled() {
        whenever(mockFeatureToggle.isFeatureEnabled(PrivacyFeatureName.GpcFeatureName.value, true))
            .thenReturn(true)
        whenever(mockGpcRepository.isGpcEnabled()).thenReturn(true)
    }

    private fun givenFeatureIsEnabledButGpcIsNot() {
        whenever(mockFeatureToggle.isFeatureEnabled(PrivacyFeatureName.GpcFeatureName.value, true))
            .thenReturn(true)
        whenever(mockGpcRepository.isGpcEnabled()).thenReturn(false)
    }

    private fun givenFeatureIsNotEnabledButGpcIsEnabled() {
        whenever(mockFeatureToggle.isFeatureEnabled(PrivacyFeatureName.GpcFeatureName.value, true))
            .thenReturn(false)
        whenever(mockGpcRepository.isGpcEnabled()).thenReturn(true)
    }

    companion object {
        const val EXCEPTION_URL = "example.com"
        const val VALID_CONSUMER_URL = "global-privacy-control.glitch.me"
    }
}
