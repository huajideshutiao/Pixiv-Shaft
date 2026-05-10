package ceui.lisa.models

import java.io.Serializable

data class ProfilePublicityBean(
    var gender: String? = null,
    var region: String? = null,
    var birth_day: String? = null,
    var birth_year: String? = null,
    var job: String? = null,
    var pawoo: Boolean = false
) : Serializable