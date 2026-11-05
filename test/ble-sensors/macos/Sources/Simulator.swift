import CoreBluetooth
import Foundation

enum SimulatorStatus: Equatable {
	case stopped
	case waitingForBluetooth(String)
	case starting
	case advertising
	case failed(String)
}

final class Simulator: NSObject, ObservableObject, CBPeripheralManagerDelegate {
	private static let WHEEL_CIRCUMFERENCE_M = 2.086
	private static let BLOOD_PRESSURE_PERIOD_TICKS = 20
	private static let MAX_QUEUED_VALUES = 20
	private static let MAX_LOG_LINES = 200

	@Published private(set) var status = SimulatorStatus.stopped
	@Published private(set) var config = SimulatorConfig()
	@Published private(set) var readings = Readings()
	@Published private(set) var subscriptions: [CBUUID: Set<UUID>] = [:]
	@Published private(set) var log: [String] = []
	var onTick: ((Int) -> Void)?

	private var manager: CBPeripheralManager!
	private var characteristics: [CBUUID: CBMutableCharacteristic] = [:]
	private var pendingServices = 0
	private var pending: [(CBMutableCharacteristic, Data)] = []
	private var running = false
	private var tick = 0
	private var totalDistanceM = 0.0
	private var wheelRevolutions = 0.0
	private var crankRevolutions = 0.0
	private var timer: Timer?
	private var activity: NSObjectProtocol?
	private let timeFormatter: DateFormatter = {
		let formatter = DateFormatter()
		formatter.dateFormat = "HH:mm:ss"
		return formatter
	}()

	override init() {
		super.init()
		manager = CBPeripheralManager(delegate: self, queue: nil)
	}

	var isRunning: Bool { running }

	var subscriberCount: Int { Set(subscriptions.values.flatMap { $0 }).count }

	func start(_ config: SimulatorConfig) {
		guard !running, !config.sensors.isEmpty else { return }
		self.config = config
		running = true
		tick = 0
		totalDistanceM = 0
		wheelRevolutions = 0
		crankRevolutions = 0
		readings = Readings()
		// keep macOS from throttling the 1 s timer (App Nap) while the app is in the background
		activity = ProcessInfo.processInfo.beginActivity(
			options: [.userInitiated, .latencyCritical, .idleSystemSleepDisabled], reason: "BLE sensor simulator")
		if manager.state == .poweredOn {
			publishServices()
		} else {
			status = .waitingForBluetooth(describe(manager.state))
		}
	}

	func stop() {
		guard running else { return }
		running = false
		timer?.invalidate()
		timer = nil
		manager.stopAdvertising()
		manager.removeAllServices()
		characteristics.removeAll()
		pending.removeAll()
		subscriptions.removeAll()
		if let activity { ProcessInfo.processInfo.endActivity(activity) }
		activity = nil
		status = .stopped
		addLog("Stopped")
	}

	private func addLog(_ message: String) {
		print(message)
		log.append("\(timeFormatter.string(from: Date())) \(message)")
		if log.count > Self.MAX_LOG_LINES { log.removeFirst(log.count - Self.MAX_LOG_LINES) }
	}

	private func describe(_ state: CBManagerState) -> String {
		switch state {
		case .poweredOff: return "Bluetooth is off"
		case .unauthorized: return "Bluetooth access denied: allow it in System Settings > Privacy & Security > Bluetooth"
		case .unsupported: return "Bluetooth LE peripheral mode is not supported"
		default: return "Waiting for Bluetooth"
		}
	}

	func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
		guard running else { return }
		if peripheral.state == .poweredOn {
			publishServices()
		} else {
			timer?.invalidate()
			characteristics.removeAll()
			subscriptions.removeAll()
			status = .waitingForBluetooth(describe(peripheral.state))
			addLog(describe(peripheral.state))
		}
	}

	private func publishServices() {
		status = .starting
		manager.removeAllServices()
		characteristics.removeAll()
		pendingServices = config.sensors.count
		config.sensors.forEach { manager.add(buildService($0)) }
	}

	private func measurement(_ uuid: CBUUID, indicate: Bool = false) -> CBMutableCharacteristic {
		let characteristic = CBMutableCharacteristic(type: uuid, properties: indicate ? [.indicate] : [.notify], value: nil, permissions: [.readable])
		characteristics[uuid] = characteristic
		return characteristic
	}

	private func readable(_ uuid: CBUUID, _ bytes: [UInt8]) -> CBMutableCharacteristic {
		CBMutableCharacteristic(type: uuid, properties: [.read], value: Data(bytes), permissions: [.readable])
	}

	private func buildService(_ sensor: Sensor) -> CBMutableService {
		let service = CBMutableService(type: sensor.uuid, primary: true)
		let options = config.options
		switch sensor {
		case .hr:
			service.characteristics = [measurement(Gatt.HR_MEASUREMENT), readable(Gatt.BODY_SENSOR_LOCATION, [0x01])] // chest
		case .rsc:
			let feature = Payloads.runningFeature(strideLength: options.contains(.runningStrideLength),
												  totalDistance: options.contains(.runningTotalDistance))
			service.characteristics = [measurement(Gatt.RSC_MEASUREMENT), readable(Gatt.RSC_FEATURE, feature)]
		case .csc:
			service.characteristics = [measurement(Gatt.CSC_MEASUREMENT), readable(Gatt.CSC_FEATURE, uint16(0x0003))]
		case .cp:
			service.characteristics = [measurement(Gatt.CYCLING_POWER_MEASUREMENT),
									   readable(Gatt.CYCLING_POWER_FEATURE, uint32(0x00000008)),
									   readable(Gatt.SENSOR_LOCATION, [0x05])] // left crank
		case .ht:
			service.characteristics = [measurement(Gatt.TEMPERATURE_MEASUREMENT, indicate: options.contains(.temperatureIndication))]
		case .bp:
			service.characteristics = [measurement(Gatt.BLOOD_PRESSURE_MEASUREMENT, indicate: options.contains(.bloodPressureIndication)),
									   measurement(Gatt.INTERMEDIATE_CUFF_PRESSURE),
									   readable(Gatt.BLOOD_PRESSURE_FEATURE, uint16(0x0000))]
		}
		return service
	}

	func peripheralManager(_ peripheral: CBPeripheralManager, didAdd service: CBService, error: Error?) {
		guard running else { return }
		if let error {
			addLog("Failed to add service \(service.uuid): \(error.localizedDescription)")
		} else {
			addLog("Added service \(service.uuid)")
		}
		pendingServices -= 1
		if pendingServices == 0 { startAdvertising() }
	}

	private func startAdvertising() {
		let advertised = config.advertiseMode.advertised(config.sensors).map(\.uuid)
		var data: [String: Any] = [CBAdvertisementDataLocalNameKey: config.name]
		if !advertised.isEmpty { data[CBAdvertisementDataServiceUUIDsKey] = advertised }
		manager.startAdvertising(data)
		addLog("Advertising '\(config.name)' with services \(advertised)")
	}

	func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager, error: Error?) {
		guard running else { return }
		if let error {
			status = .failed(error.localizedDescription)
			addLog("Advertising failed: \(error.localizedDescription)")
			return
		}
		status = .advertising
		addLog("Advertising started")
		timer?.invalidate()
		timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in self?.onTimer() }
	}

	func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didSubscribeTo characteristic: CBCharacteristic) {
		subscriptions[characteristic.uuid, default: []].insert(central.identifier)
		addLog("\(central.identifier.uuidString.prefix(8)) subscribed to \(characteristic.uuid)")
	}

	func peripheralManager(_ peripheral: CBPeripheralManager, central: CBCentral, didUnsubscribeFrom characteristic: CBCharacteristic) {
		subscriptions[characteristic.uuid]?.remove(central.identifier)
		if subscriptions[characteristic.uuid]?.isEmpty == true { subscriptions[characteristic.uuid] = nil }
		addLog("\(central.identifier.uuidString.prefix(8)) unsubscribed from \(characteristic.uuid)")
	}

	private func send(_ uuid: CBUUID, _ bytes: [UInt8]) {
		guard let characteristic = characteristics[uuid], !(characteristic.subscribedCentrals ?? []).isEmpty else { return }
		if pending.count >= Self.MAX_QUEUED_VALUES { pending.removeFirst() }
		pending.append((characteristic, Data(bytes)))
		flush()
	}

	// updateValue returns false when the transmit queue is full, the rest is resent from peripheralManagerIsReady
	private func flush() {
		while let (characteristic, data) = pending.first {
			guard manager.updateValue(data, for: characteristic, onSubscribedCentrals: nil) else { return }
			pending.removeFirst()
		}
	}

	func peripheralManagerIsReady(toUpdateSubscribers peripheral: CBPeripheralManager) {
		flush()
	}

	// time of the last whole revolution in 1/1024 s, wrapping at 64 s as the specification defines
	private func eventTime(_ revolutions: Double, revolutionsPerSecond: Double) -> Int {
		let sinceLastRevolution = revolutionsPerSecond > 0 ? (revolutions - revolutions.rounded(.down)) / revolutionsPerSecond : 0
		return Int((Double(tick) - sinceLastRevolution) * 1024) & 0xFFFF
	}

	private func nextReadings() -> Readings {
		let t = Double(tick)
		let speedMs = 3.0 + sin(t / 15)
		let cadence = 170 + Int(5 * sin(t / 7))
		let bikeSpeedMs = 7.0 + 2 * sin(t / 25)
		let crankCadence = 85 + Int(10 * sin(t / 12))
		totalDistanceM += speedMs
		wheelRevolutions += bikeSpeedMs / Self.WHEEL_CIRCUMFERENCE_M
		crankRevolutions += Double(crankCadence) / 60
		return Readings(
			heartRate: Int(120 + 30 * sin(t / 20)),
			speedMs: speedMs,
			cadence: cadence,
			strideLengthM: speedMs * 60 / Double(cadence),
			totalDistanceM: totalDistanceM,
			bikeSpeedMs: bikeSpeedMs,
			crankCadence: crankCadence,
			powerW: 200 + Int(60 * sin(t / 10)),
			temperatureC: 36.6 + 0.3 * sin(t / 30),
			systolic: 120 + Int(6 * sin(t / 40)),
			diastolic: 80 + Int(4 * sin(t / 40)),
			// deflating cuff, restarted every blood pressure measurement
			cuffPressure: 160 - (tick % Self.BLOOD_PRESSURE_PERIOD_TICKS) * 100 / Self.BLOOD_PRESSURE_PERIOD_TICKS
		)
	}

	private func onTimer() {
		tick += 1
		readings = nextReadings()
		let options = config.options
		let crankEventTime = eventTime(crankRevolutions, revolutionsPerSecond: Double(readings.crankCadence) / 60)
		send(Gatt.HR_MEASUREMENT, Payloads.heartRate(readings.heartRate))
		send(Gatt.RSC_MEASUREMENT, Payloads.runningSpeedCadence(
			speedMs: readings.speedMs, cadence: readings.cadence,
			strideLengthM: options.contains(.runningStrideLength) ? readings.strideLengthM : nil,
			totalDistanceM: options.contains(.runningTotalDistance) ? readings.totalDistanceM : nil))
		send(Gatt.CSC_MEASUREMENT, Payloads.cyclingSpeedCadence(
			wheelRevolutions: Int(wheelRevolutions),
			wheelEventTime: eventTime(wheelRevolutions, revolutionsPerSecond: readings.bikeSpeedMs / Self.WHEEL_CIRCUMFERENCE_M),
			crankRevolutions: Int(crankRevolutions) & 0xFFFF, crankEventTime: crankEventTime))
		send(Gatt.CYCLING_POWER_MEASUREMENT, Payloads.cyclingPower(
			powerW: readings.powerW, crankRevolutions: Int(crankRevolutions) & 0xFFFF, crankEventTime: crankEventTime))
		send(Gatt.TEMPERATURE_MEASUREMENT, Payloads.temperature(readings.temperatureC))
		send(Gatt.INTERMEDIATE_CUFF_PRESSURE, Payloads.intermediateCuffPressure(readings.cuffPressure, pulseRate: readings.heartRate))
		if tick % Self.BLOOD_PRESSURE_PERIOD_TICKS == 0 {
			let meanArterial = readings.diastolic + (readings.systolic - readings.diastolic) / 3
			send(Gatt.BLOOD_PRESSURE_MEASUREMENT, Payloads.bloodPressure(
				systolic: readings.systolic, diastolic: readings.diastolic, meanArterial: meanArterial, pulseRate: readings.heartRate))
		}
		onTick?(tick)
	}
}