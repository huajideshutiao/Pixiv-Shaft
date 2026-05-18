package ceui.lisa.helper

import android.text.TextUtils
import ceui.lisa.activities.Shaft
import ceui.lisa.database.AppDatabase
import ceui.lisa.models.IllustsBean
import ceui.lisa.models.NovelBean
import ceui.lisa.models.TagsBean
import ceui.lisa.utils.Common
import java.util.regex.Pattern

object IllustNovelFilter {

    @JvmStatic
    fun judge(illust: IllustsBean): Boolean {
        return judgeID(illust) || judgeTag(illust) || judgeUserID(illust)
    }

    @JvmStatic
    fun judge(illust: NovelBean): Boolean {
        return judgeID(illust) || judgeTag(illust) || judgeUserID(illust)
    }

    @JvmStatic
    fun judgeID(illust: IllustsBean): Boolean {
        val temp = AppDatabase.getAppDatabase(Shaft.getContext()).searchDao().mutedIllusts
        if (Common.isEmpty(temp)) return false
        return temp.any { it.id == illust.id }
    }

    @JvmStatic
    fun judgeID(illust: NovelBean): Boolean {
        val temp = AppDatabase.getAppDatabase(Shaft.getContext()).searchDao().mutedIllusts
        if (Common.isEmpty(temp)) return false
        return temp.any { it.id == illust.id }
    }

    @JvmStatic
    fun judgeUserID(illust: IllustsBean): Boolean {
        val temp = AppDatabase.getAppDatabase(Shaft.getContext())
            .searchDao()
            .getUserMuteEntityByID(illust.user?.userId ?: 0)
        return temp != null
    }

    @JvmStatic
    fun judgeUserID(illust: NovelBean): Boolean {
        val temp = AppDatabase.getAppDatabase(Shaft.getContext())
            .searchDao()
            .getUserMuteEntityByID(illust.user?.userId ?: 0)
        return temp != null
    }

    @JvmStatic
    fun judgeTag(illustsBean: IllustsBean): Boolean {
        val tagString = illustsBean.tagString
        if (TextUtils.isEmpty(tagString)) {
            return false
        }

        val temp = getMutedTags()
        for (bean in temp) {
            if (bean.effective) {
                val name = "*#${bean.name},"
                if (bean.filter_mode == 0 && tagString!!.contains(name)) {
                    illustsBean.isShield = true
                    return true
                } else if (bean.filter_mode == 1 && Pattern.compile(bean.name).matcher(tagString!!).find()) {
                    illustsBean.isShield = true
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun judgeTag(illustsBean: NovelBean): Boolean {
        val tagString = illustsBean.tagString
        if (TextUtils.isEmpty(tagString)) {
            return false
        }

        val temp = getMutedTags()
        for (bean in temp) {
            if (bean.effective) {
                val name = "*#${bean.name},"
                if (bean.filter_mode == 0 && tagString!!.contains(name)) {
                    return true
                } else if (bean.filter_mode == 1 && Pattern.compile(bean.name).matcher(tagString!!).find()) {
                    return true
                }
            }
        }
        return false
    }

    @JvmStatic
    fun judgeR18Filter(illustsBean: IllustsBean): Boolean {
        if (!Shaft.sSettings.isR18FilterTempEnable) {
            return false
        }
        val tagString = illustsBean.tagString
        val isHit = tagString!!.contains("*#R-18,") || tagString.contains("*#R-18G,")
        illustsBean.isShield = isHit
        return isHit
    }

    @JvmStatic
    fun judgeR18Filter(illustsBean: NovelBean): Boolean {
        if (!Shaft.sSettings.isR18FilterTempEnable) {
            return false
        }
        val tagString = illustsBean.tagString
        return tagString!!.contains("*#R-18,") || tagString.contains("*#R-18G,")
    }

    @JvmStatic
    fun getMutedTags(): List<TagsBean> {
        val result = mutableListOf<TagsBean>()
        val muteEntities = AppDatabase.getAppDatabase(Shaft.getContext()).searchDao().allMutedTags
        if (muteEntities.isNullOrEmpty()) {
            return result
        }
        for (muteEntity in muteEntities) {
            val bean = Shaft.sGson.fromJson(muteEntity.tagJson, TagsBean::class.java)
            result.add(bean)
        }
        return result
    }

    @JvmStatic
    fun getMutedWorks(): List<ceui.lisa.database.MuteEntity> {
        return AppDatabase.getAppDatabase(Shaft.getContext()).searchDao().mutedWorks
    }
}
