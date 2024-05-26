/*
 * Copyright (c) 2019 DuckDuckGo
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

package com.duckduckgo.app.onboarding.ui

import com.duckduckgo.app.browser.defaultbrowsing.DefaultBrowserDetector
import com.duckduckgo.app.global.DefaultRoleBrowserDialog
import com.duckduckgo.app.onboarding.ui.page.experiment.ExtendedOnboardingExperimentVariantManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class OnboardingPageManagerTest {

    private lateinit var testee: OnboardingPageManager
    private val onboardingPageBuilder: OnboardingPageBuilder = mock()
    private val mockDefaultBrowserDetector: DefaultBrowserDetector = mock()
    private val defaultRoleBrowserDialog: DefaultRoleBrowserDialog = mock()
    private val mockExtendedOnboardingExperimentVariantManager: ExtendedOnboardingExperimentVariantManager = mock()

    @Before
    fun setup() {
        whenever(mockExtendedOnboardingExperimentVariantManager.isComparisonChartEnabled()).thenReturn(false)
        testee = OnboardingPageManagerWithTrackerBlocking(
            defaultRoleBrowserDialog,
            onboardingPageBuilder,
            mockDefaultBrowserDetector,
            mockExtendedOnboardingExperimentVariantManager,
        )
    }

    @Test
    fun `buildPageBlueprints - DDG not default browser - expected onboarding pages are two`() {
        configureDeviceSupportsDefaultBrowser()
        whenever(mockDefaultBrowserDetector.isDefaultBrowser()).thenReturn(false)
        whenever(defaultRoleBrowserDialog.shouldShowDialog()).thenReturn(false)

        testee.buildPageBlueprints()

        assertEquals(2, testee.pageCount())
    }

    @Test
    fun `buildPageBlueprints - DDG not default browser - expected onboarding pages 1`() {
        configureDeviceSupportsDefaultBrowser()
        whenever(mockDefaultBrowserDetector.isDefaultBrowser()).thenReturn(false)
        whenever(defaultRoleBrowserDialog.shouldShowDialog()).thenReturn(true)

        testee.buildPageBlueprints()

        assertEquals(1, testee.pageCount())
    }

    @Test
    fun `buildPageBlueprints - default browser - single page onboarding`() {
        configureDeviceSupportsDefaultBrowser()
        whenever(mockDefaultBrowserDetector.isDefaultBrowser()).thenReturn(true)
        whenever(defaultRoleBrowserDialog.shouldShowDialog()).thenReturn(false)

        testee.buildPageBlueprints()

        assertEquals(1, testee.pageCount())
    }

    @Test
    fun `buildPageBlueprints - default browser and dialog - single page onboarding`() {
        configureDeviceSupportsDefaultBrowser()
        whenever(mockDefaultBrowserDetector.isDefaultBrowser()).thenReturn(true)
        whenever(defaultRoleBrowserDialog.shouldShowDialog()).thenReturn(true)

        testee.buildPageBlueprints()

        assertEquals(1, testee.pageCount())
    }

    @Test
    fun `buildPageBlueprints - device does not support default browser - single page onboarding`() {
        configureDeviceDoesNotSupportDefaultBrowser()
        whenever(defaultRoleBrowserDialog.shouldShowDialog()).thenReturn(false)

        testee.buildPageBlueprints()

        assertEquals(1, testee.pageCount())
    }

    @Test
    fun `buildPageBlueprints - device does not support default browser and should show browser dialog - single page onboarding`() {
        configureDeviceDoesNotSupportDefaultBrowser()
        whenever(defaultRoleBrowserDialog.shouldShowDialog()).thenReturn(true)

        testee.buildPageBlueprints()

        assertEquals(1, testee.pageCount())
    }

    private fun configureDeviceSupportsDefaultBrowser() {
        whenever(mockDefaultBrowserDetector.deviceSupportsDefaultBrowserConfiguration()).thenReturn(true)
    }

    private fun configureDeviceDoesNotSupportDefaultBrowser() {
        whenever(mockDefaultBrowserDetector.deviceSupportsDefaultBrowserConfiguration()).thenReturn(false)
    }
}
