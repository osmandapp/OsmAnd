package net.osmand.plus.utils

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager

object BLEUtils {

	fun isBLEEnabled(activity: Activity): Boolean {
		val bluetoothManager =
			activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		val bluetoothAdapter = bluetoothManager.adapter
		return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
				bluetoothAdapter.isEnabled
	}

	fun getBluetoothAdapter(activity: Activity): BluetoothAdapter? {
		val bluetoothManager =
			activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		return bluetoothManager.adapter
	}
}