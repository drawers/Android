package com.duckduckgo.app.survey.api

import com.duckduckgo.common.test.api.FakeChain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SurveyEndpointInterceptorTest {

    private lateinit var surveyEndpointDataStore: SurveyEndpointDataStore
    private lateinit var interceptor: SurveyEndpointInterceptor

    @Before
    fun setup() {
        surveyEndpointDataStore = FakeSurveyCustomEnvironmentUrl()
        interceptor = SurveyEndpointInterceptor(surveyEndpointDataStore)
    }

    @Test
    fun `interceptSurveyUrl - enabled - sandbox url`() {
        surveyEndpointDataStore.useSurveyCustomEnvironmentUrl = true

        val chain = FakeChain(SURVEY_URL)
        val response = interceptor.intercept(chain)

        assertEquals(SURVEY_SANDBOX_URL, response.request.url.toString())
    }

    @Test
    fun `intercept - survey url disabled - does not intercept`() {
        surveyEndpointDataStore.useSurveyCustomEnvironmentUrl = false

        val chain = FakeChain(SURVEY_URL)
        val response = interceptor.intercept(chain)

        assertEquals(SURVEY_URL, response.request.url.toString())
    }

    @Test
    fun `intercept - use custom environment url enabled - unknown url`() {
        surveyEndpointDataStore.useSurveyCustomEnvironmentUrl = true

        val chain = FakeChain(UNKNOWN_URL)
        val response = interceptor.intercept(chain)

        assertEquals(UNKNOWN_URL, response.request.url.toString())
    }

    @Test
    fun `intercept - use survey custom environment url disabled - unknown url`() {
        surveyEndpointDataStore.useSurveyCustomEnvironmentUrl = false

        val chain = FakeChain(UNKNOWN_URL)
        val response = interceptor.intercept(chain)

        assertEquals(UNKNOWN_URL, response.request.url.toString())
    }
}

private const val SURVEY_URL = "https://staticcdn.duckduckgo.com/survey/foo/survey.json"
private const val SURVEY_SANDBOX_URL = "https://ddg-sandbox.s3.amazonaws.com/survey/foo/survey.json"
private const val UNKNOWN_URL = "https://unknown.com/survey/foo/survey.json"

private class FakeSurveyCustomEnvironmentUrl(
    override var useSurveyCustomEnvironmentUrl: Boolean = false,
) : SurveyEndpointDataStore
