package com.chris.studyspot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chris.studyspot.data.model.*
import com.chris.studyspot.viewmodel.StudySpotUiState
import java.text.DateFormat
import java.util.Date

@Composable fun BookingScreen(s:StudySpotUiState,back:()->Unit,cancel:(BookingEntity)->Unit,open:(BookingEntity)->Unit) {
 var history by remember {
  mutableStateOf(false)
 }
 var pending by remember {
  mutableStateOf<BookingEntity?>(null)
 }
 Column(Modifier.fillMaxSize()) {
  Header("My Bookings","Your local reservations",back)
  Row(Modifier.padding(horizontal=12.dp)) {
   FilterChip(!history, {
    history=false
   }
    , {
     Text("Active")
    }
   )
   Spacer(Modifier.width(8.dp))
   FilterChip(history, {
    history=true
   }
    , {
     Text("History")
    }
   )
  }
  val rows=s.bookings.filter {
   (it.status==BookingStatus.ACTIVE&&it.endTime>System.currentTimeMillis()) xor history
  }
  if(rows.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
   Text(if(history)"No booking history yet" else "No active bookings")
  }
  else LazyColumn {
   items(rows,key= {
    it.id
   }
   ) {
     b->
    val seat=s.allSeats.firstOrNull {
     it.id==b.seatId
    }
    Card(onClick= {
     open(b)
    }
     ,modifier=Modifier.fillMaxWidth().padding(8.dp)) {
     Column(Modifier.padding(14.dp)) {
      Text("${s.libraries.firstOrNull{it.id==seat?.libraryId}?.name?:"Study library"} · ${seat?.seatNumber?:"Seat"}",style=MaterialTheme.typography.titleMedium)
      Text("${DateFormat.getDateInstance().format(Date(b.startTime))} · ${DateFormat.getTimeInstance().format(Date(b.startTime))} – ${DateFormat.getTimeInstance().format(Date(b.endTime))}")
      Text(b.status.name)
      if(b.status==BookingStatus.ACTIVE&&b.endTime>System.currentTimeMillis())TextButton( {
       pending=b
      }
      ) {
       Text("Cancel booking")
      }
     }
    }
   }
  }
 }
 pending?.let {
   b->
  AlertDialog(
   onDismissRequest= {
    pending=null
   }
   ,
   title= {
    Text("Cancel booking?")
   }
   ,
   text= {
    Text("This keeps the booking in your history.")
   }
   ,
   confirmButton= {
    TextButton(onClick= {
     cancel(b)
     pending=null
    }
    ) {
     Text("Cancel booking")
    }
   }
   ,
   dismissButton= {
    TextButton(onClick= {
     pending=null
    }
    ) {
     Text("Keep")
    }
   }
  )
 }
}

@Composable fun BookingDetailsScreen(s:StudySpotUiState,id:Long?,back:()->Unit,cancel:(BookingEntity)->Unit) {
 val booking=s.bookings.firstOrNull {
  it.id==id
 }
 var confirm by remember {
  mutableStateOf(false)
 }
 Column(Modifier.fillMaxSize()) {
  Header("Booking Details",back=back)
  if(booking==null) {
   Text("Booking not found",Modifier.padding(24.dp))
   return@Column
  }
  val seat=s.allSeats.firstOrNull {
   it.id==booking.seatId
  }
  val library=s.libraries.firstOrNull {
   it.id==seat?.libraryId
  }
  Card(Modifier.fillMaxWidth().padding(20.dp),shape=RoundedCornerShape(22.dp)) {
   Column(Modifier.padding(22.dp)) {
    Text("SEAT",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
    Text(seat?.seatNumber?:"—",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.Bold)
    Text(library?.name?:"Study library",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=8.dp))
    Text("${library?.sectionName?:seat?.sectionName.orEmpty()} · ${library?.address.orEmpty()}",color=MaterialTheme.colorScheme.onSurfaceVariant)
    HorizontalDivider(Modifier.padding(vertical=16.dp))
    Text("${DateFormat.getDateInstance(DateFormat.FULL).format(Date(booking.startTime))}",style=MaterialTheme.typography.titleMedium)
    Text("${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(booking.startTime))} – ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(booking.endTime))}",modifier=Modifier.padding(top=4.dp))
    Text("${booking.status.name.lowercase().replaceFirstChar{it.uppercase()}}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,modifier=Modifier.padding(top=14.dp))
   }
  }
  if(booking.status==BookingStatus.ACTIVE&&booking.endTime>System.currentTimeMillis())OutlinedButton(onClick= {
   confirm=true
  }
   ,Modifier.fillMaxWidth().padding(horizontal=20.dp)) {
   Text("Cancel booking")
  }
 }
 if(confirm)AlertDialog(onDismissRequest= {
  confirm=false
 }
  ,title= {
   Text("Cancel booking?")
  }
  ,text= {
   Text("This booking will remain in your history.")
  }
  ,confirmButton= {
   TextButton(onClick= {
    cancel(booking!!)
    confirm=false
   }
   ) {
    Text("Cancel booking")
   }
  }
  ,dismissButton= {
   TextButton(onClick= {
    confirm=false
   }
   ) {
    Text("Keep booking")
   }
  }
 )
}

