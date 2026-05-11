package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListNovel
import retrofit2.Call

class UserNovelRepo @JvmOverloads constructor(
    private val userID: Int,
    private val initialOffset: Int = 0
) : RemoteRepo<ListNovel>() {

    override fun initApi(): Call<ListNovel> {
        return if (initialOffset > 0) {
            Retro.getAppApi().getNextNovel(
                "https://app-api.pixiv.net/v1/user/novels?user_id=$userID&offset=$initialOffset"
            )
        } else {
            Retro.getAppApi().getUserSubmitNovel(userID)
        }
    }

    override fun initNextApi(): Call<ListNovel> {
        return Retro.getAppApi().getNextNovel(nextUrl)
    }
}