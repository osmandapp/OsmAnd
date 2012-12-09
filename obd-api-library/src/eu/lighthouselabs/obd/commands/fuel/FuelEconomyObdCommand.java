/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.fuel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.commands.SpeedObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * TODO put description
 */
public class FuelEconomyObdCommand extends ObdCommand {

	protected float kml = -1.0f;
	private float speed = -1.0f;

	/**
	 * Default ctor.
	 */
	public FuelEconomyObdCommand() {
		super("");
	}

	/**
	 * As it's a fake command, neither do we need to send request or read
	 * response.
	 */
	@Override
	public void run(InputStream in, OutputStream out) throws IOException,
			InterruptedException {
		// get consumption liters per hour
		FuelConsumptionObdCommand fuelConsumptionCommand = new FuelConsumptionObdCommand();
		fuelConsumptionCommand.run(in, out);
		fuelConsumptionCommand.getFormattedResult();
		float fuelConsumption = fuelConsumptionCommand.getLitersPerHour();

		// get metric speed
		SpeedObdCommand speedCommand = new SpeedObdCommand();
		speedCommand.run(in, out);
		speedCommand.getFormattedResult();
		speed = speedCommand.getMetricSpeed();

		// get l/100km
		kml = (100 / speed) * fuelConsumption;
	}

	/**
	 * 
	 * @return
	 */
	@Override
	public String getFormattedResult() {
	    if (useImperialUnits) {
	        // convert to mpg
	        return String.format("%.1f %s", getMilesPerUKGallon(), "mpg");
	    }
	    return String.format("%.1f %s", kml, "l/100km");
	}
	
	public float getLitersPer100Km() {
		return kml;
	}
	
	public float getMilesPerUSGallon() {
		return 235.2f / kml;
	}
	
	public float getMilesPerUKGallon() {
		return 282.5f / kml;
	}

	@Override
	public String getName() {
		return AvailableCommandNames.FUEL_ECONOMY.getValue();
	}

}