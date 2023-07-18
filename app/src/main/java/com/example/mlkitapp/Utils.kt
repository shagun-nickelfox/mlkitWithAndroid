package com.example.mlkitapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

fun Context.isPermissionGranted(permission:String):Boolean =
    ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_GRANTED

inline fun Context.cameraPermissionRequest(crossinline positive:()->Unit){
    AlertDialog.Builder(this)
        .setTitle("Camera permission Required")
        .setMessage("Without accessing the camera it is not possible to SCAN QR Codes")
        .setPositiveButton("Allow Camera"){ dialog,which ->
            positive.invoke()
        }
        .setNegativeButton("cancel"){dialog,which->
            dialog.cancel()
        }
        .show()
}

fun Context.openPermissionSetting(){
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also {
        val uri: Uri = Uri.fromParts("package",packageName,null)
        it.data = uri
        startActivity(it)
    }
}