package com.example.data

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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "library_roots")
data class LibraryRootEntity(
    @PrimaryKey val treeUri: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "hidden_folders")
data class HiddenFolderEntity(
    @PrimaryKey val folderId: String,
    val folderName: String,
    val displayPath: String,
    val hiddenAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_progress")
data class PlaybackProgressEntity(
    @PrimaryKey val videoUri: String,
    val title: String,
    val folderId: String,
    val folderName: String,
    val positionMs: Long,
    val durationMs: Long,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
    val manualAudioUri: String? = null,
    val manualAudioName: String? = null,
    val manualSubtitleUri: String? = null,
    val manualSubtitleName: String? = null
)

@Dao
interface NegaiDao {
    @Query("SELECT * FROM library_roots ORDER BY addedAt ASC")
    fun observeLibraryRoots(): Flow<List<LibraryRootEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryRoot(root: LibraryRootEntity)

    @Query("DELETE FROM library_roots WHERE treeUri = :treeUri")
    suspend fun deleteLibraryRoot(treeUri: String)

    @Query("SELECT * FROM hidden_folders ORDER BY hiddenAt DESC")
    fun observeHiddenFolders(): Flow<List<HiddenFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideFolder(entity: HiddenFolderEntity)

    @Query("DELETE FROM hidden_folders WHERE folderId = :folderId")
    suspend fun unhideFolder(folderId: String)

    @Query("SELECT * FROM playback_progress ORDER BY updatedAt DESC")
    fun observeAllProgress(): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress ORDER BY updatedAt DESC")
    suspend fun getAllProgressSnapshot(): List<PlaybackProgressEntity>

    @Query("SELECT * FROM playback_progress WHERE videoUri = :videoUri LIMIT 1")
    suspend fun getProgressForVideo(videoUri: String): PlaybackProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(entity: PlaybackProgressEntity)

    @Query("DELETE FROM playback_progress WHERE videoUri = :videoUri")
    suspend fun deleteProgress(videoUri: String)

    @Query("DELETE FROM playback_progress")
    suspend fun clearAllProgress()
}

typealias KinoDao = NegaiDao

@Database(
    entities = [
        LibraryRootEntity::class,
        HiddenFolderEntity::class,
        PlaybackProgressEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NegaiDatabase : RoomDatabase() {
    abstract fun negaiDao(): NegaiDao
    fun kinoDao(): NegaiDao = negaiDao()

    companion object {
        @Volatile
        private var instance: NegaiDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN videoWidth INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playback_progress ADD COLUMN videoHeight INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): NegaiDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NegaiDatabase::class.java,
                    "negai_player_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

typealias KinoDatabase = NegaiDatabase
