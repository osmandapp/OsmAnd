/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands;

import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * TODO put description
 * 
 * Current speed.
 */
public class SpeedObdCommand extends ObdCommand implements SystemOfUnits {

	private int metricSpeed = 0;

	/**
	 * Default ctor.
	 */
	public SpeedObdCommand() {
		super("01 0D");
	}

	/**
	 * Copy ctor.
	 * 
	 * @param other
	 */
	public SpeedObdCommand(SpeedObdCommand other) {
		super(other);
	}

	/**
	 * 
	 */
	public String getFormattedResult() {
		String res = getResult();
		
		if (!"NODATA".equals(res)) {
                    if (buffer.size() >= 3) {
			//Ignore first two bytes [hh hh] of the response.
			metricSpeed = buffer.get(2);
			res = String.format("%d%s", metricSpeed, "km/h");

			if (useImperialUnits)
			    res = String.format("%.2f%s", getImperialUnit(),
			            "mph");
                    } else {
                        res = "NODATA";
                    }
		}

		return res;
	}

	/**
	 * @return the speed in metric units.
	 */
	public int getMetricSpeed() {
		return metricSpeed;
	}

	/**
	 * @return the speed in imperial units.
	 */
	public float getImperialSpeed() {
		return getImperialUnit();
	}
	
	/**
	 * Convert from km/h to mph
	 */
	public float getImperialUnit() {
		Double tempValue = metricSpeed * 0.621371192;
		return Float.valueOf(tempValue.toString());
	}

	@Override
	public String getName() {
		return AvailableCommandNames.SPEED.getValue();
	}

}