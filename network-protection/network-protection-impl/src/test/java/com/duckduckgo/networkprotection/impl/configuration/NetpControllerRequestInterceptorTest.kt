package com.duckduckgo.networkprotection.impl.configuration

import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.appbuildconfig.api.BuildFlavor
import com.duckduckgo.appbuildconfig.api.BuildFlavor.INTERNAL
import com.duckduckgo.appbuildconfig.api.BuildFlavor.PLAY
import com.duckduckgo.common.test.api.FakeChain
import com.duckduckgo.subscriptions.api.Subscriptions
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class NetpControllerRequestInterceptorTest {

    private val appBuildConfig: AppBuildConfig = mock()
    private val subscriptions: Subscriptions = mock()
    private lateinit var interceptor: NetpControllerRequestInterceptor

    @Before
    fun setup() {
        runBlocking {
            // default values
            whenever(subscriptions.getAccessToken()).thenReturn(null)
            whenever(subscriptions.getEntitlementStatus()).thenReturn(flowOf(emptyList()))
        }

        interceptor = NetpControllerRequestInterceptor(
            appBuildConfig,
            subscriptions,
        )
    }

    @Test
    fun `intercept - not net p url - do nothing`() {
        val chain = FakeChain("https://this.is.not.the.url/servers")

        val headers = interceptor.intercept(chain).headers

        assertTrue(headers.size == 0)
    }

    @Test
    fun `interceptServersCall - add auth header`() {
        val chain = FakeChain("https://staging.netp.duckduckgo.com/servers")

        val headers = interceptor.intercept(chain).headers

        assertTrue(headers.size == 1)
        assertFalse(headers.map { it.first }.any { it.contains("NetP-Debug-Code") })
        assertTrue(headers.map { it.first }.any { it.contains("Authorization") })
    }

    @Test
    fun `interceptRegisterCall - add auth header`() {
        val chain = FakeChain("https://staging.netp.duckduckgo.com/register")

        val headers = interceptor.intercept(chain).headers

        assertTrue(headers.size == 1)
        assertFalse(headers.map { it.first }.any { it.contains("NetP-Debug-Code") })
        assertTrue(headers.map { it.first }.any { it.contains("Authorization") })
    }

    @Test
    fun `interceptServersCallInternalBuild - add auth and debug headers`() {
        whenever(appBuildConfig.flavor).thenReturn(BuildFlavor.INTERNAL)

        val chain = FakeChain("https://staging.netp.duckduckgo.com/servers")

        val headers = interceptor.intercept(chain).headers

        assertTrue(headers.size == 2)
        assertTrue(headers.map { it.first }.any { it.contains("NetP-Debug-Code") })
        assertTrue(headers.map { it.first }.any { it.contains("Authorization") })
    }

    @Test
    fun `interceptRegisterCallInternalBuild - add auth and debug headers`() {
        whenever(appBuildConfig.flavor).thenReturn(BuildFlavor.INTERNAL)

        val chain = FakeChain("https://staging.netp.duckduckgo.com/register")

        val headers = interceptor.intercept(chain).headers

        assertTrue(headers.size == 2)
        assertTrue(headers.map { it.first }.any { it.contains("NetP-Debug-Code") })
        assertTrue(headers.map { it.first }.any { it.contains("Authorization") })
    }

    @Test
    fun `interceptServersCallPlayBuild - add auth header`() {
        whenever(appBuildConfig.flavor).thenReturn(BuildFlavor.PLAY)

        val chain = FakeChain("https://staging.netp.duckduckgo.com/servers")

        val headers = interceptor.intercept(chain).headers

        assertTrue(headers.size == 1)
        assertFalse(headers.map { it.first }.any { it.contains("NetP-Debug-Code") })
        assertTrue(headers.map { it.first }.any { it.contains("Authorization") })
    }

    @Test
    fun `interceptRegisterCallPlayBuild - add auth header`() {
        whenever(appBuildConfig.flavor).thenReturn(BuildFlavor.PLAY)

        val chain = FakeChain("https://staging.netp.duckduckgo.com/register")

        val headers = interceptor.intercept(chain).headers

        assertTrue(headers.size == 1)
        assertFalse(headers.map { it.first }.any { it.contains("NetP-Debug-Code") })
        assertTrue(headers.map { it.first }.any { it.contains("Authorization") })
    }

    // ----------------------------------------------------------------------------------------------------------------------------------
    // Here starts the tests for the subscriptions
    // ----------------------------------------------------------------------------------------------------------------------------------
    @Test
    fun `intercept - url is servers and flavor is play - only add token to header`() = runTest {
        val fakeChain = FakeChain(url = "https://controller.netp.duckduckgo.com/servers")
        whenever(appBuildConfig.flavor).thenReturn(PLAY)
        whenever(subscriptions.getAccessToken()).thenReturn("token123")

        interceptor.intercept(fakeChain).run {
            Assert.assertEquals("bearer ddg:token123", headers["Authorization"])
            assertFalse(headers.names().contains("NetP-Debug-Code"))
        }
    }

    @Test
    fun `intercept - url is locations and flavor is play - only add token to header`() = runTest {
        val fakeChain = FakeChain(url = "https://controller.netp.duckduckgo.com/locations")
        whenever(appBuildConfig.flavor).thenReturn(PLAY)
        whenever(subscriptions.getAccessToken()).thenReturn("token123")

        interceptor.intercept(fakeChain).run {
            Assert.assertEquals("bearer ddg:token123", headers["Authorization"])
            assertFalse(headers.names().contains("NetP-Debug-Code"))
        }
    }

    @Test
    fun `intercept - url is register and flavor is play - only add token to header`() = runTest {
        val fakeChain = FakeChain(url = "https://staging1.netp.duckduckgo.com/register")
        whenever(appBuildConfig.flavor).thenReturn(PLAY)
        whenever(subscriptions.getAccessToken()).thenReturn("token123")

        interceptor.intercept(fakeChain).run {
            Assert.assertEquals("bearer ddg:token123", headers["Authorization"])
            assertFalse(headers.names().contains("NetP-Debug-Code"))
        }
    }

    @Test
    fun `intercept - url not net p and flavor internal - do nothing with headers`() = runTest {
        val fakeChain = FakeChain(url = "https://improving.duckduckgo.com/t/m_netp_ev_enabled_android_phone?atb=v336-7&appVersion=5.131.0&test=1")

        interceptor.intercept(fakeChain).run {
            Assert.assertNull(headers["Authorization"])
            assertFalse(headers.names().contains("NetP-Debug-Code"))
        }
    }

    @Test
    fun `intercept - url is net p and flavor is internal - add token and debug code to header`() = runTest {
        val fakeChain = FakeChain(url = "https://controller.netp.duckduckgo.com/servers")
        whenever(appBuildConfig.flavor).thenReturn(INTERNAL)
        whenever(subscriptions.getAccessToken()).thenReturn("token123")

        interceptor.intercept(fakeChain).run {
            Assert.assertEquals("bearer ddg:token123", headers["Authorization"])
            assertTrue(headers.names().contains("NetP-Debug-Code"))
        }
    }
}
