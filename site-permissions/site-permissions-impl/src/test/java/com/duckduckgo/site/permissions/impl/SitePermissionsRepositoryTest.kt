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

package com.duckduckgo.site.permissions.impl

import android.webkit.PermissionRequest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.site.permissions.impl.drmblock.DrmBlock
import com.duckduckgo.site.permissions.store.SitePermissionsPreferences
import com.duckduckgo.site.permissions.store.sitepermissions.SitePermissionAskSettingType
import com.duckduckgo.site.permissions.store.sitepermissions.SitePermissionsDao
import com.duckduckgo.site.permissions.store.sitepermissions.SitePermissionsEntity
import com.duckduckgo.site.permissions.store.sitepermissionsallowed.SitePermissionAllowedEntity
import com.duckduckgo.site.permissions.store.sitepermissionsallowed.SitePermissionsAllowedDao
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlin.math.abs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SitePermissionsRepositoryTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private val mockSitePermissionsDao: SitePermissionsDao = mock()
    private val mockSitePermissionsAllowedDao: SitePermissionsAllowedDao = mock()
    private val mockSitePermissionsPreferences: SitePermissionsPreferences = mock()
    private val mockDrmBlock: DrmBlock = mock()

    private val repository = SitePermissionsRepositoryImpl(
        mockSitePermissionsDao,
        mockSitePermissionsAllowedDao,
        mockSitePermissionsPreferences,
        coroutineRule.testScope,
        coroutineRule.testDispatcherProvider,
        mockDrmBlock,
    )

    private val url = "https://domain.com/whatever"
    private val domain = "domain.com"

    @Test
    fun `givenPermissionNotSupported - domain is not allowed to ask`() {
        setInitialSettings()
        val permission = PermissionRequest.RESOURCE_MIDI_SYSEX

        assertFalse(repository.isDomainAllowedToAsk(url, permission))
    }

    @Test
    fun `givenPermissionSupported - domain allowed to ask`() {
        setInitialSettings()
        val permission = PermissionRequest.RESOURCE_AUDIO_CAPTURE

        assertTrue(repository.isDomainAllowedToAsk(url, permission))
    }

    @Test
    fun `askForPermission - is disabled - domain not allowed to ask`() {
        setInitialSettings(cameraEnabled = false)
        val permission = PermissionRequest.RESOURCE_VIDEO_CAPTURE

        assertFalse(repository.isDomainAllowedToAsk(url, permission))
    }

    @Test
    fun `isDomainAllowedToAsk - ask for permission disabled but site permission setting is always allow`() {
        val testEntity = SitePermissionsEntity(domain, askMicSetting = SitePermissionAskSettingType.ALLOW_ALWAYS.name)
        setInitialSettings(micEnabled = false, sitePermissionEntity = testEntity)
        val permission = PermissionRequest.RESOURCE_AUDIO_CAPTURE

        assertTrue(repository.isDomainAllowedToAsk(url, permission))
    }

    @Test
    fun `isDomainAllowedToAsk - site permission setting is deny always`() {
        val testEntity = SitePermissionsEntity(domain, askCameraSetting = SitePermissionAskSettingType.DENY_ALWAYS.name)
        setInitialSettings(sitePermissionEntity = testEntity)
        val permission = PermissionRequest.RESOURCE_VIDEO_CAPTURE

        assertFalse(repository.isDomainAllowedToAsk(url, permission))
    }

    @Test
    fun `isDomainAllowedToAsk - no site permission setting and drm blocked - domain not allowed to ask`() {
        val permission = PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID

        whenever(mockDrmBlock.isDrmBlockedForUrl(url)).thenReturn(true)

        assertFalse(repository.isDomainAllowedToAsk(url, permission))
    }

    @Test
    fun `isDomainAllowedToAsk - site permission setting ask and drm blocked - allowed to ask`() {
        val testEntity = SitePermissionsEntity(domain, askDrmSetting = SitePermissionAskSettingType.ASK_EVERY_TIME.name)
        setInitialSettings(sitePermissionEntity = testEntity)
        val permission = PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID

        whenever(mockDrmBlock.isDrmBlockedForUrl(url)).thenReturn(true)

        assertTrue(repository.isDomainAllowedToAsk(url, permission))
    }

    @Test
    fun `isDomainGranted - site permissions granted within 24h - permission granted`() {
        setInitialSettings()
        val permission = PermissionRequest.RESOURCE_VIDEO_CAPTURE
        val tabId = "tabId"
        val sitePermissionAllowedEntity = SitePermissionAllowedEntity(domain, tabId, permission, setAllowedAtTime(12))
        whenever(mockSitePermissionsAllowedDao.getSitePermissionAllowed(domain, tabId, permission)).thenReturn(sitePermissionAllowedEntity)

        assertTrue(repository.isDomainGranted(url, tabId, permission))
    }

    @Test
    fun `getSitePermission - site permissions was more than 24 hours ago - return permission not granted`() {
        setInitialSettings()
        val permission = PermissionRequest.RESOURCE_VIDEO_CAPTURE
        val tabId = "tabId"
        val sitePermissionAllowedEntity = SitePermissionAllowedEntity(domain, tabId, permission, setAllowedAtTime(25))
        whenever(mockSitePermissionsAllowedDao.getSitePermissionAllowed(domain, tabId, permission)).thenReturn(sitePermissionAllowedEntity)

        assertFalse(repository.isDomainGranted(url, tabId, permission))
    }

    @Test
    fun `getSitePermission - site permissions setting allow always - permission granted`() {
        val testEntity = SitePermissionsEntity(domain, askCameraSetting = SitePermissionAskSettingType.ALLOW_ALWAYS.name)
        setInitialSettings(sitePermissionEntity = testEntity)
        val permission = PermissionRequest.RESOURCE_VIDEO_CAPTURE
        val tabId = "tabId"
        whenever(mockSitePermissionsAllowedDao.getSitePermissionAllowed(domain, tabId, permission)).thenReturn(null)

        assertTrue(repository.isDomainGranted(url, tabId, permission))
    }

    @Test
    fun `whenUserGrantsSitePermissionFirstTime - save entity`() = runTest {
        val testEntity = SitePermissionsEntity(domain)
        setInitialSettings()
        repository.sitePermissionGranted(url, "tabId", PermissionRequest.RESOURCE_VIDEO_CAPTURE)

        verify(mockSitePermissionsDao).insert(testEntity)
    }

    @Test
    fun `whenUserGrantsSitePermissionAlreadyInDb - skip save entity`() = runTest {
        val testEntity = SitePermissionsEntity(domain)
        setInitialSettings(sitePermissionEntity = testEntity)
        repository.sitePermissionGranted(url, "tabId", PermissionRequest.RESOURCE_VIDEO_CAPTURE)

        verify(mockSitePermissionsDao, never()).insert(testEntity)
    }

    @Test
    fun `whenUserGrantsSitePermission - save allowed entity`() = runTest {
        setInitialSettings()
        repository.sitePermissionGranted(url, "tabId", PermissionRequest.RESOURCE_VIDEO_CAPTURE)

        verify(mockSitePermissionsAllowedDao).insert(any())
    }

    @Test
    fun `sitePermissionsWebsitesFlow - get site permissions websites flow`() = runTest {
        repository.sitePermissionsWebsitesFlow()

        verify(mockSitePermissionsDao).getAllSitesPermissionsAsFlow()
    }

    @Test
    fun `sitePermissionsForAllWebsites - get site permissions for all websites`() = runTest {
        repository.sitePermissionsForAllWebsites()

        verify(mockSitePermissionsDao).getAllSitesPermissions()
    }

    @Test
    fun `sitePermissionsAllowedFlow - get site permissions allowed flow`() = runTest {
        repository.sitePermissionsAllowedFlow()

        verify(mockSitePermissionsAllowedDao).getAllSitesPermissionsAllowedAsFlow()
    }

    @Test
    fun `undoDeleteAll - site permissions back to allowed dao`() = runTest {
        val tabId = "tabId"
        val permission = PermissionRequest.RESOURCE_AUDIO_CAPTURE
        val testAllowedEntity = SitePermissionAllowedEntity(domain, tabId, permission, setAllowedAtTime(12))
        val allowedSites = listOf(testAllowedEntity)

        repository.undoDeleteAll(emptyList(), allowedSites)

        verify(mockSitePermissionsAllowedDao).insert(testAllowedEntity)
    }

    @Test
    fun `undoDeleteAll - site permissions dao inserted`() = runTest {
        val testEntity = SitePermissionsEntity(domain)
        val sitePermissions = listOf(testEntity)

        repository.undoDeleteAll(sitePermissions, emptyList())

        verify(mockSitePermissionsDao).insert(testEntity)
    }

    @Test
    fun `deleteAll - delete entities from databases`() = runTest {
        repository.deleteAll()

        verify(mockSitePermissionsDao).deleteAll()
        verify(mockSitePermissionsAllowedDao).deleteAll()
    }

    @Test
    fun `getSitePermissionsForWebsite - get site permissions by domain`() = runTest {
        repository.getSitePermissionsForWebsite(url)

        verify(mockSitePermissionsDao).getSitePermissionsByDomain(domain)
    }

    @Test
    fun `deletePermissionForSite - delete from dbs`() = runTest {
        val testEntity = SitePermissionsEntity(domain)
        whenever(mockSitePermissionsDao.getSitePermissionsByDomain(domain)).thenReturn(testEntity)
        repository.deletePermissionsForSite(url)

        verify(mockSitePermissionsDao).delete(testEntity)
        verify(mockSitePermissionsAllowedDao).deleteAllowedSitesForDomain(domain)
    }

    @Test
    fun `savePermission - insert entity in db`() = runTest {
        val testEntity = SitePermissionsEntity(domain)
        repository.savePermission(testEntity)

        verify(mockSitePermissionsDao).insert(testEntity)
    }

    private fun setInitialSettings(
        cameraEnabled: Boolean = true,
        micEnabled: Boolean = true,
        drmEnabled: Boolean = true,
        sitePermissionEntity: SitePermissionsEntity? = null,
    ) {
        whenever(mockSitePermissionsPreferences.askCameraEnabled).thenReturn(cameraEnabled)
        whenever(mockSitePermissionsPreferences.askMicEnabled).thenReturn(micEnabled)
        whenever(mockSitePermissionsPreferences.askDrmEnabled).thenReturn(drmEnabled)
        whenever(mockSitePermissionsDao.getSitePermissionsByDomain(domain)).thenReturn(sitePermissionEntity)
    }

    private fun setAllowedAtTime(hoursAgo: Int): Long {
        val now = System.currentTimeMillis()
        return abs(now - hoursAgo * 3600000)
    }
}
