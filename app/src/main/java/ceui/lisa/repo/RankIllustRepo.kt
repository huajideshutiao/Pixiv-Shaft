package ceui.lisa.repo

import ceui.lisa.activities.Shaft
import ceui.lisa.core.Mapper
import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListIllust
import ceui.lisa.utils.PixivOperate
import retrofit2.Call
import java.util.function.Function

class RankIllustRepo(
    private val mode: String?,
    private val date: String?
) : RemoteRepo<ListIllust>() {

    override fun initApi(): Call<ListIllust> {
        return Retro.getAppApi().getRank(mode, date)
    }

    override fun initNextApi(): Call<ListIllust> {
        return Retro.getAppApi().getNextIllust(nextUrl)
    }

    override fun mapper(): Function<ListIllust, ListIllust> {
        return Function { listIllust ->
            val mapped = Mapper<ListIllust>().apply(listIllust)
            if (Shaft.sSettings.isFilterRankBookmarked) {
                val filtered = PixivOperate.getListWithoutBooked(mapped)
                mapped.setIllusts(filtered)
            }
            mapped
        }
    }
}