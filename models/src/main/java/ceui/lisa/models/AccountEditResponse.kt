package ceui.lisa.models

data class AccountEditResponse(
    var error: Boolean = false,
    var message: String? = null,
    var body: BodyBean? = null
) {
    data class BodyBean(
        var is_succeed: Boolean = false,
        var validation_errors: ValidationErrorsBean? = null
    ) {
        data class ValidationErrorsBean(
            var mail_address: String? = null
        )
    }
}