package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListIllust
import ceui.lisa.model.RecmdIllust
import retrofit2.Call

open class RecmdIllustRepo(
    private val dataType: String?
) : RemoteRepo<ListIllust>() {

    override fun initApi(): Call<RecmdIllust> {
        val source = if ("漫画" == dataType) {
            Retro.getAppApi().getRecmdManga()
        } else {
            Retro.getAppApi().getRecmdIllust(true)
        }
        return source
    }

    override fun initNextApi(): Call<ListIllust> {
        return Retro.getAppApi().getNextIllust(nextUrl)
    }

    companion object {
        const val RankingIllustTag = "RankingIllustTag"
    }
}