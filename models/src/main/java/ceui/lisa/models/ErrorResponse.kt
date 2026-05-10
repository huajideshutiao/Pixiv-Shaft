package ceui.lisa.models

data class ErrorResponse(
    var has_error: Boolean = false,
    var errors: ErrorsBean? = null,
    var error: ErrorBean? = null,
    var body: ErrorBodyBean? = null
) {
    data class ErrorsBean(
        var system: SystemBean? = null
    ) {
        data class SystemBean(
            var message: String? = null,
            var code: Int = 0
        )
    }

    data class ErrorBean(
        var user_message: String? = null,
        var message: String? = null,
        var reason: String? = null,
        var user_message_details: UserMessageDetails? = null
    ) {
        data class UserMessageDetails(
            var profile_image: String? = null
        )
    }
}