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

package com.duckduckgo.app.email

import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.browser.DuckDuckGoUrlDetectorImpl
import com.duckduckgo.autofill.api.Autofill
import com.duckduckgo.autofill.api.AutofillFeature
import com.duckduckgo.autofill.api.email.EmailManager
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.feature.toggles.api.Toggle
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class EmailJavascriptInterfaceTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val mockEmailManager: EmailManager = mock()
    private val mockWebView: WebView = mock()
    private lateinit var autofillFeature: AutofillFeature
    private val mockAutofill: Autofill = mock()
    lateinit var testee: EmailJavascriptInterface
    private var counter = 0

    @Before
    fun setup() {
        autofillFeature = com.duckduckgo.autofill.api.FakeAutofillFeature.create()

        testee = EmailJavascriptInterface(
            mockEmailManager,
            mockWebView,
            DuckDuckGoUrlDetectorImpl(),
            coroutineRule.testDispatcherProvider,
            autofillFeature,
            mockAutofill,
        ) { counter++ }

        autofillFeature.self().setEnabled(Toggle.State(enable = true))
        whenever(mockAutofill.isAnException(any())).thenReturn(false)
    }

    @Test
    fun `isSignedIn - url is DuckDuckGo email - isSignedIn called`() {
        whenever(mockWebView.url).thenReturn(DUCKDUCKGO_EMAIL_URL)

        testee.isSignedIn()

        verify(mockEmailManager).isSignedIn()
    }

    @Test
    fun `isSignedIn - url is not DuckDuckGo email - isSignedIn not called`() {
        whenever(mockWebView.url).thenReturn(NON_EMAIL_URL)

        testee.isSignedIn()

        verify(mockEmailManager, never()).isSignedIn()
    }

    @Test
    fun `storeCredentials - DuckDuckGo email URL - called with correct parameters`() {
        whenever(mockWebView.url).thenReturn(DUCKDUCKGO_EMAIL_URL)

        testee.storeCredentials("token", "username", "cohort")

        verify(mockEmailManager).storeCredentials("token", "username", "cohort")
    }

    @Test
    fun `storeCredentials - url is not DuckDuckGo email - storeCredentials not called`() {
        whenever(mockWebView.url).thenReturn(NON_EMAIL_URL)

        testee.storeCredentials("token", "username", "cohort")

        verify(mockEmailManager, never()).storeCredentials("token", "username", "cohort")
    }

    @Test
    fun `getUserData - DuckDuckGo email URL - getUserData called`() {
        whenever(mockWebView.url).thenReturn(DUCKDUCKGO_EMAIL_URL)

        testee.getUserData()

        verify(mockEmailManager).getUserData()
    }

    @Test
    fun `getUserData - url is not DuckDuckGo email - getUserData is not called`() {
        whenever(mockWebView.url).thenReturn(NON_EMAIL_URL)

        testee.getUserData()

        verify(mockEmailManager, never()).getUserData()
    }

    @Test
    fun `showTooltip - lambda called`() {
        whenever(mockWebView.url).thenReturn(NON_EMAIL_URL)

        testee.showTooltip()

        assertEquals(1, counter)
    }

    @Test
    fun `showTooltip - feature disabled - lambda not called`() {
        whenever(mockWebView.url).thenReturn(NON_EMAIL_URL)
        autofillFeature.self().setEnabled(Toggle.State(enable = false))

        testee.showTooltip()

        assertEquals(0, counter)
    }

    @Test
    fun `showTooltip - url is an exception - lambda not called`() {
        whenever(mockWebView.url).thenReturn(NON_EMAIL_URL)
        whenever(mockAutofill.isAnException(any())).thenReturn(true)

        testee.showTooltip()

        assertEquals(0, counter)
    }

    @Test
    fun `getDeviceCapabilities - DuckDuckGo email URL - return non-empty string`() {
        whenever(mockWebView.url).thenReturn(DUCKDUCKGO_EMAIL_URL)

        assert(testee.getDeviceCapabilities().isNotBlank())
    }

    @Test
    fun `getDeviceCapabilities - url is not DuckDuckGo email - return empty string`() {
        whenever(mockWebView.url).thenReturn(NON_EMAIL_URL)

        assert(testee.getDeviceCapabilities().isBlank())
    }

    companion object {
        const val DUCKDUCKGO_EMAIL_URL = "https://duckduckgo.com/email"
        const val NON_EMAIL_URL = "https://example.com"
    }
}
