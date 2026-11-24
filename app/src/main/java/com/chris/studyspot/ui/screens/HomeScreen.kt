package com.chris.studyspot.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chris.studyspot.data.model.*
import com.chris.studyspot.viewmodel.StudySpotUiState

@Composable fun HomeScreen(
 state: StudySpotUiState,
 onExplore: () -> Unit,
 onBookings: () -> Unit,
 onProfile: () -> Unit,
 onOpen: (LibraryEntity) -> Unit,
) {
 val start = System.currentTimeMillis()
 val end = start + 3_600_000L
 val available = state.allSeats.count { seat ->
  !seat.isBlocked && state.all.none { booking ->
   booking.seatId == seat.id && booking.status != BookingStatus.CANCELLED &&
       BookingRules.overlaps(booking.startTime, booking.endTime, start, end)
  }
 }
 Column(Modifier.fillMaxSize()) {
  Header("StudySpot", "Hello, ${state.user?.name?.substringBefore(' ') ?: "there"}", action = onProfile)
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
   Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
    Column(Modifier.fillMaxWidth().padding(22.dp)) {
     Icon(Icons.Outlined.EventSeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
     Text("A better place to focus", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
     Text("Find an open seat and reserve your study time.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
     Button(onClick = onExplore, modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(50.dp), shape = RoundedCornerShape(14.dp)) {
      Text("Explore libraries")
      Spacer(Modifier.width(8.dp))
      Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
     }
    }
   }
   Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    MetricCard("Free right now", available.toString(), Icons.Outlined.EventAvailable, Modifier.weight(1f))
    MetricCard("Study seats", state.allSeats.size.toString(), Icons.Outlined.EventSeat, Modifier.weight(1f))
   }
   Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("Libraries", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    TextButton(onClick = onExplore) { Text("See all") }
   }
   if (state.libraries.isEmpty()) {
    Text("Loading your local library list…", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 18.dp))
   } else {
    state.libraries.forEach { library ->
     LibCard(library) { onOpen(library) }
     Spacer(Modifier.height(10.dp))
    }
   }
   val currentBooking = state.bookings.firstOrNull { it.status == BookingStatus.ACTIVE && it.endTime > start }
   if (currentBooking != null) {
    OutlinedButton(onClick = onBookings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
     Icon(Icons.AutoMirrored.Outlined.EventNote, contentDescription = null)
     Spacer(Modifier.width(8.dp))
     Text("You have an active booking · View bookings")
    }
   }
   Text("Local demo availability is saved on this device only.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
  }
 }
}

