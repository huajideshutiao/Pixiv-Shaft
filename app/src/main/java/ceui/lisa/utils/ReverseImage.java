package ceui.lisa.utils;


import android.net.Uri;

import com.blankj.utilcode.util.UriUtils;

import ceui.lisa.http.Ascii2DApi;
import ceui.lisa.http.IqdbApi;
import ceui.lisa.http.Retro;
import ceui.lisa.http.SauceNaoApi;
import ceui.lisa.http.TinEyeApi;
import ceui.lisa.core.ThreadUtil;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

//TinEye会报Internal Server Error,原因暂时不明
//没找到给出的Api,都使用了网页版
//暂时想法是使用webview.load(response),可能有更好的解决方案
public class ReverseImage {
    private static final String IQDB_BASE_URL = "https://iqdb.org/";
    private static final String SAUCENAO_BASE_URL = "https://saucenao.com/";
    private static final String TINEYE_BASE_URL = "https://www.tineye.com/";
    private static final String ASCII2D_BASE_URL = "https://ascii2d.net/";
    public static final long IMAGE_MAX_SIZE = 15 * 1024 * 1024; // SauceNao limit: 15MB
    public static final ReverseProvider DEFAULT_ENGINE = ReverseProvider.SauceNao;

    public static boolean isFileSizeOkToSearch(Uri fileUri, ReverseProvider reverseProvider){
        return Common.isFileSizeOkToReverseSearch(fileUri, IMAGE_MAX_SIZE);
    }

    public static void reverse(Uri imageUri, ReverseProvider reverseProvider, Callback callback) {
        byte[] file = UriUtils.uri2Bytes(imageUri);
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        Object o = Retro.create(reverseProvider.base_url, reverseProvider.apiClass);
        //    enum不能使用泛型,刚好Retrofit的Api Interface 不能继承,搞不了花里胡哨了
        Call<Response<ResponseBody>> call;
        MultipartBody.Part formData;
        switch (reverseProvider.name()) {
            case "Iqdb":
                formData = MultipartBody.Part.createFormData("file", "pixiv_image.png", requestBody);
                call = ((IqdbApi) o).query(formData);
                break;
            case "SauceNao":
                formData = MultipartBody.Part.createFormData("file", "pixiv_image.png", requestBody);
                call = ((SauceNaoApi) o).query(formData);
                break;
            case "TinEye":
                formData = MultipartBody.Part.createFormData("image", "pixiv_image.png", requestBody);
                call = ((TinEyeApi) o).query(formData);
                break;
            case "Ascii2D":
                formData = MultipartBody.Part.createFormData("file", "pixiv_image.png", requestBody);
                call = ((Ascii2DApi) o).query(formData);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + reverseProvider.name());
        }

        callback.onSubscribe();
        ThreadUtil.INSTANCE.runOnIo(() -> {
            try {
                retrofit2.Response<Response<ResponseBody>> rawResp = call.execute();
                ThreadUtil.INSTANCE.runOnMain(() -> {
                    Response<ResponseBody> innerResp = rawResp.body();
                    if (rawResp.isSuccessful() && innerResp != null) {
                        callback.onNext(innerResp);
                    } else {
                        callback.onError(new Exception("HTTP " + rawResp.code() + ": " + rawResp.message()));
                    }
                });
            } catch (Exception e) {
                ThreadUtil.INSTANCE.runOnMain(() -> {
                    callback.onError(e);
                });
            }
        });

    }


    public enum ReverseProvider {
        Iqdb(IQDB_BASE_URL, IqdbApi.class), SauceNao(SAUCENAO_BASE_URL, SauceNaoApi.class), TinEye(TINEYE_BASE_URL, TinEyeApi.class), Ascii2D(ASCII2D_BASE_URL, Ascii2DApi.class);

        public final String base_url;
        public Class<?> apiClass;

        ReverseProvider(String baseUrl, Class<?> apiClass) {
            this.base_url = baseUrl;
            this.apiClass = apiClass;
        }
    }


    public interface Callback {
        void onSubscribe();

        void onNext(Response<ResponseBody> response);

        void onError(Throwable e);
    }
}
