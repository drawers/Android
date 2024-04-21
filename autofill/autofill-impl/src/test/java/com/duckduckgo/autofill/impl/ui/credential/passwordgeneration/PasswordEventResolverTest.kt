/*
 * Copyright (c) 2023 DuckDuckGo
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

package com.duckduckgo.autofill.impl.ui.credential.passwordgeneration

import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.impl.ui.credential.passwordgeneration.Actions.DeleteAutoLogin
import com.duckduckgo.autofill.impl.ui.credential.passwordgeneration.Actions.DiscardAutoLoginId
import com.duckduckgo.autofill.impl.ui.credential.passwordgeneration.Actions.PromptToSave
import com.duckduckgo.autofill.impl.ui.credential.passwordgeneration.Actions.UpdateSavedAutoLogin
import org.junit.Assert.*
import org.junit.Test

class PasswordEventResolverTest {

    private val testee = PasswordEventResolver()

    @Test
    fun `decideActions - no auto saved login and not autogenerated credentials - prompt user to save`() {
        val autoSavedLogin = null
        val actions = testee.decideActions(autoSavedLogin, autogenerated = false)
        actions.assertExpectedActions(PromptToSave)
    }

    @Test
    fun `decideActions - no auto saved login and autogenerated credentials - prompt to save`() {
        val autoSavedLogin = null
        val actions = testee.decideActions(autoSavedLogin, autogenerated = true)
        actions.assertExpectedActions(PromptToSave)
    }

    @Test
    fun `decideActions - auto saved login and not autogenerated credentials - delete auto login and discard auto login id and prompt to save`() {
        val autoSavedLogin = loginCreds(id = 1)
        val actions = testee.decideActions(autoSavedLogin, autogenerated = false)
        actions.assertExpectedActions(
            DeleteAutoLogin(1),
            DiscardAutoLoginId,
            PromptToSave,
        )
    }

    @Test
    fun `decideActions - auto saved login and autogenerated credentials - update saved login`() {
        val autoSavedLogin = loginCreds(id = 1)
        val actions = testee.decideActions(autoSavedLogin, autogenerated = true)
        actions.assertExpectedActions(UpdateSavedAutoLogin(1))
    }

    private fun loginCreds(id: Long): LoginCredentials {
        return LoginCredentials(id = id, domain = "example.com", username = "username", password = "password")
    }

    private fun <Action> List<Action>.assertExpectedActions(vararg expectedActions: Action) {
        assertEquals("Number of actions is unexpected", expectedActions.size, this.size)
        expectedActions.forEach { assertTrue("Action $it is not present", this.contains(it)) }
        forEach { assertTrue("Action $it is not expected", expectedActions.contains(it)) }
    }
}
