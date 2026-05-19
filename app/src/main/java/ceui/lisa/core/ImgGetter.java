package ceui.lisa.core;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

import ceui.lisa.utils.Common;
import ceui.lisa.view.HtmlTextView;

public class ImgGetter implements Html.ImageGetter {

    private final Context mContext;
    public static final int BOUND = 54;

    public ImgGetter(Context context) {
        this.mContext = context;
    }

    public ImgGetter(TextView textView) {
        this.mContext = textView.getContext();
    }

    @Override
    public Drawable getDrawable(String source) {

        try {
            InputStream inputStream = mContext.getAssets().open(source);
            Drawable d = Drawable.createFromStream(inputStream, null);
            d.setBounds(0, 0, BOUND, BOUND);
            Common.showLog("wid: " + d.getIntrinsicWidth() + " heightL: " + d.getIntrinsicHeight());
            return d;
        } catch (IOException e) {
            Log.e(HtmlTextView.TAG, "source could not be found: " + source);
            return null;
        }
    }
}
