/*
 * Copyright (c) 2023 DuckDuckGo
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

package com.duckduckgo.contentscopescripts.impl.messaging

import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.contentscopescripts.impl.CoreContentScopeScripts
import com.duckduckgo.js.messaging.api.JsMessageCallback
import com.duckduckgo.js.messaging.api.JsMessageHelper
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ContentScopeScriptsJsMessagingTest {
    @get:Rule var coroutineRule = CoroutineTestRule()

    private val mockWebView: WebView = mock()
    private val jsMessageHelper: JsMessageHelper = mock()
    private val coreContentScopeScripts: CoreContentScopeScripts = mock()
    private lateinit var contentScopeScriptsJsMessaging: ContentScopeScriptsJsMessaging

    @Before
    fun setUp() {
        whenever(coreContentScopeScripts.secret).thenReturn("secret")
        whenever(coreContentScopeScripts.javascriptInterface).thenReturn("javascriptInterface")
        whenever(coreContentScopeScripts.callbackName).thenReturn("callbackName")
        contentScopeScriptsJsMessaging = ContentScopeScriptsJsMessaging(
            jsMessageHelper,
            coroutineRule.testDispatcherProvider,
            coreContentScopeScripts,
        )
    }

    @Test
    fun `process - unknown message - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        contentScopeScriptsJsMessaging.process("", contentScopeScriptsJsMessaging.secret)

        assertEquals(0, callback.counter)
    }

    @Test
    fun `process - unknown secret - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"contentScopeScripts","featureName":"webCompat","id":"myId","method":"webShare","params":{}}
        """.trimIndent()

        contentScopeScriptsJsMessaging.process(message, "test")

        assertEquals(0, callback.counter)
    }

    @Test
    fun `process - interface not registered - do nothing`() = runTest {
        whenever(mockWebView.url).thenReturn("https://example.com")

        val message = """
            {"context":"contentScopeScripts","featureName":"webCompat","id":"myId","method":"webShare","params":{}}
        """.trimIndent()

        contentScopeScriptsJsMessaging.process(message, contentScopeScriptsJsMessaging.secret)

        assertEquals(0, callback.counter)
    }

    @Test
    fun `register - add Js interface`() {
        contentScopeScriptsJsMessaging.register(mockWebView, callback)

        verify(mockWebView).addJavascriptInterface(any(), anyOrNull())
    }

    @Test
    fun `process - webShare - callback executed and not response sent`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"contentScopeScripts","featureName":"webCompat","id":"myId","method":"webShare","params":{}}
        """.trimIndent()

        contentScopeScriptsJsMessaging.process(message, contentScopeScriptsJsMessaging.secret)

        assertEquals(1, callback.counter)
    }

    @Test
    fun `process - feature name does not match - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"contentScopeScripts","featureName":"test","id":"myId","method":"webShare","params":{}}
        """.trimIndent()

        contentScopeScriptsJsMessaging.process(message, contentScopeScriptsJsMessaging.secret)

        assertEquals(0, callback.counter)
    }

    @Test
    fun `process - web share id does not exist - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"contentScopeScripts","webCompat":"test","method":"webShare","params":{}}
        """.trimIndent()

        contentScopeScriptsJsMessaging.process(message, contentScopeScriptsJsMessaging.secret)

        assertEquals(0, callback.counter)
    }

    @Test
    fun `process - permissions query - callback executed and no response sent`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"contentScopeScripts","featureName":"webCompat","id":"myId","method":"permissionsQuery","params":{}}
        """.trimIndent()

        contentScopeScriptsJsMessaging.process(message, contentScopeScriptsJsMessaging.secret)

        assertEquals(1, callback.counter)
    }

    @Test
    fun `process - id does not exist - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"contentScopeScripts","featureName":"webCompat","method":"permissionsQuery","params":{}}
        """.trimIndent()

        contentScopeScriptsJsMessaging.process(message, contentScopeScriptsJsMessaging.secret)

        assertEquals(0, callback.counter)
    }

    private val callback = object : JsMessageCallback() {
        var counter = 0
        override fun process(featureName: String, method: String, id: String?, data: JSONObject?) {
            counter++
        }
    }

    private fun givenInterfaceIsRegistered() {
        contentScopeScriptsJsMessaging.register(mockWebView, callback)
        whenever(mockWebView.url).thenReturn("https://example.com")
    }
}
