package com.example.rxandroidbleexample.utils

fun ByteArray.toHex() = joinToString("") { String.format("%02X", (it.toInt() and 0xff)) }