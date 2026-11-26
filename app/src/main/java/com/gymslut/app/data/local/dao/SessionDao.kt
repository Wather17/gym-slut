package com.gymslut.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gymslut.app.data.local.entity.SessionSetEntity
import com.gymslut.app.data.local.entity.WorkoutSessionEntity
import com.gymslut.app.data.model.ExerciseHistoryRow
import com.gymslut.app.data.model.SessionWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM workout_sessions WHERE finishedAtEpochMs IS NULL ORDER BY startedAtEpochMs DESC LIMIT 1")
    fun observeInProgressSession(): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE finishedAtEpochMs IS NULL ORDER BY startedAtEpochMs DESC LIMIT 1")
    suspend fun getInProgressSession(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE finishedAtEpochMs IS NOT NULL ORDER BY startedAtEpochMs DESC")
    fun observeCompletedSessions(): Flow<List<WorkoutSessionEntity>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE finishedAtEpochMs IS NOT NULL ORDER BY finishedAtEpochMs DESC, startedAtEpochMs DESC")
    fun observeCompletedSessionsWithSets(): Flow<List<SessionWithSets>>

    @Query(
        """
        SELECT ws.id AS sessionId,
               ws.workoutNameSnapshot AS workoutNameSnapshot,
               ws.startedAtEpochMs AS startedAtEpochMs,
               ws.finishedAtEpochMs AS finishedAtEpochMs,
               ss.exerciseNameSnapshot AS exerciseNameSnapshot,
               ss.equipmentNameSnapshot AS equipmentNameSnapshot,
               ss.setNumber AS setNumber,
               ss.weightKg AS weightKg,
               ss.reps AS reps,
               ss.completed AS completed
        FROM session_sets ss
        INNER JOIN workout_sessions ws ON ss.sessionId = ws.id
        WHERE ss.exerciseId = :exerciseId
          AND ws.finishedAtEpochMs IS NOT NULL
        ORDER BY ws.finishedAtEpochMs DESC, ss.setNumber ASC, ss.id ASC
        """
    )
    fun observeCompletedExerciseHistory(exerciseId: Long): Flow<List<ExerciseHistoryRow>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun observeSessionById(sessionId: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM session_sets WHERE sessionId = :sessionId ORDER BY exerciseOrderIndex ASC, setNumber ASC, id ASC")
    fun observeSetsForSession(sessionId: Long): Flow<List<SessionSetEntity>>

    @Query("SELECT * FROM session_sets WHERE sessionId = :sessionId ORDER BY exerciseOrderIndex ASC, setNumber ASC, id ASC")
    suspend fun getSetsForSession(sessionId: Long): List<SessionSetEntity>

    @Query(
        """
        SELECT ss.*
        FROM session_sets ss
        INNER JOIN workout_sessions ws ON ss.sessionId = ws.id
        WHERE ss.exerciseId = :exerciseId
          AND ss.completed = 1
          AND ws.finishedAtEpochMs IS NOT NULL
          AND ws.id = (
              SELECT ws2.id
              FROM workout_sessions ws2
              INNER JOIN session_sets ss2 ON ss2.sessionId = ws2.id
              WHERE ss2.exerciseId = :exerciseId
                AND ss2.completed = 1
                AND ws2.finishedAtEpochMs IS NOT NULL
              ORDER BY ws2.finishedAtEpochMs DESC
              LIMIT 1
          )
        ORDER BY ss.setNumber ASC, ss.id ASC
        """
    )
    suspend fun getLatestCompletedSetsForExercise(exerciseId: Long): List<SessionSetEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Transaction
    suspend fun insertSessionWithSets(
        session: WorkoutSessionEntity,
        sets: List<SessionSetEntity>
    ): Long {
        val sessionId = insertSession(session)
        if (sets.isNotEmpty()) {
            insertSets(sets.map { it.copy(sessionId = sessionId) })
        }
        return sessionId
    }

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSets(sets: List<SessionSetEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSet(set: SessionSetEntity): Long

    @Update
    suspend fun updateSet(set: SessionSetEntity)

    @Query("UPDATE session_sets SET weightKg = :weightKg WHERE id = :setId")
    suspend fun updateSetWeight(setId: Long, weightKg: Double)

    @Query("UPDATE session_sets SET reps = :reps WHERE id = :setId")
    suspend fun updateSetReps(setId: Long, reps: Int)

    @Query("UPDATE session_sets SET completed = :completed, completedAtEpochMs = :completedAtEpochMs WHERE id = :setId")
    suspend fun updateSetCompleted(setId: Long, completed: Boolean, completedAtEpochMs: Long?)

    @Query("DELETE FROM session_sets WHERE id = :setId")
    suspend fun deleteSetById(setId: Long)
}
