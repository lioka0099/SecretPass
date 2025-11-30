package com.example.secretpass

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.example.secretpass.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding

    //Sensors
    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null

    //Conditions
    private var condition1North = false
    private var condition2Noise = false
    private var condition3Shaken = false
    private var condition4ContactExists = false
    private var condition5PasswordCorrect = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialize sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

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


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)

            // Convert the rotation-vector to a 4x4 matrix.
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            // Get azimuth/pitch/roll from the rotation matrix
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val azimuthRad = orientationAngles[0] // Radians
            val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()

            // Normalize to 0..360
            val azimuth = (azimuthDeg + 360) % 360

            // Consider "north" if we are in a window around 0 degrees, e.g. 350–360 or 0–10
            val isNorth =
                (azimuth in 350f..360f) || (azimuth in 0f..10f)

            // Only update UI if the value actually changed
            if (isNorth != condition1North) {
                condition1North = isNorth
                updateUI()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rotationVectorSensor?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }
}
