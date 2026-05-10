package ceui.lisa.models

import java.io.Serializable

open class UserModel : UserHolder(), UserContainer, Starable {
    @JvmField
    var access_token: String? = null
    var expires_in: Int = 0
    var token_type: String? = null
    var scope: String? = null
    var refresh_token: String? = null
    var device_token: String? = null
    var local_user: String? = null

    @JvmName("getAccess_token")
    fun accessTokenBearer(): String = "Bearer $access_token"

    fun getRawAccessToken(): String? = access_token

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