package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private var incomingEmailLink by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.example.ui.theme.ThemeManager.init(this)
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    )

    incomingEmailLink = intent?.data?.toString()

    setContent {
      MyApplicationTheme {
        val appContainer = (application as VelorixApplication).appContainer
        com.example.ui.TournamentApp(
            tournamentRepository = appContainer.tournamentRepository,
            incomingEmailLink = incomingEmailLink,
            onEmailLinkHandled = { incomingEmailLink = null }
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    incomingEmailLink = intent.data?.toString()
  }
}
