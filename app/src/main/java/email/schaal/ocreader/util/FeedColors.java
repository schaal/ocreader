package email.schaal.ocreader.util;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.ColorUtils;
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

    public FeedColors(@NonNull Palette palette) {
        colorMap.put(Type.TEXT, initTextColor(palette));
        colorMap.put(Type.BACKGROUND, initBackgroundColor(palette));
    }

    public FeedColors(@ColorInt int backgroundColor) {
        colorMap.put(Type.BACKGROUND, backgroundColor);
        colorMap.put(Type.TEXT, backgroundColor);
    }

    private Integer initBackgroundColor(Palette palette) {
        Integer color = null;
        Palette.Swatch swatch = palette.getLightMutedSwatch();
        if(swatch != null) {
            color = swatch.getRgb();
        }
        return color;
    }

    private Integer initTextColor(Palette palette) {
        Integer color = null;
        Palette.Swatch swatch = palette.getDarkVibrantSwatch();
        if(swatch == null) {
            swatch = palette.getVibrantSwatch();
            if(swatch != null) {
                final float[] newHsl = new float[3];
                System.arraycopy(swatch.getHsl(), 0, newHsl, 0, 2);
                newHsl[2] = TARGET_DARK_LUMA;
                color = ColorUtils.HSLToColor(newHsl);
            }
        } else {
            color = swatch.getRgb();
        }
        return color;
    }

    public static int get(@Nullable FeedColors feedColors, @NonNull Type type, @ColorInt int defaultColor) {
        if(feedColors != null) {
            Integer color = feedColors.colorMap.get(type);
            return color != null ? color : defaultColor;
        }
        return defaultColor;
    }

    private static final float TARGET_DARK_LUMA = 0.26f;
}
