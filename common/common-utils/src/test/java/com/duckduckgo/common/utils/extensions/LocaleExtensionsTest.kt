package com.duckduckgo.common.utils.extensions

import java.util.Locale
import junit.framework.TestCase.assertEquals
import org.junit.Test

class LocaleExtensionsTest {

    @Test
    fun `toSanitizedLanguageTag - has unicode locale extension - removes unicode locale extension`() {
        val locale = Locale.Builder()
            .setLanguage("en")
            .setRegion("US")
            .setExtension(Locale.UNICODE_LOCALE_EXTENSION, "test")
            .build()

        assertEquals("en-US-u-test", locale.toLanguageTag())
        assertEquals("en-US", locale.toSanitizedLanguageTag())
    }

    @Test
    fun `toSanitizedLanguageTag - does not have unicode locale extension - unchanged`() {
        val locale = Locale.Builder()
            .setLanguage("en")
            .setRegion("US")
            .build()

        assertEquals("en-US", locale.toLanguageTag())
        assertEquals("en-US", locale.toSanitizedLanguageTag())
    }
}
