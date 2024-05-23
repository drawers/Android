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

package com.duckduckgo.privacy.config.impl.plugins

import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.privacy.config.api.PrivacyFeatureName
import com.duckduckgo.privacy.config.store.PrivacyFeatureTogglesRepository
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

class PrivacyFeatureTogglesPluginTest {

    @get:Rule var coroutineRule = CoroutineTestRule()
    lateinit var testee: PrivacyFeatureTogglesPlugin

    private val mockFeatureTogglesRepository: PrivacyFeatureTogglesRepository = mock()
    private val mockAppBuildConfig: AppBuildConfig = mock()

    @Before
    fun before() {
        testee = PrivacyFeatureTogglesPlugin(mockFeatureTogglesRepository, mockAppBuildConfig)
    }

    @Test
    fun `isEnabled - feature is not a privacy feature - return null`() = runTest {
        assertNull(testee.isEnabled(NonPrivacyFeature().value, true))
    }

    @Test
    fun `isEnabled - privacy feature enabled - returns true`() =
        runTest {
            givenPrivacyFeatureIsEnabled()

            val isEnabled = testee.isEnabled(PrivacyFeatureName.ContentBlockingFeatureName.value, true)

            assertTrue(isEnabled!!)
        }

    @Test
    fun `isEnabled - privacy feature disabled - return false`() =
        runTest {
            givenPrivacyFeatureIsDisabled()

            val isEnabled = testee.isEnabled(PrivacyFeatureName.ContentBlockingFeatureName.value, true)

            assertFalse(isEnabled!!)
        }

    @Test
    fun `isEnabled - privacy feature does not exist - return default value`() =
        runTest {
            val defaultValue = true
            givenPrivacyFeatureReturnsDefaultValue(defaultValue)

            val isEnabled =
                testee.isEnabled(PrivacyFeatureName.ContentBlockingFeatureName.value, defaultValue)

            assertEquals(defaultValue, isEnabled)
        }

    @Test
    fun `isEnabled - privacy feature and app version equal to min supported version - return true`() =
        runTest {
            givenPrivacyFeatureIsEnabled()
            givenAppVersionIsEqualToMinSupportedVersion()

            val isEnabled = testee.isEnabled(PrivacyFeatureName.ContentBlockingFeatureName.value, true)

            assertTrue(isEnabled!!)
        }

    @Test
    fun `isEnabled - privacy feature enabled and app version greater than min supported - returns true`() =
        runTest {
            givenPrivacyFeatureIsEnabled()
            givenAppVersionIsGreaterThanMinSupportedVersion()

            val isEnabled = testee.isEnabled(PrivacyFeatureName.ContentBlockingFeatureName.value, true)

            assertTrue(isEnabled!!)
        }

    @Test
    fun `isEnabled - privacy feature enabled and app version smaller than min supported version - return false`() =
        runTest {
            givenPrivacyFeatureIsEnabled()
            givenAppVersionIsSmallerThanMinSupportedVersion()

            val isEnabled = testee.isEnabled(PrivacyFeatureName.ContentBlockingFeatureName.value, true)

            assertFalse(isEnabled!!)
        }

    private fun givenPrivacyFeatureIsEnabled() {
        whenever(
            mockFeatureTogglesRepository.get(
                PrivacyFeatureName.ContentBlockingFeatureName,
                true,
            ),
        ).thenReturn(true)
    }

    private fun givenPrivacyFeatureIsDisabled() {
        whenever(
            mockFeatureTogglesRepository.get(
                PrivacyFeatureName.ContentBlockingFeatureName,
                true,
            ),
        ).thenReturn(false)
    }

    private fun givenPrivacyFeatureReturnsDefaultValue(defaultValue: Boolean) {
        whenever(
            mockFeatureTogglesRepository.get(
                PrivacyFeatureName.ContentBlockingFeatureName,
                defaultValue,
            ),
        ).thenReturn(defaultValue)
    }

    private fun givenAppVersionIsEqualToMinSupportedVersion() {
        whenever(
            mockFeatureTogglesRepository.getMinSupportedVersion(
                PrivacyFeatureName.ContentBlockingFeatureName,
            ),
        ).thenReturn(1234)

        whenever(mockAppBuildConfig.versionCode).thenReturn(1234)
    }

    private fun givenAppVersionIsGreaterThanMinSupportedVersion() {
        whenever(
            mockFeatureTogglesRepository.getMinSupportedVersion(
                PrivacyFeatureName.ContentBlockingFeatureName,
            ),
        ).thenReturn(1234)

        whenever(mockAppBuildConfig.versionCode).thenReturn(5678)
    }

    private fun givenAppVersionIsSmallerThanMinSupportedVersion() {
        whenever(
            mockFeatureTogglesRepository.getMinSupportedVersion(
                PrivacyFeatureName.ContentBlockingFeatureName,
            ),
        ).thenReturn(1234)

        whenever(mockAppBuildConfig.versionCode).thenReturn(123)
    }

    data class NonPrivacyFeature(val value: String = "test")
}
