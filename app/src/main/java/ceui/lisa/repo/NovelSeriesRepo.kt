package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListNovelSeries
import retrofit2.Call

class NovelSeriesRepo(private val userID: Int) : RemoteRepo<ListNovelSeries>() {

    override fun initApi(): Call<ListNovelSeries> {
        return Retro.getAppApi().getUserNovelSeries(userID)
    }

    override fun initNextApi(): Call<ListNovelSeries>? {
        return Retro.getAppApi().getNextUserNovelSeries(nextUrl)
    }
}