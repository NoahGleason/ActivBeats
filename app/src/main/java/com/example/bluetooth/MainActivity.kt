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
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.support.v4.app.SupportActivity
import android.support.v4.app.SupportActivity.ExtraData
import android.support.v4.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.DisplayMetrics
import android.view.View
import android.widget.ImageButton
import android.view.LayoutInflater
import android.os.Build
import android.transition.Slide
import android.graphics.Color
import android.view.Gravity
import kotlinx.android.synthetic.main.activity_main.*
import android.transition.TransitionManager
import android.widget.*




private const val TAG = "ACTIVBEATS"
private const val SONG_TIME_MILLIS: Long = 60 * 1000

class MainActivity : AppCompatActivity(), A5BluetoothCallback {

    private var connectedDevices = mutableListOf<A5Device?>()
    private var readings = arrayListOf<Int>()
    private var times = arrayListOf<Long>()
    private var device: A5Device? = null
    private var counter: Int = 0
    private var countDownTimer: CountDownTimer? = null
    private var timeIsoStarted: Long = 0
    private var songStarted: Boolean = false
    private var cursorX: Double = 300.0




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

        startIsometricButton.setOnClickListener {
            readings.clear()
            times.clear()
            timeIsoStarted = System.currentTimeMillis()
            device?.startIsometric()
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

        goButton.setOnClickListener {
            textView.visibility = View.INVISIBLE
            recyclerView.visibility = View.INVISIBLE
            connectButton.visibility = View.INVISIBLE
            disconnectButton.visibility = View.INVISIBLE
            startIsometricButton.visibility = View.INVISIBLE
            scanDevices.visibility = View.INVISIBLE
            goButton.visibility = View.INVISIBLE
            tracksContainer.visibility = View.VISIBLE
            testImage.visibility = View.VISIBLE
            otrTrack.visibility = View.VISIBLE
            emptyTrack1.visibility = View.VISIBLE
            emptyTrack2.visibility = View.VISIBLE
            emptyTrack3.visibility = View.VISIBLE
            emptyTrack4.visibility = View.VISIBLE
            startCursor.visibility = View.VISIBLE
            resetCursor.visibility = View.VISIBLE
            currentbeat.visibility = View.VISIBLE
            beattype.visibility = View.VISIBLE
            testImage.bringToFront()
        }

        startCursor.setOnClickListener {
            songStarted = true
        }

        resetCursor.setOnClickListener {
            cursorX = 300.0
            songStarted = false
            testImage.x = cursorX.toFloat()
        }

        emptyTrack1.setOnClickListener {
            show_popup(R.layout.popup_1)
            beattype.setText("Hi Hat");
        }
        emptyTrack2.setOnClickListener {
            show_popup(R.layout.popup_2)
            beattype.setText("Snare Drum");
        }
        emptyTrack3.setOnClickListener {
            show_popup(R.layout.popup_3)
            beattype.setText("Kick");
        }
        emptyTrack4.setOnClickListener {
            show_popup(R.layout.popup_4)
            beattype.setText("Tom Tom");
        }

        testImage.x = cursorX.toFloat()
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

    @Synchronized
    private fun print2(name: String, value: Int) {
        runOnUiThread {
            pressureChangedTextView2.text =
                String.format(
                    Locale.US, "%s: %d", name, value
                )
        }
    }

    private fun manageReceiveIsometric(thisDevice: A5Device, thisValue: Int) {
        val time = System.currentTimeMillis()
        //var testImage: ImageView = findViewById(R.id.testImage)
        if (time > timeIsoStarted + SONG_TIME_MILLIS){
            thisDevice.stop()
        } else {
            print(thisDevice.device.name, thisValue)
            readings.add(thisValue)
            times.add(time)
            Log.v(TAG, "$time, $thisValue")
            //xCoord+=10.0
            //testImage.x = xCoord.toFloat()
        }

        if (songStarted) {
            startCursor()
        }
    }
    // ??????
    fun startCursor() {
        if(cursorX >= 2000) {
            songStarted = false
            cursorX += 13.0
            testImage.x = cursorX.toFloat()
        }
        cursorX += 17.0
        testImage.x = cursorX.toFloat()
    }
    // ????
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

    private fun show_popup(popup: Int){

        // Initialize a new layout inflater instance
        val inflater:LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Inflate a custom view using layout inflater
        val view = inflater.inflate(popup,null)

        // Initialize a new instance of popup window
        val popupWindow = PopupWindow(
            view, // Custom view to show in popup window
            LinearLayout.LayoutParams.WRAP_CONTENT, // Width of popup window
            LinearLayout.LayoutParams.WRAP_CONTENT // Window height
        )

        // Set an elevation for the popup window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.elevation = 10.0F
        }


        // If API level 23 or higher then execute the code
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Create a new slide animation for popup window enter transition
            val slideIn = Slide()
            slideIn.slideEdge = Gravity.TOP
            popupWindow.enterTransition = slideIn

            // Slide animation for popup window exit transition
            val slideOut = Slide()
            slideOut.slideEdge = Gravity.RIGHT
            popupWindow.exitTransition = slideOut

        }

        // Get the widgets reference from custom view
        val tv = view.findViewById<TextView>(R.id.text_view)
        val buttonPopup = view.findViewById<Button>(R.id.button_popup)

        // Set click listener for popup window's text view
        tv.setOnClickListener{
            // Change the text color of popup window's text view
            tv.setTextColor(Color.RED)
        }

        // Set a click listener for popup's button widget
        buttonPopup.setOnClickListener{
            // Dismiss the popup window
            popupWindow.dismiss()
        }

        // Set a dismiss listener for popup window
        popupWindow.setOnDismissListener {
            Toast.makeText(applicationContext,"Popup closed",Toast.LENGTH_SHORT).show()
        }


        // Finally, show the popup window on app
        TransitionManager.beginDelayedTransition(root_layout)
        popupWindow.showAtLocation(
            root_layout, // Location to display popup window
            Gravity.CENTER, // Exact position of layout to display popup
            0, // X offset
            0 // Y offset
        )
    }
}
