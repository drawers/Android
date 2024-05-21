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

package com.duckduckgo.cookies.impl.features

import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.cookies.api.CookiesFeatureName.Cookie
import com.duckduckgo.cookies.store.CookiesFeatureToggleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CookiesFeatureTogglesPluginTest {

    @get:Rule var coroutineRule = CoroutineTestRule()
    lateinit var testee: CookiesFeatureTogglesPlugin

    private val mockFeatureTogglesRepository: CookiesFeatureToggleRepository = mock()
    private val mockAppBuildConfig: AppBuildConfig = mock()

    @Before
    fun before() {
        testee = CookiesFeatureTogglesPlugin(mockFeatureTogglesRepository, mockAppBuildConfig)
    }

    @Test
    fun `isEnabled - is not a cookie feature - return null`() = runTest {
        assertNull(testee.isEnabled(NonPrivacyFeature().value, true))
    }

    @Test
    fun `isEnabled - cookie feature enabled - returns true when enabled`() =
        runTest {
            giveCookieFeatureIsEnabled()

            val isEnabled = testee.isEnabled(Cookie.value, true)

            assertTrue(isEnabled!!)
        }

    @Test
    fun `isEnabled - cookie feature disabled - returns false when disabled`() =
        runTest {
            givenCookieFeatureIsDisabled()

            val isEnabled = testee.isEnabled(Cookie.value, true)

            assertFalse(isEnabled!!)
        }

    @Test
    fun `isEnabled - cookie feature does not exist - return default value`() =
        runTest {
            val defaultValue = true
            givenCookieFeatureReturnsDefaultValue(defaultValue)

            val isEnabled =
                testee.isEnabled(Cookie.value, defaultValue)

            assertEquals(defaultValue, isEnabled)
        }

    @Test
    fun `isEnabled - cookie feature enabled and app version equal to min supported version - returns true when enabled`() =
        runTest {
            giveCookieFeatureIsEnabled()
            givenAppVersionIsEqualToMinSupportedVersion()

            val isEnabled = testee.isEnabled(Cookie.value, true)

            assertTrue(isEnabled!!)
        }

    @Test
    fun `isEnabled - cookie feature enabled and app version greater than min supported version - returns true when enabled`() =
        runTest {
            giveCookieFeatureIsEnabled()
            givenAppVersionIsGreaterThanMinSupportedVersion()

            val isEnabled = testee.isEnabled(Cookie.value, true)

            assertTrue(isEnabled!!)
        }

    @Test
    fun `isEnabled - cookie feature enabled and app version smaller than min supported version - false when enabled`() =
        runTest {
            giveCookieFeatureIsEnabled()
            givenAppVersionIsSmallerThanMinSupportedVersion()

            val isEnabled = testee.isEnabled(Cookie.value, true)

            assertFalse(isEnabled!!)
        }

    private fun giveCookieFeatureIsEnabled() {
        whenever(mockFeatureTogglesRepository.get(Cookie, true)).thenReturn(true)
    }

    private fun givenCookieFeatureIsDisabled() {
        whenever(mockFeatureTogglesRepository.get(Cookie, true)).thenReturn(false)
    }

    private fun givenCookieFeatureReturnsDefaultValue(defaultValue: Boolean) {
        whenever(mockFeatureTogglesRepository.get(Cookie, defaultValue)).thenReturn(defaultValue)
    }

    private fun givenAppVersionIsEqualToMinSupportedVersion() {
        whenever(mockFeatureTogglesRepository.getMinSupportedVersion(Cookie)).thenReturn(1234)
        whenever(mockAppBuildConfig.versionCode).thenReturn(1234)
    }

    private fun givenAppVersionIsGreaterThanMinSupportedVersion() {
        whenever(
            mockFeatureTogglesRepository.getMinSupportedVersion(
                Cookie,
            ),
        ).thenReturn(1234)

        whenever(mockAppBuildConfig.versionCode).thenReturn(5678)
    }

    private fun givenAppVersionIsSmallerThanMinSupportedVersion() {
        whenever(mockFeatureTogglesRepository.getMinSupportedVersion(Cookie)).thenReturn(1234)
        whenever(mockAppBuildConfig.versionCode).thenReturn(123)
    }

    data class NonPrivacyFeature(val value: String = "test")
}
