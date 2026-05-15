package ceui.lisa.interfaces;

import android.content.Context;

import java.util.List;

import ceui.lisa.models.IllustsBean;
import ceui.pixiv.route.AppRoute;
import ceui.pixiv.ui.bulk.BulkSelectStorage;

/**
 * 旧入口（列表长按 / popup "批量下载"）。
 * 跳到 V3 风格的多选页 BulkSelectV3Fragment，让用户勾选要下哪些。
 */
public interface MultiDownload {

    Context getContext();

    List<IllustsBean> getIllustList();

    default void startDownload() {
        List<IllustsBean> list = getIllustList();
        if (list == null || list.isEmpty()) return;
        BulkSelectStorage.INSTANCE.put(list);
        AppRoute.BulkSelect.INSTANCE.start(getContext());
    }
}
