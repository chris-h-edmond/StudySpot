package com.chris.studyspot.data.repository

import androidx.room.withTransaction
import com.chris.studyspot.data.local.StudySpotDatabase
import com.chris.studyspot.data.model.*
import com.chris.studyspot.data.session.SessionManager
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class StudySpotRepository(private val db: StudySpotDatabase, private val session: SessionManager) {
    val libraries = db.libraries().observeAll()
    val allBookings = db.bookings().observeAll()
    val allSeats = db.seats().observeAll()
    fun seats(libraryId: Long) = db.seats().observeForLibrary(libraryId)
    fun bookings(userId: Long) = db.bookings().observeForUser(userId)
    fun library(id: Long) = db.libraries().observe(id)
    suspend fun user(id: Long) = db.users().byId(id)
    suspend fun register(name: String, email: String, password: String): Long {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return db.users().insert(UserEntity(name = name.trim(), email = email.trim().lowercase(), passwordHash = derive(password, salt), passwordSalt = salt.toHex()))
    }
    suspend fun login(email: String, password: String): UserEntity? {
        val user = db.users().byEmail(email.trim().lowercase()) ?: return null
        return user.takeIf { derive(password, it.passwordSalt.hexToBytes()) == it.passwordHash }
    }
    suspend fun createBooking(userId: Long, seatId: Long, start: Long, end: Long) = db.withTransaction {
        require(start >= System.currentTimeMillis() - 60_000L && BookingRules.validDuration(start, end)) { "Choose a future time and a duration of 1, 2, or 3 hours." }
        val seat = db.seats().byId(seatId) ?: error("This seat is no longer available.")
        require(!seat.isBlocked) { "This seat is unavailable." }
        require(db.bookings().seatConflict(seatId, start, end) == null) { "This seat has another booking at that time." }
        require(db.bookings().userConflict(userId, start, end) == null) { "You already have a booking during that time." }
        db.bookings().insert(BookingEntity(userId = userId, seatId = seatId, startTime = start, endTime = end))
    }
    suspend fun cancel(id: Long) = db.withTransaction {
        val booking = db.bookings().byId(id) ?: error("Booking not found.")
        require(booking.status == BookingStatus.ACTIVE && booking.endTime > System.currentTimeMillis()) { "This booking can no longer be cancelled." }
        db.bookings().update(booking.copy(status = BookingStatus.CANCELLED))
    }
    suspend fun logout() = session.clear()
    suspend fun completeExpiredBookings() = db.bookings().completeExpired(System.currentTimeMillis())
    private fun derive(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.toHex() } finally { spec.clearPassword() }
    }
}
private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
private fun String.hexToBytes() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
