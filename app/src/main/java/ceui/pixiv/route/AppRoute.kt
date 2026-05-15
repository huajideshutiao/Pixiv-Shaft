package ceui.pixiv.route

import android.content.Context
import android.content.Intent
import ceui.lisa.activities.ContainerActivity
import ceui.lisa.models.IllustsBean
import ceui.lisa.models.NovelBean
import ceui.lisa.utils.Params
import ceui.lisa.utils.ReverseResult

enum class RouteType {
    LOGIN_REGISTER,
    RELATED_ILLUST,
    HISTORY_TABS,
    WEB_LINK,
    SETTINGS,
    RECMD_USER,
    PV,
    SEARCH_USER,
    REVERSE_SEARCH,
    COMMENTS,
    LOCAL_USERS,
    BOOKED_TAG,
    SB_TAG,
    ABOUT,
    BATCH_DOWNLOAD,
    BULK_SELECT,
    WALK_THROUGH,
    FOLLOWING,
    NICE_FRIEND,
    USER_INFO,
    NEW_WORKS,
    FANS,
    LIKE_USERS,
    NOVEL_SERIES_DETAIL,
    USER_ILLUST,
    USER_MANGA,
    LIKE_ILLUST,
    DOWNLOAD_MANAGER,
    RECMD_ILLUST_MANGA,
    RECMD_NOVEL,
    LIKE_NOVEL,
    USER_NOVEL,
    NOVEL_DETAIL,
    NOVEL_READER,
    NOVEL_SERIES,
    UNCATEGORIZED_NOVELS,
    IMAGE_DETAIL,
    URL_IMAGE,
    DOWNLOAD_IMAGE,
    FULL_SCREEN,
    EDIT_ACCOUNT,
    EDIT_FILE,
    VIEW_PAGER_MUTED,
    DOWNLOAD_PATH_SETTINGS,
    NOVEL_HEADER_SETTINGS,
    FOLLOWING_NOVELS,
    MANGA_SERIES,
    MANGA_SERIES_DETAIL,
    NOVEL_SERIES_WORKS,
    WORK_SPACE,
    COLLECTION_ILLUST,
    COLLECTION_NOVEL,
    COLLECTION_WATCHLIST,
    COLLECTION_FOLLOWING,
    NOVEL_MARKERS,
    COLORS,
    FLAG_DESC,
    RELATED_USER,
    MARKDOWN,
    VERSION_HISTORY,
}

sealed class AppRoute(val type: RouteType) {

    abstract fun writeExtras(intent: Intent)

    fun start(context: Context) {
        val intent = Intent(context, ContainerActivity::class.java)
        intent.putExtra(ContainerActivity.EXTRA_FRAGMENT, type.name)
        writeExtras(intent)
        context.startActivity(intent)
    }

    val postponesTransition: Boolean
        get() = type == RouteType.IMAGE_DETAIL ||
                type == RouteType.DOWNLOAD_IMAGE ||
                type == RouteType.URL_IMAGE

    val hideStatusBarByDefault: Boolean
        get() = type != RouteType.COMMENTS

    // ====== 登录/账号 ======

    object LoginRegister : AppRoute(RouteType.LOGIN_REGISTER) {
        override fun writeExtras(intent: Intent) {}
    }

    object LocalUsers : AppRoute(RouteType.LOCAL_USERS) {
        override fun writeExtras(intent: Intent) {}
    }

    // ====== 图片详情 ======

    data class ImageDetail(val illust: IllustsBean?, val index: Int) : AppRoute(RouteType.IMAGE_DETAIL) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra("illust", illust)
            intent.putExtra("index", index)
        }
    }

    data class UrlImage(val url: String, val saveName: String?) : AppRoute(RouteType.URL_IMAGE) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.URL, url)
            if (!saveName.isNullOrBlank()) intent.putExtra(Params.TITLE, saveName)
        }
    }

    data class DownloadImage(val localIllust: List<String>?, val index: Int) : AppRoute(RouteType.DOWNLOAD_IMAGE) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra("illust", ArrayList(localIllust ?: emptyList()))
            intent.putExtra("index", index)
        }
    }

    data class FullScreen(val pageUUID: String?, val position: Int, val seed: String?) : AppRoute(RouteType.FULL_SCREEN) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.PAGE_UUID, pageUUID)
            intent.putExtra(Params.POSITION, position)
            if (!seed.isNullOrEmpty()) intent.putExtra(Params.SEED, seed)
        }
    }

    // ====== 作品列表 ======

    data class RelatedIllust(val illustId: Int, val illustTitle: String?) : AppRoute(RouteType.RELATED_ILLUST) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.ILLUST_ID, illustId)
            if (!illustTitle.isNullOrEmpty()) intent.putExtra(Params.ILLUST_TITLE, illustTitle)
        }
    }

    // ====== Web / 杂项 ======

    data class WebLink(val url: String, val title: String?) : AppRoute(RouteType.WEB_LINK) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.URL, url)
            if (!title.isNullOrEmpty()) intent.putExtra(Params.TITLE, title)
        }
    }

    data class ReverseSearch(val result: ReverseResult) : AppRoute(RouteType.REVERSE_SEARCH) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.REVERSE_SEARCH_RESULT, result)
        }
    }

    object Pv : AppRoute(RouteType.PV) {
        override fun writeExtras(intent: Intent) {}
    }

    object WalkThrough : AppRoute(RouteType.WALK_THROUGH) {
        override fun writeExtras(intent: Intent) {}
    }

    data class Markdown(val url: String?) : AppRoute(RouteType.MARKDOWN) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.URL, url)
        }
    }

    // ====== 用户相关 ======

    data class RecmdUser(val recmdKey: String?) : AppRoute(RouteType.RECMD_USER) {
        override fun writeExtras(intent: Intent) {
            if (!recmdKey.isNullOrEmpty()) intent.putExtra(Params.USER_MODEL, recmdKey)
        }
    }

    data class SearchUser(val keyword: String?) : AppRoute(RouteType.SEARCH_USER) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(ContainerActivity.EXTRA_KEYWORD, keyword)
        }
    }

    data class Following(val userId: Int) : AppRoute(RouteType.FOLLOWING) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.USER_ID, userId)
        }
    }

    object NiceFriend : AppRoute(RouteType.NICE_FRIEND) {
        override fun writeExtras(intent: Intent) {}
    }

    object UserInfo : AppRoute(RouteType.USER_INFO) {
        override fun writeExtras(intent: Intent) {}
    }

    object NewWorks : AppRoute(RouteType.NEW_WORKS) {
        override fun writeExtras(intent: Intent) {}
    }

    data class Fans(val userId: Int) : AppRoute(RouteType.FANS) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.USER_ID, userId)
        }
    }

    data class LikeUsers(val illust: IllustsBean) : AppRoute(RouteType.LIKE_USERS) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.CONTENT, illust)
        }
    }

    data class RelatedUser(val userId: Int) : AppRoute(RouteType.RELATED_USER) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.USER_ID, userId)
        }
    }

    // ====== 用户作品/收藏列表 ======

    data class UserIllust(
        val userId: Int,
        val initialOffset: Int = 0,
        val targetDate: String? = null
    ) : AppRoute(RouteType.USER_ILLUST) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.USER_ID, userId)
            intent.putExtra(Params.INITIAL_OFFSET, initialOffset)
            if (!targetDate.isNullOrEmpty()) intent.putExtra(Params.TARGET_DATE, targetDate)
        }
    }

    data class UserManga(
        val userId: Int,
        val initialOffset: Int = 0,
        val targetDate: String? = null
    ) : AppRoute(RouteType.USER_MANGA) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.USER_ID, userId)
            intent.putExtra(Params.INITIAL_OFFSET, initialOffset)
            if (!targetDate.isNullOrEmpty()) intent.putExtra(Params.TARGET_DATE, targetDate)
        }
    }

    data class UserNovel(val userId: Int) : AppRoute(RouteType.USER_NOVEL) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.USER_ID, userId)
        }
    }

    data class LikeIllust(val userId: Int) : AppRoute(RouteType.LIKE_ILLUST) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.USER_ID, userId)
        }
    }

    data class LikeNovel(val userId: Int) : AppRoute(RouteType.LIKE_NOVEL) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.USER_ID, userId)
        }
    }

    data class MangaSeries(val userId: Int) : AppRoute(RouteType.MANGA_SERIES) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.USER_ID, userId)
        }
    }

    data class MangaSeriesDetail(val seriesId: Int) : AppRoute(RouteType.MANGA_SERIES_DETAIL) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.MANGA_SERIES_ID, seriesId)
        }
    }

    object NovelSeriesWorks : AppRoute(RouteType.NOVEL_SERIES_WORKS) {
        override fun writeExtras(intent: Intent) {}
    }

    object RecmdIllustManga : AppRoute(RouteType.RECMD_ILLUST_MANGA) {
        override fun writeExtras(intent: Intent) {}
    }

    object RecmdNovel : AppRoute(RouteType.RECMD_NOVEL) {
        override fun writeExtras(intent: Intent) {}
    }

    object FollowingNovels : AppRoute(RouteType.FOLLOWING_NOVELS) {
        override fun writeExtras(intent: Intent) {}
    }

    // ====== 设置 ======

    object Settings : AppRoute(RouteType.SETTINGS) {
        override fun writeExtras(intent: Intent) {}
    }

    object EditAccount : AppRoute(RouteType.EDIT_ACCOUNT) {
        override fun writeExtras(intent: Intent) {}
    }

    object EditFile : AppRoute(RouteType.EDIT_FILE) {
        override fun writeExtras(intent: Intent) {}
    }

    object Colors : AppRoute(RouteType.COLORS) {
        override fun writeExtras(intent: Intent) {}
    }

    object DownloadPathSettings : AppRoute(RouteType.DOWNLOAD_PATH_SETTINGS) {
        override fun writeExtras(intent: Intent) {}
    }

    object NovelHeaderSettings : AppRoute(RouteType.NOVEL_HEADER_SETTINGS) {
        override fun writeExtras(intent: Intent) {}
    }

    object About : AppRoute(RouteType.ABOUT) {
        override fun writeExtras(intent: Intent) {}
    }

    object VersionHistory : AppRoute(RouteType.VERSION_HISTORY) {
        override fun writeExtras(intent: Intent) {}
    }

    // ====== 收藏 / 历史 ======

    object HistoryTabs : AppRoute(RouteType.HISTORY_TABS) {
        override fun writeExtras(intent: Intent) {}
    }

    object CollectionIllust : AppRoute(RouteType.COLLECTION_ILLUST) {
        override fun writeExtras(intent: Intent) {}
    }

    object CollectionNovel : AppRoute(RouteType.COLLECTION_NOVEL) {
        override fun writeExtras(intent: Intent) {}
    }

    object CollectionFollowing : AppRoute(RouteType.COLLECTION_FOLLOWING) {
        override fun writeExtras(intent: Intent) {}
    }

    object CollectionWatchlist : AppRoute(RouteType.COLLECTION_WATCHLIST) {
        override fun writeExtras(intent: Intent) {}
    }

    object NovelMarkers : AppRoute(RouteType.NOVEL_MARKERS) {
        override fun writeExtras(intent: Intent) {}
    }

    object WorkSpace : AppRoute(RouteType.WORK_SPACE) {
        override fun writeExtras(intent: Intent) {}
    }

    object ViewPagerMuted : AppRoute(RouteType.VIEW_PAGER_MUTED) {
        override fun writeExtras(intent: Intent) {}
    }

    // ====== 下载 ======

    object BatchDownload : AppRoute(RouteType.BATCH_DOWNLOAD) {
        override fun writeExtras(intent: Intent) {}
    }

    object BulkSelect : AppRoute(RouteType.BULK_SELECT) {
        override fun writeExtras(intent: Intent) {}
    }

    object DownloadManager : AppRoute(RouteType.DOWNLOAD_MANAGER) {
        override fun writeExtras(intent: Intent) {}
    }

    // ====== 标签 ======

    data class BookedTag(val dataType: Int, val keyword: String?) : AppRoute(RouteType.BOOKED_TAG) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.DATA_TYPE, dataType)
            intent.putExtra(ContainerActivity.EXTRA_KEYWORD, keyword)
        }
    }

    data class SbTag(val illustId: Int, val dataType: String?, val tagNames: Array<String>?) : AppRoute(RouteType.SB_TAG) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.ILLUST_ID, illustId)
            intent.putExtra(Params.DATA_TYPE, dataType)
            intent.putExtra(Params.TAG_NAMES, tagNames)
        }
    }

    // ====== 评论 ======

    data class Comments(val workId: Int, val isNovel: Boolean = false) : AppRoute(RouteType.COMMENTS) {
        override fun writeExtras(intent: Intent) {
            if (isNovel) intent.putExtra(Params.NOVEL_ID, workId)
            else intent.putExtra(Params.ILLUST_ID, workId)
        }
    }

    // ====== 小说 ======

    data class NovelDetail(val novelId: Long) : AppRoute(RouteType.NOVEL_DETAIL) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.NOVEL_ID, novelId)
        }
    }

    data class NovelReader(val novelId: Long, val novelBean: NovelBean? = null) : AppRoute(RouteType.NOVEL_READER) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.NOVEL_ID, novelId)
            if (novelBean != null) intent.putExtra(Params.CONTENT, novelBean)
        }
    }

    data class NovelSeries(val seriesId: Long) : AppRoute(RouteType.NOVEL_SERIES) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra("series_id", seriesId)
        }
    }

    data class NovelSeriesDetail(val id: Int) : AppRoute(RouteType.NOVEL_SERIES_DETAIL) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.ID, id)
        }
    }

    data class UncategorizedNovels(val userId: Int) : AppRoute(RouteType.UNCATEGORIZED_NOVELS) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra(Params.USER_ID, userId)
        }
    }

    // ====== 举报 ======

    data class FlagDesc(val reasonId: Int, val objectId: Int, val objectType: Int) : AppRoute(RouteType.FLAG_DESC) {
        override fun writeExtras(intent: Intent) {
            intent.putExtra("flag_reason_id", reasonId)
            intent.putExtra("flag_object_id", objectId)
            intent.putExtra("flag_object_type", objectType)
        }
    }
}