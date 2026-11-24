package com.example.studyspot.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.studyspot.data.model.*
import com.example.studyspot.viewmodel.StudySpotUiState
import com.example.studyspot.assistant.SeatAssistantSheet
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SeatScreen(s:StudySpotUiState,back:()->Unit,book:(SeatEntity,Int)->Unit) {
 val library=s.library
 var chosen by remember(library?.id) {
  mutableStateOf<SeatEntity?>(null)
 }
 var sheet by remember {
  mutableStateOf(false)
 }
 var hours by remember {
  mutableIntStateOf(1)
 }
 var assistantOpen by remember { mutableStateOf(false) }
 val now=System.currentTimeMillis()
 val occupied=s.all.filter {
  it.status==BookingStatus.ACTIVE&&it.startTime<=now+60_000L&&it.endTime>now
 }
  .map {
   it.seatId
  }
  .toSet()
 Column(Modifier.fillMaxSize()) {
  Header(library?.name?:"Seat map",library?.sectionName?:"",back)
  if(library==null) {
   Text("Library not found",Modifier.padding(24.dp))
   return@Column
  }
   Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("Choose a seat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    OutlinedButton(onClick = { assistantOpen = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
     Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
     Spacer(Modifier.width(8.dp))
     Text("Seat Guide")
    }
   }
   Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
    SeatLegend("Free", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outline)
    SeatLegend("Booked", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline)
    SeatLegend("Selected", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)
    SeatLegend("Blocked", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
  }
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
     Column(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
      Text("${library.sectionName.uppercase()} · FLOOR PLAN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
     s.seats.groupBy {
      it.rowLabel
     }
      .forEach {
        (row,seats)->
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center,modifier=Modifier.padding(vertical=5.dp)) {
         Text(row,Modifier.width(20.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        seats.take(4).forEach {
          seat->Seat(seat,seat.id in occupied,chosen?.id==seat.id) {
         if(chosen?.id==seat.id)chosen=null else if(!seat.isBlocked&&seat.id !in occupied)chosen=seat
        }
        }
         Text("AISLE",style=MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 4.dp))
        seats.drop(4).forEach {
          seat->Seat(seat,seat.id in occupied,chosen?.id==seat.id) {
         if(chosen?.id==seat.id)chosen=null else if(!seat.isBlocked&&seat.id !in occupied)chosen=seat
        }
        }
       }
      }
    }
   }
    Text("Availability is stored on this device and isn’t shared with other students.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=14.dp))
  }
  if(chosen!=null)Button( {
   sheet=true
  }
    ,Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).height(52.dp), shape = RoundedCornerShape(14.dp)) {
    Icon(Icons.Outlined.EventSeat, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("Reserve ${chosen?.seatNumber}")
  }
 }
 if(sheet&&chosen!=null)ModalBottomSheet(onDismissRequest= {
  sheet=false
 }
 ) {
   Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
    Text("Confirm your seat",style=MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("${library?.name} · ${chosen?.seatNumber}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    Text("Starts now · ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    Text("Choose duration", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    listOf(1,2,3).forEach {
      h->FilterChip(hours==h, {
     hours=h
    }
     , {
      Text("$h h")
     }
    )
    }
   }
    Text("Ends at ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(now+hours*3600000L))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
   Button( {
    chosen?.let {
     book(it,hours)
    }
    chosen=null
    sheet=false
   }
     ,Modifier.fillMaxWidth().padding(top = 14.dp).height(52.dp), shape = RoundedCornerShape(14.dp)) {
    Text("Confirm booking")
   }
   TextButton( {
    sheet=false
   }
   ) {
    Text("Close")
   }
  }
 }
 if (assistantOpen && library != null) SeatAssistantSheet(library, s.seats, s.all, onDismiss = { assistantOpen = false })
}

