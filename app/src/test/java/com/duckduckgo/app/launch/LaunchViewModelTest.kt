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

package com.duckduckgo.app.launch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.duckduckgo.app.launch.LaunchViewModel.Command.Home
import com.duckduckgo.app.launch.LaunchViewModel.Command.Onboarding
import com.duckduckgo.app.onboarding.store.AppStage
import com.duckduckgo.app.onboarding.store.UserStageStore
import com.duckduckgo.app.onboarding.ui.page.experiment.ExtendedOnboardingExperimentVariantManager
import com.duckduckgo.app.referral.StubAppReferrerFoundStateListener
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class LaunchViewModelTest {

    @get:Rule
    @Suppress("unused")
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private val userStageStore = mock<UserStageStore>()
    private val mockCommandObserver: Observer<LaunchViewModel.Command> = mock()
    private val mockExtendedOnboardingExperimentVariantManager: ExtendedOnboardingExperimentVariantManager = mock()

    private lateinit var testee: LaunchViewModel

    @After
    fun after() {
        testee.command.removeObserver(mockCommandObserver)
    }

    @Test
    fun `determineViewToShow - onboarding should show and referrer data returns quickly - command is onboarding`() = runTest {
        testee = LaunchViewModel(
            userStageStore,
            StubAppReferrerFoundStateListener("xx"),
            mockExtendedOnboardingExperimentVariantManager,
        )
        whenever(userStageStore.getUserAppStage()).thenReturn(AppStage.NEW)
        testee.command.observeForever(mockCommandObserver)

        testee.determineViewToShow()

        verify(mockCommandObserver).onChanged(any(Onboarding::class.java))
    }

    @Test
    fun `determineViewToShow - onboarding should show and referrer data returns but not instantly - command is onboarding`() = runTest {
        testee = LaunchViewModel(
            userStageStore,
            StubAppReferrerFoundStateListener("xx", mockDelayMs = 1_000),
            mockExtendedOnboardingExperimentVariantManager,
        )
        whenever(userStageStore.getUserAppStage()).thenReturn(AppStage.NEW)
        testee.command.observeForever(mockCommandObserver)

        testee.determineViewToShow()

        verify(mockCommandObserver).onChanged(any(Onboarding::class.java))
    }

    @Test
    fun `determineViewToShow - onboarding should show and referrer data times out - command is onboarding`() = runTest {
        testee = LaunchViewModel(
            userStageStore,
            StubAppReferrerFoundStateListener("xx", mockDelayMs = Long.MAX_VALUE),
            mockExtendedOnboardingExperimentVariantManager,
        )
        whenever(userStageStore.getUserAppStage()).thenReturn(AppStage.NEW)
        testee.command.observeForever(mockCommandObserver)

        testee.determineViewToShow()

        verify(mockCommandObserver).onChanged(any(Onboarding::class.java))
    }

    @Test
    fun `determineViewToShow - onboarding should not show and referrer data returns quickly - command is home`() = runTest {
        testee = LaunchViewModel(userStageStore, StubAppReferrerFoundStateListener("xx"), mockExtendedOnboardingExperimentVariantManager)
        whenever(userStageStore.getUserAppStage()).thenReturn(AppStage.DAX_ONBOARDING)
        testee.command.observeForever(mockCommandObserver)
        testee.determineViewToShow()
        verify(mockCommandObserver).onChanged(any(Home::class.java))
    }

    @Test
    fun `determineViewToShow - onboarding should not show and referrer data returns but not instantly - command is home`() = runTest {
        testee = LaunchViewModel(
            userStageStore,
            StubAppReferrerFoundStateListener("xx", mockDelayMs = 1_000),
            mockExtendedOnboardingExperimentVariantManager,
        )
        whenever(userStageStore.getUserAppStage()).thenReturn(AppStage.DAX_ONBOARDING)
        testee.command.observeForever(mockCommandObserver)
        testee.determineViewToShow()
        verify(mockCommandObserver).onChanged(any(Home::class.java))
    }

    @Test
    fun `determineViewToShow - onboarding times out - command is home`() = runTest {
        testee = LaunchViewModel(
            userStageStore,
            StubAppReferrerFoundStateListener("xx", mockDelayMs = Long.MAX_VALUE),
            mockExtendedOnboardingExperimentVariantManager,
        )
        whenever(userStageStore.getUserAppStage()).thenReturn(AppStage.DAX_ONBOARDING)
        testee.command.observeForever(mockCommandObserver)
        testee.determineViewToShow()
        verify(mockCommandObserver).onChanged(any(Home::class.java))
    }
}
