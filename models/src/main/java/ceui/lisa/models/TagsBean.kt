package ceui.lisa.models

import java.io.Serializable

data class TagsBean(
    var name: String? = null,
    var translated_name: String? = null,
    var effective: Boolean = true,
    var added_by_uploaded_user: Boolean = false,
    var is_registered: Boolean = false,
    var count: Int = 0,
    var isSelected: Boolean = false,
    var filter_mode: Int = 0
) : Serializable {

    fun isSelectedLocalOrRemote(): Boolean = isSelected || is_registered

    fun setSelectedLocalAndRemote(selected: Boolean) {
        isSelected = selected
        is_registered = selected
    }
}