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

package com.duckduckgo.app.browser.state

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.browser.BrowserActivity
import com.duckduckgo.browser.api.BrowserLifecycleObserver
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*

@RunWith(AndroidJUnit4::class)
class BrowserApplicationStateInfoTest {

    private lateinit var browserApplicationStateInfo: BrowserApplicationStateInfo
    private val observer: BrowserLifecycleObserver = mock()
    private val activity = FakeBrowserActivity()

    class FakeBrowserActivity : BrowserActivity() {
        var isConfigChange = false

        override fun isChangingConfigurations(): Boolean {
            return isConfigChange
        }
    }

    @Before
    fun setup() {
        activity.destroyedByBackPress = false
        browserApplicationStateInfo = BrowserApplicationStateInfo(setOf(observer))
    }

    @Test
    fun `onActivityCreated - noop`() {
        browserApplicationStateInfo.onActivityCreated(activity, null)

        verifyNoInteractions(observer)
    }

    @Test
    fun `onActivityStarted - first activity created - notify fresh app launch`() {
        browserApplicationStateInfo.onActivityCreated(activity, null)

        browserApplicationStateInfo.onActivityStarted(activity)

        verify(observer).onOpen(true)
    }

    @Test
    fun `onActivityStarted - all activities stop and restart - notify app open`() {
        browserApplicationStateInfo.onActivityCreated(activity, null)
        browserApplicationStateInfo.onActivityCreated(activity, null)

        browserApplicationStateInfo.onActivityStarted(activity)
        browserApplicationStateInfo.onActivityStarted(activity)

        verify(observer).onOpen(true)

        browserApplicationStateInfo.onActivityStopped(activity)
        browserApplicationStateInfo.onActivityStopped(activity)
        verify(observer).onClose()
        verify(observer, never()).onExit()

        browserApplicationStateInfo.onActivityStarted(activity)
        browserApplicationStateInfo.onActivityStarted(activity)
        verify(observer).onOpen(false)
    }

    @Test
    fun `onActivityCreated - all activities destroyed and recreated - notify fresh app launch`() {
        browserApplicationStateInfo.onActivityCreated(activity, null)
        browserApplicationStateInfo.onActivityCreated(activity, null)

        browserApplicationStateInfo.onActivityStarted(activity)
        browserApplicationStateInfo.onActivityStarted(activity)

        verify(observer).onOpen(true)

        browserApplicationStateInfo.onActivityStopped(activity)
        browserApplicationStateInfo.onActivityStopped(activity)
        verify(observer).onClose()

        browserApplicationStateInfo.onActivityDestroyed(activity)
        browserApplicationStateInfo.onActivityDestroyed(activity)
        verify(observer).onClose()
        verify(observer).onExit()

        browserApplicationStateInfo.onActivityStarted(activity)
        browserApplicationStateInfo.onActivityStarted(activity)
        verify(observer).onOpen(true)
    }

    @Test
    fun `onActivityLifecycle - all activities destroyed by back press and recreated - do not notify fresh app launch`() {
        activity.destroyedByBackPress = true

        browserApplicationStateInfo.onActivityCreated(activity, null)
        browserApplicationStateInfo.onActivityCreated(activity, null)

        browserApplicationStateInfo.onActivityStarted(activity)
        browserApplicationStateInfo.onActivityStarted(activity)

        verify(observer).onOpen(true)

        browserApplicationStateInfo.onActivityStopped(activity)
        browserApplicationStateInfo.onActivityStopped(activity)
        verify(observer).onClose()

        browserApplicationStateInfo.onActivityDestroyed(activity)
        browserApplicationStateInfo.onActivityDestroyed(activity)
        verify(observer).onClose()
        verify(observer, never()).onExit()

        browserApplicationStateInfo.onActivityStarted(activity)
        browserApplicationStateInfo.onActivityStarted(activity)
        verify(observer).onOpen(false)
    }

    @Test
    fun `onActivityLifecycle - config change and recreate - do not notify fresh app launch`() {
        activity.isConfigChange = true

        browserApplicationStateInfo.onActivityCreated(activity, null)
        browserApplicationStateInfo.onActivityCreated(activity, null)

        browserApplicationStateInfo.onActivityStarted(activity)
        browserApplicationStateInfo.onActivityStarted(activity)

        verify(observer).onOpen(true)

        browserApplicationStateInfo.onActivityStopped(activity)
        browserApplicationStateInfo.onActivityStopped(activity)
        verify(observer).onClose()

        browserApplicationStateInfo.onActivityDestroyed(activity)
        browserApplicationStateInfo.onActivityDestroyed(activity)
        verify(observer).onClose()
        verify(observer, never()).onExit()

        browserApplicationStateInfo.onActivityStarted(activity)
        browserApplicationStateInfo.onActivityStarted(activity)
        verify(observer).onOpen(false)
    }
}
