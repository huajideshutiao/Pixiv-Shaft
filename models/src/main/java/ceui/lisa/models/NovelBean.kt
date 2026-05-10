package ceui.lisa.models

import java.io.Serializable

data class NovelBean(
    var id: Int = 0,
    var title: String? = null,
    var coverUrl: String? = null,
    var caption: String? = null,
    var restrict: Int = 0,
    var x_restrict: Int = 0,
    var is_original: Boolean = false,
    var viewable: Boolean = false,
    var image_urls: ImageUrlsBean? = null,
    var create_date: String? = null,
    var contentOrder: String? = null,
    var page_count: Int = 0,
    var text_length: Int = 0,
    var user: UserBean? = null,
    var series: SeriesBean? = null,
    @get:JvmName("isIs_bookmarked") var is_bookmarked: Boolean = false,
    var total_bookmarks: Int = 0,
    var total_view: Int = 0,
    var visible: Boolean = false,
    var isLocalSaved: Boolean = false,
    var total_comments: Int = 0,
    var is_muted: Boolean = false,
    var is_mypixiv_only: Boolean = false,
    var is_x_restricted: Boolean = false,
    var tags: List<TagsBean>? = null,
    var is_concluded: Boolean = false,
    var content_count: Int = 0,
    var total_character_count: Int = 0,
    var display_text: String? = null
) : Serializable, Starable, ModelObject {

    override fun getItemID(): Int = id
    override fun setItemID(id: Int) {
        this.id = id
    }

    override fun isItemStared(): Boolean = is_bookmarked
    override fun setItemStared(isLiked: Boolean) {
        is_bookmarked = isLiked
    }

    fun getTagString(): String {
        val tags = this.tags ?: return ""
        if (tags.isEmpty()) return ""
        return tags.joinToString("") { "*#${it.name}," }
    }

    val tagNames: Array<String>
        get() {
            val tags = this.tags ?: return emptyArray()
            return tags.mapNotNull { it.name }.toTypedArray()
        }

    override val objectUniqueId: Long
        get() = id.toLong()

    override val objectType: Int
        get() = ObjectSpec.JNOVEL
}