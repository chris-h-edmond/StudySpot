package com.example.studyspot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyspot.data.model.*
import com.example.studyspot.data.repository.StudySpotRepository
import com.example.studyspot.data.session.SessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StudySpotUiState(val user: UserEntity?=null,val libraries:List<LibraryEntity> = emptyList(),val library:LibraryEntity?=null,val seats:List<SeatEntity> = emptyList(),val allSeats:List<SeatEntity> = emptyList(),val bookings:List<BookingEntity> = emptyList(),val all:List<BookingEntity> = emptyList())

class StudySpotViewModel(private val repo:StudySpotRepository,private val session:SessionManager):ViewModel() {
 private val s=MutableStateFlow(StudySpotUiState())
 val state=s.asStateFlow()
 private val e=MutableSharedFlow<String>()
 val events=e.asSharedFlow()
 init {
  viewModelScope.launch {
   session.userId.collectLatest {
     id->val u=id?.let {
    repo.user(it)
   }
    s.update {
     it.copy(user=u)
    }
    if(id!=null&&u==null)session.clear()
    if(u!=null) {
     repo.completeExpiredBookings()
     repo.bookings(u.id).collectLatest {
       b->s.update {
      it.copy(bookings=b)
     }
     }
    }
   }
  }
  viewModelScope.launch {
   repo.libraries.collectLatest {
     l->s.update {
    it.copy(libraries=l)
   }
   }
  }
  viewModelScope.launch {
   repo.allBookings.collectLatest {
     b->s.update {
    it.copy(all=b)
   }
   }
  }
  viewModelScope.launch {
   repo.allSeats.collectLatest {
     rows->s.update {
    it.copy(allSeats=rows)
   }
   }
  }
 }
 fun login(email:String,pw:String,done:()->Unit)=viewModelScope.launch {
  runCatching {
   repo.login(email,pw)
  }
   .onSuccess {
    if(it==null)e.emit("Email or password is incorrect")else {
     session.setUser(it.id)
     done()
    }
   }
   .onFailure {
    e.emit("Couldn't sign in")
   }
 }
 fun register(name:String,email:String,pw:String,done:()->Unit)=viewModelScope.launch {
  if(name.isBlank()||!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()||pw.length<8) {
   e.emit("Enter a name, valid email, and password with 8+ characters")
   return@launch
  }
  runCatching {
   repo.register(name,email,pw)
  }
   .onSuccess {
    session.setUser(it)
    done()
   }
   .onFailure {
    e.emit("Email already registered or account could not be created")
   }
 }
 fun select(l:LibraryEntity) {
  s.update {
   it.copy(library=l,seats=emptyList())
  }
  viewModelScope.launch {
   repo.seats(l.id).collectLatest {
     rows->s.update {
    it.copy(seats=rows)
   }
   }
  }
 }
 fun book(seat:SeatEntity,h:Int)=viewModelScope.launch {
  val u=s.value.user?:return@launch
  val t=System.currentTimeMillis()
  runCatching {
   repo.createBooking(u.id,seat.id,t,t+h*3600000L)
  }
   .onSuccess {
    e.emit("Seat ${seat.seatNumber} reserved")
   }
   .onFailure {
    e.emit(it.message?:"Booking failed")
   }
 }
 fun cancel(b:BookingEntity)=viewModelScope.launch {
  runCatching {
   repo.cancel(b.id)
  }
   .onSuccess {
    e.emit("Booking cancelled")
   }
   .onFailure {
    e.emit(it.message?:"Could not cancel")
   }
 }
 fun logout()=viewModelScope.launch {
  repo.logout()
 }
}

