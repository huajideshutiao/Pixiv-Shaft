package ceui.lisa.fragments;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import ceui.lisa.R;
import ceui.lisa.adapters.BaseAdapter;
import ceui.lisa.adapters.PivisionHAdapter;
import ceui.lisa.core.BaseRepo;
import ceui.lisa.databinding.FragmentPivisionHorizontalBinding;
import ceui.lisa.databinding.RecyArticalHorizonBinding;
import ceui.lisa.interfaces.OnItemClickListener;
import ceui.lisa.model.ListArticle;
import ceui.lisa.models.SpotlightArticlesBean;
import ceui.lisa.repo.PivisionRepo;
import ceui.lisa.utils.DensityUtil;
import ceui.lisa.utils.Params;
import ceui.lisa.view.LinearItemHorizontalDecoration;
import ceui.pixiv.route.AppRoute;
import jp.wasabeef.recyclerview.animators.BaseItemAnimator;

public class FragmentPivisionHorizontal extends NetListFragment<FragmentPivisionHorizontalBinding,
        ListArticle, SpotlightArticlesBean> {

    @Override
    public BaseAdapter<SpotlightArticlesBean, RecyArticalHorizonBinding> adapter() {
        return new PivisionHAdapter(allItems, mContext).setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View v, int position, int viewType) {
                new AppRoute.WebLink(allItems.get(position).getArticle_url(), null).start(mContext);
            }
        });
    }

    @Override
    public void initLayout() {
        mLayoutID = R.layout.fragment_pivision_horizontal;
    }

    @Override
    public BaseRepo repository() {
        return new PivisionRepo("all", true);
    }

    @Override
    public void initRecyclerView() {
        baseBind.recyclerView.addItemDecoration(new LinearItemHorizontalDecoration(DensityUtil.dp2px(12.0f)));
        LinearLayoutManager manager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
        baseBind.recyclerView.setLayoutManager(manager);
        baseBind.recyclerView.setHasFixedSize(true);
        ViewGroup.LayoutParams layoutParams = baseBind.recyclerView.getLayoutParams();
        layoutParams.width = MATCH_PARENT;
        layoutParams.height =
                mContext.getResources().getDimensionPixelSize(R.dimen.article_horizontal_height) +
                mContext.getResources().getDimensionPixelSize(R.dimen.twenty_four_dp);
        baseBind.recyclerView.setLayoutParams(layoutParams);
    }

    @Override
    public void initView() {
        super.initView();
        baseBind.seeMore.setOnClickListener(v -> {
            AppRoute.Pv.INSTANCE.start(mContext);
        });
    }

    @Override
    public BaseItemAnimator animation() {
        return null;
    }

    @Override
    public void onFirstLoaded(List<SpotlightArticlesBean> spotlightArticlesBeans) {
        mRefreshLayout.setEnableRefresh(true);
        mRefreshLayout.setEnableLoadMore(false);
    }

    @Override
    public void showDataBase() {
        baseBind.refreshLayout.finishRefresh(true);
        emptyRela.setVisibility(View.VISIBLE);
    }

    @Override
    protected FragmentPivisionHorizontalBinding onCreateBinding(
        @NonNull LayoutInflater inflater,
        ViewGroup container,
        boolean attachToParent
    ) {
        return FragmentPivisionHorizontalBinding.inflate(inflater, container, false);
    }
}
