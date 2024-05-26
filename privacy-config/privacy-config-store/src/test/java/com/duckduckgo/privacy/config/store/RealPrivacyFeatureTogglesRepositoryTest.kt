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

package com.duckduckgo.privacy.config.store

import com.duckduckgo.privacy.config.api.PrivacyFeatureName
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class RealPrivacyFeatureTogglesRepositoryTest {

    lateinit var testee: RealPrivacyFeatureTogglesRepository

    private val mockPrivacyFeatureTogglesDataStore: PrivacyFeatureTogglesDataStore = mock()

    @Before
    fun before() {
        testee = RealPrivacyFeatureTogglesRepository(mockPrivacyFeatureTogglesDataStore)
    }

    @Test
    fun `deleteAll - deleteAll called`() {
        testee.deleteAll()

        verify(mockPrivacyFeatureTogglesDataStore).deleteAll()
    }

    @Test
    fun `get - get called`() {
        testee.get(PrivacyFeatureName.GpcFeatureName, true)

        verify(mockPrivacyFeatureTogglesDataStore).get(PrivacyFeatureName.GpcFeatureName, true)
    }

    @Test
    fun `insert - insert called`() {
        val privacyFeatureToggle = PrivacyFeatureToggles(PrivacyFeatureName.GpcFeatureName.value, true, null)
        testee.insert(privacyFeatureToggle)

        verify(mockPrivacyFeatureTogglesDataStore).insert(privacyFeatureToggle)
    }
}
