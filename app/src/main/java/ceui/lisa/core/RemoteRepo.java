package ceui.lisa.core;

import ceui.lisa.fragments.NetListFragment;
import ceui.lisa.interfaces.ListShow;
import retrofit2.Call;

import java.util.function.Function;

import static ceui.lisa.core.CallExtKt.executeCall;

/**
 * The class stores response got from remote repo (pixiv) in the form of {@link ListShow}
 * */
public abstract class RemoteRepo<Response extends ListShow<?>> extends BaseRepo {

    private Call<? extends Response> mApi;

    private final Function<Response, Response> mFunction;
    protected String nextUrl = "";

    public RemoteRepo() {
        mFunction = mapper();
    }

    public abstract Call<? extends Response> initApi();

    public abstract Call<? extends Response> initNextApi();

    @SuppressWarnings("unchecked")
    public void getFirstData(NetCallback<Response> callback) {
        mApi = initApi();
        if (mApi != null) {
            executeCall((Call<Response>) mApi, mFunction, callback);
        }
    }

    @SuppressWarnings("unchecked")
    public void getNextData(NetCallback<Response> callback) {
        mApi = initNextApi();
        if (mApi != null) {
            executeCall((Call<Response>) mApi, mFunction, callback);
        }
    }

    public Function<Response, Response> mapper() {
        return new Mapper<>();
    }

    public String getNextUrl() {
        return nextUrl;
    }

    public void setNextUrl(String nextUrl) {
        this.nextUrl = nextUrl;
    }

    public boolean hasEffectiveUserFollowStatus() {
        return true;
    }
}