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

package com.duckduckgo.app.browser

import android.net.Uri
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.referral.AppReferrerDataStore
import com.duckduckgo.app.statistics.model.Atb
import com.duckduckgo.app.statistics.store.StatisticsDataStore
import com.duckduckgo.common.utils.AppUrl.ParamKey
import com.duckduckgo.experiments.api.VariantManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class DuckDuckGoRequestRewriterTest {

    private lateinit var testee: DuckDuckGoRequestRewriter
    private val mockStatisticsStore: StatisticsDataStore = mock()
    private val mockVariantManager: VariantManager = mock()
    private val mockAppReferrerDataStore: AppReferrerDataStore = mock()
    private lateinit var builder: Uri.Builder
    private val currentUrl = "http://www.duckduckgo.com"

    @Before
    fun before() {
        whenever(mockVariantManager.getVariantKey()).thenReturn("")
        whenever(mockAppReferrerDataStore.installedFromEuAuction).thenReturn(false)
        testee = DuckDuckGoRequestRewriter(
            DuckDuckGoUrlDetectorImpl(),
            mockStatisticsStore,
            mockVariantManager,
            mockAppReferrerDataStore,
        )
        builder = Uri.Builder()
    }

    @Test
    fun `addCustomQueryParams - source parameter added`() {
        testee.addCustomQueryParams(builder)
        val uri = builder.build()
        assertTrue(uri.queryParameterNames.contains(ParamKey.SOURCE))
        assertEquals("ddg_android", uri.getQueryParameter(ParamKey.SOURCE))
    }

    @Test
    fun `addCustomQueryParams - user sourced from eu auction - eu source parameter added`() {
        whenever(mockAppReferrerDataStore.installedFromEuAuction).thenReturn(true)
        testee.addCustomQueryParams(builder)
        val uri = builder.build()
        assertTrue(uri.queryParameterNames.contains(ParamKey.SOURCE))
        assertEquals("ddg_androideu", uri.getQueryParameter(ParamKey.SOURCE))
    }

    @Test
    fun `addCustomQueryParams - store contains atb is added`() {
        whenever(mockStatisticsStore.atb).thenReturn(Atb("v105-2ma"))
        testee.addCustomQueryParams(builder)
        val uri = builder.build()
        assertTrue(uri.queryParameterNames.contains(ParamKey.ATB))
        assertEquals("v105-2ma", uri.getQueryParameter(ParamKey.ATB))
    }

    @Test
    fun `addCustomQueryParams - is store missing atb - atb not added`() {
        whenever(mockStatisticsStore.atb).thenReturn(null)

        testee.addCustomQueryParams(builder)
        val uri = builder.build()
        assertFalse(uri.queryParameterNames.contains(ParamKey.ATB))
    }

    @Test
    fun `addCustomQueryParams - serp removal feature active - hide param added to serp url`() {
        testee.addCustomQueryParams(builder)

        val uri = builder.build()
        assertTrue(uri.queryParameterNames.contains(ParamKey.HIDE_SERP))
    }

    @Test
    fun `shouldRewriteRequest - serp query - returns true`() {
        val uri = "http://duckduckgo.com/?q=weather".toUri()
        assertTrue(testee.shouldRewriteRequest(uri))
    }

    @Test
    fun `shouldRewriteRequest - serp query with source and atb - return false`() {
        val uri = "http://duckduckgo.com/?q=weather&atb=test&t=test".toUri()
        assertFalse(testee.shouldRewriteRequest(uri))
    }

    @Test
    fun `shouldRewriteRequest - url is a duckduckgo static url - returns true`() {
        val uri = "http://duckduckgo.com/settings".toUri()
        assertTrue(testee.shouldRewriteRequest(uri))

        val uri2 = "http://duckduckgo.com/params".toUri()
        assertTrue(testee.shouldRewriteRequest(uri2))
    }

    @Test
    fun `shouldRewriteRequest - url is duckduckgo email - return false`() {
        val uri = "http://duckduckgo.com/email".toUri()
        assertFalse(testee.shouldRewriteRequest(uri))
    }
}
