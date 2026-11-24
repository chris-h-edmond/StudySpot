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

@Composable fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
 Card(modifier, shape = RoundedCornerShape(18.dp)) {
  Column(Modifier.fillMaxWidth().padding(16.dp)) {
   Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
   Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
   Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

@Composable fun Header(title: String, sub: String = "", back: (() -> Unit)? = null, action: (() -> Unit)? = null, actionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.AccountCircle) {
 Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
  if (back != null) IconButton(onClick = back, modifier = Modifier.padding(end = 8.dp)) {
   Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Go back")
  }
  Column(Modifier.weight(1f)) {
   Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
   if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
  if (action != null) IconButton(onClick = action) { Icon(actionIcon, contentDescription = if (actionIcon == Icons.Outlined.Map) "Open map" else "Open profile") }
 }
}

@Composable fun LibCard(l: LibraryEntity, open: () -> Unit) {
 Card(onClick = open, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
  Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
   Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
    Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
   }
   Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
    Text(l.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(l.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
    Text("${l.totalSeats} study seats · local availability", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 7.dp))
   }
   Icon(Icons.Outlined.ChevronRight, contentDescription = "View ${l.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

@Composable fun Bottom(
 selectedRoute: String,
 onHome: () -> Unit,
 onExplore: () -> Unit,
 onBookings: () -> Unit,
 onProfile: () -> Unit,
) {
 NavigationBar {
  NavigationBarItem(selected = selectedRoute == "home", onClick = onHome, icon = { Icon(Icons.Outlined.Home, contentDescription = null) }, label = { Text("Home") })
  NavigationBarItem(selected = selectedRoute == "libraries", onClick = onExplore, icon = { Icon(Icons.Outlined.Explore, contentDescription = null) }, label = { Text("Explore") })
  NavigationBarItem(selected = selectedRoute == "bookings", onClick = onBookings, icon = { Icon(Icons.AutoMirrored.Outlined.EventNote, contentDescription = null) }, label = { Text("Bookings") })
  NavigationBarItem(selected = selectedRoute == "profile", onClick = onProfile, icon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) }, label = { Text("Profile") })
 }
}
@Composable fun SeatLegend(label: String, fill: Color, border: Color) {
 Row(verticalAlignment = Alignment.CenterVertically) {
  Surface(Modifier.size(13.dp), shape = RoundedCornerShape(4.dp), color = fill, border = BorderStroke(1.dp, border)) {}
  Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 5.dp), maxLines = 1)
 }
}

@Composable fun Seat(seat:SeatEntity,occupied:Boolean,selected:Boolean,click:()->Unit) {
 val color=when {
  selected->MaterialTheme.colorScheme.primary
  seat.isBlocked->MaterialTheme.colorScheme.errorContainer
  occupied->MaterialTheme.colorScheme.surfaceVariant
  else->MaterialTheme.colorScheme.surface
 }
 val ink=when {
  selected -> MaterialTheme.colorScheme.onPrimary
  seat.isBlocked -> MaterialTheme.colorScheme.onErrorContainer
  occupied -> MaterialTheme.colorScheme.onSurfaceVariant
  else -> MaterialTheme.colorScheme.onSurface
 }
 Surface(onClick=click,enabled=!occupied&&!seat.isBlocked,modifier=Modifier.padding(horizontal=2.dp).size(36.dp,46.dp),shape=RoundedCornerShape(9.dp),color=color,border=BorderStroke(if(selected)2.dp else 1.dp,if(seat.isBlocked)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)) {
  Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
   Icon(if (seat.isBlocked) Icons.Outlined.Block else if (occupied) Icons.Outlined.EventBusy else Icons.Outlined.EventSeat, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
   Text(seat.seatNumber,style=MaterialTheme.typography.labelSmall,color=ink,fontWeight=FontWeight.Bold)
  }
 }
}

