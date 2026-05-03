package ceui.lisa.fragments

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ceui.lisa.R
import ceui.lisa.activities.BaseActivity
import ceui.lisa.activities.Shaft
import ceui.lisa.databinding.ActivityImageDetailBinding
import ceui.lisa.download.IllustDownload
import ceui.lisa.helper.PageTransformerHelper
import ceui.lisa.interfaces.FragmentBackHandler
import ceui.lisa.interfaces.VolumeKeyHandler
import ceui.lisa.models.IllustsBean
import ceui.lisa.utils.Common
import ceui.lisa.utils.Params
import ceui.lisa.utils.PixivOperate
import ceui.pixiv.ui.works.ToggleToolnarViewModel
import ceui.pixiv.utils.animateFadeInQuickly
import ceui.pixiv.utils.animateFadeOutQuickly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.Locale

class FragmentImageDetailPager : BaseFragment<ActivityImageDetailBinding>(), FragmentBackHandler,
    VolumeKeyHandler {

    var mIllustsBean: IllustsBean? = null
        private set
    private var localIllust: List<String>? = ArrayList()
    private var currentPage: TextView? = null
    private var downloadSingle: TextView? = null
    private var currentSize: TextView? = null
    var initialIndex = 0
    private val viewModel by viewModels<ToggleToolnarViewModel>(ownerProducer = { requireActivity() })

    override fun initBundle(bundle: Bundle) {
        mIllustsBean = BundleCompat.getSerializable(bundle, "illust", IllustsBean::class.java)
        initialIndex = bundle.getInt("index", 0)
        localIllust = bundle.getStringArrayList("local_illust")
    }

    override fun initLayout() {
        mLayoutID = R.layout.activity_image_detail
    }

    override fun initView() {
        val dataType = arguments?.getString("dataType")
        baseBind.viewPager.setPageTransformer(PageTransformerHelper.getCurrentTransformer() as? ViewPager2.PageTransformer)
        baseBind.viewPager.offscreenPageLimit = 1

        val infoItems = listOfNotNull(baseBind.bottomRela)

        viewModel.isFullscreenMode.observe(viewLifecycleOwner) { isFullScreen ->
            if (isFullScreen) {
                infoItems.forEach { it.animateFadeOutQuickly() }
            } else {
                infoItems.forEach { it.animateFadeInQuickly() }
            }
        }

        if ("二级详情" == dataType) {
            currentSize = baseBind.root.findViewById(R.id.current_size)
            currentPage = baseBind.root.findViewById(R.id.current_page)
            downloadSingle = baseBind.root.findViewById(R.id.download_this_one)

            if (mIllustsBean == null) {
                return
            }

            baseBind.viewPager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = mIllustsBean!!.page_count
                override fun createFragment(position: Int): Fragment =
                    FragmentImageDetail.newInstance(position)
            }
            baseBind.viewPager.setCurrentItem(initialIndex, false)
            checkDownload(initialIndex)

            downloadSingle?.setOnClickListener {
                IllustDownload.downloadIllustCertainPage(
                    mIllustsBean,
                    baseBind.viewPager.currentItem,
                    activity as BaseActivity<*>
                )
                if (Shaft.sSettings.isAutoPostLikeWhenDownload && !mIllustsBean!!.isIs_bookmarked) {
                    PixivOperate.postLikeDefaultStarType(mIllustsBean)
                }
            }

            baseBind.viewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(i: Int) {
                    checkDownload(i)
                    currentPage?.text = String.format(
                        Locale.getDefault(),
                        "第 %d/%d P",
                        i + 1,
                        mIllustsBean!!.page_count
                    )
                }
            })

            if (mIllustsBean!!.page_count == 1) {
                currentPage?.visibility = View.INVISIBLE
            } else {
                currentPage?.text = String.format(
                    Locale.getDefault(),
                    "第 %d/%d P",
                    initialIndex + 1,
                    mIllustsBean!!.page_count
                )
            }
        } else if (ceui.pixiv.ui.common.ImageUrlViewer.DATA_TYPE_URL_SINGLE == dataType) {
            currentPage = baseBind.root.findViewById(R.id.current_page)
            currentPage?.visibility = View.INVISIBLE
            downloadSingle = baseBind.root.findViewById(R.id.download_this_one)
            downloadSingle?.visibility = View.INVISIBLE
            val singleUrl = arguments?.getString(Params.URL)
            val singleTitle = arguments?.getString(Params.TITLE)
            if (singleUrl.isNullOrEmpty()) {
                finish()
                return
            }
            baseBind.viewPager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = 1
                override fun createFragment(position: Int): Fragment =
                    FragmentImageDetail.newInstance(singleUrl, singleTitle)
            }
        } else if ("下载详情" == dataType) {
            currentPage = baseBind.root.findViewById(R.id.current_page)
            downloadSingle = baseBind.root.findViewById(R.id.download_this_one)

            if (localIllust == null) {
                finish()
                return
            }

            baseBind.viewPager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = localIllust!!.size
                override fun createFragment(position: Int): Fragment =
                    FragmentImageDetail.newInstance(localIllust!![position])
            }
            currentPage?.visibility = View.INVISIBLE
            baseBind.viewPager.setCurrentItem(initialIndex, false)
            baseBind.viewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(i: Int) {
                    try {
                        downloadSingle?.text = String.format(
                            "%s%s",
                            getString(R.string.file_path),
                            URLDecoder.decode(localIllust!![i], "utf-8")
                        )
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                }
            })
            try {
                downloadSingle?.text = String.format(
                    "%s%s",
                    getString(R.string.file_path),
                    URLDecoder.decode(localIllust!![initialIndex], "utf-8")
                )
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
        }
    }

    private fun checkDownload(i: Int) {
        val illust = mIllustsBean ?: return
        lifecycleScope.launch {
            val downloaded = withContext(Dispatchers.IO) {
                Common.isIllustDownloaded(illust, i)
            }
            downloadSingle?.visibility = if (downloaded) View.INVISIBLE else View.VISIBLE
        }
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

    override fun onBackPressed(): Boolean {
        if (initialIndex == baseBind.viewPager.currentItem) {
            val currentFragment =
                childFragmentManager.findFragmentByTag("f${baseBind.viewPager.currentItem}")

            val detailFrag = currentFragment as? FragmentImageDetail ?: return false

            // 1. 调用原子级物理锁定。
            detailFrag.prepareForExit()

            // 2. 核心修复：直接通过 supportFinishAfterTransition 触发捕捉。
            activity?.supportFinishAfterTransition()
            return true
        } else {
            return false
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(illust: IllustsBean?, index: Int): FragmentImageDetailPager {
            val args = Bundle()
            args.putSerializable("illust", illust)
            args.putInt("index", index)
            args.putString("dataType", "二级详情")
            val fragment = FragmentImageDetailPager()
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun newInstance(localIllust: List<String>?, index: Int): FragmentImageDetailPager {
            val args = Bundle()
            args.putStringArrayList("local_illust", localIllust as? ArrayList<String>)
            args.putInt("index", index)
            args.putString("dataType", "下载详情")
            val fragment = FragmentImageDetailPager()
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun newInstance(url: String?, title: String?): FragmentImageDetailPager {
            val args = Bundle()
            args.putString(Params.URL, url)
            args.putString(Params.TITLE, title)
            args.putString("dataType", ceui.pixiv.ui.common.ImageUrlViewer.DATA_TYPE_URL_SINGLE)
            val fragment = FragmentImageDetailPager()
            fragment.arguments = args
            return fragment
        }
    }
}
