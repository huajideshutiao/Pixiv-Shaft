package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListArticle
import ceui.lisa.utils.Dev
import retrofit2.Call

open class PivisionRepo(
    private val dataType: String?,
    private val isHorizontal: Boolean
) : RemoteRepo<ListArticle>() {

    override fun initApi(): Call<ListArticle> {
        return Retro.getAppApi().getArticles(dataType)
    }

    override fun initNextApi(): Call<ListArticle>? {
        if (isHorizontal) {
            return null
        }
        return Retro.getAppApi().getNextArticles(nextUrl)
    }
}