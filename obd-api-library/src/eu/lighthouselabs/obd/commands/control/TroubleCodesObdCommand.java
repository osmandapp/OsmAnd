/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.control;

import eu.lighthouselabs.obd.commands.ObdCommand;
import eu.lighthouselabs.obd.enums.AvailableCommandNames;

/**
 * In order to get ECU Trouble Codes, one must first send a DtcNumberObdCommand
 * and by so, determining the number of error codes available by means of
 * getTotalAvailableCodes().
 * 
 * If none are available (totalCodes < 1), don't instantiate this command.
 */
public class TroubleCodesObdCommand extends ObdCommand {

	protected final static char[] dtcLetters = { 'P', 'C', 'B', 'U' };

	private StringBuffer codes = null;
	private int howManyTroubleCodes = 0;

	/**
	 * Default ctor.
	 */
	public TroubleCodesObdCommand(int howManyTroubleCodes) {
		super("03");

		codes = new StringBuffer();
		this.howManyTroubleCodes = howManyTroubleCodes;
	}

	/**
	 * Copy ctor.
	 * 
	 * @param other
	 */
	public TroubleCodesObdCommand(TroubleCodesObdCommand other) {
		super(other);
		codes = new StringBuffer();
	}

	// TODO clean
	// int count = numCmd.getCodeCount();
	// int dtcNum = (count + 2) / 3;
	// for (int i = 0; i < dtcNum; i++) {
	// sendCommand(cmd);
	// String res = getResult();
	// for (int j = 0; j < 3; j++) {
	// String byte1 = res.substring(3 + j * 6, 5 + j * 6);
	// String byte2 = res.substring(6 + j * 6, 8 + j * 6);
	// int b1 = Integer.parseInt(byte1, 16);
	// int b2 = Integer.parseInt(byte2, 16);
	// int val = (b1 << 8) + b2;
	// if (val == 0) {
	// break;
	// }
	// String code = "P";
	// if ((val & 0xC000) > 14) {
	// code = "C";
	// }
	// code += Integer.toString((val & 0x3000) >> 12);
	// code += Integer.toString((val & 0x0fff));
	// codes.append(code);
	// codes.append("\n");
	// }

	/**
	 * @return the formatted result of this command in string representation.
	 */
	public String formatResult() {
		String res = getResult();

		if (!"NODATA".equals(res)) {
			/*
			 * Ignore first byte [43] of the response and then read each two
			 * bytes.
			 */
			int begin = 2; // start at 2nd byte
			int end = 6; // end at 4th byte

			for (int i = 0; i < howManyTroubleCodes * 2; i++) {
				// read and jump 2 bytes
				byte b1 = Byte.parseByte(res.substring(begin, end));
				begin += 2;
				end += 2;

				// read and jump 2 bytes
				byte b2 = Byte.parseByte(res.substring(begin, end));
				begin += 2;
				end += 2;

				int tempValue = b1 << 8 | b2;
			}
		}

		String[] ress = res.split("\r");
		for (String r : ress) {
			String k = r.replace("\r", "");
			codes.append(k);
			codes.append("\n");
		}
		return codes.toString();
	}

	@Override
	public String getFormattedResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return AvailableCommandNames.TROUBLE_CODES.getValue();
	}
}
