package ceui.lisa.models

data class SpotlightArticlesBean(
    var id: Int = 0,
    var title: String? = null,
    var pure_title: String? = null,
    var thumbnail: String? = null,
    var article_url: String? = null,
    var publish_date: String? = null,
    var category: String? = null,
    var subcategory_label: String? = null
) : java.io.Serializable