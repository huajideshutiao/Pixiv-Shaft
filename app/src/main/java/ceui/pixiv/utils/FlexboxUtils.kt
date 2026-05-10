package ceui.pixiv.utils

import android.view.LayoutInflater
import android.view.View
import com.google.android.flexbox.FlexboxLayout

fun interface FlexboxBinder<T> {
    fun bind(view: View, item: T, index: Int)
}

object FlexboxUtils {
    @JvmStatic
    fun <T> populate(
        flexbox: FlexboxLayout,
        items: List<T>?,
        layoutId: Int,
        binder: FlexboxBinder<T>
    ) {
        flexbox.removeAllViews()
        if (items == null) return

        val inflater = LayoutInflater.from(flexbox.context)
        items.forEachIndexed { index, item ->
            val view = inflater.inflate(layoutId, flexbox, false)
            binder.bind(view, item, index)
            flexbox.addView(view)
        }
    }
}
