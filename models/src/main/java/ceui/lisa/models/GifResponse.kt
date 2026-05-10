package ceui.lisa.models

import java.io.Serializable

data class GifResponse(
    var ugoira_metadata: UgoiraMetadataBean? = null
) : Serializable {

    fun getDelay(): Int {
        val frames = ugoira_metadata?.frames
        return if (frames != null && frames.isNotEmpty()) {
            frames[0].delay
        } else {
            DEFAULT_DELAY
        }
    }

    companion object {
        private const val DEFAULT_DELAY = 60
    }
}