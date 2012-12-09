/*
 * TODO put header 
 */
package eu.lighthouselabs.obd.commands.pressure;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.commands.SystemOfUnits;

/**
 * TODO put description
 */
public abstract class PressureObdCommand extends ObdCommand implements
		SystemOfUnits {

	protected int tempValue = 0;
	protected int pressure = 0;

	/**
	 * Default ctor
	 * 
	 * @param cmd
	 */
	public PressureObdCommand(String cmd) {
		super(cmd);
	}

	/**
	 * Copy ctor.
	 * 
	 * @param cmd
	 */
	public PressureObdCommand(PressureObdCommand other) {
		super(other);
	}

	/**
	 * Some PressureObdCommand subclasses will need to implement this method in
	 * order to determine the final kPa value.
	 * 
	 * *NEED* to read tempValue
	 * 
	 * @return
	 */
	protected int preparePressureValue() {
		return tempValue;
	}

	/**
	 * 
	 */
	@Override
	public String getFormattedResult() {
		String res = getResult();

		if (!"NODATA".equals(res)) {
                    if (buffer.size() >= 3) {
			// ignore first two bytes [hh hh] of the response
			tempValue = buffer.get(2);
			pressure = preparePressureValue(); // this will need tempValue
			res = String.format("%d%s", pressure, "kPa");

			if (useImperialUnits) {
			    res = String.format("%.1f%s", getImperialUnit(), "psi");
			}
                    } else {
                        res = "NODATA";
                    }
		}

		return res;
	}
	
	/**
	 * @return the pressure in kPa
	 */
	public int getMetricUnit() {
		return pressure;
	}

	/**
	 * @return the pressure in psi
	 */
	public float getImperialUnit() {
		Double d = pressure * 0.145037738;
		return Float.valueOf(d.toString());
	}
}