package com.example.studyspot

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.studyspot.data.local.StudySpotDatabase
import com.example.studyspot.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {
    private lateinit var database: StudySpotDatabase

    @Before fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, StudySpotDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun closeDatabase() = database.close()

    @Test fun libraryAndItsSeatsCanBeInsertedAndObserved() = runBlocking {
        val libraryId = database.libraries().insert(
            LibraryEntity(name = "Test Library", address = "Campus", description = "Test", latitude = null, longitude = null, totalSeats = 2, sectionName = "Hall")
        )
        database.seats().insertAll(listOf(
            SeatEntity(libraryId = libraryId, seatNumber = "A1", rowLabel = "A", columnNumber = 1, sectionName = "Hall"),
            SeatEntity(libraryId = libraryId, seatNumber = "A2", rowLabel = "A", columnNumber = 2, sectionName = "Hall")
        ))

        assertEquals(2, database.seats().observeForLibrary(libraryId).first().size)
    }

    @Test fun bookingsRetainCancelledHistoryAndAllowNewBookingAfterCancellation() = runBlocking {
        val userId = database.users().insert(UserEntity(name = "Student", email = "student@example.com", passwordHash = "hash", passwordSalt = "salt"))
        val libraryId = database.libraries().insert(LibraryEntity(name = "Test Library", address = "Campus", description = "Test", latitude = null, longitude = null, totalSeats = 1, sectionName = "Hall"))
        database.seats().insertAll(listOf(SeatEntity(libraryId = libraryId, seatNumber = "A1", rowLabel = "A", columnNumber = 1, sectionName = "Hall")))
        val seat = database.seats().observeForLibrary(libraryId).first().single()
        val start = System.currentTimeMillis() + 60_000
        val id = database.bookings().insert(BookingEntity(userId = userId, seatId = seat.id, startTime = start, endTime = start + 3_600_000))
        val booking = database.bookings().byId(id)!!
        database.bookings().update(booking.copy(status = BookingStatus.CANCELLED))

        assertEquals(BookingStatus.CANCELLED, database.bookings().observeForUser(userId).first().single().status)
        assertEquals(null, database.bookings().seatConflict(seat.id, start, start + 3_600_000))
    }
}
