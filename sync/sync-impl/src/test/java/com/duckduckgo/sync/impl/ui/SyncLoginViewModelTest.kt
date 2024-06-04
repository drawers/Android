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

package com.duckduckgo.sync.impl.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.sync.TestSyncFixtures.jsonRecoveryKeyEncoded
import com.duckduckgo.sync.impl.Result.Success
import com.duckduckgo.sync.impl.SyncAccountRepository
import com.duckduckgo.sync.impl.pixels.SyncPixels
import com.duckduckgo.sync.impl.ui.SyncLoginViewModel.Command
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SyncLoginViewModelTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val syncRepostitory: SyncAccountRepository = mock()
    private val syncPixels: SyncPixels = mock()

    private val testee = SyncLoginViewModel(
        syncRepostitory,
        syncPixels,
        coroutineTestRule.testDispatcherProvider,
    )

    @Test
    fun `onReadTextCodeClicked - command read text code`() = runTest {
        testee.commands().test {
            testee.onReadTextCodeClicked()
            val command = awaitItem()
            assertTrue(command is Command.ReadTextCode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onQRCodeScanned - perform login and emit result`() = runTest {
        whenever(syncRepostitory.processCode(jsonRecoveryKeyEncoded)).thenReturn(Success(true))

        testee.commands().test {
            testee.onQRCodeScanned(jsonRecoveryKeyEncoded)
            val command = awaitItem()
            assertTrue(command is Command.LoginSucess)
            verify(syncPixels).fireLoginPixel()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onLoginSuccess - command login success`() = runTest {
        testee.commands().test {
            testee.onLoginSuccess()
            val command = awaitItem()
            assertTrue(command is Command.LoginSucess)
            verify(syncPixels).fireLoginPixel()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
