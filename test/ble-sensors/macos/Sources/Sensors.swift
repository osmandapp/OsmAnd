import CoreBluetooth
import Foundation

func uuid16(_ value: String) -> CBUUID { CBUUID(string: value) }

enum Gatt {
	static let HR_MEASUREMENT = uuid16("2A37"), BODY_SENSOR_LOCATION = uuid16("2A38")
	static let RSC_MEASUREMENT = uuid16("2A53"), RSC_FEATURE = uuid16("2A54")
	static let CSC_MEASUREMENT = uuid16("2A5B"), CSC_FEATURE = uuid16("2A5C")
	static let CYCLING_POWER_MEASUREMENT = uuid16("2A63"), CYCLING_POWER_FEATURE = uuid16("2A65"), SENSOR_LOCATION = uuid16("2A5D")
	static let TEMPERATURE_MEASUREMENT = uuid16("2A1C")
	static let BLOOD_PRESSURE_MEASUREMENT = uuid16("2A35"), INTERMEDIATE_CUFF_PRESSURE = uuid16("2A36"), BLOOD_PRESSURE_FEATURE = uuid16("2A49")
}

/// [sensorType] names the simulator when it is the only selected sensor, [code] in combinations.
enum Sensor: String, CaseIterable, Identifiable {
	case hr, rsc, csc, cp, ht, bp

	var id: String { rawValue }

	var uuid: CBUUID {
		switch self {
		case .hr: return uuid16("180D")
		case .rsc: return uuid16("1814")
		case .csc: return uuid16("1816")
		case .cp: return uuid16("1818")
		case .ht: return uuid16("1809")
		case .bp: return uuid16("1810")
		}
	}

	var title: String {
		switch self {
		case .hr: return "Heart Rate (0x180D)"
		case .rsc: return "Running Speed and Cadence (0x1814)"
		case .csc: return "Cycling Speed and Cadence (0x1816)"
		case .cp: return "Cycling Power (0x1818)"
		case .ht: return "Health Thermometer (0x1809)"
		case .bp: return "Blood Pressure (0x1810)"
		}
	}

	var sensorType: String {
		switch self {
		case .hr: return "Heart rate"
		case .rsc: return "Running"
		case .csc: return "Bike speed/cadence"
		case .cp: return "Bike power"
		case .ht: return "Thermometer"
		case .bp: return "Blood pressure"
		}
	}

	var code: String { rawValue.uppercased() }

	var options: [SensorOption] { SensorOption.allCases.filter { $0.sensor == self } }
}

enum SensorOption: String, CaseIterable, Identifiable {
	case runningStrideLength, runningTotalDistance, temperatureIndication, bloodPressureIndication

	var id: String { rawValue }

	var sensor: Sensor {
		switch self {
		case .runningStrideLength, .runningTotalDistance: return .rsc
		case .temperatureIndication: return .ht
		case .bloodPressureIndication: return .bp
		}
	}

	var title: String {
		switch self {
		case .runningStrideLength: return "Send stride length"
		case .runningTotalDistance: return "Send total distance"
		case .temperatureIndication: return "Send via indication"
		case .bloodPressureIndication: return "Send measurement via indication"
		}
	}

	var details: String {
		switch self {
		case .runningStrideLength: return "Optional field, flags bit 0"
		case .runningTotalDistance: return "Optional field, flags bit 1"
		case .temperatureIndication: return "As the specification defines, off sends notifications"
		case .bloodPressureIndication: return "As the specification defines, off sends notifications; cuff pressure is always notified"
		}
	}

	static let defaults: Set<SensorOption> = [.runningStrideLength, .runningTotalDistance]
}

/// Selections of real devices, sensors are in their advertisement order.
enum Preset: String, CaseIterable, Identifiable {
	case hrm600, momentum

	var id: String { rawValue }

	var title: String {
		switch self {
		case .hrm600: return "Garmin HRM 600"
		case .momentum: return "Sennheiser Momentum Sport"
		}
	}

	var details: String {
		switch self {
		case .hrm600: return "Running Speed and Cadence before Heart Rate (issue #25694)"
		case .momentum: return "Health Thermometer before Heart Rate"
		}
	}

	var sensors: [Sensor] {
		switch self {
		case .hrm600: return [.rsc, .hr]
		case .momentum: return [.ht, .hr]
		}
	}
}

enum AdvertiseMode: String, CaseIterable, Identifiable {
	case all, first, none

	var id: String { rawValue }

	var title: String {
		switch self {
		case .all: return "All services"
		case .first: return "First service only"
		case .none: return "No service UUIDs"
		}
	}

	// with a single sensor the first service is all of them
	func isApplicable(_ sensors: [Sensor]) -> Bool { self != .first || sensors.count > 1 }

	func advertised(_ sensors: [Sensor]) -> [Sensor] {
		switch self {
		case .all: return sensors
		case .first: return Array(sensors.prefix(1))
		case .none: return []
		}
	}
}

// Android cuts the advertised name to 26 bytes, the same limit keeps both simulators named alike
let MAX_ADVERTISED_NAME_BYTES = 26

func bluetoothName(_ sensors: [Sensor]) -> String {
	let label = sensors.count == 1 ? sensors[0].sensorType : sensors.map(\.code).joined(separator: "+")
	let name = "\(label) simulator"
	return name.utf8.count <= MAX_ADVERTISED_NAME_BYTES ? name : "\(label) sim"
}

struct SimulatorConfig {
	var sensors: [Sensor] = [.hr]
	var advertiseMode: AdvertiseMode = .all
	var options: Set<SensorOption> = SensorOption.defaults
	var customName: String?

	var name: String { customName ?? bluetoothName(sensors) }
}

struct Readings {
	var heartRate = 0
	var speedMs = 0.0
	var cadence = 0
	var strideLengthM = 0.0
	var totalDistanceM = 0.0
	var bikeSpeedMs = 0.0
	var crankCadence = 0
	var powerW = 0
	var temperatureC = 0.0
	var systolic = 0
	var diastolic = 0
	var cuffPressure = 0

	func values(of sensor: Sensor, options: Set<SensorOption>) -> [(String, String)] {
		switch sensor {
		case .hr:
			return [("Heart rate", "\(heartRate) bpm")]
		case .rsc:
			var values = [("Speed", String(format: "%.2f m/s (%.1f km/h)", speedMs, speedMs * 3.6)), ("Cadence", "\(cadence) steps/min")]
			if options.contains(.runningStrideLength) { values.append(("Stride length", String(format: "%.2f m", strideLengthM))) }
			if options.contains(.runningTotalDistance) { values.append(("Total distance", String(format: "%.0f m", totalDistanceM))) }
			return values
		case .csc:
			return [("Speed", String(format: "%.2f m/s (%.1f km/h)", bikeSpeedMs, bikeSpeedMs * 3.6)), ("Cadence", "\(crankCadence) rpm")]
		case .cp:
			return [("Power", "\(powerW) W"), ("Cadence", "\(crankCadence) rpm")]
		case .ht:
			return [("Temperature", String(format: "%.2f °C", temperatureC))]
		case .bp:
			return [("Blood pressure", "\(systolic)/\(diastolic) mmHg"), ("Cuff pressure", "\(cuffPressure) mmHg")]
		}
	}
}

func uint16(_ value: Int) -> [UInt8] { [UInt8(value & 0xFF), UInt8((value >> 8) & 0xFF)] }
func uint32(_ value: Int) -> [UInt8] { uint16(value & 0xFFFF) + uint16((value >> 16) & 0xFFFF) }

// IEEE-11073 32-bit FLOAT: 24-bit mantissa, 8-bit exponent
func ieee11073Float(_ value: Double, decimals: Int) -> [UInt8] {
	let mantissa = Int((value * pow(10, Double(decimals))).rounded())
	return [UInt8(mantissa & 0xFF), UInt8((mantissa >> 8) & 0xFF), UInt8((mantissa >> 16) & 0xFF), UInt8(bitPattern: Int8(-decimals))]
}

// IEEE-11073 16-bit SFLOAT with zero exponent: a 12-bit signed mantissa
func sfloat(_ value: Int) -> [UInt8] { uint16(min(max(value, -2046), 2046) & 0x0FFF) }
let SFLOAT_NAN = uint16(0x07FF)

/// Characteristic values encoded as defined by the Bluetooth GATT specifications.
enum Payloads {
	// flags 0x06: uint8 value, sensor contact supported and detected
	static func heartRate(_ bpm: Int) -> [UInt8] { [0x06, UInt8(min(max(bpm, 0), 255))] }

	// flags bit 0 = stride length, bit 1 = total distance, bit 2 = running;
	// speed in 1/256 m/s, stride length in 1/100 m, total distance in 1/10 m
	static func runningSpeedCadence(speedMs: Double, cadence: Int, strideLengthM: Double?, totalDistanceM: Double?) -> [UInt8] {
		let flags = 0x04 | (strideLengthM != nil ? 0x01 : 0) | (totalDistanceM != nil ? 0x02 : 0)
		return [UInt8(flags)] + uint16(Int((speedMs * 256).rounded())) + [UInt8(min(max(cadence, 0), 255))]
			+ (strideLengthM.map { uint16(Int(($0 * 100).rounded())) } ?? [])
			+ (totalDistanceM.map { uint32(Int($0 * 10)) } ?? [])
	}

	static func runningFeature(strideLength: Bool, totalDistance: Bool) -> [UInt8] {
		uint16(0x04 | (strideLength ? 0x01 : 0) | (totalDistance ? 0x02 : 0))
	}

	// flags 0x03: wheel and crank revolution data, event times in 1/1024 s
	static func cyclingSpeedCadence(wheelRevolutions: Int, wheelEventTime: Int, crankRevolutions: Int, crankEventTime: Int) -> [UInt8] {
		[0x03] + uint32(wheelRevolutions) + uint16(wheelEventTime) + uint16(crankRevolutions) + uint16(crankEventTime)
	}

	// flags 0x0020: crank revolution data, instantaneous power sint16 in W
	static func cyclingPower(powerW: Int, crankRevolutions: Int, crankEventTime: Int) -> [UInt8] {
		uint16(0x0020) + uint16(powerW) + uint16(crankRevolutions) + uint16(crankEventTime)
	}

	// flags 0x04: mmHg, pulse rate present
	static func bloodPressure(systolic: Int, diastolic: Int, meanArterial: Int, pulseRate: Int) -> [UInt8] {
		[0x04] + sfloat(systolic) + sfloat(diastolic) + sfloat(meanArterial) + sfloat(pulseRate)
	}

	static func intermediateCuffPressure(_ cuffPressure: Int, pulseRate: Int) -> [UInt8] {
		[0x04] + sfloat(cuffPressure) + SFLOAT_NAN + SFLOAT_NAN + sfloat(pulseRate)
	}

	// flags 0x00: Celsius
	static func temperature(_ celsius: Double) -> [UInt8] { [0x00] + ieee11073Float(celsius, decimals: 2) }
}