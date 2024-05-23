package com.duckduckgo.autofill.impl.ui.credential.management.survey

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutofillSurveyStoreImplTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val testee = AutofillSurveyStoreImpl(
        context = context,
        dispatchers = coroutineTestRule.testDispatcherProvider,
    )

    @Test
    fun `hasSurveyBeenTaken - survey id never recorded before - returns false`() = runTest {
        assertFalse(testee.hasSurveyBeenTaken("surveyId-1"))
    }

    @Test
    fun `hasSurveyBeenTaken - another survey id recorded - returns false`() = runTest {
        testee.recordSurveyWasShown("surveyId-1")
        assertFalse(testee.hasSurveyBeenTaken("surveyId-2"))
    }

    @Test
    fun `hasSurveyBeenTaken - survey recorded before - returns true`() = runTest {
        testee.recordSurveyWasShown("surveyId-1")
        assertTrue(testee.hasSurveyBeenTaken("surveyId-1"))
    }

    @Test
    fun `hasSurveyBeenTaken - multiple surveys recorded - returns true`() = runTest {
        testee.recordSurveyWasShown("surveyId-1")
        testee.recordSurveyWasShown("surveyId-2")
        testee.recordSurveyWasShown("surveyId-3")
        assertTrue(testee.hasSurveyBeenTaken("surveyId-2"))
    }
}
