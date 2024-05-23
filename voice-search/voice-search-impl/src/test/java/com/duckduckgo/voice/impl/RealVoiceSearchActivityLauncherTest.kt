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

package com.duckduckgo.voice.impl

import android.app.Activity
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.voice.api.VoiceSearchLauncher.Event
import com.duckduckgo.voice.api.VoiceSearchLauncher.Source.BROWSER
import com.duckduckgo.voice.api.VoiceSearchLauncher.Source.WIDGET
import com.duckduckgo.voice.impl.ActivityResultLauncherWrapper.Action.LaunchVoiceSearch
import com.duckduckgo.voice.impl.fakes.FakeActivityResultLauncherWrapper
import com.duckduckgo.voice.impl.listeningmode.VoiceSearchActivity
import com.duckduckgo.voice.impl.listeningmode.ui.VoiceSearchBackgroundBlurRenderer
import com.duckduckgo.voice.store.VoiceSearchRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RealVoiceSearchActivityLauncherTest {
    @Mock
    private lateinit var blurRenderer: VoiceSearchBackgroundBlurRenderer

    @Mock
    private lateinit var pixel: Pixel

    private lateinit var activityResultLauncherWrapper: FakeActivityResultLauncherWrapper

    private lateinit var testee: RealVoiceSearchActivityLauncher

    @Mock
    private lateinit var voiceSearchRepository: VoiceSearchRepository

    @Mock
    private lateinit var dialogLauncher: VoiceSearchPermissionDialogsLauncher

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        activityResultLauncherWrapper = FakeActivityResultLauncherWrapper()
        testee = RealVoiceSearchActivityLauncher(
            blurRenderer,
            pixel,
            activityResultLauncherWrapper,
            voiceSearchRepository,
            dialogLauncher,
        )
    }

    @Test
    fun `registerResultsCallback - result from voice search browser is OK and not empty - emit voice recognition success`() {
        var lastKnownEvent: Event? = null
        testee.registerResultsCallback(mock(), mock(), BROWSER) {
            lastKnownEvent = it
        }

        val lastKnownRequest = activityResultLauncherWrapper.lastKnownRequest as ActivityResultLauncherWrapper.Request.ResultFromVoiceSearch
        lastKnownRequest.onResult(Activity.RESULT_OK, "Result")

        verify(pixel).fire(VoiceSearchPixelNames.VOICE_SEARCH_DONE, mapOf("source" to "browser"))
        assertEquals(Event.VoiceRecognitionSuccess("Result"), lastKnownEvent)
    }

    @Test
    fun `registerResultsCallback - result from voice search browser is error - emit voice recognition error`() {
        var lastKnownEvent: Event? = null
        testee.registerResultsCallback(mock(), mock(), BROWSER) {
            lastKnownEvent = it
        }

        val lastKnownRequest = activityResultLauncherWrapper.lastKnownRequest as ActivityResultLauncherWrapper.Request.ResultFromVoiceSearch
        lastKnownRequest.onResult(VoiceSearchActivity.VOICE_SEARCH_ERROR, "1")
        verify(pixel).fire(VoiceSearchPixelNames.VOICE_SEARCH_ERROR, mapOf("error" to "1"))

        assertNull(lastKnownEvent)
    }

    @Test
    fun `registerResultsCallback - result from voice search widget is OK and not empty - emit voice recognition success`() {
        var lastKnownEvent: Event? = null
        testee.registerResultsCallback(mock(), mock(), WIDGET) {
            lastKnownEvent = it
        }

        val lastKnownRequest = activityResultLauncherWrapper.lastKnownRequest as ActivityResultLauncherWrapper.Request.ResultFromVoiceSearch
        lastKnownRequest.onResult(Activity.RESULT_OK, "Result")

        verify(pixel).fire(VoiceSearchPixelNames.VOICE_SEARCH_DONE, mapOf("source" to "widget"))
        assertEquals(Event.VoiceRecognitionSuccess("Result"), lastKnownEvent)
    }

    @Test
    fun `registerResultsCallback - result from voice search is OK and empty - emit search cancelled`() {
        var lastKnownEvent: Event? = null
        testee.registerResultsCallback(mock(), mock(), BROWSER) {
            lastKnownEvent = it
        }

        val lastKnownRequest = activityResultLauncherWrapper.lastKnownRequest as ActivityResultLauncherWrapper.Request.ResultFromVoiceSearch
        lastKnownRequest.onResult(Activity.RESULT_OK, "")

        verify(pixel, never()).fire(VoiceSearchPixelNames.VOICE_SEARCH_DONE, mapOf("source" to "browser"))
        assertEquals(Event.SearchCancelled, lastKnownEvent)
    }

    @Test
    fun `registerResultsCallback - result from voice search cancelled - emit search cancelled`() {
        var lastKnownEvent: Event? = null
        testee.registerResultsCallback(mock(), mock(), BROWSER) {
            lastKnownEvent = it
        }

        val lastKnownRequest = activityResultLauncherWrapper.lastKnownRequest as ActivityResultLauncherWrapper.Request.ResultFromVoiceSearch
        lastKnownRequest.onResult(Activity.RESULT_CANCELED, "Result")

        verify(pixel, never()).fire(VoiceSearchPixelNames.VOICE_SEARCH_DONE, mapOf("source" to "browser"))
        verify(voiceSearchRepository).dismissVoiceSearch()
        assertEquals(Event.SearchCancelled, lastKnownEvent)
    }

    @Test
    fun `registerResultsCallback - voice search cancelled several times - show dialog`() {
        var lastKnownEvent: Event? = null
        testee.registerResultsCallback(mock(), mock(), BROWSER) {
            lastKnownEvent = it
        }
        whenever(voiceSearchRepository.countVoiceSearchDismissed()).thenReturn(3)

        val lastKnownRequest = activityResultLauncherWrapper.lastKnownRequest as ActivityResultLauncherWrapper.Request.ResultFromVoiceSearch
        lastKnownRequest.onResult(Activity.RESULT_CANCELED, "Result")

        verify(pixel, never()).fire(VoiceSearchPixelNames.VOICE_SEARCH_DONE, mapOf("source" to "browser"))
        verify(dialogLauncher).showRemoveVoiceSearchDialog(any(), any(), any())
        assertEquals(Event.SearchCancelled, lastKnownEvent)
    }

    @Test
    fun `registerResultsCallback - voice search cancelled less than two times - do not show dialog`() {
        var lastKnownEvent: Event? = null
        testee.registerResultsCallback(mock(), mock(), BROWSER) {
            lastKnownEvent = it
        }
        whenever(voiceSearchRepository.countVoiceSearchDismissed()).thenReturn(1)

        val lastKnownRequest = activityResultLauncherWrapper.lastKnownRequest as ActivityResultLauncherWrapper.Request.ResultFromVoiceSearch
        lastKnownRequest.onResult(Activity.RESULT_CANCELED, "Result")

        verify(pixel, never()).fire(VoiceSearchPixelNames.VOICE_SEARCH_DONE, mapOf("source" to "browser"))
        verify(dialogLauncher, never()).showRemoveVoiceSearchDialog(any(), any(), any())
        assertEquals(Event.SearchCancelled, lastKnownEvent)
    }

    @Test
    fun `registerResultsCallback - result from voice search is OK - reset dismissed counter`() {
        var lastKnownEvent: Event? = null
        testee.registerResultsCallback(mock(), mock(), BROWSER) {
            lastKnownEvent = it
        }
        whenever(voiceSearchRepository.countVoiceSearchDismissed()).thenReturn(1)

        val lastKnownRequest = activityResultLauncherWrapper.lastKnownRequest as ActivityResultLauncherWrapper.Request.ResultFromVoiceSearch
        lastKnownRequest.onResult(Activity.RESULT_OK, "Result")

        verify(voiceSearchRepository).resetVoiceSearchDismissed()
        verify(voiceSearchRepository, never()).dismissVoiceSearch()
    }

    @Test
    fun `launch - browser voice search - emit started pixel and call launch voice search`() {
        testee.registerResultsCallback(mock(), mock(), BROWSER) { }

        testee.launch(mock())

        verify(pixel).fire(VoiceSearchPixelNames.VOICE_SEARCH_STARTED, mapOf("source" to "browser"))
        assertEquals(LaunchVoiceSearch, activityResultLauncherWrapper.lastKnownAction)
    }

    @Test
    fun `launch - widget voice search - emit started pixel and call launch voice search`() {
        testee.registerResultsCallback(mock(), mock(), WIDGET) { }

        testee.launch(mock())

        verify(pixel).fire(VoiceSearchPixelNames.VOICE_SEARCH_STARTED, mapOf("source" to "widget"))
        assertEquals(LaunchVoiceSearch, activityResultLauncherWrapper.lastKnownAction)
    }
}
