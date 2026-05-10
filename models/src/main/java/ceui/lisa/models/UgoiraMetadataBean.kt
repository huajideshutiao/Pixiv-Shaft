package ceui.lisa.models

import java.io.Serializable

data class UgoiraMetadataBean(
    var zip_urls: ImageUrlsBean? = null,
    var frames: List<FramesBean>? = null
) : Serializable