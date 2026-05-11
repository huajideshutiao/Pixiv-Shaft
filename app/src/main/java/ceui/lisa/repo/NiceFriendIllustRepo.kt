package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListIllust
import retrofit2.Call

class NiceFriendIllustRepo : RemoteRepo<ListIllust>() {

    override fun initApi(): Call<ListIllust> {
        return Retro.getAppApi().getNiceFriendIllust()
    }

    override fun initNextApi(): Call<ListIllust> {
        return Retro.getAppApi().getNextIllust(nextUrl)
    }
}