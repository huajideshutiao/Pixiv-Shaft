package ceui.lisa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import java.util.List;

import ceui.lisa.R;
import ceui.lisa.core.ArtworksMap;
import ceui.lisa.core.Container;
import ceui.lisa.core.Mapper;
import ceui.lisa.core.PageData;
import ceui.lisa.databinding.ActivityViewPagerBinding;
import ceui.lisa.fragments.FragmentImageDetail;
import ceui.lisa.fragments.FragmentSingleIllust;
import ceui.lisa.fragments.FragmentSingleUgora;
import ceui.lisa.helper.DeduplicateArrayList;
import ceui.lisa.http.NullCtrl;
import ceui.lisa.http.Retro;
import ceui.lisa.model.ListIllust;
import ceui.lisa.models.IllustsBean;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.Params;
import ceui.lisa.utils.PixivOperate;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class VActivity extends BaseActivity<ActivityViewPagerBinding> {

    private String pageUUID = "";
    private int index = 0;
    private String seed = "";

    @Override
    protected void initBundle(Bundle bundle) {
        pageUUID = bundle.getString(Params.PAGE_UUID);
        index = bundle.getInt(Params.POSITION);
        seed = bundle.getString(Params.SEED);
    }

    @Override
    protected int initLayout() {
        return R.layout.activity_view_pager;
    }

    @Override
    protected void initView() {
        if (!TextUtils.isEmpty(seed)) {
            setupFromSeed();
        } else {
            setupFromPageData();
        }
    }

    private void setupFromSeed() {
        List<Long> ids = ArtworksMap.INSTANCE.getStore().get(seed);
        if (ids == null || ids.isEmpty()) {
            finish();
            return;
        }

        baseBind.viewPager.setAdapter(new FragmentStatePagerAdapter(
            getSupportFragmentManager(),
            0
        ) {
            @NonNull
            @Override
            public Fragment getItem(int position) {
                long illustId = ids.get(position);
                return FragmentSingleIllust.newInstance(illustId);
            }

            @Override
            public int getCount() {
                return ids.size();
            }

            @Nullable
            @org.jetbrains.annotations.Nullable
            @Override
            public Parcelable saveState() {
                Bundle bundle = (Bundle) super.saveState();
                if (bundle != null) {
                    bundle.putParcelableArray("states", null);
                }
                return bundle;
            }
        });
        baseBind.viewPager.setOffscreenPageLimit(1);

        int pos = -1;
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i) == index) {
                pos = i;
                break;
            }
        }
        if (pos > 0) {
            baseBind.viewPager.setCurrentItem(pos);
        }
    }

    private void setupFromPageData() {
        PageData pageData = Container.get().getPage(pageUUID);
        if (pageData != null) {
            baseBind.viewPager.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager(), 0) {
                @NonNull
                @Override
                public Fragment getItem(int position) {
                    IllustsBean illustsBean = pageData.getList().get(position);
                    if (illustsBean.getId() == 0 || !illustsBean.isVisible()) {
                        return FragmentImageDetail.newInstance(illustsBean.getImage_urls().getMaxImage());
                    } else if (illustsBean.isGif()) {
                        return FragmentSingleUgora.newInstance(illustsBean);
                    } else {
                        return FragmentSingleIllust.newInstance(illustsBean);
                    }
                }

                @Override
                public int getCount() {
                    return pageData.getList().size();
                }

                @Nullable
                @org.jetbrains.annotations.Nullable
                @Override
                public Parcelable saveState() {
                    Bundle bundle = (Bundle) super.saveState();
                    if (bundle != null) {
                        bundle.putParcelableArray("states", null);
                    }
                    return bundle;
                }
            });
            baseBind.viewPager.setOffscreenPageLimit(1);

            ViewPager.OnPageChangeListener listener = new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    Common.showLog("Container onPageSelected " + position);
                    if (Common.isEmpty(pageData.getList())) {
                        return;
                    }

                    if (position >= pageData.getList().size()) {
                        return;
                    }

                    if (Shaft.sSettings.isSaveViewHistory()) {
                        PixivOperate.insertIllustViewHistory(pageData.getList().get(position));
                    }

                    if (position == (pageData.getList().size() - 1) || position == (pageData.getList().size() - 2)) {
                        String nextUrl = pageData.getNextUrl();
                        if (!TextUtils.isEmpty(nextUrl)) {
                            if (!Container.get().isNetworking()) {
                                Common.showLog("Container 去请求下一页 " + nextUrl);
                                Retro.getAppApi().getNextIllust(nextUrl)
                                    .subscribeOn(io.reactivex.schedulers.Schedulers.newThread())
                                        .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new NullCtrl<>() {
                                            @Override
                                            public void success(ListIllust listIllust) {
                                                Mapper<ListIllust> mapper = new Mapper<>();
                                                listIllust = mapper.apply(listIllust);
                                                Common.showLog("Container 下一页请求成功 ");
                                                Intent intent =
                                                    new Intent(Params.FRAGMENT_ADD_DATA);
                                                intent.putExtra(Params.PAGE_UUID, pageUUID);
                                                intent.putExtra(Params.CONTENT, listIllust);
                                                LocalBroadcastManager.getInstance(Shaft.getContext())
                                                    .sendBroadcast(intent);

                                                DeduplicateArrayList.addAllWithNoRepeat(
                                                    pageData.getList(),
                                                    listIllust.getList()
                                                );
                                                pageData.setNextUrl(listIllust.getNextUrl());
                                                if (baseBind.viewPager.getAdapter() != null) {
                                                    baseBind.viewPager.getAdapter()
                                                        .notifyDataSetChanged();
                                                }
                                            }

                                            @Override
                                            public void must() {
                                                super.must();
                                                Container.get().setNetworking(false);
                                            }

                                            @Override
                                            public void subscribe(io.reactivex.disposables.Disposable d) {
                                                super.subscribe(d);
                                                Container.get().setNetworking(true);
                                            }
                                        });
                            } else {
                                Common.showLog("Container 不去请求下一页 00");
                            }
                        } else {
                            Common.showLog("Container 不去请求下一页 11");
                        }
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            };
            baseBind.viewPager.addOnPageChangeListener(listener);

            if (index < pageData.getList().size()) {
                baseBind.viewPager.setCurrentItem(index);
            }

            if (index == 0) {
                baseBind.viewPager.post(() -> listener.onPageSelected(baseBind.viewPager.getCurrentItem()));
            }
        } else {
            finish();
        }
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void onDestroy() {
        PixivOperate.clearBack();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (TextUtils.isEmpty(seed)) {
            Intent intent = new Intent(Params.FRAGMENT_SCROLL_TO_POSITION);
            intent.putExtra(Params.INDEX, baseBind.viewPager.getCurrentItem());
            intent.putExtra(Params.PAGE_UUID, pageUUID);
            LocalBroadcastManager.getInstance(Shaft.getContext()).sendBroadcast(intent);
        }
        super.onPause();
    }

    @Override
    public boolean hideStatusBar() {
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN &&
            (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            androidx.viewpager.widget.ViewPager viewPager = baseBind.viewPager;
            if (viewPager.getAdapter() != null) {
                int currentItem = viewPager.getCurrentItem();
                int nextItem =
                    event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN ? currentItem + 1 : currentItem - 1;
                if (nextItem >= 0 && nextItem < viewPager.getAdapter().getCount()) {
                    viewPager.setCurrentItem(nextItem, true);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
