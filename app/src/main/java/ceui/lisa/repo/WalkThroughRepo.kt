package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListIllust
import retrofit2.Call

class WalkThroughRepo : RemoteRepo<ListIllust>() {

    override fun initApi(): Call<ListIllust> {
        return Retro.getAppApi().getLoginBg()
    }

    override fun initNextApi(): Call<ListIllust>? {
        return null
    }

    override fun hasEffectiveUserFollowStatus(): Boolean {
        return false
    }
}