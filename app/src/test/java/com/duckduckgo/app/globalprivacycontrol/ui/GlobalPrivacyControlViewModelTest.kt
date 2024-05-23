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

package com.duckduckgo.app.globalprivacycontrol.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.duckduckgo.app.pixels.AppPixelName
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.common.test.InstantSchedulersRule
import com.duckduckgo.feature.toggles.api.FeatureToggle
import com.duckduckgo.privacy.config.api.Gpc
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.lastValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class GlobalPrivacyControlViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val schedulers = InstantSchedulersRule()

    private val mockPixel: Pixel = mock()
    private val mockFeatureToggle: FeatureToggle = mock()
    private val mockGpc: Gpc = mock()
    private val commandCaptor = ArgumentCaptor.forClass(GlobalPrivacyControlViewModel.Command::class.java)
    private val viewStateCaptor = ArgumentCaptor.forClass(GlobalPrivacyControlViewModel.ViewState::class.java)
    private val mockCommandObserver: Observer<GlobalPrivacyControlViewModel.Command> = mock()
    private val mockViewStateObserver: Observer<GlobalPrivacyControlViewModel.ViewState> = mock()
    lateinit var testee: GlobalPrivacyControlViewModel

    @Before
    fun setup() {
        testee = GlobalPrivacyControlViewModel(mockPixel, mockFeatureToggle, mockGpc)
        testee.command.observeForever(mockCommandObserver)
        testee.viewState.observeForever(mockViewStateObserver)
    }

    @After
    fun after() {
        testee.command.removeObserver(mockCommandObserver)
        testee.viewState.removeObserver(mockViewStateObserver)
    }

    @Test
    fun `create - initialised with default viewState`() {
        val defaultViewState = GlobalPrivacyControlViewModel.ViewState()
        verify(mockViewStateObserver, atLeastOnce()).onChanged(viewStateCaptor.capture())
        assertEquals(defaultViewState, viewStateCaptor.value)
    }

    @Test
    fun `onCreate - pixel sent`() {
        verify(mockPixel).fire(AppPixelName.SETTINGS_DO_NOT_SELL_SHOWN)
    }

    @Test
    fun `onLearnMoreSelected - open learn more command issued with correct url`() {
        testee.onLearnMoreSelected()

        verify(mockCommandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(commandCaptor.lastValue, GlobalPrivacyControlViewModel.Command.OpenLearnMore())
    }

    @Test
    fun `onUserToggleGlobalPrivacyControl - do not sell on pixel sent`() {
        testee.onUserToggleGlobalPrivacyControl(true)

        verify(mockPixel).fire(AppPixelName.SETTINGS_DO_NOT_SELL_ON)
    }

    @Test
    fun `onUserToggleGlobalPrivacyControl - do not sell off pixel sent`() {
        testee.onUserToggleGlobalPrivacyControl(false)

        verify(mockPixel).fire(AppPixelName.SETTINGS_DO_NOT_SELL_OFF)
    }

    @Test
    fun `onUserToggleGlobalPrivacyControl - switched on - value stored in settings`() {
        testee.onUserToggleGlobalPrivacyControl(true)

        verify(mockGpc).enableGpc()
    }

    @Test
    fun `onUserToggleGlobalPrivacyControl - switched off - value stored in settings`() {
        testee.onUserToggleGlobalPrivacyControl(false)

        verify(mockGpc).disableGpc()
    }

    @Test
    fun `onUserToggleGlobalPrivacyControl - switched on - viewState updated to true`() {
        testee.onUserToggleGlobalPrivacyControl(true)

        verify(mockViewStateObserver, atLeastOnce()).onChanged(viewStateCaptor.capture())
        assertTrue(viewStateCaptor.value.globalPrivacyControlEnabled)
    }

    @Test
    fun `onUserToggleGlobalPrivacyControl - switched on - viewState updated to false`() {
        testee.onUserToggleGlobalPrivacyControl(false)

        verify(mockViewStateObserver, atLeastOnce()).onChanged(viewStateCaptor.capture())
        assertFalse(viewStateCaptor.value.globalPrivacyControlEnabled)
    }
}
