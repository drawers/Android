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

package com.duckduckgo.mobile.android.vpn.ui.notification

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.common.utils.plugins.PluginPoint
import com.duckduckgo.mobile.android.app.tracking.AppTrackingProtection
import com.duckduckgo.mobile.android.vpn.feature.removal.VpnFeatureRemover
import com.duckduckgo.mobile.android.vpn.service.VpnReminderNotificationContentPlugin
import com.duckduckgo.mobile.android.vpn.service.VpnReminderNotificationContentPlugin.NotificationContent
import com.duckduckgo.mobile.android.vpn.service.VpnReminderNotificationContentPlugin.NotificationPriority
import com.duckduckgo.mobile.android.vpn.service.VpnReminderNotificationContentPlugin.NotificationPriority.HIGH
import com.duckduckgo.mobile.android.vpn.service.VpnReminderNotificationContentPlugin.Type
import com.duckduckgo.mobile.android.vpn.service.VpnReminderNotificationContentPlugin.Type.DISABLED
import com.duckduckgo.mobile.android.vpn.service.VpnReminderNotificationWorker
import com.duckduckgo.mobile.android.vpn.service.VpnReminderReceiverManager
import com.duckduckgo.mobile.android.vpn.service.notification.getHighestPriorityPluginForType
import com.duckduckgo.mobile.android.vpn.state.VpnStateMonitor.VpnStopReason.REVOKED
import com.duckduckgo.mobile.android.vpn.state.VpnStateMonitor.VpnStopReason.SELF_STOP
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AppTPReminderNotificationSchedulerTest {

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var workManager: WorkManager
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var testee: AppTPReminderNotificationScheduler
    private val vpnFeatureRemover: VpnFeatureRemover = mock()
    private val mockVpnReminderReceiverManager: VpnReminderReceiverManager = mock()
    private val mockPluginPoint: PluginPoint<VpnReminderNotificationContentPlugin> = mock()
    private val vpnReminderNotificationBuilder: VpnReminderNotificationBuilder = mock()
    private val appTrackingProtection: AppTrackingProtection = mock()

    @Before
    fun before() {
        whenever(vpnReminderNotificationBuilder.buildReminderNotification(any())).thenReturn(mock())
        initializeWorkManager()
        notificationManager = NotificationManagerCompat.from(context)
        testee =
            AppTPReminderNotificationScheduler(
                context,
                workManager,
                notificationManager,
                vpnFeatureRemover,
                coroutinesTestRule.testDispatcherProvider,
                vpnReminderNotificationBuilder,
                mockPluginPoint,
                appTrackingProtection,
            )
    }

    @After
    fun after() {
        workManager.cancelAllWork()
    }

    // https://developer.android.com/topic/libraries/architecture/workmanager/how-to/integration-testing
    private fun initializeWorkManager() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(testWorkerFactory())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun `onVpnStarted - appTP enabled - undesired reminder enqueued`() = runTest {
        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG)
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)

        testee.onVpnStarted(coroutinesTestRule.testScope)

        assertWorkersAreEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG)
    }

    @Test
    fun `onVpnStarted - appTP disabled - enqueue nothing`() = runTest {
        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG)
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)

        testee.onVpnStarted(coroutinesTestRule.testScope)

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG)
    }

    @Test
    fun `onVpnStarted - VPN starts - daily reminder not enqueued`() = runTest {
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)

        testee.onVpnStarted(coroutinesTestRule.testScope)

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)
    }

    @Test
    fun `onVpnStarted - daily reminder was enqueued - daily reminder is not enqueued`() = runTest {
        enqueueDailyReminderNotificationWorker()
        assertWorkersAreEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)

        testee.onVpnStarted(coroutinesTestRule.testScope)

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)
    }

    @Test
    fun `onVpnStopped - VPN manually stops due to snooze - daily reminder is not enqueued`() = runTest {
        whenever(vpnFeatureRemover.isFeatureRemoved()).thenReturn(false)
        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)
        whenever(appTrackingProtection.isOnboarded()).thenReturn(true)

        testee.onVpnStarted(coroutinesTestRule.testScope)

        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        testee.onVpnStopped(coroutinesTestRule.testScope, SELF_STOP(1234L))

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)
    }

    @Test
    fun `onVpnStopped - VPN manually stops - daily reminder enqueued`() = runTest {
        whenever(vpnFeatureRemover.isFeatureRemoved()).thenReturn(false)
        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)
        whenever(appTrackingProtection.isOnboarded()).thenReturn(true)

        testee.onVpnStarted(coroutinesTestRule.testScope)

        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        testee.onVpnStopped(coroutinesTestRule.testScope, SELF_STOP())

        assertWorkersAreEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)
    }

    @Test
    fun `onVpnStopped - VPN stopped but appTP disabled was removed - nothing enqueued`() = runTest {
        whenever(vpnFeatureRemover.isFeatureRemoved()).thenReturn(false)
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        whenever(appTrackingProtection.isOnboarded()).thenReturn(true)

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)

        testee.onVpnStopped(coroutinesTestRule.testScope, SELF_STOP())

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)
    }

    @Test
    fun `onVpnStopped - feature removed - nothing enqueued`() = runTest {
        whenever(vpnFeatureRemover.isFeatureRemoved()).thenReturn(true)

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)

        testee.onVpnStopped(coroutinesTestRule.testScope, SELF_STOP())

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)
    }

    @Test
    fun `onVpnStopped - daily reminder enqueued - still enqueued`() = runTest {
        whenever(vpnFeatureRemover.isFeatureRemoved()).thenReturn(false)
        enqueueDailyReminderNotificationWorker()
        assertWorkersAreEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)

        testee.onVpnStopped(coroutinesTestRule.testScope, SELF_STOP())

        assertWorkersAreEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)
    }

    @Test
    fun `onVpnStopped - manually stops - undesired reminder not scheduled`() = runTest {
        whenever(vpnFeatureRemover.isFeatureRemoved()).thenReturn(false)
        testee.onVpnStopped(coroutinesTestRule.testScope, SELF_STOP())

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG)
    }

    @Test
    fun `onVpnStopped - undesired reminder was scheduled - undesired reminder is no longer scheduled`() = runTest {
        whenever(vpnFeatureRemover.isFeatureRemoved()).thenReturn(false)
        enqueueUndesiredReminderNotificationWorker()
        assertWorkersAreEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG)
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)
        whenever(appTrackingProtection.isOnboarded()).thenReturn(true)

        testee.onVpnStarted(coroutinesTestRule.testScope)

        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        testee.onVpnStopped(coroutinesTestRule.testScope, SELF_STOP())

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG)
    }

    @Test
    fun `onVpnStopped - no content plugin for disabled - no immediate notification shown`() = runTest {
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)
        whenever(vpnFeatureRemover.isFeatureRemoved()).thenReturn(false)
        whenever(mockPluginPoint.getHighestPriorityPluginForType(DISABLED)).thenReturn(null)

        testee.onVpnStopped(coroutinesTestRule.testScope, SELF_STOP())

        verifyNoInteractions(vpnReminderNotificationBuilder)
    }

    @Test
    fun `onVpnStopped - user onboarded and VPN manually stops with content plugin for disabled - immediate notification shown`() = runTest {
        whenever(mockPluginPoint.getPlugins()).thenReturn(listOf(fakeRevokedPlugin, fakeDisabledPlugin))
        whenever(vpnFeatureRemover.isFeatureRemoved()).thenReturn(false)
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)
        whenever(appTrackingProtection.isOnboarded()).thenReturn(true)
        testee.onVpnStarted(coroutinesTestRule.testScope)

        testee.onVpnStopped(coroutinesTestRule.testScope, SELF_STOP())

        verify(vpnReminderNotificationBuilder).buildReminderNotification(fakeDisabledPlugin.getContent())
    }

    @Test
    fun `onVpnStopped - user not onboarded and VPN manually stops with content plugin disabled - no immediate notification`() = runTest {
        whenever(mockPluginPoint.getPlugins()).thenReturn(listOf(fakeRevokedPlugin, fakeDisabledPlugin))
        whenever(vpnFeatureRemover.isFeatureRemoved()).thenReturn(false)
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)
        whenever(appTrackingProtection.isOnboarded()).thenReturn(false)
        testee.onVpnStarted(coroutinesTestRule.testScope)

        testee.onVpnStopped(coroutinesTestRule.testScope, SELF_STOP())

        verifyNoInteractions(vpnReminderNotificationBuilder)
    }

    @Test
    fun `onVpnStopped - no content plugin for revoked - no immediate notification shown`() = runTest {
        whenever(vpnFeatureRemover.isFeatureRemoved()).thenReturn(false)
        whenever(mockPluginPoint.getHighestPriorityPluginForType(Type.REVOKED)).thenReturn(null)
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)

        testee.onVpnStarted(coroutinesTestRule.testScope)
        testee.onVpnStopped(coroutinesTestRule.testScope, REVOKED)

        verifyNoInteractions(vpnReminderNotificationBuilder)
    }

    @Test
    fun `onVpnStopped - appTP enabled and onboarded, VPN revoked with content plugin for revoked - immediate notification shown`() = runTest {
        whenever(mockPluginPoint.getPlugins()).thenReturn(listOf(fakeRevokedPlugin, fakeDisabledPlugin))
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)
        whenever(appTrackingProtection.isOnboarded()).thenReturn(true)

        testee.onVpnStarted(coroutinesTestRule.testScope)
        testee.onVpnStopped(coroutinesTestRule.testScope, REVOKED)

        verify(vpnReminderNotificationBuilder).buildReminderNotification(fakeRevokedPlugin.getContent())
    }

    @Test
    fun `onVpnStopped - appTP disabled and VPN revoked with content plugin for revoked - no immediate notification shown`() = runTest {
        whenever(mockPluginPoint.getPlugins()).thenReturn(listOf(fakeRevokedPlugin, fakeDisabledPlugin))
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        whenever(appTrackingProtection.isOnboarded()).thenReturn(true)

        testee.onVpnStarted(coroutinesTestRule.testScope)
        testee.onVpnStopped(coroutinesTestRule.testScope, REVOKED)

        verifyNoInteractions(vpnReminderNotificationBuilder)
    }

    @Test
    fun `onVpnStopped - appTP not onboarded and vpn revoked - no immediate notification shown`() = runTest {
        whenever(mockPluginPoint.getPlugins()).thenReturn(listOf(fakeRevokedPlugin, fakeDisabledPlugin))
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        whenever(appTrackingProtection.isOnboarded()).thenReturn(false)

        testee.onVpnStarted(coroutinesTestRule.testScope)
        testee.onVpnStopped(coroutinesTestRule.testScope, REVOKED)

        verifyNoInteractions(vpnReminderNotificationBuilder)
    }

    @Test
    fun `onVpnStopped - VPN killed - undesired reminder enqueued`() {
        testee.onVpnStopped(coroutinesTestRule.testScope, REVOKED)

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG)
    }

    @Test
    fun `onVpnStopped - reminder was scheduled - undesired reminder no longer scheduled`() = runTest {
        enqueueUndesiredReminderNotificationWorker()
        assertWorkersAreEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG)
        whenever(appTrackingProtection.isOnboarded()).thenReturn(true)
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)
        testee.onVpnStarted(coroutinesTestRule.testScope)

        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        testee.onVpnStopped(coroutinesTestRule.testScope, REVOKED)

        assertWorkersAreNotEnqueued(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG)
    }

    @Test
    fun `onVpnReconfigured - appTP disabled on VPN reconfigure - immediate notification shown`() = runTest {
        whenever(mockPluginPoint.getPlugins()).thenReturn(listOf(fakeRevokedPlugin, fakeDisabledPlugin))
        whenever(appTrackingProtection.isOnboarded()).thenReturn(true)
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)
        testee.onVpnStarted(coroutinesTestRule.testScope)

        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        testee.onVpnReconfigured(coroutinesTestRule.testScope)

        verify(vpnReminderNotificationBuilder).buildReminderNotification(fakeDisabledPlugin.getContent())
    }

    @Test
    fun `onVpnReconfigured - appTP disabled - no immediate notification`() = runTest {
        whenever(mockPluginPoint.getPlugins()).thenReturn(listOf(fakeRevokedPlugin, fakeDisabledPlugin))
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        testee.onVpnStarted(coroutinesTestRule.testScope)
        testee.onVpnReconfigured(coroutinesTestRule.testScope)

        verifyNoInteractions(vpnReminderNotificationBuilder)
    }

    @Test
    fun `onVpnReconfigured - appTP enabled - no immediate notification`() = runTest {
        whenever(mockPluginPoint.getPlugins()).thenReturn(listOf(fakeRevokedPlugin, fakeDisabledPlugin))
        whenever(appTrackingProtection.isEnabled()).thenReturn(false)
        testee.onVpnStarted(coroutinesTestRule.testScope)
        whenever(appTrackingProtection.isEnabled()).thenReturn(true)
        testee.onVpnReconfigured(coroutinesTestRule.testScope)

        verifyNoInteractions(vpnReminderNotificationBuilder)
    }

    private fun enqueueDailyReminderNotificationWorker() {
        val requestBuilder = OneTimeWorkRequestBuilder<VpnReminderNotificationWorker>()
        val request = requestBuilder
            .addTag(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG)
            .setInitialDelay(24, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniqueWork(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_DAILY_TAG, ExistingWorkPolicy.KEEP, request)
    }

    private fun enqueueUndesiredReminderNotificationWorker() {
        val requestBuilder = PeriodicWorkRequestBuilder<VpnReminderNotificationWorker>(5, TimeUnit.HOURS)
        val request = requestBuilder
            .addTag(VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG)
            .setInitialDelay(5, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            VpnReminderNotificationWorker.WORKER_VPN_REMINDER_UNDESIRED_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun assertWorkersAreNotEnqueued(tag: String) {
        val scheduledWorkers = getScheduledWorkers(tag)
        scheduledWorkers.forEach { workInfo ->
            Log.d("WorkManager", "workInfo: $workInfo")
            Assert.assertTrue(workInfo.state != WorkInfo.State.ENQUEUED)
        }
    }

    private fun assertWorkersAreEnqueued(tag: String) {
        val scheduledWorkers = getScheduledWorkers(tag)
        Assert.assertTrue(scheduledWorkers.isNotEmpty())
        scheduledWorkers.forEach { workInfo ->
            Log.d("WorkManager", "workInfo: $workInfo")
            Assert.assertTrue(workInfo.state == WorkInfo.State.ENQUEUED)
        }
    }

    private fun testWorkerFactory(): WorkerFactory {
        return object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? {
                return VpnReminderNotificationWorker(appContext, workerParameters).also {
                    it.vpnReminderReceiverManager = mockVpnReminderReceiverManager
                }
            }
        }
    }

    private fun getScheduledWorkers(tag: String): List<WorkInfo> {
        return workManager
            .getWorkInfosByTag(tag)
            .get()
    }

    private val fakeRevokedPlugin = object : VpnReminderNotificationContentPlugin {
        override fun getContent(): NotificationContent = NotificationContent(
            isSilent = false,
            shouldAutoCancel = false,
            title = "",
            onNotificationPressIntent = null,
            notificationAction = emptyList(),
        )

        override fun getPriority(): NotificationPriority = HIGH
        override fun getType(): Type = Type.REVOKED
    }

    private val fakeDisabledPlugin = object : VpnReminderNotificationContentPlugin {
        override fun getContent(): NotificationContent = NotificationContent(
            isSilent = false,
            shouldAutoCancel = false,
            title = "",
            onNotificationPressIntent = null,
            notificationAction = emptyList(),
        )

        override fun getPriority(): NotificationPriority = HIGH
        override fun getType(): Type = DISABLED
    }
}
