package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListUser
import ceui.lisa.utils.Dev
import retrofit2.Call

class RecmdUserRepo(private val isHorizontal: Boolean) : RemoteRepo<ListUser>() {

    override fun initApi(): Call<ListUser> {
        return Retro.getAppApi().getRecmdUser()
    }

    override fun initNextApi(): Call<ListUser>? {
        if (isHorizontal) {
            return null
        }
        return Retro.getAppApi().getNextUser(nextUrl)
    }
}