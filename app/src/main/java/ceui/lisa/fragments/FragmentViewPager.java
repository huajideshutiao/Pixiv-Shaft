package ceui.lisa.fragments;

import static android.app.Activity.RESULT_OK;
import static android.provider.DocumentsContract.EXTRA_INITIAL_URI;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import android.view.LayoutInflater;

import ceui.lisa.R;
import ceui.lisa.activities.BaseActivity;
import ceui.lisa.activities.Shaft;
import ceui.lisa.database.AppDatabase;
import ceui.lisa.database.MuteEntity;
import ceui.lisa.databinding.ViewpagerWithTablayoutBinding;
import ceui.lisa.download.IllustDownload;
import ceui.lisa.interfaces.Callback;
import ceui.lisa.interfaces.VolumeKeyHandler;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.MyOnTabSelectedListener;
import ceui.lisa.utils.Params;

public class FragmentViewPager extends BaseLazyFragment<ViewpagerWithTablayoutBinding> implements
    VolumeKeyHandler {

    private static final String MUTE_RECORDS_FILE_NAME = "Shaft-MuteRecords.json";

    private String title;
    private ListFragment[] mFragments = null;

    private final ActivityResultLauncher<Intent> importMuteLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                Uri uri = result.getData().getData();
                if (uri == null) {
                    Common.showToast(getString(R.string.mute_records_import_no_file));
                    return;
                }
                new Thread(() -> {
                    try {
                        InputStream is = mContext.getContentResolver().openInputStream(uri);
                        if (is == null) {
                            mActivity.runOnUiThread(() ->
                                    Common.showToast(getString(R.string.mute_records_import_no_file)));
                            return;
                        }
                        int imported = 0;
                        try (JsonReader reader = new JsonReader(new InputStreamReader(is))) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                MuteEntity entity = Shaft.sGson.fromJson(reader, MuteEntity.class);
                                if (entity == null || entity.getTagJson() == null || entity.getTagJson().isEmpty()) {
                                    continue;
                                }
                                AppDatabase.getAppDatabase(mContext).searchDao().insertMuteTag(entity);
                                imported++;
                            }
                            reader.endArray();
                        }
                        if (imported == 0) {
                            mActivity.runOnUiThread(() ->
                                    Common.showToast(getString(R.string.mute_records_import_invalid)));
                            return;
                        }
                        int finalImported = imported;
                        mActivity.runOnUiThread(() -> {
                            forceRefresh();
                            Common.showToast(getString(R.string.mute_records_import_success, finalImported));
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        mActivity.runOnUiThread(() ->
                                Common.showToast(getString(R.string.mute_records_import_failed, String.valueOf(e.getMessage()))));
                    }
                }).start();
            }
    );

    @Override
    public boolean handleVolumeKey(int keyCode) {
        if (mFragments != null && baseBind != null) {
            int currentItem = baseBind.viewPager.getCurrentItem();
            if (currentItem >= 0 && currentItem < mFragments.length) {
                ListFragment currentFragment = mFragments[currentItem];
                if (currentFragment != null) {
                    return currentFragment.handleVolumeKey(keyCode);
                }
            }
        }
        return false;
    }

    public static FragmentViewPager newInstance(String title) {
        Bundle args = new Bundle();
        args.putString(Params.TITLE, title);
        FragmentViewPager fragment = new FragmentViewPager();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void initBundle(Bundle bundle) {
        title = bundle.getString(Params.TITLE);
    }


    @Override
    public void initLayout() {
        mLayoutID = R.layout.viewpager_with_tablayout;
    }

    @Override
    public void initView() {
        // EdgeToEdge: push AppBarLayout below the status bar
        ViewCompat.setOnApplyWindowInsetsListener(baseBind.appBar, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, bars.top, 0, 0);
            return windowInsets;
        });
        baseBind.toolbar.setNavigationOnClickListener(v -> mActivity.finish());
    }

    @Override
    public void lazyData() {
        if (TextUtils.equals(title, Params.VIEW_PAGER_MUTED)) {
            String[] CHINESE_TITLES = new String[]{
                    Shaft.getContext().getString(R.string.string_353),
                    Shaft.getContext().getString(R.string.string_381),
                    Shaft.getContext().getString(R.string.string_354),
            };
            mFragments = new ListFragment[]{
                    new FragmentMutedTags(),
                    new FragmentMutedUser(),
                    new FragmentMutedObjects(),
            };
            baseBind.toolbar.inflateMenu(R.menu.delete_and_add);
            setMuteMenuListener(mFragments[0]);
            baseBind.toolbarTitle.setText(R.string.muted_history);
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
                    ListFragment fragment = (ListFragment) super.instantiateItem(container, position);
                    if (mFragments != null && position < mFragments.length) {
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
            baseBind.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    setMuteMenuListener(mFragments[position]);
                    if (position == 0) {
                        baseBind.toolbar.getMenu().clear();
                        baseBind.toolbar.inflateMenu(R.menu.delete_and_add);
                    } else {
                        baseBind.toolbar.getMenu().clear();
                        baseBind.toolbar.inflateMenu(R.menu.delete_muted_history);
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });

        } else if (TextUtils.equals(title, Params.VIEW_PAGER_R18)) {
            baseBind.toolbar.setVisibility(View.GONE);
            String[] CHINESE_TITLES = new String[]{
                    Shaft.getContext().getString(R.string.r_eighteen),
                    Shaft.getContext().getString(R.string.r_eighteen_weekly_rank),
                    Shaft.getContext().getString(R.string.r_eighteen_male_rank),
                    Shaft.getContext().getString(R.string.r_eighteen_female_rank),
                    Shaft.getContext().getString(R.string.r_eighteen_ai_rank)
            };
            mFragments = new ListFragment[]{
//                    FragmentRankIllust.newInstance(7, "", false),
                    FragmentRankIllust.newInstance(8, "", false),
                    FragmentRankIllust.newInstance(9, "", false),
                    FragmentRankIllust.newInstance(10, "", false),
                    FragmentRankIllust.newInstance(11, "", false),
                    FragmentRankIllust.newInstance(12, "", false)
            };
            baseBind.toolbarTitle.setText(R.string.string_r);
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
                    ListFragment fragment = (ListFragment) super.instantiateItem(container, position);
                    if (mFragments != null && position < mFragments.length) {
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

        }
        baseBind.tabLayout.setupWithViewPager(baseBind.viewPager);
        MyOnTabSelectedListener listener = new MyOnTabSelectedListener(mFragments);
        baseBind.tabLayout.addOnTabSelectedListener(listener);
    }

    public void forceRefresh() {
        try {
            if (mFragments != null) {
                mFragments[baseBind.viewPager.getCurrentItem()].forceRefresh();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setMuteMenuListener(ListFragment delegate) {
        baseBind.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_export_mute) {
                exportMuteRecords();
                return true;
            } else if (item.getItemId() == R.id.action_import_mute) {
                pickMuteRecordsFile();
                return true;
            }
            return ((Toolbar.OnMenuItemClickListener) delegate).onMenuItemClick(item);
        });
    }

    private void exportMuteRecords() {
        IllustDownload.downloadBackupFile((BaseActivity<?>) mActivity,
                MUTE_RECORDS_FILE_NAME, new Callback<File>() {
                    @Override
                    public void doSomething(File file) {
                        List<MuteEntity> all = AppDatabase.getAppDatabase(mContext)
                                .searchDao().getAllMuteEntities();
                        if (all == null || all.isEmpty()) {
                            Common.showToast(getString(R.string.mute_records_export_empty));
                            return;
                        }
                        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
                            writer.beginArray();
                            for (MuteEntity entity : all) {
                                Shaft.sGson.toJson(entity, MuteEntity.class, writer);
                            }
                            writer.endArray();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Common.showToast(getString(R.string.mute_records_export_success, all.size()));
                    }
                }, null);
    }

    private void pickMuteRecordsFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri initialUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:"
                    + "Download%2fShaftBackups%2f" + MUTE_RECORDS_FILE_NAME);
            intent.putExtra(EXTRA_INITIAL_URI, initialUri);
        }
        importMuteLauncher.launch(intent);
    }

    @Override
    protected ViewpagerWithTablayoutBinding onCreateBinding(
        LayoutInflater inflater,
        ViewGroup container,
        boolean attachToParent
    ) {
        return ViewpagerWithTablayoutBinding.inflate(inflater, container, false);
    }
}
