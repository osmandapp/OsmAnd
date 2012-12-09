/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands;

/**
 * Abstract class for percentage commands.
 */
public abstract class PercentageObdCommand extends ObdCommand {

	/**
	 * @param command
	 */
	public PercentageObdCommand(String command) {
		super(command);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param other
	 */
	public PercentageObdCommand(ObdCommand other) {
		super(other);
		// TODO Auto-generated constructor stub
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
			float tempValue = (buffer.get(2) * 100.0f) / 255.0f;
			res = String.format("%.1f%s", tempValue, "%");
	            } else {
	                res = "NODATA";
	            }
		}

		return res;
	}

}