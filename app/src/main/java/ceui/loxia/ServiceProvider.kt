package ceui.loxia

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import android.content.SharedPreferences
import ceui.pixiv.db.EntityWrapper
import ceui.pixiv.utils.NetworkStateManager


interface ServicesProvider {
    val prefStore: SharedPreferences
    val networkStateManager: NetworkStateManager
    val entityWrapper: EntityWrapper
}

fun Fragment.requireEntityWrapper(): EntityWrapper {
    return requireActivity().requireEntityWrapper()
}

fun FragmentActivity.requireEntityWrapper(): EntityWrapper {
    return (application as ServicesProvider).entityWrapper
}