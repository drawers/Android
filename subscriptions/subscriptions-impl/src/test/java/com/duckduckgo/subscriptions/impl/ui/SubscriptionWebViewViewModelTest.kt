package com.duckduckgo.subscriptions.impl.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.feature.toggles.api.FakeFeatureToggleFactory
import com.duckduckgo.feature.toggles.api.FakeToggleStore
import com.duckduckgo.feature.toggles.api.Toggle
import com.duckduckgo.networkprotection.api.NetworkProtectionAccessState
import com.duckduckgo.networkprotection.api.NetworkProtectionScreens.NetworkProtectionManagementScreenNoParams
import com.duckduckgo.subscriptions.api.SubscriptionStatus.AUTO_RENEWABLE
import com.duckduckgo.subscriptions.api.SubscriptionStatus.EXPIRED
import com.duckduckgo.subscriptions.api.SubscriptionStatus.INACTIVE
import com.duckduckgo.subscriptions.impl.CurrentPurchase
import com.duckduckgo.subscriptions.impl.JSONObjectAdapter
import com.duckduckgo.subscriptions.impl.PrivacyProFeature
import com.duckduckgo.subscriptions.impl.SubscriptionOffer
import com.duckduckgo.subscriptions.impl.SubscriptionsChecker
import com.duckduckgo.subscriptions.impl.SubscriptionsConstants
import com.duckduckgo.subscriptions.impl.SubscriptionsManager
import com.duckduckgo.subscriptions.impl.pixels.SubscriptionPixelSender
import com.duckduckgo.subscriptions.impl.ui.SubscriptionWebViewViewModel.Command
import com.duckduckgo.subscriptions.impl.ui.SubscriptionWebViewViewModel.Companion
import com.duckduckgo.subscriptions.impl.ui.SubscriptionWebViewViewModel.PurchaseStateView
import com.duckduckgo.subscriptions.impl.ui.SubscriptionWebViewViewModel.PurchaseStateView.Success
import com.duckduckgo.subscriptions.impl.ui.SubscriptionWebViewViewModel.SubscriptionOptionsJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SubscriptionWebViewViewModelTest {
    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val moshi = Moshi.Builder().add(JSONObjectAdapter()).build()
    private val jsonAdapter: JsonAdapter<SubscriptionOptionsJson> = moshi.adapter(SubscriptionOptionsJson::class.java)
    private val subscriptionsManager: SubscriptionsManager = mock()
    private val networkProtectionAccessState: NetworkProtectionAccessState = mock()
    private val subscriptionsChecker: SubscriptionsChecker = mock()
    private val pixelSender: SubscriptionPixelSender = mock()
    private val privacyProFeature = FakeFeatureToggleFactory.create(PrivacyProFeature::class.java, FakeToggleStore())

    private lateinit var viewModel: SubscriptionWebViewViewModel

    @Before
    fun setup() = runTest {
        whenever(networkProtectionAccessState.getScreenForCurrentState()).thenReturn(NetworkProtectionManagementScreenNoParams)
        viewModel = SubscriptionWebViewViewModel(
            coroutineTestRule.testDispatcherProvider,
            subscriptionsManager,
            subscriptionsChecker,
            networkProtectionAccessState,
            pixelSender,
            privacyProFeature,
        )
    }

    @Test
    fun `whenCurrentPurchaseStateChangesThenReturnCorrectState - view state updated`() = runTest {
        val flowTest: MutableSharedFlow<CurrentPurchase> = MutableSharedFlow()
        whenever(subscriptionsManager.currentPurchaseState).thenReturn(flowTest)
        viewModel.start()

        viewModel.currentPurchaseViewState.test {
            assertTrue(awaitItem().purchaseState is PurchaseStateView.Inactive)
            flowTest.emit(CurrentPurchase.Failure("test"))
            assertTrue(awaitItem().purchaseState is PurchaseStateView.Failure)

            flowTest.emit(CurrentPurchase.Success)
            val success = awaitItem().purchaseState
            assertTrue(success is Success)
            assertEquals(Companion.PURCHASE_COMPLETED_FEATURE_NAME, (success as Success).subscriptionEventData.featureName)
            assertEquals(Companion.PURCHASE_COMPLETED_SUBSCRIPTION_NAME, success.subscriptionEventData.subscriptionName)
            assertNotNull(success.subscriptionEventData.params)
            assertEquals("completed", success.subscriptionEventData.params!!.getString("type"))

            flowTest.emit(CurrentPurchase.InProgress)
            assertTrue(awaitItem().purchaseState is PurchaseStateView.InProgress)

            flowTest.emit(CurrentPurchase.Recovered)
            assertTrue(awaitItem().purchaseState is PurchaseStateView.Recovered)

            flowTest.emit(CurrentPurchase.PreFlowInProgress)
            assertTrue(awaitItem().purchaseState is PurchaseStateView.InProgress)

            flowTest.emit(CurrentPurchase.PreFlowFinished)
            assertTrue(awaitItem().purchaseState is PurchaseStateView.Inactive)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `start - purchase state failed - send canceled message`() = runTest {
        val flowTest: MutableSharedFlow<CurrentPurchase> = MutableSharedFlow()
        whenever(subscriptionsManager.currentPurchaseState).thenReturn(flowTest)
        viewModel.start()

        viewModel.commands().test {
            flowTest.emit(CurrentPurchase.Failure("test"))

            val result = awaitItem()
            assertTrue(result is Command.SendJsEvent)
            assertEquals("{\"type\":\"canceled\"}", (result as Command.SendJsEvent).event.params.toString())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `start - purchase state canceled - send canceled message`() = runTest {
        val flowTest: MutableSharedFlow<CurrentPurchase> = MutableSharedFlow()
        whenever(subscriptionsManager.currentPurchaseState).thenReturn(flowTest)
        viewModel.start()

        viewModel.commands().test {
            flowTest.emit(CurrentPurchase.Canceled)

            val result = awaitItem()
            assertTrue(result is Command.SendJsEvent)
            assertEquals("{\"type\":\"canceled\"}", (result as Command.SendJsEvent).event.params.toString())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `processJsCallbackMessage - subscription selected and id in object empty - return failure`() = runTest {
        val json = """
            {"id":""}
        """.trimIndent()

        viewModel.currentPurchaseViewState.test {
            assertTrue(awaitItem().purchaseState is PurchaseStateView.Inactive)
            viewModel.processJsCallbackMessage("test", "subscriptionSelected", "id", JSONObject(json))
            assertTrue(awaitItem().purchaseState is PurchaseStateView.Failure)
        }
    }

    @Test
    fun `processJsCallbackMessage - subscription selected and id is in object null - return failure`() = runTest {
        viewModel.currentPurchaseViewState.test {
            assertTrue(awaitItem().purchaseState is PurchaseStateView.Inactive)
            viewModel.processJsCallbackMessage("test", "subscriptionSelected", "id", JSONObject("{}"))
            assertTrue(awaitItem().purchaseState is PurchaseStateView.Failure)
        }
    }

    @Test
    fun `processJsCallbackMessage - subscription selected - send command with correct id`() = runTest {
        val json = """
            {"id":"myId"}
        """.trimIndent()
        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "subscriptionSelected", "id", JSONObject(json))
            val result = awaitItem()
            assertTrue(result is Command.SubscriptionSelected)
            assertEquals("myId", (result as Command.SubscriptionSelected).id)
        }
    }

    @Test
    fun `processJsCallbackMessage - back to settings - command sent`() = runTest {
        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "backToSettings", "id", JSONObject("{}"))
            assertTrue(awaitItem() is Command.BackToSettings)
        }
    }

    @Test
    fun `processJsCallbackMessage - back to settings activate success - command sent`() = runTest {
        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "backToSettingsActivateSuccess", "id", JSONObject("{}"))
            assertTrue(awaitItem() is Command.BackToSettingsActivateSuccess)
        }
    }

    @Test
    fun `whenGetSubscriptionOptions - send command`() = runTest {
        privacyProFeature.allowPurchase().setEnabled(Toggle.State(enable = true))

        whenever(subscriptionsManager.getSubscriptionOffer()).thenReturn(
            SubscriptionOffer(
                monthlyPlanId = "monthly",
                monthlyFormattedPrice = "$1",
                yearlyPlanId = "yearly",
                yearlyFormattedPrice = "$10",
            ),
        )

        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "getSubscriptionOptions", "id", JSONObject("{}"))
            val result = awaitItem()
            assertTrue(result is Command.SendResponseToJs)
            val response = (result as Command.SendResponseToJs).data

            val params = jsonAdapter.fromJson(response.params.toString())
            assertEquals("id", response.id)
            assertEquals("test", response.featureName)
            assertEquals("getSubscriptionOptions", response.method)
            assertEquals("yearly", params?.options?.first()?.id)
            assertEquals("monthly", params?.options?.last()?.id)
        }
    }

    @Test
    fun `whenGetSubscriptionsAndNoSubscriptionOffer - send command with empty data`() = runTest {
        privacyProFeature.allowPurchase().setEnabled(Toggle.State(enable = true))

        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "getSubscriptionOptions", "id", JSONObject("{}"))

            val result = awaitItem()
            assertTrue(result is Command.SendResponseToJs)

            val response = (result as Command.SendResponseToJs).data
            assertEquals("id", response.id)
            assertEquals("test", response.featureName)
            assertEquals("getSubscriptionOptions", response.method)

            val params = jsonAdapter.fromJson(response.params.toString())!!
            assertEquals(0, params.options.size)
            assertEquals(0, params.features.size)
        }
    }

    @Test
    fun `whenGetSubscriptionsAndToggleOffThenSendCommandWithEmptyData - send command with empty data`() = runTest {
        privacyProFeature.allowPurchase().setEnabled(Toggle.State(enable = false))
        whenever(subscriptionsManager.getSubscriptionOffer()).thenReturn(
            SubscriptionOffer(
                monthlyPlanId = "monthly",
                monthlyFormattedPrice = "$1",
                yearlyPlanId = "yearly",
                yearlyFormattedPrice = "$10",
            ),
        )

        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "getSubscriptionOptions", "id", JSONObject("{}"))

            val result = awaitItem()
            assertTrue(result is Command.SendResponseToJs)

            val response = (result as Command.SendResponseToJs).data
            assertEquals("id", response.id)
            assertEquals("test", response.featureName)
            assertEquals("getSubscriptionOptions", response.method)

            val params = jsonAdapter.fromJson(response.params.toString())!!
            assertEquals(0, params.options.size)
            assertEquals(0, params.features.size)
        }
    }

    @Test
    fun `commands - activate subscription and subscription active - no command sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)
        viewModel.commands().test {
            expectNoEvents()
        }
    }

    @Test
    fun `processJsCallbackMessage - activate subscription and subscription inactive - command sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(INACTIVE)
        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "activateSubscription", null, null)
            assertTrue(awaitItem() is Command.RestoreSubscription)
        }
    }

    @Test
    fun `processJsCallbackMessage - set subscription and expired subscription - command not sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(EXPIRED)
        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "setSubscription", null, null)
            assertTrue(awaitItem() is Command.SubscriptionRecoveredExpired)
        }
    }

    @Test
    fun `processJsCallbackMessage - set subscription and active subscription - command not sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)
        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "setSubscription", null, null)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `processJsCallbackMessage - feature selected and no data - command not sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(EXPIRED)
        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "featureSelected", null, null)
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `processJsCallbackMessage - feature selected and invalid data - command not sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(EXPIRED)
        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "featureSelected", null, JSONObject("{}"))
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `processJsCallbackMessage - feature selected and invalid feature - command not sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(EXPIRED)
        viewModel.commands().test {
            viewModel.processJsCallbackMessage("test", "featureSelected", null, JSONObject("""{"feature":"test"}"""))
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `processJsCallbackMessage - feature selected and feature is net p - command sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(EXPIRED)
        viewModel.commands().test {
            viewModel.processJsCallbackMessage(
                "test",
                "featureSelected",
                null,
                JSONObject("""{"feature":"${SubscriptionsConstants.NETP}"}"""),
            )
            assertTrue(awaitItem() is Command.GoToNetP)
        }
    }

    @Test
    fun `processJsCallbackMessage - feature selected and feature is ITR - command sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(EXPIRED)
        viewModel.commands().test {
            viewModel.processJsCallbackMessage(
                "test",
                "featureSelected",
                null,
                JSONObject("""{"feature":"${SubscriptionsConstants.ITR}"}"""),
            )
            assertTrue(awaitItem() is Command.GoToITR)
        }
    }

    @Test
    fun `processJsCallbackMessage - feature selected and feature is PIR - command sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(EXPIRED)
        viewModel.commands().test {
            viewModel.processJsCallbackMessage(
                "test",
                "featureSelected",
                null,
                JSONObject("""{"feature":"${SubscriptionsConstants.PIR}"}"""),
            )
            assertTrue(awaitItem() is Command.GoToPIR)
        }
    }

    @Test
    fun `processJsCallbackMessage - subscription selected - pixel sent`() = runTest {
        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "subscriptionSelected",
            id = "id",
            data = JSONObject("""{"id":"myId"}"""),
        )
        verify(pixelSender).reportOfferSubscribeClick()
    }

    @Test
    fun `processJsCallbackMessage - restore purchase clicked - pixel sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(EXPIRED)
        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "activateSubscription",
            id = null,
            data = null,
        )
        verify(pixelSender).reportOfferRestorePurchaseClick()
    }

    @Test
    fun `start - add email clicked and in purchase flow - pixel sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)
        whenever(subscriptionsManager.currentPurchaseState).thenReturn(flowOf(CurrentPurchase.Success))
        viewModel.start()

        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "subscriptionsWelcomeAddEmailClicked",
            id = null,
            data = null,
        )
        verify(pixelSender).reportOnboardingAddDeviceClick()
    }

    @Test
    fun `processJsCallbackMessage - add email clicked and not in purchase flow - pixel is not sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)

        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "subscriptionsWelcomeAddEmailClicked",
            id = null,
            data = null,
        )
        verifyNoInteractions(pixelSender)
    }

    @Test
    fun `start - feature selected and in purchase flow - pixel sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)
        whenever(subscriptionsManager.currentPurchaseState).thenReturn(flowOf(CurrentPurchase.Success))
        viewModel.start()

        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "featureSelected",
            id = null,
            data = JSONObject("""{"feature":"${SubscriptionsConstants.NETP}"}"""),
        )
        verify(pixelSender).reportOnboardingVpnClick()
    }

    @Test
    fun `processJsCallbackMessage - feature selected and not in purchase flow - pixel is not sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)

        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "featureSelected",
            id = null,
            data = JSONObject("""{"feature":"${SubscriptionsConstants.NETP}"}"""),
        )
        verifyNoInteractions(pixelSender)
    }

    @Test
    fun whenFeatureSelectedAndFeatureIsItrAndInPurchaseFlowThenPixelIsSent() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)
        whenever(subscriptionsManager.currentPurchaseState).thenReturn(flowOf(CurrentPurchase.Success))
        viewModel.start()

        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "featureSelected",
            id = null,
            data = JSONObject("""{"feature":"${SubscriptionsConstants.ITR}"}"""),
        )
        verify(pixelSender).reportOnboardingIdtrClick()
    }

    @Test
    fun whenFeatureSelectedAndFeatureIsItrAndNotInPurchaseFlowThenPixelIsNotSent() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)

        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "featureSelected",
            id = null,
            data = JSONObject("""{"feature":"${SubscriptionsConstants.ITR}"}"""),
        )
        verifyNoInteractions(pixelSender)
    }

    @Test
    fun `start - feature selected and pir in purchase flow - pixel sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)
        whenever(subscriptionsManager.currentPurchaseState).thenReturn(flowOf(CurrentPurchase.Success))
        viewModel.start()

        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "featureSelected",
            id = null,
            data = JSONObject("""{"feature":"${SubscriptionsConstants.PIR}"}"""),
        )
        verify(pixelSender).reportOnboardingPirClick()
    }

    @Test
    fun whenFeatureSelectedAndFeatureIsPirAndNotInPurchaseFlowThenPixelIsNotSent() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)

        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "featureSelected",
            id = null,
            data = JSONObject("""{"feature":"${SubscriptionsConstants.PIR}"}"""),
        )
        verifyNoInteractions(pixelSender)
    }

    @Test
    fun `start - subscriptions welcome faq clicked and in purchase flow - pixel is sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)
        whenever(subscriptionsManager.currentPurchaseState).thenReturn(flowOf(CurrentPurchase.Success))
        viewModel.start()

        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "subscriptionsWelcomeFaqClicked",
            id = null,
            data = null,
        )
        verify(pixelSender).reportOnboardingFaqClick()
    }

    @Test
    fun `processJsCallbackMessage - subscriptions welcome faq clicked and not in purchase flow - pixel is not sent`() = runTest {
        whenever(subscriptionsManager.subscriptionStatus()).thenReturn(AUTO_RENEWABLE)

        viewModel.processJsCallbackMessage(
            featureName = "test",
            method = "subscriptionsWelcomeFaqClicked",
            id = null,
            data = null,
        )
        verifyNoInteractions(pixelSender)
    }
}
