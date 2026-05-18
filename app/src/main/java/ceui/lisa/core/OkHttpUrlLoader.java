package ceui.lisa.core;

import androidx.annotation.NonNull;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.InputStream;

public class OkHttpUrlLoader implements ModelLoader<GlideUrl, InputStream> {

    private final Call.Factory client;

    public OkHttpUrlLoader(@NonNull Call.Factory client) {
        this.client = client;
    }

    @Override
    public boolean handles(@NonNull GlideUrl url) {
        return true;
    }

    @Override
    public LoadData<InputStream> buildLoadData(
            @NonNull GlideUrl model, int width, int height, @NonNull Options options) {
        return new LoadData<>(model, new OkHttpStreamFetcher(client, model));
    }

    public static class Factory implements ModelLoaderFactory<GlideUrl, InputStream> {
        private static volatile Call.Factory internalClient;
        private final Call.Factory client;

        private static Call.Factory getInternalClient() {
            if (internalClient == null) {
                synchronized (Factory.class) {
                    if (internalClient == null) {
                        internalClient = new OkHttpClient();
                    }
                }
            }
            return internalClient;
        }

        public Factory() {
            this(getInternalClient());
        }

        public Factory(@NonNull Call.Factory client) {
            this.client = client;
        }

        @NonNull
        @Override
        public ModelLoader<GlideUrl, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
            return new OkHttpUrlLoader(client);
        }

        @Override
        public void teardown() {
        }
    }

    private static class OkHttpStreamFetcher implements com.bumptech.glide.load.data.DataFetcher<InputStream> {
        private final Call.Factory client;
        private final GlideUrl url;
        private volatile Call call;
        private InputStream stream;

        OkHttpStreamFetcher(Call.Factory client, GlideUrl url) {
            this.client = client;
            this.url = url;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
            try {
                Request.Builder requestBuilder = new Request.Builder().url(url.toStringUrl());
                for (String key : url.getHeaders().keySet()) {
                    requestBuilder.addHeader(key, url.getHeaders().get(key));
                }
                call = client.newCall(requestBuilder.build());
                Response response = call.execute();
                ResponseBody responseBody = response.body();
                if (response.isSuccessful()) {
                    if (responseBody != null) {
                        stream = responseBody.byteStream();
                        callback.onDataReady(stream);
                    } else {
                        callback.onLoadFailed(new IOException("Response body is null"));
                    }
                } else {
                    callback.onLoadFailed(new IOException("Request failed with code: " + response.code()));
                }
            } catch (IOException e) {
                callback.onLoadFailed(e);
            }
        }

        @Override
        public void cleanup() {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }

        @Override
        public void cancel() {
            Call localCall = call;
            if (localCall != null) {
                localCall.cancel();
            }
        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public com.bumptech.glide.load.DataSource getDataSource() {
            return com.bumptech.glide.load.DataSource.REMOTE;
        }
    }
}
