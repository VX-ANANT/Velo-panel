package com.example

import android.content.Context
import com.example.data.repository.GeminiRepository
import com.example.data.repository.GeminiRepositoryImpl
import com.example.data.repository.TournamentRepositoryImpl

class AppContainer(private val context: Context) {
    val tournamentRepository: TournamentRepositoryImpl by lazy {
        TournamentRepositoryImpl(context.applicationContext)
    }

    val geminiRepository: GeminiRepository by lazy {
        GeminiRepositoryImpl()
    }
}

