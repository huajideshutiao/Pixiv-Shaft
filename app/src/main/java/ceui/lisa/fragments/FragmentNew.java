package ceui.lisa.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.blankj.utilcode.util.BarUtils;

import android.view.LayoutInflater;

import ceui.lisa.R;
import ceui.lisa.activities.Shaft;
import ceui.lisa.databinding.ViewpagerWithTablayoutBinding;
import ceui.lisa.utils.MyOnTabSelectedListener;

public class FragmentNew extends BaseFragment<ViewpagerWithTablayoutBinding> {

    @Override
    public void initLayout() {
        mLayoutID = R.layout.viewpager_with_tablayout;
    }

    @Override
    public void initView() {
        baseBind.placeHolder.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams p = baseBind.placeHolder.getLayoutParams();
        p.height = BarUtils.getStatusBarHeight();
        baseBind.placeHolder.setLayoutParams(p);

        String[] CHINESE_TITLES = new String[]{
                Shaft.getContext().getString(R.string.type_illust),
                Shaft.getContext().getString(R.string.type_manga),
                Shaft.getContext().getString(R.string.type_novel)
        };
        final Fragment[] mFragments = new Fragment[]{
                FragmentLatestWorks.newInstance("illust"),
                FragmentLatestWorks.newInstance("manga"),
                new FragmentLatestNovel()
        };
        baseBind.toolbar.setNavigationOnClickListener(v -> mActivity.finish());
        baseBind.toolbarTitle.setText(R.string.string_204);
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
                Fragment fragment = (Fragment) super.instantiateItem(container, position);
                if (position < mFragments.length) {
                    mFragments[position] = fragment;
                }
                return fragment;
            }

            @NonNull
            @Override
            public Fragment getItem(int position) {
                return mFragments[position];
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
        MyOnTabSelectedListener listener = new MyOnTabSelectedListener(mFragments);
        baseBind.tabLayout.addOnTabSelectedListener(listener);
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
