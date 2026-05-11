package ceui.lisa.models

import java.io.Serializable

data class IllustsBean(
    var id: Int = 0,
    var title: String? = null,
    var type: String? = null,
    var image_urls: ImageUrlsBean? = null,
    var caption: String? = null,
    var restrict: Int = 0,
    var isChecked: Boolean = false,
    var isRelated: Boolean = false,
    var user: UserBean? = null,
    var create_date: String? = null,
    var page_count: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var sanity_level: Int = 0,
    var x_restrict: Int = 0,
    var series: SeriesBean? = null,
    var meta_single_page: MetaSinglePageBean? = null,
    var total_view: Int = 0,
    var total_bookmarks: Int = 0,
    var illust_ai_type: Int = 0,
    @get:JvmName("isIs_bookmarked") var is_bookmarked: Boolean = false,
    @get:JvmName("isVisible") var visible: Boolean = false,
    var is_muted: Boolean = false,
    var tags: List<TagsBean>? = null,
    var tools: List<String>? = null,
    var meta_pages: List<MetaPagesBean>? = null,
    var isShield: Boolean = false
) : Serializable, Starable, Deduplicatable, ModelObject {

    val isGif: Boolean
        get() = "ugoira" == type

    fun getSize(): String = "${width}px * ${height}px"

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

    override fun getItemID(): Int = id
    override fun setItemID(id: Int) {
        this.id = id
    }

    override fun isItemStared(): Boolean = is_bookmarked
    override fun setItemStared(isLike: Boolean) {
        is_bookmarked = isLike
    }

    val isR18File: Boolean
        get() = x_restrict == 1 || sanity_level >= 4

    override fun getDuplicateKey(): Any = id

    override val objectUniqueId: Long
        get() = id.toLong()

    override val objectType: Int
        get() = ObjectSpec.POST

    val isCreatedByAI: Boolean
        get() = illust_ai_type == IllustAIType.CreatedByAI

    object IllustAIType {
        const val CreatedByHuman = 1
        const val CreatedByAI = 2
    }
}