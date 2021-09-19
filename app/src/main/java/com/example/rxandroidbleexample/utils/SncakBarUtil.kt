package com.example.rxandroidbleexample.utils

import android.app.Activity
import com.google.android.material.snackbar.Snackbar

internal fun Activity.showSnackbarShort(text: CharSequence) {
    Snackbar.make(findViewById(android.R.id.content), text, Snackbar.LENGTH_SHORT).show()
}