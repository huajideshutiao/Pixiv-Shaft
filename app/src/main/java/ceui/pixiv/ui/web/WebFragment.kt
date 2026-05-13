package ceui.pixiv.ui.web

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.ContextMenu
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import ceui.lisa.R
import ceui.lisa.activities.OutWakeActivity
import ceui.lisa.activities.Shaft
import ceui.lisa.databinding.FragmentWebBinding
import ceui.lisa.utils.ClipBoardUtils
import ceui.lisa.utils.Params
import ceui.loxia.ClientManager
import ceui.loxia.CsrfTokenProvider
import ceui.pixiv.session.SessionManager
import ceui.pixiv.ui.common.PixivFragment
import ceui.pixiv.ui.common.setupMaterialHeader
import ceui.pixiv.ui.common.setUpToolbar
import ceui.pixiv.ui.common.viewBinding
import java.io.InputStream
import java.net.URLEncoder

class WebFragment : PixivFragment(R.layout.fragment_web) {

    private val binding by viewBinding(FragmentWebBinding::bind)

    private val args by lazy {
        val bundle = arguments ?: Bundle()
        object {
            val url = bundle.getString(Params.URL) ?: bundle.getString("url") ?: "https://www.pixiv.net/"
            val title = bundle.getString(Params.TITLE)
            val saveCookies = bundle.getBoolean("save_cookies", false)
            val htmlContent = bundle.getString(Params.RESPONSE)
            val mime = bundle.getString(Params.MIME) ?: "text/html"
            val encoding = bundle.getString(Params.ENCODING) ?: "utf-8"
            val historyUrl = bundle.getString(Params.HISTORY_URL)
        }
    }

    private var uploadMessageAboveL: ValueCallback<Array<Uri>>? = null
    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val data = result.data
            val results = if (data != null) {
                val dataString = data.dataString
                val clipData = data.clipData
                if (clipData != null) {
                    Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                } else if (dataString != null) {
                    arrayOf(Uri.parse(dataString))
                } else null
            } else null
            uploadMessageAboveL?.onReceiveValue(results)
        } else {
            uploadMessageAboveL?.onReceiveValue(null)
        }
        uploadMessageAboveL = null
    }

    private inner class CsrfBridge {
        @JavascriptInterface
        fun onCsrfToken(token: String) {
            CsrfTokenProvider.set(token)
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                back()
            }
        }
    }

    private fun back() {
        onBackPressedCallback.isEnabled = false
        if (!try {
                androidx.navigation.fragment.NavHostFragment.findNavController(this).popBackStack()
            } catch (e: Exception) {
                false
            }
        ) {
            activity?.finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpToolbar(binding.toolbarLayout, binding.refreshLayout)
        binding.toolbarLayout.naviTitle.text = args.title ?: ""
        binding.toolbarLayout.naviMore.setImageResource(R.drawable.ic_baseline_launch_24)
        binding.toolbarLayout.naviMore.setOnClickListener {
            val jumpUrl = binding.webView.url ?: args.url
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(jumpUrl)))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.refreshLayout.setupMaterialHeader(this)
        binding.refreshLayout.setEnableLoadMore(false)
        binding.refreshLayout.setOnRefreshListener {
            binding.webView.reload()
        }

        setupWebView()
        loadContent()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }

    private fun setupWebView() {
        val webSettings: WebSettings = binding.webView.settings
        webSettings.userAgentString = ClientManager.WEB_USER_AGENT
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        binding.webView.setBackgroundColor(0)

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.refreshLayout.finishRefresh()
                binding.progressBar.visibility = View.GONE
                
                if (args.saveCookies) {
                    val cookie = CookieManager.getInstance().getCookie("https://www.pixiv.net")
                    if (!cookie.isNullOrEmpty() && cookie.contains("PHPSESSID")) {
                        Shaft.getDefaultPrefs().edit { putString(SessionManager.COOKIE_KEY, cookie) }
                    }
                }

                if (requireContext().resources.getBoolean(R.bool.is_night_mode) && url?.contains("pixivision.net") == true) {
                    injectCSS()
                }

                if (url?.contains("www.pixiv.net") == true) {
                    extractCsrfToken(view)
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.progress = 0
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val destiny = uri.toString()
                if (destiny.contains("www.pixiv.net")) {
                    val segments = uri.pathSegments
                    val isNativeContent = segments.contains("artworks") ||
                            segments.contains("i") ||
                            segments.contains("users") ||
                            segments.contains("u") ||
                            (segments.contains("novel") && uri.getQueryParameter("id") != null) ||
                            segments.contains("n") ||
                            uri.getQueryParameter("illust_id") != null ||
                            (uri.getQueryParameter("id") != null && uri.path?.contains("member") == true)

                    if (isNativeContent) {
                        return try {
                            val intent = Intent(requireContext(), OutWakeActivity::class.java)
                            intent.setData(uri)
                            startActivity(intent)
                            true
                        } catch (e: Exception) {
                            false
                        }
                    } else {
                        return false
                    }
                } else if (destiny.startsWith("pixiv://") || destiny.startsWith("shaftintent://")) {
                    return try {
                        val intent = Intent(requireContext(), OutWakeActivity::class.java)
                        intent.setData(uri)
                        startActivity(intent)
                        true
                    } catch (e: Exception) {
                        false
                    }
                } else if (destiny.contains("intent://account/")) {
                    return try {
                        val urlForThisAPP = destiny.replace("intent", "shaftintent")
                        val intent = Intent(requireContext(), OutWakeActivity::class.java)
                        intent.setData(Uri.parse(urlForThisAPP))
                        startActivity(intent)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    binding.progressBar.visibility = View.GONE
                } else {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.progressBar.progress = newProgress
                }
            }

            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                uploadMessageAboveL = filePathCallback
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                }
                fileChooserLauncher.launch(Intent.createChooser(intent, "Image Chooser"))
                return true
            }
        }

        registerForContextMenu(binding.webView)
        binding.webView.addJavascriptInterface(CsrfBridge(), "CsrfBridge")
    }

    private fun loadContent() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)

        if (args.saveCookies) {
            cookieManager.removeAllCookies(null)
            cookieManager.flush()
        } else {
            val savedCookies = Shaft.getDefaultPrefs().getString(SessionManager.COOKIE_KEY, "")
            if (!savedCookies.isNullOrEmpty()) {
                for (cookie in savedCookies.split(";")) {
                    cookieManager.setCookie(args.url, cookie.trim())
                }
                cookieManager.flush()
            }
        }

        if (args.htmlContent != null) {
            binding.webView.loadDataWithBaseURL(args.url,
                args.htmlContent!!, args.mime, args.encoding, args.historyUrl)
        } else {
            binding.webView.loadUrl(args.url)
        }
    }

    private fun injectCSS() {
        try {
            val inputStream: InputStream = requireContext().assets.open("pixivision-dark.css")
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            val encoded = Base64.encodeToString(buffer, Base64.NO_WRAP)
            binding.webView.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractCsrfToken(view: WebView?) {
        view?.evaluateJavascript(
            """
            (function(){
                try {
                    if (window.pixiv && window.pixiv.context && window.pixiv.context.token) {
                        CsrfBridge.onCsrfToken(window.pixiv.context.token);
                        return;
                    }
                } catch(e) {}
                try {
                    if (window.globalInitData && window.globalInitData.token) {
                        CsrfBridge.onCsrfToken(window.globalInitData.token);
                        return;
                    }
                } catch(e) {}
                try {
                    var meta = document.getElementById('meta-global-data');
                    if (meta) {
                        var c = meta.getAttribute('content');
                        var m = c.match(/"token":"([a-f0-9]{32})"/);
                        if (m) { CsrfBridge.onCsrfToken(m[1]); return; }
                    }
                } catch(e) {}
            })()
            """.trimIndent(), null
        )
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        val result = binding.webView.hitTestResult
        val extra = result.extra ?: return
        
        menu.setHeaderTitle(extra)
        if (result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            menu.add(0, 1, 0, R.string.webview_handler_open_in_browser).setOnMenuItemClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(extra)))
                true
            }
            menu.add(0, 2, 1, R.string.webview_handler_copy_link_addr).setOnMenuItemClickListener {
                ClipBoardUtils.putTextIntoClipboard(requireContext(), extra)
                true
            }
        }
        if (result.type == WebView.HitTestResult.IMAGE_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
            menu.add(0, 3, 2, R.string.webview_handler_open_image).setOnMenuItemClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(extra), "image/*")
                startActivity(intent)
                true
            }
            menu.add(0, 4, 3, R.string.webview_handler_search_with_ggl).setOnMenuItemClickListener {
                val encodeUrl = URLEncoder.encode(extra, "utf-8")
                binding.webView.loadUrl("https://www.google.com/searchbyimage?image_url=$encodeUrl")
                true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ceui.lisa.feature.WeissUtil.end()
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun newInstance(url: String, title: String? = null, saveCookies: Boolean = false): WebFragment {
            return WebFragment().apply {
                arguments = Bundle().apply {
                    putString(Params.URL, url)
                    putString(Params.TITLE, title)
                    putBoolean("save_cookies", saveCookies)
                }
            }
        }

        @JvmStatic
        @JvmOverloads
        fun newInstance(title: String, url: String, response: String?, mime: String?, encoding: String?, historyUrl: String? = null): WebFragment {
            return WebFragment().apply {
                arguments = Bundle().apply {
                    putString(Params.TITLE, title)
                    putString(Params.URL, url)
                    putString(Params.RESPONSE, response)
                    putString(Params.MIME, mime)
                    putString(Params.ENCODING, encoding)
                    putString(Params.HISTORY_URL, historyUrl)
                }
            }
        }
    }
}
