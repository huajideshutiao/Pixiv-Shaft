package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListSimpleUser
import retrofit2.Call

class SimpleUserRepo(private val illustID: Int) : RemoteRepo<ListSimpleUser>() {

    override fun initApi(): Call<ListSimpleUser> {
        return Retro.getAppApi().getUsersWhoLikeThisIllust(illustID)
    }

    override fun initNextApi(): Call<ListSimpleUser> {
        return Retro.getAppApi().getNextSimpleUser(nextUrl)
    }
}