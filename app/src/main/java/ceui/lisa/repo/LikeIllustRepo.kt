package ceui.lisa.repo

import android.text.TextUtils
import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListIllust
import retrofit2.Call

class LikeIllustRepo(
    private val userID: Int,
    private val starType: String?,
    var tag: String?
) : RemoteRepo<ListIllust>() {

    override fun initApi(): Call<ListIllust> {
        return if (TextUtils.isEmpty(tag)) {
            Retro.getAppApi().getUserLikeIllust(userID, starType)
        } else {
            Retro.getAppApi().getUserLikeIllust(userID, starType, tag)
        }
    }

    override fun initNextApi(): Call<ListIllust> {
        return Retro.getAppApi().getNextIllust(nextUrl)
    }
}