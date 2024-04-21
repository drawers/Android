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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.autofill.api.domain.app.LoginCredentials
import com.duckduckgo.autofill.impl.encoding.TestUrlUnicodeNormalizer
import com.duckduckgo.autofill.impl.ui.credential.management.sorting.CredentialInitialExtractor
import com.duckduckgo.autofill.impl.ui.credential.management.sorting.CredentialInitialExtractor.Companion.INITIAL_CHAR_FOR_NON_LETTERS
import com.duckduckgo.autofill.impl.urlmatcher.AutofillDomainNameUrlMatcher
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialInitialExtractorTest {

    private val unicodeNormalizer = TestUrlUnicodeNormalizer()
    private val testee = CredentialInitialExtractor(autofillUrlMatcher = AutofillDomainNameUrlMatcher(unicodeNormalizer))

    @Test
    fun `whenMissingTitleAndDomainThenPlaceholderChar - placeholder char`() {
        val result = testee.extractInitial(creds(title = null, domain = null))
        result.assertIsPlaceholder()
    }

    @Test
    fun `whenEmptyStringTitleAndEmptyStringDomainThenPlaceholderChar - placeholder char`() {
        val result = testee.extractInitial(creds(title = "", domain = ""))
        result.assertIsPlaceholder()
    }

    @Test
    fun `whenMissingTitleThenDomainInitialUsed - missing title - E`() {
        val loginCredentials = creds(title = null, domain = "example.com")
        val result = testee.extractInitial(loginCredentials)
        assertEquals("E", result)
    }

    @Test
    fun `whenTitleIsPresentThenTitleInitialIsUsedAndDomainIsIgnored - title present - initial used and domain ignored`() {
        val loginCredentials = creds(title = "A website", domain = "example.com")
        val result = testee.extractInitial(loginCredentials)
        assertEquals("A", result)
    }

    @Test
    fun `whenTitleStartsWithANumberThenPlaceholderUsed - placeholder used`() {
        val loginCredentials = creds(title = "123 website")
        testee.extractInitial(loginCredentials).assertIsPlaceholder()
    }

    @Test
    fun `whenTitleStartsWithASpecialCharacterThenPlaceholderUsed - placeholder used`() {
        val loginCredentials = creds(title = "$123 website")
        testee.extractInitial(loginCredentials).assertIsPlaceholder()
    }

    @Test
    fun `whenTitleStartsWithANonLatinLetterThatCannotBeDecomposedThenOriginalLetterIsUsed - extract initial - ß`() {
        val loginCredentials = creds(title = "ß website")
        assertEquals("ß", testee.extractInitial(loginCredentials))
    }

    @Test
    fun `whenDomainStartsWithANonLatinLetterThatCannotBeDecomposedThenOriginalLetterIsUsed - extract initial - ß`() {
        val loginCredentials = creds(title = "ß.com")
        assertEquals("ß", testee.extractInitial(loginCredentials))
    }

    @Test
    fun `whenTitleStartsWithAnAccentedLetterThenThatBaseLetterIsUsed - extract initial - base letter used`() {
        val loginCredentials = creds(title = "Ça va website")
        assertEquals("C", testee.extractInitial(loginCredentials))
    }

    @Test
    fun `whenDomainStartsWithAnAccentedLetterThenThatBaseLetterIsUsed - extract initial - C`() {
        unicodeNormalizer.overrides["ça.com"] = "ca.com"
        val loginCredentials = creds(domain = "ça.com")
        assertEquals("C", testee.extractInitial(loginCredentials))
    }

    @Test
    fun `whenTitleStartsWithANonLatinLetterThenThatLetterIsUsed - extract initial - non Latin letter used`() {
        val loginCredentials = creds(title = "あ")
        assertEquals("あ", testee.extractInitial(loginCredentials))
    }

    @Test
    fun `whenSubdomainIsPresentThenNotUsedForInitialExtraction - not used for initial extraction`() {
        val loginCredentials = creds(domain = "a.example.com")
        assertEquals("E", testee.extractInitial(loginCredentials))
    }

    private fun String.assertIsPlaceholder() {
        assertEquals(INITIAL_CHAR_FOR_NON_LETTERS, this)
    }

    private fun creds(title: String? = null, domain: String? = null): LoginCredentials {
        return LoginCredentials(
            domainTitle = title,
            domain = domain,
            username = "",
            password = "",
        )
    }
}
