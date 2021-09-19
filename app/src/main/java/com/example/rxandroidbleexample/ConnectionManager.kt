package com.example.rxandroidbleexample

import android.content.Context
import android.util.Log
import com.example.rxandroidbleexample.utils.toHex
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.RxBleScanResult
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.*

object ConnectionManager {

    var isScanRuntimePermissionGranted: Boolean = false
    lateinit var rxBleClient: RxBleClient
    lateinit var rxBleConnection: RxBleConnection

    fun initBleClient(context: Context) {
        rxBleClient = RxBleClient.create(context)
        isScanRuntimePermissionGranted = rxBleClient.isScanRuntimePermissionGranted
    }

    fun scanBleDevices(): Observable<ScanResult> {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        val scanFilter = ScanFilter.Builder()
//            .setDeviceAddress("B4:99:4C:34:DC:8B")
            // add custom filters if needed
            .build()

        return rxBleClient.scanBleDevices(scanSettings, scanFilter)
    }



    fun readCharacteristicValue(charUuid: UUID): Single<ByteArray>? {
        return rxBleConnection.readCharacteristic(charUuid)

    }

    fun subToCharacteristic(charUuid: UUID): Observable<Observable<ByteArray>>? {
        return rxBleConnection.setupNotification(charUuid)
    }


}

