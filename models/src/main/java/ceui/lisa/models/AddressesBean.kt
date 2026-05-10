package ceui.lisa.models

data class AddressesBean(
    var id: Int = 0,
    var name: String? = null,
    var is_global: Boolean? = null
) {
    override fun toString(): String = name ?: ""
}