package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListMangaSeries
import retrofit2.Call

class MangaSeriesRepo(private val userID: Int) : RemoteRepo<ListMangaSeries>() {

    override fun initApi(): Call<ListMangaSeries> {
        return Retro.getAppApi().getUserMangaSeries(userID)
    }

    override fun initNextApi(): Call<ListMangaSeries>? {
        return Retro.getAppApi().getNextUserMangaSeries(nextUrl)
    }
}