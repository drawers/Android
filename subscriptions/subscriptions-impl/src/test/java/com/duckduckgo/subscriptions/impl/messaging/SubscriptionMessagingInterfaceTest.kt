package com.duckduckgo.subscriptions.impl.messaging

import android.webkit.WebView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.js.messaging.api.JsMessageCallback
import com.duckduckgo.js.messaging.api.JsMessageHelper
import com.duckduckgo.js.messaging.api.JsRequestResponse
import com.duckduckgo.subscriptions.api.SubscriptionStatus.AUTO_RENEWABLE
import com.duckduckgo.subscriptions.impl.AuthToken
import com.duckduckgo.subscriptions.impl.SubscriptionsChecker
import com.duckduckgo.subscriptions.impl.SubscriptionsManager
import com.duckduckgo.subscriptions.impl.pixels.SubscriptionPixelSender
import com.duckduckgo.subscriptions.impl.repository.Entitlement
import com.duckduckgo.subscriptions.impl.repository.Subscription
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SubscriptionMessagingInterfaceTest {
    @get:Rule var coroutineRule = CoroutineTestRule()

    private val webView: WebView = mock()
    private val jsMessageHelper: JsMessageHelper = mock()
    private val subscriptionsManager: SubscriptionsManager = mock()
    private val pixelSender: SubscriptionPixelSender = mock()
    private val subscriptionsChecker: SubscriptionsChecker = mock()
    private val messagingInterface = SubscriptionMessagingInterface(
        subscriptionsManager,
        jsMessageHelper,
        coroutineRule.testDispatcherProvider,
        coroutineRule.testScope,
        pixelSender,
        subscriptionsChecker,
    )

    private val callback = object : JsMessageCallback() {
        var counter = 0
        var id: String? = null

        override fun process(featureName: String, method: String, id: String?, data: JSONObject?) {
            this.id = id
            counter++
        }
    }

    @Test
    fun `processUnknownMessage - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        messagingInterface.process("", "secret")

        verifyNoInteractions(jsMessageHelper)
        verify(webView, never()).evaluateJavascript(any(), any())
    }

    @Test
    fun `processUnknownSecret - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"backToSettings","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "secret")

        verifyNoInteractions(jsMessageHelper)
        verify(webView, never()).evaluateJavascript(any(), any())
    }

    @Test
    fun `process - no url - do nothing`() = runTest {
        messagingInterface.register(webView, callback)

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"getSubscription","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verifyNoInteractions(jsMessageHelper)
        verify(webView, never()).evaluateJavascript(any(), any())
    }

    @Test
    fun `processInterfaceNotRegistered - do nothing`() = runTest {
        whenever(webView.url).thenReturn("https://duckduckgo.com/test")

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"getSubscription","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verifyNoInteractions(jsMessageHelper)
        verify(webView, never()).evaluateJavascript(any(), any())
    }

    @Test
    fun `processIfMethodDoesNotMatch - do nothing`() = runTest {
        givenInterfaceIsRegistered()
        givenAuthTokenIsSuccess()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"test","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verifyNoInteractions(jsMessageHelper)
    }

    @Test
    fun `processAndGetSubscriptions - message if active - return response`() = runTest {
        givenInterfaceIsRegistered()
        givenAuthTokenIsSuccess()
        givenSubscriptionIsActive()

        val expected = JsRequestResponse.Success(
            context = "subscriptionPages",
            featureName = "useSubscription",
            method = "getSubscription",
            id = "myId",
            result = JSONObject("""{ "token":"authToken"}"""),
        )

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"getSubscription","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        val captor = argumentCaptor<JsRequestResponse>()
        verify(jsMessageHelper).sendJsResponse(captor.capture(), eq(CALLBACK_NAME), eq(SECRET), eq(webView))
        val jsMessage = captor.firstValue

        assertTrue(jsMessage is JsRequestResponse.Success)
        checkEquals(expected, jsMessage)
    }

    @Test
    fun `processAndGetSubscriptions - not active - return error`() = runTest {
        givenInterfaceIsRegistered()
        givenAuthTokenIsSuccess()

        val expected = JsRequestResponse.Success(
            context = "subscriptionPages",
            featureName = "useSubscription",
            method = "getSubscription",
            id = "myId",
            result = JSONObject("""{ }"""),
        )

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"getSubscription","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        val captor = argumentCaptor<JsRequestResponse>()
        verify(jsMessageHelper).sendJsResponse(captor.capture(), eq(CALLBACK_NAME), eq(SECRET), eq(webView))
        val jsMessage = captor.firstValue

        assertTrue(jsMessage is JsRequestResponse.Success)
        checkEquals(expected, jsMessage)
    }

    @Test
    fun `processAndGetSubscriptionsMessageError - return response`() = runTest {
        givenInterfaceIsRegistered()
        givenAuthTokenIsFailure()

        val expected = JsRequestResponse.Success(
            context = "subscriptionPages",
            featureName = "useSubscription",
            method = "getSubscription",
            id = "myId",
            result = JSONObject("""{ }"""),
        )

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"getSubscription","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        val captor = argumentCaptor<JsRequestResponse>()
        verify(jsMessageHelper).sendJsResponse(captor.capture(), eq(CALLBACK_NAME), eq(SECRET), eq(webView))
        val jsMessage = captor.firstValue

        assertTrue(jsMessage is JsRequestResponse.Success)
        checkEquals(expected, jsMessage)
    }

    @Test
    fun `processAndGetSubscriptions - feature name does not match - do nothing`() = runTest {
        givenInterfaceIsRegistered()
        givenAuthTokenIsSuccess()

        val message = """
            {"context":"subscriptionPages","featureName":"test","method":"getSubscription","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verifyNoInteractions(jsMessageHelper)
    }

    @Test
    fun `processAndGetSubscription - no id - do nothing`() = runTest {
        givenInterfaceIsRegistered()
        givenAuthTokenIsSuccess()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"getSubscription", "params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verifyNoInteractions(jsMessageHelper)
    }

    @Test
    fun `processAndBackToSettings - feature name does not match - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"test","id":"myId","method":"backToSettings","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(0, callback.counter)
    }

    @Test
    fun `processAndBackToSettings - callback executed`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","id":"myId","method":"backToSettings","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(1, callback.counter)
    }

    @Test
    fun `processAndSetSubscriptionMessageIfFeatureNameDoesNotMatch - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"test","method":"setSubscription","params":{"token":"authToken"}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verifyNoInteractions(subscriptionsManager)
        verifyNoInteractions(pixelSender)
    }

    @Test
    fun `processAndSetSubscriptionMessage - authenticate`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"setSubscription","params":{"token":"authToken"}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verify(subscriptionsManager).exchangeAuthToken("authToken")
        verify(pixelSender).reportRestoreUsingEmailSuccess()
        verify(pixelSender).reportSubscriptionActivated()
        assertEquals(1, callback.counter)
    }

    @Test
    fun `processAndSetSubscriptionMessage - no token - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"setSubscription","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verifyNoInteractions(subscriptionsManager)
        verifyNoInteractions(pixelSender)
    }

    @Test
    fun `processAndGetSubscriptionOptionsMessage - feature name does not match - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"test","method":"getSubscriptionOptions","id":"id","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(0, callback.counter)
    }

    @Test
    fun `processAndGetSubscriptionOptionsMessage - callback called`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"getSubscriptionOptions","id":"id","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(1, callback.counter)
    }

    @Test
    fun `processAndGetSubscriptionOptionsMessage - no id - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"getSubscriptionOptions","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(1, callback.counter)
        assertNull(callback.id)
    }

    @Test
    fun `processAndSubscriptionSelectedMessage - feature name does not match - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"test","method":"subscriptionSelected","id":"id","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(0, callback.counter)
    }

    @Test
    fun `processAndSubscriptionSelectedMessage - callback called`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"subscriptionSelected","id":"id","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(1, callback.counter)
    }

    @Test
    fun `processAndActivateSubscription - feature name does not match - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"test","id":"myId","method":"activateSubscription","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(0, callback.counter)
    }

    @Test
    fun `processAndActivateSubscription - callback executed`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","id":"myId","method":"activateSubscription","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(1, callback.counter)
    }

    @Test
    fun `processAndFeatureSelected - feature name does not match - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"test","id":"myId","method":"featureSelected","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(0, callback.counter)
    }

    @Test
    fun `processAndFeatureSelected - callback executed`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"featureSelected","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(1, callback.counter)
    }

    @Test
    fun `process - feature selected message if url not in allow listed domains - do nothing`() = runTest {
        messagingInterface.register(webView, callback)
        whenever(webView.url).thenReturn("https://duckduckgo.example.com")

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"featureSelected","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(0, callback.counter)
    }

    @Test
    fun `processAndBackToSettings - activate success if feature name does not match - do nothing`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"test","id":"myId","method":"backToSettingsActivateSuccess","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(0, callback.counter)
    }

    @Test
    fun `processAndBackToSettingsActiveSuccess - callback executed`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","id":"myId","method":"backToSettingsActivateSuccess","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        assertEquals(1, callback.counter)
    }

    @Test
    fun `processAndMonthlyPriceClicked - pixel sent`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"subscriptionsMonthlyPriceClicked","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verify(pixelSender).reportMonthlyPriceClick()
        verifyNoMoreInteractions(pixelSender)
    }

    @Test
    fun `processAndYearlyPriceClicked - pixel sent`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"subscriptionsYearlyPriceClicked","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verify(pixelSender).reportYearlyPriceClick()
        verifyNoMoreInteractions(pixelSender)
    }

    @Test
    fun `processAndAddEmail - success - pixel sent`() = runTest {
        givenInterfaceIsRegistered()

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"subscriptionsAddEmailSuccess","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verify(pixelSender).reportAddEmailSuccess()
        verifyNoMoreInteractions(pixelSender)
    }

    @Test
    fun `processAndFaqClicked - callback executed`() = runTest {
        val jsMessageCallback: JsMessageCallback = mock()
        messagingInterface.register(webView, jsMessageCallback)
        whenever(webView.url).thenReturn("https://duckduckgo.com/test")

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"subscriptionsWelcomeFaqClicked","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verify(jsMessageCallback).process(eq("useSubscription"), eq("subscriptionsWelcomeFaqClicked"), any(), any())
    }

    @Test
    fun `processAndAddEmailClicked - callback executed`() = runTest {
        val jsMessageCallback: JsMessageCallback = mock()
        messagingInterface.register(webView, jsMessageCallback)
        whenever(webView.url).thenReturn("https://duckduckgo.com/test")

        val message = """
            {"context":"subscriptionPages","featureName":"useSubscription","method":"subscriptionsWelcomeAddEmailClicked","id":"myId","params":{}}
        """.trimIndent()

        messagingInterface.process(message, "duckduckgo-android-messaging-secret")

        verify(jsMessageCallback).process(eq("useSubscription"), eq("subscriptionsWelcomeAddEmailClicked"), any(), any())
    }

    private fun givenInterfaceIsRegistered() {
        messagingInterface.register(webView, callback)
        whenever(webView.url).thenReturn("https://duckduckgo.com/test")
    }

    private suspend fun givenSubscriptionIsActive() {
        whenever(subscriptionsManager.getSubscription()).thenReturn(
            Subscription(
                productId = "productId",
                startedAt = 10000L,
                expiresOrRenewsAt = 10000L,
                status = AUTO_RENEWABLE,
                platform = "google",
                entitlements = listOf(Entitlement("name", "product")),
            ),
        )
    }

    private suspend fun givenAuthTokenIsSuccess() {
        whenever(subscriptionsManager.getAuthToken()).thenReturn(AuthToken.Success(authToken = "authToken"))
    }

    private suspend fun givenAuthTokenIsFailure() {
        whenever(subscriptionsManager.getAuthToken()).thenReturn(AuthToken.Failure(message = "something happened"))
    }

    private fun checkEquals(expected: JsRequestResponse, actual: JsRequestResponse) {
        if (expected is JsRequestResponse.Success && actual is JsRequestResponse.Success) {
            assertEquals(expected.id, actual.id)
            assertEquals(expected.context, actual.context)
            assertEquals(expected.featureName, actual.featureName)
            assertEquals(expected.method, actual.method)
            assertEquals(expected.result.toString(), actual.result.toString())
        } else if (expected is JsRequestResponse.Error && actual is JsRequestResponse.Error) {
            assertEquals(expected.id, actual.id)
            assertEquals(expected.context, actual.context)
            assertEquals(expected.featureName, actual.featureName)
            assertEquals(expected.method, actual.method)
            assertEquals(expected.error, actual.error)
        } else {
            assertTrue(false)
        }
    }

    companion object {
        private const val CALLBACK_NAME = "messageCallback"
        private const val SECRET = "duckduckgo-android-messaging-secret"
    }
}
