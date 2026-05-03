package ceui.lisa.utils

import ceui.lisa.models.IllustsBean
import ceui.lisa.models.ImageUrlsBean
import ceui.lisa.models.MetaPagesBean
import ceui.lisa.models.MetaSinglePageBean
import ceui.lisa.models.ProfileImageUrlsBean
import ceui.lisa.models.SeriesBean
import ceui.lisa.models.TagsBean
import ceui.lisa.models.UserBean
import ceui.loxia.Illust
import ceui.loxia.ImageUrls
import ceui.loxia.MetaPage
import ceui.loxia.MetaSinglePage
import ceui.loxia.Series
import ceui.loxia.Tag
import ceui.loxia.User

fun Illust.toIllustsBean(): IllustsBean {
    return IllustsBean().apply {
        id = this@toIllustsBean.id.toInt()
        title = this@toIllustsBean.title
        type = this@toIllustsBean.type
        image_urls = this@toIllustsBean.image_urls?.toImageUrlsBean()
        caption = this@toIllustsBean.caption
        restrict = this@toIllustsBean.restrict ?: 0
        user = this@toIllustsBean.user?.toUserBean()
        create_date = this@toIllustsBean.create_date
        page_count = this@toIllustsBean.page_count
        width = this@toIllustsBean.width
        height = this@toIllustsBean.height
        sanity_level = this@toIllustsBean.sanity_level ?: 0
        x_restrict = this@toIllustsBean.x_restrict ?: 0
        series = this@toIllustsBean.series?.toSeriesBean()
        meta_single_page = this@toIllustsBean.meta_single_page?.toMetaSinglePageBean()
        total_view = this@toIllustsBean.total_view ?: 0
        total_bookmarks = this@toIllustsBean.total_bookmarks ?: 0
        illust_ai_type = this@toIllustsBean.illust_ai_type
        is_bookmarked = this@toIllustsBean.is_bookmarked ?: false
        visible = this@toIllustsBean.visible ?: true
        is_muted = this@toIllustsBean.is_muted ?: false
        tags = this@toIllustsBean.tags?.map { it.toTagsBean() }
        tools = this@toIllustsBean.tools
        meta_pages = this@toIllustsBean.meta_pages?.map { it.toMetaPagesBean() }
    }
}

fun ImageUrls.toImageUrlsBean(): ImageUrlsBean {
    return ImageUrlsBean().apply {
        square_medium = this@toImageUrlsBean.square_medium
        medium = this@toImageUrlsBean.medium
        large = this@toImageUrlsBean.large
        original = this@toImageUrlsBean.original
    }
}

fun User.toUserBean(): UserBean {
    return UserBean().apply {
        id = this@toUserBean.id.toInt()
        name = this@toUserBean.name
        account = this@toUserBean.account
        is_followed = this@toUserBean.is_followed ?: false
        profile_image_urls = this@toUserBean.profile_image_urls?.toProfileImageUrlsBean()
        is_premium = this@toUserBean.is_premium ?: false
        comment = this@toUserBean.comment
        x_restrict = this@toUserBean.x_restrict ?: 0
        is_mail_authorized = this@toUserBean.is_mail_authorized ?: false
        require_policy_agreement = this@toUserBean.require_policy_agreement ?: false
    }
}

fun ImageUrls.toProfileImageUrlsBean(): ProfileImageUrlsBean {
    return ProfileImageUrlsBean().apply {
        medium = this@toProfileImageUrlsBean.medium
        px_16x16 = this@toProfileImageUrlsBean.px_16x16
        px_50x50 = this@toProfileImageUrlsBean.px_50x50
        px_170x170 = this@toProfileImageUrlsBean.px_170x170
    }
}

fun Tag.toTagsBean(): TagsBean {
    return TagsBean().apply {
        name = this@toTagsBean.name
        translated_name = this@toTagsBean.translated_name
    }
}

fun MetaPage.toMetaPagesBean(): MetaPagesBean {
    return MetaPagesBean().apply {
        image_urls = this@toMetaPagesBean.image_urls?.toImageUrlsBean()
    }
}

fun MetaSinglePage.toMetaSinglePageBean(): MetaSinglePageBean {
    return MetaSinglePageBean().apply {
        original_image_url = this@toMetaSinglePageBean.original_image_url
    }
}

fun Series.toSeriesBean(): SeriesBean {
    return SeriesBean().apply {
        id = this@toSeriesBean.id.toInt()
        title = this@toSeriesBean.title
    }
}
