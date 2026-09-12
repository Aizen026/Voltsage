package com.aizen.voltsage.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_packs")
data class StudyPackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val sourceType: String,
    val subject: String,
    val summary: String,
    val detailedNotes: String,
    val keyTakeawaysJson: String,
    val quizQuestionsJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long = 0,
    val nextReviewDate: String = "",
    val masteryScore: Int = 0,
    val isFavorite: Boolean = false,
    val sharedInGroupId: Long? = null
)

@Entity(tableName = "review_schedules")
data class ReviewScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studyPackId: Long,
    val studyPackTitle: String,
    val subject: String,
    val scheduledDate: String, // YYYY-MM-DD
    val completed: Boolean = false,
    val reviewType: String = "QUIZ"
)

@Entity(tableName = "study_groups")
data class StudyGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val subject: String,
    val description: String,
    val code: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val activeChallenge: String = "Score 80%+ on weekly retention quiz"
)
