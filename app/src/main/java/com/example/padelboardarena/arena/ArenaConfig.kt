package com.example.padelboardarena.arena

import com.example.padelboardarena.BuildConfig

object ArenaBuildConfig {
    fun load(): ArenaConfig {
        return ArenaConfig(
            apiBaseUrl = BuildConfig.ARENA_API_BASE_URL,
            courtId = BuildConfig.ARENA_COURT_ID,
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabasePublishableKey =
                BuildConfig.SUPABASE_PUBLISHABLE_KEY,
            email = BuildConfig.ARENA_EMAIL
        )
    }
}
