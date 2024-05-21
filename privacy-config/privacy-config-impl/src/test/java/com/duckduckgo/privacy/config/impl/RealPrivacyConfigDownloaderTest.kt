/*
 * Copyright (c) 2021 DuckDuckGo
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

package com.duckduckgo.privacy.config.impl

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.feature.toggles.api.FeatureExceptions.FeatureException
import com.duckduckgo.privacy.config.impl.PrivacyConfigDownloader.ConfigDownloadResult.Error
import com.duckduckgo.privacy.config.impl.PrivacyConfigDownloader.ConfigDownloadResult.Success
import com.duckduckgo.privacy.config.impl.RealPrivacyConfigPersisterTest.FakeFakePrivacyConfigCallbackPluginPoint
import com.duckduckgo.privacy.config.impl.RealPrivacyConfigPersisterTest.FakePrivacyConfigCallbackPlugin
import com.duckduckgo.privacy.config.impl.models.JsonPrivacyConfig
import com.duckduckgo.privacy.config.impl.network.PrivacyConfigService
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class RealPrivacyConfigDownloaderTest {

    @get:Rule var coroutineRule = CoroutineTestRule()

    lateinit var testee: RealPrivacyConfigDownloader

    private val mockPrivacyConfigPersister: PrivacyConfigPersister = mock()
    private val pluginPoint = FakeFakePrivacyConfigCallbackPluginPoint(listOf(FakePrivacyConfigCallbackPlugin()))

    @Before
    fun before() {
        testee = RealPrivacyConfigDownloader(TestPrivacyConfigService(), mockPrivacyConfigPersister, pluginPoint)
    }

    @Test
    fun `download - not successful - return false`() =
        runTest {
            testee =
                RealPrivacyConfigDownloader(
                    TestFailingPrivacyConfigService(),
                    mockPrivacyConfigPersister,
                    pluginPoint,
                )
            assertTrue(testee.download() is Error)
        }

    @Test
    fun `download - successful - true`() =
        runTest { assertTrue(testee.download() is Success) }

    @Test
    fun `download - privacy config persisted`() =
        runTest {
            testee.download()

            verify(mockPrivacyConfigPersister).persistPrivacyConfig(any(), any())
        }

    class TestFailingPrivacyConfigService : PrivacyConfigService {
        override suspend fun privacyConfig(): Response<JsonPrivacyConfig> {
            throw Exception()
        }
    }

    class TestPrivacyConfigService : PrivacyConfigService {
        override suspend fun privacyConfig(): Response<JsonPrivacyConfig> {
            return Response.success(
                JsonPrivacyConfig(
                    version = 1,
                    readme = "readme",
                    features = mapOf(FEATURE_NAME to JSONObject(FEATURE_JSON)),
                    unprotectedTemporary = unprotectedTemporaryList,
                    experimentalVariants = VARIANT_MANAGER_JSON,
                ),
            )
        }
    }

    companion object {
        private const val FEATURE_NAME = "test"
        private const val FEATURE_JSON = "{\"state\": \"enabled\"}"
        val unprotectedTemporaryList = listOf(FeatureException("example.com", "reason"))
        private val VARIANT_MANAGER_JSON = null
    }
}
