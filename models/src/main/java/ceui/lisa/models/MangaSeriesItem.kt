package ceui.lisa.models

import java.io.Serializable

data class MangaSeriesItem(
    var id: Int = 0,
    var title: String? = null,
    var caption: String? = null,
    var cover_image_urls: ImageUrlsBean? = null,
    var series_work_count: Int = 0,
    var create_date: String? = null,
    var width: Int = 0,
    var height: Int = 0,
    var user: UserBean? = null
) : Serializable