/*
 * Copyright (c) 2023 DuckDuckGo
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

package com.duckduckgo.autofill.impl.ui.credential.management.viewing.duckaddress

import org.junit.Assert.*
import org.junit.Test

class RealDuckAddressIdentifierTest {
    private val testee = RealDuckAddressIdentifier()

    @Test
    fun `isPrivateDuckAddress - only suffix - not a private address`() {
        assertFalse(testee.isPrivateDuckAddress("@duck.com", MAIN_ADDRESS))
    }

    @Test
    fun `isPrivateDuckAddress - input matches main address - not a private address`() {
        assertFalse(testee.isPrivateDuckAddress(MAIN_ADDRESS, MAIN_ADDRESS))
    }

    @Test
    fun `isPrivateDuckAddress - input matches main address with suffix - not a private address`() {
        assertFalse(testee.isPrivateDuckAddress(MAIN_ADDRESS, "test@duck.com"))
    }

    @Test
    fun `isPrivateDuckAddress - input does not end with suffix - not a private address`() {
        assertFalse(testee.isPrivateDuckAddress("test@gmail.com", MAIN_ADDRESS))
    }

    @Test
    fun `isPrivateDuckAddress - main address missing suffix - not a private address`() {
        assertFalse(testee.isPrivateDuckAddress("test@duck.com", MAIN_ADDRESS))
    }

    @Test
    fun `isPrivateDuckAddress - different from main address - is a private address`() {
        assertTrue(testee.isPrivateDuckAddress("foo@duck.com", MAIN_ADDRESS))
    }

    companion object {
        private const val MAIN_ADDRESS = "test"
    }
}
