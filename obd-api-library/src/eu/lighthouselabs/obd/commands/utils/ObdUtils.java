/*
 * TODO put header
 */
package eu.lighthouselabs.obd.commands.utils;

/**
 * Misc utilities
 */
public final class ObdUtils {

	/**
	 * @param an integer value
	 * @return the equivalent FuelType name.
	 */
	public final static String getFuelTypeName(int value) {
		String name = null;

		switch (value) {
		case 1:
			name = "Gasoline";
			break;
		case 2:
			name = "Methanol";
			break;
		case 3:
			name = "Ethanol";
			break;
		case 4:
			name = "Diesel";
			break;
		case 5:
			name = "GPL/LGP";
			break;
		case 6:
			name = "Natural Gas (CNG)";
			break;
		case 7:
			name = "Propane";
			break;
		case 8:
			name = "Electric";
			break;
		case 9:
			name = "Biodiesel + Gasoline";
			break;
		case 10:
			name = "Biodiesel + Methanol";
			break;
		case 11:
			name = "Biodiesel + Ethanol";
			break;
		case 12:
			name = "Biodiesel + GPL/LPG";
			break;
		case 13:
			name = "Biodiesel + Natural Gas";
			break;
		case 14:
			name = "Biodiesel + Propane";
			break;
		case 15:
			name = "Biodiesel + Electric";
			break;
		case 16:
			name = "Biodiesel + Gasoline/Electric";
			break;
		case 17:
			name = "Hybrid Gasoline";
			break;
		case 18:
			name = "Hybrid Ethanol";
			break;
		case 19:
			name = "Hybrid Diesel";
			break;
		case 20:
			name = "Hybrid Electric";
			break;
		case 21:
			name = "Hybrid Mixed";
			break;
		case 22:
			name = "Hybrid Regenerative";
			break;
		default:
			name = "NODATA";
		}

		return name;
	}
	
}