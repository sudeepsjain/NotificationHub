package com.notificationhub

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.notificationhub.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWelcomeBinding
    private val preferenceManager by lazy { SmartNotifyApplication.instance.preferenceManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
    }

    private fun setupViews() {
        binding.getStartedButton.setOnClickListener {
            // Mark that the user has completed the welcome flow
            preferenceManager.isFirstLaunch = false
            
            // Navigate to the main activity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}