package ceui.lisa.models

data class MutedHistory(
    var muted_count: Int = 0,
    var muted_tags_count: Int = 0,
    var muted_users_count: Int = 0,
    var mute_limit_count: Int = 0,
    var muted_tags: List<MutedTagsBean>? = null,
    var muted_users: List<MutedUsersBean>? = null
) {

    data class MutedTagsBean(
        var tag: TagsBean? = null,
        var is_premium_slot: Boolean = false
    )
}