package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.TournamentRepositoryImpl
import com.example.ui.viewmodel.TournamentDashboardViewModel
import com.example.ui.viewmodel.DashboardState

@Composable
fun TournamentApp(tournamentRepository: TournamentRepositoryImpl) {
    val navController = rememberNavController()
    
    val viewModel: TournamentDashboardViewModel = viewModel(
        factory = TournamentDashboardViewModel.provideFactory(tournamentRepository)
    )
    
    val uiState by viewModel.uiState.collectAsState()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                supabaseClient = tournamentRepository.supabase,
                onLoginSuccess = {
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
                    if (route != "dashboard") navController.navigate(route)
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
