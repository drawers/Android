package com.duckduckgo.autofill.impl.feature.plugin.emailprotection

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.autofill.impl.email.incontext.store.EmailProtectionInContextDataStore
import com.duckduckgo.autofill.impl.email.remoteconfig.EmailProtectionInContextRemoteSettingsPersister
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class EmailProtectionInContextRemoteSettingsPersisterTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val dataStore: EmailProtectionInContextDataStore = mock()

    private val testee = EmailProtectionInContextRemoteSettingsPersister(
        dataStore = dataStore,
        appCoroutineScope = coroutineTestRule.testScope,
        dispatchers = coroutineTestRule.testDispatcherProvider,
    )

    @Test
    fun `whenInvalidJsonThenNotSaved - invalid JSON - not saved`() = runTest {
        testee.store("invalid json")
        verify(dataStore, never()).updateMaximumPermittedDaysSinceInstallation(any())
    }

    @Test
    fun `whenValidJsonButMissingInstalledDaysFieldThenSavedAsMaxValue - store - updates maximum permitted days since installation to max value`() = runTest {
        testee.store("{}")
        verify(dataStore).updateMaximumPermittedDaysSinceInstallation(Int.MAX_VALUE)
    }

    @Test
    fun `whenValidJsonPositiveNumberThenStoredCorrectly - store valid json positive number - update maximum permitted days since installation`() = runTest {
        testee.store(validJson(10))
        verify(dataStore).updateMaximumPermittedDaysSinceInstallation(10)
    }

    @Test
    fun `whenValidJsonNegativeNumberThenStoredCorrectly - negative number stored correctly`() = runTest {
        testee.store(validJson(-1))
        verify(dataStore).updateMaximumPermittedDaysSinceInstallation(-1)
    }

    private fun validJson(installDays: Int): String {
        return """
            {"installedDays": $installDays}
        """.trimIndent()
    }
}
