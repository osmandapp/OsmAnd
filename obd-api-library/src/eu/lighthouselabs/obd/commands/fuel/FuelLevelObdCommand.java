/*
 * TODO put header 
 */
package eu.lighthouselabs.obd.commands.fuel;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.commands.utils.ObdUtils;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * Get fuel level in percentage
 */
public class FuelLevelObdCommand extends ObdCommand {

	private float fuelLevel = 0f;

	/**
	 * @param command
	 */
	public FuelLevelObdCommand() {
		super("01 2F");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.lighthouselabs.obd.commands.ObdCommand#getFormattedResult()
	 */
	@Override
	public String getFormattedResult() {
	    String res = getResult();

	    if (!"NODATA".equals(res)) {
	        if (buffer.size() >= 3) {
	            // ignore first two bytes [hh hh] of the response
	            fuelLevel = 100.0f * buffer.get(2) / 255.0f;
	            res = String.format("%.1f%s", fuelLevel, "%");
	        } else {
	            res = "NODATA";
	        }
	    }

	    return res;
	}

	@Override
	public String getName() {
		return AvailableCommandNames.FUEL_LEVEL.getValue();
	}

}