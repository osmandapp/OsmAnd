import CoreBluetooth
import SwiftUI

struct SimulatorApp: App {
	@NSApplicationDelegateAdaptor private var appDelegate: AppDelegate
	@StateObject private var simulator = Simulator()

	var body: some Scene {
		Window("BLE Sensor Simulator", id: "main") {
			ContentView(simulator: simulator)
				.frame(minWidth: 520, minHeight: 640)
		}
	}
}

final class AppDelegate: NSObject, NSApplicationDelegate {
	func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { true }
}

struct ContentView: View {
	@ObservedObject var simulator: Simulator
	@State private var sensors: [Sensor] = [.hr]
	@State private var advertiseMode = AdvertiseMode.all
	@State private var options = SensorOption.defaults
	@State private var editedSensor: Sensor?

	private var config: SimulatorConfig {
		SimulatorConfig(sensors: sensors, advertiseMode: advertiseMode, options: options)
	}

	var body: some View {
		ScrollView {
			VStack(alignment: .leading, spacing: 16) {
				settingsCard
				controls
				statusCard
				if simulator.isRunning {
					readingsCard
				}
				logCard
			}
			.padding(16)
		}
		.sheet(item: $editedSensor) { sensor in
			SensorOptionsView(sensor: sensor, options: $options)
		}
	}

	private var settingsCard: some View {
		GroupBox {
			VStack(alignment: .leading, spacing: 8) {
				ForEach(Sensor.allCases) { sensor in
					sensorRow(sensor)
				}
				Divider()
				Text("Presets").font(.headline)
				HStack {
					ForEach(Preset.allCases) { preset in
						Button(preset.title) { setSensors(preset.sensors) }
							.help(preset.details)
					}
				}
				Text("Bluetooth name: \(sensors.isEmpty ? "-" : config.name)")
					.foregroundStyle(.secondary)
				Divider()
				Text("Advertisement").font(.headline)
				Picker("Advertisement", selection: $advertiseMode) {
					ForEach(AdvertiseMode.allCases.filter { $0.isApplicable(sensors) }) { mode in
						Text(mode.title).tag(mode)
					}
				}
				.pickerStyle(.radioGroup)
				.labelsHidden()
			}
			.frame(maxWidth: .infinity, alignment: .leading)
			.padding(4)
		} label: {
			Text("Sensors, in advertisement order").font(.headline)
		}
		.disabled(simulator.isRunning)
	}

	private func sensorRow(_ sensor: Sensor) -> some View {
		HStack {
			Toggle(isOn: Binding(get: { sensors.contains(sensor) }, set: { _ in toggle(sensor) })) {
				VStack(alignment: .leading, spacing: 2) {
					Text(sensors.firstIndex(of: sensor).map { "\($0 + 1). \(sensor.sensorType)" } ?? sensor.sensorType)
					Text(sensor.title).font(.caption).foregroundStyle(.secondary)
				}
			}
			.toggleStyle(.checkbox)
			Spacer()
			if !sensor.options.isEmpty {
				Button {
					editedSensor = sensor
				} label: {
					Image(systemName: "pencil")
				}
				.buttonStyle(.borderless)
				.help("Sensor settings")
			}
		}
	}

	private func toggle(_ sensor: Sensor) {
		if let index = sensors.firstIndex(of: sensor) {
			var updated = sensors
			updated.remove(at: index)
			setSensors(updated)
		} else {
			setSensors(sensors + [sensor])
		}
	}

	private func setSensors(_ updated: [Sensor]) {
		sensors = updated
		if !advertiseMode.isApplicable(updated) {
			advertiseMode = .all
		}
	}

	private var controls: some View {
		HStack {
			if simulator.isRunning {
				Button("Stop") { simulator.stop() }
					.keyboardShortcut(.cancelAction)
			} else {
				Button("Start") { simulator.start(config) }
					.keyboardShortcut(.defaultAction)
					.disabled(sensors.isEmpty)
			}
			Text("macOS does not allow publishing the Battery service")
				.font(.caption)
				.foregroundStyle(.secondary)
		}
	}

	private var statusText: String {
		switch simulator.status {
		case .stopped: return "Stopped"
		case .waitingForBluetooth(let reason): return reason
		case .starting: return "Publishing services…"
		case .advertising: return "Advertising '\(simulator.config.name)'"
		case .failed(let error): return "Failed: \(error)"
		}
	}

	private var statusCard: some View {
		GroupBox {
			VStack(alignment: .leading, spacing: 4) {
				Text(statusText)
				if simulator.isRunning {
					let advertised = simulator.config.advertiseMode.advertised(simulator.config.sensors)
					Text("Advertised services: " + (advertised.isEmpty ? "none" : advertised.map(\.code).joined(separator: ", ")))
						.foregroundStyle(.secondary)
					Text("Connected centrals: \(simulator.subscriberCount)")
						.foregroundStyle(.secondary)
					ForEach(simulator.subscriptions.keys.sorted { $0.uuidString < $1.uuidString }, id: \.self) { uuid in
						Text("Subscribed to \(uuid.uuidString) \(uuid)")
							.font(.caption)
							.foregroundStyle(.secondary)
					}
				}
			}
			.frame(maxWidth: .infinity, alignment: .leading)
			.padding(4)
		} label: {
			Text("Status").font(.headline)
		}
	}

	private var readingsCard: some View {
		GroupBox {
			VStack(alignment: .leading, spacing: 8) {
				ForEach(simulator.config.sensors) { sensor in
					VStack(alignment: .leading, spacing: 2) {
						Text(sensor.sensorType).bold()
						ForEach(simulator.readings.values(of: sensor, options: simulator.config.options), id: \.0) { name, value in
							HStack {
								Text(name).foregroundStyle(.secondary)
								Spacer()
								Text(value).monospacedDigit()
							}
						}
					}
				}
			}
			.frame(maxWidth: .infinity, alignment: .leading)
			.padding(4)
		} label: {
			Text("Sent values").font(.headline)
		}
	}

	private var logCard: some View {
		GroupBox {
			VStack(alignment: .leading, spacing: 2) {
				ForEach(Array(simulator.log.suffix(50).reversed().enumerated()), id: \.offset) { _, line in
					Text(line)
						.font(.system(.caption, design: .monospaced))
						.textSelection(.enabled)
				}
			}
			.frame(maxWidth: .infinity, alignment: .leading)
			.padding(4)
		} label: {
			Text("Log").font(.headline)
		}
	}
}

struct SensorOptionsView: View {
	let sensor: Sensor
	@Binding var options: Set<SensorOption>
	@Environment(\.dismiss) private var dismiss

	var body: some View {
		VStack(alignment: .leading, spacing: 12) {
			Text(sensor.sensorType).font(.title3).bold()
			ForEach(sensor.options) { option in
				Toggle(isOn: Binding(
					get: { options.contains(option) },
					set: { enabled in
						if enabled { options.insert(option) } else { options.remove(option) }
					})) {
					VStack(alignment: .leading, spacing: 2) {
						Text(option.title)
						Text(option.details).font(.caption).foregroundStyle(.secondary)
					}
				}
				.toggleStyle(.checkbox)
			}
			HStack {
				Spacer()
				Button("Done") { dismiss() }
					.keyboardShortcut(.defaultAction)
			}
		}
		.padding(20)
		.frame(width: 380)
	}
}