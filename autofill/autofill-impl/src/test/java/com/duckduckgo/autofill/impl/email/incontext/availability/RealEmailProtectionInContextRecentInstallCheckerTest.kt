package com.duckduckgo.autofill.impl.email.incontext.availability

import com.duckduckgo.autofill.impl.email.incontext.store.EmailProtectionInContextDataStore
import com.duckduckgo.browser.api.UserBrowserProperties
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RealEmailProtectionInContextRecentInstallCheckerTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val userBrowserProperties: UserBrowserProperties = mock()
    private val dataStore: EmailProtectionInContextDataStore = mock()
    private val testee = RealEmailProtectionInContextRecentInstallChecker(userBrowserProperties, dataStore, coroutineTestRule.testDispatcherProvider)

    @Test
    fun `isRecentInstall - installed for less time than max days - is recent install`() = runTest {
        configureInstallationDaysLessThanMaximum()
        assertTrue(testee.isRecentInstall())
    }

    @Test
    fun `isRecentInstall - installed for same time as max days allows`() = runTest {
        configureInstallationDayToMatchMaximum()
        assertTrue(testee.isRecentInstall())
    }

    @Test
    fun `isRecentInstall - installed for more time than max days allows - not recent install`() = runTest {
        configureInstallationDaysMoreThanMaximum()
        assertFalse(testee.isRecentInstall())
    }

    @Test
    fun `isRecentInstall - install rules missing - not permitted`() = runTest {
        configureMaxInstallDaysRuleNotSet()
        assertFalse(testee.isRecentInstall())
    }

    private suspend fun configureMaxInstallDaysRuleNotSet() {
        whenever(dataStore.getMaximumPermittedDaysSinceInstallation()).thenReturn(-1)
        configureNumberOfDaysInstalled(1)
    }

    private suspend fun configureInstallationDaysLessThanMaximum() {
        configureNumberOfDaysInstalled(1)
        configureMaximumPermittedDaysRule(2)
    }

    private suspend fun configureInstallationDaysMoreThanMaximum() {
        configureNumberOfDaysInstalled(5)
        configureMaximumPermittedDaysRule(2)
    }

    private suspend fun configureInstallationDayToMatchMaximum() {
        configureNumberOfDaysInstalled(1)
        configureMaximumPermittedDaysRule(1)
    }

    private fun configureNumberOfDaysInstalled(days: Int) {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(days.toLong())
    }

    private suspend fun configureMaximumPermittedDaysRule(maxDays: Int) {
        whenever(dataStore.getMaximumPermittedDaysSinceInstallation()).thenReturn(maxDays)
    }
}
