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

    internal constructor(palette: Palette) {
        this.palette = palette
    }

    internal constructor(@ColorInt color: Int?) {
        val swatch: Swatch? = if (color != null) {
            Swatch(color, 1)
        } else {
            null
        }
        palette = if (swatch != null) Palette.Builder(listOf(swatch)).addTarget(Target.MUTED).generate() else null
    }

    @ColorInt
    fun getColor(type: Type, @ColorInt defaultColor: Int): Int {
        val swatch: Swatch? = if (palette != null) {
            when (type) {
                Type.TEXT -> palette.dominantSwatch
                Type.BACKGROUND -> palette.mutedSwatch
            }
        } else {
            null
        }
        return swatch?.rgb ?: defaultColor
    }
}