package ceui.lisa.activities

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
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
    var initialIndex = 0
    private val viewModel by viewModels<ToggleToolnarViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()

        // 彻底消除退出闪烁：确保窗口背景始终为黑
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))

        // 核心配置：使用 ChangeBounds + ChangeImageTransform
        // 两者同步运行，确保在 View 容器位移的同时，图片像素内容按比例缩放，绝对不产生扭曲变形。
        val naturalInterpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
        window.sharedElementsUseOverlay = true
        window.sharedElementEnterTransition = android.transition.TransitionSet().apply {
            addTransition(android.transition.ChangeBounds())
            addTransition(android.transition.ChangeImageTransform())
            interpolator = naturalInterpolator
            duration = 300
        }
        window.sharedElementReturnTransition = android.transition.TransitionSet().apply {
            addTransition(android.transition.ChangeBounds())
            addTransition(android.transition.ChangeImageTransform())
            interpolator = naturalInterpolator
            duration = 300
        }

        (this as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
    }

    override fun initLayout(): Int {
        return R.layout.activity_image_detail
    }

    override fun initView() {
        val dataType = intent.getStringExtra("dataType")
        baseBind!!.viewPager.setPageTransformer(PageTransformerHelper.getCurrentTransformer())
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
            initialIndex = intent.getIntExtra("index", 0)
            if (mIllustsBean == null) {
                return
            }

            baseBind!!.viewPager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int {
                    return mIllustsBean!!.page_count
                }

                override fun createFragment(position: Int): Fragment {
                    return FragmentImageDetail.newInstance(position)
                }
            }
            baseBind!!.viewPager.setCurrentItem(initialIndex, false)
            checkDownload(initialIndex)
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
            baseBind!!.viewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
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
            })
            if (mIllustsBean!!.page_count == 1) {
                currentPage?.setVisibility(View.INVISIBLE)
            } else {
                currentPage?.setText(
                    String.format(
                        Locale.getDefault(),
                        "第 %d/%d P",
                        initialIndex + 1,
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
            baseBind!!.viewPager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = 1

                override fun createFragment(position: Int): Fragment =
                    FragmentImageDetail.newInstance(singleUrl, singleTitle)
            }
        } else if ("下载详情" == dataType) {
            currentPage = findViewById(R.id.current_page)
            downloadSingle = findViewById(R.id.download_this_one)
            localIllust = intent.getSerializableExtra("illust") as List<String>?
            initialIndex = intent.getIntExtra("index", 0)

            baseBind!!.viewPager.adapter = object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int {
                    return localIllust!!.size
                }

                override fun createFragment(position: Int): Fragment {
                    return FragmentImageDetail.newInstance(localIllust!![position])
                }
            }
            currentPage?.setVisibility(View.INVISIBLE)
            baseBind!!.viewPager.setCurrentItem(initialIndex, false)
            baseBind!!.viewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
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
            })
            try {
                downloadSingle?.setText(
                    String.format(
                        "%s%s", getString(R.string.file_path),
                        URLDecoder.decode(localIllust!![initialIndex], "utf-8")
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
    }

    override fun onBackPressed() {
        if (initialIndex == baseBind!!.viewPager.currentItem) {
            val currentFragment =
                supportFragmentManager.findFragmentByTag("f${baseBind!!.viewPager.currentItem}")

            val detailFrag = currentFragment as? FragmentImageDetail ?: run {
                super.onBackPressed()
                return
            }

            // 1. 调用原子级物理锁定。
            detailFrag.prepareForExit()

            // 2. 核心修复：直接通过 supportFinishAfterTransition 触发捕捉。
            // 此时物理状态已通过 layout() 同步锁定，系统读取到的就是最终态，不会产生阶梯跳变。
            supportFinishAfterTransition()
        } else {
            super.onBackPressed()
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
