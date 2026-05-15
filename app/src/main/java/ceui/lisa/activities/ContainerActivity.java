package ceui.lisa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

import ceui.lisa.R;
import ceui.lisa.databinding.ActivityFragmentBinding;
import ceui.lisa.fragments.FragmentAboutApp;
import ceui.lisa.fragments.FragmentBookedTag;
import ceui.lisa.fragments.FragmentCollection;
import ceui.lisa.fragments.FragmentColors;
import ceui.lisa.fragments.FragmentEditAccount;
import ceui.lisa.fragments.FragmentEditFile;
import ceui.lisa.fragments.FragmentFollowUser;
import ceui.lisa.fragments.FragmentHistoryTabs;
import ceui.lisa.fragments.FragmentImageDetailPager;
import ceui.lisa.fragments.FragmentLikeIllust;
import ceui.lisa.fragments.FragmentLikeNovel;
import ceui.lisa.fragments.FragmentListSimpleUser;
import ceui.lisa.fragments.FragmentLocalUsers;
import ceui.lisa.fragments.FragmentLogin;
import ceui.lisa.fragments.FragmentMangaSeries;
import ceui.lisa.fragments.FragmentMangaSeriesDetail;
import ceui.lisa.fragments.FragmentMarkdown;
import ceui.lisa.fragments.FragmentNew;
import ceui.lisa.fragments.FragmentNewNovel;
import ceui.lisa.fragments.FragmentNewNovels;
import ceui.lisa.fragments.FragmentNiceFriend;
import ceui.lisa.fragments.FragmentNovelMarkers;
import ceui.lisa.fragments.FragmentNovelSeries;
import ceui.lisa.fragments.FragmentNovelSeriesDetail;
import ceui.lisa.fragments.FragmentPv;
import ceui.lisa.fragments.FragmentRecmdIllust;
import ceui.lisa.fragments.FragmentRecmdUser;
import ceui.lisa.fragments.FragmentRelatedIllust;
import ceui.lisa.fragments.FragmentRelatedUser;
import ceui.lisa.fragments.FragmentSB;
import ceui.lisa.fragments.FragmentSearchUser;
import ceui.lisa.fragments.FragmentSettings;
import ceui.lisa.fragments.FragmentUserIllust;
import ceui.lisa.fragments.FragmentUserInfo;
import ceui.lisa.fragments.FragmentUserManga;
import ceui.lisa.fragments.FragmentUserNovel;
import ceui.lisa.fragments.FragmentViewPager;
import ceui.lisa.fragments.FragmentWalkThrough;
import ceui.lisa.fragments.FragmentWhoFollowThisUser;
import ceui.lisa.fragments.FragmentWorkSpace;
import ceui.lisa.fragments.RecmdUserMap;
import ceui.lisa.fragments.RecmdUserSnapshot;
import ceui.lisa.fragments.VFragment;
import ceui.lisa.helper.BackHandlerHelper;
import ceui.lisa.interfaces.VolumeKeyHandler;
import ceui.lisa.models.IllustsBean;
import ceui.lisa.models.NovelBean;
import ceui.lisa.models.UserBean;
import ceui.lisa.utils.Params;
import ceui.lisa.utils.ReverseResult;
import ceui.loxia.ObjectPool;
import ceui.loxia.ObjectType;
import ceui.loxia.flag.FlagDescFragment;
import ceui.pixiv.ui.comments.CommentsFragment;
import ceui.pixiv.ui.novel.NovelSeriesFragment;
import ceui.pixiv.ui.novel.NovelTextFragment;
import ceui.pixiv.ui.novel.UncategorizedNovelsFragment;
import ceui.pixiv.ui.novel.reader.NovelReaderV3Fragment;
import ceui.pixiv.ui.web.WebFragment;

import ceui.pixiv.route.RouteType;

public class ContainerActivity extends BaseActivity<ActivityFragmentBinding> implements
    ColorPickerDialogListener {

    public static final String EXTRA_FRAGMENT = "dataType";
    public static final String EXTRA_KEYWORD = "keyword";
    protected Fragment childFragment;
    private String dataType;

    @Override
    protected void initBundle(Bundle bundle) {
        dataType = bundle.getString(EXTRA_FRAGMENT);
        try {
            RouteType routeType = RouteType.valueOf(dataType);
            if (routeType == RouteType.IMAGE_DETAIL ||
                routeType == RouteType.DOWNLOAD_IMAGE ||
                routeType == RouteType.URL_IMAGE) {
                postponeEnterTransition();
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    protected Fragment createNewFragment() {
        Intent intent = getIntent();
        if (TextUtils.isEmpty(dataType)) return null;
        RouteType routeType;
        try {
            routeType = RouteType.valueOf(dataType);
        } catch (IllegalArgumentException e) {
            return new Fragment();
        }

        switch (routeType) {
            case LOGIN_REGISTER:
                return new FragmentLogin();
            case RELATED_ILLUST: {
                int id = intent.getIntExtra(Params.ILLUST_ID, 0);
                String title = intent.getStringExtra(Params.ILLUST_TITLE);
                return FragmentRelatedIllust.newInstance(id, title);
            }
            case HISTORY_TABS:
                return new FragmentHistoryTabs();
            case WEB_LINK: {
                String url = intent.getStringExtra(Params.URL);
                String title = intent.getStringExtra(Params.TITLE);
                return WebFragment.newInstance(url, title);
            }
            case SETTINGS:
                return new FragmentSettings();
            case RECMD_USER: {
                String recmdKey = intent.getStringExtra(Params.USER_MODEL);
                if (recmdKey == null) {
                    return new FragmentRecmdUser();
                }
                RecmdUserSnapshot snapshot = RecmdUserMap.store.remove(recmdKey);
                if (snapshot == null) {
                    return new FragmentRecmdUser();
                }
                return new FragmentRecmdUser(snapshot.items, snapshot.nextUrl);
            }
            case PV:
                return new FragmentPv();
            case SEARCH_USER: {
                String keyword = intent.getStringExtra(EXTRA_KEYWORD);
                return FragmentSearchUser.newInstance(keyword);
            }
            case REVERSE_SEARCH:
                ReverseResult result = intent.getParcelableExtra(Params.REVERSE_SEARCH_RESULT);
                return WebFragment.newInstance(result.getTitle(), result.getUrl(), result.getResponseBody(), result.getMime(), result.getEncoding(), result.getHistory_url());
            case COMMENTS: {
                return getCommentsFragment(intent);
            }
            case LOCAL_USERS:
                return new FragmentLocalUsers();
            case BOOKED_TAG: {
                return FragmentBookedTag.newInstance(intent.getIntExtra(Params.DATA_TYPE, 0), intent.getStringExtra(EXTRA_KEYWORD));
            }
            case SB_TAG: {
                int id = intent.getIntExtra(Params.ILLUST_ID, 0);
                String type = intent.getStringExtra(Params.DATA_TYPE);
                String[] tagNames = intent.getStringArrayExtra(Params.TAG_NAMES);
                return FragmentSB.newInstance(id, type, tagNames);
            }
            case ABOUT:
                return new FragmentAboutApp();
            case BATCH_DOWNLOAD:
                return new ceui.pixiv.ui.download.DownloadManagerV3Fragment();
            case BULK_SELECT:
                return new ceui.pixiv.ui.bulk.BulkSelectV3Fragment();
            case WALK_THROUGH:
                return new FragmentWalkThrough();
            case FOLLOWING:
                return FragmentFollowUser.newInstance(
                        getIntent().getIntExtra(Params.USER_ID, 0),
                        Params.TYPE_PUBLIC, true);
            case NICE_FRIEND:
                return new FragmentNiceFriend();
            case USER_INFO:
                return new FragmentUserInfo();
            case NEW_WORKS:
                return new FragmentNew();
            case FANS:
                return FragmentWhoFollowThisUser.newInstance(intent.getIntExtra(Params.USER_ID, 0));
            case LIKE_USERS:
                return FragmentListSimpleUser.newInstance((IllustsBean) intent.getSerializableExtra(Params.CONTENT));
            case NOVEL_SERIES_DETAIL:
                return FragmentNovelSeriesDetail.newInstance(intent.getIntExtra(Params.ID, 0));
            case USER_ILLUST:
                return FragmentUserIllust.newInstance(intent.getIntExtra(Params.USER_ID, 0),
                        true, intent.getIntExtra(Params.INITIAL_OFFSET, 0),
                        intent.getStringExtra(Params.TARGET_DATE));
            case USER_MANGA:
                return FragmentUserManga.newInstance(intent.getIntExtra(Params.USER_ID, 0),
                        true, intent.getIntExtra(Params.INITIAL_OFFSET, 0),
                        intent.getStringExtra(Params.TARGET_DATE));
            case LIKE_ILLUST:
                return FragmentLikeIllust.newInstance(intent.getIntExtra(Params.USER_ID, 0),
                        Params.TYPE_PUBLIC, true);
            case DOWNLOAD_MANAGER:
                return new ceui.pixiv.ui.download.DownloadManagerV3Fragment();
            case RECMD_ILLUST_MANGA:
                return FragmentRecmdIllust.newInstance("漫画");
            case RECMD_NOVEL:
                return new FragmentNewNovel();
            case LIKE_NOVEL:
                return FragmentLikeNovel.newInstance(intent.getIntExtra(Params.USER_ID, 0),
                        Params.TYPE_PUBLIC, true);
            case USER_NOVEL:
                return FragmentUserNovel.newInstance(intent.getIntExtra(Params.USER_ID, 0));
            case NOVEL_DETAIL: {
                NovelBean bean = (NovelBean) intent.getSerializableExtra(Params.CONTENT);
                long tid = bean != null ? bean.getId() : intent.getLongExtra(Params.NOVEL_ID, 0L);
                return NovelTextFragment.Companion.newInstance(tid);
            }
            case NOVEL_READER: {
                NovelBean bean = (NovelBean) intent.getSerializableExtra(Params.CONTENT);
                if (bean != null) {
                    return NovelReaderV3Fragment.newInstance(bean);
                }
                long rid = intent.getLongExtra(Params.NOVEL_ID, 0L);
                return NovelReaderV3Fragment.newInstance(rid);
            }
            case NOVEL_SERIES: {
                long sid = intent.getLongExtra(NovelSeriesFragment.ARG_SERIES_ID, 0L);
                return NovelSeriesFragment.Companion.newInstance(sid);
            }
            case UNCATEGORIZED_NOVELS: {
                int uid = intent.getIntExtra(Params.USER_ID, 0);
                return UncategorizedNovelsFragment.Companion.newInstance((long) uid);
            }
            case IMAGE_DETAIL:
                return FragmentImageDetailPager.newInstance(
                    (IllustsBean) intent.getSerializableExtra("illust"),
                    intent.getIntExtra("index", 0)
                );
            case URL_IMAGE:
                return FragmentImageDetailPager.newInstance(
                    intent.getStringExtra(Params.URL),
                    intent.getStringExtra(Params.TITLE)
                );
            case DOWNLOAD_IMAGE:
                return FragmentImageDetailPager.newInstance(
                    (java.util.List<String>) intent.getSerializableExtra("illust"),
                    intent.getIntExtra("index", 0)
                );
            case FULL_SCREEN: {
                String pageUUID = intent.getStringExtra(Params.PAGE_UUID);
                int index = intent.getIntExtra(Params.POSITION, 0);
                String seed = intent.getStringExtra(Params.SEED);
                return VFragment.newInstance(pageUUID, index, seed);
            }
            case EDIT_ACCOUNT:
                return new FragmentEditAccount();
            case EDIT_FILE:
                return new FragmentEditFile();
            case VIEW_PAGER_MUTED:
                return FragmentViewPager.newInstance(Params.VIEW_PAGER_MUTED);
            case DOWNLOAD_PATH_SETTINGS:
                return new ceui.pixiv.ui.settings.DownloadPathSettingsFragment();
            case NOVEL_HEADER_SETTINGS:
                return new ceui.pixiv.ui.settings.NovelHeaderSettingsFragment();
            case FOLLOWING_NOVELS:
                return new FragmentNewNovels();
            case MANGA_SERIES:
                return FragmentMangaSeries.newInstance(intent.getIntExtra(Params.USER_ID, 0));
            case MANGA_SERIES_DETAIL:
                return FragmentMangaSeriesDetail.newInstance(intent.getIntExtra(Params.MANGA_SERIES_ID, 0));
            case NOVEL_SERIES_WORKS:
                return new FragmentNovelSeries();
            case WORK_SPACE:
                return new FragmentWorkSpace();
            case COLLECTION_ILLUST:
                return FragmentCollection.newInstance(0);
            case COLLECTION_NOVEL:
                return FragmentCollection.newInstance(1);
            case COLLECTION_WATCHLIST:
                return FragmentCollection.newInstance(3);
            case COLLECTION_FOLLOWING:
                return FragmentCollection.newInstance(2);
            case NOVEL_MARKERS:
                return new FragmentNovelMarkers();
            case COLORS:
                return new FragmentColors();
            case FLAG_DESC:
                return FlagDescFragment.Companion.newInstance(
                        intent.getIntExtra(FlagDescFragment.FlagReasonIdKey, 0),
                        intent.getIntExtra(FlagDescFragment.FlagObjectIdKey, 0),
                        intent.getIntExtra(FlagDescFragment.FlagObjectTypeKey, 0)
                );
            case RELATED_USER:
                return FragmentRelatedUser.newInstance(intent.getIntExtra(Params.USER_ID, 0));
            case MARKDOWN:
                String url = intent.getStringExtra(Params.URL);
                return FragmentMarkdown.newInstance(url);
            case VERSION_HISTORY:
                return new ceui.lisa.update.FragmentVersionHistory();
            default:
                return new Fragment();
        }
    }


    private CommentsFragment getCommentsFragment(Intent intent) {
        int workId = intent.getIntExtra(Params.ILLUST_ID, 0);

        if (workId == 0) {
            workId = intent.getIntExtra(Params.NOVEL_ID, 0);
            NovelBean hit = ObjectPool.INSTANCE.getNovel(workId).getValue();
            int illustArthurId = getArthurIdFromNovel(hit);
            return CommentsFragment.Companion.newInstance(workId, illustArthurId, ObjectType.NOVEL);
        } else {
            IllustsBean hit = ObjectPool.INSTANCE.getIllust(workId).getValue();
            int illustArthurId = getArthurIdFromIllust(hit);
            return CommentsFragment.Companion.newInstance(workId, illustArthurId, ObjectType.ILLUST);
        }
    }

    // Helper method to extract Arthur ID from NovelBean
    private int getArthurIdFromNovel(NovelBean hit) {
        if (hit != null) {
            UserBean user = hit.getUser();
            if (user != null) {
                return user.getId();
            }
        }
        return 0;
    }

    // Helper method to extract Arthur ID from IllustsBean
    private int getArthurIdFromIllust(IllustsBean hit) {
        if (hit != null) {
            UserBean user = hit.getUser();
            if (user != null) {
                return user.getId();
            }
        }
        return 0;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (childFragment instanceof VolumeKeyHandler) {
                if (((VolumeKeyHandler) childFragment).handleVolumeKey(keyCode)) {
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected int initLayout() {
        return R.layout.activity_fragment;
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void initData() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            fragment = createNewFragment();
            if (fragment != null) {
                fragmentManager.beginTransaction()
                        .add(R.id.fragment_container, fragment)
                        .commit();
                childFragment = fragment;
            }
        } else {
            childFragment = fragment;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (childFragment != null) {
            childFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean hideStatusBar() {
        if (RouteType.COMMENTS.name().equals(dataType)) {
            return false;
        } else {
            return getIntent().getBooleanExtra("hideStatusBar", true);
        }
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
    }

    @Override
    public void onDialogDismissed(int dialogId) {

    }

    @Override
    public void onBackPressed() {
        if (!BackHandlerHelper.handleBackPress(this)) {
            super.onBackPressed();
        }
    }

    @Override
    protected ActivityFragmentBinding onCreateBinding(LayoutInflater inflater) {
        return ActivityFragmentBinding.inflate(inflater);
    }
}
