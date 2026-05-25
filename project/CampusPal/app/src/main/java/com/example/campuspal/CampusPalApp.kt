package com.example.campuspal

import android.app.Application
import com.example.campuspal.data.db.AppDatabase
import com.example.campuspal.data.datastore.SettingsDataStore

class CampusPalApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(this) }

    override fun onCreate() {
        super.onCreate()
    }
}
