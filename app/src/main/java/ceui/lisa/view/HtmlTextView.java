package ceui.lisa.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatTextView;

import java.io.IOException;
import java.io.InputStream;

public class HtmlTextView extends AppCompatTextView {

    public static final String TAG = "HtmlTextView";

    public HtmlTextView(Context context) {
        super(context);
    }

    public HtmlTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HtmlTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setHtml(String html) {
        if (html == null) {
            setText("");
            return;
        }
        Html.ImageGetter imageGetter = new AssetsImageGetter(getContext());
        CharSequence parsed = Html.fromHtml(html, imageGetter, null);
        setText(parsed);
    }

    private static class AssetsImageGetter implements Html.ImageGetter {

        private final Context context;

        AssetsImageGetter(Context context) {
            this.context = context;
        }

        @Override
        public Drawable getDrawable(String source) {
            if (source == null) {
                return null;
            }
            try {
                InputStream inputStream = context.getAssets().open(source);
                Drawable d = Drawable.createFromStream(inputStream, null);
                if (d != null) {
                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                }
                return d;
            } catch (IOException e) {
                Log.e(TAG, "source could not be found: " + source);
                return null;
            }
        }
    }
}
