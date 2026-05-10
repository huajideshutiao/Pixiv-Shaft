package ceui.lisa.models

data class ErrorBodyBean(
    var is_succeed: Boolean = false,
    var validation_errors: ValidationErrorsBean? = null
) {
    data class ValidationErrorsBean(
        var mail_address: String? = null,
        var pixiv_id: String? = null
    )
}