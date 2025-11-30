package com.example.secretpass

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.secretpass.databinding.ActivityMainBinding
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding

    private val contactNameToCheck = "Chiburashka"
    //Sensors
    private var sensorManager: SensorManager? = null
    private var rotationVectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null

    //Shake detection
    private var lastShakeTime: Long = 0
    private val shakeThreshold = 12f
    private val minTimeBetweenShakesMs = 1000L

    //Conditions
    private var condition1North = false
    private var condition2Noise = false
    private var condition3Shaken = false
    private var condition4ContactExists = false
    private var condition5PasswordCorrect = false


    private val requestContactsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Permission granted → check the contact now
                checkContactConditionInternal()
            } else {
                // Permission denied → condition stays false
                condition4ContactExists = false
                updateUI()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialize sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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

        checkContactCondition()

    }

    private fun checkPasswordCondition() {
        val password = binding.etPassword.text?.toString().orEmpty()

        val batteryPercent = getBatteryPercentage()
        // If for some reason we couldn't get battery, just treat as incorrect
        if (batteryPercent !in 0..100) {
            condition5PasswordCorrect = false
            updateUI()
            return
        }

        val expectedPassword = "secret$batteryPercent"
        condition5PasswordCorrect = (password == expectedPassword)

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
        when (event?.sensor?.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                val orientationAngles = FloatArray(3)

                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                val azimuthRad = orientationAngles[0] // Radians
                val azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
                val azimuth = (azimuthDeg + 360) % 360   // 0..360

                val isNorth = (azimuth in 350f..360f) || (azimuth in 0f..10f)

                if (isNorth != condition1North) {
                    condition1North = isNorth
                    updateUI()
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                handleShake(event)
            }
        }
    }


    private fun handleShake(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Total acceleration magnitude
        val magnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        // Approximate acceleration above gravity
        val acceleration = magnitude - SensorManager.GRAVITY_EARTH

        if (acceleration > shakeThreshold) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > minTimeBetweenShakesMs) {
                lastShakeTime = now

                if (!condition3Shaken) {
                    condition3Shaken = true
                    updateUI()
                }
            }
        }
    }
    private fun checkContactCondition() {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            checkContactConditionInternal()
        } else {
            // Ask for permission
            requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun checkContactConditionInternal() {
        val uri = ContactsContract.Contacts.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )

        // Case-sensitive exact match
        val selection = "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} = ?"
        val selectionArgs = arrayOf(contactNameToCheck)

        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor.use { c ->
            val exists = c != null && c.count > 0
            condition4ContactExists = exists
        }

        updateUI()
    }


    private fun getBatteryPercentage(): Int {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (level in 0..100) {
            return level
        }

        // Fallback (older devices)
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, intentFilter)
        val rawLevel = batteryStatus?.getIntExtra("level", -1) ?: -1
        val scale = batteryStatus?.getIntExtra("scale", -1) ?: -1
        return if (rawLevel >= 0 && scale > 0) {
            (rawLevel * 100) / scale
        } else {
            -1
        }
    }

    override fun onResume() {
        super.onResume()
        // Register sensor for north condition
        rotationVectorSensor?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        // Register sensor for shake detection
        accelerometerSensor?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }

        //Check if we already have permission and if so, check the contact
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            checkContactConditionInternal()
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }
}
