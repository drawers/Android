package com.duckduckgo.subscriptions.impl.settings.views

import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.subscriptions.api.SubscriptionStatus
import com.duckduckgo.subscriptions.impl.SubscriptionsManager
import com.duckduckgo.subscriptions.impl.pixels.SubscriptionPixelSender
import com.duckduckgo.subscriptions.impl.settings.views.ProSettingViewModel.Command.OpenBuyScreen
import com.duckduckgo.subscriptions.impl.settings.views.ProSettingViewModel.Command.OpenRestoreScreen
import com.duckduckgo.subscriptions.impl.settings.views.ProSettingViewModel.Command.OpenSettings
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

class ProSettingViewModelTest {
    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val subscriptionsManager: SubscriptionsManager = mock()
    private val pixelSender: SubscriptionPixelSender = mock()
    private lateinit var viewModel: ProSettingViewModel

    @Before
    fun before() {
        viewModel = ProSettingViewModel(subscriptionsManager, pixelSender)
    }

    @Test
    fun `onSettings - command sent`() = runTest {
        viewModel.commands().test {
            viewModel.onSettings()
            assertTrue(awaitItem() is OpenSettings)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onBuy - command sent`() = runTest {
        viewModel.commands().test {
            viewModel.onBuy()
            assertTrue(awaitItem() is OpenBuyScreen)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onRestore - command sent`() = runTest {
        viewModel.commands().test {
            viewModel.onRestore()
            assertTrue(awaitItem() is OpenRestoreScreen)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onResume - emit viewState`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus).thenReturn(flowOf(SubscriptionStatus.EXPIRED))

        viewModel.onCreate(mock())
        viewModel.viewState.test {
            assertEquals(SubscriptionStatus.EXPIRED, awaitItem().status)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `onRestore - pixel sent`() = runTest {
        viewModel.commands().test {
            viewModel.onRestore()
            verify(pixelSender).reportAppSettingsRestorePurchaseClick()
            verifyNoMoreInteractions(pixelSender)
            cancelAndConsumeRemainingEvents()
        }
    }
}
