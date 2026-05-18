package ceui.lisa.http;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ceui.lisa.activities.Shaft;
import ceui.lisa.utils.Common;
import okhttp3.Dns;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import org.jetbrains.annotations.NotNull;

public class HttpDns implements Dns {

    private static final String[] DOH_ENDPOINTS = {
            CloudFlareDNSService.Companion.getCLOUDFLARE_DOH_POINT(),
            CloudFlareDNSService.Companion.getDNSSB_DOH_POINT(),
    };

    private static final String[] DOMAINS = {
            "app-api.pixiv.net",
            "oauth.secure.pixiv.net",
    };

    public static final String[] FALLBACK_API_IPS = {
            CronetInterceptor.CF_IP_PRIMARY,
            CronetInterceptor.CF_IP_SECONDARY,
    };

    public static final String[] FALLBACK_IMAGE_IPS = {
            "210.140.139.134",
            "210.140.139.133",
            "210.140.139.131",
    };

    private final Map<String, List<InetAddress>> resolvedHosts = new ConcurrentHashMap<>();
    private static volatile HttpDns sHttpDns = null;
    private List<InetAddress> fallbackApiAddresses;
    private List<InetAddress> fallbackImageAddresses;

    private HttpDns() {
        fallbackApiAddresses = new ArrayList<>();
        for (String ip : FALLBACK_API_IPS) {
            try {
                fallbackApiAddresses.add(InetAddress.getByName(ip));
            } catch (UnknownHostException ignored) {
            }
        }
        fallbackImageAddresses = new ArrayList<>();
        for (String ip : FALLBACK_IMAGE_IPS) {
            try {
                fallbackImageAddresses.add(InetAddress.getByName(ip));
            } catch (UnknownHostException ignored) {
            }
        }
        if (isSecureDnsEnabled()) {
            for (String domain : DOMAINS) {
                resolveViaDoH(domain, 0);
            }
        }
    }

    public static HttpDns getInstance() {
        if (sHttpDns == null) {
            synchronized (HttpDns.class) {
                if (sHttpDns == null) {
                    sHttpDns = new HttpDns();
                }
            }
        }
        return sHttpDns;
    }

    public static void invalidate() {
        HttpDns instance = sHttpDns;
        if (instance == null) {
            return;
        }
        instance.resolvedHosts.clear();
        if (isSecureDnsEnabled()) {
            for (String domain : DOMAINS) {
                instance.resolveViaDoH(domain, 0);
            }
        }
    }

    private static boolean isSecureDnsEnabled() {
        return Shaft.sSettings != null && Shaft.sSettings.isUseSecureDns();
    }

    private void resolveViaDoH(String hostname, int endpointIndex) {
        if (endpointIndex >= DOH_ENDPOINTS.length) {
            return;
        }
        try {
            CloudFlareDNSService service = CloudFlareDNSService.Companion.invoke(DOH_ENDPOINTS[endpointIndex]);
            service.query(hostname, "A").enqueue(new Callback<CloudFlareDNSResponse>() {
                @Override
                public void onResponse(Call<CloudFlareDNSResponse> call, Response<CloudFlareDNSResponse> response) {
                    CloudFlareDNSResponse body = response.body();
                    if (body != null && !Common.isEmpty(body.getAnswer())) {
                        List<InetAddress> addresses = new ArrayList<>();
                        for (CloudFlareDNSResponse.DNSAnswer answer : body.getAnswer()) {
                            try {
                                if (answer.getType() == 1) {
                                    addresses.add(InetAddress.getByName(answer.getData()));
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        if (!addresses.isEmpty()) {
                            resolvedHosts.put(hostname, addresses);
                        } else {
                            resolveViaDoH(hostname, endpointIndex + 1);
                        }
                    } else {
                        resolveViaDoH(hostname, endpointIndex + 1);
                    }
                }

                @Override
                public void onFailure(Call<CloudFlareDNSResponse> call, Throwable t) {
                    resolveViaDoH(hostname, endpointIndex + 1);
                }
            });
        } catch (Exception e) {
            resolveViaDoH(hostname, endpointIndex + 1);
        }
    }

    @NotNull
    @Override
    public List<InetAddress> lookup(@NotNull String hostname) throws UnknownHostException {
        long start = System.nanoTime();
        if (isSecureDnsEnabled()) {
            List<InetAddress> cached = resolvedHosts.get(hostname);
            if (cached != null && !cached.isEmpty()) {
                long elapsed = (System.nanoTime() - start) / 1_000_000;
                Common.showLog("HttpDns lookup " + hostname + " → DoH cached " + cached + " [" + elapsed + "ms]");
                return cached;
            }
        } else {
            try {
                List<InetAddress> systemResult = Dns.SYSTEM.lookup(hostname);
                if (!systemResult.isEmpty()) {
                    long elapsed = (System.nanoTime() - start) / 1_000_000;
                    Common.showLog("HttpDns lookup " + hostname + " → system " + systemResult + " [" + elapsed + "ms]");
                    return systemResult;
                }
            } catch (UnknownHostException ignored) {
            }
        }
        List<InetAddress> result;
        String source;
        if (hostname.endsWith("pximg.net")) {
            result = fallbackImageAddresses;
            source = "fallback-image";
        } else {
            result = fallbackApiAddresses;
            source = "fallback-api";
        }
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        Common.showLog("HttpDns lookup " + hostname + " → " + source + " " + result + " [" + elapsed + "ms]");
        return result;
    }
}
