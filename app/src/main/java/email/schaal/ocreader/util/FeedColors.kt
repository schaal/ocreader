package email.schaal.ocreader.util

import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.Swatch
import androidx.palette.graphics.Target

/**
 * Generate text and background color for Feeds
 */
class FeedColors {
    private val palette: Palette?

    enum class Type {
        TEXT, BACKGROUND
    }

    internal constructor(palette: Palette?) {
        this.palette = palette
    }

    internal constructor(@ColorInt color: Int?) {
        val swatch: Swatch? = color?.let { Swatch(it, 1) }
        palette = swatch?.let { Palette.Builder(listOf(it)).addTarget(Target.MUTED).generate() }
    }

    @ColorInt
    fun getColor(type: Type, @ColorInt defaultColor: Int): Int {
        val swatch: Swatch? = palette?.let {
            when (type) {
                Type.TEXT -> it.dominantSwatch
                Type.BACKGROUND -> it.mutedSwatch
            }
        }
        return swatch?.rgb ?: defaultColor
    }
}