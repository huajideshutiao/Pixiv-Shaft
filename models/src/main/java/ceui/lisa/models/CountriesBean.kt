package ceui.lisa.models

data class CountriesBean(
    var code: String? = null,
    var name: String? = null
) {
    override fun toString(): String = name ?: ""
}