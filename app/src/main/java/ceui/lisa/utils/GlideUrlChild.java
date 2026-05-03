package ceui.lisa.utils;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.Headers;

import java.util.HashMap;

import ceui.lisa.activities.Shaft;
import ceui.lisa.http.PixivHeaders;

public class GlideUrlChild extends GlideUrl {

    private static final String PXIMG_HOST = "https://i.pximg.net";

    public GlideUrlChild(String url) {
        this(applyProxy(url), formatHeader());
    }

    public GlideUrlChild(String url, Headers headers) {
        super(url, headers);
    }

    private static String applyProxy(String url) {
        if (url == null || !url.startsWith(PXIMG_HOST)) {
            return url;
        }
        if (!Shaft.sSettings.isUsePixivCat()) {
            return url;
        }
        String proxyUrl = Shaft.sSettings.getImageProxyUrl();
        if (proxyUrl == null || proxyUrl.isEmpty()) {
            return url;
        }
        if (proxyUrl.endsWith("/")) {
            proxyUrl = proxyUrl.substring(0, proxyUrl.length() - 1);
        }
        return proxyUrl + url.substring(PXIMG_HOST.length());
    }

    private static Headers formatHeader() {
        PixivHeaders pixivHeaders = new PixivHeaders();
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(Params.MAP_KEY_SMALL, Params.IMAGE_REFERER);
        hashMap.put("x-client-time", pixivHeaders.getXClientTime());
        hashMap.put("x-client-hash", pixivHeaders.getXClientHash());
        hashMap.put(Params.USER_AGENT, Params.PHONE_MODEL);
        return () -> hashMap;
    }
}
