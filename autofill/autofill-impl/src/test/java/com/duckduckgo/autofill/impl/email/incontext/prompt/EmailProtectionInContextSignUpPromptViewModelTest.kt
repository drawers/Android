package com.duckduckgo.autofill.impl.email.incontext.prompt

import app.cash.turbine.test
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.autofill.api.EmailProtectionInContextSignUpDialog.EmailProtectionInContextSignUpResult.Cancel
import com.duckduckgo.autofill.api.EmailProtectionInContextSignUpDialog.EmailProtectionInContextSignUpResult.DoNotShowAgain
import com.duckduckgo.autofill.api.EmailProtectionInContextSignUpDialog.EmailProtectionInContextSignUpResult.SignUp
import com.duckduckgo.autofill.impl.email.incontext.prompt.EmailProtectionInContextSignUpPromptViewModel.Command.FinishWithResult
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.EMAIL_PROTECTION_IN_CONTEXT_PROMPT_CONFIRMED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.EMAIL_PROTECTION_IN_CONTEXT_PROMPT_DISMISSED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.EMAIL_PROTECTION_IN_CONTEXT_PROMPT_NEVER_AGAIN
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class EmailProtectionInContextSignUpPromptViewModelTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val pixel: Pixel = mock()
    private val testee = EmailProtectionInContextSignUpPromptViewModel(
        dispatchers = coroutineTestRule.testDispatcherProvider,
        pixel = pixel,
    )

    @Test
    fun `onProtectEmailButtonPressed - correct pixel sent`() {
        testee.onProtectEmailButtonPressed()
        verify(pixel).fire(EMAIL_PROTECTION_IN_CONTEXT_PROMPT_CONFIRMED)
    }

    @Test
    fun `onProtectEmailButtonPressed - correct result type returned`() = runTest {
        testee.onProtectEmailButtonPressed()
        testee.commands.test {
            assertTrue((awaitItem() as FinishWithResult).result is SignUp)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCloseButtonPressed - correct pixel sent`() {
        testee.onCloseButtonPressed()
        verify(pixel).fire(EMAIL_PROTECTION_IN_CONTEXT_PROMPT_DISMISSED)
    }

    @Test
    fun `onCloseButtonPressed - dismiss prompt - correct result type returned`() = runTest {
        testee.onCloseButtonPressed()
        testee.commands.test {
            assertTrue((awaitItem() as FinishWithResult).result is Cancel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDoNotShowAgainButtonPressed - correct pixel sent`() {
        testee.onDoNotShowAgainButtonPressed()
        verify(pixel).fire(EMAIL_PROTECTION_IN_CONTEXT_PROMPT_NEVER_AGAIN)
    }

    @Test
    fun `onDoNotShowAgainButtonPressed - correct result type returned`() = runTest {
        testee.onDoNotShowAgainButtonPressed()
        testee.commands.test {
            assertTrue((awaitItem() as FinishWithResult).result is DoNotShowAgain)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
