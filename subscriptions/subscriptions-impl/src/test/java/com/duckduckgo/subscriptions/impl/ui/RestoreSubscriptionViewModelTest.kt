package com.duckduckgo.subscriptions.impl.ui

import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.subscriptions.api.SubscriptionStatus.AUTO_RENEWABLE
import com.duckduckgo.subscriptions.api.SubscriptionStatus.EXPIRED
import com.duckduckgo.subscriptions.impl.RealSubscriptionsManager.Companion.SUBSCRIPTION_NOT_FOUND_ERROR
import com.duckduckgo.subscriptions.impl.RealSubscriptionsManager.RecoverSubscriptionResult
import com.duckduckgo.subscriptions.impl.SubscriptionsChecker
import com.duckduckgo.subscriptions.impl.SubscriptionsManager
import com.duckduckgo.subscriptions.impl.pixels.SubscriptionPixelSender
import com.duckduckgo.subscriptions.impl.repository.Entitlement
import com.duckduckgo.subscriptions.impl.repository.Subscription
import com.duckduckgo.subscriptions.impl.ui.RestoreSubscriptionViewModel.Command.Error
import com.duckduckgo.subscriptions.impl.ui.RestoreSubscriptionViewModel.Command.RestoreFromEmail
import com.duckduckgo.subscriptions.impl.ui.RestoreSubscriptionViewModel.Command.SubscriptionNotFound
import com.duckduckgo.subscriptions.impl.ui.RestoreSubscriptionViewModel.Command.Success
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RestoreSubscriptionViewModelTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val subscriptionsManager: SubscriptionsManager = mock()
    private val pixelSender: SubscriptionPixelSender = mock()
    private val subscriptionsChecker: SubscriptionsChecker = mock()
    private lateinit var viewModel: RestoreSubscriptionViewModel

    @Before
    fun before() {
        viewModel = RestoreSubscriptionViewModel(
            subscriptionsManager = subscriptionsManager,
            dispatcherProvider = coroutineTestRule.testDispatcherProvider,
            pixelSender = pixelSender,
            subscriptionsChecker = subscriptionsChecker,
        )
    }

    @Test
    fun `restoreFromEmail - send command`() = runTest {
        viewModel.commands().test {
            viewModel.restoreFromEmail()
            assertTrue(awaitItem() is RestoreFromEmail)
        }
    }

    @Test
    fun `restoreFromStore - restore from store if failure - return error`() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Failure("error"),
        )

        viewModel.commands().test {
            viewModel.restoreFromStore()
            val result = awaitItem()
            assertTrue(result is Error)
        }
    }

    @Test
    fun `restoreFromStore - no subscription found - not found`() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Failure(SUBSCRIPTION_NOT_FOUND_ERROR),
        )

        viewModel.commands().test {
            viewModel.restoreFromStore()
            val result = awaitItem()
            assertTrue(result is SubscriptionNotFound)
        }
    }

    @Test
    fun `restoreFromStore - not active - subscription not found`() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Success(subscriptionNotActive()),
        )

        viewModel.commands().test {
            viewModel.restoreFromStore()
            val result = awaitItem()
            assertTrue(result is SubscriptionNotFound)
        }
    }

    @Test
    fun `restoreFromStore - if active - return success`() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Success(subscriptionActive()),
        )

        viewModel.commands().test {
            viewModel.restoreFromStore()
            val result = awaitItem()
            assertTrue(result is Success)
        }
    }

    @Test
    fun `restoreFromStore - pixel sent`() = runTest {
        viewModel.restoreFromStore()
        verify(pixelSender).reportActivateSubscriptionRestorePurchaseClick()
    }

    @Test
    fun `restoreFromEmail - pixel sent`() = runTest {
        viewModel.restoreFromEmail()
        verify(pixelSender).reportActivateSubscriptionEnterEmailClick()
    }

    @Test
    fun `restoreFromStore - success - pixel sent`() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Success(subscriptionActive()),
        )

        viewModel.restoreFromStore()
        verify(pixelSender).reportRestoreUsingStoreSuccess()
    }

    @Test
    fun `restoreFromStore - fails because there are no entitlements - pixel is sent`() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Success(subscriptionNotActive()),
        )

        viewModel.restoreFromStore()
        verify(pixelSender).reportRestoreUsingStoreFailureSubscriptionNotFound()
    }

    @Test
    fun `restoreFromStore - fails because there is no subscription - pixel sent`() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Failure(SUBSCRIPTION_NOT_FOUND_ERROR),
        )

        viewModel.restoreFromStore()
        verify(pixelSender).reportRestoreUsingStoreFailureSubscriptionNotFound()
    }

    @Test
    fun `restoreFromStore - fails for other reason - pixel sent`() = runTest {
        whenever(subscriptionsManager.recoverSubscriptionFromStore()).thenReturn(
            RecoverSubscriptionResult.Failure("bad stuff happened"),
        )

        viewModel.restoreFromStore()
        verify(pixelSender).reportRestoreUsingStoreFailureOther()
    }

    private fun subscriptionNotActive(): Subscription {
        return Subscription(
            productId = "productId",
            startedAt = 10000L,
            expiresOrRenewsAt = 10000L,
            status = EXPIRED,
            platform = "google",
            entitlements = emptyList(),
        )
    }

    private fun subscriptionActive(): Subscription {
        return Subscription(
            productId = "productId",
            startedAt = 10000L,
            expiresOrRenewsAt = 10000L,
            status = AUTO_RENEWABLE,
            platform = "google",
            entitlements = listOf(Entitlement("name", "product")),
        )
    }
}
