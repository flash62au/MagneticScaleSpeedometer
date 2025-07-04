package com.akersp.magneticscalespeedometer

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.round

class MainActivity : AppCompatActivity(), SensorEventListener, AdapterView.OnItemSelectedListener {
    // Add these at the top of your MainActivity class, with other properties
    private val PREFS_NAME = "MagneticScaleSpeedometer"
    private val KEY_SELECTED_SCALE_POSITION = "selectedScale"
    private val KEY_SELECTED_DISTANCE = "distance"
    private val KEY_SELECTED_AXIS = "axis"
    private val KEY_THRESHOLD = "threshold"

    private val INDEX_X = 0
    private val INDEX_Y = 1
    private val INDEX_Z = 2

    private val INCREASING = 1
    private val DECREASING = -1
    private val STEADY = 0

    private lateinit var sharedPref: SharedPreferences

    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null

    private lateinit var xAxisTextView: TextView

    private lateinit var resetButton: Button
    private lateinit var yAxisTextView: TextView
    private lateinit var zAxisTextView: TextView
    private lateinit var highestValueTextView: TextView

    private val thresholdLowValues: Array<Float> = arrayOf(0F, 0F, 0F)
    private val thresholdHighValues: Array<Float> = arrayOf(0F, 0F, 0F)

    private lateinit var thresholdValueEditText: EditText
    private var threshold: Float = 50F
    
    private val ambientValues: Array<Float> = arrayOf(0F, 0F, 0F)
    private lateinit var ambientValuesEditText: Array<EditText>
    private var isAmbientInitialised:Boolean = false

    private lateinit var ignoreFirstResponseCheckBox: CheckBox
    private var ignoreFirstResponse: Boolean = false
    private var haveSeenFirstResponse: Boolean = false

    private var highestXYZ: Float = 0F
    private val maxHistorySize = 20 // Maximum number of values to store
    private val lastXValues = mutableListOf<Float>() // List to store last 10 X values
    private val lastYValues = mutableListOf<Float>() // List to store last 10 Y values
    private val lastZValues = mutableListOf<Float>() // List to store last 10 Z values
    private val averageValues: Array<Float> = arrayOf(0F, 0F, 0F)
    private val increasingOrDecreasing: Array<Int> = arrayOf(0, 0, 0)
    private var wasIncreasingOrDecreasingWhenEventStarted: Int = 0

    private lateinit var startTimeTextView: TextView
    private lateinit var endTimeTextView: TextView
    private lateinit var runTimeTextView: TextView

    private var hasStarted = false
    private var hasFinished = false
    private var hasDroppedBelowThreshold = false
    private var startTime: Long = 0L
    private var endTime: Long = 0L
    private var runTime: Long = 0L

    private var highestStart: Float = 0F
    private var highestEnd: Float = 0F

    private lateinit var scalesSpinner: Spinner
    private lateinit var scaleRatioValues: Array<String> // To store ratios as strings
    private lateinit var scaleRatioFloats: FloatArray    // To store ratios as floats
    private lateinit var ratioTextView: TextView
    private var ratio: Float = 0F

    private lateinit var distanceEditText: EditText
    private var distance: Float = 0F

    private lateinit var axisSpinner: Spinner
    private var selectedAxis: Int = 0 // X

    private lateinit var speedTextView: TextView
    private var speedCmPerSec: Float = 0F
    private var speedKmPerHour: Float = 0F
    private var scaleSpeedKph: Float = 0F
    private var scaleSpeedMph: Float = 0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        xAxisTextView = findViewById(R.id.xAxisValue)
        yAxisTextView = findViewById(R.id.yAxisValue)
        zAxisTextView = findViewById(R.id.zAxisValue)
        highestValueTextView = findViewById(R.id.highestValue)

        // Get an instance of the SensorManager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Get the magnetometer sensor
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (magnetometer == null) {
            // Handle the case where the device doesn't have a magnetometer
            xAxisTextView.text = getString(R.string.warningNotAvailable)
            yAxisTextView.text = ""
            zAxisTextView.text = ""
            highestValueTextView.text = ""
        }

        // *****************************

        startTimeTextView = findViewById(R.id.startTime)
        endTimeTextView = findViewById(R.id.endTime)
        runTimeTextView = findViewById(R.id.runTime)

        // Set up the click listener for the reset button
        resetButton = findViewById(R.id.resetButton) // Initialize the reset button
        resetButton.setOnClickListener {
            resetSensorReadings()
        }

        // *****************************

        threshold = sharedPref.getFloat(KEY_THRESHOLD, 50F)
        thresholdValueEditText = findViewById(R.id.thresholdValue)
        thresholdValueEditText.setText(String.format("%.0f",threshold))

        // *****************************

        ambientValuesEditText = Array(3) { index ->
            EditText(this) // Requires context, 'this' is the Activity
        }
        ambientValuesEditText[INDEX_X] = findViewById(R.id.ambientXValue)
        ambientValuesEditText[INDEX_Y] = findViewById(R.id.ambientYValue)
        ambientValuesEditText[INDEX_Z] = findViewById(R.id.ambientZValue)
        setAmbientFromCurrent()

        // *****************************

        ratioTextView = findViewById(R.id.ratio)

        // Load the scaleRatios array
        scaleRatioValues = resources.getStringArray(R.array.scaleRatios)
        scaleRatioFloats = scaleRatioValues.mapNotNull { it.toFloatOrNull() }.toFloatArray()

        scalesSpinner = findViewById(R.id.scalesList)

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.scaleNames, // Your string array resource
            android.R.layout.simple_spinner_item // Default layout for the selected item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            scalesSpinner.adapter = adapter
        }

        // Restore the selected scale position
        val savedPosition = sharedPref.getInt(KEY_SELECTED_SCALE_POSITION, 0) // Default to 0 if not found
        scalesSpinner.setSelection(savedPosition) // Set the spinner to the saved position
        ratio = scaleRatioFloats[savedPosition]
        scalesSpinner.onItemSelectedListener = this // If your Activity implements OnItemSelectedListener

        // *****************************

        axisSpinner = findViewById(R.id.axisList)
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.axisNames, // Your string array resource
            android.R.layout.simple_spinner_item // Default layout for the selected item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            axisSpinner.adapter = adapter
        }

        // Restore the selected axis position
        val selectAxis = sharedPref.getInt(KEY_SELECTED_AXIS, 0) // Default to 0 if not found
        axisSpinner.setSelection(selectAxis) // Set the spinner to the saved position
        axisSpinner.onItemSelectedListener = this // If your Activity implements OnItemSelectedListener

        // *****************************

        // Find the EditText by its ID
        val savedDistance = sharedPref.getFloat(KEY_SELECTED_DISTANCE, 10F) // Default to 0F if not found
        distance = savedDistance // Update your class member variable
        distanceEditText = findViewById<EditText>(R.id.distanceValue)
        distanceEditText.setText(savedDistance.toString()) // Set the text in the EditText

        // *****************************

        ignoreFirstResponseCheckBox = findViewById(R.id.ignoreFirstResponse)

        // *****************************

        speedTextView = findViewById(R.id.speed)

        // *****************************

        resetSensorReadings()
    }

    override fun onResume() {
        super.onResume()
        // Register the sensor listener when the activity is resumed
        magnetometer?.also { magnet ->
            sensorManager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the sensor listener when the activity is paused to save battery
        sensorManager.unregisterListener(this)
    }

    private fun setAmbientFromCurrent() {
        threshold = thresholdValueEditText.getText().toString().toFloat()
        with(sharedPref.edit()) {
            putFloat(KEY_THRESHOLD, threshold)
            apply()
        }

        for (i in 0 until ambientValues.size) {
//            ambientValues[i] = round(averageValues[i])
            ambientValues[i] = averageValues[i]
            ambientValuesEditText[i].setText(String.format("%.2f",ambientValues[i]))

            thresholdLowValues[i] = ambientValues[i] - threshold
            thresholdHighValues[i] = ambientValues[i] + threshold
        }

        hideKeyboard()
    }

    private fun resetSensorReadings() {
        startTime = 0L
        endTime = 0L
        runTime = 0L

        hasStarted = false
        hasFinished = false
        hasDroppedBelowThreshold = true
        wasIncreasingOrDecreasingWhenEventStarted = STEADY
        lastXValues.clear() // Clear the history
        lastYValues.clear() // Clear the history
        lastZValues.clear() // Clear the history

        highestXYZ = 0F

        threshold = thresholdValueEditText.getText().toString().toFloat()
        with(sharedPref.edit()) {
            putFloat(KEY_THRESHOLD, threshold)
            apply()
        }

        haveSeenFirstResponse = false
        ignoreFirstResponse = ignoreFirstResponseCheckBox.isChecked
        ignoreFirstResponseCheckBox.setOnCheckedChangeListener { _, isChecked ->
            Log.d("Magnetic Scale Speedometer", "onCheckedChangeListener: checked: $isChecked")
            ignoreFirstResponse = isChecked
            haveSeenFirstResponse = false
        }

        startTimeTextView.text = ""
        endTimeTextView.text = ""
        runTimeTextView.text = ""
        speedTextView.text = ""

        // Get the text from the EditText
        val distanceString = distanceEditText.text.toString()

        // If you expect a number, you might want to parse it
        // Handle potential NumberFormatException
        try {
            distance = distanceString.toFloat()
            with(sharedPref.edit()) {
                putFloat(KEY_SELECTED_DISTANCE, distance)
                apply()
            }
            // Use the distanceDouble value
        } catch (e: NumberFormatException) {
            distance = 0F
            // Handle the case where the input is not a valid number
            distanceEditText.error = "Invalid number"
        }
        setAmbientFromCurrent()
        hideKeyboard()
    }

    private fun setAverageValues(index:Int ) {
        when (index) {
            INDEX_X -> averageValues[index] = lastXValues.average().toFloat()
            INDEX_Y -> averageValues[index] = lastYValues.average().toFloat()
            INDEX_Z -> averageValues[index] = lastZValues.average().toFloat()
        }

        increasingOrDecreasing[index] = STEADY
        if ( (averageValues[index] > thresholdLowValues[index]) && (averageValues[index] < thresholdHighValues[index]) ) {
            increasingOrDecreasing[index] = STEADY
        } else if (averageValues[index] < ambientValues[index]) {
            increasingOrDecreasing[index] = DECREASING
        } else if (averageValues[index] > ambientValues[index]) {
            increasingOrDecreasing[index] = INCREASING
        }
    }

    private fun addXValueToHistory(value: Float) {
        if (lastXValues.size >= maxHistorySize) {
            lastXValues.removeAt(0) // Remove the oldest value if the list is full

            if (!isAmbientInitialised) {
                setAmbientFromCurrent()
                isAmbientInitialised = true
            }
        }
        lastXValues.add(value) // Add the new value

        setAverageValues(INDEX_X)
    }

    private fun addYValueToHistory(value: Float) {
        if (lastYValues.size >= maxHistorySize) {
            lastYValues.removeAt(0) // Remove the oldest value if the list is full
        }
        lastYValues.add(value) // Add the new value

        setAverageValues(INDEX_Y)
    }

    private fun addZValueToHistory(value: Float) {
        if (lastZValues.size >= maxHistorySize) {
            lastZValues.removeAt(0) // Remove the oldest value if the list is full
        }
        lastZValues.add(value) // Add the new value

        setAverageValues(INDEX_Z)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // You can handle changes in sensor accuracy here if needed
        // For example, you might want to inform the user if the sensor accuracy is low.
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            // Get the magnetic field values for each axis


            // Update the TextViews with the new values
            xAxisTextView.text = String.format(getString(R.string.xAxisValueLabel), event.values[INDEX_X], averageValues[INDEX_X], ambientValues[INDEX_X])
            yAxisTextView.text = String.format(getString(R.string.yAxisValueLabel), event.values[INDEX_Y], averageValues[INDEX_Y], ambientValues[INDEX_Y])
            zAxisTextView.text = String.format(getString(R.string.zAxisValueLabel), event.values[INDEX_Z], averageValues[INDEX_Z], ambientValues[INDEX_Z])
            highestValueTextView.text = String.format(getString(R.string.highestValueLabel), highestXYZ)

            addXValueToHistory(event.values[INDEX_X])
            addYValueToHistory(event.values[INDEX_Y])
            addZValueToHistory(event.values[INDEX_Z])

//            Log.d("Magnetic Scale Speedometer", "onSensorChanged(): xAxis: $xAxis")

            var axisXYZ = event.values[selectedAxis]
            var thresholdLow = thresholdLowValues[selectedAxis]
            var thresholdHigh = thresholdHighValues[selectedAxis]
            var isIncreasing = increasingOrDecreasing[selectedAxis] == INCREASING
            var isDecreasing = increasingOrDecreasing[selectedAxis] == DECREASING

            if (isIncreasing) {
                Log.d("Magnetic Scale Speedometer", "onSensorChanged(): is Increasing: $axisXYZ  started as: $wasIncreasingOrDecreasingWhenEventStarted")
            } else if (isDecreasing) {
                Log.d("Magnetic Scale Speedometer", "onSensorChanged(): is Decreasing: $axisXYZ  started as: $wasIncreasingOrDecreasingWhenEventStarted")
            }

            if (axisXYZ < thresholdLow || axisXYZ > thresholdHigh) {
//                Log.d("Magnetic Scale Speedometer", "onSensorChanged(): Below threshold - Ignore")
                hasDroppedBelowThreshold = true
                return
            }

            if (!hasDroppedBelowThreshold) {
                Log.d("Magnetic Scale Speedometer", "onSensorChanged(): Has not dropped below threshold yet")
                return
            }

            var triggerNow = false
            if (isIncreasing) {
                if (wasIncreasingOrDecreasingWhenEventStarted == STEADY) { // something started
                    wasIncreasingOrDecreasingWhenEventStarted = INCREASING
                    highestXYZ = axisXYZ
                } else if (wasIncreasingOrDecreasingWhenEventStarted == INCREASING) {  // continuing above threshold
                    if (axisXYZ >= highestXYZ) {
                        highestXYZ = axisXYZ
                    } else {
                        triggerNow = true
                    }
                } else { // it has now started but now it is below threshold - should not happen
                    triggerNow = true
                }
            } else if (isDecreasing) {
                if (wasIncreasingOrDecreasingWhenEventStarted == STEADY) {  // STEADY or DECREASING
                    wasIncreasingOrDecreasingWhenEventStarted = DECREASING
                    highestXYZ = axisXYZ
                } else if (wasIncreasingOrDecreasingWhenEventStarted == DECREASING) {  // continuing below threshold
                    if (axisXYZ <= highestXYZ) {
                        highestXYZ = axisXYZ
                    } else {
                        triggerNow = true
                    }
                } else { // it has now started but now it is above threshold - should not happen
                    triggerNow = true
                }
            }

            if (triggerNow) {

                if ( (ignoreFirstResponse) && (!haveSeenFirstResponse) ) {
                    Toast.makeText(this, getString(R.string.ignoreFirstResponseNotice), Toast.LENGTH_SHORT).show()
                    Log.d("Magnetic Scale Speedometer", "onSensorChanged(): Ignoring first response")
                    haveSeenFirstResponse = true
                    hasStarted = false
                    hasDroppedBelowThreshold = false
                    highestXYZ = 0F
                    hideKeyboard()
                    return
                }

                if (!hasStarted) {  // starting
                    Toast.makeText(this, getString(R.string.startedNotice), Toast.LENGTH_SHORT).show()
                    Log.d("Magnetic Scale Speedometer", "onSensorChanged(): Started")
                    startTime = System.currentTimeMillis()
                    highestStart = highestXYZ
                    hasStarted = true
                    hasDroppedBelowThreshold = false
                    startOrEndTimeHasChanged()
                    return
                }

                if (!hasFinished) {
                    Toast.makeText(this, getString(R.string.endedNotice), Toast.LENGTH_SHORT).show()
                    Log.d("Magnetic Scale Speedometer", "onSensorChanged(): Ended")
                    highestEnd = highestXYZ
                    endTime = System.currentTimeMillis()
                    startOrEndTimeHasChanged()
                    return
                }
            }

        }
    }

    private fun startOrEndTimeHasChanged() {
        if (hasFinished) return

        hideKeyboard()

        startTimeTextView.text = String.format(getString(R.string.startTimeLabel), startTime, highestStart)

        runTime = 0L;
        if ( (hasStarted) && (endTime != 0L) ) {
            runTime = endTime - startTime
            hasFinished = true

            runTimeTextView.text = String.format(getString(R.string.runTimeLabel), runTime)

            endTimeTextView.text = String.format(getString(R.string.endTimeLabel), endTime, highestEnd)
            runTimeTextView.text = String.format(getString(R.string.runTimeLabel), runTime)

            refreshScaleSpeed()
            speedTextView.text = String.format(getString(R.string.speedLabel), scaleSpeedKph, scaleSpeedMph)

        } else {
            endTimeTextView.text = ""
            runTimeTextView.text = ""
            speedTextView.text = ""
        }
        ratioTextView.text = String.format(getString(R.string.ratioLabel), ratio)

        highestXYZ = 0F

    }

    private fun refreshScaleSpeed() {
        if ( (distance == 0F) || (runTime == 0L) ) return

        speedCmPerSec = distance / runTime * 1000
        speedKmPerHour = speedCmPerSec * 3600 / 100000
        scaleSpeedKph = speedKmPerHour / ratio
        var mph: Float = speedKmPerHour * 0.621371F / ratio
        scaleSpeedMph = mph
    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)

        if (parent?.id == R.id.axisList) { // Check if it's the correct spinner
            selectedAxis = position

//            val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putInt(KEY_SELECTED_AXIS, position)
                apply()
            }
            return
        }

        if (parent?.id == R.id.scalesList) { // Check if it's the correct spinner
            val selectedScale = parent.getItemAtPosition(position).toString()
//            Toast.makeText(this, "Selected: $selectedScale", Toast.LENGTH_SHORT).show()
            Log.d("Magnetic Scale Speedometer", "Selected scale: $selectedScale at position $position")

            ratio = scaleRatioFloats[position]
            ratioTextView.text = String.format(getString(R.string.ratioLabel), ratio)
            refreshScaleSpeed()

            with(sharedPref.edit()) {
                putInt(KEY_SELECTED_SCALE_POSITION, position)
                apply()
            }

            return
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Another interface callback
        // This is called when the selection disappears from the spinner.
        // For example, when the adapter becomes empty.
        Log.d("Magnetic Scale Speedometer", "Nothing selected")
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }
}
