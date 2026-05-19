package ceui.lisa.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import ceui.lisa.activities.Shaft
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.content.ContextCompat
import java.io.*
import java.util.zip.ZipInputStream

object AppKit {

    @JvmStatic
    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    @JvmStatic
    fun getActionBarHeight(context: Context): Int {
        val ta = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
        val height = ta.getDimension(0, 0f).toInt()
        ta.recycle()
        return height
    }

    @JvmStatic
    fun getColor(context: Context, colorResId: Int): Int {
        return ContextCompat.getColor(context, colorResId)
    }

    @JvmStatic
    fun getScreenWidth(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(dm)
        return dm.widthPixels
    }

    @JvmStatic
    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    @JvmStatic
    fun getExternalPicturesPath(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
    }

    @JvmStatic
    fun getExternalDownloadsPath(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    }

    @JvmStatic
    fun getInternalAppCachePath(context: Context): String {
        return context.cacheDir.absolutePath
    }

    @JvmStatic
    fun getExternalAppCachePath(context: Context): String {
        return context.externalCacheDir?.absolutePath ?: ""
    }

    @JvmStatic
    fun getDirSize(file: File?): Long {
        if (file == null || !file.exists()) return 0
        if (file.isFile) return file.length()
        val files = file.listFiles() ?: return 0
        return files.sumOf { getDirSize(it) }
    }

    @JvmStatic
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceIn(0, units.size - 1)
        return String.format("%.2f%s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    @JvmStatic
    fun getSize(file: File?): String = formatFileSize(getDirSize(file))

    @JvmStatic
    fun deleteAllInDir(dir: File?) {
        if (dir == null || !dir.exists()) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) deleteAllInDir(file)
            file.delete()
        }
    }

    @JvmStatic
    fun writeFileFromIS(filePath: String, inputStream: InputStream): Boolean {
        return try {
            val file = File(filePath)
            file.parentFile?.let { if (!it.exists()) it.mkdirs() }
            FileOutputStream(file).use { fos ->
                inputStream.use { it.copyTo(fos) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @JvmStatic
    fun uri2Bytes(context: Context, uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    }

    @JvmStatic
    fun uri2File(context: Context, uri: Uri): File {
        if (uri.scheme == "file") {
            return File(uri.path ?: "")
        }
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw FileNotFoundException("Cannot open URI: $uri")
        val fileName = getFileName(context, uri)
        val tempFile = File(context.cacheDir, fileName)
        tempFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
        FileOutputStream(tempFile).use { fos ->
            inputStream.use { it.copyTo(fos) }
        }
        return tempFile
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var fileName = "temp_file"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    /**
     * 使用 RenderScript 进行高斯模糊
     * 
     * ScriptIntrinsicBlur 是 Android 提供的 GPU 加速高斯模糊 intrinsic，
     * 虽然在 API 31 被标记为废弃，但在 API 24+ 上仍然完全可用且性能优秀。
     * 
     * @param bitmap 输入位图（会被原地修改）
     * @param radius 模糊半径，范围 0~25（RenderScript 限制）
     * @return 模糊后的位图（与输入为同一对象）
     */
    @JvmStatic
    fun fastBlur(bitmap: Bitmap, radius: Float): Bitmap {
        // 创建 RenderScript 上下文
        val rs = RenderScript.create(Shaft.getContext())
        // 从 bitmap 创建输入 Allocation，USAGE_SCRIPT 表示用于脚本计算
        val input = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)
        // 创建输出 Allocation，类型与输入相同
        val output = Allocation.createTyped(rs, input.type)
        // 创建高斯模糊 intrinsic，U8_4 对应 RGBA 四通道 8 位无符号
        val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        // 设置模糊半径，RenderScript 限制最大 25
        blur.setRadius(radius.coerceIn(0f, 25f))
        // 设置输入
        blur.setInput(input)
        // 执行模糊，结果写入 output
        blur.forEach(output)
        // 将结果拷贝回 bitmap
        output.copyTo(bitmap)
        // 释放 RenderScript 资源
        rs.destroy()
        return bitmap
    }

    @JvmStatic
    fun unzipFile(zipFile: File, destDir: File) {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
