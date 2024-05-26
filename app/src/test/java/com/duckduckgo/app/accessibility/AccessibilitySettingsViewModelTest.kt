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

package com.duckduckgo.app.accessibility

import app.cash.turbine.test
import com.duckduckgo.app.accessibility.data.AccessibilitySettingsDataStore
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.voice.api.VoiceSearchAvailability
import com.duckduckgo.voice.impl.VoiceSearchPixelNames
import com.duckduckgo.voice.store.VoiceSearchRepository
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalTime
class AccessibilitySettingsViewModelTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private val voiceSearchRepository: VoiceSearchRepository = mock()
    private val voiceSearchAvailability: VoiceSearchAvailability = mock()
    private val accessibilitySettings: AccessibilitySettingsDataStore = mock()
    private val pixel: Pixel = mock()
    private val testee = AccessibilitySettingsViewModel(accessibilitySettings, voiceSearchAvailability, voiceSearchRepository, pixel)

    @Test
    fun `viewState - default view state emitted`() = runTest {
        val viewState = AccessibilitySettingsViewModel.ViewState(
            overrideSystemFontSize = false,
            appFontSize = 100f,
            forceZoom = false,
            voiceSearchEnabled = false,
            showVoiceSearch = false,
        )
        testee.viewState().test {
            assertEquals(viewState, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `start - view state emitted`() = runTest {
        val viewState = AccessibilitySettingsViewModel.ViewState(
            overrideSystemFontSize = true,
            appFontSize = 150f,
            forceZoom = true,
            voiceSearchEnabled = true,
            showVoiceSearch = true,
        )
        whenever(accessibilitySettings.overrideSystemFontSize).thenReturn(true)
        whenever(accessibilitySettings.appFontSize).thenReturn(150f)
        whenever(accessibilitySettings.forceZoom).thenReturn(true)
        whenever(voiceSearchAvailability.isVoiceSearchSupported).thenReturn(true)
        whenever(voiceSearchAvailability.isVoiceSearchAvailable).thenReturn(true)

        testee.start()

        testee.viewState().test {
            assertEquals(viewState, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onForceZoomChanged - view state emitted`() = runTest {
        val viewState = defaultViewState()
        whenever(accessibilitySettings.forceZoom).thenReturn(true)

        testee.onForceZoomChanged(true)

        testee.viewState().test {
            assertEquals(viewState.copy(forceZoom = true), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onForceZoomChanged - settings updated`() = runTest {
        whenever(accessibilitySettings.forceZoom).thenReturn(true)

        testee.onForceZoomChanged(true)

        verify(accessibilitySettings).forceZoom = true
        verify(accessibilitySettings).forceZoom
    }

    @Test
    fun `onSystemFontSizeChanged - view state emitted`() = runTest {
        val viewState = defaultViewState()
        whenever(accessibilitySettings.overrideSystemFontSize).thenReturn(true)

        testee.onSystemFontSizeChanged(true)

        testee.viewState().test {
            assertEquals(viewState.copy(overrideSystemFontSize = true), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onSystemFontSizeChanged - settings updated`() = runTest {
        whenever(accessibilitySettings.overrideSystemFontSize).thenReturn(true)

        testee.onSystemFontSizeChanged(true)

        verify(accessibilitySettings).overrideSystemFontSize = true
        verify(accessibilitySettings).overrideSystemFontSize
    }

    @Test
    fun `onFontSizeChanged - view state emitted`() = runTest {
        val viewState = defaultViewState()
        whenever(accessibilitySettings.appFontSize).thenReturn(150f)

        testee.onFontSizeChanged(150f)

        testee.viewState().test {
            assertEquals(viewState.copy(appFontSize = 150f), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onFontSizeChanged - settings updated`() = runTest {
        whenever(accessibilitySettings.appFontSize).thenReturn(150f)

        testee.onFontSizeChanged(150f)

        verify(accessibilitySettings).appFontSize = 150f
        verify(accessibilitySettings).appFontSize
    }

    @Test
    fun `onVoiceSearchChanged - view state emitted`() = runTest {
        val viewState = defaultViewState()
        whenever(voiceSearchAvailability.isVoiceSearchAvailable).thenReturn(true)

        testee.onVoiceSearchChanged(true)

        testee.viewState().test {
            assertEquals(viewState.copy(voiceSearchEnabled = true), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onVoiceSearchChanged - settings updated`() = runTest {
        testee.onVoiceSearchChanged(true)
        verify(voiceSearchRepository).setVoiceSearchUserEnabled(true)
    }

    @Test
    fun whenVoiceSearchDisabledThenSettingsUpdated() = runTest {
        testee.onVoiceSearchChanged(false)
        verify(voiceSearchRepository).setVoiceSearchUserEnabled(false)
    }

    @Test
    fun `onVoiceSearchChanged - fire pixel`() = runTest {
        testee.onVoiceSearchChanged(true)
        verify(pixel).fire(VoiceSearchPixelNames.VOICE_SEARCH_ON)
    }

    @Test
    fun `onVoiceSearchChanged - voice search disabled - fire pixel`() = runTest {
        testee.onVoiceSearchChanged(false)
        verify(pixel).fire(VoiceSearchPixelNames.VOICE_SEARCH_OFF)
    }

    private fun defaultViewState() = AccessibilitySettingsViewModel.ViewState()
}
