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

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                uiState = uiState,
                onSettingsClick = { tournamentId ->
                    navController.navigate("settings/$tournamentId")
                },
                onVerifyClick = {
                    navController.navigate("verification")
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
