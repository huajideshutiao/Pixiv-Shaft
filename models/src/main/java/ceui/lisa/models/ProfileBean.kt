package ceui.lisa.models

import android.text.TextUtils
import java.io.Serializable
import java.util.Calendar

data class ProfileBean(
    var webpage: String? = null,
    var gender: String? = null,
    var birth: String? = null,
    var birth_day: String? = null,
    var birth_year: Int = 0,
    var region: String? = null,
    var address_id: Int = 0,
    var country_code: String? = null,
    var job: String? = null,
    var job_id: Int = 0,
    var total_follow_users: Int = 0,
    var total_mypixiv_users: Int = 0,
    var total_illusts: Int = 0,
    var total_manga: Int = 0,
    var total_novels: Int = 0,
    var total_illust_bookmarks_public: Int = 0,
    var total_illust_series: Int = 0,
    var total_novel_series: Int = 0,
    var background_image_url: String? = null,
    var twitter_account: String? = null,
    var twitter_url: String? = null,
    var pawoo_url: String? = null,
    var comment: String? = null,
    var is_premium: Boolean = false,
    var is_using_custom_profile_image: Boolean = false
) : Serializable {

    fun getContent(): String {
        var result = ""
        when (gender) {
            "male" -> result += "男性"
            "female" -> result += "女性"
            else -> result += "未知性别"
        }

        if (birth_year > 0) {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val age = year - birth_year
            result += if (!TextUtils.isEmpty(result)) {
                "/${age}岁"
            } else {
                "${age}岁"
            }
        }

        if (!TextUtils.isEmpty(birth_day)) {
            result += if (!TextUtils.isEmpty(result)) {
                "/${birth_day}生日"
            } else {
                "${birth_day}生日"
            }
        }

        if (!TextUtils.isEmpty(job)) {
            result += if (!TextUtils.isEmpty(result)) {
                "/$job"
            } else {
                "$job"
            }
        }

        return result
    }
}