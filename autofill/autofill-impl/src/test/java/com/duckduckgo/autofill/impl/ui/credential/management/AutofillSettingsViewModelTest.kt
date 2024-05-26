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

package com.duckduckgo.autofill.impl.ui.credential.management

import app.cash.turbine.test
import com.duckduckgo.app.browser.favicon.FaviconManager
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.app.statistics.pixels.Pixel.PixelName
import com.duckduckgo.app.statistics.pixels.Pixel.PixelType.COUNT
import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.api.email.EmailManager
import com.duckduckgo.autofill.impl.deviceauth.DeviceAuthenticator
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_ENABLE_AUTOFILL_TOGGLE_MANUALLY_DISABLED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_ENABLE_AUTOFILL_TOGGLE_MANUALLY_ENABLED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_NEVER_SAVE_FOR_THIS_SITE_CONFIRMATION_PROMPT_CONFIRMED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_NEVER_SAVE_FOR_THIS_SITE_CONFIRMATION_PROMPT_DISMISSED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_NEVER_SAVE_FOR_THIS_SITE_CONFIRMATION_PROMPT_DISPLAYED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.MENU_ACTION_AUTOFILL_PRESSED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.SETTINGS_AUTOFILL_MANAGEMENT_OPENED
import com.duckduckgo.autofill.impl.store.InternalAutofillStore
import com.duckduckgo.autofill.impl.store.NeverSavedSiteRepository
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command.ExitCredentialMode
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command.ExitListMode
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command.ExitLockedMode
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command.LaunchDeviceAuth
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command.OfferUserUndoMassDeletion
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command.ShowCredentialMode
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command.ShowDeviceUnsupportedMode
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command.ShowDisabledMode
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command.ShowLockedMode
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command.ShowUserPasswordCopied
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.Command.ShowUserUsernameCopied
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.CredentialMode
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.CredentialMode.EditingExisting
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.ListModeCommand
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.ListModeCommand.LaunchDeleteAllPasswordsConfirmation
import com.duckduckgo.autofill.impl.ui.credential.management.AutofillSettingsViewModel.ListModeCommand.PromptUserToAuthenticateMassDeletion
import com.duckduckgo.autofill.impl.ui.credential.management.searching.CredentialListFilter
import com.duckduckgo.autofill.impl.ui.credential.management.survey.AutofillSurvey
import com.duckduckgo.autofill.impl.ui.credential.management.survey.AutofillSurvey.SurveyDetails
import com.duckduckgo.autofill.impl.ui.credential.management.viewing.duckaddress.DuckAddressIdentifier
import com.duckduckgo.autofill.impl.ui.credential.management.viewing.duckaddress.RealDuckAddressIdentifier
import com.duckduckgo.autofill.impl.ui.credential.repository.DuckAddressStatusRepository
import com.duckduckgo.common.test.CoroutineTestRule
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AutofillSettingsViewModelTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val mockStore: InternalAutofillStore = mock()
    private val emailManager: EmailManager = mock()
    private val duckAddressStatusRepository: DuckAddressStatusRepository = mock()
    private val clipboardInteractor: AutofillClipboardInteractor = mock()
    private val pixel: Pixel = mock()
    private val deviceAuthenticator: DeviceAuthenticator = mock()
    private val credentialListFilter: CredentialListFilter = TestFilterPassthrough()
    private val faviconManager: FaviconManager = mock()
    private val webUrlIdentifier: WebUrlIdentifier = mock()
    private val duckAddressIdentifier: DuckAddressIdentifier = RealDuckAddressIdentifier()
    private val neverSavedSiteRepository: NeverSavedSiteRepository = mock()
    private val autofillSurvey: AutofillSurvey = mock()
    private val testee = AutofillSettingsViewModel(
        autofillStore = mockStore,
        clipboardInteractor = clipboardInteractor,
        deviceAuthenticator = deviceAuthenticator,
        pixel = pixel,
        dispatchers = coroutineTestRule.testDispatcherProvider,
        credentialListFilter = credentialListFilter,
        faviconManager = faviconManager,
        webUrlIdentifier = webUrlIdentifier,
        emailManager = emailManager,
        duckAddressStatusRepository = duckAddressStatusRepository,
        duckAddressIdentifier = duckAddressIdentifier,
        syncEngine = mock(),
        neverSavedSiteRepository = neverSavedSiteRepository,
        autofillSurvey = autofillSurvey,
    )

    @Before
    fun setup() {
        whenever(webUrlIdentifier.isLikelyAUrl(anyOrNull())).thenReturn(true)

        runTest {
            whenever(mockStore.getAllCredentials()).thenReturn(emptyFlow())
            whenever(neverSavedSiteRepository.neverSaveListCount()).thenReturn(emptyFlow())
            whenever(deviceAuthenticator.isAuthenticationRequiredForAutofill()).thenReturn(true)
        }
    }

    @Test
    fun `onEnableAutofill - viewState updated`() = runTest {
        testee.onEnableAutofill()
        testee.viewState.test {
            assertTrue(this.awaitItem().autofillEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDisableAutofill - viewState updated`() = runTest {
        testee.onDisableAutofill()
        testee.viewState.test {
            assertFalse(this.awaitItem().autofillEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onEnableAutofill - correct pixel fired`() {
        testee.onEnableAutofill()
        verify(pixel).fire(AUTOFILL_ENABLE_AUTOFILL_TOGGLE_MANUALLY_ENABLED)
    }

    @Test
    fun `onDisableAutofill - correct pixel fired`() {
        testee.onDisableAutofill()
        verify(pixel).fire(AUTOFILL_ENABLE_AUTOFILL_TOGGLE_MANUALLY_DISABLED)
    }

    @Test
    fun `onCopyPassword - command issued to show change`() = runTest {
        testee.onCopyPassword("hello")

        verify(clipboardInteractor).copyToClipboard("hello", isSensitive = true)
        testee.commands.test {
            awaitItem().first().assertCommandType(ShowUserPasswordCopied::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCopyUsername - command issued to show change`() = runTest {
        testee.onCopyUsername("username")

        verify(clipboardInteractor).copyToClipboard("username", isSensitive = false)
        testee.commands.test {
            awaitItem().first().assertCommandType(ShowUserUsernameCopied::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeleteCredentials - viewed credentials - store deletion called`() = runTest {
        val credentials = someCredentials()
        testee.onDeleteCredentials(credentials)
        verify(mockStore).deleteCredentials(credentials.id!!)
    }

    @Test
    fun `onDeleteCredentials - launched directly - credentials deleted`() = runTest {
        val credentials = someCredentials()
        testee.onViewCredentials(credentials)
        testee.onDeleteCredentials(credentials)
        verify(mockStore).deleteCredentials(credentials.id!!)
    }

    @Test
    fun `onViewCredentials - called from list - show credential viewing mode`() = runTest {
        val credentials = someCredentials()
        testee.onViewCredentials(credentials)

        testee.commands.test {
            assertEquals(ShowCredentialMode, awaitItem().first())
            cancelAndIgnoreRemainingEvents()
        }
        testee.viewState.test {
            assertEquals(CredentialMode.Viewing(credentials, showLinkButton = true), this.awaitItem().credentialMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onViewCredentials - first call - show credential viewing mode`() = runTest {
        val credentials = someCredentials()
        testee.onViewCredentials(credentials)

        testee.commands.test {
            assertEquals(ShowCredentialMode, awaitItem().first())
            cancelAndIgnoreRemainingEvents()
        }
        testee.viewState.test {
            assertEquals(CredentialMode.Viewing(credentials, showLinkButton = true), this.awaitItem().credentialMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onViewCredentials - called with website - show link button`() = runTest {
        whenever(webUrlIdentifier.isLikelyAUrl(anyOrNull())).thenReturn(true)
        val credentials = someCredentials()
        testee.onViewCredentials(credentials)

        testee.commands.test {
            assertEquals(ShowCredentialMode, awaitItem().first())
            cancelAndIgnoreRemainingEvents()
        }
        testee.viewState.test {
            assertEquals(CredentialMode.Viewing(credentials, showLinkButton = true), this.awaitItem().credentialMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onViewCredentials - without valid website - hide link button`() = runTest {
        whenever(webUrlIdentifier.isLikelyAUrl(anyOrNull())).thenReturn(false)
        val credentials = someCredentials()
        testee.onViewCredentials(credentials)

        testee.commands.test {
            assertEquals(ShowCredentialMode, awaitItem().first())
            cancelAndIgnoreRemainingEvents()
        }
        testee.viewState.test {
            assertEquals(CredentialMode.Viewing(credentials, showLinkButton = false), this.awaitItem().credentialMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onEditCurrentCredentials - show credential editing mode`() = runTest {
        val credentials = someCredentials()
        testee.onViewCredentials(credentials)
        testee.onEditCurrentCredentials()

        testee.viewState.test {
            assertEquals(
                EditingExisting(credentials, startedCredentialModeWithEdit = false, hasPopulatedFields = true),
                this.awaitItem().credentialMode,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onEditCredentials - show credential editing mode`() = runTest {
        val credentials = someCredentials()
        testee.onEditCredentials(credentials)

        testee.viewState.test {
            assertEquals(
                EditingExisting(credentials, startedCredentialModeWithEdit = true, hasPopulatedFields = false),
                this.awaitItem().credentialMode,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onEditCredentials - not from view - show credential editing mode`() = runTest {
        val credentials = someCredentials()

        testee.onEditCredentials(credentials)

        testee.commands.test {
            assertEquals(
                ShowCredentialMode,
                this.awaitItem().first(),
            )
            cancelAndIgnoreRemainingEvents()
        }

        testee.viewState.test {
            assertEquals(
                EditingExisting(credentials, startedCredentialModeWithEdit = true, hasPopulatedFields = false),
                this.awaitItem().credentialMode,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCredentialEditModePopulated - viewState updated`() = runTest {
        val credentials = someCredentials()

        testee.onEditCredentials(credentials)
        testee.onCredentialEditModePopulated()

        testee.viewState.test {
            assertEquals(
                EditingExisting(credentials, startedCredentialModeWithEdit = true, hasPopulatedFields = true),
                this.awaitItem().credentialMode,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lock - show locked mode`() = runTest {
        testee.lock()

        testee.commands.test {
            awaitItem().first().assertCommandType(ShowLockedMode::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `lock - called more than once - show locked mode only once`() = runTest {
        testee.lock()

        testee.commands.test {
            val count = awaitItem().filter { it == ShowLockedMode }.size
            assertEquals(1, count)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unlock - exit locked mode`() = runTest {
        testee.unlock()

        testee.commands.test {
            val commands = this.awaitItem()
            assertTrue(commands.contains(ExitLockedMode))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disabled - show disabled mode`() = runTest {
        testee.disabled()

        testee.commands.test {
            assertEquals(
                listOf(
                    ExitListMode,
                    ExitCredentialMode,
                    ExitLockedMode,
                    ShowDisabledMode,
                ),
                this.expectMostRecentItem().toList(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `saveOrUpdateCredentials - update credentials called - update autofill store`() = runTest {
        val credentials = someCredentials()
        testee.onEditCredentials(credentials)

        val updatedCredentials = credentials.copy(username = "helloworld123")
        whenever(mockStore.updateCredentials(credentials)).thenReturn(updatedCredentials)
        testee.saveOrUpdateCredentials(updatedCredentials)

        verify(mockStore).updateCredentials(updatedCredentials)
    }

    @Test
    fun `saveOrUpdateCredentials - called from manual creation - save autofill store`() = runTest {
        val credentials = someCredentials()
        testee.onCreateNewCredentials()
        testee.saveOrUpdateCredentials(credentials)
        verify(mockStore).saveCredentials(any(), eq(credentials))
    }

    @Test
    fun `onCancelEditMode - update credential mode state to viewing`() = runTest {
        val credentials = someCredentials()
        testee.onViewCredentials(credentials)
        testee.onEditCurrentCredentials()

        testee.onCancelEditMode()

        testee.viewState.test {
            assertEquals(CredentialMode.Viewing(credentials, showLinkButton = true), this.awaitItem().credentialMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onCancelEditMode - not from view - exit credential mode`() = runTest {
        val credentials = someCredentials()
        testee.onEditCredentials(credentials)

        testee.onCancelEditMode()

        testee.commands.test {
            awaitItem().last().assertCommandType(ExitCredentialMode::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onExitCredentialMode - exit credential mode`() = runTest {
        testee.onExitCredentialMode()

        testee.commands.test {
            awaitItem().first().assertCommandType(ExitCredentialMode::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unlock - was list mode - previous viewState restored`() = runTest {
        testee.onInitialiseListMode()
        testee.lock()
        testee.unlock()

        testee.viewState.test {
            assertEquals(CredentialMode.ListMode, this.awaitItem().credentialMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unlock - was credential viewing mode - previous view state restored`() = runTest {
        testee.onViewCredentials(someCredentials())
        testee.lock()
        testee.unlock()

        testee.viewState.test {
            assertTrue(this.awaitItem().credentialMode is CredentialMode.Viewing)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `disabled - in edit mode - update not in credential mode and show disabled mode`() = runTest {
        testee.onEditCredentials(someCredentials())
        testee.disabled()

        testee.commands.test {
            val commands = expectMostRecentItem().toList()
            assertTrue(commands[1] is ExitListMode)
            assertTrue(commands[2] is ExitCredentialMode)
            assertTrue(commands[3] is ExitLockedMode)
            assertTrue(commands[4] is ShowDisabledMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `allowSaveInEditMode - set to false - update viewState to editing saveable false`() = runTest {
        val credentials = someCredentials()
        testee.onViewCredentials(credentials)
        testee.onEditCurrentCredentials()

        testee.allowSaveInEditMode(false)

        testee.viewState.test {
            val finalResult = this.expectMostRecentItem()
            assertEquals(
                EditingExisting(credentials, saveable = false, startedCredentialModeWithEdit = false, hasPopulatedFields = true),
                finalResult.credentialMode,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launchDeviceAuth - device unsupported - emit unsupported mode command`() = runTest {
        configureDeviceToBeUnsupported()
        testee.launchDeviceAuth()

        testee.commands.test {
            assertEquals(
                listOf(
                    ExitListMode,
                    ExitCredentialMode,
                    ExitLockedMode,
                    ShowDeviceUnsupportedMode,
                ),
                this.expectMostRecentItem().toList(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launchDeviceAuth - update state to is authenticating - emit launch device command`() = runTest {
        configureDeviceToBeSupported()
        configureDeviceToHaveValidAuthentication(true)
        configureStoreToHaveThisManyCredentialsStored(1)
        testee.launchDeviceAuth()

        testee.commands.test {
            awaitItem().first().assertCommandType(LaunchDeviceAuth::class)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launchDeviceAuth - no saved credentials - is unlocked and auth not launched`() = runTest {
        configureDeviceToHaveValidAuthentication(true)
        configureStoreToHaveThisManyCredentialsStored(0)
        testee.launchDeviceAuth()

        testee.commands.test {
            val commands = this.awaitItem()
            assertTrue(commands.contains(ExitLockedMode))
            assertFalse(commands.contains(LaunchDeviceAuth))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launchDeviceAuth - no valid auth - disabled shown`() = runTest {
        configureDeviceToHaveValidAuthentication(false)
        testee.launchDeviceAuth()

        testee.commands.test {
            val commands = this.awaitItem()
            assertTrue(commands.contains(ExitLockedMode))
            assertFalse(commands.contains(LaunchDeviceAuth))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launchDeviceAuth - launched twice - emit launch device command twice`() = runTest {
        configureDeviceToBeSupported()
        configureDeviceToHaveValidAuthentication(true)
        configureStoreToHaveThisManyCredentialsStored(1)
        testee.launchDeviceAuth()
        testee.launchDeviceAuth()

        testee.commands.test {
            assertEquals(
                listOf(LaunchDeviceAuth, LaunchDeviceAuth),
                this.expectMostRecentItem().toList(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `launchDeviceAuth - no valid authentication - show disabled view and auth not launched`() = runTest {
        configureDeviceToBeSupported()
        configureDeviceToHaveValidAuthentication(false)
        testee.launchDeviceAuth()
        testee.commands.test {
            val commands = awaitItem()
            assertTrue(commands.contains(ShowDisabledMode))
            assertFalse(commands.contains(LaunchDeviceAuth))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onViewCreated - autofill disabled - autofill enabled state returned`() = runTest {
        whenever(mockStore.autofillEnabled).thenReturn(false)
        configureDeviceToHaveValidAuthentication(true)

        testee.onViewCreated()
        testee.viewState.test {
            assertEquals(false, this.awaitItem().autofillEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onViewCreated - autofill enabled - autofill enabled state returned`() = runTest {
        whenever(mockStore.autofillEnabled).thenReturn(true)
        configureDeviceToHaveValidAuthentication(true)

        testee.onViewCreated()
        testee.viewState.test {
            assertEquals(true, this.awaitItem().autofillEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSearchQueryChanged - empty query - show enable toggle`() = runTest {
        testee.onSearchQueryChanged("")

        testee.onViewCreated()
        testee.viewState.test {
            assertEquals(true, this.awaitItem().showAutofillEnabledToggle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onSearchQueryChanged - non-empty query - should not show enable toggle`() = runTest {
        testee.onSearchQueryChanged("foo")

        testee.onViewCreated()
        testee.viewState.test {
            assertEquals(false, this.awaitItem().showAutofillEnabledToggle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sendLaunchPixel - launched directly into credential view - no launch pixel sent`() {
        val launchedFromBrowser = false
        val directLinkToCredentials = true
        testee.sendLaunchPixel(launchedFromBrowser, directLinkToCredentials)
        verify(pixel, never()).fire(any<PixelName>(), any(), any(), eq(COUNT))
    }

    @Test
    fun `sendLaunchPixel - launched from browser and direct link to credentials - no launch pixel sent`() {
        val launchedFromBrowser = true
        val directLinkToCredentials = true
        testee.sendLaunchPixel(launchedFromBrowser, directLinkToCredentials)
        verify(pixel, never()).fire(any<PixelName>(), any(), any(), eq(COUNT))
    }

    @Test
    fun `sendLaunchPixel - launched from browser and not direct link - correct launch pixel sent`() {
        val launchedFromBrowser = true
        val directLinkToCredentials = false
        testee.sendLaunchPixel(launchedFromBrowser, directLinkToCredentials)
        verify(pixel).fire(eq(MENU_ACTION_AUTOFILL_PRESSED), any(), any(), eq(COUNT))
    }

    @Test
    fun `sendLaunchPixel - not from browser and not direct link - correct launch pixel sent`() {
        val launchedFromBrowser = false
        val directLinkToCredentials = false
        testee.sendLaunchPixel(launchedFromBrowser, directLinkToCredentials)
        verify(pixel).fire(eq(SETTINGS_AUTOFILL_MANAGEMENT_OPENED), any(), any(), eq(COUNT))
    }

    @Test
    fun `onResetNeverSavedSitesInitialSelection - correct pixel fired`() = runTest {
        testee.onResetNeverSavedSitesInitialSelection()
        verify(pixel).fire(AUTOFILL_NEVER_SAVE_FOR_THIS_SITE_CONFIRMATION_PROMPT_DISPLAYED)
    }

    @Test
    fun `onUserConfirmationToClearNeverSavedSites - repository cleared`() = runTest {
        testee.onUserConfirmationToClearNeverSavedSites()
        verify(neverSavedSiteRepository).clearNeverSaveList()
    }

    @Test
    fun `onUserConfirmationToClearNeverSavedSites - correct pixel fired`() = runTest {
        testee.onUserConfirmationToClearNeverSavedSites()
        verify(pixel).fire(AUTOFILL_NEVER_SAVE_FOR_THIS_SITE_CONFIRMATION_PROMPT_CONFIRMED)
    }

    @Test
    fun `onUserCancelledFromClearNeverSavedSitesPrompt - correct pixel fired`() = runTest {
        testee.onUserCancelledFromClearNeverSavedSitesPrompt()
        verify(pixel).fire(AUTOFILL_NEVER_SAVE_FOR_THIS_SITE_CONFIRMATION_PROMPT_DISMISSED)
    }

    @Test
    fun `onDeleteAllPasswordsInitialSelection - no saved logins - no command sent to show confirmation dialog`() = runTest {
        configureStoreToHaveThisManyCredentialsStored(0)
        testee.onViewCreated()
        testee.onDeleteAllPasswordsInitialSelection()
        testee.commandsListView.test {
            awaitItem().verifyDoesNotHaveCommandToShowDeleteAllConfirmation()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeleteAllPasswordsInitialSelection - one saved login - show confirmation dialog`() = runTest {
        configureStoreToHaveThisManyCredentialsStored(1)
        testee.onViewCreated()
        testee.onDeleteAllPasswordsInitialSelection()
        testee.commandsListView.test {
            awaitItem().verifyHasCommandToShowDeleteAllConfirmation(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeleteAllPasswordsConfirmed - no passwords saved - does not issue command to show undo snackbar`() = runTest {
        whenever(mockStore.deleteAllCredentials()).thenReturn(emptyList())
        testee.onDeleteAllPasswordsConfirmed()
        testee.commandsListView.test {
            awaitItem().verifyHasCommandToAuthenticateMassDeletion()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeleteAllPasswordsConfirmed - passwords saved - issue command to show undo snackbar`() = runTest {
        testee.onDeleteAllPasswordsConfirmed()
        testee.commandsListView.test {
            awaitItem().verifyHasCommandToAuthenticateMassDeletion()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAuthenticatedToDeleteAllPasswords - authentication succeeds - issues command to show undo snackbar`() = runTest {
        whenever(mockStore.deleteAllCredentials()).thenReturn(listOf(someCredentials()))
        testee.onAuthenticatedToDeleteAllPasswords()
        testee.commands.test {
            awaitItem().verifyDoesHaveCommandToShowUndoDeletionSnackbar(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDeleteAllPasswordsInitialSelection - many saved logins - show confirmation dialog`() = runTest {
        configureStoreToHaveThisManyCredentialsStored(100)
        testee.onViewCreated()
        testee.onDeleteAllPasswordsInitialSelection()
        testee.commandsListView.test {
            awaitItem().verifyHasCommandToShowDeleteAllConfirmation(100)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onViewStarted - no surveys available - no survey in viewState`() = runTest {
        testee.onViewStarted()
        verifySurveyNotAvailable()
    }

    @Test
    fun `onInitialiseListMode - unused survey available - survey in viewState`() = runTest {
        whenever(autofillSurvey.firstUnusedSurvey()).thenReturn(SurveyDetails("surveyId-1", "example.com"))
        testee.onInitialiseListMode()
        "surveyId-1".verifySurveyAvailable()
    }

    @Test
    fun `onSurveyShown - no survey in viewState`() = runTest {
        testee.onSurveyShown("surveyId-1")
        verifySurveyNotAvailable()
    }

    @Test
    fun `onSurveyShown - survey marked as used`() = runTest {
        testee.onSurveyShown("surveyId-1")
        verify(autofillSurvey).recordSurveyAsUsed("surveyId-1")
    }

    @Test
    fun `onSurveyPromptDismissed - no survey in viewState`() = runTest {
        testee.onSurveyPromptDismissed("surveyId-1")
        verifySurveyNotAvailable()
    }

    @Test
    fun `onSurveyPromptDismissed - survey marked as used`() = runTest {
        testee.onSurveyPromptDismissed("surveyId-1")
        verify(autofillSurvey).recordSurveyAsUsed("surveyId-1")
    }

    private fun String.verifySurveyAvailable() {
        val survey = testee.viewState.value.survey
        assertNotNull(survey)
        assertEquals(this, survey!!.id)
    }

    private fun verifySurveyNotAvailable() {
        val survey = testee.viewState.value.survey
        assertNull(survey)
    }

    private fun List<ListModeCommand>.verifyHasCommandToShowDeleteAllConfirmation(expectedNumberOfCredentialsToDelete: Int) {
        val confirmationCommand = this.firstOrNull { it is LaunchDeleteAllPasswordsConfirmation }
        assertNotNull(confirmationCommand)
        assertEquals(expectedNumberOfCredentialsToDelete, (confirmationCommand as LaunchDeleteAllPasswordsConfirmation).numberToDelete)
    }

    private fun List<ListModeCommand>.verifyDoesNotHaveCommandToShowDeleteAllConfirmation() {
        val confirmationCommand = this.firstOrNull { it is LaunchDeleteAllPasswordsConfirmation }
        assertNull(confirmationCommand)
    }

    private fun List<Command>.verifyDoesHaveCommandToShowUndoDeletionSnackbar(expectedNumberOfCredentialsToDelete: Int) {
        val confirmationCommand = this.firstOrNull { it is OfferUserUndoMassDeletion }
        assertNotNull(confirmationCommand)
        assertEquals(expectedNumberOfCredentialsToDelete, (confirmationCommand as OfferUserUndoMassDeletion).credentials.size)
    }

    private fun List<ListModeCommand>.verifyHasCommandToAuthenticateMassDeletion() {
        val command = this.firstOrNull { it is PromptUserToAuthenticateMassDeletion }
        assertNotNull(command)
        assertTrue((command as PromptUserToAuthenticateMassDeletion).authConfiguration.requireUserAction)
    }

    private fun List<Command>.verifyDoesNotHaveCommandToShowUndoDeletionSnackbar() {
        val confirmationCommand = this.firstOrNull { it is OfferUserUndoMassDeletion }
        assertNull(confirmationCommand)
    }

    private suspend fun configureStoreToHaveThisManyCredentialsStored(value: Int) {
        whenever(mockStore.getCredentialCount()).thenReturn(flowOf(value))

        val credentialList = mutableListOf<LoginCredentials>()
        repeat(value) { credentialList.add(someCredentials()) }
        whenever(mockStore.getAllCredentials()).thenReturn(flowOf(credentialList))
    }

    private fun configureDeviceToHaveValidAuthentication(hasValidAuth: Boolean) {
        whenever(deviceAuthenticator.hasValidDeviceAuthentication()).thenReturn(hasValidAuth)
    }

    private fun configureDeviceToBeUnsupported() {
        whenever(mockStore.autofillAvailable).thenReturn(false)
    }

    private fun configureDeviceToBeSupported() {
        whenever(mockStore.autofillAvailable).thenReturn(true)
    }

    private fun someCredentials(): LoginCredentials {
        return LoginCredentials(
            id = -1,
            domain = "example.com",
            username = "username",
            password = "password",
        )
    }

    private fun Command.assertCommandType(expectedType: KClass<out Command>) {
        assertTrue(String.format("Unexpected command type: %s", this::class.simpleName), this::class == expectedType)
    }
}

private class TestFilterPassthrough : CredentialListFilter {
    override suspend fun filter(
        originalList: List<LoginCredentials>,
        query: String,
    ): List<LoginCredentials> {
        return originalList
    }
}
