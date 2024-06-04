/*
 * Copyright (c) 2024 DuckDuckGo
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

package com.duckduckgo.app.statistics.api

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LifecycleOwner
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.duckduckgo.app.statistics.api.PixelSender.SendPixelResult.PIXEL_IGNORED
import com.duckduckgo.app.statistics.api.PixelSender.SendPixelResult.PIXEL_SENT
import com.duckduckgo.app.statistics.api.RxPixelSenderTest.TestPixels.TEST
import com.duckduckgo.app.statistics.config.StatisticsLibraryConfig
import com.duckduckgo.app.statistics.model.Atb
import com.duckduckgo.app.statistics.model.PixelEntity
import com.duckduckgo.app.statistics.model.QueryParamsTypeConverter
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.app.statistics.pixels.Pixel.PixelType.COUNT
import com.duckduckgo.app.statistics.pixels.Pixel.PixelType.DAILY
import com.duckduckgo.app.statistics.pixels.Pixel.PixelType.UNIQUE
import com.duckduckgo.app.statistics.store.PendingPixelDao
import com.duckduckgo.app.statistics.store.PixelFiredRepository
import com.duckduckgo.app.statistics.store.StatisticsDataStore
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.common.test.InstantSchedulersRule
import com.duckduckgo.common.utils.device.DeviceInfo
import com.duckduckgo.experiments.api.VariantManager
import io.reactivex.Completable
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class RxPixelSenderTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val schedulers = InstantSchedulersRule()

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    @Mock
    val api: PixelService = mock()

    @Mock
    val mockStatisticsDataStore: StatisticsDataStore = mock()

    @Mock
    val mockVariantManager: VariantManager = mock()

    @Mock
    val mockDeviceInfo: DeviceInfo = mock()

    private lateinit var db: TestAppDatabase
    private lateinit var pendingPixelDao: PendingPixelDao
    private lateinit var testee: RxPixelSender
    private val mockLifecycleOwner: LifecycleOwner = mock()
    private val pixelFiredRepository = FakePixelFiredRepository()

    @Before
    fun before() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, TestAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        pendingPixelDao = db.pixelDao()

        testee = RxPixelSender(
            api,
            pendingPixelDao,
            mockStatisticsDataStore,
            mockVariantManager,
            mockDeviceInfo,
            object : StatisticsLibraryConfig {
                override fun shouldFirePixelsAsDev() = true
            },
            pixelFiredRepository,
        )
    }

    @After
    fun after() {
        db.close()
    }

    @Test
    fun `sendPixel - pixel fired - correct atb and variant`() {
        givenPixelApiSucceeds()
        givenAtbVariant(Atb("atb"))
        givenVariant("variant")
        givenFormFactor(DeviceInfo.FormFactor.PHONE)

        testee.sendPixel(TEST.pixelName, emptyMap(), emptyMap(), COUNT)
            .test().assertValue(PIXEL_SENT)

        verify(api).fire(eq("test"), eq("phone"), eq("atbvariant"), any(), any(), any())
    }

    @Test
    fun `sendPixel - pixel fired tablet form factor - pixel service called with tablet parameter`() {
        givenApiSendPixelSucceeds()
        givenFormFactor(DeviceInfo.FormFactor.TABLET)

        testee.sendPixel(TEST.pixelName, emptyMap(), emptyMap(), COUNT)
            .test().assertValue(PIXEL_SENT)

        verify(api).fire(eq("test"), eq("tablet"), eq(""), any(), any(), any())
    }

    @Test
    fun `sendPixel - pixel fired with no atb - pixel service called with correct pixel name and no atb`() {
        givenApiSendPixelSucceeds()
        givenFormFactor(DeviceInfo.FormFactor.PHONE)

        testee.sendPixel(TEST.pixelName, emptyMap(), emptyMap(), COUNT)
            .test().assertValue(PIXEL_SENT)

        verify(api).fire(eq("test"), eq("phone"), eq(""), any(), any(), any())
    }

    @Test
    fun `sendPixel - pixel fired with default and additional parameters`() {
        givenPixelApiSucceeds()
        givenAtbVariant(Atb("atb"))
        givenVariant("variant")
        givenFormFactor(DeviceInfo.FormFactor.PHONE)
        givenAppVersion("1.0.0")

        val params = mapOf("param1" to "value1", "param2" to "value2")
        val expectedParams = mapOf("param1" to "value1", "param2" to "value2", "appVersion" to "1.0.0")
        testee.sendPixel(TEST.pixelName, params, emptyMap(), COUNT)
            .test().assertValue(PIXEL_SENT)

        verify(api).fire("test", "phone", "atbvariant", expectedParams, emptyMap())
    }

    @Test
    fun `sendPixel - pixel fired without additional parameters - pixel service called with default parameters`() {
        givenPixelApiSucceeds()
        givenAtbVariant(Atb("atb"))
        givenVariant("variant")
        givenFormFactor(DeviceInfo.FormFactor.PHONE)
        givenAppVersion("1.0.0")

        testee.sendPixel(TEST.pixelName, emptyMap(), emptyMap(), COUNT)
            .test().assertValue(PIXEL_SENT)

        val expectedParams = mapOf("appVersion" to "1.0.0")
        verify(api).fire("test", "phone", "atbvariant", expectedParams, emptyMap())
    }

    @Test
    fun `enqueuePixel - with additional parameters - enqueued with parameters`() {
        givenAtbVariant(Atb("atb"))
        givenVariant("variant")
        givenFormFactor(DeviceInfo.FormFactor.PHONE)
        givenAppVersion("1.0.0")
        val params = mapOf("param1" to "value1", "param2" to "value2")

        testee.enqueuePixel(TEST.pixelName, params, emptyMap()).test()

        val testObserver = pendingPixelDao.pixels().test()
        val pixels = testObserver.assertNoErrors().values().last()

        assertEquals(1, pixels.size)
        assertPixelEntity(
            PixelEntity(
                pixelName = "test",
                atb = "atbvariant",
                additionalQueryParams = params + mapOf("appVersion" to "1.0.0"),
                encodedQueryParams = emptyMap(),
            ),
            pixels.first(),
        )
    }

    @Test
    fun `enqueuePixel - pixel enqueued with default parameters`() {
        givenAtbVariant(Atb("atb"))
        givenVariant("variant")
        givenFormFactor(DeviceInfo.FormFactor.PHONE)
        givenAppVersion("1.0.0")

        testee.enqueuePixel(TEST.pixelName, emptyMap(), emptyMap()).test()

        val pixels = pendingPixelDao.pixels().test().assertNoErrors().values().last()
        assertEquals(1, pixels.size)
        assertPixelEntity(
            PixelEntity(
                pixelName = "test",
                atb = "atbvariant",
                additionalQueryParams = mapOf("appVersion" to "1.0.0"),
                encodedQueryParams = emptyMap(),
            ),
            pixels.first(),
        )
    }

    @Test
    fun `onStart - app foregrounded - pixel sent`() {
        givenPixelApiSucceeds()
        val pixelEntity = PixelEntity(
            pixelName = "test",
            atb = "atbvariant",
            additionalQueryParams = mapOf("appVersion" to "1.0.0"),
            encodedQueryParams = emptyMap(),
        )
        pendingPixelDao.insert(pixelEntity)
        givenFormFactor(DeviceInfo.FormFactor.PHONE)

        testee.onStart(mockLifecycleOwner)

        verify(api).fire(
            pixelEntity.pixelName,
            "phone",
            pixelEntity.atb,
            pixelEntity.additionalQueryParams,
            pixelEntity.encodedQueryParams,
        )
    }

    @Test
    fun `onStart - app foregrounded and pixel sent - pixel removed`() {
        givenPixelApiSucceeds()
        val pixelEntity = PixelEntity(
            pixelName = "test",
            atb = "atbvariant",
            additionalQueryParams = mapOf("appVersion" to "1.0.0"),
            encodedQueryParams = emptyMap(),
        )
        pendingPixelDao.insert(pixelEntity)
        givenFormFactor(DeviceInfo.FormFactor.PHONE)

        testee.onStart(mockLifecycleOwner)

        val pixels = pendingPixelDao.pixels().test().assertNoErrors().values().last()
        assertTrue(pixels.isEmpty())
    }

    @Test
    fun `onStart - app foregrounded and send pixel fails - pixel not removed`() {
        givenPixelApiFails()
        val pixelEntity = PixelEntity(
            pixelName = "test",
            atb = "atbvariant",
            additionalQueryParams = mapOf("appVersion" to "1.0.0"),
            encodedQueryParams = emptyMap(),
        )
        pendingPixelDao.insert(pixelEntity)
        givenFormFactor(DeviceInfo.FormFactor.PHONE)

        testee.onStart(mockLifecycleOwner)

        val testObserver = pendingPixelDao.pixels().test()
        val pixels = testObserver.assertNoErrors().values().last()
        assertTrue(pixels.isNotEmpty())
    }

    @Test
    fun `onStart - app foregrounded with multiple pixels enqueued - send all pixels`() {
        givenPixelApiSucceeds()
        val pixelEntity = PixelEntity(
            pixelName = "test",
            atb = "atbvariant",
            additionalQueryParams = mapOf("appVersion" to "1.0.0"),
            encodedQueryParams = emptyMap(),
        )
        pendingPixelDao.insert(pixelEntity, times = 5)
        givenFormFactor(DeviceInfo.FormFactor.PHONE)

        testee.onStart(mockLifecycleOwner)

        verify(api, times(5)).fire(
            pixelEntity.pixelName,
            "phone",
            pixelEntity.atb,
            pixelEntity.additionalQueryParams,
            pixelEntity.encodedQueryParams,
        )
    }

    @Test
    fun `sendPixel - daily pixel fired - pixel name stored`() = runTest {
        givenPixelApiSucceeds()
        givenFormFactor(DeviceInfo.FormFactor.PHONE)

        testee.sendPixel(TEST.pixelName, emptyMap(), emptyMap(), DAILY)
            .test().assertValue(PIXEL_SENT)

        verify(api).fire(eq(TEST.pixelName), any(), any(), any(), any(), any())
        assertTrue(TEST.pixelName in pixelFiredRepository.dailyPixelsFiredToday)
    }

    @Test
    fun `sendPixel - daily pixel fire fails - pixel name not stored`() = runTest {
        givenPixelApiFails()
        givenFormFactor(DeviceInfo.FormFactor.PHONE)

        testee.sendPixel(TEST.pixelName, emptyMap(), emptyMap(), DAILY)
            .test().assertError(RuntimeException::class.java)

        verify(api).fire(eq(TEST.pixelName), any(), any(), any(), any(), any())
        assertFalse(TEST.pixelName in pixelFiredRepository.dailyPixelsFiredToday)
    }

    @Test
    fun `sendPixel - daily pixel already fired today - not fired again`() = runTest {
        pixelFiredRepository.dailyPixelsFiredToday += TEST.pixelName

        testee.sendPixel(TEST.pixelName, emptyMap(), emptyMap(), DAILY)
            .test().assertValue(PIXEL_IGNORED)

        verifyNoInteractions(api)
        assertTrue(TEST.pixelName in pixelFiredRepository.dailyPixelsFiredToday)
    }

    @Test
    fun `sendPixel - unique pixel fired - pixel name stored`() = runTest {
        givenPixelApiSucceeds()
        givenFormFactor(DeviceInfo.FormFactor.PHONE)

        testee.sendPixel(TEST.pixelName, emptyMap(), emptyMap(), UNIQUE)
            .test().assertValue(PIXEL_SENT)

        verify(api).fire(eq(TEST.pixelName), any(), any(), any(), any(), any())
        assertTrue(TEST.pixelName in pixelFiredRepository.uniquePixelsFired)
    }

    @Test
    fun `sendPixel - unique pixel fire fails - pixel name not stored`() = runTest {
        givenPixelApiFails()
        givenFormFactor(DeviceInfo.FormFactor.PHONE)

        testee.sendPixel(TEST.pixelName, emptyMap(), emptyMap(), UNIQUE)
            .test().assertError(RuntimeException::class.java)

        verify(api).fire(eq(TEST.pixelName), any(), any(), any(), any(), any())
        assertFalse(TEST.pixelName in pixelFiredRepository.uniquePixelsFired)
    }

    @Test
    fun `sendPixel - unique pixel already fired - not fired again`() = runTest {
        pixelFiredRepository.uniquePixelsFired += TEST.pixelName

        testee.sendPixel(TEST.pixelName, emptyMap(), emptyMap(), UNIQUE)
            .test().assertValue(PIXEL_IGNORED)

        verifyNoInteractions(api)
        assertTrue(TEST.pixelName in pixelFiredRepository.uniquePixelsFired)
    }

    private fun assertPixelEntity(
        expectedEntity: PixelEntity,
        pixelEntity: PixelEntity,
    ) {
        assertEquals(expectedEntity.pixelName, pixelEntity.pixelName)
        assertEquals(expectedEntity.atb, pixelEntity.atb)
        assertEquals(expectedEntity.additionalQueryParams, pixelEntity.additionalQueryParams)
        assertEquals(expectedEntity.encodedQueryParams, pixelEntity.encodedQueryParams)
    }

    @Suppress("SameParameterValue")
    private fun givenAppVersion(appVersion: String) {
        whenever(mockDeviceInfo.appVersion).thenReturn(appVersion)
    }

    private fun givenApiSendPixelSucceeds() {
        whenever(api.fire(any(), any(), any(), any(), any(), any())).thenReturn(Completable.complete())
    }

    private fun givenVariant(variantKey: String) {
        whenever(mockVariantManager.getVariantKey()).thenReturn(variantKey)
    }

    private fun givenAtbVariant(atb: Atb) {
        whenever(mockStatisticsDataStore.atb).thenReturn(atb)
    }

    private fun givenFormFactor(formFactor: DeviceInfo.FormFactor) {
        whenever(mockDeviceInfo.formFactor()).thenReturn(formFactor)
    }

    private fun givenPixelApiSucceeds() {
        whenever(api.fire(any(), any(), any(), anyOrNull(), any(), any())).thenReturn(Completable.complete())
    }

    private fun givenPixelApiFails() {
        whenever(api.fire(any(), any(), any(), anyOrNull(), any(), any())).thenReturn(Completable.error(TimeoutException()))
    }

    private fun PendingPixelDao.insert(
        pixel: PixelEntity,
        times: Int,
    ) {
        for (x in 1..times) {
            this.insert(pixel)
        }
    }

    enum class TestPixels(override val pixelName: String, val enqueue: Boolean = false) : Pixel.PixelName {
        TEST("test"),
    }
}

@Database(
    exportSchema = false,
    version = 1,
    entities = [PixelEntity::class],
)
@TypeConverters(
    QueryParamsTypeConverter::class,
)
private abstract class TestAppDatabase : RoomDatabase() {
    abstract fun pixelDao(): PendingPixelDao
}

private class FakePixelFiredRepository : PixelFiredRepository {

    val dailyPixelsFiredToday = mutableSetOf<String>()
    val uniquePixelsFired = mutableSetOf<String>()

    override suspend fun storeDailyPixelFiredToday(name: String) {
        dailyPixelsFiredToday += name
    }

    override suspend fun hasDailyPixelFiredToday(name: String): Boolean =
        name in dailyPixelsFiredToday

    override suspend fun storeUniquePixelFired(name: String) {
        uniquePixelsFired += name
    }

    override suspend fun hasUniquePixelFired(name: String): Boolean =
        name in uniquePixelsFired
}
