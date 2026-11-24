package com.example.studyspot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.studyspot.app.StudySpotApp
import com.example.studyspot.data.session.SessionManager
import com.example.studyspot.data.local.StudySpotDatabase
import com.example.studyspot.data.repository.StudySpotRepository
import com.example.studyspot.ui.theme.StudySpotTheme
import com.example.studyspot.viewmodel.StudySpotViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = StudySpotDatabase.get(applicationContext)
        val session = SessionManager(applicationContext)
        val repository = StudySpotRepository(database, session)
        val viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                StudySpotViewModel(repository, session) as T
        })[StudySpotViewModel::class.java]
        setContent { StudySpotTheme(dynamicColor = false, darkTheme = true) { StudySpotApp(viewModel) } }
    }
}
