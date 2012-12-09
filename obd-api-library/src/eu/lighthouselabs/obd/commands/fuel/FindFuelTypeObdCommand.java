/*
 * TODO put header 
 */
package eu.lighthouselabs.obd.commands.fuel;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.commands.utils.ObdUtils;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * This command is intended to determine the vehicle fuel type.
 */
public class FindFuelTypeObdCommand extends ObdCommand {

	private int fuelType = 0;

	/**
	 * Default ctor.
	 */
	public FindFuelTypeObdCommand() {
		super("10 51");
	}

	/**
	 * Copy ctor
	 * 
	 * @param other
	 */
	public FindFuelTypeObdCommand(ObdCommand other) {
		super(other);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.lighthouselabs.obd.command.ObdCommand#getFormattedResult()
	 */
	@Override
	public String getFormattedResult() {
		String res = getResult();

		if (!"NODATA".equals(res)) {
                    if (buffer.size() >= 3) {
			// ignore first two bytes [hh hh] of the response
			fuelType = buffer.get(2);
			res = getFuelTypeName();
                    } else {
                        res = "NODATA";
                    }
		}

		return res;
	}
	
	/**
	 * @return Fuel type name.
	 */
	public final String getFuelTypeName() {
		return ObdUtils.getFuelTypeName(fuelType);
	}

	@Override
	public String getName() {
		return AvailableCommandNames.FUEL_TYPE.getValue();
	}

}