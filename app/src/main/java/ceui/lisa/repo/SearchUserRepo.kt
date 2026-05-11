package ceui.lisa.repo

import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListUser
import retrofit2.Call

class SearchUserRepo(private var word: String?) : RemoteRepo<ListUser>() {

    override fun initApi(): Call<ListUser> {
        return Retro.getAppApi().searchUser(word)
    }

    override fun initNextApi(): Call<ListUser> {
        return Retro.getAppApi().getNextUser(nextUrl)
    }

    fun update(keyWord: String){
        word=keyWord
    }
}