package ceui.lisa.models

import java.io.Serializable

data class HitoResponse(
    var id: Int = 0,
    var hitokoto: String? = null,
    var type: String? = null,
    var from: String? = null,
    var creator: String? = null,
    var created_at: String? = null
) : Serializable