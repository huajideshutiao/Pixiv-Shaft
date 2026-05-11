package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListWatchlistManga
import retrofit2.Call

class WatchlistMangaRepo: RemoteRepo<ListWatchlistManga>() {
    override fun initApi(): Call<ListWatchlistManga> {
        return Retro.getAppApi().getWatchlistManga()
    }

    override fun initNextApi(): Call<ListWatchlistManga> {
        return Retro.getAppApi().getNextWatchlistManga(nextUrl)
    }
}