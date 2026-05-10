package ceui.lisa.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.blankj.utilcode.util.BarUtils;

import java.util.Calendar;

import ceui.lisa.R;
import ceui.lisa.databinding.ActivityMultiViewPagerBinding;
import ceui.lisa.fragments.FragmentRankIllust;
import ceui.lisa.fragments.FragmentRankNovel;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.MyOnTabSelectedListener;

public class RankActivity extends BaseActivity<ActivityMultiViewPagerBinding> {

    private String dataType = "";
    private String queryDate = "";

    @Override
    protected int initLayout() {
        return R.layout.activity_multi_view_pager;
    }

    @Override
    protected void initView() {
        setSupportActionBar(baseBind.toolbar);
        baseBind.placeHolder.setVisibility(View.VISIBLE);
        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) baseBind.placeHolder.getLayoutParams();
        p.height = BarUtils.getStatusBarHeight();
        baseBind.placeHolder.setLayoutParams(p);
        baseBind.toolbar.setNavigationOnClickListener(v -> finish());
        baseBind.toolbarTitle.setText(mContext.getString(R.string.ranking_illust));
        dataType = getIntent().getStringExtra("dataType");
        queryDate = getIntent().getStringExtra("date");
//        baseBind.viewPager.setPageTransformer(true, new DrawerTransformer());

        final String[] CHINESE_TITLES = new String[]{
                mContext.getString(R.string.daily_rank),
                mContext.getString(R.string.weekly_rank),
                mContext.getString(R.string.monthly_rank),
                mContext.getString(R.string.created_by_ai),
                mContext.getString(R.string.man_like),
                mContext.getString(R.string.woman_like),
                mContext.getString(R.string.self_done),
                mContext.getString(R.string.new_fish),
                mContext.getString(R.string.r_eighteen),
                mContext.getString(R.string.r_eighteen_weekly_rank),
                mContext.getString(R.string.r_eighteen_male_rank),
                mContext.getString(R.string.r_eighteen_female_rank),
                mContext.getString(R.string.r_eighteen_ai_rank),
                mContext.getString(R.string.r_eighteen_guro_rank)
        };

        final String[] CHINESE_TITLES_MANGA = new String[]{
                getString(R.string.string_124),
                getString(R.string.string_125),
                getString(R.string.string_126),
                getString(R.string.string_127),
                getString(R.string.string_128)
        };
        final String[] CHINESE_TITLES_NOVEL = new String[]{
                getString(R.string.string_129),
                getString(R.string.string_130),
                getString(R.string.string_131),
                getString(R.string.string_132),
                getString(R.string.string_133),
                getString(R.string.string_134)
        };

        final String[] titles = getTitles(CHINESE_TITLES, CHINESE_TITLES_MANGA, CHINESE_TITLES_NOVEL);
        final Fragment[] mFragments = getFragments(CHINESE_TITLES, CHINESE_TITLES_MANGA, CHINESE_TITLES_NOVEL);

        baseBind.viewPager.setAdapter(new FragmentStatePagerAdapter(
            getSupportFragmentManager(),
            FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        ) {
            @Override
            public Fragment getItem(int i) {
                return mFragments[i];
            }

            @Override
            public int getCount() {
                return titles.length;
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                return titles[position];
            }
        });
        baseBind.tabLayout.setupWithViewPager(baseBind.viewPager);
        MyOnTabSelectedListener listener = new MyOnTabSelectedListener(mFragments);
        baseBind.tabLayout.addOnTabSelectedListener(listener);
        //如果指定了跳转到某一个排行，就显示该页排行
        if (getIntent().getIntExtra("index", 0) >= 0) {
            baseBind.viewPager.setCurrentItem(getIntent().getIntExtra("index", 0));
        }
    }

    private String[] getTitles(String[] CHINESE_TITLES, String[] CHINESE_TITLES_MANGA, String[] CHINESE_TITLES_NOVEL) {
        if ("插画".equals(dataType)) {
            return CHINESE_TITLES;
        } else if ("漫画".equals(dataType)) {
            return CHINESE_TITLES_MANGA;
        } else if ("小说".equals(dataType)) {
            return CHINESE_TITLES_NOVEL;
        }
        return new String[0];
    }

    private Fragment[] getFragments(String[] CHINESE_TITLES, String[] CHINESE_TITLES_MANGA, String[] CHINESE_TITLES_NOVEL) {
        final Fragment[] mFragments;
        if ("插画".equals(dataType)) {
            mFragments = new Fragment[CHINESE_TITLES.length];
            for (int i = 0; i < CHINESE_TITLES.length; i++) {
                mFragments[i] = FragmentRankIllust.newInstance(i, queryDate, false);
            }
        } else if ("漫画".equals(dataType)) {
            mFragments = new Fragment[CHINESE_TITLES_MANGA.length];
            for (int i = 0; i < CHINESE_TITLES_MANGA.length; i++) {
                mFragments[i] = FragmentRankIllust.newInstance(i, queryDate, true);
            }
        } else if ("小说".equals(dataType)) {
            mFragments = new Fragment[CHINESE_TITLES_NOVEL.length];
            for (int i = 0; i < CHINESE_TITLES_NOVEL.length; i++) {
                mFragments[i] = FragmentRankNovel.newInstance(i, queryDate);
            }
        } else {
            mFragments = new Fragment[0];
        }
        return mFragments;
    }

    @Override
    protected void initData() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.select_date, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_select_date) {
            Calendar initial = Calendar.getInstance();
            if (!TextUtils.isEmpty(queryDate) && queryDate.contains("-")) {
                try {
                    String[] t = queryDate.split("-");
                    if (t.length == 3) {
                        initial.set(
                            Integer.parseInt(t[0]),
                            Integer.parseInt(t[1]) - 1,
                            Integer.parseInt(t[2])
                        );
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                mContext,
                (view, year, month, dayOfMonth) -> {
                    String date = year + "-" + (month + 1) + "-" + dayOfMonth;
                Common.showLog(date);
                Intent intent = new Intent(mContext, RankActivity.class);
                intent.putExtra("date", date);
                intent.putExtra("dataType", dataType);
                intent.putExtra("index", baseBind.viewPager.getCurrentItem());
                startActivity(intent);
                finish();
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean hideStatusBar() {
        return false;
    }

    @Override
    protected ActivityMultiViewPagerBinding onCreateBinding(LayoutInflater inflater) {
        return ActivityMultiViewPagerBinding.inflate(inflater);
    }
}
