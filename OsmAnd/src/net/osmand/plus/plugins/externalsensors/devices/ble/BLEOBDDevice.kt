package net.osmand.plus.plugins.externalsensors.devices.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import net.osmand.PlatformUtil
import net.osmand.plus.plugins.externalsensors.DeviceType
import net.osmand.plus.plugins.externalsensors.GattAttributes
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEOBDSensor
import net.osmand.util.Algorithms
import okio.Buffer
import okio.IOException
import okio.Sink
import okio.Source
import okio.Timeout
import java.util.UUID
import kotlin.math.min

class BLEOBDDevice(bluetoothAdapter: BluetoothAdapter, deviceId: String) :
	BLEAbstractDevice(bluetoothAdapter, deviceId), Source, Sink {
	private val log = PlatformUtil.getLog("OBD2")

	var response: String = ""

	private var bufferToRead: String? = null
	private var deviceReadyListener: DeviceReadyListener? = null
	private var isReady = false
	private val bleReadSensor: BLEOBDSensor = BLEOBDSensor(this)

	interface DeviceReadyListener {
		fun onDeviceReadyStateChange(isReady: Boolean)
	}

	private var writeCharacteristic: BluetoothGattCharacteristic? = null

	init {
		sensors.add(bleReadSensor)
	}

	override fun getDeviceType(): DeviceType {
		return DeviceType.BLE_OBD
	}

	override fun onGattConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
		super.onGattConnectionStateChange(gatt, status, newState)
		if (newState == BluetoothProfile.STATE_DISCONNECTED) {
			isReady = false
			writeCharacteristic = null
			deviceReadyListener?.onDeviceReadyStateChange(false)
		}
	}

	@SuppressLint("MissingPermission")
	override fun onGattServicesDiscovered(gatt: BluetoothGatt, status: Int) {
		super.onGattServicesDiscovered(gatt, status)
		if (status == BluetoothGatt.GATT_SUCCESS) {
			val obdService = gatt.getService(serviceUUID)
			writeCharacteristic =
				obdService?.getCharacteristic(GattAttributes.UUID_CHARACTERISTIC_OBD_WRITE)
			isReady = true
			deviceReadyListener?.onDeviceReadyStateChange(true)
		}
	}

	@SuppressLint("MissingPermission")
	fun sendCommand(command: String) {
		var result = false
		log.debug("sendCommand gatt {${bluetoothGatt}}, characteristic {$writeCharacteristic}")
		bluetoothGatt?.let { gatt ->
			writeCharacteristic?.let {
				val startTime = System.currentTimeMillis()
				var errorMessage: String
				do {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						val sendResult = gatt.writeCharacteristic(
							it,
							command.toByteArray(),
							BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
						)
						result = sendResult == BluetoothStatusCodes.SUCCESS
						errorMessage = "Send result code {$sendResult}"
					} else {
						result = gatt.writeCharacteristic(it)
						errorMessage = "writeCharacteristic failed"
					}
					val writeDuration = System.currentTimeMillis() - startTime
					if (writeDuration > 10000) {
						throw IOException("Can't write command $command. $errorMessage")
					}
				} while (!result)
			}

		}
	}

	companion object {
		val serviceUUID: UUID
			get() = GattAttributes.UUID_CHARACTERISTIC_OBD_SCANNER
	}

	override fun flush() {
	}

	override fun write(source: Buffer, byteCount: Long) {
		val fullCommand = source.readUtf8()
		if (writeCharacteristic != null) {
			bleReadSensor.resetData()
			sendCommand(fullCommand)
		}
	}

	override fun read(sink: Buffer, byteCount: Long): Long {
		if (Algorithms.isEmpty(bufferToRead)) {
			val lastSensorDataList = bleReadSensor.getLastSensorDataQueue()
			while (!Algorithms.isEmpty(lastSensorDataList)) {
				try {
					log.debug("lastSensorData getFirst")
					val lastSensorData = lastSensorDataList.first()
					if (lastSensorData == null) {
						log.debug("lastSensorData == null")
					} else {
						log.debug("lastSensorData == ${lastSensorData.response}")
					}
					lastSensorDataList.remove()
					bufferToRead = lastSensorData.response
				} catch (error: Exception) {
					log.debug("lastSensorData error")
				}
			}
		}
		bufferToRead?.let {
			val readCount = min(byteCount, it.length.toLong())
			if (readCount > 0) {
				val data = it.substring(0, readCount.toInt())
				bufferToRead = it.substring(readCount.toInt())
				sink.writeUtf8(data)
				return readCount
			}
		}
		return 0
	}


	override fun timeout(): Timeout {
		return Timeout.NONE
	}

	override fun close() {
	}

	fun setDeviceReadyListener(listener: DeviceReadyListener?) {
		deviceReadyListener = listener
		if (isReady) {
			deviceReadyListener?.onDeviceReadyStateChange(true)
		}
	}

	override fun disconnect(): Boolean {
		log.info("BLEDevice disconnect")
		val result = super.disconnect()
		bleReadSensor.resetData()
		return result
	}
}