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

package com.duckduckgo.autofill.impl.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.appbuildconfig.api.BuildFlavor.INTERNAL
import com.duckduckgo.appbuildconfig.api.BuildFlavor.PLAY
import com.duckduckgo.autofill.store.InternalTestUserStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class RealInternalTestUserCheckerTest {
    @Mock
    private lateinit var appBuildConfig: AppBuildConfig
    private lateinit var internalTestUserStore: InternalTestUserStore
    private lateinit var testee: RealInternalTestUserChecker

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        internalTestUserStore = FakeInternalTestUserStore()
        testee = RealInternalTestUserChecker(internalTestUserStore, appBuildConfig)

        whenever(appBuildConfig.flavor).thenReturn(PLAY)
    }

    @Test
    fun `whenErrorReceivedForInvalidUrlThenIsNotInternalTestUser - verification error received for invalid URL - not internal test user`() {
        testee.verifyVerificationErrorReceived(INVALID_TEST_URL)
        testee.verifyVerificationCompleted(INVALID_TEST_URL)

        assertFalse(testee.isInternalTestUser)
    }

    @Test
    fun `whenCompletedForInvalidUrlThenisNotInternalTestUser - invalid URL - not internal test user`() {
        testee.verifyVerificationCompleted(INVALID_TEST_URL)

        assertFalse(testee.isInternalTestUser)
    }

    @Test
    fun `whenErrorForValidButHttpUrlThenIsNotInternalTestUser - is not internal test user`() {
        testee.verifyVerificationErrorReceived(VALID_TEST_HTTP_URL)
        testee.verifyVerificationCompleted(VALID_TEST_HTTPS_URL)

        assertFalse(testee.isInternalTestUser)
    }

    @Test
    fun `whenErrorReceivedForValidUrlThenisNotInternalTestUser - is not internal test user`() {
        testee.verifyVerificationErrorReceived(VALID_TEST_HTTPS_URL)
        testee.verifyVerificationCompleted(VALID_TEST_HTTPS_URL)

        assertFalse(testee.isInternalTestUser)
    }

    @Test
    fun `whenCompletedForValidUrlThenisInternalTestUser - is internal test user`() {
        testee.verifyVerificationCompleted(VALID_TEST_HTTPS_URL)

        assertTrue(testee.isInternalTestUser)
    }

    @Test
    fun `whenCompletedForValidButHttpUrlThenisInternalTestUser - is internal test user`() {
        testee.verifyVerificationCompleted(VALID_TEST_HTTP_URL)

        assertTrue(testee.isInternalTestUser)
    }

    @Test
    fun `whenUserBuildIsInternalNoValidationThenIsInternalTestUser - is internal test user`() {
        whenever(appBuildConfig.flavor).thenReturn(INTERNAL)

        assertTrue(testee.isInternalTestUser)
    }

    @Test
    fun `whenUserBuildIsInternalAndErrorReceivedWhenValidatingThenIsInternalTestUser - internal build - verification error received - is internal test user`() {
        whenever(appBuildConfig.flavor).thenReturn(INTERNAL)

        testee.verifyVerificationErrorReceived(VALID_TEST_HTTPS_URL)
        testee.verifyVerificationCompleted(VALID_TEST_HTTPS_URL)

        assertTrue(testee.isInternalTestUser)
    }

    class FakeInternalTestUserStore : InternalTestUserStore {
        private var _value: Boolean = false
        override var isVerifiedInternalTestUser: Boolean
            get() = _value
            set(value) {
                _value = value
            }
    }

    companion object {
        private const val VALID_TEST_HTTP_URL = "http://use-login.duckduckgo.com/patestsucceeded"
        private const val VALID_TEST_HTTPS_URL = "https://use-login.duckduckgo.com/patestsucceeded"
        private const val INVALID_TEST_URL = "https://invalid.com"
    }
}
