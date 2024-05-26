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

package com.duckduckgo.app.referral

import com.duckduckgo.app.referral.ParsedReferrerResult.CampaignReferrerFound
import com.duckduckgo.app.referral.ParsedReferrerResult.EuAuctionBrowserChoiceReferrerFound
import com.duckduckgo.app.referral.ParsedReferrerResult.EuAuctionSearchChoiceReferrerFound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryParamReferrerParserTest {

    private val testee: QueryParamReferrerParser = QueryParamReferrerParser()

    @Test
    fun `parse - referrer does not contain target - no referrer found`() {
        verifyReferrerNotFound(testee.parse("ABC"))
    }

    @Test
    fun `parse - referrer contains target and long suffix - shortened referrer found`() {
        val result = testee.parse("DDGRAABC")
        verifyCampaignReferrerFound("AB", result)
    }

    @Test
    fun `parse - referrer contains target and two char suffix - referrer found`() {
        val result = testee.parse("DDGRAXY")
        verifyCampaignReferrerFound("XY", result)
    }

    @Test
    fun `parse - referrer contains target and one char suffix - no referrer found`() {
        val result = testee.parse("DDGRAX")
        verifyReferrerNotFound(result)
    }

    @Test
    fun `parse - referrer contains target but no suffix - no referrer found`() {
        val result = testee.parse("DDGRAX")
        verifyReferrerNotFound(result)
    }

    @Test
    fun `parse - referrer empty - no referrer found`() {
        verifyReferrerNotFound(testee.parse(""))
    }

    @Test
    fun `parse - referrer contains target as first param - referrer found`() {
        val result = testee.parse("key1=DDGRAAB&key2=foo&key3=bar")
        verifyCampaignReferrerFound("AB", result)
    }

    @Test
    fun `parse - referrer contains target as last param - referrer found`() {
        val result = testee.parse("key1=foo&key2=bar&key3=DDGRAAB")
        verifyCampaignReferrerFound("AB", result)
    }

    @Test
    fun `parse - referrer contains target with different case - no referrer found`() {
        verifyReferrerNotFound(testee.parse("ddgraAB"))
    }

    @Test
    fun `parse - referrer contains eu auction search choice data - eu action referrer found`() {
        val result = testee.parse("$INSTALLATION_SOURCE_KEY=$INSTALLATION_SOURCE_EU_SEARCH_CHOICE_AUCTION_VALUE")
        assertTrue(result is EuAuctionSearchChoiceReferrerFound)
    }

    @Test
    fun `parse - referrer contains eu auction search choice and campaign referrer data - eu action referrer found`() {
        val result = testee.parse("key1=DDGRAAB&key2=foo&key3=bar&$INSTALLATION_SOURCE_KEY=$INSTALLATION_SOURCE_EU_SEARCH_CHOICE_AUCTION_VALUE")
        assertTrue(result is EuAuctionSearchChoiceReferrerFound)
    }

    @Test
    fun `parse - referrer contains installation source key but not matching value - no referrer found`() {
        val result = testee.parse("$INSTALLATION_SOURCE_KEY=bar")
        verifyReferrerNotFound(result)
    }

    @Test
    fun `parse - referrer contains installation source key and no eu auction value but has campaign referrer data - campaign referrer found`() {
        val result = testee.parse("key1=DDGRAAB&key2=foo&key3=bar&$INSTALLATION_SOURCE_KEY=bar")
        verifyCampaignReferrerFound("AB", result)
    }

    @Test
    fun `parse - referrer contains eu auction browser choice data - eu action referrer found`() {
        val result = testee.parse("$INSTALLATION_SOURCE_KEY=$INSTALLATION_SOURCE_EU_BROWSER_CHOICE_AUCTION_VALUE")
        assertTrue(result is EuAuctionBrowserChoiceReferrerFound)
    }

    @Test
    fun `parse - referrer contains eu auction browser choice and campaign referrer data - eu action referrer found`() {
        val result = testee.parse("key1=DDGRAAB&key2=foo&key3=bar&$INSTALLATION_SOURCE_KEY=$INSTALLATION_SOURCE_EU_BROWSER_CHOICE_AUCTION_VALUE")
        assertTrue(result is EuAuctionBrowserChoiceReferrerFound)
    }

    private fun verifyCampaignReferrerFound(
        expectedReferrer: String,
        result: ParsedReferrerResult,
    ) {
        assertTrue(result is CampaignReferrerFound)
        val value = (result as CampaignReferrerFound).campaignSuffix
        assertEquals(expectedReferrer, value)
    }

    private fun verifyReferrerNotFound(result: ParsedReferrerResult) {
        assertTrue(result is ParsedReferrerResult.ReferrerNotFound)
    }

    companion object {
        private const val INSTALLATION_SOURCE_KEY = "utm_source"
        private const val INSTALLATION_SOURCE_EU_SEARCH_CHOICE_AUCTION_VALUE = "eea-search-choice"
        private const val INSTALLATION_SOURCE_EU_BROWSER_CHOICE_AUCTION_VALUE = "eea-browser-choice"
    }
}
