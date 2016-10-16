package email.schaal.ocreader.util;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;

import java.util.HashMap;
import java.util.Map;

/**
 * Generate text and background color for Feeds
 */
public class FeedColors {
    private final Map<Type, Integer> colorMap = new HashMap<>(Type.values().length);

    public enum Type {
        TEXT,
        BACKGROUND
    }

    FeedColors(@NonNull Palette palette) {
        colorMap.put(Type.TEXT, loadColor(palette.getDominantSwatch()));
        colorMap.put(Type.BACKGROUND, loadColor(palette.getMutedSwatch()));
    }

    FeedColors(@Nullable @ColorInt Integer color) {
        colorMap.put(Type.BACKGROUND, color);
        colorMap.put(Type.TEXT, color);
    }

    private Integer loadColor(Palette.Swatch swatch) {
        return swatch != null ? swatch.getRgb() : null;
    }

    public @ColorInt int getColor(@NonNull Type type, @ColorInt int defaultColor) {
        final Integer color = colorMap.get(type);
        return color != null ? color : defaultColor;
    }
}
