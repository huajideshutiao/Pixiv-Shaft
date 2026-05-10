package ceui.lisa.models

import java.io.Serializable

data class NovelSeriesItem(
    var id: Int = 0,
    var title: String? = null,
    var caption: String? = null,
    var is_original: Boolean = false,
    var is_concluded: Boolean = false,
    var content_count: Int = 0,
    var total_character_count: Int = 0,
    var user: UserBean? = null,
    var display_text: String? = null,
    var watchlist_added: Boolean = false
) : Serializable