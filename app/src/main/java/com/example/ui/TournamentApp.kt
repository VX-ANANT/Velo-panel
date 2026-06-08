package com.example.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.TournamentRepositoryImpl
import com.example.ui.theme.VelorixAccent
import com.example.ui.theme.VelorixBg
import com.example.ui.viewmodel.TournamentDashboardViewModel
import com.example.ui.viewmodel.DashboardState
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay

@Composable
fun TournamentApp(tournamentRepository: TournamentRepositoryImpl) {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val viewModel: TournamentDashboardViewModel = viewModel(
        factory = TournamentDashboardViewModel.provideFactory(tournamentRepository)
    )
    
    val uiState by viewModel.uiState.collectAsState()

    // Setup SharedPreferences
    val sharedPrefs = remember { context.getSharedPreferences("VelorixPrefs", Context.MODE_PRIVATE) }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            LaunchedEffect(Unit) {
                delay(2000L) // 2-second splash
                val savedEmail = sharedPrefs.getString("user_email", null)
                val sessionUser = tournamentRepository.supabase.auth.currentSessionOrNull()?.user?.email
                
                if (savedEmail != null) {
                    viewModel.setManualLoginEmail(savedEmail)
                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else if (sessionUser != null) {
                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VelorixBg),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "VELORIX",
                        color = VelorixAccent,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TOURNAMENT OVERSIGHT",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
        composable("login") {
            LoginScreen(
                supabaseClient = tournamentRepository.supabase,
                onLoginSuccess = { userEmail ->
                    if (userEmail != null) {
                        viewModel.setManualLoginEmail(userEmail)
                        sharedPrefs.edit().putString("user_email", userEmail).apply()
                    }
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                uiState = uiState,
                onSettingsClick = { tournamentId ->
                    navController.navigate("settings/$tournamentId")
                },
                onCreateTournamentClick = {
                    navController.navigate("create_tournament")
                },
                onVerifyClick = {
                    navController.navigate("verification")
                },
                onBracketClick = { tournamentId ->
                    navController.navigate("bracket/$tournamentId")
                },
                onNavClick = { route ->
                    if (route == "logout") {
                        sharedPrefs.edit().remove("user_email").apply()
                        /* Supabase sign out if needed */
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else if (route != "dashboard") {
                        navController.navigate(route)
                    }
                }
            )
        }
        composable("verification") {
            val verificationViewModel: com.example.ui.viewmodel.PlayerVerificationViewModel = viewModel(
                factory = com.example.ui.viewmodel.PlayerVerificationViewModel.provideFactory(tournamentRepository)
            )
            PlayerVerificationScreen(
                viewModel = verificationViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("bracket/{tournamentId}") { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId")
            if (tournamentId != null) {
                val bracketViewModel: com.example.ui.viewmodel.BracketViewModel = viewModel(
                    factory = com.example.ui.viewmodel.BracketViewModel.provideFactory(tournamentRepository)
                )
                BracketManagementScreen(
                    tournamentId = tournamentId,
                    viewModel = bracketViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable("tournaments_list") {
            TournamentsListScreen(uiState = uiState, onNavigateBack = { navController.popBackStack() })
        }
        composable("analytics") {
            AnalyticsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("global_settings") {
            GlobalSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("player_profiles") {
            PlayerProfilesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("create_tournament") {
            com.example.ui.CreateTournamentScreen(
                onNavigateBack = { navController.popBackStack() },
                onCreate = { newTournament ->
                    viewModel.createTournament(newTournament)
                    navController.popBackStack()
                }
            )
        }
        composable("staff_management") {
            StaffManagementScreen(
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings/{tournamentId}") { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getString("tournamentId")
            val tournament = (uiState as? DashboardState.Success)?.tournaments?.find { it.id == tournamentId }
            
            if (tournament != null) {
                TournamentSettingsScreen(
                    tournament = tournament,
                    onNavigateBack = { navController.popBackStack() },
                    onSave = { updated ->
                        viewModel.updateTournament(updated)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
