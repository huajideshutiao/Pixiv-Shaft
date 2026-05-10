package ceui.lisa.models

import android.text.TextUtils

open class ProfileImageUrlsBean : ImageUrlsBean() {
    var px_16x16: String? = null
    var px_50x50: String? = null
    var px_170x170: String? = null

    override val maxImage: String
        get() {
            val url = super.maxImage
            if (!TextUtils.isEmpty(url)) {
                return url
            }
            return when {
                !TextUtils.isEmpty(px_170x170) -> px_170x170!!
                !TextUtils.isEmpty(px_50x50) -> px_50x50!!
                !TextUtils.isEmpty(px_16x16) -> px_16x16!!
                else -> ""
            }
        }
}