package net.osmand.sensors.simulate

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.floor
import kotlin.math.sin

data class SimulatorConfig(
    // in the advertisement order, Battery is added to them
    val services: List<SensorService>,
    val advertiseMode: AdvertiseMode,
    val options: Set<SensorOption>
) {
    val bluetoothName: String get() = SensorService.bluetoothName(services)
    val publishedServices: List<SensorService> get() = services + SensorService.BATTERY
}

data class Readings(
    val heartRate: Int,
    val speedMs: Double,
    val cadence: Int,
    val strideLengthM: Double,
    val totalDistanceM: Double,
    val bikeSpeedMs: Double,
    val crankCadence: Int,
    val powerW: Int,
    val temperatureC: Double,
    val systolic: Int,
    val diastolic: Int,
    val cuffPressure: Int,
    val batteryLevel: Int
)

data class CentralInfo(val address: String, val subscriptions: Map<UUID, String>)

data class SimulatorState(
    val running: Boolean = false,
    val status: String = "Stopped",
    val config: SimulatorConfig? = null,
    val deviceName: String? = null,
    val centrals: List<CentralInfo> = emptyList(),
    val readings: Readings? = null,
    val log: List<String> = emptyList()
)

/**
 * BLE peripheral that publishes the GATT services of the selected sensors and notifies
 * subscribed centrals once a second, like a real multi-service sensor does.
 * All Bluetooth callbacks are moved to the main thread.
 */
@SuppressLint("MissingPermission")
class BleSensorSimulator(context: Context) {

    companion object {
        private const val TAG = "BleSensorSim"
        private const val TICK_MS = 1000L
        private const val MAX_QUEUED_VALUES = 20
        private const val CCCD_IMPROPERLY_CONFIGURED = 0xFD
        private const val MAX_LOG_LINES = 200
        private const val WHEEL_CIRCUMFERENCE_M = 2.086
        private const val BLOOD_PRESSURE_PERIOD_TICKS = 20
        private const val NAME_CHECK_MS = 100L
        private const val NAME_CHECK_ATTEMPTS = 30
        private const val PREFS = "simulator"
        private const val KEY_ORIGINAL_NAME = "original_bluetooth_name"
    }

    private class Central(val device: BluetoothDevice) {
        val subscriptions = mutableMapOf<UUID, Boolean>() // characteristic -> indication
        val queue = ArrayDeque<Pair<BluetoothGattCharacteristic, ByteArray>>()
        var sending = false
    }

    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(SimulatorState())
    val state: StateFlow<SimulatorState> = _state

    private var config: SimulatorConfig? = null
    private var gattServer: BluetoothGattServer? = null
    private val pendingServices = ArrayDeque<BluetoothGattService>()
    private val characteristics = mutableMapOf<UUID, BluetoothGattCharacteristic>()
    private val centrals = linkedMapOf<String, Central>()
    private var advertising = false
    private var advertiseWithName = true
    private var tick = 0
    private var totalDistanceM = 0.0
    private var wheelRevolutions = 0.0
    private var crankRevolutions = 0.0
    private var nameCheckAttempts = 0
    private var readings = nextReadings()

    private val tickRunnable = object : Runnable {
        override fun run() {
            onTick()
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val nameCheckRunnable = Runnable { startAdvertisingWhenNamed() }

    val isBluetoothEnabled: Boolean get() = bluetoothManager?.adapter?.isEnabled == true

    init {
        // the process may have been killed while advertising under the simulated name
        try {
            restoreAdapterName()
        } catch (e: SecurityException) {
            Log.d(TAG, "Bluetooth name will be restored once the permission is granted")
        }
    }

    fun start(config: SimulatorConfig) {
        if (this.config != null) return
        val adapter = bluetoothManager?.adapter
        if (adapter == null || !adapter.isEnabled) {
            setStatus("Bluetooth is off")
            return
        }
        if (adapter.bluetoothLeAdvertiser == null) {
            setStatus("This device can't advertise as a BLE peripheral")
            return
        }
        this.config = config
        tick = 0
        totalDistanceM = 0.0
        wheelRevolutions = 0.0
        crankRevolutions = 0.0
        readings = nextReadings()
        _state.update { it.copy(running = true, config = config, deviceName = null, readings = readings) }
        val options = config.services.flatMap { service -> service.options.map { "${it.title}: ${if (it in config.options) "on" else "off"}" } }
        log("Starting ${config.services.joinToString { it.sensorType }}, advertise: ${config.advertiseMode.title}" +
                options.joinToString("") { ", $it" })
        renameAdapter(config.bluetoothName)

        val server = bluetoothManager?.openGattServer(appContext, gattCallback)
        if (server == null) {
            stop("Can't open GATT server")
            return
        }
        gattServer = server
        pendingServices.clear()
        config.publishedServices.forEach { pendingServices.add(buildService(it, config)) }
        addNextService()
    }

    fun stop(reason: String = "Stopped") {
        if (config == null) return
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacks(nameCheckRunnable)
        if (advertising) {
            bluetoothManager?.adapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            advertising = false
        }
        gattServer?.let { server ->
            centrals.values.forEach { server.cancelConnection(it.device) }
            server.close()
        }
        gattServer = null
        centrals.clear()
        characteristics.clear()
        pendingServices.clear()
        config = null
        log(reason)
        restoreAdapterName()
        _state.update { it.copy(running = false, status = reason, centrals = emptyList(), deviceName = null) }
    }

    // the advertised name is always the adapter name, so the whole phone is renamed while simulating
    private fun renameAdapter(name: String) {
        val adapter = bluetoothManager?.adapter ?: return
        if (prefs.getString(KEY_ORIGINAL_NAME, null) == null) {
            prefs.edit().putString(KEY_ORIGINAL_NAME, adapter.name).apply()
        }
        if (adapter.name != name && !adapter.setName(name)) {
            log("Can't rename Bluetooth to \"$name\"")
        }
    }

    private fun restoreAdapterName() {
        val original = prefs.getString(KEY_ORIGINAL_NAME, null) ?: return
        val adapter = bluetoothManager?.adapter ?: return
        if (adapter.name == original || adapter.setName(original)) {
            prefs.edit().remove(KEY_ORIGINAL_NAME).apply()
            log("Bluetooth name restored to \"$original\"")
        } else {
            log("Can't restore Bluetooth name \"$original\", will retry on the next start")
        }
    }

    // the new name is applied asynchronously and the advertiser takes the name current at start
    private fun startAdvertisingWhenNamed() {
        val config = config ?: return
        val name = bluetoothManager?.adapter?.name
        if (name == config.bluetoothName || nameCheckAttempts >= NAME_CHECK_ATTEMPTS) {
            if (name != config.bluetoothName) {
                log("Bluetooth name is still \"$name\"")
            }
            _state.update { it.copy(deviceName = name) }
            startAdvertising(includeName = true)
        } else {
            nameCheckAttempts++
            handler.postDelayed(nameCheckRunnable, NAME_CHECK_MS)
        }
    }

    private fun buildService(service: SensorService, config: SimulatorConfig): BluetoothGattService {
        val gattService = BluetoothGattService(service.uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        when (service) {
            SensorService.HEART_RATE -> {
                gattService.addCharacteristic(notifying(GattUuids.HEART_RATE_MEASUREMENT, BluetoothGattCharacteristic.PROPERTY_NOTIFY))
                gattService.addCharacteristic(readable(GattUuids.BODY_SENSOR_LOCATION))
            }
            SensorService.RUNNING_SPEED_CADENCE -> {
                gattService.addCharacteristic(notifying(GattUuids.RSC_MEASUREMENT, BluetoothGattCharacteristic.PROPERTY_NOTIFY))
                gattService.addCharacteristic(readable(GattUuids.RSC_FEATURE))
            }
            SensorService.CYCLING_SPEED_CADENCE -> {
                gattService.addCharacteristic(notifying(GattUuids.CSC_MEASUREMENT, BluetoothGattCharacteristic.PROPERTY_NOTIFY))
                gattService.addCharacteristic(readable(GattUuids.CSC_FEATURE))
            }
            SensorService.CYCLING_POWER -> {
                gattService.addCharacteristic(notifying(GattUuids.CYCLING_POWER_MEASUREMENT, BluetoothGattCharacteristic.PROPERTY_NOTIFY))
                gattService.addCharacteristic(readable(GattUuids.CYCLING_POWER_FEATURE))
                gattService.addCharacteristic(readable(GattUuids.SENSOR_LOCATION))
            }
            SensorService.HEALTH_THERMOMETER -> {
                gattService.addCharacteristic(notifying(GattUuids.TEMPERATURE_MEASUREMENT, measurementProperty(config, SensorOption.TEMPERATURE_INDICATION)))
            }
            SensorService.BLOOD_PRESSURE -> {
                gattService.addCharacteristic(notifying(GattUuids.BLOOD_PRESSURE_MEASUREMENT, measurementProperty(config, SensorOption.BLOOD_PRESSURE_INDICATION)))
                gattService.addCharacteristic(notifying(GattUuids.INTERMEDIATE_CUFF_PRESSURE, BluetoothGattCharacteristic.PROPERTY_NOTIFY))
                gattService.addCharacteristic(readable(GattUuids.BLOOD_PRESSURE_FEATURE))
            }
            SensorService.BATTERY -> {
                gattService.addCharacteristic(notifying(GattUuids.BATTERY_LEVEL,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ))
            }
        }
        return gattService
    }

    private fun measurementProperty(config: SimulatorConfig, indication: SensorOption): Int = if (indication in config.options) {
        BluetoothGattCharacteristic.PROPERTY_INDICATE
    } else {
        BluetoothGattCharacteristic.PROPERTY_NOTIFY
    }

    private fun notifying(uuid: UUID, properties: Int): BluetoothGattCharacteristic {
        val characteristic = BluetoothGattCharacteristic(uuid, properties, BluetoothGattCharacteristic.PERMISSION_READ)
        characteristic.addDescriptor(BluetoothGattDescriptor(GattUuids.CLIENT_CHARACTERISTIC_CONFIG,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE))
        characteristics[uuid] = characteristic
        return characteristic
    }

    private fun readable(uuid: UUID): BluetoothGattCharacteristic =
        BluetoothGattCharacteristic(uuid, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)

    // GATT server accepts the next service only after onServiceAdded of the previous one
    private fun addNextService() {
        val service = pendingServices.removeFirstOrNull()
        if (service == null) {
            nameCheckAttempts = 0
            startAdvertisingWhenNamed()
            return
        }
        if (gattServer?.addService(service) != true) {
            stop("Can't add service ${service.uuid}")
        }
    }

    private fun startAdvertising(includeName: Boolean) {
        val config = config ?: return
        val advertiser = bluetoothManager?.adapter?.bluetoothLeAdvertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        val advertised = config.advertiseMode.advertisedServices(config.services)
        val data = AdvertiseData.Builder().setIncludeDeviceName(false)
        advertised.forEach { data.addServiceUuid(ParcelUuid(it.uuid)) }
        val scanResponse = AdvertiseData.Builder().setIncludeDeviceName(includeName).build()
        advertiseWithName = includeName
        advertiser.startAdvertising(settings, data.build(), scanResponse, advertiseCallback)
        log("Advertising services ${advertised.joinToString { it.title }}" + if (includeName) ", with name" else ", without name")
    }


    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            handler.post {
                advertising = true
                setStatus("Advertising")
                handler.removeCallbacks(tickRunnable)
                handler.post(tickRunnable)
            }
        }

        override fun onStartFailure(errorCode: Int) {
            handler.post {
                if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE && advertiseWithName) {
                    log("Device name doesn't fit into the scan response, advertising without it")
                    startAdvertising(includeName = false)
                } else {
                    stop("Advertising failed, error $errorCode")
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattServerCallback() {
        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            handler.post {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    log("Published service ${service.uuid.shortName()}")
                    addNextService()
                } else {
                    stop("Can't add service ${service.uuid.shortName()}, status $status")
                }
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            handler.post {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    centrals.getOrPut(device.address) { Central(device) }
                    log("${device.address} connected")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    centrals.remove(device.address)
                    log("${device.address} disconnected, status $status")
                }
                publishCentrals()
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                                 characteristic: BluetoothGattCharacteristic) {
            handler.post {
                val value = currentValue(characteristic.uuid)
                if (value == null) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_READ_NOT_PERMITTED, offset, null)
                } else {
                    val from = offset.coerceAtMost(value.size)
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value.copyOfRange(from, value.size))
                }
                log("${device.address} read ${characteristic.uuid.shortName()}")
            }
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                             descriptor: BluetoothGattDescriptor) {
            handler.post {
                val indication = centrals[device.address]?.subscriptions?.get(descriptor.characteristic.uuid)
                val value = when (indication) {
                    true -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    false -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    null -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int, descriptor: BluetoothGattDescriptor,
                                              preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            handler.post {
                val status = onCccdWrite(device, descriptor, value)
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, status, offset, value)
                }
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            handler.post {
                val central = centrals[device.address] ?: return@post
                central.sending = false
                sendNext(central)
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            handler.post { log("${device.address} MTU $mtu") }
        }
    }

    private fun onCccdWrite(device: BluetoothDevice, descriptor: BluetoothGattDescriptor, value: ByteArray?): Int {
        if (descriptor.uuid != GattUuids.CLIENT_CHARACTERISTIC_CONFIG || value == null || value.isEmpty()) {
            return BluetoothGatt.GATT_WRITE_NOT_PERMITTED
        }
        val central = centrals.getOrPut(device.address) { Central(device) }
        val characteristic = descriptor.characteristic
        val name = characteristic.uuid.shortName()
        val notify = value[0].toInt() and 0x01 != 0
        val indicate = value[0].toInt() and 0x02 != 0
        val properties = characteristic.properties
        // a strict sensor rejects a mode its characteristic doesn't support
        if (notify && properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0
            || indicate && properties and BluetoothGattCharacteristic.PROPERTY_INDICATE == 0) {
            log("${device.address} asked ${if (notify) "notification" else "indication"} for $name, rejected: not supported")
            return CCCD_IMPROPERLY_CONFIGURED
        }
        if (notify || indicate) {
            central.subscriptions[characteristic.uuid] = indicate
            log("${device.address} subscribed to $name (${if (indicate) "indication" else "notification"})")
        } else {
            central.subscriptions.remove(characteristic.uuid)
            log("${device.address} unsubscribed from $name")
        }
        publishCentrals()
        return BluetoothGatt.GATT_SUCCESS
    }

    private fun onTick() {
        tick++
        readings = nextReadings()
        _state.update { it.copy(readings = readings) }
        send(GattUuids.HEART_RATE_MEASUREMENT, SensorPayloads.heartRate(readings.heartRate))
        val options = config?.options.orEmpty()
        send(GattUuids.RSC_MEASUREMENT, SensorPayloads.runningSpeedCadence(
            readings.speedMs, readings.cadence,
            readings.strideLengthM.takeIf { SensorOption.RUNNING_STRIDE_LENGTH in options },
            readings.totalDistanceM.takeIf { SensorOption.RUNNING_TOTAL_DISTANCE in options }))
        send(GattUuids.CSC_MEASUREMENT, SensorPayloads.cyclingSpeedCadence(
            wheelRevolutions.toLong(), eventTime(wheelRevolutions, readings.bikeSpeedMs / WHEEL_CIRCUMFERENCE_M),
            crankRevolutions.toInt() and 0xFFFF, eventTime(crankRevolutions, readings.crankCadence / 60.0)))
        send(GattUuids.CYCLING_POWER_MEASUREMENT, SensorPayloads.cyclingPower(
            readings.powerW, crankRevolutions.toInt() and 0xFFFF, eventTime(crankRevolutions, readings.crankCadence / 60.0)))
        send(GattUuids.TEMPERATURE_MEASUREMENT, SensorPayloads.temperature(readings.temperatureC))
        send(GattUuids.INTERMEDIATE_CUFF_PRESSURE, SensorPayloads.intermediateCuffPressure(readings.cuffPressure, readings.heartRate))
        if (tick % BLOOD_PRESSURE_PERIOD_TICKS == 0) {
            val meanArterial = readings.diastolic + (readings.systolic - readings.diastolic) / 3
            send(GattUuids.BLOOD_PRESSURE_MEASUREMENT, SensorPayloads.bloodPressure(
                readings.systolic, readings.diastolic, meanArterial, readings.heartRate))
        }
        if (tick % 60 == 0) {
            send(GattUuids.BATTERY_LEVEL, SensorPayloads.batteryLevel(readings.batteryLevel))
        }
    }

    // time of the last whole revolution in 1/1024 s, wrapping at 64 s as the specification defines
    private fun eventTime(revolutions: Double, revolutionsPerSecond: Double): Int {
        val sinceLastRevolution = if (revolutionsPerSecond > 0) (revolutions - floor(revolutions)) / revolutionsPerSecond else 0.0
        return ((tick - sinceLastRevolution) * 1024).toLong().toInt() and 0xFFFF
    }

    private fun nextReadings(): Readings {
        val t = tick.toDouble()
        val seconds = TICK_MS / 1000.0
        val speedMs = 3.0 + sin(t / 15)
        val cadence = 170 + (5 * sin(t / 7)).toInt()
        val bikeSpeedMs = 7.0 + 2 * sin(t / 25)
        val crankCadence = 85 + (10 * sin(t / 12)).toInt()
        totalDistanceM += speedMs * seconds
        wheelRevolutions += bikeSpeedMs * seconds / WHEEL_CIRCUMFERENCE_M
        crankRevolutions += crankCadence * seconds / 60
        return Readings(
            heartRate = (120 + 30 * sin(t / 20)).toInt(),
            speedMs = speedMs,
            cadence = cadence,
            strideLengthM = speedMs * 60 / cadence,
            totalDistanceM = totalDistanceM,
            bikeSpeedMs = bikeSpeedMs,
            crankCadence = crankCadence,
            powerW = 200 + (60 * sin(t / 10)).toInt(),
            temperatureC = 36.6 + 0.3 * sin(t / 30),
            systolic = 120 + (6 * sin(t / 40)).toInt(),
            diastolic = 80 + (4 * sin(t / 40)).toInt(),
            // deflating cuff, restarted every blood pressure measurement
            cuffPressure = 160 - (tick % BLOOD_PRESSURE_PERIOD_TICKS) * 100 / BLOOD_PRESSURE_PERIOD_TICKS,
            batteryLevel = (95 - tick / 60).coerceAtLeast(5)
        )
    }

    private fun currentValue(uuid: UUID): ByteArray? = when (uuid) {
        GattUuids.BODY_SENSOR_LOCATION -> SensorPayloads.bodySensorLocation()
        GattUuids.RSC_FEATURE -> config?.options.orEmpty().let {
            SensorPayloads.runningFeature(SensorOption.RUNNING_STRIDE_LENGTH in it, SensorOption.RUNNING_TOTAL_DISTANCE in it)
        }
        GattUuids.CSC_FEATURE -> SensorPayloads.cyclingSpeedCadenceFeature()
        GattUuids.CYCLING_POWER_FEATURE -> SensorPayloads.cyclingPowerFeature()
        GattUuids.SENSOR_LOCATION -> SensorPayloads.cyclingPowerSensorLocation()
        GattUuids.BLOOD_PRESSURE_FEATURE -> SensorPayloads.bloodPressureFeature()
        GattUuids.BATTERY_LEVEL -> SensorPayloads.batteryLevel(readings.batteryLevel)
        else -> null
    }

    private fun send(uuid: UUID, value: ByteArray) {
        val characteristic = characteristics[uuid] ?: return
        for (central in centrals.values) {
            if (central.subscriptions.containsKey(uuid)) {
                if (central.queue.size >= MAX_QUEUED_VALUES) {
                    central.queue.removeFirst()
                }
                central.queue.addLast(characteristic to value)
                sendNext(central)
            }
        }
    }

    // only one notification may be in flight per connection, the next one goes from onNotificationSent
    @Suppress("DEPRECATION")
    private fun sendNext(central: Central) {
        val server = gattServer ?: return
        if (central.sending) return
        val (characteristic, value) = central.queue.removeFirstOrNull() ?: return
        val indicate = central.subscriptions[characteristic.uuid] ?: return
        val sent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            server.notifyCharacteristicChanged(central.device, characteristic, indicate, value) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.value = value
            server.notifyCharacteristicChanged(central.device, characteristic, indicate)
        }
        if (sent) {
            central.sending = true
        } else {
            log("${central.device.address} failed to send ${characteristic.uuid.shortName()}")
        }
    }

    private fun publishCentrals() {
        val list = centrals.values.map { central ->
            CentralInfo(central.device.address, central.subscriptions.mapValues { if (it.value) "indication" else "notification" })
        }
        _state.update { it.copy(centrals = list) }
    }

    private fun setStatus(status: String) {
        log(status)
        _state.update { it.copy(status = status) }
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        val line = "${timeFormat.format(Date())} $message"
        _state.update { it.copy(log = (listOf(line) + it.log).take(MAX_LOG_LINES)) }
    }
}

fun UUID.shortName(): String {
    val value = toString()
    return if (value.endsWith("-0000-1000-8000-00805f9b34fb")) "0x" + value.substring(4, 8).uppercase() else value
}