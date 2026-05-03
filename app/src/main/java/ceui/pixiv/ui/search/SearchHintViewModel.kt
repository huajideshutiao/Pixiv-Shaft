package ceui.pixiv.ui.search

import android.util.Log

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ceui.lisa.model.ListTrendingtag
import ceui.loxia.Client
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
class SearchHintViewModel : ViewModel() {

    private val _hints = MutableLiveData<List<ListTrendingtag.TrendTagsBean>>(emptyList())
    val hints: LiveData<List<ListTrendingtag.TrendTagsBean>> = _hints

    private val _currentKeyword = MutableLiveData("")
    val currentKeyword: LiveData<String> = _currentKeyword

    private val _hintsVisible = MutableLiveData(false)
    val hintsVisible: LiveData<Boolean> = _hintsVisible

    private var debounceJob: Job? = null

    fun onTextChanged(lastWord: String) {
        debounceJob?.cancel()
        if (lastWord.isBlank()) {
            clearHints()
            return
        }
        debounceJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            try {
                val result = Client.appApi.searchAutocomplete(lastWord)
                val list = result.list.orEmpty()
                _hints.value = list
                _currentKeyword.value = lastWord
                _hintsVisible.value = list.isNotEmpty()
            } catch (e: Exception) {
                Log.e(TAG, "searchAutocomplete failed for word=$lastWord", e)
                _hintsVisible.value = false
            }
        }
    }

    fun hideHints() {
        _hintsVisible.value = false
    }

    fun clearHints() {
        debounceJob?.cancel()
        _hints.value = emptyList()
        _currentKeyword.value = ""
        _hintsVisible.value = false
    }

    fun showHintsIfAvailable() {
        if (!_hints.value.isNullOrEmpty()) {
            _hintsVisible.value = true
        }
    }

    companion object {
        private const val TAG = "SearchHintViewModel"
        private const val DEBOUNCE_MS = 400L
    }
}
