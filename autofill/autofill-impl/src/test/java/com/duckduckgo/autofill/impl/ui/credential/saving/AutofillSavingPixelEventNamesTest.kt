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

package com.duckduckgo.autofill.impl.ui.credential.saving

import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_SAVE_LOGIN_PROMPT_DISMISSED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_SAVE_LOGIN_PROMPT_SAVED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_SAVE_LOGIN_PROMPT_SHOWN
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_SAVE_PASSWORD_PROMPT_DISMISSED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_SAVE_PASSWORD_PROMPT_SAVED
import com.duckduckgo.autofill.impl.pixel.AutofillPixelNames.AUTOFILL_SAVE_PASSWORD_PROMPT_SHOWN
import com.duckduckgo.autofill.impl.ui.credential.saving.AutofillSavingCredentialsDialogFragment.AutofillSavingPixelEventNames.Companion.pixelNameDialogAccepted
import com.duckduckgo.autofill.impl.ui.credential.saving.AutofillSavingCredentialsDialogFragment.AutofillSavingPixelEventNames.Companion.pixelNameDialogDismissed
import com.duckduckgo.autofill.impl.ui.credential.saving.AutofillSavingCredentialsDialogFragment.AutofillSavingPixelEventNames.Companion.pixelNameDialogShown
import com.duckduckgo.autofill.impl.ui.credential.saving.AutofillSavingCredentialsDialogFragment.AutofillSavingPixelEventNames.Companion.saveType
import com.duckduckgo.autofill.impl.ui.credential.saving.AutofillSavingCredentialsDialogFragment.CredentialSaveType.PasswordOnly
import com.duckduckgo.autofill.impl.ui.credential.saving.AutofillSavingCredentialsDialogFragment.CredentialSaveType.UsernameAndPassword
import com.duckduckgo.autofill.impl.ui.credential.saving.AutofillSavingCredentialsDialogFragment.CredentialSaveType.UsernameOnly
import org.junit.Assert.assertEquals
import org.junit.Test

class AutofillSavingPixelEventNamesTest {

    @Test
    fun `whenSavingAcceptedWithUsernameAndPasswordThenCorrectPixelUsed - username and password saved - correct pixel used`() {
        assertEquals(pixelNameDialogAccepted(UsernameAndPassword), AUTOFILL_SAVE_LOGIN_PROMPT_SAVED)
    }

    @Test
    fun `whenSavingAcceptedWithPasswordOnlyThenCorrectPixelUsed - password only - correct pixel used`() {
        assertEquals(pixelNameDialogAccepted(PasswordOnly), AUTOFILL_SAVE_PASSWORD_PROMPT_SAVED)
    }

    @Test
    fun `whenDialogShownWithUsernameAndPasswordThenCorrectPixelUsed - username and password - correct pixel used`() {
        assertEquals(pixelNameDialogShown(UsernameAndPassword), AUTOFILL_SAVE_LOGIN_PROMPT_SHOWN)
    }

    @Test
    fun `whenDialogShownWithPasswordOnlyThenCorrectPixelUsed - password only - correct pixel used`() {
        assertEquals(pixelNameDialogShown(PasswordOnly), AUTOFILL_SAVE_PASSWORD_PROMPT_SHOWN)
    }

    @Test
    fun `whenDialogDismissedWithUsernameAndPasswordThenCorrectPixelUsed - dialog dismissed with username and password - correct pixel used`() {
        assertEquals(pixelNameDialogDismissed(UsernameAndPassword), AUTOFILL_SAVE_LOGIN_PROMPT_DISMISSED)
    }

    @Test
    fun `whenDialogDismissedWithPasswordOnlyThenCorrectPixelUsed - password only - correct pixel used`() {
        assertEquals(pixelNameDialogDismissed(PasswordOnly), AUTOFILL_SAVE_PASSWORD_PROMPT_DISMISSED)
    }

    @Test
    fun `whenUsernameAndPasswordProvidedThenSaveTypeIsUsernameAndPassword - save type is username and password`() {
        val loginCredentials = loginCredentials(username = "username", password = "password")
        assertEquals(loginCredentials.saveType(), UsernameAndPassword)
    }

    @Test
    fun `whenUsernameOnlyProvidedThenSaveTypeIsUsernameOnly - save type is username only`() {
        val loginCredentials = loginCredentials(username = "username", password = null)
        assertEquals(loginCredentials.saveType(), UsernameOnly)
    }

    @Test
    fun `whenPassworldOnlyProvidedThenSaveTypeIsUsernameOnly - save type is username only`() {
        val loginCredentials = loginCredentials(username = null, password = "password")
        assertEquals(loginCredentials.saveType(), PasswordOnly)
    }

    private fun loginCredentials(username: String? = null, password: String? = null): LoginCredentials {
        return LoginCredentials(id = 0, domain = "example.com", username = username, password = password)
    }
}
