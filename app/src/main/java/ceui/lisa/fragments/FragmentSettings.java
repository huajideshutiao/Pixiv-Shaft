package ceui.lisa.fragments;

import static android.app.Activity.RESULT_OK;
import static android.provider.DocumentsContract.EXTRA_INITIAL_URI;
import static ceui.lisa.helper.ThemeHelper.ThemeType.DARK_MODE;
import static ceui.lisa.helper.ThemeHelper.ThemeType.DEFAULT_MODE;
import static ceui.lisa.helper.ThemeHelper.ThemeType.LIGHT_MODE;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import ceui.lisa.utils.AppKit;
import com.qmuiteam.qmui.skin.QMUISkinManager;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.scwang.smart.refresh.header.FalsifyFooter;
import com.scwang.smart.refresh.header.FalsifyHeader;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import java.util.Arrays;
import java.util.Locale;

import ceui.lisa.R;
import ceui.lisa.activities.BaseActivity;
import ceui.lisa.activities.Shaft;
import ceui.lisa.databinding.FragmentSettingsBinding;
import ceui.lisa.download.IllustDownload;
import ceui.lisa.file.LegacyFile;
import ceui.lisa.helper.NavigationLocationHelper;
import ceui.lisa.helper.ThemeHelper;
import ceui.lisa.http.Retro;
import ceui.lisa.utils.BackupUtils;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.DownloadLimitTypeUtil;
import ceui.lisa.utils.Local;
import ceui.lisa.utils.Params;
import ceui.lisa.utils.PixivSearchParamUtil;
import ceui.lisa.utils.Settings;
import ceui.loxia.Client;
import ceui.pixiv.download.DownloadsRegistry;
import ceui.pixiv.download.config.OverwritePolicy;
import ceui.pixiv.download.config.StorageChoice;
import ceui.pixiv.route.AppRoute;


public class FragmentSettings extends SwipeFragment<FragmentSettingsBinding> {

    private final ActivityResultLauncher<Intent> openDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                try {
                    Uri uri = result.getData().getData();
                    String fileString = new String(AppKit.uri2Bytes(mContext, uri));
                    boolean restoreResult = BackupUtils.restoreBackups(mContext, fileString);
                    Common.showToast(restoreResult ? getString(R.string.restore_success) : getString(R.string.restore_failed));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    );

    @Override
    public void initLayout() {
        mLayoutID = R.layout.fragment_settings;
    }

    @Override
    protected void initData() {
        baseBind.toolbar.setNavigationOnClickListener(view -> mActivity.finish());
        Common.animate(baseBind.parentLinear);

        // 1. 账号
        {
            baseBind.userManage.setOnClickListener(v -> {
                AppRoute.LocalUsers.INSTANCE.start(mContext);
            });

            baseBind.editAccount.setOnClickListener(v -> {
                AppRoute.EditAccount.INSTANCE.start(mContext);
            });

            baseBind.editFile.setOnClickListener(v -> {
                AppRoute.EditFile.INSTANCE.start(mContext);
            });

            baseBind.workSpace.setOnClickListener(v -> {
                AppRoute.WorkSpace.INSTANCE.start(mContext);
            });

            baseBind.r18Space.setOnClickListener(v -> {
                new AppRoute.WebLink(Params.URL_R18_SETTING, null).start(mContext);
            });

            baseBind.premiumSpace.setOnClickListener(v -> {
                new AppRoute.WebLink(Params.URL_PREMIUM_SETTING, null).start(mContext);
            });

            baseBind.loginOut.setOnClickListener(v -> {
                QMUIDialog.CheckBoxMessageDialogBuilder builder =
                    new QMUIDialog.CheckBoxMessageDialogBuilder(getActivity());
                builder.setTitle(getString(R.string.string_185))
                    .setMessage(getString(R.string.string_186)).setChecked(true)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addAction(
                        getString(R.string.string_187), (dialog, index) -> dialog.dismiss()
                    ).addAction(
                        R.string.login_out, (dialog, index) -> {
                            Common.logOut(mContext, builder.isChecked());
                            mActivity.finish();
                            dialog.dismiss();
                        }
                    ).create().show();
            });
        }

        // 2. 网络
        {
            baseBind.autoDns.setChecked(Shaft.sSettings.isDirectConnect());
            // DoH 只在直连开启时生效，跟随直连开关显隐
            baseBind.useSecureDnsGroup.setVisibility(
                    Shaft.sSettings.isDirectConnect() ? android.view.View.VISIBLE : android.view.View.GONE);
            baseBind.autoDns.setOnCheckedChangeListener((buttonView, isChecked) -> {
                boolean changed = isChecked != Shaft.sSettings.isDirectConnect();
                Shaft.sSettings.setDirectConnect(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);

                android.view.ViewGroup secureDnsParent = (android.view.ViewGroup) baseBind.useSecureDnsGroup.getParent();
                if (secureDnsParent != null) {
                    androidx.transition.TransitionManager.beginDelayedTransition(secureDnsParent, new androidx.transition.AutoTransition());
                }
                baseBind.useSecureDnsGroup.setVisibility(isChecked ? android.view.View.VISIBLE : android.view.View.GONE);

                if (changed) {
                    Retro.refreshAppApi();
                    Client.INSTANCE.reset();
                }
            });
            baseBind.directConnectLink.setOnClickListener(v -> {
                new AppRoute.WebLink("https://github.com/Notsfsssf/Pix-EzViewer", "PxEz项目主页").start(mContext);
            });
            baseBind.directConnectRela.setOnClickListener(v -> baseBind.autoDns.performClick());

            // 安全 DNS (DoH)
            baseBind.useSecureDns.setChecked(Shaft.sSettings.isUseSecureDns());
            baseBind.useSecureDns.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setUseSecureDns(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);
                ceui.lisa.http.HttpDns.invalidate();
            });
            baseBind.useSecureDnsRela.setOnClickListener(v -> baseBind.useSecureDns.performClick());

            //自定义图片代理
            baseBind.usePixivCat.setChecked(Shaft.sSettings.isUsePixivCat());
            baseBind.usePixivCat.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setUsePixivCat(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);
                updateImageProxyUrlUI();
            });
            baseBind.usePixivCatRela.setOnClickListener(v -> baseBind.usePixivCat.performClick());

            updateImageProxyUrlUI();
            baseBind.imageProxyUrlRela.setOnClickListener(v -> {
                final QMUIDialog.EditTextDialogBuilder builder =
                    new QMUIDialog.EditTextDialogBuilder(mActivity);
                builder.setTitle(getString(R.string.image_proxy_url_dialog_title))
                    .setPlaceholder(getString(R.string.image_proxy_url_dialog_hint))
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext))
                    .addAction(getString(R.string.string_187), (dialog, index) -> dialog.dismiss())
                    .addAction(
                        R.string.sure, (dialog, index) -> {
                            @SuppressWarnings("deprecation")
                            CharSequence input = builder.getEditText().getText();
                            String url = input != null ? input.toString().trim() : "";
                            Shaft.sSettings.setImageProxyUrl(url);
                            Local.setSettings(Shaft.sSettings);
                            updateImageProxyUrlUI();
                            Common.showToast(getString(R.string.string_428), 2);
                            dialog.dismiss();
                        }
                    )
                    .create().show();
                String currentUrl = Shaft.sSettings.getImageProxyUrl();
                if (!currentUrl.isEmpty()) {
                    @SuppressWarnings("deprecation")
                    var editText = builder.getEditText();
                    editText.setText(currentUrl);
                }
            });

            //缩略图是否显示大图
            baseBind.showLargeThumbnailImage.setChecked(Shaft.sSettings.isShowLargeThumbnailImage());
            baseBind.showLargeThumbnailImage.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setShowLargeThumbnailImage(isChecked);
                Common.showToast(getString(R.string.string_428));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.showLargeThumbnailImageRela.setOnClickListener(v -> baseBind.showLargeThumbnailImage.performClick());

            //详情是否显示原图
            baseBind.showOriginalPreviewImage.setChecked(Shaft.sSettings.isShowOriginalPreviewImage());
            baseBind.showOriginalPreviewImage.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setShowOriginalPreviewImage(isChecked);
                Common.showToast(getString(R.string.string_428));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.showOriginalPreviewImageRela.setOnClickListener(v -> baseBind.showOriginalPreviewImage.performClick());
        }

        // 5. 浏览与收藏 (原常规 + 个性化)
        {
            baseBind.saveHistory.setChecked(Shaft.sSettings.isSaveViewHistory());
            baseBind.saveHistory.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setSaveViewHistory(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.saveHistoryRela.setOnClickListener(v -> baseBind.saveHistory.performClick());

            baseBind.deleteStarIllust.setChecked(Shaft.sSettings.isDeleteStarIllust());
            baseBind.deleteStarIllust.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setDeleteStarIllust(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.deleteStarIllustRela.setOnClickListener(v -> baseBind.deleteStarIllust.performClick());

            baseBind.filterRankBookmarked.setChecked(Shaft.sSettings.isFilterRankBookmarked());
            baseBind.filterRankBookmarked.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setFilterRankBookmarked(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.filterRankBookmarkedRela.setOnClickListener(v -> baseBind.filterRankBookmarked.performClick());

            baseBind.filterInvalidBookmarks.setChecked(Shaft.sSettings.isFilterInvalidBookmarks());
            baseBind.filterInvalidBookmarks.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setFilterInvalidBookmarks(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.filterInvalidBookmarksRela.setOnClickListener(v -> baseBind.filterInvalidBookmarks.performClick());

            baseBind.deleteAiIllust.setChecked(Shaft.sSettings.isDeleteAIIllust());
            baseBind.deleteAiIllust.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setDeleteAIIllust(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.deleteAiIllustRela.setOnClickListener(v -> baseBind.deleteAiIllust.performClick());

            baseBind.toastDownloadResult.setChecked(Shaft.sSettings.isToastDownloadResult());
            baseBind.toastDownloadResult.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setToastDownloadResult(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.toastDownloadResultRela.setOnClickListener(v -> baseBind.toastDownloadResult.performClick());

            final String searchFilter = Shaft.sSettings.getSearchFilter();
            baseBind.searchFilter.setText(PixivSearchParamUtil.getSizeName(searchFilter));
            baseBind.searchFilterRela.setOnClickListener(v -> new QMUIDialog.CheckableDialogBuilder(mContext).setCheckedIndex(
                    PixivSearchParamUtil.getSizeIndex(Shaft.sSettings.getSearchFilter()))
                .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                    PixivSearchParamUtil.ALL_SIZE_NAME, (dialog, which) -> {
                        Shaft.sSettings.setSearchFilter(PixivSearchParamUtil.ALL_SIZE_VALUE[which]);
                        Common.showToast(getString(R.string.string_428), 2);
                        Local.setSettings(Shaft.sSettings);
                        baseBind.searchFilter.setText(PixivSearchParamUtil.ALL_SIZE_NAME[which]);
                        dialog.dismiss();
                    }
                ).create().show());

            // 搜索结果默认排序方式
            final String searchDefaultSortType = Shaft.sSettings.getSearchDefaultSortType();
            baseBind.searchDefaultSortType.setText(PixivSearchParamUtil.getSortTypeName(
                searchDefaultSortType));
            baseBind.searchDefaultSortTypeRela.setOnClickListener(v -> new QMUIDialog.CheckableDialogBuilder(mContext).setCheckedIndex(
                    PixivSearchParamUtil.getSortTypeIndex(Shaft.sSettings.getSearchDefaultSortType()))
                .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                    PixivSearchParamUtil.SORT_TYPE_NAME, (dialog, which) -> {
                        Shaft.sSettings.setSearchDefaultSortType(PixivSearchParamUtil.SORT_TYPE_VALUE[which]);
                        Common.showToast(getString(R.string.string_428), 2);
                        Local.setSettings(Shaft.sSettings);
                        baseBind.searchDefaultSortType.setText(PixivSearchParamUtil.SORT_TYPE_NAME[which]);
                        dialog.dismiss();
                    }
                ).create().show());

            // 过滤垃圾评论
            baseBind.filterComment.setChecked(Shaft.sSettings.isFilterComment());
            baseBind.filterComment.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setFilterComment(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.filterCommentRela.setOnClickListener(v -> baseBind.filterComment.performClick());

            // 默认开启R18内容过滤
            baseBind.r18FilterDefaultEnable.setChecked(Shaft.sSettings.isR18FilterDefaultEnable());
            baseBind.r18FilterDefaultEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setR18FilterDefaultEnable(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.r18FilterDefaultEnableRela.setOnClickListener(v -> baseBind.r18FilterDefaultEnable.performClick());
        }

        // 3. 界面
        {
            // APP主页显示R页面
            baseBind.mainViewR18.setChecked(Shaft.sSettings.isMainViewR18());
            baseBind.mainViewR18.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setMainViewR18(isChecked);
                Common.showToast(getString(R.string.please_restart_app), 2);
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.mainViewR18Rela.setOnClickListener(v -> baseBind.mainViewR18.performClick());

            // 首页导航栏初始化位置
            String navigationInitPositionSettingValue = Shaft.sSettings.getNavigationInitPosition();
            final String navigationInitPosition =
                !TextUtils.isEmpty(navigationInitPositionSettingValue) ? navigationInitPositionSettingValue : NavigationLocationHelper.TUIJIAN;
            baseBind.navigationInitPosition.setText(NavigationLocationHelper.SETTING_NAME_MAP.get(
                navigationInitPosition));
            baseBind.navigationInitPositionRela.setOnClickListener(v -> {
                String[] OPTION_VALUES =
                    NavigationLocationHelper.SETTING_NAME_MAP.keySet().toArray(new String[0]);
                String[] OPTION_NAMES =
                    NavigationLocationHelper.SETTING_NAME_MAP.values().toArray(new String[0]);
                String navigationInitPositionSettingValue1 =
                    Shaft.sSettings.getNavigationInitPosition();
                final String navigationInitPosition1 =
                    !TextUtils.isEmpty(navigationInitPositionSettingValue1) ? navigationInitPositionSettingValue1 : NavigationLocationHelper.TUIJIAN;
                final int index = Arrays.asList(OPTION_VALUES).indexOf(navigationInitPosition1);
                new QMUIDialog.CheckableDialogBuilder(mActivity).setCheckedIndex(index)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                        OPTION_NAMES, (dialog, which) -> {
                            if (which != index) {
                                Shaft.sSettings.setNavigationInitPosition(OPTION_VALUES[which]);
                                baseBind.navigationInitPosition.setText(OPTION_NAMES[which]);
                                Local.setSettings(Shaft.sSettings);
                            }
                            dialog.dismiss();
                        }
                    ).show();
            });

            // 主题模式
            baseBind.themeMode.setText(Shaft.sSettings.getThemeType().toDisplayString(mContext));
            baseBind.themeModeRela.setOnClickListener(v -> {
                final int index = Shaft.sSettings.getThemeType().themeTypeIndex;
                ThemeHelper.ThemeType[] THEME_MODES =
                    new ThemeHelper.ThemeType[]{DEFAULT_MODE, LIGHT_MODE, DARK_MODE};
                String[] THEME_NAME =
                    new String[]{THEME_MODES[0].toDisplayString(mContext), THEME_MODES[1].toDisplayString(
                        mContext), THEME_MODES[2].toDisplayString(mContext)};
                new QMUIDialog.CheckableDialogBuilder(mActivity).setCheckedIndex(index)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                        THEME_NAME, (dialog, which) -> {
                            if (which != index) {
                                Shaft.sSettings.setThemeType(
                                    ((AppCompatActivity) mActivity),
                                    THEME_MODES[which]
                                );
                                baseBind.themeMode.setText(THEME_NAME[which]);
                                Local.setSettings(Shaft.sSettings);
                            }
                            dialog.dismiss();
                        }
                    ).show();
            });

            // 主题色彩
            setThemeName();
            baseBind.colorSelectRela.setOnClickListener(v -> {
                AppRoute.Colors.INSTANCE.start(mContext);
            });

            baseBind.layoutMode.setText(Shaft.sSettings.isUseStaggeredLayout() ? getString(R.string.layout_staggered) : getString(
                R.string.layout_linear));
            baseBind.layoutModeRela.setOnClickListener(v -> {
                String[] options =
                    new String[]{getString(R.string.layout_staggered), getString(R.string.layout_linear)};
                int currentIndex = Shaft.sSettings.isUseStaggeredLayout() ? 0 : 1;
                new QMUIDialog.CheckableDialogBuilder(mActivity).setCheckedIndex(currentIndex)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                        options, (dialog, which) -> {
                            if (which != currentIndex) {
                                Shaft.sSettings.setUseStaggeredLayout(which == 0);
                                baseBind.layoutMode.setText(options[which]);
                                Local.setSettings(Shaft.sSettings);
                            }
                            dialog.dismiss();
                        }
                    ).show();
            });

            baseBind.lineCount.setText(getString(
                R.string.string_349,
                Shaft.sSettings.getLineCount()
            ));
            baseBind.lineCountRela.setOnClickListener(v -> {
                int index = 0;
                if (Shaft.sSettings.getLineCount() == 3) {
                    index = 1;
                } else if (Shaft.sSettings.getLineCount() == 4) {
                    index = 2;
                }
                String[] LINE_COUNT = new String[]{getString(
                    R.string.string_349,
                    2
                ), getString(R.string.string_349, 3), getString(R.string.string_349, 4)};
                final int selectIndex = index;
                new QMUIDialog.CheckableDialogBuilder(mActivity).setCheckedIndex(selectIndex)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                        LINE_COUNT, (dialog, which) -> {
                            if (which != selectIndex) {
                                int lineCount = which + 2;
                                Shaft.sSettings.setLineCount(lineCount);
                                baseBind.lineCount.setText(getString(
                                    R.string.string_349,
                                    lineCount
                                ));
                                Local.setSettings(Shaft.sSettings);
                                Common.showToast(getString(R.string.please_restart_app), 2);
                            }
                            dialog.dismiss();
                        }
                    ).show();
            });

            // 首页底部页签顺序
            setOrderName();
            baseBind.orderSelect.setOnClickListener(v -> {
                final int index = Shaft.sSettings.getBottomBarOrder();
                String[] ORDER_NAME =
                    new String[]{getString(R.string.string_343), getString(R.string.string_344), getString(
                        R.string.string_345), getString(R.string.string_346), getString(R.string.string_347), getString(
                        R.string.string_348),};
                new QMUIDialog.CheckableDialogBuilder(mActivity).setCheckedIndex(index)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                        ORDER_NAME, (dialog, which) -> {
                            if (which == index) {
                                Common.showLog("什么也不做");
                            } else {
                                Shaft.sSettings.setBottomBarOrder(which);
                                baseBind.orderSelect.setText(ORDER_NAME[which]);
                                Local.setSettings(Shaft.sSettings);
                                Common.showToast(getString(R.string.please_restart_app));
                            }
                            dialog.dismiss();
                        }
                    ).show();
            });
            baseBind.bottomBarOrderRela.setOnClickListener(v -> baseBind.orderSelect.performClick());

            // 语言
            baseBind.appLanguage.setText(currentLanguageDisplay());
            baseBind.appLanguageRela.setOnClickListener(v -> {
                // "跟随系统" 放在首位
                java.util.List<String> labels = new java.util.ArrayList<>();
                java.util.List<String> tags = new java.util.ArrayList<>();
                labels.add(getString(R.string.language_follow_system));
                tags.add(null);
                for (String tag : ceui.pixiv.i18n.AppLocales.INSTANCE.getSupportedTags()) {
                    labels.add(ceui.pixiv.i18n.AppLocales.INSTANCE.displayName(tag));
                    tags.add(tag);
                }
                int checkedIndex = 0; // default: follow system
                if (!ceui.pixiv.i18n.AppLocales.INSTANCE.isFollowingSystem()) {
                    String currentTag =
                        ceui.pixiv.i18n.AppLocales.INSTANCE.currentLocale().toLanguageTag();
                    int idx = tags.indexOf(currentTag);
                    if (idx >= 0) checkedIndex = idx;
                }
                new QMUIDialog.CheckableDialogBuilder(getActivity()).setCheckedIndex(checkedIndex)
                    .addItems(
                        labels.toArray(new String[0]), (dialog, which) -> {
                            String tag = tags.get(which);
                            ceui.pixiv.i18n.AppLocales.INSTANCE.apply(tag);
                            baseBind.appLanguage.setText(labels.get(which));
                            Common.showToast(getString(R.string.string_428), 2);
                            dialog.dismiss();
                        }
                    ).show();
            });
        }

        // 4. 下载
        {
            baseBind.r18DivideSave.setChecked(Shaft.sSettings.isR18DivideSave());
            baseBind.r18DivideSave.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setR18DivideSave(isChecked);
                Common.showToast(getString(R.string.string_428));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.r18DivideSaveRela.setOnClickListener(v -> baseBind.r18DivideSave.performClick());

            // AI作品下载至单独的目录
            baseBind.aiDivideSave.setChecked(Shaft.sSettings.isAIDivideSave());
            baseBind.aiDivideSave.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setAIDivideSave(isChecked);
                Common.showToast(getString(R.string.string_428));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.aiDivideSaveRela.setOnClickListener(v -> baseBind.aiDivideSave.performClick());

            // 下载路径 / 文件名 —— 所有分目录 / 命名 / 存储位置的配置都收在这一个入口
            baseBind.fileNameS.setText(getString(R.string.download_path_title));
            baseBind.fileName.setText(getString(R.string.download_path_entry_desc));
            baseBind.fileNameRela.setOnClickListener(v -> {
                AppRoute.DownloadPathSettings.INSTANCE.start(mContext);
            });

            // 下载内容信息头 —— 可视化勾选 / 拖拽排序小说 TXT 的元信息块
            baseBind.novelHeaderRela.setOnClickListener(v -> {
                AppRoute.NovelHeaderSettings.INSTANCE.start(mContext);
            });

            // 默认小说下载格式
            final String[] NOVEL_FORMAT_NAMES =
                new String[]{getString(R.string.option_always_ask), getString(R.string.format_txt), getString(
                    R.string.format_markdown), getString(R.string.format_epub), getString(R.string.format_pdf)};
            final String[] NOVEL_FORMAT_VALUES = new String[]{"", "Txt", "Markdown", "Epub", "Pdf"};
            {
                int idx = 0;
                String cur = Shaft.sSettings.getDefaultNovelExportFormat();
                for (int i = 0; i < NOVEL_FORMAT_VALUES.length; i++) {
                    if (NOVEL_FORMAT_VALUES[i].equals(cur)) {
                        idx = i;
                        break;
                    }
                }
                baseBind.defaultNovelFormat.setText(NOVEL_FORMAT_NAMES[idx]);
            }
            baseBind.defaultNovelFormatRela.setOnClickListener(v -> {
                int checkedIdx = 0;
                String cur = Shaft.sSettings.getDefaultNovelExportFormat();
                for (int i = 0; i < NOVEL_FORMAT_VALUES.length; i++) {
                    if (NOVEL_FORMAT_VALUES[i].equals(cur)) {
                        checkedIdx = i;
                        break;
                    }
                }
                new QMUIDialog.CheckableDialogBuilder(mActivity).setCheckedIndex(checkedIdx)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                        NOVEL_FORMAT_NAMES, (dialog, which) -> {
                            Shaft.sSettings.setDefaultNovelExportFormat(NOVEL_FORMAT_VALUES[which]);
                            baseBind.defaultNovelFormat.setText(NOVEL_FORMAT_NAMES[which]);
                            Local.setSettings(Shaft.sSettings);
                            dialog.dismiss();
                        }
                    ).show();
            });

            // 默认图片保存清晰度
            final String[] IMG_RES_NAMES =
                new String[]{getString(R.string.resolution_original), getString(R.string.resolution_large), getString(
                    R.string.resolution_medium), getString(R.string.resolution_square_medium)};
            final String[] IMG_RES_VALUES =
                new String[]{Params.IMAGE_RESOLUTION_ORIGINAL, Params.IMAGE_RESOLUTION_LARGE, Params.IMAGE_RESOLUTION_MEDIUM, Params.IMAGE_RESOLUTION_SQUARE_MEDIUM};
            {
                int idx = 0;
                String cur = Shaft.sSettings.getDefaultImageResolution();
                if (!cur.isEmpty()) {
                    for (int i = 0; i < IMG_RES_VALUES.length; i++) {
                        if (IMG_RES_VALUES[i].equals(cur)) {
                            idx = i;
                            break;
                        }
                    }
                }
                baseBind.defaultImageResolution.setText(IMG_RES_NAMES[idx]);
            }
            baseBind.defaultImageResolutionRela.setOnClickListener(v -> {
                int checkedIdx = 0;
                String cur = Shaft.sSettings.getDefaultImageResolution();
                if (!cur.isEmpty()) {
                    for (int i = 0; i < IMG_RES_VALUES.length; i++) {
                        if (IMG_RES_VALUES[i].equals(cur)) {
                            checkedIdx = i;
                            break;
                        }
                    }
                }
                new QMUIDialog.CheckableDialogBuilder(mActivity).setCheckedIndex(checkedIdx)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                        IMG_RES_NAMES, (dialog, which) -> {
                            Shaft.sSettings.setDefaultImageResolution(IMG_RES_VALUES[which]);
                            baseBind.defaultImageResolution.setText(IMG_RES_NAMES[which]);
                            Local.setSettings(Shaft.sSettings);
                            dialog.dismiss();
                        }
                    ).show();
            });

            // 文件重复时（OverwritePolicy）
            final String[] POLICY_NAMES =
                new String[]{getString(R.string.download_path_policy_skip), getString(R.string.download_path_policy_replace), getString(
                    R.string.download_path_policy_rename)};
            final OverwritePolicy[] POLICY_VALUES = OverwritePolicy.values();
            {
                OverwritePolicy cur =
                    DownloadsRegistry.getStore().loadOrFallback().getDefaults().getOverwrite();
                baseBind.overwritePolicy.setText(POLICY_NAMES[cur.ordinal()]);
            }
            baseBind.overwritePolicyRela.setOnClickListener(v -> {
                OverwritePolicy cur =
                    DownloadsRegistry.getStore().loadOrFallback().getDefaults().getOverwrite();
                new QMUIDialog.CheckableDialogBuilder(mActivity).setCheckedIndex(cur.ordinal())
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                        POLICY_NAMES, (dialog, which) -> {
                            OverwritePolicy selected = POLICY_VALUES[which];
                            DownloadsRegistry.getStore().update(cfg -> cfg.copy(
                                cfg.getVersion(), cfg.getDefaults().copy(
                                    cfg.getDefaults().getTemplate(),
                                    cfg.getDefaults().getStorage(),
                                    selected
                                ), cfg.getPerBucket(), cfg.getWifiOnly(), cfg.getPageIndexFrom1()
                            ));
                            baseBind.overwritePolicy.setText(POLICY_NAMES[which]);
                            dialog.dismiss();
                        }
                    ).show();
            });

            // 存储位置（StorageChoice）
            // 0 = Pictures, 1 = Downloads, 2 = SAF
            final String[] STORAGE_NAMES =
                new String[]{getString(R.string.setting_storage_pictures), getString(R.string.setting_storage_downloads), getString(
                    R.string.setting_storage_saf)};
            final Runnable refreshStorageLabel = () -> {
                StorageChoice cur = DownloadsRegistry.currentImagesStorage();
                int idx;
                if (cur instanceof StorageChoice.Saf) {
                    idx = 2;
                } else if (cur instanceof StorageChoice.MediaStore && ((StorageChoice.MediaStore) cur).getCollection() == StorageChoice.MediaStore.Collection.Downloads) {
                    idx = 1;
                } else {
                    idx = 0;
                }
                baseBind.storageChoice.setText(STORAGE_NAMES[idx]);
            };
            refreshStorageLabel.run();
            baseBind.storageChoiceRela.setOnClickListener(v -> {
                StorageChoice cur = DownloadsRegistry.currentImagesStorage();
                int checkedIdx;
                if (cur instanceof StorageChoice.Saf) {
                    checkedIdx = 2;
                } else if (cur instanceof StorageChoice.MediaStore && ((StorageChoice.MediaStore) cur).getCollection() == StorageChoice.MediaStore.Collection.Downloads) {
                    checkedIdx = 1;
                } else {
                    checkedIdx = 0;
                }
                new QMUIDialog.CheckableDialogBuilder(mActivity).setCheckedIndex(checkedIdx)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                        STORAGE_NAMES, (dialog, which) -> {
                            dialog.dismiss();
                            if (which == 0) {
                                DownloadsRegistry.applyGlobalStorage(new StorageChoice.MediaStore(
                                    StorageChoice.MediaStore.Collection.Images));
                                refreshStorageLabel.run();
                            } else if (which == 1) {
                                DownloadsRegistry.applyGlobalStorage(new StorageChoice.MediaStore(
                                    StorageChoice.MediaStore.Collection.Downloads));
                                refreshStorageLabel.run();
                            } else {
                                // SAF — launch document tree picker
                                ((BaseActivity<?>) mActivity).setFeedBack(() -> {
                                    String uriStr = Shaft.sSettings.getRootPathUri();
                                    if (uriStr != null && !uriStr.isEmpty()) {
                                        DownloadsRegistry.applyGlobalStorage(new StorageChoice.Saf(
                                            android.net.Uri.parse(uriStr)));
                                        refreshStorageLabel.run();
                                    }
                                });
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                ((BaseActivity<?>) mActivity).getTreeUriLauncher().launch(intent);
                            }
                        }
                    ).show();
            });

            // 多图页码起始（pageIndexFrom1）
            final String[] PAGE_INDEX_NAMES =
                new String[]{getString(R.string.setting_page_index_from_0), getString(R.string.setting_page_index_from_1)};
            {
                boolean from1 = DownloadsRegistry.getStore().loadOrFallback().getPageIndexFrom1();
                baseBind.pageIndex.setText(PAGE_INDEX_NAMES[from1 ? 1 : 0]);
            }
            baseBind.pageIndexRela.setOnClickListener(v -> {
                boolean from1 = DownloadsRegistry.getStore().loadOrFallback().getPageIndexFrom1();
                new QMUIDialog.CheckableDialogBuilder(mActivity).setCheckedIndex(from1 ? 1 : 0)
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                        PAGE_INDEX_NAMES, (dialog, which) -> {
                            boolean selected = which == 1;
                            DownloadsRegistry.getStore().update(cfg -> cfg.copy(
                                cfg.getVersion(),
                                cfg.getDefaults(),
                                cfg.getPerBucket(),
                                cfg.getWifiOnly(),
                                selected
                            ));
                            baseBind.pageIndex.setText(PAGE_INDEX_NAMES[which]);
                            dialog.dismiss();
                        }
                    ).show();
            });

            //插画详情长按下载
            baseBind.illustLongPressDownload.setChecked(Shaft.sSettings.isIllustLongPressDownload());
            baseBind.illustLongPressDownload.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setIllustLongPressDownload(isChecked);
                Common.showToast(getString(R.string.please_restart_app));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.illustLongPressDownloadRela.setOnClickListener(v -> baseBind.illustLongPressDownload.performClick());

            //下载限制类型
            final String[] DOWNLOAD_START_TYPE_NAMES =
                new String[]{getString(DownloadLimitTypeUtil.DOWNLOAD_START_TYPE_IDS[0]), getString(
                    DownloadLimitTypeUtil.DOWNLOAD_START_TYPE_IDS[1]), getString(
                    DownloadLimitTypeUtil.DOWNLOAD_START_TYPE_IDS[2])};
            baseBind.downloadLimitType.setText(DOWNLOAD_START_TYPE_NAMES[DownloadLimitTypeUtil.getCurrentStatusIndex()]);
            baseBind.downloadLimitType.setOnClickListener(v -> new QMUIDialog.CheckableDialogBuilder(mActivity).setCheckedIndex(Shaft.sSettings.getDownloadLimitType())
                .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addItems(
                    DOWNLOAD_START_TYPE_NAMES, (dialog, which) -> {
                        if (which == Shaft.sSettings.getDownloadLimitType()) {
                            Common.showLog("什么也不做");
                        } else {
                            Shaft.sSettings.setDownloadLimitType(which);
                            baseBind.downloadLimitType.setText(DOWNLOAD_START_TYPE_NAMES[DownloadLimitTypeUtil.getCurrentStatusIndex()]);
                            Common.showToast(getString(R.string.string_428));
                            Local.setSettings(Shaft.sSettings);
                        }
                        dialog.dismiss();
                    }
                ).show());
            baseBind.downloadLimitTypeRela.setOnClickListener(v -> baseBind.downloadLimitType.performClick());
        }

        // 5. 浏览与收藏 - 收藏交互部分
        {
            baseBind.showLikeButton.setChecked(Shaft.sSettings.isPrivateStar());
            baseBind.showLikeButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setPrivateStar(isChecked);
                Common.showToast(getString(R.string.string_428), 2);
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.showLikeButtonRela.setOnClickListener(v -> baseBind.showLikeButton.performClick());

            baseBind.showNovelCardTags.setChecked(Shaft.sSettings.isShowNovelCardTags());
            baseBind.showNovelCardTags.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setShowNovelCardTags(isChecked);
                Common.showToast(getString(R.string.string_428));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.showNovelCardTagsRela.setOnClickListener(v -> baseBind.showNovelCardTags.performClick());

            baseBind.hideStarBar.setChecked(Shaft.sSettings.isHideStarButtonAtMyCollection());
            baseBind.hideStarBar.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setHideStarButtonAtMyCollection(isChecked);
                Common.showToast(getString(R.string.string_428));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.hideStarButtonRela.setOnClickListener(v -> baseBind.hideStarBar.performClick());

            baseBind.selectAllTag.setChecked(Shaft.sSettings.isStarWithTagSelectAll());
            baseBind.selectAllTag.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setStarWithTagSelectAll(isChecked);
                Common.showToast(getString(R.string.string_428));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.selectAllTagRela.setOnClickListener(v -> baseBind.selectAllTag.performClick());

            baseBind.showRelatedWhenStar.setChecked(Shaft.sSettings.isShowRelatedWhenStar());
            baseBind.showRelatedWhenStar.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setShowRelatedWhenStar(isChecked);
                Common.showToast(getString(R.string.please_restart_app));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.showRelatedWhenStarRela.setOnClickListener(v -> baseBind.showRelatedWhenStar.performClick());

            baseBind.downloadAutoPostLike.setChecked(Shaft.sSettings.isAutoPostLikeWhenDownload());
            baseBind.downloadAutoPostLike.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setAutoPostLikeWhenDownload(isChecked);
                Common.showToast(getString(R.string.string_428));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.downloadAutoPostLikeRela.setOnClickListener(v -> baseBind.downloadAutoPostLike.performClick());

            baseBind.autoFollowAfterStar.setChecked(Shaft.sSettings.isAutoFollowAfterStar());
            baseBind.autoFollowAfterStar.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setAutoFollowAfterStar(isChecked);
                Common.showToast(getString(R.string.string_428));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.autoFollowAfterStarRela.setOnClickListener(v -> baseBind.autoFollowAfterStar.performClick());

            //插画二级详情保持屏幕常亮
            baseBind.illustDetailKeepScreenOn.setChecked(Shaft.sSettings.isIllustDetailKeepScreenOn());
            baseBind.illustDetailKeepScreenOn.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Shaft.sSettings.setIllustDetailKeepScreenOn(isChecked);
                Common.showToast(getString(R.string.string_428));
                Local.setSettings(Shaft.sSettings);
            });
            baseBind.illustDetailKeepScreenOnRela.setOnClickListener(v -> baseBind.illustDetailKeepScreenOn.performClick());
        }

        // 6. 缓存
        {
            baseBind.imageCacheSize.setText(AppKit.getSize(LegacyFile.imageCacheFolder(mContext)));
            baseBind.clearImageCache.setOnClickListener(v -> {
                AppKit.deleteAllInDir(LegacyFile.imageCacheFolder(mContext));
                Common.showToast(getString(R.string.success_clearImageCache));
                baseBind.imageCacheSize.setText(AppKit.getSize(LegacyFile.imageCacheFolder(
                    mContext)));
            });

            baseBind.gifCacheSize.setText(AppKit.getSize(LegacyFile.gifCacheFolder(mContext)));
            baseBind.clearGifCache.setOnClickListener(v -> {
                AppKit.deleteAllInDir(LegacyFile.gifCacheFolder(mContext));
                Common.showToast(getString(R.string.success_clearGifCache), 2);
                baseBind.gifCacheSize.setText(AppKit.getSize(LegacyFile.gifCacheFolder(
                    mContext)));
            });
        }

        // 7. 备份与还原
        {
            baseBind.backupRela.setOnClickListener(v -> {
                QMUIDialog.CheckBoxMessageDialogBuilder builder =
                    new QMUIDialog.CheckBoxMessageDialogBuilder(getActivity());
                builder.setTitle(getString(R.string.string_420))
                    .setMessage(getString(R.string.string_423))
                    .setSkinManager(QMUISkinManager.defaultInstance(mContext)).addAction(
                        getString(R.string.string_187), (dialog, index) -> dialog.dismiss()
                    ).addAction(
                        R.string.sure, (dialog, index) -> {
                            String backupString =
                                BackupUtils.getBackupString(mContext, builder.isChecked());
                            IllustDownload.downloadBackupFile(
                                (BaseActivity<?>) mActivity,
                                "Shaft-Backup.json",
                                backupString,
                                t -> Common.showToast(getString(R.string.backup_success) + Settings.FILE_PATH_BACKUP)
                            );
                            dialog.dismiss();
                        }
                    ).create().show();
            });

            baseBind.restoreRela.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Uri backupFileUri = Uri.parse(
                        "content://com.android.externalstorage.documents/document/primary:" + "Download%2fShaftBackups%2fShaft-Backup.json");
                    intent.putExtra(EXTRA_INITIAL_URI, backupFileUri);
                }
                openDocumentLauncher.launch(intent);
            });
        }

        baseBind.refreshLayout.setRefreshHeader(new FalsifyHeader(mContext));
        baseBind.refreshLayout.setRefreshFooter(new FalsifyFooter(mContext));

        if (!Common.isAndroidQ()) {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    Common.showToast(getString(R.string.access_denied));
                    finish();
                }
            }).launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public SmartRefreshLayout getSmartRefreshLayout() {
        return baseBind.refreshLayout;
    }

    private void setOrderName() {
        final int index = Shaft.sSettings.getBottomBarOrder();
        String[] ORDER_NAME =
            new String[]{getString(R.string.string_343), getString(R.string.string_344), getString(R.string.string_345), getString(
                R.string.string_346), getString(R.string.string_347), getString(R.string.string_348),};
        baseBind.orderSelect.setText(ORDER_NAME[index]);
    }

    private void setThemeName() {
        final int index = Shaft.sSettings.getThemeIndex();
        baseBind.colorSelect.setText(getString(FragmentColors.COLOR_NAME_CODES[index]));
    }

    private void updateImageProxyUrlUI() {
        String url = Shaft.sSettings.getImageProxyUrl();
        baseBind.imageProxyUrlValue.setText(url);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private String currentLanguageDisplay() {
        if (ceui.pixiv.i18n.AppLocales.INSTANCE.isFollowingSystem()) {
            return getString(R.string.language_follow_system);
        }
        Locale loc = ceui.pixiv.i18n.AppLocales.INSTANCE.currentLocale();
        return ceui.pixiv.i18n.AppLocales.INSTANCE.displayName(loc.toLanguageTag());
    }

    @Override
    protected FragmentSettingsBinding onCreateBinding(
        @NonNull LayoutInflater inflater,
        ViewGroup container,
        boolean attachToParent
    ) {
        return FragmentSettingsBinding.inflate(inflater, container, false);
    }
}
