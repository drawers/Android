package com.duckduckgo.autofill.impl.ui.credential.saving

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.autofill.api.AutofillEventListener
import com.duckduckgo.autofill.impl.AutofillFireproofDialogSuppressor
import com.duckduckgo.autofill.impl.store.InternalAutofillStore
import com.duckduckgo.autofill.impl.ui.credential.saving.declines.AutofillDeclineCounter
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.common.utils.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ResultHandlerPromptToDisableCredentialSavingTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()
    private val autofillFireproofDialogSuppressor: AutofillFireproofDialogSuppressor = mock()
    private val pixel: Pixel = mock()
    private val dispatchers: DispatcherProvider = coroutineTestRule.testDispatcherProvider
    private val declineCounter: AutofillDeclineCounter = mock()
    private val autofillStore: InternalAutofillStore = mock()
    private val appCoroutineScope: CoroutineScope = coroutineTestRule.testScope
    private val context = getInstrumentation().targetContext
    private val callback: AutofillEventListener = mock()

    private val testee = ResultHandlerPromptToDisableCredentialSaving(
        autofillFireproofDialogSuppressor = autofillFireproofDialogSuppressor,
        pixel = pixel,
        dispatchers = dispatchers,
        declineCounter = declineCounter,
        autofillStore = autofillStore,
        appCoroutineScope = appCoroutineScope,
    )

    @Test
    fun `processResult - fireproof notified - dialog not visible`() {
        val result = bundleForAutofillDisablePrompt()
        testee.processResult(result, context, "tab-id-123", Fragment(), callback)
        verify(autofillFireproofDialogSuppressor).autofillSaveOrUpdateDialogVisibilityChanged(false)
    }

    @Test
    fun `onDisableAutofill - store updated to false`() {
        testee.onDisableAutofill(callback)
        verify(autofillStore).autofillEnabled = false
    }

    @Test
    fun `onDisableAutofill - decline counter disabled`() = runTest {
        testee.onDisableAutofill(callback)
        verify(declineCounter).disableDeclineCounter()
    }

    @Test
    fun `onDisableAutofill - page refresh requested`() = runTest {
        testee.onDisableAutofill(callback)
        verify(callback).onAutofillStateChange()
    }

    @Test
    fun `onKeepUsingAutofill - decline counter disabled`() = runTest {
        testee.onKeepUsingAutofill()
        verify(declineCounter).disableDeclineCounter()
    }

    private fun bundleForAutofillDisablePrompt(): Bundle = Bundle()
}
