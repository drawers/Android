package com.duckduckgo.subscriptions.impl.ui

import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.subscriptions.api.SubscriptionStatus
import com.duckduckgo.subscriptions.api.SubscriptionStatus.*
import com.duckduckgo.subscriptions.impl.SubscriptionsConstants
import com.duckduckgo.subscriptions.impl.SubscriptionsManager
import com.duckduckgo.subscriptions.impl.pixels.SubscriptionPixelSender
import com.duckduckgo.subscriptions.impl.repository.Subscription
import com.duckduckgo.subscriptions.impl.ui.SubscriptionSettingsViewModel.Command.FinishSignOut
import com.duckduckgo.subscriptions.impl.ui.SubscriptionSettingsViewModel.Command.GoToPortal
import com.duckduckgo.subscriptions.impl.ui.SubscriptionSettingsViewModel.SubscriptionDuration.Monthly
import com.duckduckgo.subscriptions.impl.ui.SubscriptionSettingsViewModel.SubscriptionDuration.Yearly
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SubscriptionSettingsViewModelTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val subscriptionsManager: SubscriptionsManager = mock()
    private val pixelSender: SubscriptionPixelSender = mock()
    private lateinit var viewModel: SubscriptionSettingsViewModel

    @Before
    fun before() {
        viewModel = SubscriptionSettingsViewModel(subscriptionsManager, pixelSender)
    }

    @Test
    fun `removeFromDevice - finish sign out`() = runTest {
        viewModel.commands().test {
            viewModel.removeFromDevice()
            assertTrue(awaitItem() is FinishSignOut)
        }
    }

    @Test
    fun `onCreate - subscription - format date correctly`() = runTest {
        whenever(subscriptionsManager.getSubscription()).thenReturn(
            Subscription(
                productId = SubscriptionsConstants.MONTHLY_PLAN,
                startedAt = 1234,
                expiresOrRenewsAt = 1701694623000,
                status = AUTO_RENEWABLE,
                platform = "android",
                entitlements = emptyList(),
            ),
        )
        val flowTest: MutableSharedFlow<SubscriptionStatus> = MutableSharedFlow()
        whenever(subscriptionsManager.subscriptionStatus).thenReturn(flowTest)

        viewModel.onCreate(mock())
        flowTest.emit(AUTO_RENEWABLE)
        viewModel.viewState.test {
            assertEquals("December 04, 2023", awaitItem().date)
        }
    }

    @Test
    fun `onCreate - subscription monthly - return monthly`() = runTest {
        whenever(subscriptionsManager.getSubscription()).thenReturn(
            Subscription(
                productId = SubscriptionsConstants.MONTHLY_PLAN,
                startedAt = 1234,
                expiresOrRenewsAt = 1701694623000,
                status = AUTO_RENEWABLE,
                platform = "android",
                entitlements = emptyList(),
            ),
        )
        val flowTest: MutableSharedFlow<SubscriptionStatus> = MutableSharedFlow()
        whenever(subscriptionsManager.subscriptionStatus).thenReturn(flowTest)

        viewModel.onCreate(mock())
        flowTest.emit(AUTO_RENEWABLE)
        viewModel.viewState.test {
            assertEquals(Monthly, awaitItem().duration)
        }
    }

    @Test
    fun `onCreate - subscription yearly - yearly`() = runTest {
        whenever(subscriptionsManager.getSubscription()).thenReturn(
            Subscription(
                productId = SubscriptionsConstants.YEARLY_PLAN,
                startedAt = 1234,
                expiresOrRenewsAt = 1701694623000,
                status = AUTO_RENEWABLE,
                platform = "android",
                entitlements = emptyList(),
            ),
        )

        val flowTest: MutableSharedFlow<SubscriptionStatus> = MutableSharedFlow()
        whenever(subscriptionsManager.subscriptionStatus).thenReturn(flowTest)

        viewModel.onCreate(mock())
        flowTest.emit(AUTO_RENEWABLE)
        viewModel.viewState.test {
            assertEquals(Yearly, awaitItem().duration)
        }
    }

    @Test
    fun `goToStripe - no url - do nothing`() = runTest {
        whenever(subscriptionsManager.getPortalUrl()).thenReturn(null)

        viewModel.commands().test {
            viewModel.goToStripe()
            expectNoEvents()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `goToStripe - no url - do send command with url`() = runTest {
        whenever(subscriptionsManager.getPortalUrl()).thenReturn("example.com")

        viewModel.commands().test {
            viewModel.goToStripe()
            val value = awaitItem() as GoToPortal
            assertEquals("example.com", value.url)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `removeFromDevice - pixel sent`() = runTest {
        viewModel.removeFromDevice()
        verify(pixelSender).reportSubscriptionSettingsRemoveFromDeviceClick()
    }
}
