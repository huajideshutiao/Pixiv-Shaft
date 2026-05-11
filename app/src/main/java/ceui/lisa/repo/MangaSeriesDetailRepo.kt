package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListMangaOfSeries
import retrofit2.Call

class MangaSeriesDetailRepo(private val seriesID: Int) : RemoteRepo<ListMangaOfSeries>() {

    override fun initApi(): Call<ListMangaOfSeries> {
        return Retro.getAppApi().getMangaSeriesById(seriesID)
    }

    override fun initNextApi(): Call<ListMangaOfSeries>? {
        return null
    }
}