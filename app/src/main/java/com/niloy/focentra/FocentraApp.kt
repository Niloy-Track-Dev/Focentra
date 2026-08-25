package com.niloy.focentra

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.niloy.focentra.data.local.AppDatabase
import com.niloy.focentra.data.repository.StudyRepository
import com.niloy.focentra.engine.TimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FocentraApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val prefs: SharedPreferences by lazy {
        getSharedPreferences("focentra_prefs", Context.MODE_PRIVATE)
    }

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { StudyRepository(database) }
    val timerEngine by lazy { TimerEngine.getInstance(this, repository) }

    fun getInitialTheme(): String {
        return prefs.getString("saved_theme", "midnight") ?: "midnight"
    }

    fun saveCachedTheme(theme: String) {
        prefs.edit().putString("saved_theme", theme).apply()
    }

    fun getInitialLanguage(): String {
        return prefs.getString("saved_language", "en") ?: "en"
    }

    fun saveCachedLanguage(language: String) {
        prefs.edit().putString("saved_language", language).apply()
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            // Check and update achievements on app start
            repository.checkAndUnlockAchievements()
        }
    }
}

