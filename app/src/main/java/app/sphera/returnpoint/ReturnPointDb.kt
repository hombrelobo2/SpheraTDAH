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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.sphera.rabbit.RabbitHole
import app.sphera.rabbit.RabbitHoleDao
import kotlinx.coroutines.flow.Flow

/** v2 añade "No sigas al conejo" sin tocar el Punto de Retorno ya guardado. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `rabbit_hole` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`question` TEXT NOT NULL, `notes` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        )
    }
}

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

@Database(entities = [ReturnPoint::class, RabbitHole::class], version = 2, exportSchema = false)
abstract class SpheraDb : RoomDatabase() {
    abstract fun returnPoints(): ReturnPointDao
    abstract fun rabbitHoles(): RabbitHoleDao

    companion object {
        @Volatile private var instance: SpheraDb? = null

        fun get(ctx: Context): SpheraDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(ctx.applicationContext, SpheraDb::class.java, "sphera.db")
                .addMigrations(MIGRATION_1_2).build()
                .also { instance = it }
        }
    }
}
