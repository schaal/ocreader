package email.schaal.ocreader.api.json;

import android.support.annotation.Nullable;

import java.util.List;

import email.schaal.ocreader.api.Level;

/**
 * API response containing supported API levels
 */
public class APILevels {

    private List<String> apiLevels;

    @Nullable
    public Level highestSupportedApi() {
        for(Level level : Level.values())
            if(level.isSupported() && apiLevels.contains(level.getLevel()))
                return level;

        return null;
    }

    public void setApiLevels(List<String> apiLevels) {
        this.apiLevels = apiLevels;
    }
}
