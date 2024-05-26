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

package com.duckduckgo.remote.messaging.impl.matchers

import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.appbuildconfig.api.BuildFlavor
import com.duckduckgo.appbuildconfig.api.BuildFlavor.INTERNAL
import com.duckduckgo.browser.api.AppProperties
import com.duckduckgo.remote.messaging.impl.models.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AndroidAppAttributeMatcherTest {

    private val appProperties: AppProperties = mock()
    private val appBuildConfig: AppBuildConfig = mock()

    private val testee = AndroidAppAttributeMatcher(appProperties, appBuildConfig)

    @Test
    fun `evaluate - flavor matches - returns match`() = runTest {
        givenDeviceProperties(flavor = INTERNAL)

        val result = testee.evaluate(
            Flavor(value = listOf("internal")),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - flavor does not match - return fail`() = runTest {
        givenDeviceProperties(flavor = INTERNAL)

        val result = testee.evaluate(
            Flavor(value = listOf("play")),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - appId matches - returns match`() = runTest {
        givenDeviceProperties(appId = "com.duckduckgo.mobile.android")

        val result = testee.evaluate(
            AppId(value = "com.duckduckgo.mobile.android"),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - appId does not match - return fail`() = runTest {
        givenDeviceProperties(appId = "com.duckduckgo.mobile.android")

        val result = testee.evaluate(
            AppId(value = "com.duckduckgo.mobile.android.debug"),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - app version equal or lower than max - returns match`() = runTest {
        givenDeviceProperties(appVersion = "5.100.0")

        val result = testee.evaluate(
            AppVersion(max = "5.100.0"),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - app version greater than max - return fail`() = runTest {
        givenDeviceProperties(appVersion = "5.100.0")

        val result = testee.evaluate(
            AppVersion(max = "5.99.0"),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - app version equal or greater than min - return match`() = runTest {
        givenDeviceProperties(appVersion = "5.100.0")

        val result = testee.evaluate(
            AppVersion(min = "5.100.0"),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - app version lower than min - return fail`() = runTest {
        givenDeviceProperties(appVersion = "5.99.0")

        val result = testee.evaluate(
            AppVersion(min = "5.100.0"),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - app version in range - returns match`() = runTest {
        givenDeviceProperties(appVersion = "5.150.0")

        val result = testee.evaluate(
            AppVersion(min = "5.99.0", max = "5.200.0"),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - app version not in range - return match`() = runTest {
        givenDeviceProperties(appVersion = "5.000.0")

        val result = testee.evaluate(
            AppVersion(min = "5.100.0", max = "5.200.0"),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - app version same as device - return match`() = runTest {
        givenDeviceProperties(appVersion = "5.100.0")

        val result = testee.evaluate(
            AppVersion(value = "5.100.0"),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - app version different - return fail`() = runTest {
        givenDeviceProperties(appVersion = "5.99.0")

        val result = testee.evaluate(
            AppVersion(value = "5.100.0"),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - atb matches - returns match`() = runTest {
        givenDeviceProperties(atb = "v105-2")

        val result = testee.evaluate(
            Atb(value = "v105-2"),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - atb does not match - return fail`() = runTest {
        givenDeviceProperties(atb = "v105-2")

        val result = testee.evaluate(
            Atb(value = "v105-0"),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - app atb matches - returns match`() = runTest {
        givenDeviceProperties(appAtb = "v105-2")

        val result = testee.evaluate(
            AppAtb(value = "v105-2"),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - app atb does not match - return fail`() = runTest {
        givenDeviceProperties(appAtb = "v105-2")

        val result = testee.evaluate(
            AppAtb(value = "v105-0"),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - search atb matches - returns match`() = runTest {
        givenDeviceProperties(searchAtb = "v105-2")

        val result = testee.evaluate(
            SearchAtb(value = "v105-2"),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - search atb does not match - return fail`() = runTest {
        givenDeviceProperties(searchAtb = "v105-2")

        val result = testee.evaluate(
            SearchAtb(value = "v105-0"),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - exp variant matches - returns match`() = runTest {
        givenDeviceProperties(expVariant = "zo")

        val result = testee.evaluate(
            ExpVariant(value = "zo"),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - exp variant does not match - return fail`() = runTest {
        givenDeviceProperties(expVariant = "zo")

        val result = testee.evaluate(
            ExpVariant(value = "zz"),
        )

        assertEquals(false, result)
    }

    @Test
    fun `evaluate - installed GPlay matches - returns match`() = runTest {
        givenDeviceProperties(installedGPlay = true)

        val result = testee.evaluate(
            InstalledGPlay(value = true),
        )

        assertEquals(true, result)
    }

    @Test
    fun `evaluate - installed G Play does not match - return fail`() = runTest {
        givenDeviceProperties(installedGPlay = false)

        val result = testee.evaluate(
            InstalledGPlay(value = true),
        )

        assertEquals(false, result)
    }

    private fun givenDeviceProperties(
        flavor: BuildFlavor = BuildFlavor.PLAY,
        appId: String = "com.duckduckgo.mobile.android.debug",
        appVersion: String = "5.106.0",
        atb: String = "v105-2",
        appAtb: String = "v105-2",
        searchAtb: String = "v105-2",
        expVariant: String = "zo",
        installedGPlay: Boolean = true,
    ) {
        whenever(appBuildConfig.flavor).thenReturn(flavor)
        whenever(appBuildConfig.applicationId).thenReturn(appId)
        whenever(appBuildConfig.versionName).thenReturn(appVersion)
        whenever(appProperties.atb()).thenReturn(atb)
        whenever(appProperties.appAtb()).thenReturn(appAtb)
        whenever(appProperties.searchAtb()).thenReturn(searchAtb)
        whenever(appProperties.expVariant()).thenReturn(expVariant)
        whenever(appProperties.installedGPlay()).thenReturn(installedGPlay)
    }
}
