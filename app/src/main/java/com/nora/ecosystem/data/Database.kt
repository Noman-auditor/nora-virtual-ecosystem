package com.nora.ecosystem.data

import androidx.room.*
import com.nora.ecosystem.core.VirtualSpace
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val id: String, val name: String, val type: String,
    val isolation: String, val backend: String, val status: String
) {
    companion object {
        fun fromDomain(d: VirtualSpace) =
            SpaceEntity(d.id, d.name, d.type.name, d.isolationLevel.name, d.networkBackend.name, d.status.name)
    }
}

@Entity(tableName = "audit")
data class AuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ts: Long, val actor: String, val action: String, val details: String
)

@Dao interface SpaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(e: SpaceEntity)
    @Query("SELECT * FROM spaces") fun observe(): Flow<List<SpaceEntity>>
    @Query("DELETE FROM spaces WHERE id = :id") suspend fun delete(id: String)
}
@Dao interface AuditDao {
    @Insert suspend fun insert(e: AuditEventEntity)
    @Query("SELECT * FROM audit ORDER BY ts DESC LIMIT 500") fun observe(): Flow<List<AuditEventEntity>>
}

@Database(entities = [SpaceEntity::class, AuditEventEntity::class], version = 1, exportSchema = false)
abstract class NoraDatabase : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao
    abstract fun auditDao(): AuditDao
}
