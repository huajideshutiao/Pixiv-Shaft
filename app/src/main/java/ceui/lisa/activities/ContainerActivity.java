package ceui.lisa.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;

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

public class ContainerActivity extends BaseActivity<ActivityFragmentBinding> implements
    ColorPickerDialogListener {

    public static final String EXTRA_FRAGMENT = "dataType";
    public static final String EXTRA_KEYWORD = "keyword";
    protected Fragment childFragment;
    private String dataType;

    @Override
    protected void initBundle(Bundle bundle) {
        dataType = bundle.getString(EXTRA_FRAGMENT);
        if ("图片详情".equals(dataType) || "下载图片".equals(dataType) || "URL图片".equals(dataType)) {
            postponeEnterTransition();
        }
    }

    protected Fragment createNewFragment() {
        Intent intent = getIntent();
        if (!TextUtils.isEmpty(dataType)) {
            switch (dataType) {
                case "登录注册":
                    return new FragmentLogin();
                case "相关作品": {
                    int id = intent.getIntExtra(Params.ILLUST_ID, 0);
                    String title = intent.getStringExtra(Params.ILLUST_TITLE);
                    return FragmentRelatedIllust.newInstance(id, title);
                }
                case "浏览记录":
                    return new FragmentHistoryTabs();
                case "网页链接": {
                    String url = intent.getStringExtra(Params.URL);
                    String title = intent.getStringExtra(Params.TITLE);
                    return WebFragment.newInstance(url, title);
                }
                case "设置":
                    return new FragmentSettings();
                case "推荐用户": {
                    // Paired with FragmentRight#seeMore: the producer stashes
                    // the Feed horizontal preview's snapshot under a random
                    // key in RecmdUserMap and passes the key here. We remove
                    // it on consume so the map doesn't leak.
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
                case "特辑":
                    return new FragmentPv();
                case "搜索用户": {
                    String keyword = intent.getStringExtra(EXTRA_KEYWORD);
                    return FragmentSearchUser.newInstance(keyword);
                }
                case "以图搜图":
                    ReverseResult result = intent.getParcelableExtra(Params.REVERSE_SEARCH_RESULT);
                    return WebFragment.newInstance(result.getTitle(), result.getUrl(), result.getResponseBody(), result.getMime(), result.getEncoding(), result.getHistory_url());
                case "相关评论": {
                    return getCommentsFragment(intent);
                }
                case "账号管理":
                    return new FragmentLocalUsers();
                case "按标签筛选": {
                    return FragmentBookedTag.newInstance(intent.getIntExtra(Params.DATA_TYPE, 0), intent.getStringExtra(EXTRA_KEYWORD));
                }
                case "按标签收藏": {
                    int id = intent.getIntExtra(Params.ILLUST_ID, 0);
                    String type = intent.getStringExtra(Params.DATA_TYPE);
                    String[] tagNames = intent.getStringArrayExtra(Params.TAG_NAMES);
                    return FragmentSB.newInstance(id, type, tagNames);
                }
                case "关于软件":
                    return new FragmentAboutApp();
                case "批量下载队列":
                    // 统一路由到新的 V3 下载管理页（默认进队列 tab）
                    return new ceui.pixiv.ui.download.DownloadManagerV3Fragment();
                case "批量选择":
                    return new ceui.pixiv.ui.bulk.BulkSelectV3Fragment();
                case "画廊":
                    return new FragmentWalkThrough();
                case "正在关注":
                    return FragmentFollowUser.newInstance(
                            getIntent().getIntExtra(Params.USER_ID, 0),
                            Params.TYPE_PUBLIC, true);
                case "好P友":
                    return new FragmentNiceFriend();
                case "详细信息":
                    return new FragmentUserInfo();
                case "最新作品":
                    return new FragmentNew();
                case "粉丝":
                    return FragmentWhoFollowThisUser.newInstance(intent.getIntExtra(Params.USER_ID, 0));
                case "喜欢这个作品的用户":
                    return FragmentListSimpleUser.newInstance((IllustsBean) intent.getSerializableExtra(Params.CONTENT));
                case "小说系列详情":
                    return FragmentNovelSeriesDetail.newInstance(intent.getIntExtra(Params.ID, 0));
                case "插画作品":
                    return FragmentUserIllust.newInstance(intent.getIntExtra(Params.USER_ID, 0),
                            true, intent.getIntExtra(Params.INITIAL_OFFSET, 0),
                            intent.getStringExtra(Params.TARGET_DATE));
                case "漫画作品":
                    return FragmentUserManga.newInstance(intent.getIntExtra(Params.USER_ID, 0),
                            true, intent.getIntExtra(Params.INITIAL_OFFSET, 0),
                            intent.getStringExtra(Params.TARGET_DATE));
                case "插画/漫画收藏":
                    return FragmentLikeIllust.newInstance(intent.getIntExtra(Params.USER_ID, 0),
                            Params.TYPE_PUBLIC, true);
                case "下载管理":
                    return new ceui.pixiv.ui.download.DownloadManagerV3Fragment();
                case "推荐漫画":
                    return FragmentRecmdIllust.newInstance("漫画");
                case "推荐小说":
                    return new FragmentNewNovel();
                case "小说收藏":
                    return FragmentLikeNovel.newInstance(intent.getIntExtra(Params.USER_ID, 0),
                            Params.TYPE_PUBLIC, true);
                case "小说作品":
                    return FragmentUserNovel.newInstance(intent.getIntExtra(Params.USER_ID, 0));
                case "小说详情": {
                    NovelBean bean = (NovelBean) intent.getSerializableExtra(Params.CONTENT);
                    long tid = bean != null ? bean.getId() : intent.getLongExtra(Params.NOVEL_ID, 0L);
                    return NovelTextFragment.Companion.newInstance(tid);
                }
                case "小说正文": {
                    NovelBean bean = (NovelBean) intent.getSerializableExtra(Params.CONTENT);
                    if (bean != null) {
                        return NovelReaderV3Fragment.newInstance(bean);
                    }
                    long rid = intent.getLongExtra(Params.NOVEL_ID, 0L);
                    return NovelReaderV3Fragment.newInstance(rid);
                }
                case "小说系列": {
                    long sid = intent.getLongExtra(NovelSeriesFragment.ARG_SERIES_ID, 0L);
                    return NovelSeriesFragment.Companion.newInstance(sid);
                }
                case "未归类小说": {
                    int uid = intent.getIntExtra(Params.USER_ID, 0);
                    return UncategorizedNovelsFragment.Companion.newInstance((long) uid);
                }
                case "图片详情":
                    return FragmentImageDetailPager.newInstance(
                        (IllustsBean) intent.getSerializableExtra("illust"),
                        intent.getIntExtra("index", 0)
                    );
                case "URL图片":
                    return FragmentImageDetailPager.newInstance(
                        intent.getStringExtra(Params.URL),
                        intent.getStringExtra(Params.TITLE)
                    );
                case "下载图片":
                    return FragmentImageDetailPager.newInstance(
                        (java.util.List<String>) intent.getSerializableExtra("illust"),
                        intent.getIntExtra("index", 0)
                    );
                case "全屏查看": {
                    String pageUUID = intent.getStringExtra(Params.PAGE_UUID);
                    int index = intent.getIntExtra(Params.POSITION, 0);
                    String seed = intent.getStringExtra(Params.SEED);
                    return VFragment.newInstance(pageUUID, index, seed);
                }
                case "绑定邮箱":
                    return new FragmentEditAccount();
                case "编辑个人资料":
                    return new FragmentEditFile();
                case "标签屏蔽记录":
                    return FragmentViewPager.newInstance(Params.VIEW_PAGER_MUTED);
                case "下载路径与文件名":
                    return new ceui.pixiv.ui.settings.DownloadPathSettingsFragment();
                case "小说信息头":
                    return new ceui.pixiv.ui.settings.NovelHeaderSettingsFragment();
                case "关注者的小说":
                    return new FragmentNewNovels();
                case "漫画系列作品":
                    return FragmentMangaSeries.newInstance(intent.getIntExtra(Params.USER_ID, 0));
                case "漫画系列详情":
                    return FragmentMangaSeriesDetail.newInstance(intent.getIntExtra(Params.MANGA_SERIES_ID, 0));
                case "小说系列作品":
                    return new FragmentNovelSeries();
                case "我的作业环境":
                    return new FragmentWorkSpace();
                case "我的插画收藏":
                    return FragmentCollection.newInstance(0);
                case "我的小说收藏":
                    return FragmentCollection.newInstance(1);
                case "追更列表":
                    return FragmentCollection.newInstance(3);
                case "我的关注":
                    return FragmentCollection.newInstance(2);
                case "小说书签":
                    return new FragmentNovelMarkers();
                case "主题颜色":
                    return new FragmentColors();
                case "填写举报详细信息":
                    return FlagDescFragment.Companion.newInstance(
                            intent.getIntExtra(FlagDescFragment.FlagReasonIdKey, 0),
                            intent.getIntExtra(FlagDescFragment.FlagObjectIdKey, 0),
                            intent.getIntExtra(FlagDescFragment.FlagObjectTypeKey, 0)
                    );
                case "相关用户":
                    return FragmentRelatedUser.newInstance(intent.getIntExtra(Params.USER_ID, 0));
                case "Markdown":
                    String url = intent.getStringExtra(Params.URL);
                    return FragmentMarkdown.newInstance(url);
                case "版本历史":
                    return new ceui.lisa.update.FragmentVersionHistory();
                default:
                    return new Fragment();
            }
        }
        return null;
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
        if ("相关评论".equals(dataType)) {
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

}
