package ceui.pixiv.ui.common

import android.content.Context
import ceui.pixiv.route.AppRoute

object ImageUrlViewer {

    const val DATA_TYPE_URL_SINGLE = "URL单图"

    fun open(ctx: Context, url: String, saveName: String? = null) {
        if (url.isBlank()) return
        AppRoute.UrlImage(url, saveName).start(ctx)
    }
}
