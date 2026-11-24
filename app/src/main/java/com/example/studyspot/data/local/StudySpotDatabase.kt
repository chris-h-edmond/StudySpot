package com.example.studyspot.data.local

import android.content.Context
import androidx.room.*
import com.example.studyspot.data.model.*

@Database(entities = [UserEntity::class, LibraryEntity::class, SeatEntity::class, BookingEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class StudySpotDatabase : RoomDatabase() {
    abstract fun users(): UserDao
    abstract fun libraries(): LibraryDao
    abstract fun seats(): SeatDao
    abstract fun bookings(): BookingDao
    companion object {
        @Volatile private var instance: StudySpotDatabase? = null
        fun get(context: Context): StudySpotDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, StudySpotDatabase::class.java, "studyspot.db").build().also { database ->
                instance = database
                Thread { kotlinx.coroutines.runBlocking {
                    if (database.libraries().count() == 0) {
                        val one = database.libraries().insert(LibraryEntity(name = "Central Library", address = "Campus Main Building", description = "Quiet study spaces across two reading halls", latitude = 28.545, longitude = 77.192, totalSeats = 40, sectionName = "Reading Hall A"))
                        val two = database.libraries().insert(LibraryEntity(name = "Academic Reading Hall", address = "Campus Academic Block", description = "A calm place for focused study", latitude = 28.547, longitude = 77.195, totalSeats = 24, sectionName = "Study Area"))
                        seedSeats(database, one, 40, "Reading Hall A")
                        seedSeats(database, two, 24, "Study Area")
                    }
                } }.start()
            }
        }
        private suspend fun seedSeats(db: StudySpotDatabase, libraryId: Long, count: Int, section: String) {
            db.seats().insertAll((0 until count).map { i ->
                val row = ('A'.code + i / 8).toChar().toString()
                SeatEntity(libraryId = libraryId, seatNumber = "$row${i % 8 + 1}", rowLabel = row, columnNumber = i % 8 + 1, isBlocked = i % 8 == 7 || i == 30, sectionName = section)
            })
        }
    }
}

