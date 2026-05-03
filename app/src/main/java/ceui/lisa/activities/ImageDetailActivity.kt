package ceui.lisa.activities

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import ceui.lisa.R
import ceui.lisa.databinding.ActivityImageDetailBinding
import ceui.lisa.download.IllustDownload
import ceui.lisa.fragments.FragmentImageDetail
import ceui.lisa.helper.PageTransformerHelper
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

/**
 * 图片二级详情
 */
class ImageDetailActivity : BaseActivity<ActivityImageDetailBinding?>() {
    var mIllustsBean: IllustsBean? = null
        private set
    private var localIllust: List<String>? = ArrayList()
    private var currentPage: TextView? = null
    private var downloadSingle: TextView? = null
    private var currentSize: TextView? = null
    private var index = 0
    private val viewModel by viewModels<ToggleToolnarViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (this as? ComponentActivity)?.enableEdgeToEdge()
    }

    override fun initLayout(): Int {
        return R.layout.activity_image_detail
    }

    override fun initView() {
        val dataType = intent.getStringExtra("dataType")
        baseBind!!.viewPager.setPageTransformer(true, PageTransformerHelper.getCurrentTransformer())
        val windowInsetsController = WindowInsetsControllerCompat(
            window,
            window.decorView
        )
        val btnAi = null
        val infoItems = listOfNotNull(
            baseBind?.bottomRela,
            btnAi
        )
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        viewModel.isFullscreenMode.observe(this) { isFullScreen ->
            if (isFullScreen) {
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                infoItems.forEach {
                    it.animateFadeOutQuickly()
                }
            } else {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                infoItems.forEach {
                    it.animateFadeInQuickly()
                }
            }
        }
        if ("二级详情" == dataType) {
            currentSize = findViewById(R.id.current_size)
            currentPage = findViewById(R.id.current_page)
            downloadSingle = findViewById(R.id.download_this_one)
            mIllustsBean = intent.getSerializableExtra("illust") as IllustsBean?
            index = intent.getIntExtra("index", 0)
            if (mIllustsBean == null) {
                return
            }

            baseBind!!.viewPager.adapter = object : FragmentPagerAdapter(
                supportFragmentManager
            ) {
                override fun getItem(i: Int): Fragment {
                    return FragmentImageDetail.newInstance(i)
                }

                override fun getCount(): Int {
                    return mIllustsBean!!.page_count
                }
            }
            baseBind!!.viewPager.currentItem = index
            checkDownload(index)
            downloadSingle?.setOnClickListener(View.OnClickListener {
                IllustDownload.downloadIllustCertainPage(
                    mIllustsBean,
                    baseBind!!.viewPager.currentItem,
                    mContext as BaseActivity<*>
                )
                if (Shaft.sSettings.isAutoPostLikeWhenDownload && !mIllustsBean!!.isIs_bookmarked) {
                    PixivOperate.postLikeDefaultStarType(mIllustsBean)
                }
            })
            baseBind!!.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(i: Int, v: Float, i1: Int) {
                }

                override fun onPageSelected(i: Int) {
                    checkDownload(i)
                    currentPage?.setText(
                        String.format(
                            Locale.getDefault(),
                            "第 %d/%d P",
                            i + 1,
                            mIllustsBean!!.page_count
                        )
                    )
                }

                override fun onPageScrollStateChanged(i: Int) {
                }
            })
            if (mIllustsBean!!.page_count == 1) {
                currentPage?.setVisibility(View.INVISIBLE)
            } else {
                currentPage?.setText(
                    String.format(
                        Locale.getDefault(),
                        "第 %d/%d P",
                        index + 1,
                        mIllustsBean!!.page_count
                    )
                )
            }
        } else if (ceui.pixiv.ui.common.ImageUrlViewer.DATA_TYPE_URL_SINGLE == dataType) {
            currentPage = findViewById(R.id.current_page)
            currentPage?.visibility = View.INVISIBLE
            downloadSingle = findViewById(R.id.download_this_one)
            downloadSingle?.visibility = View.INVISIBLE
            val singleUrl = intent.getStringExtra(Params.URL)
            val singleTitle = intent.getStringExtra(Params.TITLE)
            if (singleUrl.isNullOrEmpty()) {
                finish()
                return
            }
            baseBind!!.viewPager.adapter = object : FragmentPagerAdapter(
                supportFragmentManager
            ) {
                override fun getItem(i: Int): Fragment =
                    FragmentImageDetail.newInstance(singleUrl, singleTitle)

                override fun getCount(): Int = 1
            }
        } else if ("下载详情" == dataType) {
            currentPage = findViewById(R.id.current_page)
            downloadSingle = findViewById(R.id.download_this_one)
            localIllust = intent.getSerializableExtra("illust") as List<String>?
            index = intent.getIntExtra("index", 0)

            baseBind!!.viewPager.adapter = object : FragmentPagerAdapter(
                supportFragmentManager
            ) {
                override fun getItem(i: Int): Fragment {
                    return FragmentImageDetail.newInstance(localIllust!![i])
                }

                override fun getCount(): Int {
                    return localIllust!!.size
                }
            }
            currentPage?.setVisibility(View.INVISIBLE)
            baseBind!!.viewPager.currentItem = index
            baseBind!!.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(i: Int, v: Float, i1: Int) {
                }

                override fun onPageSelected(i: Int) {
                    try {
                        downloadSingle?.setText(
                            String.format(
                                "%s%s", getString(R.string.file_path),
                                URLDecoder.decode(localIllust!![i], "utf-8")
                            )
                        )
                    } catch (e: UnsupportedEncodingException) {
                        e.printStackTrace()
                    }
                }

                override fun onPageScrollStateChanged(i: Int) {
                }
            })
            try {
                downloadSingle?.setText(
                    String.format(
                        "%s%s", getString(R.string.file_path),
                        URLDecoder.decode(localIllust!![index], "utf-8")
                    )
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

    override fun initData() {
        postponeEnterTransition()
    }

    override fun onBackPressed() {
        if (index == baseBind!!.viewPager.currentItem) {
            super.onBackPressed()
        } else {
            mActivity.finish()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
        ) {
            val viewPager = baseBind?.viewPager ?: return super.dispatchKeyEvent(event)
            val adapter = viewPager.adapter ?: return super.dispatchKeyEvent(event)
            val currentItem = viewPager.currentItem
            val nextItem =
                if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) currentItem + 1 else currentItem - 1
            if (nextItem in 0 until adapter.count) {
                viewPager.setCurrentItem(nextItem, true)
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun hideStatusBar(): Boolean {
        return true
    }
}
