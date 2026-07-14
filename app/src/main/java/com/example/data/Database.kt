package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String,
    val name: String,
    val role: String, // "Admin", "Family", "Guest"
    val isFaceEnrolled: Boolean,
    val isFingerprintEnrolled: Boolean,
    val isVoiceEnrolled: Boolean,
    val enrolledVoicePhrase: String
)

@Entity(tableName = "command_history")
data class CommandHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val inputText: String,
    val parsedIntent: String,
    val targetDevice: String, // "Phone", "Laptop", "Both"
    val riskTier: String, // "Low", "Medium", "High"
    val executionStatus: String, // "Success", "Failed", "Unauthorized", "Pending Approval"
    val responseText: String,
    val technicalLogs: String
)

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles")
    fun getAllProfilesFlow(): Flow<List<UserProfile>>

    @Query("SELECT * FROM user_profiles WHERE id = :id")
    suspend fun getProfileById(id: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)

    @Delete
    suspend fun deleteProfile(profile: UserProfile)
}

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<CommandHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CommandHistory)

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()
}

@Database(entities = [UserProfile::class, CommandHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun commandHistoryDao(): CommandHistoryDao
}
