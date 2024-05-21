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

package com.duckduckgo.autoconsent.impl.handlers

import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.duckduckgo.autoconsent.api.AutoconsentCallback
import com.duckduckgo.autoconsent.impl.FakeSettingsRepository
import com.duckduckgo.autoconsent.impl.adapters.JSONObjectAdapter
import com.duckduckgo.autoconsent.impl.handlers.InitMessageHandlerPlugin.InitResp
import com.duckduckgo.autoconsent.impl.remoteconfig.AutoconsentFeatureSettingsRepository
import com.duckduckgo.common.test.CoroutineTestRule
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class InitMessageHandlerPluginTest {
    @get:Rule var coroutineRule = CoroutineTestRule()

    private val mockCallback: AutoconsentCallback = mock()
    private val repository: AutoconsentFeatureSettingsRepository = mock()
    private val webView: WebView = WebView(InstrumentationRegistry.getInstrumentation().targetContext)
    private val settingsRepository = FakeSettingsRepository()

    private val initHandlerPlugin = InitMessageHandlerPlugin(
        TestScope(),
        coroutineRule.testDispatcherProvider,
        settingsRepository,
        repository,
    )

    @Test
    fun `process - if message type is not init - do nothing`() {
        initHandlerPlugin.process("noMatching", "", webView, mockCallback)

        assertNull(shadowOf(webView).lastEvaluatedJavascript)
    }

    @Test
    fun `processIfCannotParseMessage - do nothing`() {
        val message = """
            {"type":"${initHandlerPlugin.supportedTypes.first()}", url: "http://www.example.com"}
        """.trimIndent()

        initHandlerPlugin.process(initHandlerPlugin.supportedTypes.first(), message, webView, mockCallback)

        assertNull(shadowOf(webView).lastEvaluatedJavascript)
    }

    @Test
    fun `processIfNotUrlSchema - do nothing - null`() {
        val message = """
            {"type":"${initHandlerPlugin.supportedTypes.first()}", "url": "ftp://www.example.com"}
        """.trimIndent()

        initHandlerPlugin.process(initHandlerPlugin.supportedTypes.first(), message, webView, mockCallback)

        assertNull(shadowOf(webView).lastEvaluatedJavascript)
    }

    @Test
    fun `processIfAutoconsentIsDisabledAndAlreadyHandled - do nothing`() {
        settingsRepository.userSetting = false
        settingsRepository.firstPopupHandled = true

        initHandlerPlugin.process(initHandlerPlugin.supportedTypes.first(), message(), webView, mockCallback)

        assertNull(shadowOf(webView).lastEvaluatedJavascript)
    }

    @Test
    fun `processIfAutoconsentIsDisabledAndNotHandled - do not call evaluate`() {
        settingsRepository.userSetting = false
        settingsRepository.firstPopupHandled = false

        initHandlerPlugin.process(initHandlerPlugin.supportedTypes.first(), message(), webView, mockCallback)

        assertNull(shadowOf(webView).lastEvaluatedJavascript)
    }

    @Test
    fun `processMessageForFirstTime - do not call evaluate - null result`() {
        whenever(repository.disabledCMPs).thenReturn(CopyOnWriteArrayList<String>().apply { add("MyCmp") })
        settingsRepository.userSetting = false
        settingsRepository.firstPopupHandled = false

        initHandlerPlugin.process(initHandlerPlugin.supportedTypes.first(), message(), webView, mockCallback)

        val shadow = shadowOf(webView)
        val result = shadow.lastEvaluatedJavascript
        assertNull(result)
    }

    @Test
    fun `processMessageResponseSent - correct response - `() {
        settingsRepository.userSetting = true
        settingsRepository.firstPopupHandled = true
        whenever(repository.disabledCMPs).thenReturn(CopyOnWriteArrayList())

        initHandlerPlugin.process(initHandlerPlugin.supportedTypes.first(), message(), webView, mockCallback)

        val shadow = shadowOf(webView)
        val result = shadow.lastEvaluatedJavascript
        val initResp = jsonToInitResp(result)
        assertEquals("optOut", initResp!!.config.autoAction)
        assertNotNull(initResp.rules)
        assertTrue(initResp.config.enablePrehide)
        assertTrue(initResp.config.enabled)
        assertEquals(20, initResp.config.detectRetries)
        assertEquals("initResp", initResp.type)
    }

    @Test
    fun `processMessage - on result received called - consent not managed`() {
        settingsRepository.userSetting = true
        settingsRepository.firstPopupHandled = true

        initHandlerPlugin.process(initHandlerPlugin.supportedTypes.first(), message(), webView, mockCallback)

        verify(mockCallback).onResultReceived(consentManaged = false, optOutFailed = false, selfTestFailed = false, isCosmetic = false)
    }

    private fun message(): String {
        return """
            {"type":"${initHandlerPlugin.supportedTypes.first()}", "url": "http://www.example.com"}
        """.trimIndent()
    }

    private fun jsonToInitResp(json: String): InitResp? {
        val trimmedJson = json
            .removePrefix("javascript:(function() {\n    window.autoconsentMessageCallback(")
            .removeSuffix(", window.origin);\n})();")
        val moshi = Moshi.Builder().add(JSONObjectAdapter()).build()
        val jsonAdapter: JsonAdapter<InitResp> = moshi.adapter(InitResp::class.java)
        return jsonAdapter.fromJson(trimmedJson)
    }
}
