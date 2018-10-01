package email.schaal.ocreader.util;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;
import androidx.palette.graphics.Target;

import java.util.Collections;

/**
 * Generate text and background color for Feeds
 */
public class FeedColors {
    @Nullable
    private final Palette palette;

    public enum Type {
        TEXT,
        BACKGROUND
    }

    FeedColors(@NonNull Palette palette) {
        this.palette = palette;
    }

    FeedColors(@Nullable @ColorInt Integer color) {
        final Palette.Swatch swatch;

        if(color != null) {
            swatch = new Palette.Swatch(color, 1);
        } else {
            swatch = null;
        }

        if(swatch != null)
            palette = new Palette.Builder(Collections.singletonList(swatch)).addTarget(Target.MUTED).generate();
        else
            palette = null;
    }

    @ColorInt public int getColor(@NonNull Type type, @ColorInt int defaultColor) {
        final Palette.Swatch swatch;

        if(palette != null) {
            switch (type) {
                case TEXT:
                    swatch = palette.getDominantSwatch();
                    break;
                case BACKGROUND:
                    swatch = palette.getMutedSwatch();
                    break;
                default:
                    swatch = null;
                    break;
            }
        } else {
            swatch = null;
        }

        if(swatch != null)
            return swatch.getRgb();
        else
            return defaultColor;
    }
}
