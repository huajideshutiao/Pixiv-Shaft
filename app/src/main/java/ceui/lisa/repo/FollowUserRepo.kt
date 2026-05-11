package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListUser
import retrofit2.Call

class FollowUserRepo(
    private val userID: Int,
    private val starType: String?
) : RemoteRepo<ListUser>() {

    override fun initApi(): Call<ListUser> {
        return Retro.getAppApi().getFollowUser(userID, starType)
    }

    override fun initNextApi(): Call<ListUser> {
        return Retro.getAppApi().getNextUser(nextUrl)
    }
}