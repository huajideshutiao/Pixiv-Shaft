package ceui.lisa.core

import ceui.lisa.activities.Shaft
import ceui.lisa.helper.IllustNovelFilter
import ceui.lisa.interfaces.ListShow
import ceui.lisa.models.IllustsBean
import ceui.lisa.models.NovelBean
import ceui.loxia.ObjectPool
import java.util.function.Function

open class Mapper<T : ListShow<*>> : Function<T, T> {

    override fun apply(t: T): T {
        val dash = mutableListOf<Any>()
        val shouldHidAiIllusts = Shaft.sSettings.isDeleteAIIllust
        for (o in t.list) {
            if (o is IllustsBean) {
                if (!o.visible) {
                    dash.add(o)
                    continue
                }
                val isTagBanned = IllustNovelFilter.judgeTag(o)
                val isIdBanned = IllustNovelFilter.judgeID(o)
                val isUserBanned = IllustNovelFilter.judgeUserID(o)
                val isR18FilterBanned = IllustNovelFilter.judgeR18Filter(o)
                val isCreatedByAI = o.isCreatedByAI
                if (isTagBanned || isIdBanned || isUserBanned || isR18FilterBanned) {
                    dash.add(o)
                }
                if (shouldHidAiIllusts && isCreatedByAI) {
                    dash.add(o)
                }
                ObjectPool.updateIllust(o)
            }
            if (o is NovelBean) {
                val isTagBanned = IllustNovelFilter.judgeTag(o)
                val isIdBanned = IllustNovelFilter.judgeID(o)
                val isUserBanned = IllustNovelFilter.judgeUserID(o)
                val isR18FilterBanned = IllustNovelFilter.judgeR18Filter(o)
                if (isTagBanned || isIdBanned || isUserBanned || isR18FilterBanned) {
                    dash.add(o)
                }
            }
        }

        if (t.list != null && dash.isNotEmpty()) {
            t.list.removeAll(dash)
        }
        return t
    }
}
