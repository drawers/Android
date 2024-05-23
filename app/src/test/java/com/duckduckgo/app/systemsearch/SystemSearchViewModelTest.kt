/*
 * Copyright (c) 2022 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.systemsearch

import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.duckduckgo.app.autocomplete.api.AutoComplete
import com.duckduckgo.app.autocomplete.api.AutoComplete.AutoCompleteResult
import com.duckduckgo.app.autocomplete.api.AutoComplete.AutoCompleteSuggestion.AutoCompleteSearchSuggestion
import com.duckduckgo.app.browser.favorites.FavoritesQuickAccessAdapter.QuickAccessFavorite
import com.duckduckgo.app.onboarding.store.*
import com.duckduckgo.app.pixels.AppPixelName.*
import com.duckduckgo.app.settings.db.SettingsDataStore
import com.duckduckgo.app.statistics.pixels.Pixel
import com.duckduckgo.app.systemsearch.SystemSearchViewModel.Command
import com.duckduckgo.app.systemsearch.SystemSearchViewModel.Command.LaunchDuckDuckGo
import com.duckduckgo.app.systemsearch.SystemSearchViewModel.Command.UpdateVoiceSearch
import com.duckduckgo.app.systemsearch.SystemSearchViewModel.Suggestions.QuickAccessItems
import com.duckduckgo.app.systemsearch.SystemSearchViewModel.Suggestions.SystemSearchResultsViewState
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.common.test.InstantSchedulersRule
import com.duckduckgo.savedsites.api.SavedSitesRepository
import com.duckduckgo.savedsites.api.models.SavedSite.Favorite
import io.reactivex.Observable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.Mockito.verify
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("EXPERIMENTAL_API_USAGE")
class SystemSearchViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val schedulers = InstantSchedulersRule()

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private val mockUserStageStore: UserStageStore = mock()
    private val mockDeviceAppLookup: DeviceAppLookup = mock()
    private val mockAutoComplete: AutoComplete = mock()
    private val mocksavedSitesRepository: SavedSitesRepository = mock()
    private val mockPixel: Pixel = mock()
    private val mockSettingsStore: SettingsDataStore = mock()

    private val commandObserver: Observer<Command> = mock()
    private val commandCaptor = argumentCaptor<Command>()

    private lateinit var testee: SystemSearchViewModel

    @Before
    fun setup() {
        whenever(mockAutoComplete.autoComplete(QUERY)).thenReturn(Observable.just(autocompleteQueryResult))
        whenever(mockAutoComplete.autoComplete(BLANK_QUERY)).thenReturn(Observable.just(autocompleteBlankResult))
        whenever(mockDeviceAppLookup.query(QUERY)).thenReturn(appQueryResult)
        whenever(mockDeviceAppLookup.query(BLANK_QUERY)).thenReturn(appBlankResult)
        whenever(mocksavedSitesRepository.getFavorites()).thenReturn(flowOf())
        doReturn(true).whenever(mockSettingsStore).autoCompleteSuggestionsEnabled
        testee = SystemSearchViewModel(
            mockUserStageStore,
            mockAutoComplete,
            mockDeviceAppLookup,
            mockPixel,
            mocksavedSitesRepository,
            mockSettingsStore,
            coroutineRule.testDispatcherProvider,
            coroutineRule.testScope,
        )
        testee.command.observeForever(commandObserver)
    }

    @After
    fun tearDown() {
        testee.command.removeObserver(commandObserver)
    }

    @Test
    fun `resetViewState - onboarding should not show - view is not visible and unexpanded`() = runTest {
        whenever(mockUserStageStore.getUserAppStage()).thenReturn(AppStage.DAX_ONBOARDING)
        testee.resetViewState()

        val viewState = testee.onboardingViewState.value
        assertFalse(viewState!!.visible)
        assertFalse(viewState.expanded)
    }

    @Test
    fun `resetViewState - onboarding should show - view is visible and unexpanded`() = runTest {
        whenever(mockUserStageStore.getUserAppStage()).thenReturn(AppStage.NEW)
        testee.resetViewState()

        val viewState = testee.onboardingViewState.value
        assertTrue(viewState!!.visible)
        assertFalse(viewState.expanded)
    }

    @Test
    fun `resetViewState - onboarding shown - pixel sent`() = runTest {
        whenever(mockUserStageStore.getUserAppStage()).thenReturn(AppStage.NEW)
        testee.resetViewState()
        verify(mockPixel).fire(INTERSTITIAL_ONBOARDING_SHOWN)
    }

    @Test
    fun `userTappedOnboardingToggle - onboarding unexpanded - expanded and pixel sent`() = runTest {
        whenOnboardingShowing()
        testee.userTappedOnboardingToggle()

        val viewState = testee.onboardingViewState.value
        assertTrue(viewState!!.expanded)
        verify(mockPixel).fire(INTERSTITIAL_ONBOARDING_MORE_PRESSED)
    }

    @Test
    fun `userTappedOnboardingToggle - onboarding expanded - unexpanded and pixel sent`() = runTest {
        whenOnboardingShowing()
        testee.userTappedOnboardingToggle() // first press to expand
        testee.userTappedOnboardingToggle() // second press to minimize

        val viewState = testee.onboardingViewState.value
        assertFalse(viewState!!.expanded)
        verify(mockPixel).fire(INTERSTITIAL_ONBOARDING_LESS_PRESSED)
    }

    @Test
    fun `userDismissedOnboarding - onboarding dismissed - view hidden, pixel sent, store notified`() = runTest {
        whenOnboardingShowing()
        testee.userDismissedOnboarding()

        val viewState = testee.onboardingViewState.value
        assertFalse(viewState!!.visible)
        verify(mockPixel).fire(INTERSTITIAL_ONBOARDING_DISMISSED)
        verify(mockUserStageStore).stageCompleted(AppStage.NEW)
    }

    @Test
    fun `userUpdatedQuery - viewState updated`() = runTest {
        testee.userUpdatedQuery(QUERY)

        val newViewState = testee.resultsViewState.value as SystemSearchResultsViewState
        assertNotNull(newViewState)
        assertEquals(appQueryResult, newViewState.appResults)
        assertEquals(autocompleteQueryResult, newViewState.autocompleteResults)
    }

    @Test
    fun `userUpdatedQuery - adds space to query - viewState matches and space trimmed from autocomplete`() = runTest {
        testee.userUpdatedQuery(QUERY)
        testee.userUpdatedQuery("$QUERY ")

        val newViewState = testee.resultsViewState.value as SystemSearchResultsViewState
        assertNotNull(newViewState)
        assertEquals(appQueryResult, newViewState.appResults)
        assertEquals(autocompleteQueryResult, newViewState.autocompleteResults)
    }

    @Test
    fun `userUpdatedQuery - autoComplete enabled - suggestions not empty`() = runTest {
        doReturn(true).whenever(mockSettingsStore).autoCompleteSuggestionsEnabled
        testee.userUpdatedQuery(QUERY)

        val newViewState = testee.resultsViewState.value as SystemSearchResultsViewState
        assertNotNull(newViewState)
        assertEquals(appQueryResult, newViewState.appResults)
        assertEquals(autocompleteQueryResult, newViewState.autocompleteResults)
    }

    @Test
    fun `userUpdatedQuery - autoComplete disabled - viewState reset`() = runTest {
        doReturn(false).whenever(mockSettingsStore).autoCompleteSuggestionsEnabled
        testee.userUpdatedQuery(QUERY)

        assertTrue(testee.resultsViewState.value is SystemSearchViewModel.Suggestions.QuickAccessItems)
    }

    @Test
    fun `userRequestedClear - viewState reset`() = runTest {
        testee.userUpdatedQuery(QUERY)
        testee.userRequestedClear()

        assertTrue(testee.resultsViewState.value is SystemSearchViewModel.Suggestions.QuickAccessItems)
    }

    @Test
    fun `userUpdatedQuery - blank query - viewState reset`() = runTest {
        testee.userUpdatedQuery(QUERY)
        testee.userUpdatedQuery(BLANK_QUERY)

        assertTrue(testee.resultsViewState.value is SystemSearchViewModel.Suggestions.QuickAccessItems)
    }

    @Test
    fun `userSubmittedQuery - browser launched with query - pixel sent`() {
        testee.userSubmittedQuery(QUERY)
        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(Command.LaunchBrowser(QUERY), commandCaptor.lastValue)
        verify(mockPixel).fire(INTERSTITIAL_LAUNCH_BROWSER_QUERY)
    }

    @Test
    fun `userSubmittedQuery - query with space - browser launched with trimmed query and pixel sent`() {
        testee.userSubmittedQuery("$QUERY ")
        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(Command.LaunchBrowser(QUERY), commandCaptor.lastValue)
        verify(mockPixel).fire(INTERSTITIAL_LAUNCH_BROWSER_QUERY)
    }

    @Test
    fun `userSubmittedQuery - blank query - ignored`() {
        testee.userSubmittedQuery(BLANK_QUERY)
        assertFalse(commandCaptor.allValues.any { it is Command.LaunchBrowser })
        verify(mockPixel, never()).fire(INTERSTITIAL_LAUNCH_BROWSER_QUERY)
    }

    @Test
    fun `userSubmittedQuery - onboarding completed`() = runTest {
        testee.userSubmittedQuery(QUERY)
        verify(mockUserStageStore).stageCompleted(AppStage.NEW)
    }

    @Test
    fun `userSubmittedAutocompleteResult - browser launched and pixel sent`() {
        testee.userSubmittedAutocompleteResult(AUTOCOMPLETE_RESULT)
        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(Command.LaunchBrowser(AUTOCOMPLETE_RESULT), commandCaptor.lastValue)
        verify(mockPixel).fire(INTERSTITIAL_LAUNCH_BROWSER_QUERY)
    }

    @Test
    fun `userSelectedApp - app result selected - app launched and pixel sent`() {
        testee.userSelectedApp(deviceApp)
        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(Command.LaunchDeviceApplication(deviceApp), commandCaptor.lastValue)
        verify(mockPixel).fire(INTERSTITIAL_LAUNCH_DEVICE_APP)
    }

    @Test
    fun `userTappedDax - app launched and pixel sent`() {
        testee.userTappedDax()
        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertTrue(commandCaptor.lastValue is LaunchDuckDuckGo)
        verify(mockPixel).fire(INTERSTITIAL_LAUNCH_DAX)
    }

    @Test
    fun `userTappedDax - onboarding completed`() = runTest {
        testee.userTappedDax()
        verify(mockUserStageStore).stageCompleted(AppStage.NEW)
    }

    @Test
    fun `onViewModelCreated - apps refreshed`() = runTest {
        verify(mockDeviceAppLookup).refreshAppList()
    }

    @Test
    fun `appNotFound - app cannot be found - apps refreshed and user message shown`() = runTest {
        testee.appNotFound(deviceApp)
        verify(mockDeviceAppLookup, times(2)).refreshAppList()
        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(Command.ShowAppNotFoundMessage(deviceApp.shortName), commandCaptor.lastValue)
    }

    @Test
    fun `onUserSelectedToEditQuery - edit query command sent`() {
        val query = "test"
        testee.onUserSelectedToEditQuery(query)
        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(Command.EditQuery(query), commandCaptor.lastValue)
    }

    @Test
    fun `onQuickAccessItemClicked - launch browser`() {
        val quickAccessItem = QuickAccessFavorite(Favorite("favorite1", "title", "http://example.com", "timestamp", 0))

        testee.onQuickAccessItemClicked(quickAccessItem)

        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(Command.LaunchBrowser(quickAccessItem.favorite.url), commandCaptor.lastValue)
    }

    @Test
    fun `onQuickAccessItemClicked - pixel sent`() {
        val quickAccessItem = QuickAccessFavorite(Favorite("favorite1", "title", "http://example.com", "timestamp", 0))

        testee.onQuickAccessItemClicked(quickAccessItem)

        verify(mockPixel).fire(FAVORITE_SYSTEM_SEARCH_ITEM_PRESSED)
    }

    @Test
    fun `onEditQuickAccessItemRequested - launch edit dialog`() {
        val quickAccessItem = QuickAccessFavorite(Favorite("favorite1", "title", "http://example.com", "timestamp", 0))

        testee.onEditQuickAccessItemRequested(quickAccessItem)

        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(Command.LaunchEditDialog(quickAccessItem.favorite), commandCaptor.lastValue)
    }

    @Test
    fun `onDeleteQuickAccessItemRequested - show delete favorite confirmation`() {
        val quickAccessItem = QuickAccessFavorite(Favorite("favorite1", "title", "http://example.com", "timestamp", 0))

        testee.onDeleteQuickAccessItemRequested(quickAccessItem)

        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(Command.DeleteFavoriteConfirmation(quickAccessItem.favorite), commandCaptor.lastValue)
    }

    @Test
    fun `onDeleteSavedSiteRequested - show delete saved site confirmation`() {
        val quickAccessItem = QuickAccessFavorite(Favorite("favorite1", "title", "http://example.com", "timestamp", 0))

        testee.onDeleteSavedSiteRequested(quickAccessItem)

        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(Command.DeleteSavedSiteConfirmation(quickAccessItem.favorite), commandCaptor.lastValue)
    }

    @Test
    fun `onFavouriteEdited - repository updated`() {
        val savedSite = Favorite("favorite1", "title", "http://example.com", "timestamp", 0)

        testee.onFavouriteEdited(savedSite)

        verify(mocksavedSitesRepository).updateFavourite(savedSite)
    }

    @Test
    fun `onDeleteQuickAccessItemRequested - quick access delete requested - favorite deleted from viewState`() = runTest {
        val savedSite = Favorite("favorite1", "title", "http://example.com", "timestamp", 0)
        whenever(mocksavedSitesRepository.getFavorites()).thenReturn(flowOf(listOf(savedSite)))
        testee = SystemSearchViewModel(
            mockUserStageStore,
            mockAutoComplete,
            mockDeviceAppLookup,
            mockPixel,
            mocksavedSitesRepository,
            mockSettingsStore,
            coroutineRule.testDispatcherProvider,
            coroutineRule.testScope,
        )

        val viewState = testee.resultsViewState.value as QuickAccessItems
        assertFalse(viewState.favorites.isEmpty())

        testee.onDeleteQuickAccessItemRequested(QuickAccessFavorite(savedSite))

        val newViewState = testee.resultsViewState.value as QuickAccessItems
        assertTrue(newViewState.favorites.isEmpty())
    }

    @Test
    fun `undoDelete - quick access delete undo - viewState updated`() = runTest {
        val savedSite = Favorite("favorite1", "title", "http://example.com", "timestamp", 0)
        whenever(mocksavedSitesRepository.getFavorites()).thenReturn(flowOf(listOf(savedSite)))
        testee = SystemSearchViewModel(
            mockUserStageStore,
            mockAutoComplete,
            mockDeviceAppLookup,
            mockPixel,
            mocksavedSitesRepository,
            mockSettingsStore,
            coroutineRule.testDispatcherProvider,
            coroutineRule.testScope,
        )

        val viewState = testee.resultsViewState.value as QuickAccessItems
        assertFalse(viewState.favorites.isEmpty())

        testee.undoDelete(savedSite)

        assertFalse(viewState.favorites.isEmpty())
    }

    @Test
    fun `deleteFavoriteSnackbarDismissed - quick access deleted - repository deletes favorite`() = runTest {
        val savedSite = Favorite("favorite1", "title", "http://example.com", "timestamp", 0)

        testee.deleteFavoriteSnackbarDismissed(savedSite)

        verify(mocksavedSitesRepository).delete(savedSite)
    }

    @Test
    fun `deleteSavedSiteSnackbarDismissed - associated bookmark deleted - repository deletes bookmark`() = runTest {
        val savedSite = Favorite("favorite1", "title", "http://example.com", "timestamp", 0)

        testee.deleteSavedSiteSnackbarDismissed(savedSite)

        verify(mocksavedSitesRepository).delete(savedSite, true)
    }

    @Test
    fun `onQuickAccessListChanged - repository updated`() {
        val savedSite = Favorite("favorute1", "title", "http://example.com", "timestamp", 0)
        val savedSites = listOf(QuickAccessFavorite(savedSite))

        testee.onQuickAccessListChanged(savedSites)

        verify(mocksavedSitesRepository).updateWithPosition(listOf(savedSite))
    }

    @Test
    fun `initialState - user has favorites - shows favorites`() {
        val savedSite = Favorite("favorite1", "title", "http://example.com", "timestamp", 0)
        whenever(mocksavedSitesRepository.getFavorites()).thenReturn(flowOf(listOf(savedSite)))
        testee = SystemSearchViewModel(
            mockUserStageStore,
            mockAutoComplete,
            mockDeviceAppLookup,
            mockPixel,
            mocksavedSitesRepository,
            mockSettingsStore,
            coroutineRule.testDispatcherProvider,
            coroutineRule.testScope,
        )

        val viewState = testee.resultsViewState.value as SystemSearchViewModel.Suggestions.QuickAccessItems
        assertEquals(1, viewState.favorites.size)
        assertEquals(savedSite, viewState.favorites.first().favorite)
    }

    @Test
    fun `voiceSearchDisabled - should emit update voice search command`() {
        testee.voiceSearchDisabled()

        verify(commandObserver, atLeastOnce()).onChanged(commandCaptor.capture())
        assertEquals(UpdateVoiceSearch, commandCaptor.lastValue)
    }

    private suspend fun whenOnboardingShowing() {
        whenever(mockUserStageStore.getUserAppStage()).thenReturn(AppStage.NEW)
        testee.resetViewState()
    }

    private fun givenEmptyUserStageStore(): UserStageStore {
        val emptyUserStageDao = object : UserStageDao {
            override suspend fun currentUserAppStage() = UserStage(appStage = AppStage.NEW)
            override fun insert(userStage: UserStage) {}
        }
        return AppUserStageStore(emptyUserStageDao, coroutineRule.testDispatcherProvider)
    }

    companion object {
        const val QUERY = "abc"
        const val BLANK_QUERY = ""
        const val AUTOCOMPLETE_RESULT = "autocomplete result"
        val deviceApp = DeviceApp("", "", Intent())
        val autocompleteQueryResult = AutoCompleteResult(QUERY, listOf(AutoCompleteSearchSuggestion(QUERY, false)))
        val autocompleteBlankResult = AutoCompleteResult(BLANK_QUERY, emptyList())
        val appQueryResult = listOf(deviceApp)
        val appBlankResult = emptyList<DeviceApp>()
    }
}
