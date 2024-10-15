package net.osmand.plus.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import net.osmand.plus.R

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

	@SuppressLint("MissingPermission")
	fun BluetoothDevice.getAliasName(context: Context): String {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			alias
		} else {
			name
		} ?: context.getString(R.string.unknown_bt_device)
	}
}