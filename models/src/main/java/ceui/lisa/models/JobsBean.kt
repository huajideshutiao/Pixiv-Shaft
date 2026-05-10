package ceui.lisa.models

data class JobsBean(
    var id: Int = 0,
    var name: String? = null
) {
    override fun toString(): String = name ?: ""
}