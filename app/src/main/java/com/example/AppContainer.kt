package com.example

import android.content.Context
import com.example.data.repository.TournamentRepositoryImpl
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

class AppContainer(private val context: Context) {
    val supabaseClient: SupabaseClient by lazy {
        val url = "https://fjsrkftoqjevvmqfubwg.supabase.co"
        val key = "sb_publishable_AKH7akEgiiZPd9LNux2z6g_W4IgfR72"
        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            defaultSerializer = KotlinXSerializer(Json { 
                ignoreUnknownKeys = true 
            })
            install(Postgrest)
            install(Auth)
            install(Realtime)
        }
    }

    val tournamentRepository: TournamentRepositoryImpl by lazy {
        TournamentRepositoryImpl(supabaseClient)
    }
}
