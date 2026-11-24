package com.example.studyspot.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.example.studyspot.ui.screens.*
import com.example.studyspot.viewmodel.StudySpotViewModel

@Composable fun StudySpotApp(vm:StudySpotViewModel) {
 val s by vm.state.collectAsStateWithLifecycle()
 val nav= rememberNavController()
 val backStackEntry by nav.currentBackStackEntryAsState()
 val currentRoute = backStackEntry?.destination?.route
 val mainRoutes = setOf("home", "libraries", "bookings", "profile")
 fun navigateToTab(route: String) {
  if (currentRoute != route) nav.navigate(route) {
   popUpTo("home") { saveState = true }
   launchSingleTop = true
   restoreState = true
  }
 }
 val snack= remember {
  SnackbarHostState()
 }
 LaunchedEffect(vm) {
  vm.events.collect {
   snack.showSnackbar(it)
  }
 }
 LaunchedEffect(s.user) {
  val destination=if(s.user==null)"login" else "home"
  if(nav.currentDestination?.route!=destination)nav.navigate(destination) {
   launchSingleTop=true
   popUpTo(if(destination=="home")"login" else "home") {
    inclusive=true
   }
  }
 }
 Scaffold(
 snackbarHost = { SnackbarHost(snack) },
 bottomBar = {
  if (s.user != null && currentRoute in mainRoutes) {
   Bottom(
    selectedRoute = currentRoute.orEmpty(),
    onHome = { navigateToTab("home") },
    onExplore = { navigateToTab("libraries") },
    onBookings = { navigateToTab("bookings") },
    onProfile = { navigateToTab("profile") },
   )
  }
 }
 ) {
   p->NavHost(nav,"login",Modifier.padding(p)) {
  composable("login") {
   Auth(false, {
     _,e,pw->vm.login(e,pw) {
   }
   }
    , {
     nav.navigate("register")
    }
   )
  }
  composable("register") {
   Auth(true, {
     n,e,pw->vm.register(n,e,pw) {
   }
   }
    , {
     nav.popBackStack()
    }
   )
  }
  composable("home") {
   HomeScreen(
    state = s,
    onExplore = { nav.navigate("libraries") },
    onBookings = { nav.navigate("bookings") },
    onProfile = { nav.navigate("profile") },
    onOpen = { library -> vm.select(library); nav.navigate("seats") },
   )
  }
  composable("libraries") {
   var search by remember { mutableStateOf("") }
   Column(Modifier.fillMaxSize()) {
    Header("Explore Libraries","Choose a study space", action = { nav.navigate("map") }, actionIcon = Icons.Outlined.Map)
    OutlinedTextField(
     value = search,
     onValueChange = { search = it },
     modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
     placeholder = { Text("Search libraries") },
     leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
     trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Outlined.Close, contentDescription = "Clear search") } },
     singleLine = true,
     shape = RoundedCornerShape(14.dp),
    )
    val matches = s.libraries.filter { it.name.contains(search, ignoreCase = true) || it.address.contains(search, ignoreCase = true) }
    if (matches.isEmpty()) {
     Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(if (search.isBlank()) "Loading libraries…" else "No libraries match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    } else LazyColumn(
     modifier = Modifier.weight(1f).fillMaxWidth(),
     contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
     verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
     items(matches, key = { it.id }) { library ->
      LibCard(library) { vm.select(library); nav.navigate("seats") }
     }
    }
   }
  }
  composable("map") {
   MapScreen(s.libraries, {
    nav.popBackStack()
   }
    , {
     vm.select(it)
     nav.navigate("seats")
    }
   )
  }
  composable("seats") {
   SeatScreen(s, {
    nav.popBackStack()
   }
    ,vm::book)
  }
  composable("bookings") {
   BookingScreen(s, {
    nav.popBackStack()
   }
    ,vm::cancel, {
     nav.navigate("booking/${it.id}")
    }
   )
  }
  composable("booking/{id}") {
    entry->val id=entry.arguments?.getString("id")?.toLongOrNull()
   BookingDetailsScreen(s,id, {
    nav.popBackStack()
   }
    ,vm::cancel)
  }
  composable("profile") {
   val uriHandler = LocalUriHandler.current
   Column(Modifier.fillMaxSize()) {
    Header("Profile", "Your StudySpot account", back = { nav.popBackStack() })
    Column(
     Modifier.fillMaxWidth().weight(1f).padding(horizontal = 20.dp, vertical = 24.dp),
     verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
     Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
      Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
       Surface(
        modifier = Modifier.size(60.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
       ) {
        Box(contentAlignment = Alignment.Center) {
         Text(
          s.user?.name?.split(" ")?.mapNotNull { it.firstOrNull()?.uppercaseChar() }?.take(2)?.joinToString("") ?: "?",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontWeight = FontWeight.Bold,
         )
        }
       }
       Column(Modifier.weight(1f).padding(start = 16.dp)) {
        Text(s.user?.name.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(s.user?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
       }
      }
     }
     OutlinedButton(
      onClick = { uriHandler.openUri("https://github.com/chris-h-edmond/StudySpot") },
      modifier = Modifier.fillMaxWidth().height(52.dp),
      shape = RoundedCornerShape(14.dp),
     ) {
      Icon(painterResource(com.example.studyspot.R.drawable.ic_github), contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(Modifier.width(10.dp))
      Text("View project on GitHub")
     }
     Button(
      onClick = { vm.logout() },
      modifier = Modifier.fillMaxWidth().height(52.dp),
      shape = RoundedCornerShape(14.dp),
     ) {
      Text("Log out")
     }
    }
   }
  }
 }
 }
}

