package ceui.lisa.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;

import ceui.lisa.R;
import ceui.lisa.activities.Shaft;
import ceui.lisa.adapters.BaseAdapter;
import ceui.lisa.adapters.IAdapterWithStar;
import ceui.lisa.core.RemoteRepo;
import ceui.lisa.databinding.FragmentBaseListBinding;
import ceui.lisa.databinding.RecyIllustStaggerBinding;
import ceui.lisa.model.ListIllust;
import ceui.lisa.models.IllustsBean;
import ceui.lisa.notification.BaseReceiver;
import ceui.lisa.notification.FilterReceiver;
import ceui.lisa.repo.LikeIllustRepo;
import ceui.lisa.utils.Params;
import ceui.pixiv.session.SessionManager;

/**
 * 某人收藏的插畫
 */
public class FragmentLikeIllust extends NetListFragment<FragmentBaseListBinding,
        ListIllust, IllustsBean> {

    private int userID;
    private String starType, tag = "";
    private boolean showToolbar = false;
    private BroadcastReceiver filterReceiver;

    public static FragmentLikeIllust newInstance(int userID, String starType) {
        return newInstance(userID, starType, false);
    }

    public static FragmentLikeIllust newInstance(int userID, String starType,
                                                 boolean paramShowToolbar) {
        Bundle args = new Bundle();
        args.putInt(Params.USER_ID, userID);
        args.putString(Params.STAR_TYPE, starType);
        args.putBoolean(Params.FLAG, paramShowToolbar);
        FragmentLikeIllust fragment = new FragmentLikeIllust();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void initView() {
        super.initView();
    }

    @Override
    public void initBundle(Bundle bundle) {
        userID = bundle.getInt(Params.USER_ID);
        starType = bundle.getString(Params.STAR_TYPE);
        showToolbar = bundle.getBoolean(Params.FLAG);
    }

    @Override
    public RemoteRepo<ListIllust> repository() {
        return new LikeIllustRepo(userID, starType, tag);
    }

    @Override
    public BaseAdapter<IllustsBean, RecyIllustStaggerBinding> adapter() {
        boolean isOwnPage = (int) SessionManager.INSTANCE.getLoggedInUid() == userID;
        return new IAdapterWithStar(allItems, mContext).setHideStarIcon(
                isOwnPage && Shaft.sSettings.isHideStarButtonAtMyCollection()
        );
    }

    @Override
    public void onAdapterPrepared() {
        super.onAdapterPrepared();
        IntentFilter intentFilter = new IntentFilter();
        filterReceiver = new FilterReceiver(new BaseReceiver.CallBack() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    String type = bundle.getString(Params.STAR_TYPE);
                    if (starType.equals(type)) {
                        tag = bundle.getString(Params.CONTENT);
                        ((LikeIllustRepo) mRemoteRepo).setTag(tag);
                        baseBind.refreshLayout.autoRefresh();
                    }
                }
            }
        });
        intentFilter.addAction(Params.FILTER_ILLUST);
        LocalBroadcastManager.getInstance(mContext).registerReceiver(filterReceiver, intentFilter);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (filterReceiver != null) {
            LocalBroadcastManager.getInstance(mContext).unregisterReceiver(filterReceiver);
        }
    }

    @Override
    public void beforeFirstLoad(List<IllustsBean> items) {
        if (Shaft.sSettings.isFilterInvalidBookmarks()) {
            items.removeIf(illust -> !illust.isVisible() || illust.getUser() == null || illust.getUser().getId() == 0);
        }
    }

    @Override
    public void beforeNextLoad(List<IllustsBean> items) {
        if (Shaft.sSettings.isFilterInvalidBookmarks()) {
            items.removeIf(illust -> !illust.isVisible() || illust.getUser() == null || illust.getUser().getId() == 0);
        }
    }

    @Override
    public void initRecyclerView() {
        staggerRecyclerView();
    }

    @Override
    public boolean showToolbar() {
        return showToolbar;
    }

    @Override
    public String getToolbarTitle() {
        return showToolbar ? getString(R.string.string_164) : super.getToolbarTitle();
    }

    @Override
    protected FragmentBaseListBinding onCreateBinding(
        @NonNull LayoutInflater inflater,
        ViewGroup container,
        boolean attachToParent
    ) {
        return FragmentBaseListBinding.inflate(inflater, container, false);
    }
}
