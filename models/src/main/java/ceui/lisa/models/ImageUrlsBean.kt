package ceui.lisa.models

import android.text.TextUtils
import java.io.Serializable

open class ImageUrlsBean : Serializable {
    var square_medium: String? = null
    var medium: String? = null
    var large: String? = null
    open var original: String? = null

    open val maxImage: String
        get() {
            return when {
                !TextUtils.isEmpty(original) -> original!!
                !TextUtils.isEmpty(large) -> large!!
                !TextUtils.isEmpty(medium) -> medium!!
                !TextUtils.isEmpty(square_medium) -> square_medium!!
                else -> ""
            }
        }

    override fun toString(): String {
        return "ImageUrlsBean{" +
            "square_medium='$square_medium'" +
            ", medium='$medium'" +
            ", large='$large'" +
            ", original='$original'" +
            '}'
    }
}