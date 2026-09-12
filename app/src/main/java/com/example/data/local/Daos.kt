package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyPackDao {
    @Query("SELECT * FROM study_packs ORDER BY createdAt DESC")
    fun getAllPacks(): Flow<List<StudyPackEntity>>

    @Query("SELECT * FROM study_packs WHERE id = :id LIMIT 1")
    suspend fun getPackById(id: Long): StudyPackEntity?

    @Query("SELECT * FROM study_packs WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoritePacks(): Flow<List<StudyPackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: StudyPackEntity): Long

    @Update
    suspend fun updatePack(pack: StudyPackEntity)

    @Query("UPDATE study_packs SET masteryScore = :score, lastReviewedAt = :timestamp WHERE id = :id")
    suspend fun updateMastery(id: Long, score: Int, timestamp: Long)

    @Query("UPDATE study_packs SET quizQuestionsJson = :quizJson WHERE id = :id")
    suspend fun updateQuizQuestions(id: Long, quizJson: String)

    @Query("UPDATE study_packs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE study_packs SET nextReviewDate = :nextDate WHERE id = :id")
    suspend fun updateNextReviewDate(id: Long, nextDate: String)

    @Query("DELETE FROM study_packs WHERE id = :id")
    suspend fun deletePackById(id: Long)

    @Query("SELECT COUNT(*) FROM study_packs")
    fun getTotalPacksCount(): Flow<Int>
}

@Dao
interface ReviewScheduleDao {
    @Query("SELECT * FROM review_schedules ORDER BY scheduledDate ASC")
    fun getAllReviews(): Flow<List<ReviewScheduleEntity>>

    @Query("SELECT * FROM review_schedules WHERE scheduledDate = :date")
    fun getReviewsForDate(date: String): Flow<List<ReviewScheduleEntity>>

    @Query("SELECT * FROM review_schedules WHERE completed = 0 ORDER BY scheduledDate ASC")
    fun getPendingReviews(): Flow<List<ReviewScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewScheduleEntity): Long

    @Query("UPDATE review_schedules SET completed = :completed WHERE id = :id")
    suspend fun updateCompleted(id: Long, completed: Boolean)

    @Query("DELETE FROM review_schedules WHERE id = :id")
    suspend fun deleteReviewById(id: Long)

    @Query("DELETE FROM review_schedules WHERE studyPackId = :studyPackId")
    suspend fun deleteReviewsForPack(studyPackId: Long)
}

@Dao
interface StudyGroupDao {
    @Query("SELECT * FROM study_groups ORDER BY createdTimestamp DESC")
    fun getAllGroups(): Flow<List<StudyGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: StudyGroupEntity): Long

    @Query("DELETE FROM study_groups WHERE id = :id")
    suspend fun deleteGroupById(id: Long)
}
