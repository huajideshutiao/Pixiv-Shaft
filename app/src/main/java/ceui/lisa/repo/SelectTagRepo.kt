package ceui.lisa.repo

import ceui.lisa.core.NetCallback
import ceui.lisa.core.executeCall
import ceui.lisa.core.RemoteRepo
import ceui.lisa.http.Retro
import ceui.lisa.model.ListBookmarkTag
import ceui.lisa.model.ListTag
import ceui.lisa.utils.Params
import retrofit2.Call
import java.util.function.Function

class SelectTagRepo(
        private val id: Int,
        private val type: String,
        private val tagNames: List<String>,
) : RemoteRepo<ListBookmarkTag>() {

    var listTag: ListTag? = null

    override fun initApi(): Call<ListBookmarkTag>? {
        return null
    }

    override fun initNextApi(): Call<ListBookmarkTag>? {
        return null
    }

    override fun getFirstData(netCallback: NetCallback<ListBookmarkTag>) {
        val api2 = when (type) {
            Params.TYPE_ILLUST -> {
                Retro.getAppApi().getAllIllustBookmarkTags(currentUserID(), Params.TYPE_PUBLIC)
            }
            Params.TYPE_NOVEL -> {
                Retro.getAppApi().getAllNovelBookmarkTags(currentUserID(), Params.TYPE_PUBLIC)
            }

            else -> throw IllegalArgumentException("Unknown type: $type")
        }

        executeCall(api2, Function.identity(), object : NetCallback<ListTag>() {
            override fun onSuccess(t: ListTag) {
                listTag = t
                val api1 = when (type) {
                    Params.TYPE_ILLUST -> {
                        Retro.getAppApi().getIllustBookmarkTags(id)
                    }

                    Params.TYPE_NOVEL -> {
                        Retro.getAppApi().getNovelBookmarkTags(id)
                    }

                    else -> throw IllegalArgumentException("Unknown type: $type")
                }
                executeCall(api1, mapper(), netCallback)
            }

            override fun onError(e: Throwable) {
                super.onError(e)
                netCallback.must(false)
            }
        })
    }

    override fun mapper(): Function<ListBookmarkTag, ListBookmarkTag> {
        return Function { listBookmarkTag ->
            val tags = listBookmarkTag.list
            if (listTag != null) {
                tags.forEach { tag ->
                    if (listTag!!.list.any { t -> t.name == tag.name } && tagNames.contains(tag.name)) {
                        tag.isSelected = true
                    }
                }
            }

            listBookmarkTag
        }
    }
}