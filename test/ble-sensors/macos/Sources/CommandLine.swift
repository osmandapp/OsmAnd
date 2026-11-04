import Foundation

let usage = """
usage: blesim [--sensors LIST | --preset NAME] [--adv all|first|none] [options]
Without arguments the simulator opens its window.

  --sensors LIST   comma separated, in advertisement order: hr,rsc,csc,cp,ht,bp (default hr)
                   hr  Heart Rate (0x180D)            rsc Running Speed and Cadence (0x1814)
                   csc Cycling Speed and Cadence (0x1816)  cp Cycling Power (0x1818)
                   ht  Health Thermometer (0x1809)    bp  Blood Pressure (0x1810)
  --preset NAME    hrm600   Garmin HRM 600: rsc before hr (issue #25694)
                   momentum Sennheiser Momentum Sport: ht before hr
  --adv MODE       all: every selected service (default); first: only the first one,
                   the rest are found after connecting (needs several sensors); none: no service UUIDs
  --no-stride      running: leave out the stride length (flags bit 0)
  --no-distance    running: leave out the total distance (flags bit 1)
  --indicate LIST  send ht and/or bp measurements via indication, as the specification defines
  --name NAME      advertised name, default "<sensor type> simulator"

macOS does not allow publishing the Battery service (0x180F), so there is no battery level.
"""

private func fail(_ message: String) -> Never {
	print(message)
	print(usage)
	exit(2)
}

private func parseSensors(_ list: String?) -> [Sensor] {
	guard let list else { fail("missing sensor list") }
	return list.split(separator: ",").map { code in
		guard let sensor = Sensor(rawValue: code.lowercased()) else { fail("unknown sensor \(code)") }
		return sensor
	}
}

func parseCommandLine(_ arguments: [String]) -> SimulatorConfig {
	var config = SimulatorConfig()
	var args = arguments.makeIterator()
	while let arg = args.next() {
		switch arg {
		case "--sensors": config.sensors = parseSensors(args.next())
		case "--preset", "--profile":
			let name = args.next() ?? ""
			if name == "hr" {
				config.sensors = [.hr]
			} else if let preset = Preset(rawValue: name) {
				config.sensors = preset.sensors
			} else {
				fail("unknown preset \(name)")
			}
		case "--adv":
			let mode = args.next() ?? ""
			guard let advertiseMode = AdvertiseMode(rawValue: mode) else { fail("unknown advertisement mode \(mode)") }
			config.advertiseMode = advertiseMode
		case "--no-stride": config.options.remove(.runningStrideLength)
		case "--no-distance": config.options.remove(.runningTotalDistance)
		case "--indicate":
			for sensor in parseSensors(args.next()) {
				switch sensor {
				case .ht: config.options.insert(.temperatureIndication)
				case .bp: config.options.insert(.bloodPressureIndication)
				default: fail("\(sensor.rawValue) has no indication option, only ht and bp")
				}
			}
		case "--name": config.customName = args.next()
		case "-h", "--help":
			print(usage)
			exit(0)
		default: fail("unknown argument \(arg)")
		}
	}
	config.sensors = config.sensors.reduce(into: []) { if !$0.contains($1) { $0.append($1) } }
	if !config.advertiseMode.isApplicable(config.sensors) {
		print("--adv first needs several sensors, advertising all")
		config.advertiseMode = .all
	}
	return config
}

func runCommandLine(_ arguments: [String]) -> Never {
	setvbuf(stdout, nil, _IOLBF, 0)
	let config = parseCommandLine(arguments)
	let simulator = Simulator()
	simulator.onTick = { tick in
		guard tick % 10 == 0 else { return }
		let values = config.sensors.flatMap { simulator.readings.values(of: $0, options: config.options) }
			.map { "\($0.0.lowercased())=\($0.1)" }
		print("t=\(tick)s subs=\(simulator.subscriberCount) " + values.joined(separator: " "))
	}
	simulator.start(config)
	RunLoop.main.run()
	exit(0)
}