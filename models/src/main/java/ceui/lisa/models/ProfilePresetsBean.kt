package ceui.lisa.models

import java.io.Serializable

data class ProfilePresetsBean(
    var default_profile_image_urls: ImageUrlsBean? = null,
    var addresses: List<AddressesBean>? = null,
    var countries: List<CountriesBean>? = null,
    var jobs: List<JobsBean>? = null
) : Serializable