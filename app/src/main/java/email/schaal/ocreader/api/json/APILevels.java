package email.schaal.ocreader.api.json;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by daniel on 22.08.16.
 */

public class APILevels {
    public enum Level {
        V12("v1-2");

        private final String level;

        public String getLevel() {
            return level;
        }

        Level(String level) {
            this.level = level;
        }

        @Nullable
        public static Level get(String level) {
            for(Level supportedLevel: values()) {
                if(supportedLevel.level.equals(level))
                    return supportedLevel;
            }
            return null;
        }
    }

    private List<String> apiLevels;

    @Nullable
    public Level highestSupportedApi() {
        for(Level level : Level.values())
            if(apiLevels.contains(level.getLevel()))
                return level;

        return null;
    }

    public void setApiLevels(List<String> apiLevels) {
        this.apiLevels = apiLevels;
    }
}
