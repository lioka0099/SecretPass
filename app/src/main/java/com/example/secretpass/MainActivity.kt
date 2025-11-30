package com.example.secretpass

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.secretpass.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var condition1North = false
    private var condition2Noise = false
    private var condition3Shaken = false
    private var condition4ContactExists = false
    private var condition5PasswordCorrect = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initially all indicators red, login disabled
        updateUI()

        binding.btnLogin.setOnClickListener {
            // All conditions should already be true when button is enabled,
            if (allConditionsMet()) {
                startActivity(Intent(this, SuccessActivity::class.java))
            }
        }
        binding.etPassword.addTextChangedListener {
            checkPasswordCondition()
        }
    }

    private fun checkPasswordCondition() {
        // TODO later: get battery percentage and build the correct password
        condition5PasswordCorrect = false
        updateUI()
    }

    private fun allConditionsMet(): Boolean {
        return condition1North &&
                condition2Noise &&
                condition3Shaken &&
                condition4ContactExists &&
                condition5PasswordCorrect
    }

    private fun updateUI() {
        // Update circles
        binding.indicatorCondition1.setBackgroundResource(
            if (condition1North) R.drawable.circle_green else R.drawable.circle_red
        )
        binding.indicatorCondition2.setBackgroundResource(
            if (condition2Noise) R.drawable.circle_green else R.drawable.circle_red
        )
        binding.indicatorCondition3.setBackgroundResource(
            if (condition3Shaken) R.drawable.circle_green else R.drawable.circle_red
        )
        binding.indicatorCondition4.setBackgroundResource(
            if (condition4ContactExists) R.drawable.circle_green else R.drawable.circle_red
        )
        binding.indicatorCondition5.setBackgroundResource(
            if (condition5PasswordCorrect) R.drawable.circle_green else R.drawable.circle_red
        )

        // Enable/disable login button
        binding.btnLogin.isEnabled = allConditionsMet()
    }
}