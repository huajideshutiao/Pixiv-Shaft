package ceui.lisa.models

import java.io.Serializable

data class NovelDetail(
    var novel_marker: NovelMarkerBean? = null,
    var novel_text: String? = null,
    var series_prev: NovelBean? = null,
    var series_next: NovelBean? = null,
    var parsedChapters: List<NovelChapterBean>? = null
) : Serializable {

    data class NovelMarkerBean(
        var page: Int = 0
    ) : Serializable

    data class NovelChapterBean(
        var chapterIndex: Int = 0,
        var chapterName: String? = null,
        var chapterContent: String? = null
    ) : Serializable
}