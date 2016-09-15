package email.schaal.ocreader.api.json;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by daniel on 22.08.16.
 */

public class APILevels {
    public enum Level {
        V2("v2", false),
        V12("v1-2", true);

        private final String level;
        private final boolean supported;

        public String getLevel() {
            return level;
        }

        Level(String level, boolean supported) {
            this.level = level;
            this.supported = supported;
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
            if(level.supported && apiLevels.contains(level.getLevel()))
                return level;

        return null;
    }

    public void setApiLevels(List<String> apiLevels) {
        this.apiLevels = apiLevels;
    }
}
