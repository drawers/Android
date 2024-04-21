package com.duckduckgo.autofill.impl.email.incontext.verifier

import org.junit.Assert.*
import org.junit.Test

class RealEmailProtectionLinkVerifierTest {
    private val testee = RealEmailProtectionLinkVerifier()

    @Test
    fun `whenUrlIsNullThenDoNotConsumeLink - null url - do not consume link`() {
        assertFalse(null.testUrl())
    }

    @Test
    fun `whenUrlDoesNotContainVerificationLinkThenDoNotConsumeLink - does not contain verification link - do not consume link`() {
        assertFalse(NOT_VERIFICATION_URL.testUrl())
    }

    @Test
    fun `whenUrlBeginsWithVerificationLinkThenConsumeLink - verification link consumed`() {
        assertTrue(VERIFICATION_URL.testUrl())
    }

    @Test
    fun `whenUrlContainsVerificationLinkThenConsumeLink - verification link consumed`() {
        assertTrue(VERIFICATION_PREPENDED_WITH_ANOTHER_DOMAIN.testUrl())
    }

    @Test
    fun `whenInContextViewNotShowingThenNeverConsumeLink - never consume link`() {
        assertFalse(testee.shouldDelegateToInContextView(VERIFICATION_URL, inContextViewAlreadyShowing = false))
        assertFalse(testee.shouldDelegateToInContextView(VERIFICATION_PREPENDED_WITH_ANOTHER_DOMAIN, inContextViewAlreadyShowing = false))
        assertFalse(testee.shouldDelegateToInContextView(NOT_VERIFICATION_URL, inContextViewAlreadyShowing = false))
    }

    private fun String?.testUrl(): Boolean {
        return testee.shouldDelegateToInContextView(url = this, inContextViewAlreadyShowing = true)
    }

    companion object {
        private const val VERIFICATION_URL = "https://duckduckgo.com/email/login?otp"
        private const val NOT_VERIFICATION_URL = "https://example.com"

        // test this because links clicked in Gmail prepend Google's URL
        private const val VERIFICATION_PREPENDED_WITH_ANOTHER_DOMAIN = "https://www.google.com/url?q=$VERIFICATION_URL"
    }
}
