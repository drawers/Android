package com.duckduckgo.savedsites.impl.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.savedsites.store.FavoritesDisplayMode
import com.duckduckgo.sync.settings.api.SyncSettingsListener
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class DisplayModeSyncableSettingTest {

    @get:Rule
    private val coroutineRule = CoroutineTestRule()

    private val savedSitesSettingsStore = FakeSavedSitesSettingsStore(coroutineRule.testScope)
    private val syncSettingsListener: SyncSettingsListener = mock()

    private val testee = DisplayModeSyncableSetting(
        savedSitesSettingsStore,
        syncSettingsListener,
    )

    @Test
    fun `getValue - return stored value`() = runTest {
        savedSitesSettingsStore.favoritesDisplayMode = FavoritesDisplayMode.NATIVE
        assertEquals(FavoritesDisplayMode.NATIVE.value, testee.getValue())
    }

    @Test
    fun `save - valid value - return true`() = runTest {
        assertTrue(testee.save(FavoritesDisplayMode.NATIVE.value))
    }

    @Test
    fun `save - invalid value - return false`() = runTest {
        assertFalse(testee.save("unknown_value"))
    }

    @Test
    fun `deduplicate - valid value - return true`() = runTest {
        assertTrue(testee.deduplicate(FavoritesDisplayMode.NATIVE.value))
    }

    @Test
    fun `deduplicate - invalid value - return false`() = runTest {
        assertFalse(testee.deduplicate("unknown_value"))
    }

    @Test
    fun `onSettingChanged - notify listener`() = runTest {
        testee.onSettingChanged()
        verify(syncSettingsListener).onSettingChanged(testee.key)
    }
}
