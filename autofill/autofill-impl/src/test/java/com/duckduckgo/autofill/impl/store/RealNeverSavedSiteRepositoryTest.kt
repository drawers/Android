package com.duckduckgo.autofill.impl.store

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.autofill.impl.encoding.TestUrlUnicodeNormalizer
import com.duckduckgo.autofill.impl.securestorage.SecureStorage
import com.duckduckgo.autofill.impl.urlmatcher.AutofillDomainNameUrlMatcher
import com.duckduckgo.autofill.impl.urlmatcher.AutofillUrlMatcher
import com.duckduckgo.common.test.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class RealNeverSavedSiteRepositoryTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val urlMatcher: AutofillUrlMatcher = AutofillDomainNameUrlMatcher(TestUrlUnicodeNormalizer())
    private val secureStorage: SecureStorage = mock()

    private val testee: RealNeverSavedSiteRepository = RealNeverSavedSiteRepository(
        autofillUrlMatcher = urlMatcher,
        dispatchers = coroutineTestRule.testDispatcherProvider,
        secureStorage = secureStorage,
    )

    @Test
    fun `addToNeverSaveList - domain added - eTld+1 is used`() = runTest {
        testee.addToNeverSaveList("foo.example.com")
        verify(secureStorage).addToNeverSaveList("example.com")
    }

    @Test
    fun `addToNeverSaveList - domain with query params - only eTld+1 is used`() = runTest {
        testee.addToNeverSaveList("https://foo.example.com/?q=123")
        verify(secureStorage).addToNeverSaveList("example.com")
    }

    @Test
    fun `isInNeverSaveList - domain added - returns true`() = runTest {
        whenever(secureStorage.isInNeverSaveList("example.com")).thenReturn(true)
        assertTrue(testee.isInNeverSaveList("example.com"))
    }

    @Test
    fun `isInNeverSaveList - domain added - uses etld plus one`() = runTest {
        whenever(secureStorage.isInNeverSaveList("example.com")).thenReturn(true)
        assertTrue(testee.isInNeverSaveList("foo.example.com"))
    }
}
