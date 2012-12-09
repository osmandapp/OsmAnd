/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.temperature;

import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * Engine Coolant Temperature.
 */
public class EngineCoolantTemperatureObdCommand extends TemperatureObdCommand {

	/**
	 * 
	 */
	public EngineCoolantTemperatureObdCommand() {
		super("01 05");
	}

	/**
	 * @param other
	 */
	public EngineCoolantTemperatureObdCommand(TemperatureObdCommand other) {
		super(other);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.lighthouselabs.obd.commands.ObdCommand#getName()
	 */
	@Override
	public String getName() {
		return AvailableCommandNames.ENGINE_COOLANT_TEMP.getValue();
	}

}