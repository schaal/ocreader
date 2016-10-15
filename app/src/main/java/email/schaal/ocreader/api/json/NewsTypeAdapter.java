package email.schaal.ocreader.api.json;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;

import java.io.IOException;

/**
 * Base class for TypeAdapters
 */
abstract class NewsTypeAdapter<T> extends JsonAdapter<T> {
    String nullSafeString(JsonReader in) throws IOException {
        if(in.peek() == JsonReader.Token.NULL) {
            in.nextNull();
            return null;
        } else
            return in.nextString();
    }

    int nullSafeInt(JsonReader in, int def) throws IOException {
        if(in.peek() == JsonReader.Token.NULL) {
            in.nextNull();
            return def;
        } else
            return in.nextInt();
    }

    boolean nullSafeBoolean(JsonReader in, boolean def) throws IOException {
        if(in.peek() == JsonReader.Token.NULL) {
            in.nextNull();
            return def;
        } else
            return in.nextBoolean();
    }
}
