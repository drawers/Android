/*
 * Copyright (c) 2020 DuckDuckGo
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

package com.duckduckgo.app.location.data

import com.duckduckgo.common.utils.extensions.asLocationPermissionOrigin
import org.junit.Assert
import org.junit.Test

class LocationPermissionEntityTest {

    @Test
    fun `forFireproofing - domain starts with https - drop prefix`() {
        val locationPermissionEntity = LocationPermissionEntity("https://www.example.com/", LocationPermissionType.ALLOW_ONCE)
        val host = locationPermissionEntity.forFireproofing()
        Assert.assertEquals("www.example.com", host)
    }

    @Test
    fun `forFireproofing - domain starts with HTTPS uppercase - drop prefix`() {
        val locationPermissionEntity = LocationPermissionEntity("HTTPS://www.example.com/", LocationPermissionType.ALLOW_ONCE)
        val host = locationPermissionEntity.forFireproofing()
        Assert.assertEquals("www.example.com", host)
    }

    @Test
    fun `forFireproofing - domain does not start with https - domain unchanged`() {
        val locationPermissionEntity = LocationPermissionEntity("mobile.example.com/", LocationPermissionType.ALLOW_ONCE)
        val host = locationPermissionEntity.forFireproofing()
        Assert.assertEquals("mobile.example.com/", host)
    }

    @Test
    fun `asLocationPermissionOrigin - domain - matches`() {
        val domain = "www.example.com"
        val host = domain.asLocationPermissionOrigin()
        Assert.assertEquals("https://www.example.com/", host)
    }
}
