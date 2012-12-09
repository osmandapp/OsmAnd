package eu.lighthouselabs.obd.commands.pressure;

import eu.lighthouselabs.obd.enums.AvailableCommandNames;


public class FuelPressureObdCommand extends PressureObdCommand {

	public FuelPressureObdCommand() {
		super("010A");
	}

	public FuelPressureObdCommand(FuelPressureObdCommand other) {
		super(other);
	}

	/**
	 * TODO
	 * 
	 * put description of why we multiply by 3
	 * 
	 * @param temp
	 * @return
	 */
	@Override
	protected final int preparePressureValue() {
		return tempValue * 3;
	}

	@Override
	public String getName() {
		return AvailableCommandNames.FUEL_PRESSURE.getValue();
	}
	
}