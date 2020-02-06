package email.schaal.ocreader

import android.graphics.Color
import android.util.Pair
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import email.schaal.ocreader.database.model.Feed
import email.schaal.ocreader.util.FaviconLoader.Companion.getCssColor
import email.schaal.ocreader.util.FaviconLoader.Companion.getDrawable
import email.schaal.ocreader.util.TextDrawable
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Tests for FaviconLoader
 */
@RunWith(AndroidJUnit4::class)
class FaviconLoaderTest {
    @Test
    @Throws(Exception::class)
    fun testGetCssColor() {
        val colorPairs: MutableList<Pair<Int, String>> = ArrayList(3)
        colorPairs.add(Pair(Color.argb(30, 30, 30, 30), "rgba(30,30,30,0.12)"))
        colorPairs.add(Pair(Color.argb(0, 0, 0, 0), "rgba(0,0,0,0.00)"))
        colorPairs.add(Pair(Color.argb(255, 255, 255, 255), "rgba(255,255,255,1.00)"))
        for (colorPair in colorPairs) Assert.assertEquals(colorPair.second, getCssColor(colorPair.first))
    }

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