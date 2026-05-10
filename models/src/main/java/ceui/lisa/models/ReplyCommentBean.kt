package ceui.lisa.models

open class ReplyCommentBean : CommentBean() {
    var parent_comment: CommentBean? = null
}