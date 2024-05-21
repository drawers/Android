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

package com.duckduckgo.voice.store

import com.duckduckgo.voice.api.VoiceSearchStatusListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealVoiceSearchRepositoryTest {

    private lateinit var testee: RealVoiceSearchRepository
    private var dataSource = FakeVoiceSearchDataStore()
    private var voiceSearchStatusListener = FakeVoiceSearchStatusListener()

    @Before
    fun setUp() {
        testee = RealVoiceSearchRepository(dataSource, voiceSearchStatusListener)
    }

    @Test
    fun `acceptRationaleDialog - has accepted rationale dialog - true`() {
        assertFalse(testee.getHasAcceptedRationaleDialog())

        testee.acceptRationaleDialog()

        assertTrue(testee.getHasAcceptedRationaleDialog())
    }

    @Test
    fun `get - permission declined forever - true`() {
        assertFalse(testee.getHasPermissionDeclinedForever())

        testee.declinePermissionForever()

        assertTrue(testee.getHasPermissionDeclinedForever())
    }

    @Test
    fun `saveLoggedAvailability - availability logged - has logged availability`() {
        assertFalse(testee.getHasLoggedAvailability())

        testee.saveLoggedAvailability()

        assertTrue(testee.getHasLoggedAvailability())
    }

    @Test
    fun `setVoiceSearchEnabled - is voice search enabled should be true`() {
        assertFalse(testee.isVoiceSearchUserEnabled(false))

        testee.setVoiceSearchUserEnabled(true)

        assertTrue(testee.isVoiceSearchUserEnabled(false))
    }

    @Test
    fun `setVoiceSearchEnabled - listener should be called`() {
        testee.setVoiceSearchUserEnabled(true)

        assertTrue(voiceSearchStatusListener.statusChanged)
    }

    @Test
    fun `dismissVoiceSearch - count voice search dismissed value should increase`() {
        assertEquals(0, testee.countVoiceSearchDismissed())

        testee.dismissVoiceSearch()

        assertEquals(1, testee.countVoiceSearchDismissed())
    }
}

class FakeVoiceSearchDataStore : VoiceSearchDataStore {
    override var permissionDeclinedForever: Boolean = false
    override var userAcceptedRationaleDialog: Boolean = false
    override var availabilityLogged: Boolean = false
    override var countVoiceSearchDismissed: Int = 0

    private var _voiceSearchEnabled = false

    override fun isVoiceSearchEnabled(default: Boolean): Boolean {
        return _voiceSearchEnabled
    }

    override fun setVoiceSearchEnabled(value: Boolean) {
        _voiceSearchEnabled = value
    }
}

class FakeVoiceSearchStatusListener : VoiceSearchStatusListener {

    var statusChanged: Boolean = false
        private set
    override fun voiceSearchStatusChanged() {
        statusChanged = true
    }
}
