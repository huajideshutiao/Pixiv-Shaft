package ceui.lisa.models

data class ErrorResponse2(
    var has_error: Boolean = false,
    var errors: ErrorsBean? = null,
    var error: String? = null,
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
}