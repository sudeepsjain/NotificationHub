// MainActivity.kt - Fixed version with safe WorkManager usage
package com.notificationhub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.notificationhub.databinding.ActivityMainBinding
import com.notificationhub.service.ReAlertService
import com.notificationhub.ui.AppManagementFragment
import com.notificationhub.ui.HomeFragment
import com.notificationhub.ui.NotificationLogFragment
import com.notificationhub.ui.SettingsFragment
import com.notificationhub.utils.NotificationCleanupWorker
import com.notificationhub.utils.NotificationHelper
import com.notificationhub.utils.PermissionHelper

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private val preferenceManager by lazy { SmartNotifyApplication.instance.preferenceManager }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if this is the first launch
        if (preferenceManager.isFirstLaunch) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupBottomNavigation()
            setupInitialFragment()
            initializeServices()
            NotificationHelper.createNotificationChannels(this)

            Log.d(TAG, "MainActivity created successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            // Don't crash the app, show error fragment or finish activity
            finish()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnNavigationItemSelectedListener(this)
    }

    private fun setupInitialFragment() {
        if (supportFragmentManager.fragments.isEmpty()) {
            loadFragment(HomeFragment())
        }
    }

    private fun initializeServices() {
        try {
            // Only start services if permissions are granted
            if (PermissionHelper.isNotificationListenerEnabled(this)) {
                startReAlertService()
            }

            // Schedule cleanup work safely
            scheduleCleanupSafely()

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing services", e)
            // Don't crash - services can be initialized later
        }
    }

    private fun scheduleCleanupSafely() {
        try {
            // Check if WorkManager is initialized before using it
            NotificationCleanupWorker.scheduleCleanup(this)
            Log.d(TAG, "Cleanup worker scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule cleanup worker", e)
            // App can still function without the cleanup worker
        }
    }

    private fun startReAlertService() {
        try {
            val reAlertIntent = Intent(this, ReAlertService::class.java)
            startForegroundService(reAlertIntent)
            Log.d(TAG, "ReAlertService started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ReAlertService", e)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val fragment = when (item.itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_log -> NotificationLogFragment()
            R.id.nav_apps -> AppManagementFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> return false
        }
        return loadFragment(fragment)
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        return try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error loading fragment", e)
            false
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            if (PermissionHelper.isNotificationListenerEnabled(this)) {
                startReAlertService()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume", e)
        }
    }
}