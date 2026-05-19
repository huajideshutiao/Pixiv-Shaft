package ceui.lisa.utils

import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.hjq.toast.Toaster
import com.qmuiteam.qmui.skin.QMUISkinManager
import com.qmuiteam.qmui.widget.dialog.QMUIDialog
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.charset.UnsupportedCharsetException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.IntStream
import ceui.lisa.R
import ceui.lisa.activities.MainActivity
import ceui.lisa.activities.Shaft
import ceui.lisa.activities.UActivity
import ceui.lisa.database.AppDatabase
import ceui.lisa.database.UserEntity
import ceui.lisa.download.FileCreator
import ceui.lisa.file.LegacyFile
import ceui.lisa.file.SAFile
import ceui.lisa.models.IllustsBean
import ceui.lisa.models.UserContainer
import ceui.pixiv.route.AppRoute
import ceui.pixiv.session.SessionManager

object Common {

    private val safeReplacer = arrayOf(
        arrayOf("|", "%7c"), arrayOf("\\", "%5c"), arrayOf("?", "%3f"),
        arrayOf("*", "\u22c6"), arrayOf("<", "%3c"), arrayOf("\"", "%22"),
        arrayOf(":", "%3a"), arrayOf(">", "%3e"), arrayOf("/", "%2f")
    )

    @JvmStatic
    fun isNumeric(str: String): Boolean {
        for (i in str.length - 1 downTo 0) {
            if (!Character.isDigit(str[i])) {
                return false
            }
        }
        return true
    }

    @JvmStatic
    fun isEmpty(list: List<*>?): Boolean {
        return list == null || list.isEmpty()
    }

    @JvmStatic
    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive && activity.currentFocus != null) {
            activity.currentFocus?.windowToken?.let {
                imm.hideSoftInputFromWindow(it, InputMethodManager.HIDE_NOT_ALWAYS)
            }
        }
    }

    @JvmStatic
    fun logOut(context: Context, deleteUser: Boolean) {
        if (SessionManager.isLoggedIn) {
            if (deleteUser) {
                val userEntity = UserEntity()
                userEntity.userID = SessionManager.loggedInUid.toInt()
                AppDatabase.getAppDatabase(context)
                    .downloadDao().deleteUser(userEntity)
            }
            SessionManager.updateSession(null)
            AppRoute.LoginRegister.start(context)
        }
    }

    @JvmStatic
    fun <T> showLog(t: T) {
        Log.d("==SHAFT==>", t.toString())
    }

    @JvmStatic
    fun <T> showToast(t: T) {
        Toaster.show(t)
    }

    @JvmStatic
    fun showToast(id: Int) {
        Toaster.show(id)
    }

    @JvmStatic
    fun <T> showToast(t: T, type: Int) {
        Toaster.show(t)
    }

    @JvmStatic
    fun getAppVersionCode(context: Context): String {
        var versioncode = 0
        try {
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, 0)
            versioncode = pi.versionCode
        } catch (e: Exception) {
            Log.e("VersionInfo", "Exception", e)
        }
        return versioncode.toString()
    }

    @JvmStatic
    fun getAppVersionName(context: Context): String? {
        var versionName: String? = null
        try {
            val pm = context.packageManager
            val pi = pm.getPackageInfo(context.packageName, 0)
            versionName = pi.versionName
        } catch (e: Exception) {
            Log.e("VersionInfo", "Exception", e)
        }
        return versionName
    }

    @JvmStatic
    fun <T> showToast(t: T, isLong: Boolean) {
        Toaster.show(t)
    }

    @JvmStatic
    fun copy(context: Context, s: String) {
        ClipBoardUtils.putTextIntoClipboard(context, s, true)
    }

    @JvmStatic
    fun copy(context: Context, s: String, hasHint: Boolean) {
        ClipBoardUtils.putTextIntoClipboard(context, s, hasHint)
    }

    @JvmStatic
    fun checkEmpty(before: String?): String {
        return if (TextUtils.isEmpty(before)) Shaft.getContext().getString(R.string.no_info) else before!!
    }

    @JvmStatic
    fun checkEmpty(before: EditText?): String {
        return if (before != null && before.text != null && !TextUtils.isEmpty(before.text.toString())) {
            before.text.toString()
        } else {
            ""
        }
    }

    @JvmStatic
    fun animate(linearLayout: LinearLayout) {
        val childCount = linearLayout.childCount
        for (i in 0 until childCount) {
            val view = linearLayout.getChildAt(i)
            view.translationX = 400f
            view.animate()
                .translationX(0f)
                .setStartDelay((i * 50).toLong())
                .setDuration(300)
                .start()
        }
    }

    @JvmStatic
    fun createDialog(context: Context) {
        val qmuiDialog = QMUIDialog.MessageDialogBuilder(context)
            .setTitle(context.getString(R.string.string_188))
            .setMessage(context.getString(R.string.dont_catch_me))
            .setSkinManager(QMUISkinManager.defaultInstance(context))
            .addAction(context.getString(R.string.string_189)) { dialog, _ ->
                Shaft.getDefaultPrefs().edit().putBoolean(Params.SHOW_DIALOG, false).apply()
                dialog.dismiss()
            }
            .addAction(context.getString(R.string.string_190)) { dialog, _ ->
                Shaft.getDefaultPrefs().edit().putBoolean(Params.SHOW_DIALOG, true).apply()
                dialog.dismiss()
            }
            .create()
        val window: Window? = qmuiDialog.window
        if (window != null) {
            window.setWindowAnimations(R.style.dialog_animation_scale)
        }
        qmuiDialog.show()
    }

    @JvmStatic
    fun getResponseBody(response: Response): String {
        val utf8 = StandardCharsets.UTF_8
        val responseBody = response.body!!
        val source = responseBody.source()
        try {
            source.request(Long.MAX_VALUE)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val buffer = source.buffer
        var charset: Charset = utf8
        val contentType = responseBody.contentType()
        if (contentType != null) {
            try {
                charset = contentType.charset(utf8)!!
            } catch (e: UnsupportedCharsetException) {
                e.printStackTrace()
            }
        }
        return buffer.clone().readString(charset)
    }

    @JvmStatic
    fun showUser(context: Context, userContainer: UserContainer) {
        val intent = Intent(context, UActivity::class.java)
        intent.putExtra(Params.USER_ID, userContainer.userId)
        context.startActivity(intent)
    }

    @JvmStatic
    fun <T> cutToJson(from: List<T>?): String {
        if (isEmpty(from)) {
            return ""
        }
        if (from!!.size > 5) {
            val temp = ArrayList<T>()
            for (i in 0..4) {
                temp.add(from[i])
            }
            return Shaft.sGson.toJson(temp)
        } else {
            return Shaft.sGson.toJson(from)
        }
    }

    @JvmStatic
    fun isAndroidQ(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    @JvmStatic
    fun restart() {
        val intent = Intent()
        val realActivityClassName = MainActivity::class.java.name
        intent.component = ComponentName(Shaft.getContext(), realActivityClassName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        Shaft.getContext().startActivity(intent)
    }

    @JvmStatic
    fun flatRandom(left: Int, right: Int): Int {
        val r = java.util.Random()
        return r.nextInt(right - left) + left
    }

    @JvmStatic
    fun flatRandom(right: Int): Int {
        return flatRandom(0, right)
    }

    @JvmStatic
    fun resolveThemeAttribute(context: Context, resId: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(resId, typedValue, true)
        return typedValue.data
    }

    @JvmStatic
    fun removeFSReservedChars(s: String): String {
        var result = s
        try {
            for (strings in safeReplacer) {
                result = result.replace(strings[0], strings[1])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    @JvmStatic
    fun isIllustDownloaded(illust: IllustsBean): Boolean {
        try {
            if (illust.page_count == 1) {
                return if (Shaft.sSettings.downloadWay == 1) {
                    SAFile.isFileExists(Shaft.getContext(), illust)
                } else {
                    FileCreator.isExist(illust, 0)
                }
            } else {
                val pageIndexStream = IntStream.range(0, illust.page_count)
                return if (Shaft.sSettings.downloadWay == 1) {
                    pageIndexStream
                        .allMatch { index -> SAFile.isFileExists(Shaft.getContext(), illust, index) }
                } else {
                    pageIndexStream
                        .allMatch { index -> FileCreator.isExist(illust, index) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    @JvmStatic
    fun isIllustDownloaded(illust: IllustsBean, index: Int): Boolean {
        try {
            if (illust.page_count == 1) {
                return if (Shaft.sSettings.downloadWay == 1) {
                    SAFile.isFileExists(Shaft.getContext(), illust)
                } else {
                    FileCreator.isExist(illust, 0)
                }
            } else {
                return if (Shaft.sSettings.downloadWay == 1) {
                    SAFile.isFileExists(Shaft.getContext(), illust, index)
                } else {
                    FileCreator.isExist(illust, index)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    @JvmStatic
    fun getLocalYYYYMMDDHHMMString(source: String): String {
        try {
            return ZonedDateTime.parse(source).withZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        } catch (e: Exception) {
            e.printStackTrace()
            return source.substring(0, 16)
        }
    }

    @JvmStatic
    fun getLocalYYYYMMDDHHMMSSString(source: String): String {
        try {
            return ZonedDateTime.parse(source).withZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } catch (e: Exception) {
            e.printStackTrace()
            return source
        }
    }

    @JvmStatic
    fun getLocalYYYYMMDDHHMMSSFileString(source: String): String {
        try {
            return ZonedDateTime.parse(source).withZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        } catch (e: Exception) {
            e.printStackTrace()
            return source
        }
    }

    @JvmStatic
    fun getNovelTextColor(): Int {
        val color = Shaft.sSettings.novelHolderTextColor
        return if (color == 0) {
            ContextCompat.getColor(Shaft.getContext(), R.color.white)
        } else color
    }

    @JvmStatic
    fun getNovelTextSize(): Int {
        val size = Shaft.sSettings.novelHolderTextSize
        return if (size == 0) 16 else size
    }

    @JvmStatic
    fun isFileSizeOkToReverseSearch(uri: Uri, maxImageSize: Long): Boolean {
        val cursor = Shaft.getContext().contentResolver.query(uri, null, null, null, null)
            ?: return false
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        cursor.moveToFirst()
        val ret = cursor.getLong(sizeIndex) <= maxImageSize
        cursor.close()
        return ret
    }

    @JvmStatic
    fun copyUriToImageCacheFolder(uri: Uri): File? {
        var inputStream: InputStream? = null
        try {
            inputStream = Shaft.getContext().contentResolver.openInputStream(uri)
            val file = File(LegacyFile.imageCacheFolder(Shaft.getContext()), System.currentTimeMillis().toString())
            AppKit.writeFileFromIS(file.absolutePath, inputStream!!)
            return file
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return null
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @JvmStatic
    fun copyBitmapToImageCacheFolder(bitmap: Bitmap, fileName: String): Uri? {
        try {
            val cachePath = File(Shaft.getContext().externalCacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, fileName)
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()
            return FileProvider.getUriForFile(
                Shaft.getContext(),
                Shaft.getContext().packageName + ".provider",
                file
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    @JvmStatic
    @JvmOverloads
    fun shareImageFile(
        context: Context,
        imageFile: File,
        fileName: String,
        shareText: String? = null
    ) {
        try {
            val cachePath = File(Shaft.getContext().externalCacheDir, "images")
            cachePath.mkdirs()
            val sharedFile = File(cachePath, fileName)
            FileInputStream(imageFile).use { inputStream ->
                FileOutputStream(sharedFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (inputStream.read(buffer).also { len = it } > 0) {
                        outputStream.write(buffer, 0, len)
                    }
                }
            }
            val uri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                sharedFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.clipData = ClipData.newRawUri(null, uri)
            if (shareText != null) {
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            }
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share))
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
