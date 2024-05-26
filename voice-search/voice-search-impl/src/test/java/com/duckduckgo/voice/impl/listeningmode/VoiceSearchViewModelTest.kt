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

package com.duckduckgo.voice.impl.listeningmode

import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.voice.impl.listeningmode.OnDeviceSpeechRecognizer.Event
import com.duckduckgo.voice.impl.listeningmode.VoiceSearchViewModel.Command
import com.duckduckgo.voice.impl.listeningmode.VoiceSearchViewModel.ViewState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify

class VoiceSearchViewModelTest {
    private lateinit var testee: VoiceSearchViewModel

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    @Mock
    private lateinit var speechRecognizer: OnDeviceSpeechRecognizer

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testee = VoiceSearchViewModel(speechRecognizer)
    }

    @Test
    fun `startVoiceSearch - starts speech recognizer`() {
        testee.startVoiceSearch()

        verify(speechRecognizer).start(any())
    }

    @Test
    fun `stopVoiceSearch - stop speech recognizer`() {
        testee.stopVoiceSearch()

        verify(speechRecognizer).stop()
    }

    @Test
    fun `startVoiceSearch - emit initial viewState`() = runTest {
        testee.startVoiceSearch()

        testee.viewState().test {
            assertEquals(ViewState("", ""), awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startVoiceSearch - partial results received - emit new viewState`() = runTest {
        val captor = argumentCaptor<(Event) -> Unit>()
        testee.startVoiceSearch()
        verify(speechRecognizer).start(captor.capture())

        captor.firstValue.invoke(Event.PartialResultReceived("This is the result"))

        testee.viewState().test {
            assertEquals(ViewState("This is the result", ""), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }

        testee.commands().test {
            expectNoEvents()
        }
    }

    @Test
    fun `startVoiceSearch - partial results exceed 30 words - emit new view state and handle speech recognition success`() = runTest {
        val result = "fermentum leo vel orci porta non pulvinar neque laoreet suspendisse interdum consectetur libero id faucibus nisl tincidunt " +
            "eget nullam non nisi est sit amet facilisis magna etiam tempor orci eu lobortis"
        val captor = argumentCaptor<(Event) -> Unit>()
        testee.startVoiceSearch()
        verify(speechRecognizer).start(captor.capture())

        captor.firstValue.invoke(Event.PartialResultReceived(result))

        testee.viewState().test {
            assertEquals(ViewState(result, ""), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }

        testee.commands().test {
            assertEquals(Command.HandleSpeechRecognitionSuccess(result), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startVoiceSearch - volume update received - emit update voice indicator command`() = runTest {
        val captor = argumentCaptor<(Event) -> Unit>()
        testee.startVoiceSearch()
        verify(speechRecognizer).start(captor.capture())

        captor.firstValue.invoke(Event.VolumeUpdateReceived(10f))

        testee.commands().test {
            assertEquals(Command.UpdateVoiceIndicator(10f), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startVoiceSearch - recognition success - emit handle speech recognition success command`() = runTest {
        val captor = argumentCaptor<(Event) -> Unit>()
        testee.startVoiceSearch()
        verify(speechRecognizer).start(captor.capture())

        captor.firstValue.invoke(Event.RecognitionSuccess("Final result"))

        testee.commands().test {
            assertEquals(Command.HandleSpeechRecognitionSuccess("Final result"), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startVoiceSearch - recognition timed out with no partial result - emit terminate voice search`() = runTest {
        val captor = argumentCaptor<(Event) -> Unit>()
        testee.startVoiceSearch()
        verify(speechRecognizer).start(captor.capture())

        captor.firstValue.invoke(Event.RecognitionTimedOut(0))

        testee.commands().test {
            assertEquals(Command.TerminateVoiceSearch(0), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startVoiceSearch - recognition fails - emit terminate voice search`() = runTest {
        val captor = argumentCaptor<(Event) -> Unit>()
        testee.startVoiceSearch()
        verify(speechRecognizer).start(captor.capture())

        captor.firstValue.invoke(Event.RecognitionFailed(0))

        testee.commands().test {
            assertEquals(Command.TerminateVoiceSearch(0), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startVoiceSearch - recognition timed out with partial result - handle speech recognition success command`() = runTest {
        val captor = argumentCaptor<(Event) -> Unit>()
        testee.startVoiceSearch()
        verify(speechRecognizer).start(captor.capture())

        captor.firstValue.invoke(Event.PartialResultReceived("This is the result"))
        captor.firstValue.invoke(Event.RecognitionTimedOut(0))

        testee.commands().test {
            assertEquals(Command.HandleSpeechRecognitionSuccess("This is the result"), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startVoiceSearch - no partial result - emit viewState with no unsent result`() = runTest {
        val captor = argumentCaptor<(Event) -> Unit>()
        testee.startVoiceSearch()
        verify(speechRecognizer).start(captor.capture())
        testee.startVoiceSearch()
        captor.firstValue.invoke(Event.PartialResultReceived("First"))

        testee.viewState().test {
            assertEquals(ViewState("First", ""), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startVoiceSearch - partial result received and view model restarted - emit view state with unsent result`() = runTest {
        val captor = argumentCaptor<(Event) -> Unit>()
        testee.startVoiceSearch()
        verify(speechRecognizer).start(captor.capture())
        captor.firstValue.invoke(Event.PartialResultReceived("First"))

        testee.startVoiceSearch()

        testee.viewState().test {
            assertEquals(ViewState("First", "First"), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `startVoiceSearch - recognition success emitted - emit viewState with appended result and unsent result`() = runTest {
        val captor = argumentCaptor<(Event) -> Unit>()
        testee.startVoiceSearch()
        verify(speechRecognizer).start(captor.capture())
        captor.firstValue.invoke(Event.PartialResultReceived("First"))
        testee.startVoiceSearch()

        captor.firstValue.invoke(Event.RecognitionSuccess("Second"))

        testee.commands().test {
            assertEquals(Command.HandleSpeechRecognitionSuccess("First Second"), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `userInitiatesSearchComplete - emit handle speech recognition success command`() = runTest {
        val captor = argumentCaptor<(Event) -> Unit>()
        testee.startVoiceSearch()
        verify(speechRecognizer).start(captor.capture())
        captor.firstValue.invoke(Event.PartialResultReceived("Test"))

        testee.userInitiatesSearchComplete()

        testee.commands().test {
            assertEquals(Command.HandleSpeechRecognitionSuccess("Test"), expectMostRecentItem())
            cancelAndConsumeRemainingEvents()
        }
    }
}
