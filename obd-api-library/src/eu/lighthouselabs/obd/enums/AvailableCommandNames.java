/*
 * TODO put header
 */
package eu.lighthouselabs.obd.enums;

/**
 * TODO put description
 */
public enum AvailableCommandNames {
	AIR_INTAKE_TEMP("Air Intake Temperature"),
	AMBIENT_AIR_TEMP("Ambient Air Temperature"),
	ENGINE_COOLANT_TEMP("Engine Coolant Temperature"),
	BAROMETRIC_PRESSURE("Barometric Pressure"),
	FUEL_PRESSURE("Fuel Pressure"),
	INTAKE_MANIFOLD_PRESSURE("Intake Manifold Pressure"),
	ENGINE_LOAD("Engine Load"),
	ENGINE_RUNTIME("Engine Runtime"),
	ENGINE_RPM("Engine RPM"),
	SPEED("Vehicle Speed"),
	MAF("Mass Air Flow"),
	THROTTLE_POS("Throttle Position"),
	TROUBLE_CODES("Trouble Codes"),
	FUEL_LEVEL("Fuel Level"),
	FUEL_TYPE("Fuel Type"),
	FUEL_CONSUMPTION("Fuel Consumption"),
	FUEL_ECONOMY("Fuel Economy"),
	FUEL_ECONOMY_WITH_MAF("Fuel Economy 2"),
	FUEL_ECONOMY_WITHOUT_MAF("Fuel Economy 3"),
	TIMING_ADVANCE("Timing Advance"),
	DTC_NUMBER("Diagnostic Trouble Codes"),
	EQUIV_RATIO("Command Equivalence Ratio");
	
	
	private final String value;

	/**
	 * 
	 * @param value
	 */
	private AvailableCommandNames(String value) {
		this.value = value;
	}

	/**
	 * 
	 * @return
	 */
	public final String getValue() {
		return value;
	}
	
}