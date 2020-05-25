package email.schaal.ocreader

import android.graphics.Color
import android.util.Pair
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import email.schaal.ocreader.database.model.Item
import email.schaal.ocreader.util.asCssString
import email.schaal.ocreader.util.cleanString
import email.schaal.ocreader.util.getByLine
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Test class for StringUtils
 */
@RunWith(AndroidJUnit4::class)
class StringUtilsTest {
    @Test
    @Throws(Exception::class)
    fun testGetCssColor() {
        val colorPairs: MutableList<Pair<Int, String>> = ArrayList(3)
        colorPairs.add(Pair(Color.argb(30, 30, 30, 30), "rgba(30,30,30,0.12)"))
        colorPairs.add(Pair(Color.argb(0, 0, 0, 0), "rgba(0,0,0,0.00)"))
        colorPairs.add(Pair(Color.argb(255, 255, 255, 255), "rgba(255,255,255,1.00)"))
        for (colorPair in colorPairs) Assert.assertEquals(colorPair.second, colorPair.first.asCssString())
    }

    @Test
    @Throws(Exception::class)
    fun testGetByLine() {
        val item = Item()
        Assert.assertEquals("", getByLine(ApplicationProvider.getApplicationContext(), "<p class=\"byline\">%s</p>", null, item.feed))
        Assert.assertEquals("<p class=\"byline\">by testAuthor</p>", getByLine(ApplicationProvider.getApplicationContext(), "<p class=\"byline\">%s</p>", "testAuthor", item.feed))
    }

    @Test
    @Throws(Exception::class)
    fun testCleanString() {
        val html = "<span>Test</span>"
        val entity = "Test &gt; Test"
        Assert.assertEquals("Test", html.cleanString())
        Assert.assertEquals("Test > Test", entity.cleanString())
    }
}