/*
 * TODO put header
 */
package eu.lighthouselabs.obd.enums;

/**
 * All OBD protocols.
 */
public enum ObdProtocols {

	/**
	 * Auto select protocol and save.
	 */
	AUTO('0'),

	/**
	 * 41.6 kbaud
	 */
	SAE_J1850_PWM('1'),

	/**
	 * 10.4 kbaud
	 */
	SAE_J1850_VPW('2'),

	/**
	 * 5 baud init
	 */
	ISO_9141_2('3'),

	/**
	 * 5 baud init
	 */
	ISO_14230_4_KWP('4'),

	/**
	 * Fast init
	 */
	ISO_14230_4_KWP_FAST('5'),

	/**
	 * 11 bit ID, 500 kbaud
	 */
	ISO_15765_4_CAN('6'),

	/**
	 * 29 bit ID, 500 kbaud
	 */
	ISO_15765_4_CAN_B('7'),

	/**
	 * 11 bit ID, 250 kbaud
	 */
	ISO_15765_4_CAN_C('8'),

	/**
	 * 29 bit ID, 250 kbaud
	 */
	ISO_15765_4_CAN_D('9'),

	/**
	 * 29 bit ID, 250 kbaud (user adjustable)
	 */
	SAE_J1939_CAN('A'),

	/**
	 * 11 bit ID (user adjustable), 125 kbaud (user adjustable)
	 */
	USER1_CAN('B'),

	/**
	 * 11 bit ID (user adjustable), 50 kbaud (user adjustable)
	 */
	USER2_CAN('C');

	private final char value;

	ObdProtocols(char value) {
		this.value = value;
	}

	public char getValue() {
		return value;
	}
}