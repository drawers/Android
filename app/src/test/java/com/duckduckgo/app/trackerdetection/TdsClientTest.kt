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

package com.duckduckgo.app.trackerdetection

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.browser.Domain
import com.duckduckgo.app.trackerdetection.Client.ClientName.TDS
import com.duckduckgo.app.trackerdetection.model.Action
import com.duckduckgo.app.trackerdetection.model.Action.BLOCK
import com.duckduckgo.app.trackerdetection.model.Action.IGNORE
import com.duckduckgo.app.trackerdetection.model.Options
import com.duckduckgo.app.trackerdetection.model.Rule
import com.duckduckgo.app.trackerdetection.model.RuleExceptions
import com.duckduckgo.app.trackerdetection.model.TdsTracker
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyMap
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class TdsClientTest {

    private val mockUrlToTypeMapper: UrlToTypeMapper = mock()
    private val trackerDomain = Domain("tracker.com")
    private val ruleString = "api\\.tracker\\.com\\/auth"
    private val ruleBlockNullExceptions = Rule(ruleString, BLOCK, null, null, null)
    private val ruleIgnoreNullExceptions = Rule(ruleString, IGNORE, null, null, null)
    private val ruleNullExceptions = Rule(ruleString, null, null, null, null)
    private val url = "http://api.tracker.com/auth/script.js"
    private val imageUrl = "http://api.tracker.com/auth/image.png"
    private val image = "image"

    @Test
    fun `matches - url has same domain as tracker entry and default action block - is true`() {
        test(url = "http://tracker.com/script.js", trackerDomain = trackerDomain, action = BLOCK, expected = true)
    }

    @Test
    fun `test - url has same domain as tracker entry and default action ignore - matches is false`() {
        test(url = "http://tracker.com/script.js", trackerDomain = trackerDomain, action = IGNORE, expected = false)
    }

    @Test
    fun `matches - url is subdomain of tracker entry and default action block - true`() {
        test(url = "http://subdomian.tracker.com/script.js", trackerDomain = trackerDomain, action = BLOCK, expected = true)
    }

    @Test
    fun `test - url not domain or subdomain of tracker entry - matches is false`() {
        test(url = "http://nontracker.com/script.js", trackerDomain = trackerDomain, action = BLOCK, expected = false)
    }

    @Test
    fun `test - url is a parent domain of a tracker entry - matches is false`() {
        test(url = "http://tracker.com/script.js", trackerDomain = Domain("subdomain.tracker.com"), action = BLOCK, expected = false)
    }

    @Test
    fun `matches - url contains but is not subdomain of a tracker entry - matches is false`() {
        test(url = "http://notsubdomainoftracker.com", trackerDomain = trackerDomain, action = BLOCK, expected = false)
    }

    @Test
    fun `whenUrlMatchesRule - no exceptions and block action - matches is true`() {
        test(rule = ruleBlockNullExceptions, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = true)
    }

    @Test
    fun `test - url matches rule with no exceptions and rule action ignore - matches is false`() {
        test(rule = ruleIgnoreNullExceptions, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = false)
    }

    @Test
    fun `test - url matches domain with default block and rule - matches is true`() {
        test(rule = ruleNullExceptions, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = true)
    }

    @Test
    fun `matches - domain matches with default ignore and no exceptions - is true`() {
        test(rule = ruleNullExceptions, url = url, trackerDomain = trackerDomain, action = IGNORE, expected = true)
    }

    @Test
    fun `test - matches is false - irrespective of action`() {
        val exampleException = RuleExceptions(listOf("example.com"), null)

        val ruleBlock = Rule(ruleString, BLOCK, exampleException, null, null)
        val ruleIgnore = Rule(ruleString, IGNORE, exampleException, null, null)
        val ruleNone = Rule(ruleString, null, exampleException, null, null)

        test(rule = ruleBlock, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = false)
        test(rule = ruleBlock, url = url, trackerDomain = trackerDomain, action = IGNORE, expected = false)
        test(rule = ruleIgnore, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = false)
        test(rule = ruleIgnore, url = url, trackerDomain = trackerDomain, action = IGNORE, expected = false)
        test(rule = ruleNone, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = false)
        test(rule = ruleNone, url = url, trackerDomain = trackerDomain, action = IGNORE, expected = false)
    }

    @Test
    fun `whenUrlMatchesRuleWithExceptionsAndExceptionDomainDoesNotMatchDocumentThenMatchesBehaviorIsStandard - matches behavior is standard`() {
        val nonMatchingExceptions = RuleExceptions(listOf("nonmatching.com"), null)

        val ruleBlock = Rule(ruleString, BLOCK, nonMatchingExceptions, null, null)
        val ruleIgnore = Rule(ruleString, IGNORE, nonMatchingExceptions, null, null)
        val ruleNone = Rule(ruleString, null, nonMatchingExceptions, null, null)

        test(rule = ruleBlock, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = true)
        test(rule = ruleBlock, url = url, trackerDomain = trackerDomain, action = IGNORE, expected = true)
        test(rule = ruleIgnore, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = false)
        test(rule = ruleIgnore, url = url, trackerDomain = trackerDomain, action = IGNORE, expected = false)
        test(rule = ruleNone, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = true)
        test(rule = ruleNone, url = url, trackerDomain = trackerDomain, action = IGNORE, expected = true)
    }

    @Test
    fun `test - matches is false - irrelevant of action`() {
        val exceptions = RuleExceptions(null, listOf("something"))

        val ruleBlock = Rule("api\\.tracker\\.com\\/auth", BLOCK, exceptions, null, null)
        val ruleIgnore = Rule("api\\.tracker\\.com\\/auth", IGNORE, exceptions, null, null)
        val ruleNone = Rule("api\\.tracker\\.com\\/auth", null, exceptions, null, null)

        val mapResult = "something"

        test(mapResult = mapResult, rule = ruleBlock, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = false)
        test(mapResult = mapResult, rule = ruleBlock, url = url, trackerDomain = trackerDomain, action = IGNORE, expected = false)
        test(mapResult = mapResult, rule = ruleIgnore, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = false)
        test(mapResult = mapResult, rule = ruleIgnore, url = url, trackerDomain = trackerDomain, action = IGNORE, expected = false)
        test(mapResult = mapResult, rule = ruleNone, url = url, trackerDomain = trackerDomain, action = BLOCK, expected = false)
        test(mapResult = mapResult, rule = ruleNone, url = url, trackerDomain = trackerDomain, action = IGNORE, expected = false)
    }

    @Test
    fun `testUrlMatchesRuleWithSurrogate - matches is true - except ignore`() {
        val exceptions = RuleExceptions(null, null)

        val ruleBlock = Rule("api\\.tracker\\.com\\/auth", BLOCK, exceptions, "testId", null)
        val ruleIgnore = Rule("api\\.tracker\\.com\\/auth", IGNORE, exceptions, "testId", null)
        val ruleNone = Rule("api\\.tracker\\.com\\/auth", null, exceptions, "testId", null)

        test(rule = ruleBlock, action = BLOCK, url = url, trackerDomain = trackerDomain, expected = true)
        test(rule = ruleBlock, action = IGNORE, url = url, trackerDomain = trackerDomain, expected = true)
        test(rule = ruleIgnore, action = BLOCK, url = url, trackerDomain = trackerDomain, expected = false)
        test(rule = ruleIgnore, action = IGNORE, url = url, trackerDomain = trackerDomain, expected = false)
        test(rule = ruleNone, action = BLOCK, url = url, trackerDomain = trackerDomain, expected = true)
        test(rule = ruleNone, action = IGNORE, url = url, trackerDomain = trackerDomain, expected = true)
    }

    @Test
    fun `whenUrlMatchesRule - type exception and domains is null - matches is false`() {
        test(
            mapResult = image,
            rule = Rule(ruleString, BLOCK, RuleExceptions(null, listOf(image)), null, null),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = false,
        )
    }

    @Test
    fun `whenUrlMatchesRuleWithTypeExceptionAndDomainsIsEmpty - matches is false`() {
        test(
            mapResult = image,
            rule = Rule(ruleString, BLOCK, RuleExceptions(emptyList(), listOf(image)), null, null),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = false,
        )
    }

    @Test
    fun `mapRule - domain exception and types is null - matches is false`() {
        test(
            mapResult = image,
            rule = Rule(ruleString, BLOCK, RuleExceptions(listOf("example.com"), null), null, null),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = false,
        )
    }

    @Test
    fun `matchesRule - domain exception and types is empty - matches is false`() {
        test(
            mapResult = image,
            rule = Rule(
                ruleString,
                BLOCK,
                RuleExceptions(listOf("example.com"), emptyList()),
                null,
                null,
            ),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = false,
        )
    }

    @Test
    fun `matches - url matches rule with domain and type exception - is false`() {
        test(
            mapResult = image,
            rule = Rule(ruleString, BLOCK, RuleExceptions(listOf("example.com"), listOf(image)), null, null),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = false,
        )
    }

    @Test
    fun `test - url matches rule with domain exception but not type - matches is true`() {
        test(
            mapResult = image,
            rule = Rule(ruleString, BLOCK, RuleExceptions(listOf("example.com"), listOf("script")), null, null),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = true,
        )
    }

    @Test
    fun `test - url matches rule with type exception but not domain - matches is true`() {
        test(
            mapResult = image,
            rule = Rule(ruleString, BLOCK, RuleExceptions(listOf("foo.com"), listOf(image)), null, null),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = true,
        )
    }

    @Test
    fun `whenUrlMatchesRule - rule matches exception and option type empty domain - matches false`() {
        // If option domain is empty and type is matching, should block would be false since exception is matching.
        test(
            mapResult = image,
            rule = Rule(
                ruleString,
                BLOCK,
                RuleExceptions(listOf("example.com"), listOf(image)),
                null,
                Options(domains = null, types = listOf(image)),
            ),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = false,
        )
    }

    @Test
    fun `whenUrlMatchesRule - option domain and empty option type then matches false`() {
        // If option type is empty and domain is matching, should block would be false since exception is matching.
        test(
            mapResult = image,
            rule = Rule(
                ruleString,
                BLOCK,
                RuleExceptions(listOf("example.com"), listOf(image)),
                null,
                Options(domains = listOf("example.com"), types = null),
            ),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = false,
        )
    }

    @Test
    fun `whenUrlMatchesRule - matches true - blocked tracker with matching exception and option domain but not option type`() {
        // If option type is not null and not matching, should block would be true since we will use the tracker's default action.
        test(
            mapResult = image,
            rule = Rule(
                ruleString,
                BLOCK,
                RuleExceptions(listOf("example.com"), listOf(image)),
                null,
                Options(domains = listOf("example.com"), types = listOf("not-matching-type")),
            ),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = true,
        )
    }

    @Test
    fun `whenUrlMatchesRule - matches true - blocked tracker with matching exception but not option domain and option type`() {
        // If option domain is not null and not matching, should block would be true since we will use the tracker's default action.
        test(
            mapResult = image,
            rule = Rule(
                ruleString,
                BLOCK,
                RuleExceptions(listOf("example.com"), listOf(image)),
                null,
                Options(domains = listOf("not-matching-domain.com"), types = listOf(image)),
            ),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = true,
        )
    }

    @Test
    fun `whenHasOptionsButDoesntMatchDomainNorTypeThenMatchesTrue - matches true`() {
        // If option type and domain are both not null and not matching, should block would be true since we will use the tracker's default action.
        test(
            mapResult = image,
            rule = Rule(
                ruleString,
                BLOCK,
                RuleExceptions(listOf("example.com"), listOf(image)),
                null,
                Options(domains = listOf("not-matching-domain.com"), types = listOf("not-matching-type")),
            ),
            trackerDomain = trackerDomain,
            url = imageUrl,
            action = BLOCK,
            expected = true,
        )
    }

    fun test(
        mapResult: String? = null,
        rule: Rule? = null,
        url: String,
        trackerDomain: Domain,
        action: Action,
        expected: Boolean,
    ) {
        mapResult?.let {
            whenever(mockUrlToTypeMapper.map(anyString(), anyMap())).thenReturn(it)
        }

        for (useUri in listOf(false, true)) {
            for (useTestee in listOf(true, false)) {
                val tdsTracker = TdsTracker(trackerDomain, action, OWNER, CATEGORY, rule?.let { listOf(it) } ?: emptyList())
                val testee = TdsClient(TDS, listOf(tdsTracker), mockUrlToTypeMapper, useTestee)
                val result = if (useUri) {
                    testee.matches(url.toUri(), DOCUMENT_URL, mapOf())
                } else {
                    testee.matches(url, DOCUMENT_URL, mapOf())
                }
                assertEquals(expected, result.matches)
            }
        }
    }

    @Test
    fun `matches - url matches rule with surrogate - returns script id`() {
        val rule = Rule("api\\.tracker\\.com\\/auth", BLOCK, null, "script.js", null)

        val testee = TdsClient(TDS, listOf(TdsTracker(Domain("tracker.com"), BLOCK, OWNER, CATEGORY, listOf(rule))), mockUrlToTypeMapper, false)

        assertEquals("script.js", testee.matches("http://api.tracker.com/auth/script.js", DOCUMENT_URL, mapOf()).surrogate)
        assertEquals("script.js", testee.matches("http://api.tracker.com/auth/script.js".toUri(), DOCUMENT_URL, mapOf()).surrogate)
    }

    companion object {
        private const val OWNER = "A Network Owner"
        private val DOCUMENT_URL = "http://example.com/index.htm".toUri()
        private val CATEGORY: List<String> = emptyList()
    }
}
