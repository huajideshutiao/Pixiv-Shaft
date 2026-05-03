package ceui.lisa.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DownloadDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(illustTask: DownloadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDownloading(entity: DownloadingEntity)

    @Delete
    fun deleteDownloading(entity: DownloadingEntity)

    @Delete
    fun delete(userEntity: DownloadEntity)

    @Query("SELECT * FROM illust_download_table ORDER BY downloadTime DESC LIMIT :limit OFFSET :offset")
    fun getAll(limit: Int, offset: Int): List<DownloadEntity>

    @Query(
        "SELECT COUNT(*) > 0 FROM illust_download_table WHERE " +
            "illustGson LIKE '%\"id\":' || :illustId || ',%' OR " +
            "illustGson LIKE '%\"id\":' || :illustId || '}%'"
    )
    fun hasDownloadRecordByIllustId(illustId: Long): Boolean

    @Query("SELECT * FROM illust_downloading_table")
    fun getAllDownloading(): List<DownloadingEntity>

    @Query("SELECT * FROM illust_downloading_table ORDER BY rowid DESC LIMIT :limit")
    fun getRecentDownloading(limit: Int): List<DownloadingEntity>

    @Query("DELETE FROM illust_downloading_table WHERE rowid NOT IN (SELECT rowid FROM illust_downloading_table ORDER BY rowid DESC LIMIT :keep)")
    fun trimDownloading(keep: Int)

    @Query("DELETE FROM illust_download_table")
    fun deleteAllDownload()

    @Query("DELETE FROM illust_downloading_table")
    fun deleteAllDownloading()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(illustHistoryEntity: IllustHistoryEntity)

    @Delete
    fun delete(userEntity: IllustHistoryEntity)

    @Query("DELETE FROM illust_table")
    fun deleteAllHistory()

    @Query("SELECT * FROM illust_table ORDER BY time DESC LIMIT :limit OFFSET :offset")
    fun getAllViewHistory(limit: Int, offset: Int): List<IllustHistoryEntity>

    @Query("SELECT * FROM illust_table")
    fun getAllViewHistoryEntities(): List<IllustHistoryEntity>

    @Query("SELECT COUNT(*) FROM illust_table")
    fun getViewHistoryCount(): Int

    @Query("SELECT * FROM illust_table WHERE type = :type ORDER BY time DESC LIMIT :limit OFFSET :offset")
    fun getViewHistoryByType(type: Int, limit: Int, offset: Int): List<IllustHistoryEntity>

    @Query("SELECT COUNT(*) FROM illust_table WHERE type = :type")
    fun getViewHistoryCountByType(type: Int): Int

    @Query("DELETE FROM illust_table WHERE type = :type")
    fun deleteAllHistoryByType(type: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(userEntity: UserEntity)

    @Delete
    fun deleteUser(userEntity: UserEntity)

    @Query("SELECT * FROM user_table ORDER BY loginTime DESC")
    fun getAllUser(): List<UserEntity>

    @Query("SELECT * FROM user_table limit 1")
    fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM upload_image_table ORDER BY uploadTime DESC")
    fun getUploadedImage(): List<ImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUploadedImage(imageEntity: ImageEntity)
}
