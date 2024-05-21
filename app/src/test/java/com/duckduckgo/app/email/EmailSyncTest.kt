package com.duckduckgo.app.email

import com.duckduckgo.app.email.db.*
import com.duckduckgo.app.email.sync.*
import com.duckduckgo.app.pixels.*
import com.duckduckgo.app.statistics.pixels.*
import com.duckduckgo.sync.settings.api.SyncSettingsListener
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.mockito.kotlin.*

class EmailSyncTest {

    private val emailDataStoreMock = mock<EmailDataStore>()
    private val syncSettingsListenerMock = mock<SyncSettingsListener>()
    private val pixelMock = mock<Pixel>()

    private val testee = EmailSync(emailDataStoreMock, syncSettingsListenerMock, pixelMock)

    @Test
    fun `getValue - user signed in - account info returned`() {
        whenever(emailDataStoreMock.emailUsername).thenReturn("username")
        whenever(emailDataStoreMock.emailToken).thenReturn("token")

        val value = testee.getValue()

        with(adapter.fromJson(value)!!) {
            assertEquals("username", this.username)
            assertEquals("token", this.personal_access_token)
        }
    }

    @Test
    fun `getValue - user signed out - null`() {
        whenever(emailDataStoreMock.emailUsername).thenReturn(null)
        whenever(emailDataStoreMock.emailToken).thenReturn(null)

        val value = testee.getValue()

        assertNull(value)
    }

    @Test
    fun `save - store credentials - email and token stored`() {
        testee.save("{\"username\":\"email\",\"personal_access_token\":\"token\"}")

        verify(emailDataStoreMock).emailUsername = "email"
        verify(emailDataStoreMock).emailToken = "token"
    }

    @Test
    fun `save - null - logout user`() {
        testee.save(null)

        verify(emailDataStoreMock).emailUsername = ""
        verify(emailDataStoreMock).emailToken = ""
    }

    @Test
    fun `deduplicate - same local address - do nothing`() {
        whenever(emailDataStoreMock.emailUsername).thenReturn("username")
        whenever(emailDataStoreMock.emailToken).thenReturn("token")

        testee.deduplicate("{\"username\":\"email\",\"personal_access_token\":\"token\"}")

        verify(emailDataStoreMock).emailUsername = "email"
        verify(emailDataStoreMock).emailToken = "token"
    }

    @Test
    fun `deduplicate - different local address - remote wins`() {
        whenever(emailDataStoreMock.emailUsername).thenReturn("username2")
        whenever(emailDataStoreMock.emailToken).thenReturn("token2")

        testee.deduplicate("{\"username\":\"email\",\"personal_access_token\":\"token\"}")

        verify(emailDataStoreMock).emailUsername = "email"
        verify(emailDataStoreMock).emailToken = "token"
    }

    @Test
    fun `deduplicate - different local address - pixel event`() {
        whenever(emailDataStoreMock.emailUsername).thenReturn("username2")
        whenever(emailDataStoreMock.emailToken).thenReturn("token2")

        testee.deduplicate("{\"username\":\"email\",\"personal_access_token\":\"token\"}")

        verify(pixelMock).fire(AppPixelName.DUCK_EMAIL_OVERRIDE_PIXEL)
    }

    @Test
    fun `deduplicate - no local account - store remote`() {
        whenever(emailDataStoreMock.emailUsername).thenReturn(null)
        whenever(emailDataStoreMock.emailToken).thenReturn(null)

        testee.deduplicate("{\"username\":\"email\",\"personal_access_token\":\"token\"}")

        verify(emailDataStoreMock).emailUsername = "email"
        verify(emailDataStoreMock).emailToken = "token"
    }

    @Test
    fun `deduplicate - null address - do nothing`() {
        whenever(emailDataStoreMock.emailUsername).thenReturn("username")
        whenever(emailDataStoreMock.emailToken).thenReturn("token")

        testee.deduplicate(null)

        verify(emailDataStoreMock, times(0)).emailToken
        verify(emailDataStoreMock, times(0)).emailUsername
    }

    companion object {
        private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter: JsonAdapter<DuckAddressSetting> = moshi.adapter(DuckAddressSetting::class.java).lenient()
    }
}
