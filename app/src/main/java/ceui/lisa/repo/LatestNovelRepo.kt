package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListNovel
import retrofit2.Call

class LatestNovelRepo : RemoteRepo<ListNovel>() {

    override fun initApi(): Call<ListNovel> {
        return Retro.getAppApi().getNewNovels()
    }

    override fun initNextApi(): Call<ListNovel> {
        return Retro.getAppApi().getNextNovel(nextUrl)
    }
}