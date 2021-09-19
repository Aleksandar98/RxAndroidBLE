package com.example.rxandroidbleexample.adapters

import com.polidea.rxandroidble2.scan.ScanResult
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rxandroidbleexample.R


class DeviceRecyAdapter(val dataSet: MutableList<ScanResult>,
                        private val onClickListener: ((device: ScanResult) -> Unit)
) : RecyclerView.Adapter<DeviceRecyAdapter.ViewHolder>() {


    class ViewHolder(
        itemView: View,
        val onClickListener: ((device: ScanResult) -> Unit)
    ) : RecyclerView.ViewHolder(itemView) {

        val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        val deviceMacAdress: TextView = itemView.findViewById(R.id.deviceMACAdress)
        val signalStrength: TextView = itemView.findViewById(R.id.deviceSignalStr)

        fun bind(result: ScanResult) {
            deviceName.text = result.bleDevice.name
            deviceMacAdress.text = result.bleDevice.macAddress
            signalStrength.text = result.rssi.toString()
            itemView.setOnClickListener { onClickListener.invoke(result) }
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false)

        return ViewHolder(view, onClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.bind(dataSet[position])

    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    fun addScanResult(bleScanResult: ScanResult) {
        // Not the best way to ensure distinct devices, just for the sake of the demo.
        dataSet.withIndex()
            .firstOrNull { it.value.bleDevice == bleScanResult.bleDevice }
            ?.let {
                // device already in data list => update
                dataSet[it.index] = bleScanResult
                notifyItemChanged(it.index)
            }
            ?: run {
                // new device => add to data list
                with(dataSet) {
                    add(bleScanResult)
                    sortBy { it.bleDevice.macAddress }
                }
                notifyDataSetChanged()
            }
    }

    fun clearScanResults() {
        dataSet.clear()
        notifyDataSetChanged()
    }
}
