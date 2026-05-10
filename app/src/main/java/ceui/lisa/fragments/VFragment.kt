package ceui.lisa.fragments

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ceui.lisa.R
import ceui.lisa.activities.Shaft
import ceui.lisa.core.ArtworksMap
import ceui.lisa.core.Container
import ceui.lisa.core.Mapper
import ceui.lisa.databinding.ActivityViewPagerBinding
import ceui.lisa.helper.DeduplicateArrayList
import ceui.lisa.http.NullCtrl
import ceui.lisa.http.Retro
import ceui.lisa.interfaces.VolumeKeyHandler
import ceui.lisa.model.ListIllust
import ceui.lisa.utils.Common
import ceui.lisa.utils.Params
import ceui.lisa.utils.PixivOperate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class VFragment : BaseFragment<ActivityViewPagerBinding>(), VolumeKeyHandler {

    private var pageUUID: String? = ""
    private var index = 0
    private var seed: String? = ""

    override fun initBundle(bundle: Bundle) {
        pageUUID = bundle.getString(Params.PAGE_UUID)
        index = bundle.getInt(Params.POSITION)
        seed = bundle.getString(Params.SEED)
    }

    override fun initLayout() {
        mLayoutID = R.layout.activity_view_pager
    }

    override fun initView() {
        if (!seed.isNullOrEmpty()) {
            setupFromSeed()
        } else {
            setupFromPageData()
        }
    }

    private fun setupFromSeed() {
        val ids = ArtworksMap.store[seed]
        if (ids.isNullOrEmpty()) {
            finish()
            return
        }

        baseBind.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = ids.size

            override fun createFragment(position: Int): Fragment {
                return FragmentSingleIllust.newInstance(ids[position])
            }
        }

        baseBind.viewPager.offscreenPageLimit = 1

        val pos = ids.indexOf(index.toLong())
        if (pos >= 0) {
            baseBind.viewPager.setCurrentItem(pos, false)
        }
    }

    private fun setupFromPageData() {
        val pageData = Container.get().getPage(pageUUID)
        if (pageData != null) {
            baseBind.viewPager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = pageData.list.size

                override fun createFragment(position: Int): Fragment {
                    val illustsBean = pageData.list[position]
                    return when {
                        illustsBean.id == 0 || !illustsBean.isVisible -> {
                            FragmentImageDetail.newInstance(illustsBean.image_urls?.maxImage!!)
                        }

                        illustsBean.isGif -> {
                            FragmentSingleUgora.newInstance(illustsBean)
                        }

                        else -> {
                            FragmentSingleIllust.newInstance(illustsBean)
                        }
                    }
                }
            }
            baseBind.viewPager.offscreenPageLimit = 1

            val callback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    Common.showLog("Container onPageSelected $position")
                    if (pageData.list.isNullOrEmpty()) {
                        return
                    }

                    if (position >= pageData.list.size) {
                        return
                    }

                    if (Shaft.sSettings.isSaveViewHistory) {
                        PixivOperate.insertIllustViewHistory(pageData.list[position])
                    }

                    if (position == pageData.list.size - 1 || position == pageData.list.size - 2) {
                        val nextUrl = pageData.nextUrl
                        if (!TextUtils.isEmpty(nextUrl)) {
                            if (!Container.get().isNetworking) {
                                Common.showLog("Container 去请求下一页 $nextUrl")
                                Retro.getAppApi().getNextIllust(nextUrl)
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(object : NullCtrl<ListIllust>() {
                                        override fun success(listIllust: ListIllust) {
                                            var mutableListIllust = listIllust
                                            val mapper = Mapper<ListIllust>()
                                            mutableListIllust = mapper.apply(mutableListIllust)
                                            Common.showLog("Container 下一页请求成功 ")

                                            val intent = Intent(Params.FRAGMENT_ADD_DATA)
                                            intent.putExtra(Params.PAGE_UUID, pageUUID)
                                            intent.putExtra(Params.CONTENT, mutableListIllust)
                                            LocalBroadcastManager.getInstance(Shaft.getContext())
                                                .sendBroadcast(intent)

                                            DeduplicateArrayList.addAllWithNoRepeat(
                                                pageData.list,
                                                mutableListIllust.list
                                            )
                                            pageData.nextUrl = mutableListIllust.nextUrl
                                            baseBind.viewPager.adapter?.notifyDataSetChanged()
                                        }

                                        override fun must() {
                                            super.must()
                                            Container.get().isNetworking = false
                                        }

                                        override fun subscribe(d: io.reactivex.disposables.Disposable) {
                                            super.subscribe(d)
                                            Container.get().isNetworking = true
                                        }
                                    })
                            } else {
                                Common.showLog("Container 不去请求下一页 00")
                            }
                        } else {
                            Common.showLog("Container 不去请求下一页 11")
                        }
                    }
                }
            }
            baseBind.viewPager.registerOnPageChangeCallback(callback)

            if (index < pageData.list.size) {
                baseBind.viewPager.setCurrentItem(index, false)
            }

            if (index == 0) {
                baseBind.viewPager.post { callback.onPageSelected(baseBind.viewPager.currentItem) }
            }
        } else {
            finish()
        }
    }

    override fun onPause() {
        if (seed.isNullOrEmpty()) {
            val intent = Intent(Params.FRAGMENT_SCROLL_TO_POSITION)
            intent.putExtra(Params.INDEX, baseBind.viewPager.currentItem)
            intent.putExtra(Params.PAGE_UUID, pageUUID)
            LocalBroadcastManager.getInstance(Shaft.getContext()).sendBroadcast(intent)
        }
        super.onPause()
    }

    override fun onDestroy() {
        PixivOperate.clearBack()
        super.onDestroy()
    }

    override fun handleVolumeKey(keyCode: Int): Boolean {
        val viewPager = baseBind.viewPager
        val adapter = viewPager.adapter ?: return false
        val currentItem = viewPager.currentItem
        val count = adapter.itemCount
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (currentItem < count - 1) {
                viewPager.setCurrentItem(currentItem + 1, true)
            } else {
                Common.showToast("已到达最后一页")
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (currentItem > 0) {
                viewPager.setCurrentItem(currentItem - 1, true)
            } else {
                Common.showToast("已到达第一页")
            }
            return true
        }
        return false
    }

    companion object {
        @JvmStatic
        fun newInstance(pageUUID: String?, index: Int, seed: String?): VFragment {
            val fragment = VFragment()
            val bundle = Bundle()
            bundle.putString(Params.PAGE_UUID, pageUUID)
            bundle.putInt(Params.POSITION, index)
            bundle.putString(Params.SEED, seed)
            fragment.arguments = bundle
            return fragment
        }
    }
}
