package com.duckduckgo.autofill.impl.ui.credential.management.survey

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.autofill.impl.store.InternalAutofillStore
import com.duckduckgo.autofill.impl.ui.credential.management.survey.AutofillSurvey.SurveyDetails
import com.duckduckgo.common.test.CoroutineTestRule
import java.util.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class AutofillSurveyImplTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val autofillSurveyStore: AutofillSurveyStore = mock()
    private val appBuildConfig: AppBuildConfig = mock()
    private val autofillStore: InternalAutofillStore = mock()
    private val testee: AutofillSurveyImpl = AutofillSurveyImpl(
        statisticsStore = mock(),
        userBrowserProperties = mock(),
        appBuildConfig = appBuildConfig,
        appDaysUsedRepository = mock(),
        dispatchers = coroutineTestRule.testDispatcherProvider,
        autofillSurveyStore = autofillSurveyStore,
        internalAutofillStore = autofillStore,
    )

    @Before
    fun setup() {
        whenever(appBuildConfig.deviceLocale).thenReturn(Locale("en"))

        coroutineTestRule.testScope.runTest {
            configureCredentialCount(0)
        }
    }

    @Test
    fun `firstUnusedSurvey - survey not shown before - returns survey`() = runTest {
        whenever(autofillSurveyStore.hasSurveyBeenTaken("autofill-2024-04-26")).thenReturn(false)
        val survey = testee.firstUnusedSurvey()
        assertEquals("autofill-2024-04-26", survey!!.id)
    }

    @Test
    fun `firstUnusedSurvey - locale not English and survey not shown - does not return survey`() = runTest {
        whenever(appBuildConfig.deviceLocale).thenReturn(Locale("fr"))
        whenever(autofillSurveyStore.hasSurveyBeenTaken("autofill-2024-04-26")).thenReturn(false)
        assertNull(testee.firstUnusedSurvey())
    }

    @Test
    fun `firstUnusedSurvey - survey shown before - does not return`() = runTest {
        whenever(autofillSurveyStore.hasSurveyBeenTaken("autofill-2024-04-26")).thenReturn(true)
        assertNull(testee.firstUnusedSurvey())
    }

    @Test
    fun `recordSurveyAsUsed - survey recorded as used - persisted`() = runTest {
        testee.recordSurveyAsUsed("surveyId-1")
        verify(autofillSurveyStore).recordSurveyWasShown("surveyId-1")
    }

    @Test
    fun `savedPasswordsLowestInNoneBucket - correct query param value added`() = runTest {
        configureCredentialCount(0)
        val survey = getAvailableSurvey()
        val savedPasswordsBucket = survey.url.toUri().getQueryParameter("saved_passwords")
        assertEquals("none", savedPasswordsBucket)
    }

    @Test
    fun `getAvailableSurvey - saved passwords highest in none bucket - correct query param value added`() = runTest {
        configureCredentialCount(2)
        val survey = getAvailableSurvey()
        val savedPasswordsBucket = survey.url.toUri().getQueryParameter("saved_passwords")
        assertEquals("none", savedPasswordsBucket)
    }

    @Test
    fun `getAvailableSurvey - saved passwords lowest in some bucket - correct query param value added`() = runTest {
        configureCredentialCount(3)
        val survey = getAvailableSurvey()
        val savedPasswordsBucket = survey.url.toUri().getQueryParameter("saved_passwords")
        assertEquals("some", savedPasswordsBucket)
    }

    @Test
    fun `savedPasswordsHighestInSomeBucket - correct query param value added`() = runTest {
        configureCredentialCount(9)
        val survey = getAvailableSurvey()
        val savedPasswordsBucket = survey.url.toUri().getQueryParameter("saved_passwords")
        assertEquals("some", savedPasswordsBucket)
    }

    @Test
    fun `savedPasswordsLowestInManyBucket - correct query param value added`() = runTest {
        configureCredentialCount(10)
        val survey = getAvailableSurvey()
        val savedPasswordsBucket = survey.url.toUri().getQueryParameter("saved_passwords")
        assertEquals("many", savedPasswordsBucket)
    }

    @Test
    fun `getAvailableSurvey - saved passwords highest in many bucket - correct query param value added`() = runTest {
        configureCredentialCount(49)
        val survey = getAvailableSurvey()
        val savedPasswordsBucket = survey.url.toUri().getQueryParameter("saved_passwords")
        assertEquals("many", savedPasswordsBucket)
    }

    @Test
    fun `savedPasswordsLowestInLotsBucket - correct query param value added`() = runTest {
        configureCredentialCount(50)
        val survey = getAvailableSurvey()
        val savedPasswordsBucket = survey.url.toUri().getQueryParameter("saved_passwords")
        assertEquals("lots", savedPasswordsBucket)
    }

    @Test
    fun `savedPasswordsIsExtremelyLarge - correct query param value added`() = runTest {
        configureCredentialCount(Int.MAX_VALUE)
        val survey = getAvailableSurvey()
        val savedPasswordsBucket = survey.url.toUri().getQueryParameter("saved_passwords")
        assertEquals("lots", savedPasswordsBucket)
    }

    private suspend fun getAvailableSurvey(): SurveyDetails {
        whenever(autofillSurveyStore.hasSurveyBeenTaken("autofill-2024-04-26")).thenReturn(false)
        return testee.firstUnusedSurvey()!!
    }

    private suspend fun configureCredentialCount(count: Int?) {
        if (count == null) {
            whenever(autofillStore.getCredentialCount()).thenReturn(null)
        } else {
            whenever(autofillStore.getCredentialCount()).thenReturn(flowOf(count))
        }
    }

    /**
     *  passwordsSaved == null -> NUMBER_PASSWORD_BUCKET_NONE
     *             passwordsSaved < 3 -> NUMBER_PASSWORD_BUCKET_NONE
     *             passwordsSaved < 10 -> NUMBER_PASSWORD_BUCKET_SOME
     *             passwordsSaved < 50 -> NUMBER_PASSWORD_BUCKET_MANY
     *             else -> NUMBER_PASSWORD_BUCKET_LOTS
     */
}
