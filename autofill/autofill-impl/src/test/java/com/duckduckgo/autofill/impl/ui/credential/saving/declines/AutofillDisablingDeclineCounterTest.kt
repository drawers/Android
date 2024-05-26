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

package com.duckduckgo.autofill.impl.ui.credential.saving.declines

import com.duckduckgo.autofill.impl.store.InternalAutofillStore
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AutofillDisablingDeclineCounterTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val autofillStore: InternalAutofillStore = mock()
    private lateinit var testee: AutofillDisablingDeclineCounter

    @Before
    fun before() {
        whenever(autofillStore.autofillDeclineCount).thenReturn(0)
        whenever(autofillStore.monitorDeclineCounts).thenReturn(true)
        configureAutofillState(enabled = true, available = true)
    }

    @Test
    fun `initialiseDeclineCounter - no previously stored domain`() = runTest {
        initialiseDeclineCounter()
        assertNull(testee.currentSessionPreviousDeclinedDomain)
    }

    @Test
    fun `initialiseDeclineCounter - not monitoring decline counts - should not record new declines`() = runTest {
        whenever(autofillStore.monitorDeclineCounts).thenReturn(false)
        initialiseDeclineCounter()

        testee.userDeclinedToSaveCredentials("example.com")
        assertDeclineNotRecorded()
    }

    @Test
    fun `monitorDeclineCounts - should record new declines`() = runTest {
        whenever(autofillStore.monitorDeclineCounts).thenReturn(true)
        initialiseDeclineCounter()

        testee.userDeclinedToSaveCredentials("example.com")
        assertDeclineRecorded(expectedNewValue = 1)
    }

    @Test
    fun `userDeclinedToSaveCredentials - current session previous declined domain matches old domain - decline not recorded`() = runTest {
        initialiseDeclineCounter()
        testee.currentSessionPreviousDeclinedDomain = "example.com"
        testee.userDeclinedToSaveCredentials("example.com")
        assertDeclineNotRecorded()
    }

    @Test
    fun `userDeclinedToSaveCredentials - new domain does not match old domain - decline recorded`() = runTest {
        initialiseDeclineCounter()
        testee.currentSessionPreviousDeclinedDomain = "foo.com"
        testee.userDeclinedToSaveCredentials("example.com")
        assertDeclineRecorded(expectedNewValue = 1)
    }

    @Test
    fun `userDeclinedToSaveCredentials - domain stored`() = runTest {
        initialiseDeclineCounter()
        testee.currentSessionPreviousDeclinedDomain = null
        testee.userDeclinedToSaveCredentials("example.com")
        assertEquals("example.com", testee.currentSessionPreviousDeclinedDomain)
    }

    @Test
    fun whenDeclineOnNewDomainWithAPreviousDomainThenDomainStored() = runTest {
        initialiseDeclineCounter()
        testee.currentSessionPreviousDeclinedDomain = "foo.com"
        testee.userDeclinedToSaveCredentials("example.com")
        assertEquals("example.com", testee.currentSessionPreviousDeclinedDomain)
    }

    @Test
    fun `userDeclinedToSaveCredentials - decline total count below threshold - should not offer to disable`() = runTest {
        initialiseDeclineCounter()
        whenever(autofillStore.autofillDeclineCount).thenReturn(0)
        testee.userDeclinedToSaveCredentials("example.com")
        assertShouldNotPromptToDisableAutofill()
    }

    @Test
    fun `userDeclines - should offer to disable at threshold`() = runTest {
        initialiseDeclineCounter()
        configureGlobalDeclineCountAtThreshold()
        testee.userDeclinedToSaveCredentials("a.com")
        assertShouldPromptToDisableAutofill()
    }

    @Test
    fun `isActive - counter not active - should never prompt to disable autofill`() = runTest {
        initialiseDeclineCounter()
        testee.isActive = false
        configureGlobalDeclineCountAtThreshold()
        assertFalse(testee.shouldPromptToDisableAutofill())
    }

    @Test
    fun `initialiseDeclineCounter - autofill not available - counter not active`() = runTest {
        whenever(autofillStore.autofillAvailable).thenReturn(false)
        initialiseDeclineCounter()
        assertFalse(testee.isActive)
    }

    @Test
    fun `initialiseDeclineCounter - autofill not enabled - counter not active`() = runTest {
        whenever(autofillStore.autofillEnabled).thenReturn(false)
        initialiseDeclineCounter()
        assertFalse(testee.isActive)
    }

    private fun configureGlobalDeclineCountAtThreshold() {
        whenever(autofillStore.autofillDeclineCount).thenReturn(2)
    }

    private fun assertDeclineNotRecorded() {
        verify(autofillStore, never()).autofillDeclineCount = any()
    }

    private suspend fun assertShouldNotPromptToDisableAutofill() {
        assertFalse(testee.shouldPromptToDisableAutofill())
    }

    private suspend fun assertShouldPromptToDisableAutofill() {
        assertTrue(testee.shouldPromptToDisableAutofill())
    }

    @Suppress("SameParameterValue")
    private fun assertDeclineRecorded(expectedNewValue: Int) {
        verify(autofillStore).autofillDeclineCount = eq(expectedNewValue)
    }

    private fun TestScope.initialiseDeclineCounter() {
        testee = AutofillDisablingDeclineCounter(
            autofillStore = autofillStore,
            appCoroutineScope = this,
            dispatchers = coroutineTestRule.testDispatcherProvider,
        )
    }

    @Suppress("SameParameterValue")
    private fun configureAutofillState(enabled: Boolean = true, available: Boolean = true) {
        whenever(autofillStore.autofillEnabled).thenReturn(enabled)
        whenever(autofillStore.autofillAvailable).thenReturn(available)
    }
}
