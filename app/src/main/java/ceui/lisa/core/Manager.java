package ceui.lisa.core;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ceui.lisa.R;
import ceui.lisa.activities.Shaft;
import ceui.lisa.database.AppDatabase;
import ceui.lisa.database.DownloadEntity;
import ceui.lisa.database.DownloadingEntity;
import ceui.lisa.download.DownloadFileFactory;
import ceui.lisa.download.DownloadProgress;
import ceui.lisa.download.MediaStoreUtil;
import ceui.lisa.helper.Android10DownloadFactory22;
import ceui.lisa.helper.SAFactory;
import ceui.lisa.interfaces.Callback;
import ceui.lisa.model.Holder;
import ceui.lisa.utils.Common;
import ceui.lisa.utils.Dev;
import ceui.lisa.utils.DownloadLimitTypeUtil;
import ceui.lisa.utils.Params;
import ceui.lisa.utils.PixivOperate;
import ceui.pixiv.ui.task.TaskPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Manager {

    private final Context mContext = Shaft.getContext();
    private List<DownloadItem> content = new ArrayList<>();
    private ThreadUtil.DownloadHandle handle = null;
    private boolean isRunning = false;

    private volatile OkHttpClient mDownloadOkHttpClient;

    private OkHttpClient getDownloadOkHttpClient() {
        OkHttpClient cached = mDownloadOkHttpClient;
        if (cached != null) return cached;
        synchronized (this) {
            if (mDownloadOkHttpClient == null) {
                OkHttpClient base = ((Shaft) Shaft.getContext()).getOkHttpClient();
                mDownloadOkHttpClient = base.newBuilder()
                        .protocols(java.util.Collections.singletonList(okhttp3.Protocol.HTTP_1_1))
                        .build();
            }
            return mDownloadOkHttpClient;
        }
    }

    private Manager() {
        uuid = "";
        currentIllustID = 0;
    }

    private static final int MAX_RESTORE_ITEMS = 100;

    public void restore() {
        ThreadUtil.INSTANCE.runOnIo(() -> {
            try {
                AppDatabase db = AppDatabase.getAppDatabase(mContext);
                db.downloadDao().trimDownloading(MAX_RESTORE_ITEMS);
                List<DownloadingEntity> downloadingEntities =
                        db.downloadDao().getRecentDownloading(MAX_RESTORE_ITEMS);
                if (Common.isEmpty(downloadingEntities)) {
                    return;
                }
                Common.showLog("downloadingEntities " + downloadingEntities.size());
                List<DownloadItem> restored = new ArrayList<>();
                for (DownloadingEntity entity : downloadingEntities) {
                    try {
                        DownloadItem downloadItem = Shaft.sGson.fromJson(entity.getTaskGson(), DownloadItem.class);
                        if (downloadItem != null) {
                            restored.add(downloadItem);
                        }
                    } catch (Exception ex) {
                        Common.showLog("Manager restore parse error: " + ex.getMessage());
                    }
                }
                synchronized (this) {
                    content = restored;
                }
                ThreadUtil.INSTANCE.runOnMain(() ->
                        Common.showToast("下载记录恢复成功"));
            } catch (Throwable t) {
                Common.showLog("Manager restore failed: " + t.getMessage());
            }
        });
    }

    public static Manager get() {
        return Manager.SingletonHolder.INSTANCE;
    }

    private static class SingletonHolder {
        private static final Manager INSTANCE = new Manager();
    }

    public void addTask(DownloadItem bean) {
        synchronized (this) {
            if (content == null) {
                content = new ArrayList<>();
            }

            long t0 = System.nanoTime();
            boolean isTaskExist = false;
            for (DownloadItem item : content) {
                if (item.isSame(bean)) {
                    isTaskExist = true;
                }
            }
            long dedupMs = (System.nanoTime() - t0) / 1_000_000;

            if (!isTaskExist) {
                long t1 = System.nanoTime();
                safeAdd(bean);
                long safeAddMs = (System.nanoTime() - t1) / 1_000_000;
                Common.showLog("[PERF] addTask #" + content.size()
                        + " dedupMs=" + dedupMs + " safeAddMs=" + safeAddMs);
            }
            if(DownloadLimitTypeUtil.startTaskWhenCreate()){
                startAll();
            }
        }
    }

    private void safeAdd(DownloadItem item) {
        Common.showLog("Manager safeAdd " + item.getUuid());
        content.add(item);
        DownloadingEntity entity = new DownloadingEntity();
        entity.setFileName(item.getName());
        entity.setUuid(item.getUuid());

        long t0 = System.nanoTime();
        entity.setTaskGson(Shaft.sGson.toJson(item));
        long gsonMs = (System.nanoTime() - t0) / 1_000_000;

        long t1 = System.nanoTime();
        AppDatabase.getAppDatabase(Shaft.getContext()).downloadDao().insertDownloading(entity);
        long dbMs = (System.nanoTime() - t1) / 1_000_000;

        Common.showLog("[PERF] safeAdd gsonMs=" + gsonMs + " dbInsertMs=" + dbMs);
    }

    private void complete(DownloadItem item, boolean isDownloadSuccess) {
        if (isDownloadSuccess) {
            item.setState(DownloadItem.DownloadState.SUCCESS);
            setCallback(uuid, null);

            DownloadingEntity entity = new DownloadingEntity();
            entity.setFileName(item.getName());
            entity.setUuid(item.getUuid());
            entity.setTaskGson(Shaft.sGson.toJson(item));
            AppDatabase.getAppDatabase(mContext).downloadDao().deleteDownloading(entity);
        } else {
            item.setNonius(0);
            item.setState(DownloadItem.DownloadState.FAILED);
        }
    }

    public void addTasks(List<DownloadItem> list) {
        if (Common.isEmpty(list)) return;

        ThreadUtil.INSTANCE.runOnIo(() -> {
            long t0 = System.nanoTime();
            synchronized (this) {
                if (content == null) {
                    content = new ArrayList<>();
                }
                java.util.Set<String> existingUrls = new java.util.HashSet<>();
                for (DownloadItem existing : content) {
                    existingUrls.add(existing.getUrl());
                }
                for (DownloadItem item : list) {
                    if (!existingUrls.contains(item.getUrl())) {
                        safeAdd(item);
                        existingUrls.add(item.getUrl());
                    }
                }
            }
            long totalMs = (System.nanoTime() - t0) / 1_000_000;
            Common.showLog("[PERF] addTasks total=" + list.size()
                    + " items, totalMs=" + totalMs);
            ThreadUtil.INSTANCE.runOnMain(() -> {
                if (DownloadLimitTypeUtil.startTaskWhenCreate()) {
                    startAll();
                }
            });
        });
    }

    public void startAll() {
        if (!Common.isEmpty(content)) {
            for (DownloadItem item : content) {
                item.setPaused(false);
                if (item.getState() == DownloadItem.DownloadState.FAILED) {
                    item.setState(DownloadItem.DownloadState.INIT);
                }
            }
        }
        if (isRunning) {
            Common.showLog("Manager 正在下载中，不用多次start");
            return;
        }
        isRunning = true;
        loop();
    }

    public void startOne(String uuid) {
        for (int i = 0; i < content.size(); i++) {
            DownloadItem downloadItem = content.get(i);
            if (downloadItem != null && downloadItem.getUuid().equals(uuid)) {
                downloadItem.setPaused(false);
                if(downloadItem.getState() == DownloadItem.DownloadState.FAILED){
                    downloadItem.setState(DownloadItem.DownloadState.INIT);
                }
                Common.showLog("已开始 " + uuid);
                break;
            }
        }

        if (isRunning) {
            Common.showLog("Manager 正在下载中，不用多次start");
            return;
        }
        isRunning = true;
        loop();
    }

    public void stopAll() {
        for (DownloadItem item : getContent()) {
            item.setPaused(true);
        }
        isRunning = false;
        if (handle != null) {
            handle.dispose();
        }
        Common.showLog("已经停止");
    }

    public void stopOne(String uuid){
        for (DownloadItem item : getContent()) {
            if(item.getUuid().equals(uuid)){
                item.setPaused(true);
                Common.showLog("已暂停 " + uuid);
                break;
            }
        }
        if(this.uuid.equals(uuid) && handle != null){
            handle.dispose();
        }
    }

    public void clearAll() {
        stopAll();
        AppDatabase.getAppDatabase(mContext).downloadDao().deleteAllDownloading();
        content.clear();
    }

    public void clearOne(String uuid) {
        stopOne(uuid);
        Optional<DownloadItem> item = content.stream().filter(it -> it.getUuid().equals(uuid)).findFirst();
        if (item.isPresent()) {
            DownloadItem downloadItem = item.get();
            DownloadingEntity entity = new DownloadingEntity();
            entity.setFileName(downloadItem.getName());
            entity.setUuid(downloadItem.getUuid());
            entity.setTaskGson(Shaft.sGson.toJson(downloadItem));
            AppDatabase.getAppDatabase(mContext).downloadDao().deleteDownloading(entity);
            content.remove(downloadItem);
        }
    }

    private void loop() {
        if (Common.isEmpty(content.stream().filter(it->!it.isPaused()).collect(Collectors.toList()))) {
            isRunning = false;
            Common.showLog("Manager 已经全部下载完成");
            return;
        }
        if(!isRunning){
            return;
        }

        DownloadItem item = getFirstOne();
        if (item != null) {
            downloadOne(mContext, item);
        } else {
            stopAll();
        }
    }

    private DownloadItem getFirstOne() {
        for (int i = 0; i < content.size(); i++) {
            DownloadItem downloadItem = content.get(i);
            if (downloadItem.getState() == DownloadItem.DownloadState.INIT || downloadItem.getState() == DownloadItem.DownloadState.DOWNLOADING) {
                return downloadItem;
            }
        }
        return null;
    }

    private void downloadOne(Context context, DownloadItem downloadItem) {
        Common.showLog("[DL-CACHE] downloadOne enter uuid=" + downloadItem.getUuid()
                + " name=" + downloadItem.getName() + " url=" + downloadItem.getUrl());
        if(!DownloadLimitTypeUtil.canDownloadNow()){
            stopAll();
            return;
        }

        currentIllustID = downloadItem.getIllust().getId();
        uuid = downloadItem.getUuid();
        Common.showLog("Manager 下载单个 当前进度" + downloadItem.getNonius());

        ThreadUtil.INSTANCE.runOnIo(() -> {
            DownloadFileFactory factory;
            try {
                if (Shaft.sSettings.getDownloadWay() == 0 || downloadItem.getIllust().isGif()) {
                    factory = new Android10DownloadFactory22(context, downloadItem);
                } else {
                    factory = new SAFactory(context, downloadItem);
                }
            } catch (Exception e) {
                Common.showLog("[DL] factory init failed: " + e);
                e.printStackTrace();
                ThreadUtil.INSTANCE.runOnMain(() -> {
                    Common.showToast(mContext.getString(R.string.string_365));
                    complete(downloadItem, false);
                    stopAll();
                });
                return;
            }

            boolean shouldSkip =
                    (factory instanceof Android10DownloadFactory22 && ((Android10DownloadFactory22) factory).isSkip())
                 || (factory instanceof SAFactory && ((SAFactory) factory).isSkip());
            if (shouldSkip) {
                Common.showLog("[DL] skip download (already exists), illust=" + downloadItem.getIllust().getId());
                complete(downloadItem, true);
                ThreadUtil.INSTANCE.runOnMain(() -> {
                    content.remove(downloadItem);
                    loop();
                });
                return;
            }

            long fileSize = MediaStoreUtil.length(factory.query(), context);
            long passSize = (!downloadItem.shouldStartNewDownload() && fileSize >= 0) ? fileSize : 0;

            Uri targetUri;
            try {
                targetUri = factory.insert();
            } catch (Exception e) {
                Common.showLog("[DL] factory.insert() failed: " + e);
                e.printStackTrace();
                ThreadUtil.INSTANCE.runOnMain(() -> {
                    Common.showToast(mContext.getString(R.string.string_365));
                    complete(downloadItem, false);
                    stopAll();
                });
                return;
            }
            if (targetUri == null) {
                Common.showLog("[DL] factory.insert() returned null targetUri");
                ThreadUtil.INSTANCE.runOnMain(() -> {
                    Common.showToast(mContext.getString(R.string.string_365));
                    complete(downloadItem, false);
                    stopAll();
                });
                return;
            }

            final String dlUrl = downloadItem.getUrl();
            final boolean isGif = downloadItem.getIllust().isGif();
            final File cachedFile;
            if (passSize != 0) {
                Common.showLog("[DL-CACHE] skip peek, passSize=" + passSize + " (resume), url=" + dlUrl);
                cachedFile = null;
            } else if (isGif) {
                Common.showLog("[DL-CACHE] skip peek, illust isGif, url=" + dlUrl);
                cachedFile = null;
            } else {
                File peeked = TaskPool.peekCachedFile(dlUrl);
                if (peeked != null) {
                    Common.showLog("[DL-CACHE] HIT path=" + peeked.getAbsolutePath()
                            + " size=" + peeked.length() + " url=" + dlUrl);
                    cachedFile = peeked;
                } else {
                    Common.showLog("[DL-CACHE] MISS url=" + dlUrl);
                    cachedFile = null;
                }
            }

            ThreadUtil.INSTANCE.runOnMain(() ->
                startDownloadChain(context, downloadItem, factory, cachedFile, targetUri, dlUrl, passSize));
        });
    }

    private void startDownloadChain(Context context, DownloadItem downloadItem,
            DownloadFileFactory factory, File cachedFile, Uri targetUri, String dlUrl, long passSize) {
        OkHttpClient client = getDownloadOkHttpClient();
        Request.Builder reqBuilder = new Request.Builder()
                .url(downloadItem.getUrl())
                .addHeader(Params.MAP_KEY, Params.IMAGE_REFERER);
        if (passSize > 0) {
            reqBuilder.addHeader("Range", "bytes=" + passSize + "-");
        }
        Request request = reqBuilder.build();

        handle = new ThreadUtil.DownloadHandle();

        ThreadUtil.INSTANCE.runOnIo(() -> {
            Response response = null;
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                long contentLength;
                long copyStartNs = System.nanoTime();
                if (cachedFile != null) {
                    Common.showLog("[DL-CACHE] begin local copy, src=" + cachedFile.getAbsolutePath()
                            + " dst=" + targetUri);
                    inputStream = new java.io.FileInputStream(cachedFile);
                    contentLength = cachedFile.length();
                } else {
                    Common.showLog("[DL-CACHE] begin network fetch, url=" + dlUrl
                            + " passSize=" + passSize + " dst=" + targetUri);
                    response = client.newCall(request).execute();
                    if (!response.isSuccessful()) {
                        Common.showLog("[DL-CACHE] network HTTP " + response.code() + " url=" + dlUrl);
                        throw new IOException("HTTP " + response.code());
                    }
                    ResponseBody body = response.body();
                    if (body == null) {
                        Common.showLog("[DL-CACHE] network empty body url=" + dlUrl);
                        throw new IOException("Empty response body");
                    }
                    inputStream = body.byteStream();
                    contentLength = body.contentLength();
                }

                long totalSize = contentLength > 0 ? contentLength + passSize : 0;
                Common.showLog("[DL-CACHE] contentLength=" + contentLength + " totalSize=" + totalSize
                        + " source=" + (cachedFile != null ? "cache" : "network"));

                if ("file".equals(targetUri.getScheme())) {
                    String path = targetUri.getPath();
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(path, passSize > 0);
                    outputStream = fos;
                } else {
                    outputStream = context.getContentResolver().openOutputStream(targetUri, passSize > 0 ? "wa" : "w");
                }
                if (outputStream == null) {
                    throw new IOException("Cannot open output stream for " + targetUri);
                }

                byte[] buffer = new byte[8192];
                long downloaded = passSize;
                int lastProgress = 0;
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    if (handle != null && handle.isDisposed()) {
                        return;
                    }
                    outputStream.write(buffer, 0, len);
                    downloaded += len;
                    if (totalSize > 0) {
                        int progress = (int) (downloaded * 100 / totalSize);
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            long finalDownloaded = downloaded;
                            long finalTotal = totalSize;
                            ThreadUtil.INSTANCE.runOnMain(() -> {
                                DownloadProgress dp = new DownloadProgress(progress, finalDownloaded, finalTotal);
                                downloadItem.setNonius(progress);
                                downloadItem.setCurrentSize(finalDownloaded);
                                downloadItem.setTotalSize(finalTotal);
                                downloadItem.setState(DownloadItem.DownloadState.DOWNLOADING);
                                Common.showLog("currentProgress " + progress);
                                try {
                                    Callback<DownloadProgress> c = getCallback(uuid);
                                    if (c != null) {
                                        c.doSomething(dp);
                                    }
                                } catch (Exception e) {
                                    Common.showLog("Manager progress callback error: " + e.getMessage());
                                }
                            });
                        }
                    }
                }
                outputStream.flush();
                long elapsedMs = (System.nanoTime() - copyStartNs) / 1_000_000L;
                Common.showLog("[DL-CACHE] write done source=" + (cachedFile != null ? "cache" : "network")
                        + " bytes=" + downloaded + " elapsedMs=" + elapsedMs + " dst=" + targetUri);

                Common.showLog("downloadOne " + targetUri.toString());

                if (downloadItem.getIllust().isGif()) {
                    Shaft.getDefaultPrefs().edit()
                        .putBoolean(Params.ILLUST_ID + "_" + downloadItem.getIllust().getId(), true)
                        .apply();
                    ThreadUtil.INSTANCE.runOnMain(() ->
                        PixivOperate.unzipAndPlay(
                            context,
                            downloadItem.getIllust(),
                            downloadItem.isAutoSave()
                        ));
                }

                DownloadEntity downloadEntity = new DownloadEntity();
                downloadEntity.setIllustGson(Shaft.sGson.toJson(downloadItem.getIllust()));
                downloadEntity.setFileName(downloadItem.getName());
                downloadEntity.setDownloadTime(System.currentTimeMillis());
                downloadEntity.setFilePath(factory.getFileUri().toString());
                AppDatabase.getAppDatabase(Shaft.getContext()).downloadDao().insert(downloadEntity);
                Common.showLog("[DL-CACHE] db inserted DownloadEntity fileName=" + downloadEntity.getFileName()
                    + " filePath=" + downloadEntity.getFilePath());

                factory.finishWrite();
                complete(downloadItem, true);

                ThreadUtil.INSTANCE.runOnMain(() -> {
                    int sizeBefore = content.size();
                    boolean removed = content.remove(downloadItem);
                    Common.showLog("[DL-REMOVE] remove=" + removed + " sizeBefore=" + sizeBefore
                        + " sizeAfter=" + content.size() + " name=" + downloadItem.getName());
                    if (Shaft.sSettings.isToastDownloadResult()) {
                        Common.showToast(downloadItem.getName() + mContext.getString(R.string.has_been_downloaded));
                    }
                    {
                        Intent intent = new Intent(Params.DOWNLOAD_ING);
                        Holder holder = new Holder();
                        holder.setCode(Params.DOWNLOAD_SUCCESS);
                        holder.setDownloadItem(downloadItem);
                        intent.putExtra(Params.CONTENT, holder);
                        LocalBroadcastManager.getInstance(Shaft.getContext()).sendBroadcast(intent);
                        Common.showLog("[DL-REMOVE] DOWNLOAD_ING broadcast sent");
                    }
                    {
                        Intent intent = new Intent(Params.DOWNLOAD_FINISH);
                        intent.putExtra(Params.CONTENT, downloadEntity);
                        LocalBroadcastManager.getInstance(Shaft.getContext()).sendBroadcast(intent);
                    }
                });
            } catch (Exception e) {
                if (handle == null || !handle.isDisposed()) {
                    Common.showLog("Manager download error: " + e.getMessage());
                    if (Shaft.sSettings.isToastDownloadResult()) {
                        ThreadUtil.INSTANCE.runOnMain(() ->
                            Common.showToast("下载失败，原因：" + e.toString()));
                    }
                    Common.showLog("下载失败，原因：" + e.toString());
                    complete(downloadItem, false);
                    ThreadUtil.INSTANCE.runOnMain(() -> {
                        Intent intent = new Intent(Params.DOWNLOAD_ING);
                        Holder holder = new Holder();
                        holder.setCode(Params.DOWNLOAD_FAILED);
                        holder.setIndex(content.indexOf(downloadItem));
                        holder.setDownloadItem(downloadItem);
                        intent.putExtra(Params.CONTENT, holder);
                        LocalBroadcastManager.getInstance(Shaft.getContext()).sendBroadcast(intent);
                    });
                }
            } finally {
                if (inputStream != null) try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
                if (outputStream != null) try {
                    outputStream.close();
                } catch (Exception ignored) {
                }
                if (response != null) try {
                    response.close();
                } catch (Exception ignored) {
                }
                currentIllustID = 0;
                Common.showLog("doFinally ");
                ThreadUtil.INSTANCE.runOnMain(this::loop);
            }
        });
    }

    private String uuid;
    private int currentIllustID;

    public int getCurrentIllustID() {
        return currentIllustID;
    }

    public String getUuid() {
        return uuid;
    }

    private final Map<String, Callback<DownloadProgress>> mCallback = new HashMap<>();

    public Callback<DownloadProgress> getCallback(String uuid) {
        return mCallback.getOrDefault(uuid, null);
    }

    public void clearCallback() {
        mCallback.clear();
    }

    public void setCallback(Callback<DownloadProgress> callback) {
        mCallback.put("", callback);
    }

    public void setCallback(String uuid, Callback<DownloadProgress> callback) {
        mCallback.put(uuid, callback);
    }

    public List<DownloadItem> getContent() {
        return content;
    }
}