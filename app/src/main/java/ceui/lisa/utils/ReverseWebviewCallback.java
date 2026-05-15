package ceui.lisa.utils;

import android.content.Context;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import ceui.lisa.activities.MainActivity;
import ceui.pixiv.route.AppRoute;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class ReverseWebviewCallback implements ReverseImage.Callback {

    private final Context mContext;
    private Uri imageUri;

    public ReverseWebviewCallback(Context context) {
        mContext = context;
    }

    public ReverseWebviewCallback(Context context, Uri imageUri) {
        mContext = context;
        this.imageUri = imageUri;
    }

    @Override
    public void onSubscribe() {
        Common.showToast("Loading");
    }

    @Override
    public void onNext(Response<ResponseBody> response) {
        new AppRoute.ReverseSearch(new ReverseResult(response)).start(mContext);
        if (!(mContext instanceof MainActivity)) {
            ((AppCompatActivity) mContext).finish();
        }
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
        Common.showToast(e.getMessage());
    }
}
