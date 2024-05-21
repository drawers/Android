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

package com.duckduckgo.app.browser

import android.content.Context
import android.view.ContextMenu
import android.view.MenuItem
import android.webkit.WebView.HitTestResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duckduckgo.app.browser.customtabs.CustomTabDetector
import com.duckduckgo.app.browser.model.LongPressTarget
import com.duckduckgo.app.pixels.AppPixelName
import com.duckduckgo.app.statistics.pixels.Pixel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private const val HTTPS_IMAGE_URL = "https://example.com/1.img"
private const val DATA_URI_IMAGE_URL = "data:image/png;base64,iVB23="

@RunWith(AndroidJUnit4::class)
class WebViewLongPressHandlerTest {

    private lateinit var testee: WebViewLongPressHandler

    @Mock
    private lateinit var mockMenu: ContextMenu

    @Mock
    private lateinit var mockMenuItem: MenuItem

    @Mock
    private lateinit var mockPixel: Pixel

    @Mock
    private lateinit var mockCustomTabDetector: CustomTabDetector

    @Mock
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testee = WebViewLongPressHandler(context, mockPixel, mockCustomTabDetector)
    }

    @Test
    fun `handleLongPress - image type - pixel fired`() {
        testee.handleLongPress(HitTestResult.IMAGE_TYPE, HTTPS_IMAGE_URL, mockMenu)
        verify(mockPixel).fire(AppPixelName.LONG_PRESS)
    }

    @Test
    fun `handleLongPress - anchor image type - pixel fired`() {
        testee.handleLongPress(HitTestResult.SRC_IMAGE_ANCHOR_TYPE, HTTPS_IMAGE_URL, mockMenu)
        verify(mockPixel).fire(AppPixelName.LONG_PRESS)
    }

    @Test
    fun `handleLongPress - unknown type - pixel not fired`() {
        testee.handleLongPress(HitTestResult.UNKNOWN_TYPE, HTTPS_IMAGE_URL, mockMenu)
        verify(mockPixel, never()).fire(AppPixelName.LONG_PRESS)
    }

    @Test
    fun `handleLongPress - image type - url header added to menu`() {
        testee.handleLongPress(HitTestResult.IMAGE_TYPE, HTTPS_IMAGE_URL, mockMenu)
        verify(mockMenu).setHeaderTitle(HTTPS_IMAGE_URL)
    }

    @Test
    fun `handleLongPress - anchor image type - url header added to menu`() {
        testee.handleLongPress(HitTestResult.SRC_IMAGE_ANCHOR_TYPE, HTTPS_IMAGE_URL, mockMenu)
        verify(mockMenu).setHeaderTitle(HTTPS_IMAGE_URL)
    }

    @Test
    fun `handleLongPress - image type and https - correct options added to menu`() {
        testee.handleLongPress(HitTestResult.IMAGE_TYPE, HTTPS_IMAGE_URL, mockMenu)
        // Show "Download Image" and "Open Image in Background Tab"
        verifyDownloadImageMenuOptionsAdded()
        verifyImageMenuOpenInTabOptionsAdded()
        // Options not shown: "Open In New Tab", "Open in Background Tab", "Copy Link Address", "Share Link"
        verifyLinkMenuOpenInTabOptionsNotAdded()
        verifyLinkMenuOtherOptionsNotAdded()
    }

    @Test
    fun `handleLongPress - image type and data url - correct options added to menu`() {
        testee.handleLongPress(HitTestResult.IMAGE_TYPE, DATA_URI_IMAGE_URL, mockMenu)
        // Show "Download Image"
        verifyDownloadImageMenuOptionsAdded()
        // Options not shown: "Open Image in Background Tab", "Open In New Tab", "Open in Background Tab", "Copy Link Address", "Share Link"
        verifyImageMenuOpenInTabOptionsNotAdded()
        verifyLinkMenuOpenInTabOptionsNotAdded()
        verifyLinkMenuOtherOptionsNotAdded()
    }

    @Test
    fun `handleLongPress - custom tab and https - correct options added to menu`() {
        whenever(mockCustomTabDetector.isCustomTab()).thenReturn(true)
        testee.handleLongPress(HitTestResult.IMAGE_TYPE, HTTPS_IMAGE_URL, mockMenu)
        // Show "Download Image"
        verifyDownloadImageMenuOptionsAdded()
        // Options not shown: "Open Image in Background Tab", "Open In New Tab", "Open in Background Tab", "Copy Link Address", "Share Link"
        verifyImageMenuOpenInTabOptionsNotAdded()
        verifyLinkMenuOpenInTabOptionsNotAdded()
        verifyLinkMenuOtherOptionsNotAdded()
    }

    @Test
    fun `handleLongPress - anchor image type and https - correct options added to menu`() {
        testee.handleLongPress(HitTestResult.SRC_IMAGE_ANCHOR_TYPE, HTTPS_IMAGE_URL, mockMenu)
        // Show "Download Image", "Open Image in Background Tab", "Open In New Tab", "Open in Background Tab",
        // "Copy Link Address", "Share Link"
        verifyDownloadImageMenuOptionsAdded()
        verifyImageMenuOpenInTabOptionsAdded()
        verifyLinkMenuOpenInTabOptionsAdded()
        verifyLinkMenuOtherOptionsAdded()
    }

    @Test
    fun `handleLongPress - anchor image type and data url - correct options added to menu`() {
        testee.handleLongPress(HitTestResult.SRC_IMAGE_ANCHOR_TYPE, DATA_URI_IMAGE_URL, mockMenu)
        // Show "Download Image"
        verifyDownloadImageMenuOptionsAdded()
        // Options not shown: "Open Image in Background Tab", "Open In New Tab", "Open in Background Tab", "Copy Link Address", "Share Link"
        verifyImageMenuOpenInTabOptionsNotAdded()
        verifyLinkMenuOpenInTabOptionsNotAdded()
        verifyLinkMenuOtherOptionsNotAdded()
    }

    @Test
    fun `handleLongPress - custom tab - long press with anchor image type and https - correct options added to menu`() {
        testee.handleLongPress(HitTestResult.SRC_IMAGE_ANCHOR_TYPE, DATA_URI_IMAGE_URL, mockMenu)
        // Show "Download Image"
        verifyDownloadImageMenuOptionsAdded()
        // Options not shown: "Open Image in Background Tab", "Open In New Tab", "Open in Background Tab", "Copy Link Address", "Share Link"
        verifyImageMenuOpenInTabOptionsNotAdded()
        verifyLinkMenuOpenInTabOptionsNotAdded()
        verifyLinkMenuOtherOptionsNotAdded()
    }

    @Test
    fun `handleLongPress - anchor type and https - correct options added to menu`() {
        testee.handleLongPress(HitTestResult.SRC_ANCHOR_TYPE, HTTPS_IMAGE_URL, mockMenu)
        // Show "Open In New Tab", "Open in Background Tab", "Copy Link Address", "Share Link"
        verifyLinkMenuOpenInTabOptionsAdded()
        verifyLinkMenuOtherOptionsAdded()
        // Options not shown: "Download Image", "Open Image in Background Tab"
        verifyDownloadImageMenuOptionsNotAdded()
        verifyImageMenuOpenInTabOptionsNotAdded()
    }

    @Test
    fun `handleLongPress - anchor type and data url - correct options added to menu`() {
        testee.handleLongPress(HitTestResult.SRC_ANCHOR_TYPE, DATA_URI_IMAGE_URL, mockMenu)
        // Show "Open In New Tab", "Open in Background Tab", "Copy Link Address", "Share Link"
        verifyLinkMenuOpenInTabOptionsAdded()
        verifyLinkMenuOtherOptionsAdded()
        // Options not shown: "Download Image", "Open Image in Background Tab"
        verifyDownloadImageMenuOptionsNotAdded()
        verifyImageMenuOpenInTabOptionsNotAdded()
    }

    @Test
    fun whenInCustomTabAndUserLongPressesWithAnchorTypeAndHttpsThenCorrectOptionsAddedToMenu() {
        whenever(mockCustomTabDetector.isCustomTab()).thenReturn(true)
        testee.handleLongPress(HitTestResult.SRC_ANCHOR_TYPE, HTTPS_IMAGE_URL, mockMenu)
        // Show "Copy Link Address", "Share Link"
        verifyLinkMenuOtherOptionsAdded()
        // Options not shown: "Download Image", "Open Image in Background Tab", "Open In New Tab", "Open in Background Tab"
        verifyDownloadImageMenuOptionsNotAdded()
        verifyImageMenuOpenInTabOptionsNotAdded()
        verifyLinkMenuOpenInTabOptionsNotAdded()
    }

    @Test
    fun `handleLongPress - other image type - menu not altered`() {
        testee.handleLongPress(HitTestResult.UNKNOWN_TYPE, HTTPS_IMAGE_URL, mockMenu)
        verifyMenuNotAltered()
    }

    @Test
    fun `userSelectsDownloadImageOption - action download file required`() {
        whenever(mockMenuItem.itemId).thenReturn(WebViewLongPressHandler.CONTEXT_MENU_ID_DOWNLOAD_IMAGE)
        val longPressTarget = LongPressTarget(url = "example.com", imageUrl = "example.com/foo.jpg", type = HitTestResult.SRC_ANCHOR_TYPE)
        val action = testee.userSelectedMenuItem(longPressTarget, mockMenuItem)
        assertTrue(action is LongPressHandler.RequiredAction.DownloadFile)
    }

    @Test
    fun `userSelectsDownloadImageOption - no image url available - no action required`() {
        whenever(mockMenuItem.itemId).thenReturn(WebViewLongPressHandler.CONTEXT_MENU_ID_DOWNLOAD_IMAGE)
        val longPressTarget = LongPressTarget(url = "example.com", imageUrl = null, type = HitTestResult.SRC_ANCHOR_TYPE)
        val action = testee.userSelectedMenuItem(longPressTarget, mockMenuItem)
        assertTrue(action is LongPressHandler.RequiredAction.None)
    }

    @Test
    fun `userSelectsDownloadImageOption - download file with correct url returned`() {
        whenever(mockMenuItem.itemId).thenReturn(WebViewLongPressHandler.CONTEXT_MENU_ID_DOWNLOAD_IMAGE)
        val longPressTarget = LongPressTarget(url = "example.com", imageUrl = "example.com/foo.jpg", type = HitTestResult.SRC_ANCHOR_TYPE)
        val action = testee.userSelectedMenuItem(longPressTarget, mockMenuItem) as LongPressHandler.RequiredAction.DownloadFile
        assertEquals("example.com/foo.jpg", action.url)
    }

    @Test
    fun `userSelectsUnknownOption - no action required returned`() {
        val unknownMenuId = 123
        whenever(mockMenuItem.itemId).thenReturn(unknownMenuId)
        val longPressTarget = LongPressTarget(url = "example.com", type = HitTestResult.SRC_ANCHOR_TYPE)
        val action = testee.userSelectedMenuItem(longPressTarget, mockMenuItem)
        assertTrue(action == LongPressHandler.RequiredAction.None)
    }

    private fun verifyDownloadImageMenuOptionsAdded() {
        verify(mockMenu).add(anyInt(), eq(WebViewLongPressHandler.CONTEXT_MENU_ID_DOWNLOAD_IMAGE), anyInt(), eq(R.string.downloadImage))
    }

    private fun verifyDownloadImageMenuOptionsNotAdded() {
        verify(mockMenu, never()).add(anyInt(), eq(WebViewLongPressHandler.CONTEXT_MENU_ID_DOWNLOAD_IMAGE), anyInt(), eq(R.string.downloadImage))
    }

    private fun verifyImageMenuOpenInTabOptionsAdded() {
        verify(mockMenu).add(
            anyInt(),
            eq(WebViewLongPressHandler.CONTEXT_MENU_ID_OPEN_IMAGE_IN_NEW_BACKGROUND_TAB),
            anyInt(),
            eq(R.string.openImageInNewTab),
        )
    }

    private fun verifyImageMenuOpenInTabOptionsNotAdded() {
        verify(mockMenu, never()).add(
            anyInt(),
            eq(WebViewLongPressHandler.CONTEXT_MENU_ID_OPEN_IMAGE_IN_NEW_BACKGROUND_TAB),
            anyInt(),
            eq(R.string.openImageInNewTab),
        )
    }

    private fun verifyLinkMenuOpenInTabOptionsAdded() {
        verify(mockMenu).add(
            anyInt(),
            eq(WebViewLongPressHandler.CONTEXT_MENU_ID_OPEN_IN_NEW_TAB),
            anyInt(),
            eq(R.string.openInNewTab),
        )

        verify(mockMenu).add(
            anyInt(),
            eq(WebViewLongPressHandler.CONTEXT_MENU_ID_OPEN_IN_NEW_BACKGROUND_TAB),
            anyInt(),
            eq(R.string.openInNewBackgroundTab),
        )
    }

    private fun verifyLinkMenuOpenInTabOptionsNotAdded() {
        verify(mockMenu, never()).add(
            anyInt(),
            eq(WebViewLongPressHandler.CONTEXT_MENU_ID_OPEN_IN_NEW_TAB),
            anyInt(),
            eq(R.string.openInNewTab),
        )

        verify(mockMenu, never()).add(
            anyInt(),
            eq(WebViewLongPressHandler.CONTEXT_MENU_ID_OPEN_IN_NEW_BACKGROUND_TAB),
            anyInt(),
            eq(R.string.openInNewBackgroundTab),
        )
    }

    private fun verifyLinkMenuOtherOptionsAdded() {
        verify(mockMenu).add(
            anyInt(),
            eq(WebViewLongPressHandler.CONTEXT_MENU_ID_COPY),
            anyInt(),
            eq(R.string.copyUrl),
        )

        verify(mockMenu).add(
            anyInt(),
            eq(WebViewLongPressHandler.CONTEXT_MENU_ID_SHARE_LINK),
            anyInt(),
            eq(R.string.shareLink),
        )
    }

    private fun verifyLinkMenuOtherOptionsNotAdded() {
        verify(mockMenu, never()).add(
            anyInt(),
            eq(WebViewLongPressHandler.CONTEXT_MENU_ID_COPY),
            anyInt(),
            eq(R.string.copyUrl),
        )

        verify(mockMenu, never()).add(
            anyInt(),
            eq(WebViewLongPressHandler.CONTEXT_MENU_ID_SHARE_LINK),
            anyInt(),
            eq(R.string.shareLink),
        )
    }

    private fun verifyMenuNotAltered() {
        verify(mockMenu, never()).add(anyInt())
        verify(mockMenu, never()).add(anyString())
        verify(mockMenu, never()).add(anyInt(), anyInt(), anyInt(), anyInt())
        verify(mockMenu, never()).add(anyInt(), anyInt(), anyInt(), anyString())
    }
}
