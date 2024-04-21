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

package com.duckduckgo.autofill.impl.jsbridge.request

import com.duckduckgo.common.test.FileUtilities
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AutofillJsonRequestParserTest {

    private val moshi = Moshi.Builder().build()
    private val testee = AutofillJsonRequestParser(moshi)

    @Test
    fun `parseStoreFormDataJson - username and password provided - both in response`() = runTest {
        val parsed = "storeFormData_usernameAndPasswordProvided".parseStoreFormDataJson()
        assertEquals("dax@duck.com", parsed.credentials!!.username)
        assertEquals("123456", parsed.credentials!!.password)
    }

    @Test
    fun `parseStoreFormDataJson - username and password missing - both are null`() = runTest {
        val parsed = "storeFormData_usernameAndPasswordMissing".parseStoreFormDataJson()
        assertNull(parsed.credentials!!.username)
        assertNull(parsed.credentials!!.password)
    }

    @Test
    fun `parseStoreFormDataJson - username and password both null - both are null in parsed object`() = runTest {
        val parsed = "storeFormData_usernameAndPasswordNull".parseStoreFormDataJson()
        assertNull(parsed.credentials!!.username)
        assertNull(parsed.credentials!!.password)
    }

    @Test
    fun `parseStoreFormDataJson - additional unknown properties in request - still parses`() = runTest {
        val parsed = "storeFormData_additionalUnknownPropertiesIncluded".parseStoreFormDataJson()
        assertEquals("dax@duck.com", parsed.credentials!!.username)
        assertEquals("123456", parsed.credentials!!.password)
    }

    @Test
    fun `parseStoreFormDataJson - username missing - password populated`() = runTest {
        val parsed = "storeFormData_usernameMissing".parseStoreFormDataJson()
        assertNull(parsed.credentials!!.username)
        assertEquals("123456", parsed.credentials!!.password)
    }

    @Test
    fun `passwordMissing - username populated`() = runTest {
        val parsed = "storeFormData_passwordMissing".parseStoreFormDataJson()
        assertEquals("dax@duck.com", parsed.credentials!!.username)
        assertNull(parsed.credentials!!.password)
    }

    @Test
    fun `parseStoreFormDataJson - top level credentials object missing - parses without error`() = runTest {
        val parsed = "storeFormData_topLevelDataMissing".parseStoreFormDataJson()
        assertNull(parsed.credentials)
    }

    @Test
    fun `parseStoreFormDataRequest - request is empty - exception thrown`() = runTest {
        val result = testee.parseStoreFormDataRequest("")
        assertTrue(result.isFailure)
    }

    @Test
    fun `parseStoreFormDataRequest - malformed JSON - exception thrown`() = runTest {
        val result = testee.parseStoreFormDataRequest("invalid json")
        assertTrue(result.isFailure)
    }

    private suspend fun String.parseStoreFormDataJson(): AutofillStoreFormDataRequest {
        val json = this.loadJsonFile()
        assertNotNull("Failed to load specified JSON file: $this")
        return testee.parseStoreFormDataRequest(json).getOrThrow()
    }

    private fun String.loadJsonFile(): String {
        return FileUtilities.loadText(
            AutofillJsonRequestParserTest::class.java.classLoader!!,
            "json/$this.json",
        )
    }
}
