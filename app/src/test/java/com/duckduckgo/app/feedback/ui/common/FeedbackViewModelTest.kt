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

package com.duckduckgo.app.feedback.ui.common

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.duckduckgo.app.feedback.api.FeedbackSubmitter
import com.duckduckgo.app.feedback.ui.common.FragmentState.InitialAppEnjoymentClarifier
import com.duckduckgo.app.feedback.ui.common.FragmentState.NegativeFeedbackMainReason
import com.duckduckgo.app.feedback.ui.common.FragmentState.NegativeFeedbackSubReason
import com.duckduckgo.app.feedback.ui.common.FragmentState.NegativeOpenEndedFeedback
import com.duckduckgo.app.feedback.ui.common.FragmentState.NegativeWebSitesBrokenFeedback
import com.duckduckgo.app.feedback.ui.negative.FeedbackType.MainReason.*
import com.duckduckgo.app.feedback.ui.negative.FeedbackType.MissingBrowserFeaturesSubReasons.TAB_MANAGEMENT
import com.duckduckgo.app.playstore.PlayStoreUtils
import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.common.test.InstantSchedulersRule
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@Suppress("RemoveExplicitTypeArguments")
class FeedbackViewModelTest {

    @get:Rule
    @Suppress("unused")
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    @Suppress("unused")
    val schedulers = InstantSchedulersRule()

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private lateinit var testee: FeedbackViewModel

    private val playStoreUtils: PlayStoreUtils = mock()
    private val feedbackSubmitter: FeedbackSubmitter = mock()
    private val appBuildConfig: AppBuildConfig = mock()

    private val commandObserver: Observer<Command> = mock()
    private val commandCaptor = argumentCaptor<Command>()

    private val updateViewCommand
        get() = testee.updateViewCommand.value?.fragmentViewState

    @Before
    fun setup() {
        whenever(appBuildConfig.isDebug).thenReturn(true)

        testee = FeedbackViewModel(playStoreUtils, feedbackSubmitter, TestScope(), appBuildConfig, coroutineRule.testDispatcherProvider)
        testee.command.observeForever(commandObserver)
    }

    @After
    fun tearDown() {
        testee.command.removeObserver(commandObserver)
    }

    @Test
    fun `initialised - fragment state is for first step`() {
        assertTrue(updateViewCommand is InitialAppEnjoymentClarifier)
    }

    @Test
    fun `userSelectedPositiveFeedback - can rate app - first step of happy flow`() {
        configureRatingCanBeGiven()
        testee.userSelectedPositiveFeedback()
        assertTrue(updateViewCommand is FragmentState.PositiveFeedbackFirstStep)
        verifyForwardsNavigation(updateViewCommand)
    }

    @Test
    fun `userSelectedPositiveFeedback - cannot rate app - skips to sharing feedback`() {
        configureRatingCannotBeGiven()
        testee.userSelectedPositiveFeedback()
        assertTrue(updateViewCommand is FragmentState.PositiveShareFeedback)
        verifyForwardsNavigation(updateViewCommand)
    }

    @Test
    fun `onBackPressed - user navigates back from positive initial fragment - fragment state is initial fragment`() {
        configureRatingCanBeGiven()
        testee.userSelectedPositiveFeedback()
        testee.onBackPressed()

        assertTrue(updateViewCommand is InitialAppEnjoymentClarifier)
        verifyBackwardsNavigation(updateViewCommand)
    }

    @Test
    fun `onBackPressed - cannot rate app and user navigates back from positive initial fragment - fragment state is initial fragment`() {
        configureRatingCannotBeGiven()
        testee.userSelectedPositiveFeedback()
        testee.onBackPressed()

        assertTrue(updateViewCommand is InitialAppEnjoymentClarifier)
    }

    @Test
    fun `userGavePositiveFeedbackNoDetails - submitted`() = runTest {
        testee.userGavePositiveFeedbackNoDetails()

        verify(feedbackSubmitter).sendPositiveFeedback(null)
    }

    @Test
    fun `userGavePositiveFeedbackNoDetails - exit command issued`() = runTest {
        testee.userGavePositiveFeedbackNoDetails()

        val command = captureCommand() as Command.Exit
        assertTrue(command.feedbackSubmitted)
    }

    @Test
    fun `userProvidedPositiveOpenEndedFeedback - feedback submitted`() = runTest {
        testee.userProvidedPositiveOpenEndedFeedback("foo")

        verify(feedbackSubmitter).sendPositiveFeedback("foo")
    }

    @Test
    fun `userProvidedPositiveOpenEndedFeedback - exit command issued`() = runTest {
        testee.userProvidedPositiveOpenEndedFeedback("foo")

        val command = captureCommand() as Command.Exit
        assertTrue(command.feedbackSubmitted)
    }

    @Test
    fun `userProvidedNegativeOpenEndedFeedback - feedback submitted`() = runTest {
        testee.userProvidedNegativeOpenEndedFeedback(MISSING_BROWSING_FEATURES, TAB_MANAGEMENT, "foo")
        verify(feedbackSubmitter).sendNegativeFeedback(MISSING_BROWSING_FEATURES, TAB_MANAGEMENT, "foo")
    }

    @Test
    fun `userProvidedNegativeOpenEndedFeedback - no sub reason - feedback submitted`() = runTest {
        testee.userProvidedNegativeOpenEndedFeedback(MISSING_BROWSING_FEATURES, null, "foo")
        verify(feedbackSubmitter).sendNegativeFeedback(MISSING_BROWSING_FEATURES, null, "foo")
    }

    @Test
    fun `userProvidedNegativeOpenEndedFeedback - empty open-ended feedback - feedback submitted`() = runTest {
        testee.userProvidedNegativeOpenEndedFeedback(MISSING_BROWSING_FEATURES, null, "")
        verify(feedbackSubmitter).sendNegativeFeedback(MISSING_BROWSING_FEATURES, null, "")
    }

    @Test
    fun `userWantsToCancel - exit command issued`() {
        testee.userWantsToCancel()
        val command = captureCommand() as Command.Exit
        assertFalse(command.feedbackSubmitted)
    }

    @Test
    fun `userSelectedNegativeFeedback - initial sad face - first step of unhappy flow`() {
        testee.userSelectedNegativeFeedback()
        assertTrue(updateViewCommand is NegativeFeedbackMainReason)
        verifyForwardsNavigation(updateViewCommand)
    }

    @Test
    fun `onBackPressed - navigates back from negative main reason fragment - fragment state is initial fragment`() {
        testee.userSelectedNegativeFeedback()
        testee.onBackPressed()
        assertTrue(updateViewCommand is InitialAppEnjoymentClarifier)
        verifyBackwardsNavigation(updateViewCommand)
    }

    @Test
    fun `userSelectedNegativeFeedbackMainReason - missing browser features - fragment state is sub reason selection`() {
        testee.userSelectedNegativeFeedbackMainReason(MISSING_BROWSING_FEATURES)
        assertTrue(updateViewCommand is NegativeFeedbackSubReason)
        verifyForwardsNavigation(updateViewCommand)
    }

    @Test
    fun `userSelectedNegativeFeedbackMainReason - not enough customizations - fragment state is sub reason selection`() {
        testee.userSelectedNegativeFeedbackMainReason(NOT_ENOUGH_CUSTOMIZATIONS)
        assertTrue(updateViewCommand is NegativeFeedbackSubReason)
        verifyForwardsNavigation(updateViewCommand)
    }

    @Test
    fun `userSelectedNegativeFeedbackMainReason - search not good enough - fragment state is sub reason selection`() {
        testee.userSelectedNegativeFeedbackMainReason(SEARCH_NOT_GOOD_ENOUGH)
        assertTrue(updateViewCommand is NegativeFeedbackSubReason)
        verifyForwardsNavigation(updateViewCommand)
    }

    @Test
    fun `userSelectedNegativeFeedbackMainReason - app is slow or buggy - fragment state is sub reason selection`() {
        testee.userSelectedNegativeFeedbackMainReason(APP_IS_SLOW_OR_BUGGY)
        assertTrue(updateViewCommand is NegativeFeedbackSubReason)
        verifyForwardsNavigation(updateViewCommand)
    }

    @Test
    fun `userSelectedNegativeFeedbackMainReason - selects other - fragment state is open ended feedback`() {
        testee.userSelectedNegativeFeedbackMainReason(OTHER)
        assertTrue(updateViewCommand is NegativeOpenEndedFeedback)
        verifyForwardsNavigation(updateViewCommand)
    }

    @Test
    fun `userSelectedNegativeFeedbackMainReason - main reason broken site - fragment state is sub reason selection`() {
        testee.userSelectedNegativeFeedbackMainReason(WEBSITES_NOT_LOADING)
        assertTrue(updateViewCommand is NegativeWebSitesBrokenFeedback)
        verifyForwardsNavigation(updateViewCommand)
    }

    @Test
    fun `userSelectedSubReasonMissingBrowserFeatures - fragment state is open-ended feedback`() {
        testee.userSelectedSubReasonMissingBrowserFeatures(MISSING_BROWSING_FEATURES, TAB_MANAGEMENT)
        assertTrue(updateViewCommand is NegativeOpenEndedFeedback)
        verifyForwardsNavigation(updateViewCommand)
    }

    @Test
    fun `onBackPressed - navigates back from sub reason selection - fragment state is main reason selection`() {
        testee.userSelectedNegativeFeedbackMainReason(MISSING_BROWSING_FEATURES)
        testee.onBackPressed()
        assertTrue(updateViewCommand is NegativeFeedbackMainReason)
        verifyBackwardsNavigation(updateViewCommand)
    }

    @Test
    fun `onBackPressed - valid sub reason - fragment state is sub reason selection`() {
        testee.userSelectedNegativeFeedbackMainReason(MISSING_BROWSING_FEATURES)
        testee.userSelectedSubReasonMissingBrowserFeatures(MISSING_BROWSING_FEATURES, TAB_MANAGEMENT)
        testee.onBackPressed()
        assertTrue(updateViewCommand is NegativeFeedbackSubReason)
        verifyBackwardsNavigation(updateViewCommand)
    }

    @Test
    fun `onBackPressed - sub reason not a valid step - fragment state is main reason selection`() {
        testee.userSelectedNegativeFeedbackMainReason(OTHER)
        testee.onBackPressed()
        assertTrue(updateViewCommand is NegativeFeedbackMainReason)
        verifyBackwardsNavigation(updateViewCommand)
    }

    @Test
    fun `onBackPressed - navigates back from open-ended feedback - fragment state is sub reason selection`() {
        testee.userSelectedNegativeFeedbackMainReason(MISSING_BROWSING_FEATURES)
        testee.userSelectedSubReasonMissingBrowserFeatures(MISSING_BROWSING_FEATURES, TAB_MANAGEMENT)
        testee.onBackPressed()
        assertTrue(updateViewCommand is NegativeFeedbackSubReason)
        verifyBackwardsNavigation(updateViewCommand)
    }

    private fun verifyForwardsNavigation(fragmentViewState: FragmentState?) {
        assertTrue(fragmentViewState?.forwardDirection == true)
    }

    private fun verifyBackwardsNavigation(fragmentViewState: FragmentState?) {
        assertTrue(fragmentViewState?.forwardDirection == false)
    }

    private fun captureCommand(): Command {
        verify(commandObserver).onChanged(commandCaptor.capture())
        return commandCaptor.lastValue
    }

    private fun configureRatingCanBeGiven() {
        whenever(playStoreUtils.installedFromPlayStore()).thenReturn(true)
        whenever(playStoreUtils.isPlayStoreInstalled()).thenReturn(true)
    }

    private fun configureRatingCannotBeGiven() {
        whenever(playStoreUtils.installedFromPlayStore()).thenReturn(false)
        whenever(playStoreUtils.isPlayStoreInstalled()).thenReturn(false)
    }
}
