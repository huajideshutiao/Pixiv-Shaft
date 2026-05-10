package ceui.lisa.models

import java.io.Serializable

data class FramesBean(
    var file: String? = null,
    var delay: Int = 0
) : Serializable