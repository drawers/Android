package com.duckduckgo.sync.impl.engine

import com.duckduckgo.app.*
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.sync.api.engine.*
import com.duckduckgo.sync.store.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.mockito.kotlin.*

class SyncStateObserverTest {

    @get:Rule
    val coroutineTestRule: CoroutineTestRule = CoroutineTestRule()

    private val syncEngine: SyncEngine = mock()
    private val syncStore: SyncStore = mock<SyncStore>()

    private val testee = SyncAccountDisabledObserver(
        appCoroutineScope = coroutineTestRule.testScope,
        syncStore = syncStore,
        syncEngine = syncEngine,
        dispatcherProvider = coroutineTestRule.testDispatcherProvider,
    )

    @Test
    fun `onCreate - signed out - does not notify sync engine`() = runTest {
        val isSignedInFlow = MutableStateFlow(false)
        whenever(syncStore.isSignedInFlow()).thenReturn(isSignedInFlow)
        testee.onCreate(mock())
        verify(syncEngine, times(0)).onSyncDisabled()
    }

    @Test
    fun `onCreate - signed in - does not notify sync engine`() = runTest {
        val isSignedInFlow = MutableStateFlow(true)
        whenever(syncStore.isSignedInFlow()).thenReturn(isSignedInFlow)
        testee.onCreate(mock())
        verify(syncEngine, times(0)).onSyncDisabled()
    }

    @Test
    fun `onCreate - signed in and signed out event received - notify sync engine`() = runTest {
        val isSignedInFlow = MutableStateFlow(true)
        whenever(syncStore.isSignedInFlow()).thenReturn(isSignedInFlow)
        testee.onCreate(mock())

        isSignedInFlow.emit(false)

        verify(syncEngine).onSyncDisabled()
    }
}
