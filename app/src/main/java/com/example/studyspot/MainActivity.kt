package com.example.studyspot
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import androidx.navigation.NavHostController
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.studyspot.data.*
import com.example.studyspot.assistant.SeatAssistantSheet
import com.example.studyspot.ui.theme.StudySpotTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date


class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  enableEdgeToEdge()
  val db = StudySpotDatabase.get(applicationContext)
  val session = SessionManager(applicationContext)
  val vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
   @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = SpotVm(StudySpotRepository(db, session), session) as T
  }
  )[SpotVm::class.java]
  setContent {
   StudySpotTheme(dynamicColor = false) {
    SpotApp(vm)
   }
  }
 }
}

private data class Ui(val user: UserEntity?=null,val libraries:List<LibraryEntity> = emptyList(),val library:LibraryEntity?=null,val seats:List<SeatEntity> = emptyList(),val allSeats:List<SeatEntity> = emptyList(),val bookings:List<BookingEntity> = emptyList(),val all:List<BookingEntity> = emptyList())

private class SpotVm(private val repo:StudySpotRepository,private val session:SessionManager):ViewModel() {
 private val s=MutableStateFlow(Ui())
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

@Composable private fun SpotApp(vm:SpotVm) {
 val s by vm.state.collectAsStateWithLifecycle()
 val nav= rememberNavController()
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
 Scaffold(snackbarHost= {
  SnackbarHost(snack)
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
   Column {
    Header("Profile", s.user?.email ?: "", back = { nav.popBackStack() })
    Text("Your account, password hash, and bookings stay on this device. Seat Guide sends your message plus the selected library, requested time, and available seat labels to Groq for a reply. Don’t share personal information in chat. The assistant is only for seat planning and never makes a booking for you. Local availability isn’t shared across devices; sample coordinates are illustrative.",Modifier.padding(horizontal = 22.dp, vertical = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Button( {
     vm.logout()
    }
     ,Modifier.padding(20.dp)) {
     Text("Log out")
    }
   }
  }
 }
 }
}

@Composable private fun HomeScreen(
 state: Ui,
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
  Bottom(onExplore, onBookings, onProfile)
 }
}

@Composable private fun MetricCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
 Card(modifier, shape = RoundedCornerShape(18.dp)) {
  Column(Modifier.fillMaxWidth().padding(16.dp)) {
   Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
   Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
   Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

@Composable private fun Header(title: String, sub: String = "", back: (() -> Unit)? = null, action: (() -> Unit)? = null, actionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.AccountCircle) {
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

@Composable private fun Auth(register: Boolean, submit: (String, String, String) -> Unit, link: () -> Unit) {
 var name by remember { mutableStateOf("") }
 var email by remember { mutableStateOf("") }
 var password by remember { mutableStateOf("") }
 var confirm by remember { mutableStateOf("") }
 var visible by remember { mutableStateOf(false) }
 val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.Center) {
  Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(60.dp)) {
   Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.EventSeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)) }
  }
  Text("StudySpot", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
  Text("Find your seat. Focus on studying.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 30.dp))
  Text(if (register) "Create your account" else "Welcome back", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
  Text(if (register) "Set up your local StudySpot account." else "Sign in to see your saved study spaces.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
  if (register) {
   OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true, shape = RoundedCornerShape(14.dp))
   Spacer(Modifier.height(12.dp))
  }
  OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email address") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) }, shape = RoundedCornerShape(14.dp), isError = register && email.isNotBlank() && !emailValid)
  Spacer(Modifier.height(12.dp))
  OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text(if (register) "Password · 8+ characters" else "Password") }, singleLine = true, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) }, trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, contentDescription = if (visible) "Hide password" else "Show password") } }, shape = RoundedCornerShape(14.dp))
  if (register) {
   Spacer(Modifier.height(12.dp))
   OutlinedTextField(confirm, { confirm = it }, Modifier.fillMaxWidth(), label = { Text("Confirm password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) }, shape = RoundedCornerShape(14.dp), isError = confirm.isNotEmpty() && confirm != password)
  }
  Spacer(Modifier.height(20.dp))
  Button(onClick = { submit(if (register) name else "", email, password) }, Modifier.fillMaxWidth().height(52.dp), enabled = if (register) name.isNotBlank() && emailValid && password.length >= 8 && password == confirm else emailValid && password.isNotBlank(), shape = RoundedCornerShape(14.dp)) {
   Text(if (register) "Create account" else "Sign in")
  }
  TextButton(onClick = link, Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)) { Text(if (register) "Already have an account? Sign in" else "New to StudySpot? Create an account") }
  Text("Local demo account · data stays on this device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp))
 }
}

@Composable private fun LibCard(l: LibraryEntity, open: () -> Unit) {
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

@Composable private fun MapScreen(libraries:List<LibraryEntity>,back:()->Unit,open:(LibraryEntity)->Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)

@Composable private fun SeatScreen(s:Ui,back:()->Unit,book:(SeatEntity,Int)->Unit) {
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

@Composable private fun SeatLegend(label: String, fill: Color, border: Color) {
 Row(verticalAlignment = Alignment.CenterVertically) {
  Surface(Modifier.size(13.dp), shape = RoundedCornerShape(4.dp), color = fill, border = BorderStroke(1.dp, border)) {}
  Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 5.dp), maxLines = 1)
 }
}

@Composable private fun Seat(seat:SeatEntity,occupied:Boolean,selected:Boolean,click:()->Unit) {
 val color=when {
  selected->MaterialTheme.colorScheme.primary
  seat.isBlocked->Color(0xFFFFE1E1)
  occupied->Color.LightGray
  else->Color.White
 }
 val ink=if(selected)Color.White else Color.DarkGray
 Surface(onClick=click,enabled=!occupied&&!seat.isBlocked,modifier=Modifier.padding(horizontal=2.dp).size(36.dp,46.dp),shape=RoundedCornerShape(9.dp),color=color,border=BorderStroke(if(selected)2.dp else 1.dp,if(seat.isBlocked)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline)) {
  Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
   Icon(if (seat.isBlocked) Icons.Outlined.Block else if (occupied) Icons.Outlined.EventBusy else Icons.Outlined.EventSeat, contentDescription = null, tint = ink, modifier = Modifier.size(16.dp))
   Text(seat.seatNumber,style=MaterialTheme.typography.labelSmall,color=ink,fontWeight=FontWeight.Bold)
  }
 }
}

@Composable private fun BookingScreen(s:Ui,back:()->Unit,cancel:(BookingEntity)->Unit,open:(BookingEntity)->Unit) {
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

@Composable private fun BookingDetailsScreen(s:Ui,id:Long?,back:()->Unit,cancel:(BookingEntity)->Unit) {
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

@Composable private fun Bottom(onExplore: () -> Unit, onBookings: () -> Unit, onProfile: () -> Unit) {
 NavigationBar {
  NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Outlined.Home, contentDescription = null) }, label = { Text("Home") })
  NavigationBarItem(selected = false, onClick = onExplore, icon = { Icon(Icons.Outlined.Explore, contentDescription = null) }, label = { Text("Explore") })
  NavigationBarItem(selected = false, onClick = onBookings, icon = { Icon(Icons.AutoMirrored.Outlined.EventNote, contentDescription = null) }, label = { Text("Bookings") })
  NavigationBarItem(selected = false, onClick = onProfile, icon = { Icon(Icons.Outlined.AccountCircle, contentDescription = null) }, label = { Text("Profile") })
 }
}
