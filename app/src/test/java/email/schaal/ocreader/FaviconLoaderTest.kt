package email.schaal.ocreader

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.util.FaviconLoader.Companion.getDrawable
import email.schaal.ocreader.util.TextDrawable
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for FaviconLoader
 */
@RunWith(AndroidJUnit4::class)
class FaviconLoaderTest {
    @Test
    @Throws(Exception::class)
    fun testGetDrawable() {
        val feed = Feed()
        feed.name = "Test"
        feed.url = "http://example.com"
        feed.faviconLink = null
        Assert.assertTrue(getDrawable(ApplicationProvider.getApplicationContext(), feed) is TextDrawable)
        // TODO: 01.08.16 Test feed with favicon
    }

    @Test
    @Throws(Exception::class)
    fun testGetFeedColor() {
    }

    @Test
    @Throws(Exception::class)
    fun testLoad() {
    }
}