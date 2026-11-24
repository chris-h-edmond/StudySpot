package com.example.studyspot.data.local

import androidx.room.*
import com.example.studyspot.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(user: UserEntity): Long
    @Query("SELECT * FROM UserEntity WHERE email = :email LIMIT 1") suspend fun byEmail(email: String): UserEntity?
    @Query("SELECT * FROM UserEntity WHERE id = :id LIMIT 1") suspend fun byId(id: Long): UserEntity?
}
@Dao interface LibraryDao {
    @Query("SELECT * FROM LibraryEntity ORDER BY name") fun observeAll(): Flow<List<LibraryEntity>>
    @Query("SELECT * FROM LibraryEntity WHERE id = :id LIMIT 1") fun observe(id: Long): Flow<LibraryEntity?>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(library: LibraryEntity): Long
    @Query("SELECT COUNT(*) FROM LibraryEntity") suspend fun count(): Int
}
@Dao interface SeatDao {
    @Query("SELECT * FROM SeatEntity ORDER BY libraryId, rowLabel, columnNumber") fun observeAll(): Flow<List<SeatEntity>>
    @Query("SELECT * FROM SeatEntity WHERE libraryId = :libraryId ORDER BY rowLabel, columnNumber") fun observeForLibrary(libraryId: Long): Flow<List<SeatEntity>>
    @Query("SELECT * FROM SeatEntity WHERE id = :id LIMIT 1") suspend fun byId(id: Long): SeatEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(seats: List<SeatEntity>)
}
@Dao interface BookingDao {
    @Query("SELECT * FROM BookingEntity WHERE status != 'CANCELLED'") fun observeAll(): Flow<List<BookingEntity>>
    @Query("UPDATE BookingEntity SET status = 'COMPLETED' WHERE status = 'ACTIVE' AND endTime <= :now") suspend fun completeExpired(now: Long)
    @Query("SELECT * FROM BookingEntity WHERE userId = :userId ORDER BY startTime DESC") fun observeForUser(userId: Long): Flow<List<BookingEntity>>
    @Query("SELECT * FROM BookingEntity WHERE seatId = :seatId AND status != 'CANCELLED' AND startTime < :end AND endTime > :start LIMIT 1") suspend fun seatConflict(seatId: Long, start: Long, end: Long): BookingEntity?
    @Query("SELECT * FROM BookingEntity WHERE userId = :userId AND status != 'CANCELLED' AND startTime < :end AND endTime > :start LIMIT 1") suspend fun userConflict(userId: Long, start: Long, end: Long): BookingEntity?
    @Query("SELECT * FROM BookingEntity WHERE id = :id LIMIT 1") suspend fun byId(id: Long): BookingEntity?
    @Insert suspend fun insert(booking: BookingEntity): Long
    @Update suspend fun update(booking: BookingEntity)
}

