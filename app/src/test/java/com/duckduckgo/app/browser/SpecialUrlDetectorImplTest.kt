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

import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.browser.SpecialUrlDetector.UrlType.*
import com.duckduckgo.app.browser.SpecialUrlDetectorImpl.Companion.EMAIL_MAX_LENGTH
import com.duckduckgo.app.browser.SpecialUrlDetectorImpl.Companion.PHONE_MAX_LENGTH
import com.duckduckgo.app.browser.SpecialUrlDetectorImpl.Companion.SMS_MAX_LENGTH
import com.duckduckgo.privacy.config.api.AmpLinkType
import com.duckduckgo.privacy.config.api.AmpLinks
import com.duckduckgo.privacy.config.api.TrackingParameters
import com.duckduckgo.subscriptions.api.Subscriptions
import java.net.URISyntaxException
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class SpecialUrlDetectorImplTest {

    lateinit var testee: SpecialUrlDetector

    @Mock
    lateinit var mockPackageManager: PackageManager

    @Mock
    lateinit var mockAmpLinks: AmpLinks

    @Mock
    lateinit var mockTrackingParameters: TrackingParameters

    @Mock
    lateinit var subscriptions: Subscriptions

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testee = SpecialUrlDetectorImpl(
            packageManager = mockPackageManager,
            ampLinks = mockAmpLinks,
            trackingParameters = mockTrackingParameters,
            subscriptions = subscriptions,
        )
        whenever(mockPackageManager.queryIntentActivities(any(), anyInt())).thenReturn(emptyList())
    }

    @Test
    fun `determineType - url is http - web type detected`() {
        val expected = Web::class
        val actual = testee.determineType("http://example.com")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is http - web address in data`() {
        val type: Web = testee.determineType("http://example.com") as Web
        assertEquals("http://example.com", type.webAddress)
    }

    @Test
    fun `determineType - url is https - web type detected`() {
        val expected = Web::class
        val actual = testee.determineType("https://example.com")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is https - scheme preserved`() {
        val type = testee.determineType("https://example.com") as Web
        assertEquals("https://example.com", type.webAddress)
    }

    @Test
    fun `determineType - no non-browser activities found - return web type`() {
        whenever(mockPackageManager.queryIntentActivities(any(), anyInt())).thenReturn(listOf(buildBrowserResolveInfo()))
        val type = testee.determineType("https://example.com")
        assertTrue(type is Web)
    }

    @Test
    fun `determineType - app link throws URI syntax exception - returns web type`() {
        given(mockPackageManager.queryIntentActivities(any(), anyInt())).willAnswer { throw URISyntaxException("", "") }
        val type = testee.determineType("https://example.com")
        assertTrue(type is Web)
    }

    @Test
    fun `determineType - default non browser activity found - return app link with intent`() {
        whenever(mockPackageManager.resolveActivity(any(), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(buildAppResolveInfo())
        whenever(mockPackageManager.queryIntentActivities(any(), anyInt())).thenReturn(
            listOf(
                buildBrowserResolveInfo(),
                buildAppResolveInfo(),
                ResolveInfo(),
            ),
        )
        val type = testee.determineType("https://example.com")
        verify(mockPackageManager).queryIntentActivities(
            argThat { hasCategory(Intent.CATEGORY_BROWSABLE) },
            eq(PackageManager.GET_RESOLVED_FILTER),
        )
        assertTrue(type is AppLink)
        val appLinkType = type as AppLink
        assertEquals("https://example.com", appLinkType.uriString)
        assertEquals(EXAMPLE_APP_PACKAGE, appLinkType.appIntent!!.component!!.packageName)
        assertEquals(EXAMPLE_APP_ACTIVITY_NAME, appLinkType.appIntent!!.component!!.className)
    }

    @Test
    fun `whenFirstNonBrowserActivityFound - return app link with intent`() {
        whenever(mockPackageManager.resolveActivity(any(), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(null)
        whenever(mockPackageManager.queryIntentActivities(any(), anyInt())).thenReturn(
            listOf(
                buildAppResolveInfo(),
                buildBrowserResolveInfo(),
                ResolveInfo(),
            ),
        )
        val type = testee.determineType("https://example.com")
        verify(mockPackageManager).queryIntentActivities(
            argThat { hasCategory(Intent.CATEGORY_BROWSABLE) },
            eq(PackageManager.GET_RESOLVED_FILTER),
        )
        assertTrue(type is AppLink)
        val appLinkType = type as AppLink
        assertEquals("https://example.com", appLinkType.uriString)
        assertEquals(EXAMPLE_APP_PACKAGE, appLinkType.appIntent!!.component!!.packageName)
        assertEquals(EXAMPLE_APP_ACTIVITY_NAME, appLinkType.appIntent!!.component!!.className)
    }

    @Test
    fun `determineType - no non-browser activity found - return web type`() {
        whenever(mockPackageManager.resolveActivity(any(), eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(null)
        whenever(mockPackageManager.queryIntentActivities(any(), anyInt())).thenReturn(
            listOf(
                buildBrowserResolveInfo(),
                buildAppResolveInfo(),
                ResolveInfo(),
            ),
        )
        val type = testee.determineType("https://example.com")
        verify(mockPackageManager).queryIntentActivities(
            argThat { hasCategory(Intent.CATEGORY_BROWSABLE) },
            eq(PackageManager.GET_RESOLVED_FILTER),
        )
        assertTrue(type is Web)
    }

    @Test
    fun `determineType - url is tel with dashes - telephone type detected`() {
        val expected = Telephone::class
        val actual = testee.determineType("tel:+123-555-12323")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is tel - telephone type detected`() {
        val expected = Telephone::class
        val actual = testee.determineType("tel:12355512323")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is tel - scheme removed`() {
        val type = testee.determineType("tel:+123-555-12323") as Telephone
        assertEquals("+123-555-12323", type.telephoneNumber)
    }

    @Test
    fun `determineType - url is telprompt - telephone type detected`() {
        val expected = Telephone::class
        val actual = testee.determineType("telprompt:12355512323")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is telprompt - scheme removed`() {
        val type = testee.determineType("telprompt:123-555-12323") as Telephone
        assertEquals("123-555-12323", type.telephoneNumber)
    }

    @Test
    fun `determineType - url is mailto - email type detected`() {
        val expected = Email::class
        val actual = testee.determineType("mailto:foo@example.com")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is mailto - scheme preserved`() {
        val type = testee.determineType("mailto:foo@example.com") as Email
        assertEquals("mailto:foo@example.com", type.emailAddress)
    }

    @Test
    fun `determineType - url is sms - sms type detected`() {
        val expected = Sms::class
        val actual = testee.determineType("sms:123-555-13245")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is sms to - sms type detected`() {
        val expected = Sms::class
        val actual = testee.determineType("smsto:123-555-13245")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is sms - scheme removed`() {
        val type = testee.determineType("sms:123-555-12323") as Sms
        assertEquals("123-555-12323", type.telephoneNumber)
    }

    @Test
    fun `determineType - url is sms to - scheme removed`() {
        val type = testee.determineType("smsto:123-555-12323") as Sms
        assertEquals("123-555-12323", type.telephoneNumber)
    }

    @Test
    fun `determineType - url is custom uri scheme - non http app link detected`() {
        val type = testee.determineType("myapp:foo bar") as NonHttpAppLink
        assertEquals("myapp:foo bar", type.uriString)
    }

    @Test
    fun `determineType - url not privacy pro - query type detected`() {
        whenever(subscriptions.shouldLaunchPrivacyProForUrl(any())).thenReturn(false)
        val result = testee.determineType("duckduckgo.com")
        assertTrue(result is SearchQuery)
    }

    @Test
    fun `determineType - url is privacy pro - should launch privacy pro link`() {
        whenever(subscriptions.shouldLaunchPrivacyProForUrl(any())).thenReturn(true)
        val result = testee.determineType("duckduckgo.com")
        assertTrue(result is ShouldLaunchPrivacyProLink)
    }

    @Test
    fun `determineType - url is parameterized query - search query detected`() {
        val type = testee.determineType("foo site:duckduckgo.com") as SearchQuery
        assertEquals("foo site:duckduckgo.com", type.query)
    }

    @Test
    fun `determineType - url is javascript scheme - web search type detected`() {
        val expected = SearchQuery::class
        val actual = testee.determineType("javascript:alert(0)")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is javascript scheme - full query retained`() {
        val type = testee.determineType("javascript:alert(0)") as SearchQuery
        assertEquals("javascript:alert(0)", type.query)
    }

    @Test
    fun `determineType - about scheme - web search type detected`() {
        val expected = SearchQuery::class
        val actual = testee.determineType("about:blank")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is about scheme - full query retained`() {
        val type = testee.determineType("about:blank") as SearchQuery
        assertEquals("about:blank", type.query)
    }

    @Test
    fun `determineType - url is file scheme - web search type detected`() {
        val expected = SearchQuery::class
        val actual = testee.determineType("file:///sdcard/")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is file scheme - full query retained`() {
        val type = testee.determineType("file:///sdcard/") as SearchQuery
        assertEquals("file:///sdcard/", type.query)
    }

    @Test
    fun `determineType - url is site scheme - web search type detected`() {
        val expected = SearchQuery::class
        val actual = testee.determineType("site:example.com")
        assertEquals(expected, actual::class)
    }

    @Test
    fun `determineType - url is site scheme - full query retained`() {
        val type = testee.determineType("site:example.com") as SearchQuery
        assertEquals("site:example.com", type.query)
    }

    @Test
    fun `determineType - url is blob scheme - full query retained`() {
        val type = testee.determineType("blob:example.com") as SearchQuery
        assertEquals("blob:example.com", type.query)
    }

    @Test
    fun `determineType - sms content longer than max allowed - truncated to max`() {
        val longSms = randomString(SMS_MAX_LENGTH + 1)
        val type = testee.determineType("sms:$longSms") as Sms
        assertEquals(longSms.substring(0, SMS_MAX_LENGTH), type.telephoneNumber)
    }

    @Test
    fun `determineType - sms to content longer than max allowed - truncated`() {
        val longSms = randomString(SMS_MAX_LENGTH + 1)
        val type = testee.determineType("smsto:$longSms") as Sms
        assertEquals(longSms.substring(0, SMS_MAX_LENGTH), type.telephoneNumber)
    }

    @Test
    fun `determineType - email content longer than max allowed - truncated to max`() {
        val longEmail = "mailto:${randomString(EMAIL_MAX_LENGTH + 1)}"
        val type = testee.determineType(longEmail) as Email
        assertEquals(longEmail.substring(0, EMAIL_MAX_LENGTH), type.emailAddress)
    }

    @Test
    fun `determineType - telephone content longer than max allowed - truncated to max`() {
        val longTelephone = randomString(PHONE_MAX_LENGTH + 1)
        val type = testee.determineType("tel:$longTelephone") as Telephone
        assertEquals(longTelephone.substring(0, PHONE_MAX_LENGTH), type.telephoneNumber)
    }

    @Test
    fun `determineType - telephone prompt content longer than max allowed - truncated to max`() {
        val longTelephone = randomString(PHONE_MAX_LENGTH + 1)
        val type = testee.determineType("telprompt:$longTelephone") as Telephone
        assertEquals(longTelephone.substring(0, PHONE_MAX_LENGTH), type.telephoneNumber)
    }

    @Test
    fun `determineType - url is amp link - extracted amp link detected`() {
        whenever(mockAmpLinks.extractCanonicalFromAmpLink(anyString()))
            .thenReturn(AmpLinkType.ExtractedAmpLink(extractedUrl = "https://www.example.com"))
        val expected = ExtractedAmpLink::class
        val actual = testee.determineType("https://www.google.com/amp/s/www.example.com")
        assertEquals(expected, actual::class)
        assertEquals("https://www.example.com", (actual as ExtractedAmpLink).extractedUrl)
    }

    @Test
    fun `determineType - url is cloaked amp link - cloaked amp link type detected`() {
        whenever(mockAmpLinks.extractCanonicalFromAmpLink(anyString()))
            .thenReturn(AmpLinkType.CloakedAmpLink(ampUrl = "https://www.example.com/amp"))
        val expected = CloakedAmpLink::class
        val actual = testee.determineType("https://www.example.com/amp")
        assertEquals(expected, actual::class)
        assertEquals("https://www.example.com/amp", (actual as CloakedAmpLink).ampUrl)
    }

    @Test
    fun `determineType - url is tracking parameter link - tracking parameter link type detected`() {
        whenever(mockTrackingParameters.cleanTrackingParameters(initiatingUrl = anyString(), url = anyString()))
            .thenReturn("https://www.example.com/query.html")
        val expected = TrackingParameterLink::class
        val actual =
            testee.determineType(initiatingUrl = "https://www.example.com", uri = "https://www.example.com/query.html?utm_example=something".toUri())
        assertEquals(expected, actual::class)
        assertEquals("https://www.example.com/query.html", (actual as TrackingParameterLink).cleanedUrl)
    }

    @Test
    fun `determineType - privacy pro link detected - should launch privacy pro link`() {
        whenever(subscriptions.shouldLaunchPrivacyProForUrl(any())).thenReturn(true)

        val actual =
            testee.determineType(initiatingUrl = "https://www.example.com", uri = "https://www.example.com".toUri())
        assertTrue(actual is ShouldLaunchPrivacyProLink)
    }

    private fun randomString(length: Int): String {
        val charList: List<Char> = ('a'..'z') + ('0'..'9')
        return List(length) { charList.random() }.joinToString("")
    }

    private fun buildAppResolveInfo(): ResolveInfo {
        val activity = ResolveInfo()
        activity.filter = IntentFilter()
        activity.filter.addDataAuthority("host.com", "123")
        activity.filter.addDataPath("/path", 0)
        activity.activityInfo = ActivityInfo()
        activity.activityInfo.packageName = EXAMPLE_APP_PACKAGE
        activity.activityInfo.name = EXAMPLE_APP_ACTIVITY_NAME
        return activity
    }

    private fun buildBrowserResolveInfo(): ResolveInfo {
        val activity = ResolveInfo()
        activity.filter = IntentFilter()
        activity.activityInfo = ActivityInfo()
        activity.activityInfo.packageName = EXAMPLE_BROWSER_PACKAGE
        activity.activityInfo.name = EXAMPLE_BROWSER_ACTIVITY_NAME
        return activity
    }

    companion object {
        const val EXAMPLE_APP_PACKAGE = "com.test.apppackage"
        const val EXAMPLE_APP_ACTIVITY_NAME = "com.test.AppActivity"
        const val EXAMPLE_BROWSER_PACKAGE = "com.test.browserpackage"
        const val EXAMPLE_BROWSER_ACTIVITY_NAME = "com.test.BrowserActivity"
    }
}
