package ceui.pixiv.utils

import android.view.LayoutInflater
import android.view.View
import com.google.android.flexbox.FlexboxLayout

fun <T> FlexboxLayout.populate(
    items: List<T>,
    layoutId: Int,
    bind: (View, T) -> Unit
) {
    this.removeAllViews()
    val inflater = LayoutInflater.from(context)
    for (item in items) {
        val view = inflater.inflate(layoutId, this, false)
        bind(view, item)
        this.addView(view)
    }
}
