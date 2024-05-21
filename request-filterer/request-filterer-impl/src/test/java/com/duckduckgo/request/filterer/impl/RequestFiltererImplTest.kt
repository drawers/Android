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

package com.duckduckgo.request.filterer.impl

import android.webkit.WebResourceRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.feature.toggles.api.FeatureExceptions.FeatureException
import com.duckduckgo.feature.toggles.api.FeatureToggle
import com.duckduckgo.privacy.config.api.UnprotectedTemporary
import com.duckduckgo.request.filterer.api.RequestFiltererFeatureName
import com.duckduckgo.request.filterer.impl.RequestFiltererImpl.Companion.ORIGIN
import com.duckduckgo.request.filterer.impl.RequestFiltererImpl.Companion.REFERER
import com.duckduckgo.request.filterer.store.RequestFiltererRepository
import com.duckduckgo.request.filterer.store.SettingsEntity
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class RequestFiltererImplTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private val mockRequest: WebResourceRequest = mock()
    private val mockRepository: RequestFiltererRepository = mock()
    private val mockUnprotectedTemporary: UnprotectedTemporary = mock()
    private val mockFeatureToggle: FeatureToggle = mock()
    private lateinit var requestFilterer: RequestFiltererImpl

    @Before
    fun before() {
        whenever(mockRepository.settings).thenReturn(SettingsEntity(id = 1, windowInMs = WINDOW))
        whenever(mockFeatureToggle.isFeatureEnabled(eq(RequestFiltererFeatureName.RequestFilterer.value), any())).thenReturn(true)

        requestFilterer = RequestFiltererImpl(
            mockRepository,
            mockFeatureToggle,
            mockUnprotectedTemporary,
            coroutineTestRule.testDispatcherProvider,
        )
    }

    @Test
    fun `shouldFilterOutRequest - feature disabled - returns false`() {
        whenever(mockFeatureToggle.isFeatureEnabled(eq(RequestFiltererFeatureName.RequestFilterer.value), any())).thenReturn(false)

        val previousUrl = "http://example.com"
        val documentUrl = "http://test.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(REFERER to previousUrl))
        requestFilterer.registerOnPageCreated(previousUrl)
        requestFilterer.registerOnPageCreated(documentUrl)

        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `shouldFilterOutRequest - url in exceptions list - returns false`() {
        val exceptions = CopyOnWriteArrayList<FeatureException>().apply {
            add(FeatureException("http://test.com", "my reason here"))
        }

        whenever(mockRepository.exceptions).thenReturn(exceptions)
        val previousUrl = "http://example.com"
        val documentUrl = "http://test.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(REFERER to previousUrl))
        requestFilterer.registerOnPageCreated(previousUrl)
        requestFilterer.registerOnPageCreated(documentUrl)

        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `shouldFilterOutRequest - url in unprotected temporary - return false`() {
        whenever(mockUnprotectedTemporary.isAnException("http://test.com")).thenReturn(true)
        val previousUrl = "http://example.com"
        val documentUrl = "http://test.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(REFERER to previousUrl))
        requestFilterer.registerOnPageCreated(previousUrl)
        requestFilterer.registerOnPageCreated(documentUrl)

        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `registerOnPageCreated - referer header matches previous url - returns true`() = runTest {
        val previousUrl = "http://example.com"
        val documentUrl = "http://test.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(REFERER to previousUrl))
        requestFilterer.registerOnPageCreated(previousUrl)
        requestFilterer.registerOnPageCreated(documentUrl)

        assertTrue(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `shouldFilterOutRequest - referer header matches previous url and time has elapsed - return false`() = runTest {
        val previousUrl = "http://example.com"
        val documentUrl = "http://test.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(REFERER to previousUrl))
        requestFilterer.registerOnPageCreated(previousUrl)
        requestFilterer.registerOnPageCreated(documentUrl)
        delay(WINDOW.toLong() + 100)
        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `registerOnPageCreated - origin header matches previous url and no referer header - returns true`() {
        val previousUrl = "http://example.com"
        val documentUrl = "http://test.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(ORIGIN to previousUrl))
        requestFilterer.registerOnPageCreated(previousUrl)
        requestFilterer.registerOnPageCreated(documentUrl)

        assertTrue(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `shouldFilterOutRequest - document url matches previous page - return false`() {
        val previousUrl = "http://example.com"
        val documentUrl = "http://example.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(REFERER to previousUrl))
        requestFilterer.registerOnPageCreated(previousUrl)
        requestFilterer.registerOnPageCreated(documentUrl)

        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `shouldFilterOutRequest - document url matches previous ETL plus one - return false`() {
        val previousUrl = "http://example.com"
        val documentUrl = "http://test.example.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(REFERER to previousUrl))
        requestFilterer.registerOnPageCreated(previousUrl)
        requestFilterer.registerOnPageCreated(documentUrl)

        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `registerOnPageCreated - referer header does not match previous url - return false`() {
        val previousUrl = "http://example.com"
        val documentUrl = "http://test.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(REFERER to "http://notamatch.com"))
        requestFilterer.registerOnPageCreated(previousUrl)
        requestFilterer.registerOnPageCreated(documentUrl)

        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `registerOnPageCreated - request origin header does not match previous url - return false`() {
        val previousUrl = "http://example.com"
        val documentUrl = "http://test.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(ORIGIN to "http://notamatch.com"))
        requestFilterer.registerOnPageCreated(previousUrl)
        requestFilterer.registerOnPageCreated(documentUrl)

        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `registerOnPageCreated - no previous url registered - return false`() {
        val previousUrl = "http://example.com"
        val documentUrl = "http://test.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(REFERER to previousUrl))
        requestFilterer.registerOnPageCreated(documentUrl)

        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `registerOnPageCreated - urls change - should filter out request`() {
        val previousUrl = "http://example.com"
        val documentUrl = "http://test.com"
        whenever(mockRequest.requestHeaders).thenReturn(mapOf(REFERER to previousUrl))
        requestFilterer.registerOnPageCreated(previousUrl)
        requestFilterer.registerOnPageCreated(documentUrl)
        assertTrue(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))

        requestFilterer.registerOnPageCreated("http://notamatch.com")
        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, documentUrl))
    }

    @Test
    fun `registerOnPageCreated - document url is malformed - return false`() {
        requestFilterer.registerOnPageCreated("http://foo.com")
        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, "abc123"))
    }

    @Test
    fun `registerOnPageCreated - previous url malformed - return false`() {
        requestFilterer.registerOnPageCreated("abc123")
        requestFilterer.registerOnPageCreated("http://foo.com")
        assertFalse(requestFilterer.shouldFilterOutRequest(mockRequest, "http://bar.com"))
    }

    companion object {
        private const val WINDOW = 200
    }
}
