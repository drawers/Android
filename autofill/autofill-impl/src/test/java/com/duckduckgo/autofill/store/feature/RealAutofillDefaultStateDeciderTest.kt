package com.duckduckgo.autofill.store.feature

import com.duckduckgo.autofill.api.InternalTestUserChecker
import com.duckduckgo.browser.api.UserBrowserProperties
import com.duckduckgo.feature.toggles.api.toggle.AutofillTestFeature
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RealAutofillDefaultStateDeciderTest {

    private val userBrowserProperties: UserBrowserProperties = mock()
    private val autofillFeature = AutofillTestFeature()
    private val internalTestUserChecker: InternalTestUserChecker = mock()
    private val testee = RealAutofillDefaultStateDecider(
        userBrowserProperties = userBrowserProperties,
        autofillFeature = autofillFeature,
        internalTestUserChecker = internalTestUserChecker,
    )

    @Test
    fun `whenRemoteFeatureDisabled - default state irrelevant`() {
        configureRemoteFeatureEnabled(false)

        configureDaysInstalled(0)
        assertFalse(testee.defaultState())

        configureDaysInstalled(1000)
        assertFalse(testee.defaultState())
    }

    @Test
    fun `defaultState - numberOfDaysInstalled not zero - irrelevant feature flag`() {
        configureDaysInstalled(0)

        configureRemoteFeatureEnabled(false)
        assertFalse(testee.defaultState())

        configureRemoteFeatureEnabled(false)
        assertFalse(testee.defaultState())
    }

    @Test
    fun `configureAsInternalTester - default state always enabled`() {
        configureDaysInstalled(100)
        configureRemoteFeatureEnabled(false)
        configureAsInternalTester()
        assertTrue(testee.defaultState())
    }

    @Test
    fun `defaultState - installed same day and feature flag enabled - enabled by default`() {
        configureDaysInstalled(0)
        configureRemoteFeatureEnabled(true)
        assertTrue(testee.defaultState())
    }

    private fun configureAsInternalTester() {
        whenever(internalTestUserChecker.isInternalTestUser).thenReturn(true)
    }

    private fun configureRemoteFeatureEnabled(enabled: Boolean) {
        autofillFeature.onByDefault = enabled
    }

    private fun configureDaysInstalled(daysInstalled: Long) {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(daysInstalled)
    }
}
