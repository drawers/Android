/*
 * Copyright (c) 2024 DuckDuckGo
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

package com.duckduckgo.autofill.impl.configuration

import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.api.email.EmailManager
import com.duckduckgo.autofill.impl.InternalAutofillCapabilityChecker
import com.duckduckgo.autofill.impl.email.incontext.availability.EmailProtectionInContextAvailabilityRules
import com.duckduckgo.autofill.impl.jsbridge.response.AvailableInputTypeCredentials
import com.duckduckgo.autofill.impl.sharedcreds.ShareableCredentials
import com.duckduckgo.autofill.impl.store.InternalAutofillStore
import com.duckduckgo.autofill.impl.store.NeverSavedSiteRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RealAutofillRuntimeConfigProviderTest {

    private lateinit var testee: RealAutofillRuntimeConfigProvider

    private val emailManager: EmailManager = mock()
    private val autofillStore: InternalAutofillStore = mock()
    private val runtimeConfigurationWriter: RuntimeConfigurationWriter = mock()
    private val shareableCredentials: ShareableCredentials = mock()
    private val autofillCapabilityChecker: InternalAutofillCapabilityChecker = mock()
    private val emailProtectionInContextAvailabilityRules: EmailProtectionInContextAvailabilityRules = mock()
    private val neverSavedSiteRepository: NeverSavedSiteRepository = mock()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testee = RealAutofillRuntimeConfigProvider(
            emailManager,
            autofillStore,
            runtimeConfigurationWriter,
            autofillCapabilityChecker = autofillCapabilityChecker,
            shareableCredentials = shareableCredentials,
            emailProtectionInContextAvailabilityRules = emailProtectionInContextAvailabilityRules,
            neverSavedSiteRepository = neverSavedSiteRepository,
        )

        runTest {
            whenever(autofillStore.getCredentials(EXAMPLE_URL)).thenReturn(emptyList())
            whenever(neverSavedSiteRepository.isInNeverSaveList(any())).thenReturn(false)
        }

        whenever(runtimeConfigurationWriter.generateContentScope()).thenReturn("")
        whenever(runtimeConfigurationWriter.generateResponseGetAvailableInputTypes(any(), any())).thenReturn("")
        whenever(runtimeConfigurationWriter.generateUserUnprotectedDomains()).thenReturn("")
        whenever(
            runtimeConfigurationWriter.generateUserPreferences(
                autofillCredentials = any(),
                credentialSaving = any(),
                passwordGeneration = any(),
                showInlineKeyIcon = any(),
                showInContextEmailProtectionSignup = any(),
            ),
        ).thenReturn("")
    }

    @Test
    fun `getRuntimeConfiguration - autofill not enabled - user prefs credentials is false`() = runTest {
        configureAutofillCapabilities(enabled = false)
        testee.getRuntimeConfiguration(EXAMPLE_URL)
        verifyAutofillCredentialsReturnedAs(false)
    }

    @Test
    fun `configureAutofillCapabilities - enabled - configuration user prefs credentials is true`() = runTest {
        configureAutofillCapabilities(enabled = true)
        configureNoShareableLogins()
        testee.getRuntimeConfiguration(EXAMPLE_URL)
        verifyAutofillCredentialsReturnedAs(true)
    }

    @Test
    fun `getRuntimeConfiguration - config specifies showing key icon`() = runTest {
        configureAutofillCapabilities(enabled = true)
        configureAutofillAvailableForSite(EXAMPLE_URL)
        configureNoShareableLogins()
        testee.getRuntimeConfiguration(EXAMPLE_URL)
        verifyKeyIconRequestedToShow()
    }

    @Test
    fun `getRuntimeConfiguration - no credentials for URL - input type credentials is false`() = runTest {
        configureAutofillEnabledWithNoSavedCredentials(EXAMPLE_URL)
        testee.getRuntimeConfiguration(EXAMPLE_URL)

        val expectedCredentialResponse = AvailableInputTypeCredentials(username = false, password = false)
        verify(runtimeConfigurationWriter).generateResponseGetAvailableInputTypes(
            credentialsAvailable = eq(expectedCredentialResponse),
            emailAvailable = any(),
        )
    }

    @Test
    fun `getRuntimeConfiguration - with credentials for URL - configuration input type credentials is true`() = runTest {
        configureAutofillCapabilities(enabled = true)
        whenever(autofillStore.getCredentials(EXAMPLE_URL)).thenReturn(
            listOf(
                LoginCredentials(
                    id = 1,
                    domain = EXAMPLE_URL,
                    username = "username",
                    password = "password",
                ),
            ),
        )
        configureNoShareableLogins()

        testee.getRuntimeConfiguration(EXAMPLE_URL)

        val expectedCredentialResponse = AvailableInputTypeCredentials(username = true, password = true)
        verify(runtimeConfigurationWriter).generateResponseGetAvailableInputTypes(
            credentialsAvailable = eq(expectedCredentialResponse),
            emailAvailable = any(),
        )
    }

    @Test
    fun `getRuntimeConfiguration - with shareable credentials for URL - configuration input type credentials is true`() = runTest {
        configureAutofillCapabilities(enabled = true)
        whenever(autofillStore.getCredentials(EXAMPLE_URL)).thenReturn(emptyList())
        whenever(shareableCredentials.shareableCredentials(any())).thenReturn(
            listOf(
                LoginCredentials(
                    id = 1,
                    domain = EXAMPLE_URL,
                    username = "username",
                    password = "password",
                ),
            ),
        )

        testee.getRuntimeConfiguration(EXAMPLE_URL)

        val expectedCredentialResponse = AvailableInputTypeCredentials(username = true, password = true)
        verify(runtimeConfigurationWriter).generateResponseGetAvailableInputTypes(
            credentialsAvailable = eq(expectedCredentialResponse),
            emailAvailable = any(),
        )
    }

    @Test
    fun `getRuntimeConfiguration - with username only for URL - configuration input type credentials username is true`() = runTest {
        val url = "example.com"
        configureAutofillCapabilities(enabled = true)
        whenever(autofillStore.getCredentials(url)).thenReturn(
            listOf(
                LoginCredentials(
                    id = 1,
                    domain = url,
                    username = "username",
                    password = null,
                ),
            ),
        )
        configureNoShareableLogins()

        testee.getRuntimeConfiguration(EXAMPLE_URL)

        val expectedCredentialResponse = AvailableInputTypeCredentials(username = true, password = false)
        verify(runtimeConfigurationWriter).generateResponseGetAvailableInputTypes(
            credentialsAvailable = eq(expectedCredentialResponse),
            emailAvailable = any(),
        )
    }

    @Test
    fun `getRuntimeConfiguration - empty username only for URL - configuration input type credentials username is false`() = runTest {
        val url = "example.com"
        configureAutofillCapabilities(enabled = true)
        whenever(autofillStore.getCredentials(url)).thenReturn(
            listOf(
                LoginCredentials(
                    id = 1,
                    domain = url,
                    username = "",
                    password = null,
                ),
            ),
        )
        configureNoShareableLogins()

        testee.getRuntimeConfiguration(EXAMPLE_URL)

        val expectedCredentialResponse = AvailableInputTypeCredentials(username = false, password = false)
        verify(runtimeConfigurationWriter).generateResponseGetAvailableInputTypes(
            credentialsAvailable = eq(expectedCredentialResponse),
            emailAvailable = any(),
        )
    }

    @Test
    fun `getRuntimeConfiguration - with password only for URL - configuration input type credentials username is true`() = runTest {
        val url = "example.com"
        configureAutofillCapabilities(enabled = true)
        whenever(autofillStore.getCredentials(url)).thenReturn(
            listOf(
                LoginCredentials(
                    id = 1,
                    domain = url,
                    username = null,
                    password = "password",
                ),
            ),
        )
        configureNoShareableLogins()

        testee.getRuntimeConfiguration(EXAMPLE_URL)

        val expectedCredentialResponse = AvailableInputTypeCredentials(username = false, password = true)
        verify(runtimeConfigurationWriter).generateResponseGetAvailableInputTypes(
            credentialsAvailable = eq(expectedCredentialResponse),
            emailAvailable = any(),
        )
    }

    @Test
    fun `getRuntimeConfiguration - with empty password only for URL - configuration input type credentials username is true`() = runTest {
        val url = "example.com"
        configureAutofillCapabilities(enabled = true)
        whenever(autofillStore.getCredentials(url)).thenReturn(
            listOf(
                LoginCredentials(
                    id = 1,
                    domain = url,
                    username = null,
                    password = "",
                ),
            ),
        )
        configureNoShareableLogins()

        testee.getRuntimeConfiguration(url)

        val expectedCredentialResponse = AvailableInputTypeCredentials(username = false, password = false)
        verify(runtimeConfigurationWriter).generateResponseGetAvailableInputTypes(
            credentialsAvailable = eq(expectedCredentialResponse),
            emailAvailable = any(),
        )
    }

    @Test
    fun `getRuntimeConfiguration - with credentials for URL but autofill disabled - configuration input type credentials is false`() = runTest {
        val url = "example.com"
        configureAutofillCapabilities(enabled = false)
        whenever(autofillStore.getCredentials(url)).thenReturn(
            listOf(
                LoginCredentials(
                    id = 1,
                    domain = url,
                    username = "username",
                    password = "password",
                ),
            ),
        )

        testee.getRuntimeConfiguration(url)

        val expectedCredentialResponse = AvailableInputTypeCredentials(username = false, password = false)
        verify(runtimeConfigurationWriter).generateResponseGetAvailableInputTypes(
            credentialsAvailable = eq(expectedCredentialResponse),
            emailAvailable = any(),
        )
    }

    @Test
    fun `getRuntimeConfiguration - with credentials for URL but autofill unavailable - configuration input type credentials is false`() = runTest {
        val url = "example.com"
        configureAutofillCapabilities(enabled = false)
        whenever(autofillStore.getCredentials(url)).thenReturn(
            listOf(
                LoginCredentials(
                    id = 1,
                    domain = url,
                    username = "username",
                    password = "password",
                ),
            ),
        )

        testee.getRuntimeConfiguration(url)

        val expectedCredentialResponse = AvailableInputTypeCredentials(username = false, password = false)
        verify(runtimeConfigurationWriter).generateResponseGetAvailableInputTypes(
            credentialsAvailable = eq(expectedCredentialResponse),
            emailAvailable = any(),
        )
    }

    @Test
    fun `getRuntimeConfiguration - email signed in - configuration input type email is true`() = runTest {
        val url = "example.com"
        configureAutofillEnabledWithNoSavedCredentials(url)
        whenever(emailManager.isSignedIn()).thenReturn(true)

        testee.getRuntimeConfiguration(url)

        verify(runtimeConfigurationWriter).generateResponseGetAvailableInputTypes(
            credentialsAvailable = any(),
            emailAvailable = eq(true),
        )
    }

    @Test
    fun `getRuntimeConfiguration - email signed out - input type email is false`() = runTest {
        val url = "example.com"
        configureAutofillEnabledWithNoSavedCredentials(url)
        whenever(emailManager.isSignedIn()).thenReturn(false)

        testee.getRuntimeConfiguration(url)

        verify(runtimeConfigurationWriter).generateResponseGetAvailableInputTypes(
            credentialsAvailable = any(),
            emailAvailable = eq(false),
        )
    }

    @Test
    fun `getRuntimeConfiguration - site not in never save list - can save credentials`() = runTest {
        val url = "example.com"
        configureAutofillEnabledWithNoSavedCredentials(url)
        testee.getRuntimeConfiguration(url)
        verifyCanSaveCredentialsReturnedAs(true)
    }

    @Test
    fun `getRuntimeConfiguration - site in never save list - tell JS can save credentials`() = runTest {
        val url = "example.com"
        configureAutofillEnabledWithNoSavedCredentials(url)
        whenever(neverSavedSiteRepository.isInNeverSaveList(url)).thenReturn(true)

        testee.getRuntimeConfiguration(url)
        verifyCanSaveCredentialsReturnedAs(true)
    }

    private suspend fun RealAutofillRuntimeConfigProviderTest.configureAutofillEnabledWithNoSavedCredentials(url: String) {
        configureAutofillCapabilities(enabled = true)
        whenever(autofillStore.getCredentials(url)).thenReturn(emptyList())
        configureNoShareableLogins()
    }

    private suspend fun configureAutofillAvailableForSite(url: String) {
        whenever(autofillStore.getCredentials(url)).thenReturn(emptyList())
        whenever(autofillStore.autofillEnabled).thenReturn(true)
        whenever(autofillStore.autofillAvailable).thenReturn(true)
    }

    private suspend fun configureAutofillCapabilities(enabled: Boolean) {
        whenever(autofillCapabilityChecker.isAutofillEnabledByConfiguration(any())).thenReturn(enabled)
        whenever(autofillCapabilityChecker.canInjectCredentialsToWebView(any())).thenReturn(enabled)
        whenever(autofillCapabilityChecker.canSaveCredentialsFromWebView(any())).thenReturn(enabled)
        whenever(autofillCapabilityChecker.canGeneratePasswordFromWebView(any())).thenReturn(enabled)
        whenever(emailProtectionInContextAvailabilityRules.permittedToShow(any())).thenReturn(enabled)
    }

    private fun verifyAutofillCredentialsReturnedAs(expectedValue: Boolean) {
        verify(runtimeConfigurationWriter).generateUserPreferences(
            autofillCredentials = eq(expectedValue),
            credentialSaving = any(),
            passwordGeneration = any(),
            showInlineKeyIcon = any(),
            showInContextEmailProtectionSignup = any(),
        )
    }

    private fun verifyCanSaveCredentialsReturnedAs(expected: Boolean) {
        verify(runtimeConfigurationWriter).generateUserPreferences(
            autofillCredentials = any(),
            credentialSaving = eq(expected),
            passwordGeneration = any(),
            showInlineKeyIcon = any(),
            showInContextEmailProtectionSignup = any(),
        )
    }

    private suspend fun configureNoShareableLogins() {
        whenever(shareableCredentials.shareableCredentials(any())).thenReturn(emptyList())
    }

    private fun verifyKeyIconRequestedToShow() {
        verify(runtimeConfigurationWriter).generateUserPreferences(
            autofillCredentials = any(),
            credentialSaving = any(),
            passwordGeneration = any(),
            showInlineKeyIcon = eq(true),
            showInContextEmailProtectionSignup = any(),
        )
    }

    companion object {
        private const val EXAMPLE_URL = "example.com"
    }
}
