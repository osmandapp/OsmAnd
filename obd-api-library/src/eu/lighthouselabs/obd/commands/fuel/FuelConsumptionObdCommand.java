/*
 * TODO put header 
 */
package eu.lighthouselabs.obd.commands.fuel;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * TODO put description
 */
public class FuelConsumptionObdCommand extends ObdCommand {

	private float fuelRate = -1.0f;

	public FuelConsumptionObdCommand() {
		super("01 5E");
	}

	public FuelConsumptionObdCommand(ObdCommand other) {
		super(other);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.lighthouselabs.obd.commands.ObdCommand#getFormattedResult()
	 */
	@Override
	public String getFormattedResult() {
	    String res = getResult();

	    if (!"NODATA".equals(getResult())) {
	        if (buffer.size() >= 4) {
	            // ignore first two bytes [hh hh] of the response
	            int a = buffer.get(2);
	            int b = buffer.get(3);
	            fuelRate = (a * 256 + b) * 0.05f;
	            res = String.format("%.1f%s", fuelRate, "");
	        } else {
	            res = "NODATA";
	        }
	    }

	    return res;
	}

	public float getLitersPerHour() {
		return fuelRate;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see eu.lighthouselabs.obd.commands.ObdCommand#getName()
	 */
	@Override
	public String getName() {
		return AvailableCommandNames.FUEL_CONSUMPTION.getValue();
	}

}
