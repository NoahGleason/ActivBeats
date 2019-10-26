package com.example.bluetooth

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import a5.com.a5bluetoothlibrary.A5DeviceManager
import a5.com.a5bluetoothlibrary.A5BluetoothCallback
import a5.com.a5bluetoothlibrary.A5Device
import android.content.res.Resources
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.Toast
import com.example.bluetooth.wav.WavFile
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

private const val TAG = "ACTIVBEATS"
private const val BUFFER = 100

class MainActivity : AppCompatActivity(), A5BluetoothCallback {

    private var connectedDevices = mutableListOf<A5Device?>()
    private var readings = arrayListOf<ArrayList<Int>>()
    private var times = arrayListOf<ArrayList<Long>>()
    private var trackNums = arrayListOf<Int>()
    private var device: A5Device? = null
    private var counter: Int = 0
    private var countDownTimer: CountDownTimer? = null
    private var timeIsoStarted: Long = 0
    private var sampleRate: Long= 0
    private var numFrames: Long = 0
    private var validBits: Int = 0
    private var trackData = arrayListOf<ArrayList<Double>>()

    private lateinit var deviceAdapter: DeviceAdapter

    override fun bluetoothIsSwitchedOff() {
        Toast.makeText(this, "bluetooth is switched off", Toast.LENGTH_SHORT).show()
    }

    override fun searchCompleted() {
        Toast.makeText(this, "search completed", Toast.LENGTH_SHORT).show()
    }

    override fun didReceiveIsometric(device: A5Device, value: Int) {
        manageReceiveIsometric(device, value)
    }

    override fun onWriteCompleted(device: A5Device, value: String) {
    }

    override fun deviceConnected(device: A5Device) {
    }

    override fun deviceFound(device: A5Device) {
        deviceAdapter.addDevice(device)
        connectedDevices.add(device)
    }

    override fun deviceDisconnected(device: A5Device) {
    }

    override fun on133Error() {
    }

    object Values {
        const val REQUEST_ENABLE_INTENT = 999
        const val MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 998
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tracks = arrayOf(WavFile.openWavFile(resources.openRawResource(R.raw.l1s)),
            WavFile.openWavFile(resources.openRawResource(R.raw.l2s)),
//            WavFile.openWavFile(resources.openRawResource(R.raw.layer3)),
//            WavFile.openWavFile(resources.openRawResource(R.raw.layer4)),
//            WavFile.openWavFile(resources.openRawResource(R.raw.layer5)),
            WavFile.openWavFile(resources.openRawResource(R.raw.l3s)))
        sampleRate = tracks[0].sampleRate
        numFrames = tracks[0].numFrames
        validBits = tracks[0].validBits
        for (track in tracks) {
            trackData.add(WavFile.getRaw(track, 0) as ArrayList<Double>)
        }

        requestPermission()
        initRecyclerView()

        connectButton.setOnClickListener {
            val device = this.device
            if (device != null) {
                A5DeviceManager.connect(this, device)
            }
        }

        disconnectButton.setOnClickListener {
            device?.disconnect()
        }

        sendStopCommandButton.setOnClickListener {
            readings.clear()
            times.clear()
            trackNums.clear()
            device?.stop()
            startTimer()
        }

        abortStopCommandButton.setOnClickListener {
            device?.startIsometric()
            stopTimer()
        }

        startIsometricButton.setOnClickListener {
            trackNums.add(trackIndex.text.toString().toInt())
            timeIsoStarted = System.currentTimeMillis()
            readings.add(ArrayList())
            times.add(ArrayList())
            device?.startIsometric()
        }

        tareButton.setOnClickListener {
            export("multitrack.wav")
        }

        scanDevices.setOnClickListener {
            for (device in connectedDevices) {
                device?.disconnect()
            }
            device?.disconnect()
            device = null
            connectedDevices.clear()
            deviceAdapter.clearDevices()

            A5DeviceManager.scanForDevices()
        }
    }

    @Synchronized
    private fun print(name: String, value: Int) {
        runOnUiThread {
            pressureChangedTextView1.text =
                String.format(
                    Locale.US, "%s: %d", name, value
                )
        }
    }

    private fun manageReceiveIsometric(thisDevice: A5Device, thisValue: Int) {
        val time = System.currentTimeMillis()
        if (time > timeIsoStarted + numFrames * 1000 / sampleRate){
            thisDevice.stop()
        } else {
            print(thisDevice.device.name, thisValue)
            readings[readings.size -1].add(thisValue)
            times[times.size - 1].add(time)
            Log.v(TAG, "$time, $thisValue")
        }
    }

    private fun stretchTimeSeries(data : ArrayList<Int>, time : ArrayList<Long>, newLength : Int) : DoubleArray {
        val toRet = DoubleArray(newLength)
        val start = time[0]
        val timeStep : Double = (time[time.size - 1] - start).toDouble() / (newLength - 1).toDouble()
        var currentIndex = 0
        for (i in toRet.indices) {
            val thisTime = i*timeStep
            while(thisTime > (time[currentIndex + 1] - start)){
                currentIndex++
                if (currentIndex >= time.size - 1){
                    toRet.fill(data[data.size - 1].toDouble(), i)
                    return toRet
                }
            }
            val linearInterp : Double = (thisTime - (time[currentIndex] - start)) / (time[currentIndex + 1] - time[currentIndex]).toDouble()
            toRet[i] = data[currentIndex].toDouble() + linearInterp*(data[currentIndex + 1] - data[currentIndex]).toDouble()
        }
        return toRet
    }

    private fun export(filename: String) {
        Log.v(TAG, "export started")
        var max = 1
        var averages = DoubleArray(readings.size)
        var stretchedReadings = arrayListOf<DoubleArray>()
        for (i in readings.indices){
            var sum = 0
            for (j in readings[i]){
                sum += j
                max = kotlin.math.max(max, j)
            }
            averages[i] = sum.toDouble() / readings[i].size
            stretchedReadings.add(stretchTimeSeries(readings[i], times[i],numFrames.toInt()))
        }

        val outputFile = WavFile.newWavFile(FileOutputStream(File(getExternalFilesDir(null),filename)), 1, numFrames, validBits, sampleRate)

        Log.v(TAG, "Output file opened")

        var buffer = DoubleArray(BUFFER)

        var frameCounter = 0

        // Loop until all frames written
        while (frameCounter < outputFile.numFrames) {
            // Determine how many frames to write, up to a maximum of the buffer size
            val remaining = outputFile.framesRemaining
            Log.v(TAG, "$remaining frames left")
            val toWrite = if (remaining > 100) 100 else remaining.toInt()

            // Fill the buffer
            for (i in 0 until toWrite){
                var dat = 0.0

                //Add in each track
                for (j in stretchedReadings.indices){
                    dat += stretchedReadings[j][frameCounter]/max * trackData[trackNums[j]][frameCounter]
                }

                buffer[i] = dat

                frameCounter++
            }

            // Write the buffer
            outputFile.writeFrames(buffer, toWrite)
        }

        Log.v(TAG, "Export finished")

        outputFile.close()
    }

    fun deviceSelected(device: A5Device) {
        this.device = device
        Toast.makeText(this, "device selected: " + device.device.name, Toast.LENGTH_SHORT).show()
    }

    private fun initRecyclerView() {
        deviceAdapter = DeviceAdapter(this)

        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.adapter = deviceAdapter
    }

    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                ) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        Values.MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION
                    )

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            } else {
                startBluetooth()
            }
        } else {
            startBluetooth()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            Values.MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    startBluetooth()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Values.REQUEST_ENABLE_INTENT) {
            if (resultCode == Activity.RESULT_OK) {
                startBluetooth()
            }
        }
    }

    private fun startBluetooth() {
        val bluetoothManager = A5App().getInstance().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, Values.REQUEST_ENABLE_INTENT)
        } else {
            A5DeviceManager.setCallback(this)
            A5DeviceManager.scanForDevices()
        }
    }

    private fun startTimer() {
        counter = 0
        countDownTimer = object : CountDownTimer(420000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                counter++
            }

            override fun onFinish() {
            }
        }.start()
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
    }
}
