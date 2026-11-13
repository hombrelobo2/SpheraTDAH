package app.sphera.returnpoint

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** Dónde lo dejaste. Solo existe uno: id fijo a 1, cada guardado sobrescribe el anterior. */
@Entity(tableName = "return_point")
data class ReturnPoint(
    @PrimaryKey val id: Int = 1,
    val contextLocation: String,
    val nextStep: String,
    val requiredTools: String,
)

@Dao
interface ReturnPointDao {
    @Query("SELECT * FROM return_point WHERE id = 1")
    suspend fun get(): ReturnPoint?

    @Query("SELECT * FROM return_point WHERE id = 1")
    fun observe(): Flow<ReturnPoint?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(point: ReturnPoint)

    @Query("DELETE FROM return_point")
    suspend fun clear()
}

@Database(entities = [ReturnPoint::class], version = 1, exportSchema = false)
abstract class SpheraDb : RoomDatabase() {
    abstract fun returnPoints(): ReturnPointDao

    companion object {
        @Volatile private var instance: SpheraDb? = null

        fun get(ctx: Context): SpheraDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(ctx.applicationContext, SpheraDb::class.java, "sphera.db").build()
                .also { instance = it }
        }
    }
}
