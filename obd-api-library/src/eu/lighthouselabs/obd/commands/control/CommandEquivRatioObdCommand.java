/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.control;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * Fuel systems that use conventional oxygen sensor display the commanded open
 * loop equivalence ratio while the system is in open loop. Should report 100%
 * when in closed loop fuel.
 * 
 * To obtain the actual air/fuel ratio being commanded, multiply the
 * stoichiometric A/F ratio by the equivalence ratio. For example, gasoline,
 * stoichiometric is 14.64:1 ratio. If the fuel control system was commanded an
 * equivalence ratio of 0.95, the commanded A/F ratio to the engine would be
 * 14.64 * 0.95 = 13.9 A/F.
 */
public class CommandEquivRatioObdCommand extends ObdCommand {

	/*
	 * Equivalent ratio (%)
	 */
	private double ratio = 0.00;

	/**
	 * Default ctor.
	 */
	public CommandEquivRatioObdCommand() {
		super("01 44");
	}

	/**
	 * Copy ctor.
	 * 
	 * @param other
	 */
	public CommandEquivRatioObdCommand(CommandEquivRatioObdCommand other) {
		super(other);
	}

	/**
	 * 
	 */
	@Override
	public String getFormattedResult() {
		String res = getResult();

		if (!"NODATA".equals(res)) {
		    if (buffer.size() >= 4) {
			// ignore first two bytes [hh hh] of the response
			int a = buffer.get(2);
			int b = buffer.get(3);
			ratio = (a * 256 + b) / 32768;
			res = String.format("%.1f%s", ratio, "%");
		    } else {
		        res = "NODATA";
		    }
		}

		return res;
	}

	/**
	 * @return
	 */
	public double getRatio() {
		return ratio;
	}

	@Override
	public String getName() {
		return AvailableCommandNames.EQUIV_RATIO.getValue();
	}
}