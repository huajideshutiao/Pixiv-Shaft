package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListTrendingtag
import retrofit2.Call

class HotTagRepo(
    private val contentType: String?
) : RemoteRepo<ListTrendingtag>() {

    override fun initApi(): Call<ListTrendingtag> {
        return Retro.getAppApi().getHotTags(contentType)
    }

    override fun initNextApi(): Call<ListTrendingtag>? {
        return null
    }
}