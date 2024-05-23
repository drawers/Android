package com.duckduckgo.savedsites.impl

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.duckduckgo.app.global.db.AppDatabase
import com.duckduckgo.app.sync.FakeDisplayModeSettingsRepository
import com.duckduckgo.app.sync.FakeFavoritesDisplayModeSettingsRepository
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.savedsites.api.models.SavedSite.Favorite
import com.duckduckgo.savedsites.api.models.SavedSitesNames
import com.duckduckgo.savedsites.store.Entity
import com.duckduckgo.savedsites.store.EntityType.BOOKMARK
import com.duckduckgo.savedsites.store.FavoritesDisplayMode.NATIVE
import com.duckduckgo.savedsites.store.FavoritesDisplayMode.UNIFIED
import com.duckduckgo.savedsites.store.Relation
import com.duckduckgo.savedsites.store.SavedSitesEntitiesDao
import com.duckduckgo.savedsites.store.SavedSitesRelationsDao
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealFavoritesDelegateTest {

    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private lateinit var savedSitesEntitiesDao: SavedSitesEntitiesDao
    private lateinit var savedSitesRelationsDao: SavedSitesRelationsDao
    private lateinit var testee: RealFavoritesDelegate
    private val syncDisabledFavoritesSettings = FakeDisplayModeSettingsRepository()
    private val favoritesDisplayModeSettings = FakeFavoritesDisplayModeSettingsRepository(NATIVE)
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        savedSitesEntitiesDao = db.syncEntitiesDao()
        savedSitesRelationsDao = db.syncRelationsDao()
    }

    @After
    fun after() {
        db.close()
    }

    @Test
    fun `getFavorites - default - returns favorites from root folder flow`() = runTest {
        givenFavoriteDelegate(syncDisabledFavoritesSettings)
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        testee.getFavorites().test {
            assertEquals(3, awaitItem().size)
        }
    }

    @Test
    fun `getFavorites - unified mode - returns favorites from root folder flow`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = UNIFIED })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        testee.getFavorites().test {
            assertEquals(3, awaitItem().size)
        }
    }

    @Test
    fun `getFavorites - native mode - returns mobile favorites folder flow`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = NATIVE })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        testee.getFavorites().test {
            assertEquals(2, awaitItem().size)
        }
    }

    @Test
    fun `getFavorites - view mode changes - new flow emitted`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = NATIVE })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        testee.getFavorites().test {
            assertEquals(2, awaitItem().size)
            favoritesDisplayModeSettings.favoritesDisplayMode = UNIFIED
            favoritesDisplayModeSettings.queryFavoritesFolderFlow.emit(favoritesDisplayModeSettings.getQueryFolder())
            assertEquals(3, awaitItem().size)
        }
    }

    @Test
    fun `getFavoritesSync - default - returns root favorites folder list`() = runTest {
        givenFavoriteDelegate(syncDisabledFavoritesSettings)
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        assertTrue(testee.getFavoritesSync().size == 3)
    }

    @Test
    fun `getFavoritesSync - unified mode - returns root favorites folder list`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = UNIFIED })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        assertTrue(testee.getFavoritesSync().size == 3)
    }

    @Test
    fun `whenNativeMode - return mobile favorites folder list`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = NATIVE })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        assertTrue(testee.getFavoritesSync().size == 2)
    }

    @Test
    fun `getFavoritesCountByDomain - only check root folder`() = runTest {
        givenFavoriteDelegate(syncDisabledFavoritesSettings)
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        assertTrue(testee.getFavoritesCountByDomain("favexample.com") == 3)
    }

    @Test
    fun `getFavoritesCountByDomain - unified mode - only check root folder`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = UNIFIED })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        assertTrue(testee.getFavoritesCountByDomain("favexample.com") == 3)
    }

    @Test
    fun `getFavoritesCountByDomain - native mode - only check mobile folder`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = NATIVE })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)
        assertTrue(testee.getFavoritesCountByDomain("favexample.com") == 2)
    }

    @Test
    fun `getFavoriteById - only check root folder`() = runTest {
        givenFavoriteDelegate(syncDisabledFavoritesSettings)
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        assertNotNull(testee.getFavoriteById("favorite3"))
    }

    @Test
    fun `getFavoriteById - unified mode - only check root folder`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = UNIFIED })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        assertNotNull(testee.getFavoriteById("favorite3"))
    }

    @Test
    fun `getFavoriteById - native mode - only check mobile folder`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = NATIVE })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        assertNull(testee.getFavoriteById("favorite3"))
    }

    @Test
    fun `whenFavoritesCount - only check root folder`() = runTest {
        givenFavoriteDelegate(syncDisabledFavoritesSettings)
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        assertTrue(testee.favoritesCount() == 3L)
    }

    @Test
    fun `whenUnifiedModeFavoritesCount - only check root folder`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = UNIFIED })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        assertTrue(testee.favoritesCount() == 3L)
    }

    @Test
    fun `whenNativeModeFavoritesCount - only check mobile folder`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = NATIVE })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        assertTrue(testee.favoritesCount() == 2L)
    }

    @Test
    fun `updateWithPosition - update items on root folder - updated`() {
        givenFavoriteDelegate(syncDisabledFavoritesSettings)
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        testee.updateWithPosition(listOf(favoritethree, favoritetwo, favoriteone))

        var entitiesInFolder = savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_ROOT)
        assertEquals(entitiesInFolder.get(0).entityId, favoritethree.id)
        assertEquals(entitiesInFolder.get(1).entityId, favoritetwo.id)
        assertEquals(entitiesInFolder.get(2).entityId, favoriteone.id)

        entitiesInFolder = savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_MOBILE_ROOT)
        TestCase.assertEquals(entitiesInFolder.get(0).entityId, favoriteone.id)
        TestCase.assertEquals(entitiesInFolder.get(1).entityId, favoritetwo.id)
    }

    @Test
    fun `updateWithPosition - unified mode update with position - items updated on root folder`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = UNIFIED })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        testee.updateWithPosition(listOf(favoritethree, favoritetwo, favoriteone))

        var entitiesInFolder = savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_ROOT)
        assertEquals(entitiesInFolder.get(0).entityId, favoritethree.id)
        assertEquals(entitiesInFolder.get(1).entityId, favoritetwo.id)
        assertEquals(entitiesInFolder.get(2).entityId, favoriteone.id)

        entitiesInFolder = savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_MOBILE_ROOT)
        TestCase.assertEquals(entitiesInFolder.get(0).entityId, favoriteone.id)
        TestCase.assertEquals(entitiesInFolder.get(1).entityId, favoritetwo.id)
    }

    @Test
    fun `updateWithPosition - root folder items updated`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = NATIVE })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        val favoritetwo = Favorite("favorite2", "Favorite2", "http://favexample.com", "timestamp", 1)
        val favoritethree = Favorite("favorite3", "Favorite3", "http://favexample.com", "timestamp", 2)
        givenFavoriteStored(favoriteone, favoritetwo, favoritethree, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoritetwo, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        testee.updateWithPosition(listOf(favoritethree, favoritetwo, favoriteone))

        var entitiesInFolder = savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_ROOT)
        assertEquals(entitiesInFolder.get(0).entityId, favoriteone.id)
        assertEquals(entitiesInFolder.get(1).entityId, favoritetwo.id)
        assertEquals(entitiesInFolder.get(2).entityId, favoritethree.id)

        entitiesInFolder = savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_MOBILE_ROOT)
        TestCase.assertEquals(entitiesInFolder.get(0).entityId, favoritethree.id)
        TestCase.assertEquals(entitiesInFolder.get(1).entityId, favoritetwo.id)
    }

    @Test
    fun `insertFavorite - update items on root folder`() {
        givenFavoriteDelegate(syncDisabledFavoritesSettings)

        testee.insertFavorite("favorite1", "Favorite", "http://favexample.com", "timestamp")

        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_ROOT).size == 1)
        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_MOBILE_ROOT).size == 0)
    }

    @Test
    fun `insertFavorite - unified mode - update items on root and mobile folder`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = UNIFIED })
        testee.insertFavorite("favorite1", "Favorite", "http://favexample.com", "timestamp")

        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_ROOT).size == 1)
        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_MOBILE_ROOT).size == 1)
    }

    @Test
    fun `insertFavorite - native mode - update items on root and mobile folder`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = NATIVE })
        testee.insertFavorite("favorite1", "Favorite", "http://favexample.com", "timestamp")

        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_ROOT).size == 1)
        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_MOBILE_ROOT).size == 1)
    }

    @Test
    fun `deleteFavorite - root folder deleted`() = runTest {
        givenFavoriteDelegate(syncDisabledFavoritesSettings)

        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        givenFavoriteStored(favoriteone, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)
        givenFavoriteStored(favoriteone, favoriteFolderId = SavedSitesNames.FAVORITES_DESKTOP_ROOT)

        testee.deleteFavorite(favoriteone)

        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_ROOT).isEmpty())
        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_MOBILE_ROOT).size == 1)
        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_DESKTOP_ROOT).size == 1)
    }

    @Test
    fun `deleteFavorite - unified mode delete favorite - delete from all folders`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = UNIFIED })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        givenFavoriteStored(favoriteone, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)
        givenFavoriteStored(favoriteone, favoriteFolderId = SavedSitesNames.FAVORITES_DESKTOP_ROOT)

        testee.deleteFavorite(favoriteone)

        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_ROOT).isEmpty())
        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_MOBILE_ROOT).isEmpty())
        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_DESKTOP_ROOT).isEmpty())
    }

    @Test
    fun `deleteFavorite - native mode delete non desktop favorite - deleted from all folders`() = runTest {
        givenFavoriteDelegate(favoritesDisplayModeSettings.apply { favoritesDisplayMode = NATIVE })
        val favoriteone = Favorite("favorite1", "Favorite", "http://favexample.com", "timestamp", 0)
        givenFavoriteStored(favoriteone, favoriteFolderId = SavedSitesNames.FAVORITES_ROOT)
        givenFavoriteStored(favoriteone, favoriteFolderId = SavedSitesNames.FAVORITES_MOBILE_ROOT)

        testee.deleteFavorite(favoriteone)

        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_ROOT).isEmpty())
        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_MOBILE_ROOT).isEmpty())
        assertTrue(savedSitesEntitiesDao.entitiesInFolderSync(SavedSitesNames.FAVORITES_DESKTOP_ROOT).isEmpty())
    }

    @Test
    fun `getFavoritesObservable - data source changes - new list received`() {
        givenFavoriteDelegate(syncDisabledFavoritesSettings)
        givenNoFavoritesStored()

        testee.insertFavorite(id = "Favorite1", title = "Favorite", url = "http://favexample.com", lastModified = "timestamp")

        val testObserver = testee.getFavoritesObservable().test()
        val lastState = testObserver.assertNoErrors().values().last()

        Assert.assertEquals(1, lastState.size)
        Assert.assertEquals("Favorite", lastState.first().title)
        Assert.assertEquals("http://favexample.com", lastState.first().url)
        Assert.assertEquals(0, lastState.first().position)
    }

    private fun givenFavoriteDelegate(displayModeSettingsRepository: FavoritesDisplayModeSettingsRepository) {
        testee = RealFavoritesDelegate(
            savedSitesEntitiesDao,
            savedSitesRelationsDao,
            displayModeSettingsRepository,
            MissingEntitiesRelationReconciler(savedSitesEntitiesDao),
            coroutineRule.testDispatcherProvider,
        )
    }

    private fun givenFavoriteStored(vararg favorite: Favorite, favoriteFolderId: String) {
        favorite.forEach {
            val entity = Entity(it.id, it.title, it.url, type = BOOKMARK, lastModified = it.lastModified)
            savedSitesEntitiesDao.insert(entity)
            savedSitesRelationsDao.insert(Relation(folderId = favoriteFolderId, entityId = entity.entityId))
            savedSitesRelationsDao.insert(Relation(folderId = SavedSitesNames.BOOKMARKS_ROOT, entityId = entity.entityId))
        }
    }

    private fun givenNoFavoritesStored() {
        assertFalse(testee.favoritesCount() > 0)
    }
}
