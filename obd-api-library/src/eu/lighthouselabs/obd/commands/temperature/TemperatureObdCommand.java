/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.temperature;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.commands.SystemOfUnits;

/**
 * TODO
 * 
 * put description
 */
public abstract class TemperatureObdCommand extends ObdCommand implements SystemOfUnits {
	
	private float temperature = 0.0f;

	/**
	 * Default ctor.
	 * 
	 * @param cmd
	 */
	public TemperatureObdCommand(String cmd) {
		super(cmd);
	}

	/**
	 * Copy ctor.
	 * 
	 * @param other
	 */
	public TemperatureObdCommand(TemperatureObdCommand other) {
		super(other);
	}

	/**
	 * TODO
	 * 
	 * put description of why we subtract 40
	 * 
	 * @param temp
	 * @return
	 */
	protected final float prepareTempValue(float temp) {
		return temp - 40;
	}

	/**
	 * Get values from 'buff', since we can't rely on char/string for calculations.
	 * 
	 * @return Temperature in Celsius or Fahrenheit.
	 */
	@Override
	public String getFormattedResult() {
		String res = getResult();

		if (!"NODATA".equals(res)) {
                    if (buffer.size() >= 3) {
			// ignore first two bytes [hh hh] of the response
			temperature = prepareTempValue(buffer.get(2));
			// convert?
			if (useImperialUnits)
			    res = String.format("%.1f%s", getImperialUnit(), "F");
			else
			    res = String.format("%.0f%s", temperature, "C");
                    } else {
                        res = "NODATA";
                    }
		}

		return res;
	}
	
	/**
	 * @return the temperature in Celsius.
	 */
	public float getTemperature() {
		return temperature;
	}

	/**
	 * @return the temperature in Fahrenheit.
	 */
	public float getImperialUnit() {
		return temperature * 1.8f + 32;
	}
	
	/**
	 * @return the temperature in Kelvin.
	 */
	public float getKelvin() {
		return temperature + 273.15f;
	}
	
	/**
	 * @return the OBD command name.
	 */
	public abstract String getName();
	
}