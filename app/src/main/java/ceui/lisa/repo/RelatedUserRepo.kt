package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListUser
import retrofit2.Call

class RelatedUserRepo(
    private val userID: Int,
) : RemoteRepo<ListUser>() {

    override fun initApi(): Call<ListUser> {
        return Retro.getAppApi().getRelatedUsers(userID)
    }

    override fun initNextApi(): Call<ListUser> {
        return Retro.getAppApi().getNextUser(nextUrl)
    }
}