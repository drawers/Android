package com.duckduckgo.subscriptions.impl

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhase
import com.android.billingclient.api.ProductDetails.PricingPhases
import com.android.billingclient.api.PurchaseHistoryRecord
import com.duckduckgo.autofill.api.email.EmailManager
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.subscriptions.api.Product.NetP
import com.duckduckgo.subscriptions.api.SubscriptionStatus
import com.duckduckgo.subscriptions.api.SubscriptionStatus.*
import com.duckduckgo.subscriptions.impl.RealSubscriptionsManager.RecoverSubscriptionResult
import com.duckduckgo.subscriptions.impl.SubscriptionsConstants.MONTHLY_PLAN
import com.duckduckgo.subscriptions.impl.SubscriptionsConstants.YEARLY_PLAN
import com.duckduckgo.subscriptions.impl.billing.PlayBillingManager
import com.duckduckgo.subscriptions.impl.billing.PurchaseState
import com.duckduckgo.subscriptions.impl.pixels.SubscriptionPixelSender
import com.duckduckgo.subscriptions.impl.repository.AuthRepository
import com.duckduckgo.subscriptions.impl.repository.FakeSubscriptionsDataStore
import com.duckduckgo.subscriptions.impl.repository.RealAuthRepository
import com.duckduckgo.subscriptions.impl.services.AccessTokenResponse
import com.duckduckgo.subscriptions.impl.services.AccountResponse
import com.duckduckgo.subscriptions.impl.services.AuthService
import com.duckduckgo.subscriptions.impl.services.ConfirmationEntitlement
import com.duckduckgo.subscriptions.impl.services.ConfirmationResponse
import com.duckduckgo.subscriptions.impl.services.CreateAccountResponse
import com.duckduckgo.subscriptions.impl.services.DeleteAccountResponse
import com.duckduckgo.subscriptions.impl.services.EntitlementResponse
import com.duckduckgo.subscriptions.impl.services.PortalResponse
import com.duckduckgo.subscriptions.impl.services.StoreLoginResponse
import com.duckduckgo.subscriptions.impl.services.SubscriptionResponse
import com.duckduckgo.subscriptions.impl.services.SubscriptionsService
import com.duckduckgo.subscriptions.impl.services.ValidateTokenResponse
import com.duckduckgo.subscriptions.impl.store.SubscriptionsDataStore
import java.lang.Exception
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class RealSubscriptionsManagerTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val authService: AuthService = mock()
    private val subscriptionsService: SubscriptionsService = mock()
    private val authDataStore: SubscriptionsDataStore = FakeSubscriptionsDataStore()
    private val authRepository = RealAuthRepository(authDataStore, coroutineRule.testDispatcherProvider)
    private val emailManager: EmailManager = mock()
    private val playBillingManager: PlayBillingManager = mock()
    private val context: Context = mock()
    private val pixelSender: SubscriptionPixelSender = mock()
    private lateinit var subscriptionsManager: SubscriptionsManager

    @Before
    fun before() = runTest {
        whenever(emailManager.getToken()).thenReturn(null)
        whenever(context.packageName).thenReturn("packageName")
        subscriptionsManager = RealSubscriptionsManager(
            authService,
            subscriptionsService,
            authRepository,
            playBillingManager,
            emailManager,
            context,
            TestScope(),
            coroutineRule.testDispatcherProvider,
            pixelSender,
        )
    }

    @Test
    fun `recoverSubscriptionFromStore - user not authenticated and no purchase stored - return failure`() = runTest {
        givenUserIsNotAuthenticated()

        val value = subscriptionsManager.recoverSubscriptionFromStore()

        assertTrue(value is RecoverSubscriptionResult.Failure)
    }

    @Test
    fun `recoverSubscriptionFromStore - user not authenticated and purchase stored - return subscription and store data`() = runTest {
        givenUserIsNotAuthenticated()
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenSubscriptionSucceedsWithEntitlements()
        givenAccessTokenSucceeds()

        val result = subscriptionsManager.recoverSubscriptionFromStore() as RecoverSubscriptionResult.Success
        val subscription = result.subscription

        verify(authService).storeLogin(any())
        assertEquals("authToken", authDataStore.authToken)
        assertTrue(subscription.entitlements.firstOrNull { it.product == NetP.value } != null)
    }

    @Test
    fun `recoverSubscriptionFromStore - store login fails - return failure`() = runTest {
        givenUserIsNotAuthenticated()
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenStoreLoginFails()

        val result = subscriptionsManager.recoverSubscriptionFromStore()

        assertTrue(result is RecoverSubscriptionResult.Failure)
    }

    @Test
    fun `recoverSubscriptionFromStore - user authenticated with no purchases - return failure`() = runTest {
        givenUserIsAuthenticated()
        givenAccessTokenSucceeds()

        val result = subscriptionsManager.recoverSubscriptionFromStore()

        assertTrue(result is RecoverSubscriptionResult.Failure)
    }

    @Test
    fun `recoverSubscriptionFromStore - validate token succeeds - return external id`() = runTest {
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenSubscriptionSucceedsWithEntitlements()
        givenAccessTokenSucceeds()

        val result = subscriptionsManager.recoverSubscriptionFromStore() as RecoverSubscriptionResult.Success
        val subscription = result.subscription

        assertEquals("1234", authDataStore.externalId)
        assertTrue(subscription.entitlements.firstOrNull { it.product == NetP.value } != null)
    }

    @Test
    fun `recoverSubscriptionFromStore - subscription expired - return failure`() = runTest {
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenSubscriptionExists(EXPIRED)
        givenAccessTokenSucceeds()

        val result = subscriptionsManager.recoverSubscriptionFromStore()

        assertTrue(result is RecoverSubscriptionResult.Failure)
    }

    @Test
    fun `recoverSubscriptionFromStore - validate token fails - return failure`() = runTest {
        givenUserIsAuthenticated()
        givenValidateTokenFails("failure")

        val result = subscriptionsManager.recoverSubscriptionFromStore()

        assertTrue(result is RecoverSubscriptionResult.Failure)
    }

    @Test
    fun `recoverSubscriptionFromStore - purchase history retrieved - sign in user and set token`() = runTest {
        givenUserIsNotAuthenticated()
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenSubscriptionSucceedsWithoutEntitlements()
        givenAccessTokenSucceeds()

        subscriptionsManager.recoverSubscriptionFromStore()
        subscriptionsManager.isSignedIn.test {
            assertTrue(awaitItem())
            assertEquals("accessToken", authDataStore.accessToken)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchAndStoreAllData - user not authenticated - return null subscription`() = runTest {
        givenUserIsNotAuthenticated()

        val value = subscriptionsManager.fetchAndStoreAllData()

        assertNull(value)
    }

    @Test
    fun `fetchAndStoreAllData - token is valid - return subscription`() = runTest {
        givenUserIsAuthenticated()
        givenSubscriptionSucceedsWithEntitlements()

        val value = subscriptionsManager.fetchAndStoreAllData()
        assertEquals("1234", authDataStore.externalId)
        assertTrue(value?.entitlements?.firstOrNull { it.product == NetP.value } != null)
    }

    @Test
    fun `fetchAndStoreAllData - token is valid - emit entitlements`() = runTest {
        givenUserIsAuthenticated()
        givenSubscriptionSucceedsWithEntitlements()

        subscriptionsManager.fetchAndStoreAllData()
        subscriptionsManager.entitlements.test {
            assertTrue(awaitItem().size == 1)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchAndStoreAllData - subscription fails - return null`() = runTest {
        givenUserIsAuthenticated()
        givenSubscriptionFails()

        assertNull(subscriptionsManager.fetchAndStoreAllData())
    }

    @Test
    fun `purchase - user not authenticated and no purchase stored - create account`() = runTest {
        givenUserIsNotAuthenticated()

        subscriptionsManager.purchase(mock(), planId = "")

        verify(authService).createAccount(any())
    }

    @Test
    fun `purchase - user not authenticated and no purchase stored and signed in email - create account with email token`() = runTest {
        whenever(emailManager.getToken()).thenReturn("emailToken")
        givenUserIsNotAuthenticated()

        subscriptionsManager.purchase(mock(), planId = "")

        verify(authService).createAccount("Bearer emailToken")
    }

    @Test
    fun `purchase - create account fails - return failure`() = runTest {
        givenUserIsNotAuthenticated()
        givenCreateAccountFails()

        subscriptionsManager.currentPurchaseState.test {
            subscriptionsManager.purchase(mock(), planId = "")
            assertTrue(awaitItem() is CurrentPurchase.PreFlowInProgress)
            assertTrue(awaitItem() is CurrentPurchase.Failure)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `purchase - create account succeeds - billing flow uses correct external id`() = runTest {
        givenUserIsNotAuthenticated()
        givenCreateAccountSucceeds()
        givenValidateTokenSucceedsNoEntitlements()
        givenAccessTokenSucceeds()

        subscriptionsManager.purchase(mock(), planId = "")

        verify(playBillingManager).launchBillingFlow(any(), any(), externalId = eq("1234"))
    }

    @Test
    fun `purchase - user not authenticated and purchase not active in store - get id from purchase`() = runTest {
        givenUserIsNotAuthenticated()
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenSubscriptionSucceedsWithoutEntitlements(status = "Expired")
        givenAccessTokenSucceeds()

        subscriptionsManager.purchase(mock(), "")

        verify(playBillingManager).launchBillingFlow(any(), any(), externalId = eq("1234"))
    }

    @Test
    fun `purchase - user not authenticated and purchase active in store - recover subscription`() = runTest {
        givenUserIsNotAuthenticated()
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenSubscriptionSucceedsWithEntitlements()
        givenAccessTokenSucceeds()

        subscriptionsManager.currentPurchaseState.test {
            subscriptionsManager.purchase(mock(), planId = "")
            assertTrue(awaitItem() is CurrentPurchase.PreFlowInProgress)
            verify(playBillingManager, never()).launchBillingFlow(any(), any(), any())
            assertTrue(awaitItem() is CurrentPurchase.Recovered)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `purchase - store login fails - return failure`() = runTest {
        givenUserIsNotAuthenticated()
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenStoreLoginFails()

        subscriptionsManager.currentPurchaseState.test {
            subscriptionsManager.purchase(mock(), planId = "")
            assertTrue(awaitItem() is CurrentPurchase.PreFlowInProgress)
            assertTrue(awaitItem() is CurrentPurchase.Failure)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `purchase - user authenticated - validate token`() = runTest {
        givenUserIsAuthenticated()

        subscriptionsManager.purchase(mock(), planId = "")

        verify(authService).validateToken(any())
    }

    @Test
    fun `purchase - validate token succeeds - billing flow uses correct external id and emit states`() = runTest {
        givenUserIsAuthenticated()
        givenSubscriptionSucceedsWithoutEntitlements(status = "Expired")

        subscriptionsManager.currentPurchaseState.test {
            subscriptionsManager.purchase(mock(), planId = "")
            assertTrue(awaitItem() is CurrentPurchase.PreFlowInProgress)
            verify(playBillingManager).launchBillingFlow(any(), any(), externalId = eq("1234"))
            assertTrue(awaitItem() is CurrentPurchase.PreFlowFinished)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun whenPurchaseFlowIfCreateAccountFailsReturnFailure() = runTest {
        givenUserIsNotAuthenticated()
        givenCreateAccountFails()

        subscriptionsManager.currentPurchaseState.test {
            subscriptionsManager.purchase(mock(), planId = "")
            assertTrue(awaitItem() is CurrentPurchase.PreFlowInProgress)
            assertTrue(awaitItem() is CurrentPurchase.Failure)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `purchase - null subscription and authenticated - do not create account`() = runTest {
        givenUserIsAuthenticated()
        givenValidateTokenFails("failure")

        subscriptionsManager.purchase(mock(), "")

        verify(authService, never()).createAccount(any())
    }

    @Test
    fun `purchase - account created - set tokens`() = runTest {
        givenUserIsNotAuthenticated()
        givenCreateAccountSucceeds()
        givenSubscriptionSucceedsWithoutEntitlements()
        givenAccessTokenSucceeds()

        subscriptionsManager.purchase(mock(), planId = "")
        assertEquals("accessToken", authDataStore.accessToken)
        assertEquals("authToken", authDataStore.authToken)
    }

    @Test
    fun `purchase - purchase history retrieved - sign in user and set token`() = runTest {
        givenUserIsNotAuthenticated()
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenSubscriptionSucceedsWithoutEntitlements()
        givenAccessTokenSucceeds()

        subscriptionsManager.purchase(mock(), planId = "")
        subscriptionsManager.isSignedIn.test {
            assertTrue(awaitItem())
            assertEquals("accessToken", authDataStore.accessToken)
            assertEquals("authToken", authDataStore.authToken)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test(expected = Exception::class)
    fun `exchangeAuthToken - token exchange fails - throws exception`() = runTest {
        givenAccessTokenFails()

        subscriptionsManager.exchangeAuthToken("authToken")
    }

    @Test
    fun `exchangeAuthToken - access token - exchange token and store`() = runTest {
        givenAccessTokenSucceeds()

        val result = subscriptionsManager.exchangeAuthToken("authToken")
        assertEquals("accessToken", authDataStore.accessToken)
        assertEquals("accessToken", result)
    }

    @Test
    fun `subscriptionStatus - emit`() = runTest {
        val manager = RealSubscriptionsManager(
            authService,
            subscriptionsService,
            authRepository,
            playBillingManager,
            emailManager,
            context,
            TestScope(),
            coroutineRule.testDispatcherProvider,
            pixelSender,
        )

        manager.subscriptionStatus.test {
            assertEquals(UNKNOWN, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `subscriptionStatus - subscription exists - emit`() = runTest {
        givenUserIsAuthenticated()
        givenSubscriptionExists()
        val manager = RealSubscriptionsManager(
            authService,
            subscriptionsService,
            authRepository,
            playBillingManager,
            emailManager,
            context,
            TestScope(),
            coroutineRule.testDispatcherProvider,
            pixelSender,
        )

        manager.subscriptionStatus.test {
            assertEquals(AUTO_RENEWABLE, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `currentPurchaseState - purchase successful - success emit`() = runTest {
        givenUserIsAuthenticated()
        givenConfirmPurchaseSucceeds()

        val flowTest: MutableSharedFlow<PurchaseState> = MutableSharedFlow()
        whenever(playBillingManager.purchaseState).thenReturn(flowTest)

        val manager = RealSubscriptionsManager(
            authService,
            subscriptionsService,
            authRepository,
            playBillingManager,
            emailManager,
            context,
            TestScope(),
            coroutineRule.testDispatcherProvider,
            pixelSender,
        )

        manager.currentPurchaseState.test {
            flowTest.emit(PurchaseState.Purchased("validToken", "packageName"))
            assertTrue(awaitItem() is CurrentPurchase.InProgress)
            assertTrue(awaitItem() is CurrentPurchase.Success)
            cancelAndConsumeRemainingEvents()
        }

        manager.entitlements.test {
            flowTest.emit(PurchaseState.Purchased("validToken", "packageName"))
            assertTrue(awaitItem().size == 1)
            cancelAndConsumeRemainingEvents()
        }

        manager.subscriptionStatus.test {
            flowTest.emit(PurchaseState.Purchased("validToken", "packageName"))
            assertEquals(AUTO_RENEWABLE, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `currentPurchaseState - purchase failed - checked and waiting emit`() = runTest {
        givenUserIsAuthenticated()
        givenValidateTokenFails("failure")
        givenConfirmPurchaseFails()

        val flowTest: MutableSharedFlow<PurchaseState> = MutableSharedFlow()
        whenever(playBillingManager.purchaseState).thenReturn(flowTest)

        val manager = RealSubscriptionsManager(
            authService,
            subscriptionsService,
            authRepository,
            playBillingManager,
            emailManager,
            context,
            TestScope(),
            coroutineRule.testDispatcherProvider,
            pixelSender,
        )

        manager.currentPurchaseState.test {
            flowTest.emit(PurchaseState.Purchased("validateToken", "packageName"))
            assertTrue(awaitItem() is CurrentPurchase.InProgress)
            assertTrue(awaitItem() is CurrentPurchase.Waiting)
            cancelAndConsumeRemainingEvents()
        }

        manager.subscriptionStatus.test {
            flowTest.emit(PurchaseState.Purchased("validateToken", "packageName"))
            assertEquals(WAITING, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `currentPurchaseState - purchase canceled - emit canceled`() = runTest {
        val flowTest: MutableSharedFlow<PurchaseState> = MutableSharedFlow()
        whenever(playBillingManager.purchaseState).thenReturn(flowTest)

        val manager = RealSubscriptionsManager(
            authService,
            subscriptionsService,
            authRepository,
            playBillingManager,
            emailManager,
            context,
            TestScope(),
            coroutineRule.testDispatcherProvider,
            pixelSender,
        )

        manager.currentPurchaseState.test {
            flowTest.emit(PurchaseState.Canceled)
            assertTrue(awaitItem() is CurrentPurchase.Canceled)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getAccessToken - user is authenticated - return success`() = runTest {
        givenUserIsAuthenticated()

        val result = subscriptionsManager.getAccessToken()

        assertTrue(result is AccessToken.Success)
        assertEquals("accessToken", (result as AccessToken.Success).accessToken)
    }

    @Test
    fun `getAccessToken - user not authenticated - return failure`() = runTest {
        givenUserIsNotAuthenticated()

        val result = subscriptionsManager.getAccessToken()

        assertTrue(result is AccessToken.Failure)
    }

    @Test
    fun `getAuthToken - user authenticated and valid token - return success`() = runTest {
        givenUserIsAuthenticated()
        givenValidateTokenSucceedsWithEntitlements()

        val result = subscriptionsManager.getAuthToken()

        assertTrue(result is AuthToken.Success)
        assertEquals("authToken", (result as AuthToken.Success).authToken)
    }

    @Test
    fun `getAuthToken - user not authenticated - return failure`() = runTest {
        givenUserIsNotAuthenticated()

        val result = subscriptionsManager.getAuthToken()

        assertTrue(result is AuthToken.Failure)
    }

    @Test
    fun `getAuthToken - token expired and entitlements exist - return success`() = runTest {
        authDataStore.externalId = "1234"
        givenUserIsAuthenticated()
        givenSubscriptionSucceedsWithEntitlements()
        givenValidateTokenFailsAndThenSucceeds("""{ "error": "expired_token" }""")
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenAccessTokenSucceeds()

        val result = subscriptionsManager.getAuthToken()

        verify(authService).storeLogin(any())
        assertTrue(result is AuthToken.Success)
        assertEquals("authToken", (result as AuthToken.Success).authToken)
    }

    @Test
    fun `getAuthToken - token expired and external ID different - return failure`() = runTest {
        authDataStore.externalId = "test"
        givenUserIsAuthenticated()
        givenSubscriptionSucceedsWithEntitlements()
        givenValidateTokenFailsAndThenSucceeds("""{ "error": "expired_token" }""")
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenAccessTokenSucceeds()

        val result = subscriptionsManager.getAuthToken()

        verify(authService).storeLogin(any())
        assertTrue(result is AuthToken.Failure)
    }

    @Test
    fun `getAuthToken - token expired and no entitlements - return failure`() = runTest {
        givenUserIsAuthenticated()
        givenValidateTokenSucceedsNoEntitlements()
        givenValidateTokenFailsAndThenSucceedsWithNoEntitlements("""{ "error": "expired_token" }""")
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenAccessTokenSucceeds()

        val result = subscriptionsManager.getAuthToken()

        verify(authService).storeLogin(any())
        assertTrue(result is AuthToken.Failure)
    }

    @Test
    fun `getAuthToken - token expired and no purchase - return failure`() = runTest {
        givenUserIsAuthenticated()
        givenValidateTokenFailsAndThenSucceeds("""{ "error": "expired_token" }""")

        val result = subscriptionsManager.getAuthToken()

        verify(authService, never()).storeLogin(any())
        assertTrue(result is AuthToken.Failure)
    }

    @Test
    fun `getAuthToken - token expired and purchase not valid - return failure`() = runTest {
        givenUserIsAuthenticated()
        givenValidateTokenFailsAndThenSucceeds("""{ "error": "expired_token" }""")
        givenStoreLoginFails()
        givenPurchaseStored()

        val result = subscriptionsManager.getAuthToken()

        verify(authService).storeLogin(any())
        assertTrue(result is AuthToken.Failure)
    }

    @Test
    fun `getSubscription - authenticated user - return correct status`() = runTest {
        givenUserIsAuthenticated()
        givenValidateTokenSucceedsWithEntitlements()

        givenSubscriptionSucceedsWithEntitlements("Auto-Renewable")
        subscriptionsManager.fetchAndStoreAllData()
        assertEquals(AUTO_RENEWABLE, subscriptionsManager.getSubscription()?.status)

        givenSubscriptionSucceedsWithEntitlements("Not Auto-Renewable")
        subscriptionsManager.fetchAndStoreAllData()
        assertEquals(NOT_AUTO_RENEWABLE, subscriptionsManager.getSubscription()?.status)

        givenSubscriptionSucceedsWithEntitlements("Grace Period")
        subscriptionsManager.fetchAndStoreAllData()
        assertEquals(GRACE_PERIOD, subscriptionsManager.getSubscription()?.status)

        givenSubscriptionSucceedsWithEntitlements("Inactive")
        subscriptionsManager.fetchAndStoreAllData()
        assertEquals(INACTIVE, subscriptionsManager.getSubscription()?.status)

        givenSubscriptionSucceedsWithEntitlements("Expired")
        subscriptionsManager.fetchAndStoreAllData()
        assertEquals(EXPIRED, subscriptionsManager.getSubscription()?.status)

        givenSubscriptionSucceedsWithEntitlements("test")
        subscriptionsManager.fetchAndStoreAllData()
        assertEquals(UNKNOWN, subscriptionsManager.getSubscription()?.status)
    }

    @Test
    fun `getPortalUrl - user authenticated - return url`() = runTest {
        givenUserIsAuthenticated()
        givenUrlPortalSucceeds()

        assertEquals("example.com", subscriptionsManager.getPortalUrl())
    }

    @Test
    fun `getPortalUrl - user not authenticated - return null`() = runTest {
        givenUserIsNotAuthenticated()

        assertNull(subscriptionsManager.getPortalUrl())
    }

    @Test
    fun `getPortalUrl - portal fails - return null`() = runTest {
        givenUserIsAuthenticated()
        givenUrlPortalFails()

        assertNull(subscriptionsManager.getPortalUrl())
    }

    @Test
    fun `signOut - call repository sign out`() = runTest {
        val mockRepo: AuthRepository = mock()
        val manager = RealSubscriptionsManager(
            authService,
            subscriptionsService,
            mockRepo,
            playBillingManager,
            emailManager,
            context,
            TestScope(),
            coroutineRule.testDispatcherProvider,
            pixelSender,
        )
        manager.signOut()
        verify(mockRepo).clearSubscription()
        verify(mockRepo).clearAccount()
    }

    @Test
    fun `signOut - emit false for isSignedIn`() = runTest {
        givenSubscriptionExists()
        givenUserIsAuthenticated()

        subscriptionsManager.isSignedIn.test {
            assertTrue(awaitItem())
            subscriptionsManager.signOut()
            assertFalse(awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `signOut - emit unknown`() = runTest {
        givenUserIsAuthenticated()
        givenSubscriptionExists()

        val manager = RealSubscriptionsManager(
            authService,
            subscriptionsService,
            authRepository,
            playBillingManager,
            emailManager,
            context,
            TestScope(),
            coroutineRule.testDispatcherProvider,
            pixelSender,
        )

        manager.subscriptionStatus.test {
            assertEquals(AUTO_RENEWABLE, awaitItem())
            manager.signOut()
            assertEquals(UNKNOWN, awaitItem())
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `currentPurchaseState - purchase successful - pixel is sent`() = runTest {
        givenUserIsAuthenticated()
        givenValidateTokenSucceedsWithEntitlements()
        givenConfirmPurchaseSucceeds()

        whenever(playBillingManager.purchaseState).thenReturn(flowOf(PurchaseState.Purchased("any", "any")))

        subscriptionsManager.currentPurchaseState.test {
            assertTrue(awaitItem() is CurrentPurchase.InProgress)
            assertTrue(awaitItem() is CurrentPurchase.Success)

            verify(pixelSender).reportPurchaseSuccess()
            verify(pixelSender).reportSubscriptionActivated()
            verifyNoMoreInteractions(pixelSender)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `purchase - subscription restored - pixel sent`() = runTest {
        givenUserIsNotAuthenticated()
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenSubscriptionSucceedsWithEntitlements()
        givenAccessTokenSucceeds()

        subscriptionsManager.currentPurchaseState.test {
            subscriptionsManager.purchase(mock(), planId = "")
            assertTrue(awaitItem() is CurrentPurchase.PreFlowInProgress)
            assertTrue(awaitItem() is CurrentPurchase.Recovered)

            verify(pixelSender).reportRestoreAfterPurchaseAttemptSuccess()
            verify(pixelSender).reportSubscriptionActivated()
            verifyNoMoreInteractions(pixelSender)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `currentPurchaseState - purchase fails - pixel is sent`() = runTest {
        givenUserIsAuthenticated()
        givenValidateTokenFails("failure")
        givenConfirmPurchaseFails()

        whenever(playBillingManager.purchaseState).thenReturn(flowOf(PurchaseState.Purchased("validateToken", "packageName")))

        subscriptionsManager.currentPurchaseState.test {
            assertTrue(awaitItem() is CurrentPurchase.InProgress)
            assertTrue(awaitItem() is CurrentPurchase.Waiting)
            assertEquals(WAITING.statusName, authDataStore.status)
            verify(pixelSender).reportPurchaseFailureBackend()
            verifyNoMoreInteractions(pixelSender)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `purchase - create account fails - pixel is sent`() = runTest {
        givenUserIsNotAuthenticated()
        givenCreateAccountFails()

        subscriptionsManager.currentPurchaseState.test {
            subscriptionsManager.purchase(mock(), planId = "")
            assertTrue(awaitItem() is CurrentPurchase.PreFlowInProgress)
            assertTrue(awaitItem() is CurrentPurchase.Failure)

            verify(pixelSender).reportPurchaseFailureAccountCreation()
            verify(pixelSender).reportPurchaseFailureOther()
            verifyNoMoreInteractions(pixelSender)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getSubscriptionOffer - return value`() = runTest {
        val productDetails: ProductDetails = mock { productDetails ->
            whenever(productDetails.productId).thenReturn(SubscriptionsConstants.BASIC_SUBSCRIPTION)

            val pricingPhaseList: List<PricingPhase> = listOf(
                mock { pricingPhase ->
                    whenever(pricingPhase.formattedPrice).thenReturn("1$")
                },
            )

            val pricingPhases: PricingPhases = mock { pricingPhases ->
                whenever(pricingPhases.pricingPhaseList).thenReturn(pricingPhaseList)
            }

            val monthlyOffer: ProductDetails.SubscriptionOfferDetails = mock { offer ->
                whenever(offer.basePlanId).thenReturn(MONTHLY_PLAN)
                whenever(offer.pricingPhases).thenReturn(pricingPhases)
            }

            val yearlyOffer: ProductDetails.SubscriptionOfferDetails = mock { offer ->
                whenever(offer.basePlanId).thenReturn(YEARLY_PLAN)
                whenever(offer.pricingPhases).thenReturn(pricingPhases)
            }

            whenever(productDetails.subscriptionOfferDetails).thenReturn(listOf(monthlyOffer, yearlyOffer))
        }

        whenever(playBillingManager.products).thenReturn(listOf(productDetails))

        val subscriptionOffer = subscriptionsManager.getSubscriptionOffer()!!

        with(subscriptionOffer) {
            assertEquals(MONTHLY_PLAN, monthlyPlanId)
            assertEquals("1$", monthlyFormattedPrice)
            assertEquals(YEARLY_PLAN, yearlyPlanId)
            assertEquals("1$", yearlyFormattedPrice)
        }
    }

    @Test
    fun `canSupportEncryption - return true`() = runTest {
        assertTrue(subscriptionsManager.canSupportEncryption())
    }

    @Test
    fun `canSupportEncryption - cannot support - return false`() = runTest {
        val authDataStore: SubscriptionsDataStore = FakeSubscriptionsDataStore(supportEncryption = false)
        val authRepository = RealAuthRepository(authDataStore, coroutineRule.testDispatcherProvider)
        subscriptionsManager = RealSubscriptionsManager(
            authService,
            subscriptionsService,
            authRepository,
            playBillingManager,
            emailManager,
            context,
            TestScope(),
            coroutineRule.testDispatcherProvider,
            pixelSender,
        )

        assertFalse(subscriptionsManager.canSupportEncryption())
    }

    @Test
    fun `deleteAccount - user authenticated and valid token - return true`() = runTest {
        givenUserIsAuthenticated()
        givenValidateTokenSucceedsWithEntitlements()
        givenDeleteAccountSucceeds()

        assertTrue(subscriptionsManager.deleteAccount())
    }

    @Test
    fun `deleteAccount - user not authenticated - return false`() = runTest {
        givenUserIsNotAuthenticated()
        givenDeleteAccountFails()

        assertFalse(subscriptionsManager.deleteAccount())
    }

    @Test
    fun `deleteAccount - user authenticated with subscription, token expired, entitlements exist - return true`() = runTest {
        authDataStore.externalId = "1234"
        givenUserIsAuthenticated()
        givenSubscriptionSucceedsWithEntitlements()
        givenValidateTokenFailsAndThenSucceeds("""{ "error": "expired_token" }""")
        givenPurchaseStored()
        givenStoreLoginSucceeds()
        givenAccessTokenSucceeds()
        givenDeleteAccountSucceeds()

        assertTrue(subscriptionsManager.deleteAccount())
    }

    @Test
    fun `removeEntitlements - entitlements deleted`() = runTest {
        givenSubscriptionExists()
        assertEquals("""[{"product":"product", "name":"name"}]""", authDataStore.entitlements)

        subscriptionsManager.removeEntitlements()
        assertEquals("""[]""", authDataStore.entitlements)
    }

    private suspend fun givenDeleteAccountSucceeds() {
        whenever(authService.delete(any())).thenReturn(DeleteAccountResponse("deleted"))
    }

    private suspend fun givenDeleteAccountFails() {
        val exception = "failure".toResponseBody("text/json".toMediaTypeOrNull())
        whenever(authService.delete(any())).thenThrow(HttpException(Response.error<String>(400, exception)))
    }

    private suspend fun givenUrlPortalSucceeds() {
        whenever(subscriptionsService.portal(any())).thenReturn(PortalResponse("example.com"))
    }

    private suspend fun givenUrlPortalFails() {
        val exception = "failure".toResponseBody("text/json".toMediaTypeOrNull())
        whenever(subscriptionsService.portal(any())).thenThrow(HttpException(Response.error<String>(400, exception)))
    }

    private suspend fun givenSubscriptionFails() {
        val exception = "failure".toResponseBody("text/json".toMediaTypeOrNull())
        whenever(subscriptionsService.subscription(any())).thenThrow(HttpException(Response.error<String>(400, exception)))
    }

    private suspend fun givenSubscriptionSucceedsWithoutEntitlements(status: String = "Auto-Renewable") {
        givenValidateTokenSucceedsNoEntitlements()
        whenever(subscriptionsService.subscription(any())).thenReturn(
            SubscriptionResponse(
                productId = MONTHLY_PLAN,
                startedAt = 1234,
                expiresOrRenewsAt = 1234,
                platform = "android",
                status = status,
            ),
        )
    }

    private suspend fun givenSubscriptionSucceedsWithEntitlements(status: String = "Auto-Renewable") {
        givenValidateTokenSucceedsWithEntitlements()
        whenever(subscriptionsService.subscription(any())).thenReturn(
            SubscriptionResponse(
                productId = MONTHLY_PLAN,
                startedAt = 1234,
                expiresOrRenewsAt = 1234,
                platform = "android",
                status = status,
            ),
        )
    }

    private fun givenUserIsNotAuthenticated() {
        authDataStore.accessToken = null
        authDataStore.authToken = null
    }

    private fun givenUserIsAuthenticated() {
        authDataStore.accessToken = "accessToken"
        authDataStore.authToken = "authToken"
    }

    private suspend fun givenCreateAccountFails() {
        val exception = "account_failure".toResponseBody("text/json".toMediaTypeOrNull())
        whenever(authService.createAccount(any())).thenThrow(HttpException(Response.error<String>(400, exception)))
    }

    private suspend fun givenCreateAccountSucceeds() {
        whenever(authService.createAccount(any())).thenReturn(
            CreateAccountResponse(
                authToken = "authToken",
                externalId = "1234",
                status = "ok",
            ),
        )
    }

    private fun givenSubscriptionExists(status: SubscriptionStatus = AUTO_RENEWABLE) {
        authDataStore.platform = "google"
        authDataStore.productId = "productId"
        authDataStore.entitlements = """[{"product":"product", "name":"name"}]"""
        authDataStore.status = status.statusName
        authDataStore.startedAt = 1000L
        authDataStore.expiresOrRenewsAt = 1000L
    }

    private suspend fun givenValidateTokenFailsAndThenSucceeds(failure: String) {
        val exception = failure.toResponseBody("text/json".toMediaTypeOrNull())
        whenever(authService.validateToken(any()))
            .thenThrow(HttpException(Response.error<String>(400, exception)))
            .thenReturn(
                ValidateTokenResponse(
                    account = AccountResponse(
                        email = "accessToken",
                        externalId = "1234",
                        entitlements = listOf(
                            EntitlementResponse("id", "name", "testProduct"),
                        ),
                    ),
                ),
            )
    }

    private suspend fun givenValidateTokenFailsAndThenSucceedsWithNoEntitlements(failure: String) {
        val exception = failure.toResponseBody("text/json".toMediaTypeOrNull())
        whenever(authService.validateToken(any()))
            .thenThrow(HttpException(Response.error<String>(400, exception)))
            .thenReturn(
                ValidateTokenResponse(
                    account = AccountResponse(
                        email = "accessToken",
                        externalId = "1234",
                        entitlements = listOf(),
                    ),
                ),
            )
    }

    private suspend fun givenStoreLoginFails() {
        val exception = "failure".toResponseBody("text/json".toMediaTypeOrNull())
        whenever(authService.storeLogin(any())).thenThrow(HttpException(Response.error<String>(400, exception)))
    }

    private suspend fun givenValidateTokenSucceedsWithEntitlements() {
        whenever(authService.validateToken(any())).thenReturn(
            ValidateTokenResponse(
                account = AccountResponse(
                    email = "email",
                    externalId = "1234",
                    entitlements = listOf(
                        EntitlementResponse("id", NetP.value, NetP.value),
                    ),
                ),
            ),
        )
    }

    private suspend fun givenValidateTokenSucceedsNoEntitlements() {
        whenever(authService.validateToken(any())).thenReturn(
            ValidateTokenResponse(
                account = AccountResponse(
                    email = "accessToken",
                    externalId = "1234",
                    entitlements = emptyList(),
                ),
            ),
        )
    }

    private suspend fun givenValidateTokenFails(failure: String) {
        val exception = failure.toResponseBody("text/json".toMediaTypeOrNull())
        whenever(authService.validateToken(any())).thenThrow(HttpException(Response.error<String>(400, exception)))
    }

    private fun givenPurchaseStored() {
        val purchaseRecord = PurchaseHistoryRecord(
            """
        {"purchaseToken": "validToken", "productId": "test", "purchaseTime":1, "quantity":1}
        """,
            "signature",
        )
        whenever(playBillingManager.products).thenReturn(emptyList())
        whenever(playBillingManager.purchaseHistory).thenReturn(listOf(purchaseRecord))
    }

    private suspend fun givenStoreLoginSucceeds() {
        whenever(authService.storeLogin(any())).thenReturn(
            StoreLoginResponse(
                authToken = "authToken",
                externalId = "1234",
                email = "test@duck.com",
                status = "ok",
            ),
        )
    }

    private suspend fun givenAccessTokenSucceeds() {
        whenever(authService.accessToken(any())).thenReturn(AccessTokenResponse("accessToken"))
    }

    private suspend fun givenAccessTokenFails() {
        val exception = "account_failure".toResponseBody("text/json".toMediaTypeOrNull())
        whenever(authService.accessToken(any())).thenThrow(HttpException(Response.error<String>(400, exception)))
    }

    private suspend fun givenConfirmPurchaseFails() {
        val exception = "account_failure".toResponseBody("text/json".toMediaTypeOrNull())
        whenever(subscriptionsService.confirm(any(), any())).thenThrow(HttpException(Response.error<String>(400, exception)))
    }

    private suspend fun givenConfirmPurchaseSucceeds() {
        whenever(subscriptionsService.confirm(any(), any())).thenReturn(
            ConfirmationResponse(
                email = "test@duck.com",
                entitlements = listOf(
                    ConfirmationEntitlement(NetP.value, NetP.value),
                ),
                subscription = SubscriptionResponse(
                    productId = "id",
                    platform = "google",
                    status = "Auto-Renewable",
                    startedAt = 1000000L,
                    expiresOrRenewsAt = 1000000L,
                ),
            ),
        )
    }
}
