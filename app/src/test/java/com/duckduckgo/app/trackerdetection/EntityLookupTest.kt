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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.trackerdetection.db.TdsDomainEntityDao
import com.duckduckgo.app.trackerdetection.db.TdsEntityDao
import com.duckduckgo.app.trackerdetection.model.TdsDomainEntity
import com.duckduckgo.app.trackerdetection.model.TdsEntity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class EntityLookupTest {

    private val mockEntityDao: TdsEntityDao = mock()
    private val mockDomainEntityDao: TdsDomainEntityDao = mock()
    private var testee = TdsEntityLookup(mockEntityDao, mockDomainEntityDao)

    @Test
    fun `entityForUrl - url contains one unmatched domain - no value returned`() {
        val url = "a.com"
        whenever(mockDomainEntityDao.get(url)).thenReturn(null)

        val entity = testee.entityForUrl("https://$url")
        verify(mockDomainEntityDao).get("a.com")
        assertNull(entity)
    }

    @Test
    fun `entityForUrl - url contains one matching domain - returns value`() {
        val url = "a.com"
        whenever(mockDomainEntityDao.get(url)).thenReturn(anEntityDomain())
        whenever(mockEntityDao.get(anEntityName())).thenReturn(anEntity())
        val entity = testee.entityForUrl("https://$url")
        assertNotNull(entity)
    }

    @Test
    fun `entityForUrl - url contains one matching domain - search for domain`() {
        val url = "a.com"
        testee.entityForUrl("https://$url")
        verify(mockDomainEntityDao).get("a.com")
        verify(mockDomainEntityDao, never()).get("com")
    }

    @Test
    fun `entityForUrl - url contains one unmatched subdomain and one matching domain - returns value`() {
        val url = "a.b.com"
        whenever(mockDomainEntityDao.get(url)).thenReturn(anEntityDomain())
        whenever(mockEntityDao.get(anEntityName())).thenReturn(anEntity())
        val entity = testee.entityForUrl("https://$url")
        assertNotNull(entity)
        verify(mockDomainEntityDao).get("a.b.com")
    }

    @Test
    fun `entityForUrl - url contains one multipart TLD - search for in DB`() {
        val url = "a.co.uk"
        testee.entityForUrl("https://$url")
        verify(mockDomainEntityDao).get("a.co.uk")
        verify(mockDomainEntityDao).get("co.uk")
    }

    @Test
    fun `entityForUrl - url contains many unmatched subdomains - all intermediate values searched for`() {
        val url = "a.b.c.com"
        whenever(mockDomainEntityDao.get("a.b.c.com")).thenReturn(null)
        whenever(mockDomainEntityDao.get("b.c.com")).thenReturn(null)
        whenever(mockDomainEntityDao.get("c.com")).thenReturn(null)

        val entity = testee.entityForUrl("https://$url")
        assertNull(entity)
        verify(mockDomainEntityDao).get("a.b.c.com")
        verify(mockDomainEntityDao).get("b.c.com")
        verify(mockDomainEntityDao).get("b.c.com")
        verify(mockDomainEntityDao).get("c.com")
    }

    @Test
    fun `entityForUrl - url contains many matching subdomains - stops when value found`() {
        val url = "a.b.c.com"
        whenever(mockDomainEntityDao.get("a.b.c.com")).thenReturn(null)
        whenever(mockDomainEntityDao.get("b.c.com")).thenReturn(anEntityDomain())
        whenever(mockDomainEntityDao.get("c.com")).thenReturn(null)
        whenever(mockEntityDao.get(anEntityName())).thenReturn(anEntity())

        val entity = testee.entityForUrl("https://$url")
        assertNotNull(entity)

        verify(mockDomainEntityDao).get("a.b.c.com")
        verify(mockDomainEntityDao).get("b.c.com")
        verify(mockDomainEntityDao, never()).get("c.com")
    }

    private fun anEntityName() = "Entity Name"
    private fun anEntityDomain() = TdsDomainEntity("", anEntityName())
    private fun anEntity() = TdsEntity(anEntityName(), "", 0.0)
}
