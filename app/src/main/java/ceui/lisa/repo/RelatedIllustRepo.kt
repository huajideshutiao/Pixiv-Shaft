package ceui.lisa.repo

import ceui.lisa.activities.Shaft
import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListIllust
import retrofit2.Call

class RelatedIllustRepo(private val illustID: Int) : RemoteRepo<ListIllust>() {

    override fun initApi(): Call<ListIllust> {
        return Retro.getAppApi().relatedIllust(illustID)
    }

    override fun initNextApi(): Call<ListIllust> {
        return Retro.getAppApi().getNextIllust(getNextUrl())
    }

    override fun hasNext(): Boolean {
        return Shaft.sSettings.isRelatedIllustNoLimit
    }
}