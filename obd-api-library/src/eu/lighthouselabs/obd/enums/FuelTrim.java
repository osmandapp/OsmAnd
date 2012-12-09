/*
 * TODO put header
 */
package eu.lighthouselabs.obd.enums;

/**
 * Select one of the Fuel Trim percentage banks to access.
 */
public enum FuelTrim {

	SHORT_TERM_BANK_1(0x06),
	LONG_TERM_BANK_1(0x07),
	SHORT_TERM_BANK_2(0x08),
	LONG_TERM_BANK_2(0x09);

	private final int value;

	/**
	 * 
	 * @param value
	 */
	private FuelTrim(int value) {
		this.value = value;
	}

	/**
	 * 
	 * @return
	 */
	public final int getValue() {
		return value;
	}
	
	/**
	 * 
	 * @return
	 */
	public final String getObdCommand() {
		return new String("01 " + value);
	}
	
	public final String getBank() {
		String res = "NODATA";
		
		switch (value) {
		case 0x06:
			res = "Short Term Fuel Trim Bank 1";
			break;
		case 0x07:
			res = "Long Term Fuel Trim Bank 1";
			break;
		case 0x08:
			res = "Short Term Fuel Trim Bank 2";
			break;
		case 0x09:
			res = "Long Term Fuel Trim Bank 2";
			break;
		default:
			break;
		}
		
		return res;
	}

}