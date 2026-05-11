package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListNovelMarkers
import retrofit2.Call

class NovelMarkersRepo: RemoteRepo<ListNovelMarkers>() {
    override fun initApi(): Call<ListNovelMarkers> {
        return Retro.getAppApi().getNovelMarkers()
    }

    override fun initNextApi(): Call<ListNovelMarkers> {
        return Retro.getAppApi().getNextNovelMarkers(nextUrl)
    }
}