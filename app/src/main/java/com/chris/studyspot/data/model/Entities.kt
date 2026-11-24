package com.chris.studyspot.data.model

import androidx.room.*

@Entity(indices = [Index(value = ["email"], unique = true)])
data class UserEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val email: String, val passwordHash: String, val passwordSalt: String, val createdAt: Long = System.currentTimeMillis())

@Entity
data class LibraryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val address: String, val description: String, val latitude: Double?, val longitude: Double?, val totalSeats: Int, val sectionName: String)

@Entity(foreignKeys = [ForeignKey(entity = LibraryEntity::class, parentColumns = ["id"], childColumns = ["libraryId"], onDelete = ForeignKey.CASCADE)], indices = [Index("libraryId"), Index(value = ["libraryId", "seatNumber"], unique = true)])
data class SeatEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val libraryId: Long, val seatNumber: String, val rowLabel: String, val columnNumber: Int, val isBlocked: Boolean = false, val sectionName: String)

enum class BookingStatus { ACTIVE, COMPLETED, CANCELLED }

object BookingRules {
    fun overlaps(existingStart: Long, existingEnd: Long, requestedStart: Long, requestedEnd: Long): Boolean =
        existingStart < requestedEnd && existingEnd > requestedStart

    fun validDuration(start: Long, end: Long): Boolean {
        val duration = end - start
        return duration in 3_600_000L..10_800_000L && duration % 3_600_000L == 0L
    }
}

@Entity(foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE), ForeignKey(entity = SeatEntity::class, parentColumns = ["id"], childColumns = ["seatId"], onDelete = ForeignKey.CASCADE)], indices = [Index("userId"), Index("seatId"), Index(value = ["startTime", "endTime"])])
data class BookingEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val userId: Long, val seatId: Long, val startTime: Long, val endTime: Long, val status: BookingStatus = BookingStatus.ACTIVE, val createdAt: Long = System.currentTimeMillis())

class Converters {
    @TypeConverter fun statusToString(value: BookingStatus) = value.name
    @TypeConverter fun stringToStatus(value: String) = BookingStatus.valueOf(value)
}

