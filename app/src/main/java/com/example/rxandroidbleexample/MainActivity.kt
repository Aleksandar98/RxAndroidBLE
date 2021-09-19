package com.example.rxandroidbleexample

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rxandroidbleexample.adapters.DeviceRecyAdapter
import com.example.rxandroidbleexample.utils.showSnackbarShort
import com.google.android.material.snackbar.Snackbar
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.exceptions.BleScanException
import com.polidea.rxandroidble2.scan.ScanResult
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*

const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
const val LOCATION_PERMISSION_REQUEST_CODE = 2

class MainActivity : AppCompatActivity() {

    private val discoveryDisposable = CompositeDisposable()
    private var connectionDisposable: Disposable? = null

    private val TAG: String? = "myTag"
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager =
            ContextCompat.getSystemService(this, BluetoothManager::class.java) as BluetoothManager
        bluetoothManager.adapter
    }

    val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: DeviceRecyAdapter by lazy {
        DeviceRecyAdapter(scanResults) { result ->
            if (isScanning)
                scanDisposable?.dispose()
            with(result.bleDevice) {


                progressBar.visibility = View.VISIBLE

                Log.d(TAG, "Connecting to : $macAddress")
                //connectGatt(this@MainActivity,false, ConnectionManager.gattCallback)

                establishConnection(false)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ onConnectionReceived(it) }, { onConnectionFailure(it) })
                    .let { connectionDisposable = it }
            }
        }
    }
    private var scanDisposable: Disposable? = null




    private var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ConnectionManager.initBleClient(this)

        initRecyclerView()

        scanBtn.setOnClickListener {
            onScanToggleClick()
        }
    }


    private fun launchServicesActivity(services: MutableList<BluetoothGattService>) {

        progressBar.visibility = View.GONE

        val uuidList = arrayListOf<String>()
        services.forEach {
            it.characteristics.forEach {
                uuidList.add(it.uuid.toString())
            }
        }

        Log.d(TAG, "launchServicesActivity: uuid list: ${uuidList.size}")

        Intent(application, CharacteristicsActivity::class.java).also {
            it.putExtra(BluetoothDevice.EXTRA_DEVICE, uuidList)
            startActivity(it)
        }
    }

    private fun initRecyclerView() {
        val deviceRecyclerView: RecyclerView = findViewById(R.id.deviceRecyclerView)

        deviceRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        deviceRecyclerView.adapter = scanResultAdapter
    }


    private fun onConnectionFailure(throwable: Throwable) {
        Log.e(TAG, "onConnectionFailure: ${throwable.message}")
        progressBar.visibility = View.GONE
        showSnackbarShort(throwable.message.toString())
    }

    private fun onConnectionReceived(rxBleConnection: RxBleConnection) {

        Log.d(TAG, "onConnectionReceived: primio konekciju")

        ConnectionManager.rxBleConnection = rxBleConnection

        discoverServices()
    }

    private fun discoverServices() {

        ConnectionManager.rxBleConnection.discoverServices()
            .observeOn(AndroidSchedulers.mainThread())
            .map { it.bluetoothGattServices }
            .subscribe({ services -> launchServicesActivity(services) },
                { throwable -> showSnackbarShort("Connection error: $throwable") })
            .let { discoveryDisposable.add(it) }
    }


    private fun onScanToggleClick() {
        if (isScanning) {
            triggerDisconnect()
            isScanning = false
        } else {
            isScanning = true
            if (ConnectionManager.isScanRuntimePermissionGranted) {
                ConnectionManager.scanBleDevices()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally {
                        dispose()
                    }
                    .subscribe({
                        scanResultAdapter.addScanResult(it)
                        noDevicesTxt.visibility = View.GONE
                    }, { onScanFailure(it) })
                    .let { scanDisposable = it }
            } else {

                requestLocationPermission()
            }
        }
        updateButtonUIState()
    }


    private fun dispose() {
        scanDisposable = null
        scanResultAdapter.clearScanResults()
        updateButtonUIState()
    }

    private fun onScanFailure(throwable: Throwable) {
        if (throwable is BleScanException) showError(throwable)
    }

    private fun showError(throwable: BleScanException) {
        Toast.makeText(this, throwable.message, Toast.LENGTH_LONG).show()
    }

    private fun updateButtonUIState() =
        scanBtn.setImageResource(if (isScanning) R.drawable.ic_baseline_bluetooth_disabled_24 else R.drawable.ic_baseline_bluetooth_searching_24)

    private fun triggerDisconnect() = connectionDisposable?.dispose()

//    override fun onPause() {
//        super.onPause()
//        triggerDisconnect()
//        discoveryDisposable.clear()
//    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location permission required")
        builder.setMessage("Starting from Android M (6.0), the system requires apps to be granted location access in order to scan for BLE devices.")

        builder.setPositiveButton(android.R.string.yes) { dialog, which ->
            requestPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
        builder.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                } else {
                    //startBleScan()
                }
            }
        }
    }

    val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)


    fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

}


