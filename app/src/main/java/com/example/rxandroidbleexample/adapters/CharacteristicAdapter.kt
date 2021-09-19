package com.example.rxandroidbleexample.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rxandroidbleexample.R
import com.example.rxandroidbleexample.utils.AllGattCharacteristics
import java.util.*

class CharacteristicAdapter(
    val items : List<UUID>,
    val onClickListener: ((uuid : UUID) -> Unit )
) : RecyclerView.Adapter<CharacteristicAdapter.ViewHolder>() {

    class ViewHolder(
        val itemView: View,
        val onClickListener: ((uuid: UUID) -> Unit)
    ) : RecyclerView.ViewHolder(itemView) {

        val charTitle : TextView = itemView.findViewById(R.id.charTitle)

        fun bind ( uuid : UUID ){
            charTitle.text = AllGattCharacteristics.getCharacteristicName(uuid.toString())
            itemView.setOnClickListener { onClickListener.invoke(uuid) }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.characteristic_item,parent,false)

        return ViewHolder(view,onClickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }
}