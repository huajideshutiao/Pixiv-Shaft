package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListWatchlistNovel
import retrofit2.Call

class WatchlistNovelRepo: RemoteRepo<ListWatchlistNovel>() {
    override fun initApi(): Call<ListWatchlistNovel> {
        return Retro.getAppApi().getWatchlistNovel()
    }

    override fun initNextApi(): Call<ListWatchlistNovel> {
        return Retro.getAppApi().getNextWatchlistNovel(nextUrl)
    }
}