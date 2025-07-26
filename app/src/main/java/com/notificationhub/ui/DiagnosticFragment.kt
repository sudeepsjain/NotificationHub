package com.notificationhub.ui

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class DiagnosticFragment : Fragment() {

    private lateinit var resultText: TextView
    private val results = mutableListOf<String>()

    companion object {
        private const val TAG = "Diagnostic"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val title = TextView(requireContext()).apply {
            text = "App Installation Diagnostic"
            textSize = 20f
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)

        val runButton = android.widget.Button(requireContext()).apply {
            text = "Run Full Diagnostic"
            setOnClickListener { runDiagnostic() }
        }
        layout.addView(runButton)

        val settingsButton = android.widget.Button(requireContext()).apply {
            text = "Open Android App Settings"
            setOnClickListener { openAndroidAppSettings() }
        }
        layout.addView(settingsButton)

        val playStoreButton = android.widget.Button(requireContext()).apply {
            text = "Open Play Store"
            setOnClickListener { openPlayStore() }
        }
        layout.addView(playStoreButton)

        resultText = TextView(requireContext()).apply {
            textSize = 12f
            setPadding(0, 16, 0, 0)
        }
        layout.addView(resultText)

        return layout
    }

    private fun runDiagnostic() {
        results.clear()
        addResult("=== STARTING COMPREHENSIVE DIAGNOSTIC ===")

        try {
            checkDeviceInfo()
            checkAppInstallationMethods()
            checkSpecificApps()
            checkAppSources()
            checkPermissions()
            checkUserProfiles()

            displayResults()

        } catch (e: Exception) {
            addResult("ERROR: ${e.message}")
            Log.e(TAG, "Diagnostic error", e)
        }
    }

    private fun checkDeviceInfo() {
        addResult("\n--- DEVICE INFO ---")
        addResult("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        addResult("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        addResult("Build Type: ${Build.TYPE}")
        addResult("Hardware: ${Build.HARDWARE}")
        addResult("Product: ${Build.PRODUCT}")

        // Check if this is an emulator
        val isEmulator = Build.FINGERPRINT.contains("generic") ||
                Build.FINGERPRINT.contains("emulator") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("emulator")

        addResult("Is Emulator: $isEmulator")
    }

    private fun checkAppInstallationMethods() {
        addResult("\n--- APP INSTALLATION METHODS ---")

        val packageManager = requireContext().packageManager

        // Method 1: getInstalledApplications with different flags
        val flags = listOf(
            0,
            PackageManager.GET_META_DATA,
            PackageManager.GET_UNINSTALLED_PACKAGES,
            PackageManager.MATCH_DISABLED_COMPONENTS,
            PackageManager.MATCH_ALL
        )

        flags.forEach { flag ->
            try {
                val apps = packageManager.getInstalledApplications(flag)
                val userApps = apps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                addResult("Flag $flag: ${apps.size} total, ${userApps.size} user apps")
            } catch (e: Exception) {
                addResult("Flag $flag: ERROR - ${e.message}")
            }
        }

        // Method 2: getInstalledPackages
        try {
            val packages = packageManager.getInstalledPackages(0)
            val userPackages = packages.filter {
                it.applicationInfo?.let { app ->
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                } ?: false
            }
            addResult("getInstalledPackages: ${packages.size} total, ${userPackages.size} user")
        } catch (e: Exception) {
            addResult("getInstalledPackages: ERROR - ${e.message}")
        }
    }

    private fun checkSpecificApps() {
        addResult("\n--- SPECIFIC APP CHECKS ---")

        val packageManager = requireContext().packageManager

        val commonApps = mapOf(
            "Chrome" to "com.android.chrome",
            "Gmail" to "com.google.android.gm",
            "WhatsApp" to "com.whatsapp",
            "Instagram" to "com.instagram.android",
            "Play Store" to "com.android.vending",
            "YouTube" to "com.google.android.youtube",
            "Maps" to "com.google.android.apps.maps",
            "Photos" to "com.google.android.apps.photos"
        )

        commonApps.forEach { (name, packageName) ->
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isEnabled = appInfo.enabled
                addResult("✅ $name: $appName (System: $isSystem, Enabled: $isEnabled)")
            } catch (e: PackageManager.NameNotFoundException) {
                addResult("❌ $name ($packageName): NOT INSTALLED")
            } catch (e: Exception) {
                addResult("❌ $name ($packageName): ERROR - ${e.message}")
            }
        }
    }

    private fun checkAppSources() {
        addResult("\n--- APP INSTALLATION SOURCES ---")

        val packageManager = requireContext().packageManager

        try {
            // Check if Play Store is available
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://"))
            val playStoreAvailable = playStoreIntent.resolveActivity(packageManager) != null
            addResult("Play Store available: $playStoreAvailable")

            // Check unknown sources
            val unknownSources = Settings.Secure.getInt(
                requireContext().contentResolver,
                Settings.Secure.INSTALL_NON_MARKET_APPS, 0
            ) == 1
            addResult("Unknown sources enabled: $unknownSources")

        } catch (e: Exception) {
            addResult("App sources check error: ${e.message}")
        }
    }

    private fun checkPermissions() {
        addResult("\n--- PERMISSIONS ---")

        val context = requireContext()

        // Check if we have permission to query packages
        try {
            val hasQueryPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ query permission
                context.checkSelfPermission("android.permission.QUERY_ALL_PACKAGES") ==
                        PackageManager.PERMISSION_GRANTED
            } else {
                true // Not needed before Android 11
            }
            addResult("Query all packages permission: $hasQueryPermission")
        } catch (e: Exception) {
            addResult("Permission check error: ${e.message}")
        }
    }

    private fun checkUserProfiles() {
        addResult("\n--- USER PROFILES ---")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val userManager = requireContext().getSystemService(android.content.Context.USER_SERVICE)
                        as android.os.UserManager

                val userProfiles = userManager.userProfiles
                addResult("User profiles: ${userProfiles.size}")

                userProfiles.forEachIndexed { index, userHandle ->
                    addResult("Profile $index: $userHandle")
                }
            }
        } catch (e: Exception) {
            addResult("User profiles check error: ${e.message}")
        }
    }

    private fun openAndroidAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            addResult("Cannot open Android app settings: ${e.message}")
        }
    }

    private fun openPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://"))
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store"))
                startActivity(intent)
            } catch (e2: Exception) {
                addResult("Cannot open Play Store: ${e2.message}")
            }
        }
    }

    private fun addResult(message: String) {
        results.add(message)
        Log.d(TAG, message)
    }

    private fun displayResults() {
        val fullText = results.joinToString("\n")
        resultText.text = fullText
    }
}