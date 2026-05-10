package ceui.lisa.models

import java.io.Serializable

open class UserPreviewsBean : UserHolder(), UserContainer, Starable {
    var is_muted: Boolean = false
    var illusts: List<IllustsBean>? = null
    var novels: List<NovelBean>? = null

    override fun getUserId(): Int = user?.id ?: 0

    override fun getItemID(): Int = user?.id ?: 0

    override fun setItemID(id: Int) {
        user?.id = id
    }

    override fun isItemStared(): Boolean = user?.is_followed ?: false

    override fun setItemStared(isLiked: Boolean) {
        user?.is_followed = isLiked
    }
}