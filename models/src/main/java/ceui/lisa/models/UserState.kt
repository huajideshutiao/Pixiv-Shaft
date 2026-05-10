package ceui.lisa.models

data class UserState(
    var user_state: UserStateBean? = null
) {
    data class UserStateBean(
        var is_mail_authorized: Boolean = false,
        var has_changed_pixiv_id: Boolean = false,
        var can_change_pixiv_id: Boolean = false,
        var has_password: Boolean = false
    ) : java.io.Serializable
}