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
import okio.Sink
import okio.Source
import okio.Timeout
import java.util.UUID
import kotlin.math.min

class BLEOBDDevice(bluetoothAdapter: BluetoothAdapter, deviceId: String) :
	BLEAbstractDevice(bluetoothAdapter, deviceId), Source, Sink {
	private val log = PlatformUtil.getLog("OBD2")
	var response: String = ""

	var lastDataUpdate = 0L
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
			val obdService =
				gatt.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"))
			writeCharacteristic =
				obdService?.getCharacteristic(UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"))
		}
		isReady = true
		deviceReadyListener?.onDeviceReadyStateChange(true)
	}

	@SuppressLint("MissingPermission")
	fun sendCommand(command: String) {
		writeCharacteristic?.let {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				val res = bluetoothGatt?.writeCharacteristic(
					it,
					command.toByteArray(),
					BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
				)
				if (res == BluetoothStatusCodes.SUCCESS) {
					cachedCharacteristics?.let {
					}
				}
				log.debug("Відправлено команду: $command; result $res")
			} else {
				val res = bluetoothGatt?.writeCharacteristic(it)
				log.debug("Відправлено команду: $command; result $res")
			}
		}
	}

	companion object {
		val serviceUUID: UUID
			get() = GattAttributes.UUID_OBD
	}

	override fun flush() {
	}

	override fun write(source: Buffer, byteCount: Long) {
		val fullCommand = source.readUtf8()
		enqueueCommand {
			if (writeCharacteristic != null) {
				bleReadSensor.resetData()
				sendCommand(fullCommand)
			}
			completedCommand()
		}
	}

	override fun read(sink: Buffer, byteCount: Long): Long {
		if (Algorithms.isEmpty(bufferToRead)) {
			val lastSensorDataList = bleReadSensor.getLastSensorDataQueue()
			while (!Algorithms.isEmpty(lastSensorDataList)) {
				val lastSensorData = lastSensorDataList.removeFirst()
				if (lastSensorData.timestamp > lastDataUpdate) {
					bufferToRead = lastSensorData.response
					lastDataUpdate = lastSensorData.timestamp
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
}