/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.fuel;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.enums.FuelTrim;

/**
 * Get Fuel Trim.
 * 
 */
public class FuelTrimObdCommand extends ObdCommand {

	private float fuelTrimValue = 0.0f;
	private final FuelTrim bank;

	/**
	 * Default ctor.
	 * 
	 * Will read the bank from parameters and construct the command accordingly.
	 * Please, see FuelTrim enum for more details.
	 */
	public FuelTrimObdCommand(FuelTrim bank) {
		super(bank.getObdCommand());
		this.bank = bank;
	}

	/**
	 * @param value
	 * @return
	 */
	private float prepareTempValue(int value) {
		Double perc = (value - 128) * (100.0 / 128);
		return Float.parseFloat(perc.toString());
	}

	@Override
	public String getFormattedResult() {
	    String res = getResult();

	    if (!"NODATA".equals(getResult())) {
	        if (buffer.size() >= 3) {
	            // ignore first two bytes [hh hh] of the response
	            fuelTrimValue = prepareTempValue(buffer.get(2));
	            res = String.format("%.2f%s", fuelTrimValue, "%");
	        }
	    }

	    return res;
	}

	/**
	 * @return the readed Fuel Trim percentage value.
	 */
	public final float getValue() {
		return fuelTrimValue;
	}

	/**
	 * @return the name of the bank in string representation.
	 */
	public final String getBank() {
		return bank.getBank();
	}

	@Override
	public String getName() {
		return bank.getBank();
	}

}