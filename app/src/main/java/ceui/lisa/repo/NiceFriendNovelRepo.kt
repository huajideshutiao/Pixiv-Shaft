package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListNovel
import retrofit2.Call

class NiceFriendNovelRepo : RemoteRepo<ListNovel>() {

    override fun initApi(): Call<ListNovel> {
        return Retro.getAppApi().getNiceFriendNovel()
    }

    override fun initNextApi(): Call<ListNovel> {
        return Retro.getAppApi().getNextNovel(nextUrl)
    }
}