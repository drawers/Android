package com.duckduckgo.autofill.impl.engagement

import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.app.statistics.pixels.Pixel.PixelType.UNIQUE
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_ENGAGEMENT_ONBOARDED_USER
import com.duckduckgo.browser.api.UserBrowserProperties
import com.duckduckgo.common.test.CoroutineTestRule
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class EngagementPasswordAddedListenerTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val userBrowserProperties: UserBrowserProperties = mock()
    private val pixel: Pixel = mock()

    private val testee = EngagementPasswordAddedListener(
        userBrowserProperties = userBrowserProperties,
        appCoroutineScope = coroutineTestRule.testScope,
        dispatchers = coroutineTestRule.testDispatcherProvider,
        pixel = pixel,
    )

    @Test
    fun `onCredentialAdded - days installed less than 7 - pixel sent`() {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(0)
        testee.onCredentialAdded(0)
        verifyPixelSent()
    }

    @Test
    fun `onCredentialAdded - days installed exactly 7 - pixel not sent`() {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(7)
        testee.onCredentialAdded(0)
        verifyPixelNotSent()
    }

    @Test
    fun `onCredentialAdded - days installed above 7 - pixel not sent`() {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(10)
        testee.onCredentialAdded(0)
        verifyPixelNotSent()
    }

    private fun verifyPixelSent() {
        verify(pixel).fire(AUTOFILL_ENGAGEMENT_ONBOARDED_USER, type = UNIQUE)
    }

    private fun verifyPixelNotSent() {
        verify(pixel, never()).fire(AUTOFILL_ENGAGEMENT_ONBOARDED_USER, type = UNIQUE)
    }
}
