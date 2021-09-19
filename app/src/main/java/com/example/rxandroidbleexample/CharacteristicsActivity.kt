package com.example.rxandroidbleexample

import android.bluetooth.BluetoothDevice
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rxandroidbleexample.adapters.CharacteristicAdapter
import com.example.rxandroidbleexample.utils.showSnackbarShort
import com.example.rxandroidbleexample.utils.toHex
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_characteristics.*
import java.util.*
import kotlin.collections.ArrayList

class CharacteristicsActivity : AppCompatActivity() {

     val TAG = "myTag"
    lateinit var selectedCharacteristicUuid: UUID
    private val connectionDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_characteristics)

        val uuids: ArrayList<String> = intent.getStringArrayListExtra(BluetoothDevice.EXTRA_DEVICE)
            ?: error("Missing BluetoothDevice from MainActivity!")

        val charAdapter: CharacteristicAdapter by lazy {
            CharacteristicAdapter(uuids.map { UUID.fromString(it)}) {
                Log.d(TAG, "onCreate: klikuo item")
                selectedCharacteristicUuid = it
                showOptions()

            }
        }

        val characteristicRecyView: RecyclerView = findViewById(R.id.characteristicRecyView)
        characteristicRecyView.apply {
            adapter = charAdapter
            layoutManager =
                LinearLayoutManager(this@CharacteristicsActivity, RecyclerView.VERTICAL, false)
        }

        readBtn.setOnClickListener {
            ConnectionManager.readCharacteristicValue(selectedCharacteristicUuid)?.let{
                it.observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onCharValueRead(it) },{ onCharValueReadFail(it)})
                    .let { connectionDisposable.add(it) }
            }
        }

        subBtn.setOnClickListener {
            ConnectionManager.subToCharacteristic(selectedCharacteristicUuid)?.let{
                it.observeOn(AndroidSchedulers.mainThread())
                    .doOnNext( { runOnUiThread { notificationHasBeenSetUp() }})
                    .flatMap { it }
                    .subscribe({ onNotificationReceived(it) }, { onNotificationSetupFailure(it) })
                    .let { connectionDisposable.add(it) }
            }
        }

    }

    override fun onPause() {
        super.onPause()
        connectionDisposable.clear()
    }

    private fun notificationHasBeenSetUp() {
        showSnackbarShort("Notifications has been set up")
    }

    private fun showOptions() {
        readBtn.visibility = View.VISIBLE
        subBtn.visibility = View.VISIBLE
    }

    private fun onNotificationSetupFailure(throwable: Throwable) {
        throwable.message?.let { Log.e(TAG, "onNotificationSetupFailure: ${it}}", )
            showSnackbarShort("Failed subscribing to this characteristic")
        }
    }

    private fun onNotificationReceived(bytes: ByteArray) {
        showSnackbarShort(bytes.toHex())
    }

    private fun onCharValueRead(bytes: ByteArray){
        showSnackbarShort(bytes.toHex())
    }
    private fun onCharValueReadFail(throwable: Throwable){
        throwable.message?.let { Log.e(TAG, "onCharValueReadFail: ${it}}", )
            showSnackbarShort("Failed reading this characteristic")
        }
    }
}