package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListNovel
import ceui.lisa.utils.Params
import retrofit2.Call

class NewNovelRepo @JvmOverloads constructor(
    var restrict: String = Params.TYPE_ALL,
) : RemoteRepo<ListNovel>() {

    override fun initApi(): Call<ListNovel> {
        return Retro.getAppApi().getBookedUserSubmitNovel(restrict)
    }

    override fun initNextApi(): Call<ListNovel> {
        return Retro.getAppApi().getNextNovel(nextUrl)
    }
}