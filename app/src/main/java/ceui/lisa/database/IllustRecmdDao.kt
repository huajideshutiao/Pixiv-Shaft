package ceui.lisa.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface IllustRecmdDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(userEntity: IllustRecmdEntity)

    @Insert
    fun insertAll(userEntities: List<IllustRecmdEntity>)

    @Delete
    fun delete(userEntity: IllustRecmdEntity)

    @Query("SELECT * FROM (SELECT * FROM illust_recmd_table ORDER BY time DESC LIMIT 20) ORDER BY time")
    fun getAll(): List<IllustRecmdEntity>
}
