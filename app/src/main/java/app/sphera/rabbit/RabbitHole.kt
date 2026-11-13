package app.sphera.rabbit

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Una curiosidad aparcada. Nunca se marca como terminada: solo está ahí para cuando apetezca. */
@Entity(tableName = "rabbit_hole")
data class RabbitHole(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface RabbitHoleDao {
    @Insert
    suspend fun add(hole: RabbitHole)

    @Update
    suspend fun update(hole: RabbitHole)

    @Query("DELETE FROM rabbit_hole WHERE id = :id")
    suspend fun delete(id: Long)

    /** Todas, la más reciente primero. */
    @Query("SELECT * FROM rabbit_hole ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RabbitHole>>

    @Query("SELECT * FROM rabbit_hole WHERE id = :id")
    suspend fun get(id: Long): RabbitHole?

    @Query("SELECT COUNT(*) FROM rabbit_hole")
    suspend fun count(): Int
}
