package email.schaal.ocreader.model;

import android.util.Log;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;

/**
 * Created by daniel on 15.02.16.
 */
public abstract class NewsTypeAdapter<T> extends TypeAdapter<T> {
    private static final String TAG = NewsTypeAdapter.class.getSimpleName();

    protected String nullSafeString(JsonReader in) throws IOException {
        if(in.peek() == JsonToken.NULL) {
            Log.w(TAG, "unexpected NULL in feed json");
            in.nextNull();
            return null;
        } else
            return in.nextString();
    }
}
