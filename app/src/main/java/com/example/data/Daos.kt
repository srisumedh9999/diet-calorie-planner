package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)
}

@Dao
interface DailyMealLogDao {
    @Query("SELECT * FROM daily_meal_logs WHERE date = :date ORDER BY timestamp DESC")
    fun getMealsByDateFlow(date: String): Flow<List<DailyMealLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: DailyMealLog)

    @Delete
    suspend fun deleteMeal(meal: DailyMealLog)

    @Query("DELETE FROM daily_meal_logs WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Long)

    @Query("SELECT SUM(calories) FROM daily_meal_logs WHERE date = :date")
    fun getCaloriesSumByDate(date: String): Flow<Int?>
}

@Dao
interface WeightProgressLogDao {
    @Query("SELECT * FROM weight_progress_logs ORDER BY date ASC, timestamp ASC")
    fun getAllWeightLogsFlow(): Flow<List<WeightProgressLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeightLog(weightProgressLog: WeightProgressLog)

    @Query("DELETE FROM weight_progress_logs WHERE id = :weightId")
    suspend fun deleteWeightLogById(weightId: Long)
}
