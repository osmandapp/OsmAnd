/*
 * TODO put header 
 */
package eu.lighthouselabs.obd.enums;

import eu.lighthouselabs.obd.commands.utils.ObdUtils;

/**
 * MODE 1 PID 0x51 will return one of the following values to identify the fuel
 * type of the vehicle.
 */
public enum FuelType {
	GASOLINE(0x01),
	METHANOL(0x02),
	ETHANOL(0x03),
	DIESEL(0x04),
	LPG(0x05),
	CNG(0x06),
	PROPANE(0x07),
	ELECTRIC(0x08),
	BIFUEL_GASOLINE(0x09),
	BIFUEL_METHANOL(0x0A),
	BIFUEL_ETHANOL(0x0B),
	BIFUEL_LPG(0x0C),
	BIFUEL_CNG(0x0D),
	BIFUEL_PROPANE(0x0E),
	BIFUEL_ELECTRIC(0x0F),
	BIFUEL_GASOLINE_ELECTRIC(0x10),
	HYBRID_GASOLINE(0x11),
	HYBRID_ETHANOL(0x12),
	HYBRID_DIESEL(0x13),
	HYBRID_ELECTRIC(0x14),
	HYBRID_MIXED(0x15),
	HYBRID_REGENERATIVE(0x16);

	private final int value;

	/**
	 * 
	 * @param value
	 */
	private FuelType(int value) {
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
	public final String getName() {
		return ObdUtils.getFuelTypeName(value);
	}

}