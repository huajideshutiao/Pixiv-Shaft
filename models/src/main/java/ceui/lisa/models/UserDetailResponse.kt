package ceui.lisa.models

open class UserDetailResponse : UserHolder(), UserContainer {
    var profile: ProfileBean? = null
    var profile_publicity: ProfilePublicityBean? = null
    var workspace: WorkspaceBean? = null

    override fun getUserId(): Int = user?.id ?: 0
}