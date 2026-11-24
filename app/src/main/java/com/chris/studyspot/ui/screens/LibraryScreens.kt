package com.chris.studyspot.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.chris.studyspot.data.model.*

@Composable fun MapScreen(libraries:List<LibraryEntity>,back:()->Unit,open:(LibraryEntity)->Unit) {
 val context=LocalContext.current
 val key=runCatching {
  context.packageManager.getApplicationInfo(context.packageName,android.content.pm.PackageManager.GET_META_DATA).metaData?.getString("com.google.android.geo.API_KEY")
 }
  .getOrNull()
 var selected by remember {
  mutableStateOf<LibraryEntity?>(null)
 }
 Column(Modifier.fillMaxSize()) {
  Header("Nearby Libraries","Map and manual browsing",back)
  if(!key.isNullOrBlank()&&libraries.any {
    it.latitude!=null&&it.longitude!=null
   }
  ) {
   val first=libraries.first {
    it.latitude!=null&&it.longitude!=null
   }
   val camera= rememberCameraPositionState {
    position=com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(LatLng(first.latitude!!,first.longitude!!),15f)
   }
   GoogleMap(Modifier.fillMaxWidth().weight(1f),cameraPositionState=camera) {
    libraries.filter {
     it.latitude!=null&&it.longitude!=null
    }
     .forEach {
       library->Marker(state=MarkerState(position=LatLng(library.latitude!!,library.longitude!!)),title=library.name,snippet=library.address,onClick= {
      selected=library
      false
     }
     )
     }
   }
   (selected?:first).let {
     library->Card(Modifier.fillMaxWidth().padding(12.dp)) {
    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
     Column(Modifier.weight(1f)) {
      Text(library.name,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
      Text(library.address,style=MaterialTheme.typography.bodySmall)
      Text("${library.totalSeats} seats · local demo availability",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
     }
     Button(onClick= {
      open(library)
     }
     ) {
      Text("View seats")
     }
    }
   }
   }
  }
  else {
   Card(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=10.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
    Column(Modifier.padding(18.dp)) {
     Text("Map setup not configured",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
     Text("Add MAPS_API_KEY to local.properties to show the map. You can browse libraries without it.",modifier=Modifier.padding(top=6.dp))
    }
   }
   LazyColumn {
    items(libraries,key= {
     it.id
    }
    ) {
     LibCard(it) {
      open(it)
     }
    }
   }
  }
 }
}

