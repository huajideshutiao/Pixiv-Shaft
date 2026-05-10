package ceui.lisa.models

data class UserFollowDetail(
    var follow_detail: FollowDetail? = null
) {
    fun isFollow(): Boolean = follow_detail?.is_followed ?: false

    fun isPublicFollow(): Boolean {
        val detail = follow_detail ?: return false
        return detail.is_followed && detail.restrict == Restrict.PUBLIC
    }

    fun isPrivateFollow(): Boolean {
        val detail = follow_detail ?: return false
        return detail.is_followed && detail.restrict == Restrict.PRIVATE
    }

    data class FollowDetail(
        var is_followed: Boolean = false,
        var restrict: String? = null
    )

    private object Restrict {
        const val PUBLIC = "public"
        const val PRIVATE = "private"
    }
}