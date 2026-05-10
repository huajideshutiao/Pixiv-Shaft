package ceui.lisa.models

import java.io.Serializable

data class UserBean(
    var profile_image_urls: ProfileImageUrlsBean? = null,
    var id: Int = 0,
    var name: String? = null,
    var comment: String? = null,
    var account: String? = null,
    var password: String? = null,
    var mail_address: String? = null,
    @get:JvmName("isIs_login") var is_login: Boolean = false,
    @get:JvmName("isIs_premium") var is_premium: Boolean = false,
    @get:JvmName("isIs_followed") var is_followed: Boolean = false,
    var lastTokenTime: Long = -1,
    var x_restrict: Int = 0,
    @get:JvmName("isIs_mail_authorized") var is_mail_authorized: Boolean = false,
    var require_policy_agreement: Boolean = false
) : Serializable, UserContainer, Starable, ModelObject {

    override fun getUserId(): Int = id

    override fun getItemID(): Int = id

    override fun setItemID(id: Int) {
        this.id = id
    }

    override fun isItemStared(): Boolean = is_followed

    override fun setItemStared(isLiked: Boolean) {
        is_followed = isLiked
    }

    fun isR18Enabled(): Boolean = x_restrict != 0

    fun isR18GEnabled(): Boolean = x_restrict == 2

    override val objectUniqueId: Long
        get() = id.toLong()

    override val objectType: Int
        get() = ObjectSpec.USER
}