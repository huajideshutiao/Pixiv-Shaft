package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListNovelOfSeries
import retrofit2.Call

class NovelSeriesDetailRepo constructor(private val seriesID: Int) : RemoteRepo<ListNovelOfSeries>() {

    override fun initApi(): Call<ListNovelOfSeries> {
        return Retro.getAppApi().getNovelSeries(seriesID)
    }

    override fun initNextApi(): Call<ListNovelOfSeries> {
        return Retro.getAppApi().getNextSeriesNovel(nextUrl)
    }
}