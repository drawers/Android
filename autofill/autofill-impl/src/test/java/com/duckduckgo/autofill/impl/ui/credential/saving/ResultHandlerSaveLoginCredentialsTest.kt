/*
 * Copyright (c) 2024 DuckDuckGo
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

package com.duckduckgo.autofill.impl.ui.credential.saving

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.autofill.api.AutofillEventListener
import com.duckduckgo.autofill.api.CredentialSavePickerDialog
import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.impl.AutofillFireproofDialogSuppressor
import com.duckduckgo.autofill.impl.store.InternalAutofillStore
import com.duckduckgo.autofill.impl.ui.credential.saving.declines.AutofillDeclineCounter
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class ResultHandlerSaveLoginCredentialsTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val callback: AutofillEventListener = mock()

    private val autofillFireproofDialogSuppressor: AutofillFireproofDialogSuppressor = mock()
    private val declineCounter: AutofillDeclineCounter = mock()
    private val autofillStore: InternalAutofillStore = mock()
    private val appBuildConfig: AppBuildConfig = mock()

    private val testee = ResultHandlerSaveLoginCredentials(
        autofillFireproofDialogSuppressor = autofillFireproofDialogSuppressor,
        dispatchers = coroutineTestRule.testDispatcherProvider,
        declineCounter = declineCounter,
        autofillStore = autofillStore,
        appBuildConfig = appBuildConfig,
        appCoroutineScope = coroutineTestRule.testScope,
    )

    @Test
    fun `processResult - save bundle missing url - no attempt to save made`() = runTest {
        val bundle = bundle(url = null, credentials = someLoginCredentials())
        testee.processResult(bundle, context, "tab-id-123", Fragment(), callback)
        verifySaveNeverCalled()
        verifyNoInteractions(callback)
    }

    @Test
    fun `processBundle - save bundle missing credentials - no attempt to save made`() = runTest {
        val bundle = bundle(url = "example.com", credentials = null)
        testee.processResult(bundle, context, "tab-id-123", Fragment(), callback)
        verifySaveNeverCalled()
        verifyNoInteractions(callback)
    }

    @Test
    fun `processResult - save bundle well formed - credentials saved`() = runTest {
        val loginCredentials = LoginCredentials(domain = "example.com", username = "foo", password = "bar")
        val bundle = bundle("example.com", loginCredentials)
        whenever(autofillStore.saveCredentials(any(), any())).thenReturn(loginCredentials)
        testee.processResult(bundle, context, "tab-id-123", Fragment(), callback)
        verify(autofillStore).saveCredentials(eq("example.com"), eq(loginCredentials))
        verify(callback).onSavedCredentials(loginCredentials)
    }

    @Test
    fun `processResult - save credentials for first time - disable decline count monitoring flag`() = runTest {
        val loginCredentials = LoginCredentials(domain = "example.com", username = "foo", password = "bar")
        val bundle = bundle("example.com", loginCredentials)
        whenever(autofillStore.saveCredentials(any(), any())).thenReturn(loginCredentials)
        testee.processResult(bundle, context, "tab-id-123", Fragment(), callback)
        verify(declineCounter).disableDeclineCounter()
    }

    @Test
    fun `processResult - save credentials unsuccessful - does not disable decline count monitoring flag`() = runTest {
        val bundle = bundle("example.com", someLoginCredentials())
        whenever(autofillStore.saveCredentials(any(), any())).thenReturn(null)
        testee.processResult(bundle, context, "tab-id-123", Fragment(), callback)
        verify(declineCounter, never()).disableDeclineCounter()
    }

    private suspend fun verifySaveNeverCalled() {
        verify(autofillStore, never()).saveCredentials(any(), any())
    }

    private fun bundle(
        url: String? = "example.com",
        credentials: LoginCredentials?,
    ): Bundle {
        return Bundle().also {
            it.putString(CredentialSavePickerDialog.KEY_URL, url)
            it.putParcelable(CredentialSavePickerDialog.KEY_CREDENTIALS, credentials)
        }
    }

    private fun someLoginCredentials() = LoginCredentials(domain = "example.com", username = "foo", password = "bar")
}
