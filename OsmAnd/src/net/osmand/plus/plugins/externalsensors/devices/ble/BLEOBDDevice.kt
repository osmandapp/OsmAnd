package net.osmand.plus.plugins.externalsensors.devices.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import net.osmand.PlatformUtil
import net.osmand.plus.plugins.externalsensors.BleDeviceUuidFinder
import net.osmand.plus.plugins.externalsensors.BleDeviceUuidFinder.DeviceFoundCallback
import net.osmand.plus.plugins.externalsensors.DeviceType
import net.osmand.plus.plugins.externalsensors.GattAttributes
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionState
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEOBDSensor
import net.osmand.util.Algorithms
import okio.Buffer
import okio.IOException
import okio.Sink
import okio.Source
import okio.Timeout
import kotlin.math.min

class BLEOBDDevice(bluetoothAdapter: BluetoothAdapter, deviceId: String) :
	BLEAbstractDevice(bluetoothAdapter, deviceId), Source, Sink {
	private val log = PlatformUtil.getLog("BLEAbstractDevice")

	var response: String = ""
	var uuid: String? = null

	private var bufferToRead: String? = null
	private var deviceReadyListener: DeviceReadyListener? = null
	var isReady = false
		private set
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
		var readCharacteristicSet = false
		if (status == BluetoothGatt.GATT_SUCCESS) {
			for (service in gatt.services) {
				if (Algorithms.stringsEqual(service.uuid.toString(), uuid)) {
					for (characteristic in service.characteristics) {
						val characteristicProp = characteristic.properties
						val writeDescriptor =
							characteristic.getDescriptor(GattAttributes.UUID_CHARACTERISTIC_CLIENT_CONFIG)
						if ((characteristicProp and BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0 && writeDescriptor != null) {
							bleReadSensor.requestCharacteristic(characteristic)
							readCharacteristicSet = true
						}
						if ((characteristicProp and BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
							writeCharacteristic = characteristic
						}
						if (readCharacteristicSet && writeCharacteristic != null) {
							return
						}
					}
				}
			}

		}
	}

	override fun onDescriptorWriteCompleted() {
		super.onDescriptorWriteCompleted()
		isReady = writeCharacteristic != null && bleReadSensor.isReadCharacteristicsSet()
		deviceReadyListener?.onDeviceReadyStateChange(true)
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

	private fun baseConnectImpl(context: Context, activity: Activity?) {
		super.connect(context, activity)
	}

	override fun connect(context: Context, activity: Activity?): Boolean {
		if (uuid == null) {
			currentState = DeviceConnectionState.CONNECTING
			val deviceFinder = BleDeviceUuidFinder(deviceId, object : DeviceFoundCallback {
				override fun onDeviceFound(uuid: String?) {
					currentState = DeviceConnectionState.DISCONNECTED
					if (uuid != null) {
						this@BLEOBDDevice.uuid = uuid
						baseConnectImpl(context, activity)
					}
				}
			})
			deviceFinder.startScanning()
		} else {
			baseConnectImpl(context, activity)
		}
		return true
	}
}