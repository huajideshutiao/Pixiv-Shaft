package ceui.lisa.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import ceui.lisa.utils.AppKit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.view.LayoutInflater;

import ceui.lisa.R;
import ceui.lisa.activities.Shaft;
import ceui.lisa.databinding.ViewpagerWithTablayoutBinding;
import ceui.lisa.utils.MyOnTabSelectedListener;
import ceui.lisa.utils.Params;
import ceui.pixiv.route.AppRoute;
import ceui.pixiv.session.SessionManager;

public class FragmentCollection extends BaseFragment<ViewpagerWithTablayoutBinding> {

    private Fragment[] allPages;
    private String[] CHINESE_TITLES;

    private int type; //0插画收藏，1小说收藏，2关注, 3追更列表
    private final static Set<Integer> filterType = new HashSet<>(Arrays.asList(0,1));

    public static FragmentCollection newInstance(int type) {
        Bundle args = new Bundle();
        args.putInt(Params.DATA_TYPE, type);
        FragmentCollection fragment = new FragmentCollection();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void initBundle(Bundle bundle) {
        type = bundle.getInt(Params.DATA_TYPE);
    }

    @Override
    public void initLayout() {
        mLayoutID = R.layout.viewpager_with_tablayout;
    }

    @Override
    public void initView() {
        baseBind.placeHolder.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams p = baseBind.placeHolder.getLayoutParams();
        p.height = AppKit.getStatusBarHeight(getContext());
        baseBind.placeHolder.setLayoutParams(p);

        if (type == 0) {
            allPages = new Fragment[]{
                    FragmentLikeIllust.newInstance((int) SessionManager.INSTANCE.getLoggedInUid(),
                            Params.TYPE_PUBLIC),
                    FragmentLikeIllust.newInstance((int) SessionManager.INSTANCE.getLoggedInUid(),
                            Params.TYPE_PRIVATE)
            };
            CHINESE_TITLES = new String[]{
                    Shaft.getContext().getString(R.string.public_like_illust),
                    Shaft.getContext().getString(R.string.private_like_illust)
            };
        } else if (type == 1) {
            allPages = new Fragment[]{
                    FragmentLikeNovel.newInstance((int) SessionManager.INSTANCE.getLoggedInUid(),
                            Params.TYPE_PUBLIC, false),
                    FragmentLikeNovel.newInstance((int) SessionManager.INSTANCE.getLoggedInUid(),
                            Params.TYPE_PRIVATE, false)
            };
            CHINESE_TITLES = new String[]{
                    Shaft.getContext().getString(R.string.public_like_novel),
                    Shaft.getContext().getString(R.string.private_like_novel)
            };
        } else if (type == 2) {
            allPages = new Fragment[]{
                    FragmentFollowUser.newInstance((int) SessionManager.INSTANCE.getLoggedInUid(),
                            Params.TYPE_PUBLIC, false),
                    FragmentFollowUser.newInstance((int) SessionManager.INSTANCE.getLoggedInUid(),
                            Params.TYPE_PRIVATE, false)
            };
            CHINESE_TITLES = new String[]{
                    Shaft.getContext().getString(R.string.public_like_user),
                    Shaft.getContext().getString(R.string.private_like_user)
            };
        } else if (type == 3) {
            allPages = new Fragment[]{
                    new FragmentWatchlistManga(),
                    new FragmentWatchlistNovel()
            };
            CHINESE_TITLES = new String[]{
                    Shaft.getContext().getString(R.string.type_manga),
                    Shaft.getContext().getString(R.string.type_novel)
            };
        }

        if (type == 0) {
            baseBind.toolbarTitle.setText(R.string.string_319);
        } else if (type == 1) {
            baseBind.toolbarTitle.setText(R.string.string_320);
        } else if (type == 2) {
            baseBind.toolbarTitle.setText(R.string.string_321);
        } else if (type == 3) {
            baseBind.toolbarTitle.setText(R.string.watchlist);
        }
        baseBind.toolbar.setNavigationOnClickListener(v -> mActivity.finish());
        baseBind.toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (baseBind.viewPager.getCurrentItem() == 0) {
                    new AppRoute.BookedTag(type, Params.TYPE_PUBLIC).start(mContext);
                    return true;
                } else if (baseBind.viewPager.getCurrentItem() == 1) {
                    new AppRoute.BookedTag(type, Params.TYPE_PRIVATE).start(mContext);
                    return true;
                }
                return false;
            }
        });
        baseBind.viewPager.setAdapter(new FragmentStatePagerAdapter(
            getChildFragmentManager(),
            FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
            @Override
            public void restoreState(Parcelable state, ClassLoader loader) {
                if (state instanceof Bundle) {
                    Bundle bundle = (Bundle) state;
                    bundle.setClassLoader(loader);
                    for (String key : bundle.keySet()) {
                        if (key.startsWith("f")) {
                            try {
                                Fragment f = getChildFragmentManager().getFragment(bundle, key);
                                if (f == null) {
                                    bundle.remove(key);
                                }
                            } catch (Exception e) {
                                bundle.remove(key);
                            }
                        }
                    }
                }
                try {
                    super.restoreState(state, loader);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                NetListFragment fragment = (NetListFragment) super.instantiateItem(container, position);
                if (allPages != null && position < allPages.length) {
                    allPages[position] = fragment;
                }
                return fragment;
            }

            @NonNull
            @Override
            public Fragment getItem(int i) {
                return allPages[i];
            }

            @Override
            public int getCount() {
                return CHINESE_TITLES.length;
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                return CHINESE_TITLES[position];
            }
        });
        baseBind.tabLayout.setupWithViewPager(baseBind.viewPager);
        MyOnTabSelectedListener listener = new MyOnTabSelectedListener(allPages);
        baseBind.tabLayout.addOnTabSelectedListener(listener);
        if (filterType.contains(type)) {
            baseBind.toolbar.inflateMenu(R.menu.illust_filter);
        }
        baseBind.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                baseBind.toolbar.getMenu().clear();
                if (filterType.contains(type)) {
                    baseBind.toolbar.inflateMenu(R.menu.illust_filter);
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    @Override
    protected ViewpagerWithTablayoutBinding onCreateBinding(
        @NonNull LayoutInflater inflater,
        ViewGroup container,
        boolean attachToParent
    ) {
        return ViewpagerWithTablayoutBinding.inflate(inflater, container, false);
    }
}
