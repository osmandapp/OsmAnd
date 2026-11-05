// BLE sensor simulator (macOS peripheral) for testing the OsmAnd external sensors plugin.
// Same sensors, values and options as the Android simulator in test/ble-sensors.

import Foundation

let arguments = Array(CommandLine.arguments.dropFirst())
// Xcode and the Finder may pass their own -Key value arguments, only ours switch to the command line mode
if arguments.contains(where: { $0.hasPrefix("--") || $0 == "-h" }) {
	runCommandLine(arguments)
} else {
	SimulatorApp.main()
}