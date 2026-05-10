package ceui.lisa.models

open class MetaSinglePageBean : ImageUrlsBean() {
    var original_image_url: String? = null

    override var original: String?
        get() = original_image_url
        set(value) {}
}