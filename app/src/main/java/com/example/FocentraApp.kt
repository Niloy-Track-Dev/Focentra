package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.repository.StudyRepository
import com.example.engine.TimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FocentraApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { StudyRepository(database) }
    val timerEngine by lazy { TimerEngine.getInstance(this, repository) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            // Check and update achievements on app start
            repository.checkAndUnlockAchievements()
        }
    }
}
