package ceui.lisa.models

import java.io.Serializable

data class CommentStamp(
    var stamp_id: Long? = null,
    var stamp_url: String? = null
) : Serializable