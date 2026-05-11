package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListUser
import retrofit2.Call

class NiceFriendRepo(private val userId: Int) : RemoteRepo<ListUser>() {

    override fun initApi(): Call<ListUser> {
        return Retro.getAppApi().getNiceFriend(userId)
    }

    override fun initNextApi(): Call<ListUser> {
        return Retro.getAppApi().getNextUser(nextUrl)
    }
}