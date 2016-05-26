package email.schaal.ocreader.view;

import android.content.Context;
import android.graphics.Rect;
import android.support.design.widget.TextInputEditText;
import android.util.AttributeSet;

/**
 * TextInputEditText with ability to run a callback to check the entered text if it changed
 */
public class UrlCheckerTextInputEditText extends TextInputEditText {
    private UrlCheckCallback urlCheckCallback;

    public interface UrlCheckCallback {
        void onCheckUrl(CharSequence text);
    }

    public UrlCheckerTextInputEditText(Context context) {
        super(context);
    }

    public UrlCheckerTextInputEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UrlCheckerTextInputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setUrlCheckCallback(UrlCheckCallback urlCheckCallback) {
        this.urlCheckCallback = urlCheckCallback;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if(urlCheckCallback != null)
            urlCheckCallback.onCheckUrl(text);
    }
}
