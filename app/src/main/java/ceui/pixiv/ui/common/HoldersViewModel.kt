package ceui.pixiv.ui.common


import android.util.Log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.loxia.RefreshHint
import ceui.loxia.RefreshState
import kotlinx.coroutines.launch
open class HoldersViewModel : ViewModel(), HoldersContainer, RefreshOwner, LoadMoreOwner {

    protected val _itemHolders = MutableLiveData<List<ListItemHolder>>()
    protected val _refreshState = MutableLiveData<RefreshState>()

    open suspend fun refreshImpl(hint: RefreshHint) {

    }

    open suspend fun loadMoreImpl() {

    }

    final override fun refresh(hint: RefreshHint) {
        viewModelScope.launch {
            try {
                _refreshState.value = RefreshState.LOADING(refreshHint = hint)
                refreshImpl(hint)
            } catch (ex: Exception) {
                _refreshState.value = RefreshState.ERROR(ex)
                Log.e(TAG, "refresh error", ex)
            }
        }
    }

    final override fun loadMore() {
        viewModelScope.launch {
            try {
                _refreshState.value = RefreshState.LOADING()
                loadMoreImpl()
            } catch (ex: Exception) {
                _refreshState.value = RefreshState.ERROR(ex)
                Log.e(TAG, "loadMore error", ex)
            }
        }
    }

    override fun prepareIdMap(fragmentUniqueId: String) {
    }

    override val refreshState: LiveData<RefreshState>
        get() = _refreshState

    override val holders: LiveData<List<ListItemHolder>>
        get() = _itemHolders


    companion object {
        private const val TAG = "HoldersViewModel"
    }
}