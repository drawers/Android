package com.duckduckgo.subscriptions.impl.repository

import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.subscriptions.api.SubscriptionStatus.AUTO_RENEWABLE
import com.duckduckgo.subscriptions.api.SubscriptionStatus.EXPIRED
import com.duckduckgo.subscriptions.api.SubscriptionStatus.GRACE_PERIOD
import com.duckduckgo.subscriptions.api.SubscriptionStatus.INACTIVE
import com.duckduckgo.subscriptions.api.SubscriptionStatus.NOT_AUTO_RENEWABLE
import com.duckduckgo.subscriptions.api.SubscriptionStatus.UNKNOWN
import com.duckduckgo.subscriptions.api.SubscriptionStatus.WAITING
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RealAuthRepositoryTest {
    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private val authStore = FakeSubscriptionsDataStore()
    private val authRepository: AuthRepository = RealAuthRepository(authStore, coroutineRule.testDispatcherProvider)

    @Test
    fun `isUserAuthenticated - no access token - returns false`() = runTest {
        authStore.authToken = "authToken"
        assertFalse(authRepository.isUserAuthenticated())
    }

    @Test
    fun `isUserAuthenticated - no token - false`() = runTest {
        authStore.accessToken = "accessToken"
        assertFalse(authRepository.isUserAuthenticated())
    }

    @Test
    fun `isUserAuthenticated - no auth token and access token`() = runTest {
        assertFalse(authRepository.isUserAuthenticated())
    }

    @Test
    fun `isUserAuthenticated - authenticated`() = runTest {
        authStore.authToken = "authToken"
        authStore.accessToken = "accessToken"
        assertTrue(authRepository.isUserAuthenticated())
    }

    @Test
    fun `clearAccount - clear data`() = runTest {
        authStore.email = "email@duck.com"
        authStore.externalId = "externalId"
        authStore.authToken = "authToken"
        authStore.accessToken = "accessToken"

        authRepository.clearAccount()

        assertNull(authStore.accessToken)
        assertNull(authStore.authToken)
        assertNull(authStore.externalId)
        assertNull(authStore.email)
    }

    @Test
    fun `clearSubscription - clear data`() = runTest {
        authStore.status = "expired"
        authStore.startedAt = 1000L
        authStore.expiresOrRenewsAt = 1000L
        authStore.platform = "google"
        authStore.productId = "productId"
        authStore.entitlements = "[]"

        authRepository.clearSubscription()

        assertNull(authStore.status)
        assertNull(authStore.startedAt)
        assertNull(authStore.expiresOrRenewsAt)
        assertNull(authStore.platform)
        assertNull(authStore.productId)
        assertNull(authStore.entitlements)
    }

    @Test
    fun `saveAccountData - set data`() = runTest {
        assertNull(authStore.authToken)
        assertNull(authStore.externalId)

        authRepository.saveAccountData(authToken = "authToken", externalId = "externalId")

        assertEquals("authToken", authStore.authToken)
        assertEquals("externalId", authStore.externalId)
    }

    @Test
    fun `getTokens - tokens returned`() = runTest {
        assertNull(authStore.authToken)
        assertNull(authStore.accessToken)

        authStore.accessToken = "accessToken"
        authStore.authToken = "authToken"

        assertEquals("authToken", authRepository.getAuthToken())
        assertEquals("accessToken", authRepository.getAccessToken())
    }

    @Test
    fun `purchaseToWaitingStatus - store waiting`() = runTest {
        authRepository.purchaseToWaitingStatus()
        assertEquals(WAITING.statusName, authStore.status)
    }

    @Test
    fun `getStatus - returns correct status`() = runTest {
        authStore.status = AUTO_RENEWABLE.statusName
        assertEquals(AUTO_RENEWABLE, authRepository.getStatus())
        authStore.status = NOT_AUTO_RENEWABLE.statusName
        assertEquals(NOT_AUTO_RENEWABLE, authRepository.getStatus())
        authStore.status = GRACE_PERIOD.statusName
        assertEquals(GRACE_PERIOD, authRepository.getStatus())
        authStore.status = INACTIVE.statusName
        assertEquals(INACTIVE, authRepository.getStatus())
        authStore.status = EXPIRED.statusName
        assertEquals(EXPIRED, authRepository.getStatus())
        authStore.status = WAITING.statusName
        assertEquals(WAITING, authRepository.getStatus())
        authStore.status = "test"
        assertEquals(UNKNOWN, authRepository.getStatus())
    }

    @Test
    fun `canSupportEncryption - return value`() = runTest {
        assertTrue(authRepository.canSupportEncryption())
    }

    @Test
    fun `canSupportEncryption - cannot support encryption - return false`() = runTest {
        val repository: AuthRepository = RealAuthRepository(
            FakeSubscriptionsDataStore(supportEncryption = false),
            coroutineRule.testDispatcherProvider,
        )
        assertFalse(repository.canSupportEncryption())
    }
}
