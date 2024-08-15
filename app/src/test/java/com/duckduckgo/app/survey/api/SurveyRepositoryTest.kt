package com.duckduckgo.app.survey.api

import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.duckduckgo.app.survey.db.SurveyDao
import com.duckduckgo.app.survey.model.Survey
import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.browser.api.UserBrowserProperties
import java.util.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class SurveyRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var surveyDao: SurveyDao
    private lateinit var notificationManager: NotificationManagerCompat
    private val userBrowserProperties: UserBrowserProperties = mock()
    private val appBuildConfig: AppBuildConfig = mock()

    private lateinit var surveyRepository: SurveyRepository

    @Before
    fun setup() {
        surveyDao = FakeSurveyDao()
        notificationManager = NotificationManagerCompat.from(context)
        whenever(appBuildConfig.deviceLocale).thenReturn(Locale.US)

        surveyRepository = SurveyRepositoryImpl(surveyDao, userBrowserProperties, notificationManager, appBuildConfig)
    }

    @Test
    fun `isUserEligibleForSurvey - survey URL is null - not eligible`() {
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = null,
            url = null,
        )

        assertFalse(surveyRepository.isUserEligibleForSurvey(survey))
    }

    @Test
    fun `isUserEligibleForSurvey - locale not allowed - not eligible`() {
        whenever(appBuildConfig.deviceLocale).thenReturn(Locale.JAPAN)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = null,
            url = "https://survey.com",
        )

        assertFalse(surveyRepository.isUserEligibleForSurvey(survey))
    }

    @Test
    fun `isUserEligibleForSurvey - required days installed null - not eligible`() {
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = null,
            url = "https://survey.com",
        )

        assertFalse(surveyRepository.isUserEligibleForSurvey(survey))
    }

    @Test
    fun `isUserEligibleForSurvey - required days installed minus one - survey is eligible`() {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(0)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = -1,
            url = "https://survey.com",
        )

        assertTrue(surveyRepository.isUserEligibleForSurvey(survey))
    }

    @Test
    fun `isUserEligibleForSurvey - retention day below required days - user is eligible`() {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(0)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = 1,
            url = "https://survey.com",
        )

        assertTrue(surveyRepository.isUserEligibleForSurvey(survey))
    }

    @Test
    fun `isUserEligibleForSurvey - locale not allowed and required days installed met - not eligible`() {
        whenever(appBuildConfig.deviceLocale).thenReturn(Locale.JAPAN)
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(0)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = -1,
            url = "https://survey.com",
        )

        assertFalse(surveyRepository.isUserEligibleForSurvey(survey))
    }

    @Test
    fun `isUserEligibleForSurvey - locale is Canada and required days installed met - survey is eligible`() {
        whenever(appBuildConfig.deviceLocale).thenReturn(Locale.CANADA)
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(0)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = -1,
            url = "https://survey.com",
        )

        assertTrue(surveyRepository.isUserEligibleForSurvey(survey))
    }

    @Test
    fun `isUserEligibleForSurvey - UK locale and required days installed met - survey is eligible`() {
        whenever(appBuildConfig.deviceLocale).thenReturn(Locale.UK)
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(0)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = -1,
            url = "https://survey.com",
        )

        assertTrue(surveyRepository.isUserEligibleForSurvey(survey))
    }

    @Test
    fun `remainingDaysForShowingSurvey - required days installed is null and retention days below default - return minus 1`() {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(4)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = null,
            url = null,
        )

        assertEquals(-1, surveyRepository.remainingDaysForShowingSurvey(survey))
        assertFalse(surveyRepository.shouldShowSurvey(survey))
    }

    @Test
    fun `remainingDaysForShowingSurvey - required days installed is null and retention days above default - return 0`() {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(31)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = null,
            url = null,
        )

        assertEquals(0, surveyRepository.remainingDaysForShowingSurvey(survey))
        assertTrue(surveyRepository.shouldShowSurvey(survey))
    }

    @Test
    fun `remainingDaysForShowingSurvey - required days installed is minus 1 and not first day - remaining days is zero`() {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(4)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = -1,
            url = null,
        )

        assertEquals(0, surveyRepository.remainingDaysForShowingSurvey(survey))
        assertTrue(surveyRepository.shouldShowSurvey(survey))
    }

    @Test
    fun `shouldShowSurvey - days installed not enough - should not show survey`() {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(2)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = 4,
            url = null,
        )

        assertEquals(2, surveyRepository.remainingDaysForShowingSurvey(survey))
        assertFalse(surveyRepository.shouldShowSurvey(survey))
    }

    @Test
    fun `shouldShowSurvey - retention days above required - should not show survey`() {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(4)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = 2,
            url = null,
        )

        assertEquals(-2, surveyRepository.remainingDaysForShowingSurvey(survey))
        assertFalse(surveyRepository.shouldShowSurvey(survey))
    }

    @Test
    fun `shouldShowSurvey - survey status is done - should not show survey`() {
        whenever(userBrowserProperties.daysSinceInstalled()).thenReturn(4)
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.DONE,
            daysInstalled = -1,
            url = null,
        )

        assertFalse(surveyRepository.shouldShowSurvey(survey))
    }

    @Test
    fun `getScheduledSurvey - returns last survey`() {
        val survey = Survey(
            surveyId = "1",
            status = Survey.Status.SCHEDULED,
            daysInstalled = 2,
            url = null,
        )

        surveyDao.insert(survey.copy(surveyId = "1"))
        surveyDao.insert(survey.copy(surveyId = "2"))
        surveyDao.insert(survey.copy(surveyId = "3"))
        surveyDao.insert(survey.copy(surveyId = "5"))

        assertEquals(survey.copy(surveyId = "5"), surveyRepository.getScheduledSurvey())
    }

    @Test
    fun `surveyExists - existing survey - return true`() {
        val survey = Survey(
            surveyId = "id",
            status = Survey.Status.SCHEDULED,
            daysInstalled = 4,
            url = null,
        )

        surveyRepository.persistSurvey(survey)
        assertTrue(surveyRepository.surveyExists("id"))
    }

    @Test
    fun `surveyExists - non-existing survey - return false`() {
        assertFalse(surveyRepository.surveyExists("id"))
    }
}

private class FakeSurveyDao : SurveyDao {
    private val surveys: MutableMap<String, Survey> = linkedMapOf()
    override fun insert(survey: Survey) {
        surveys[survey.surveyId] = survey
    }

    override fun update(survey: Survey) {
        insert(survey)
    }

    override fun get(surveyId: String): Survey? {
        return surveys[surveyId]
    }

    override fun getLiveScheduled(): LiveData<Survey> {
        TODO("Not yet implemented")
    }

    override fun getScheduled(): List<Survey> {
        return surveys.values.filter { it.status == Survey.Status.SCHEDULED }
    }

    override fun deleteUnusedSurveys() {
        surveys.values
            .filter {
                it.status == Survey.Status.SCHEDULED || it.status == Survey.Status.NOT_ALLOCATED
            }.map { it.surveyId }
            .forEach { surveys.remove(it) }
    }
}
