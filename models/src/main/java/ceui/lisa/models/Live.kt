package ceui.lisa.models

import java.io.Serializable

data class Live(
    var id: String? = null,
    var created_at: String? = null,
    var owner: UserHolder? = null,
    var name: String? = null,
    var is_single: Boolean = false,
    var is_adult: Boolean = false,
    var is_r18: Boolean = false,
    var is_r15: Boolean = false,
    var publicity: String? = null,
    var is_closed: Boolean = false,
    var mode: String? = null,
    var server: String? = null,
    var channel_id: String? = null,
    var is_enabled_mic_input: Boolean = false,
    var thumbnail_image_url: String? = null,
    var member_count: Int = 0,
    var total_audience_count: Int = 0,
    var performer_count: Int = 0,
    var is_muted: Boolean = false,
    var performers: List<Any>? = null
) : Serializable