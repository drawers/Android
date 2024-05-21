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

package com.duckduckgo.experiments.impl

import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.experiments.api.VariantConfig
import com.duckduckgo.experiments.api.VariantManager
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class VariantManagerImplTest {

    private lateinit var testee: VariantManager

    private val mockRandomizer: IndexRandomizer = mock()
    private val appBuildConfig: AppBuildConfig = mock()
    private val activeVariants = mutableListOf<Variant>()
    private val mockExperimentVariantRepository: ExperimentVariantRepository = mock()
    private val mockExperimentFiltersManager: ExperimentFiltersManager = mock()

    @Before
    fun setup() {
        // mock randomizer always returns the first active variant
        whenever(mockRandomizer.random(any())).thenReturn(0)
        whenever(mockExperimentFiltersManager.addFilters(any())).thenReturn { true }

        testee = VariantManagerImpl(
            mockRandomizer,
            appBuildConfig,
            mockExperimentVariantRepository,
            mockExperimentFiltersManager,
        )
    }

    @Test
    fun `getVariantKey - variant already persisted - variant returned`() {
        whenever(mockExperimentVariantRepository.getUserVariant()).thenReturn("variantKey")

        assertEquals("variantKey", testee.getVariantKey())
    }

    @Test
    fun `getVariantKey - variant never persisted - null returned`() {
        whenever(mockExperimentVariantRepository.getUserVariant()).thenReturn(null)

        assertEquals(null, testee.getVariantKey())
    }

    @Test
    fun `getVariantKey - variant is never updated`() {
        testee.getVariantKey()

        verify(mockExperimentVariantRepository, never()).updateVariant(any())
    }

    @Test
    fun `updateVariants - variant already persisted - never invoked`() {
        val variantsConfig = listOf(VariantConfig("variantKey", 1.0))
        whenever(mockExperimentVariantRepository.getUserVariant()).thenReturn("variantKey")
        testee.updateVariants(variantsConfig)

        verify(mockRandomizer, never()).random(any())
    }

    @Test
    fun `givenVariantsUpdate - variant never persisted - variant allocator never invoked`() {
        val variantsConfig = listOf(VariantConfig("variantKey", 1.0))
        whenever(mockExperimentVariantRepository.getUserVariant()).thenReturn(null)
        testee.updateVariants(variantsConfig)

        verify(mockExperimentVariantRepository).updateVariant(any())
        verify(mockRandomizer).random(any())
    }

    @Test
    fun `updateVariants - given return user variant when variants config updated then new variant no allocated`() {
        val variantsConfig = listOf(VariantConfig("variant1", 1.0), VariantConfig("variant2", 1.0))
        whenever(mockExperimentVariantRepository.getUserVariant()).thenReturn("ru")

        testee.updateVariants(variantsConfig)

        verify(mockExperimentVariantRepository, never()).updateVariant(any())
        verify(mockRandomizer, never()).random(any())
    }

    @Test
    fun `updateVariants - no variants available - default variant assigned`() {
        whenever(mockExperimentVariantRepository.getUserVariant()).thenReturn(null)
        testee.updateVariants(emptyList())

        verify(mockExperimentVariantRepository).updateVariant("")
    }

    @Test
    fun `whenVariantPersistedIsNotFoundInActiveVariantListThenRestoredToDefaultVariant - update variant - restored to default`() {
        whenever(mockExperimentVariantRepository.getUserVariant()).thenReturn("variantKey")
        testee.updateVariants(emptyList())

        verify(mockExperimentVariantRepository).updateVariant("")
    }

    @Test
    fun `updateVariants - persisted variant has weight equal to zero in active variant list - not restored`() {
        val variantsConfig = listOf(VariantConfig("variantKey", 0.0))
        whenever(mockExperimentVariantRepository.getUserVariant()).thenReturn("variantKey")
        testee.updateVariants(variantsConfig)

        verify(mockExperimentVariantRepository, never()).updateVariant(any())
    }

    @Test
    fun `updateVariants - no variant persisted - new variant allocated`() {
        val variantsConfig = listOf(VariantConfig("variantKey", 1.0))
        whenever(mockExperimentVariantRepository.getUserVariant()).thenReturn(null)

        testee.updateVariants(variantsConfig)
        verify(mockRandomizer).random(any())
    }

    @Test
    fun `updateVariants - no variant persisted - new variant key allocated and persisted`() {
        val variantsConfig = listOf(VariantConfig("variantKey", 1.0))
        whenever(mockExperimentVariantRepository.getUserVariant()).thenReturn(null)

        testee.updateVariants(variantsConfig)

        verify(mockExperimentVariantRepository).updateVariant("variantKey")
    }

    @Test
    fun `getVariantKey - referrer variant set with no active variants - referrer variant returned`() {
        val referrerVariantKey = "xx"
        mockUpdateScenario(referrerVariantKey)

        val variantKey = testee.getVariantKey()
        assertEquals(referrerVariantKey, variantKey)
    }

    @Test
    fun `getVariantKey - referrer variant set with active variants - referrer variant returned`() {
        val referrerVariantKey = "xx"
        mockUpdateScenario(referrerVariantKey)

        activeVariants.add(Variant("foo", 100.0, filterBy = { true }))
        activeVariants.add(Variant("bar", 100.0, filterBy = { true }))
        val variantKey = testee.getVariantKey()

        assertEquals(referrerVariantKey, variantKey)
    }

    @Test
    fun `updateAppReferrerVariant - data store has its data updated`() {
        testee.updateAppReferrerVariant("xx")

        verify(mockExperimentVariantRepository).updateAppReferrerVariant("xx")
    }

    @Test
    fun `getVariantKey - updating referrer variant - new referrer variant returned`() {
        val originalVariant = testee.getVariantKey()
        mockUpdateScenario("xx")
        val newVariant = testee.getVariantKey()
        Assert.assertNotEquals(originalVariant, newVariant)
        assertEquals("xx", newVariant)
    }

    private fun mockUpdateScenario(key: String) {
        testee.updateAppReferrerVariant(key)
        whenever(mockExperimentVariantRepository.getAppReferrerVariant()).thenReturn(key)
        whenever(mockExperimentVariantRepository.getUserVariant()).thenReturn(key)
    }
}
