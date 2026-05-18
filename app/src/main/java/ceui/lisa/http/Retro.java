package ceui.lisa.http;

import static ceui.lisa.http.AppApi.API_BASE_URL;
import static ceui.lisa.http.ResourceApi.JSDELIVR_BASE_URL;
import static ceui.lisa.http.SignApi.SIGN_API;

import android.util.Log;

import com.blankj.utilcode.util.DeviceUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Collections;

import ceui.lisa.activities.Shaft;
import ceui.lisa.helper.LanguageHelper;
import ceui.pixiv.session.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Retro {

    private static final String TAG = "Retro";

    /**
     * @return AppApi the api that the request needed
     * <p>
     * Configued in {@link AppApi}
     * </p>
     * <p>
     * get() returns a Java interface to HTTP calls,and then it .create(),creates the AppApi
     * </p>
     */
    public static AppApi getAppApi() {
        return get().create(AppApi.class);
    }

    public static LofterApi getLofterApi() {
        return get().create(LofterApi.class);
    }

    public static void refreshAppApi() {
        Holder.appRetrofit = buildRetrofit(API_BASE_URL);
    }

    public static SignApi getSignApi() {
        return buildRetrofit(SIGN_API).create(SignApi.class);
    }

    public static AccountTokenApi getAccountTokenApi() {
        return buildRetrofit(AccountTokenApi.ACCOUNT_BASE_URL).create(AccountTokenApi.class);
    }

    public static ResourceApi getResourceApi() {
        return buildPlainRetrofit(JSDELIVR_BASE_URL).create(ResourceApi.class);
    }

    private static Request.Builder addHeader(Request.Builder before) {
        PixivHeaders pixivHeaders = new PixivHeaders();
        String osVersion = DeviceUtils.getSDKVersionName();
        String phoneName = DeviceUtils.getModel();
        before.addHeader("User-Agent", "PixivAndroidApp/5.0.234 (Android " + osVersion + "; " + phoneName + ")")
                .addHeader("accept-language", LanguageHelper.getRequestHeaderAcceptLanguageFromAppLanguage())
                .addHeader("x-client-time", pixivHeaders.getXClientTime())
                .addHeader("x-client-hash", pixivHeaders.getXClientHash());
        return before;
    }

    private static OkHttpClient.Builder applyDirectConnect(OkHttpClient.Builder before, boolean enable) {
        if (enable && Shaft.sSettings.isDirectConnect()) {
            before.addInterceptor(new CronetInterceptor(CronetInterceptor.getEngine(Shaft.getContext())));
        }
        return before;
    }

    /**
     * @param baseUrl The base url
     *                <p>
     *                For example:
     *                </p>
     *                <p>
     *                String API_BASE_URL = "https://app-api.pixiv.net/";in {@link AppApi}
     *                </p>
     *
     */
    private static Retrofit buildRetrofit(String baseUrl) {
        return buildRetrofit(baseUrl, true);
    }

    /**
     * Retrofit: A Type-Safe HTTP Client for Android and JVM
     * Retrofit is a popular and powerful type-safe HTTP client library for Android and Java Virtual Machine (JVM) applications. It simplifies the process of making network requests by converting your REST API endpoints into Java interfaces.
     *
     * @param baseUrl       The base URL
     * @param directConnect auto
     *
     */
    private static Retrofit buildRetrofit(String baseUrl, boolean directConnect) {
        OkHttpClient.Builder builder = getLogClient();
        
        // 1. 先加日志，确保能看到被 Cronet 接管前的完整请求
        okhttp3.logging.HttpLoggingInterceptor l = new okhttp3.logging.HttpLoggingInterceptor(
                message -> android.util.Log.i(TAG, message));
        l.setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BASIC);
        builder.addInterceptor(l);

        try {
            // 2. 加 Header
            builder.addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder reqBuilder = addHeader(original.newBuilder());
                if (original.header("Authorization") == null) {
                    String bearerToken = SessionManager.INSTANCE.getBearerTokenOrEmpty();
                    if (!bearerToken.isEmpty()) {
                        reqBuilder.addHeader("Authorization", bearerToken);
                    }
                }
                return chain.proceed(reqBuilder.build());
            });
            // 3. 加 Token 刷新
            builder.addInterceptor(new TokenInterceptor());
        } catch (Exception e) {
            android.util.Log.e(TAG, "buildRetrofit interceptor error", e);
        }
        
        // 4. 最后应用直连（Cronet 拦截器必须在最后，因为它会中断 OkHttp 链条）
        applyDirectConnect(builder, directConnect);

        OkHttpClient client = builder.build();
        Gson gson = new GsonBuilder().setLenient().create();
        return new Retrofit.Builder()
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(baseUrl)
                .build();
    }

    private static Retrofit buildPlainRetrofit(String baseUrl) {
        OkHttpClient.Builder builder = getLogClient();
        OkHttpClient client = builder.build();
        return new Retrofit.Builder()
                .client(client)
                .baseUrl(baseUrl)
                .build();
    }

    public static <T> T create(String baseUrl, final Class<T> service) {
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit retrofit = new Retrofit.Builder()
                .client(
                        getLogClient().addInterceptor(chain -> {
                            Request localRequest = chain.request().newBuilder()
                                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.80 Safari/537.36")
                                    .addHeader("Accept-Encoding:", "gzip, deflate")
                                    .addHeader("Accept:", "text/html")
                                    .build();
                            return chain.proceed(localRequest);
                        }).build()
                )
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(baseUrl)
                .build();
        return retrofit.create(service);
    }

    /**
     * @return The static Retrofit
     * <p>
     * @see Retrofit retrofit2.Retrofit
     * </p>
     */
    private static Retrofit get() {
        return Holder.appRetrofit;
    }

    public static OkHttpClient.Builder getLogClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1));
    }

    private static class Holder {

        private static final String TAG = "Retro";
        private static Retrofit appRetrofit = buildRetrofit(API_BASE_URL);
    }
}