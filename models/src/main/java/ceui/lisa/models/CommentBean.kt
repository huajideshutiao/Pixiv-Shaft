package ceui.lisa.models

open class CommentBean : UserHolder() {
    var comment: String? = null
    var date: String? = null
    var stamp: CommentStamp? = null
    var id: Int = 0
    var commentWithConvertedEmoji: String? = null
        get() = field ?: comment
}