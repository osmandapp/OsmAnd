package net.osmand.binary;

import java.util.Set;

import net.osmand.binary.BinaryMapAddressReaderAdapter.CityBlocks;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.search.core.spatial.SpatialSearchToken;

/**
 * The owner of a name for the {@code object} of {@code rules*.xml} (rules-spec.md, 3.2): the one definition for the OBF
 * writer (an object of the map), the name index (the type of an atom) and the ranking of results.
 */
public final class RuleOwner {

	public static final String STREET = "street";
	public static final String LOCALITY = "locality";
	public static final String BOUNDARY = "boundary";
	public static final String POSTCODE = "postcode";
	public static final String POI = "poi";

	/** every owner a rule can name */
	public static final Set<String> ALL = Set.of(STREET, LOCALITY, BOUNDARY, POSTCODE, POI);

	private RuleOwner() {
	}

	/** @return the owner of the names of a city of a type: a boundary, a postcode, else a locality */
	public static String of(City.CityType type) {
		return switch (type) {
			case BOUNDARY -> BOUNDARY;
			case POSTCODE -> POSTCODE;
			default -> LOCALITY;
		};
	}

	/** @return the owner of the names of an address object, null for an object no address rule names */
	public static String of(MapObject object) {
		if (object instanceof Street) {
			return STREET;
		}
		return object instanceof City city ? of(city.getType()) : null;
	}

	/**
	 * @param type type of an atom of the name index: an address block ({@link CityBlocks#index}) or
	 *             {@link SpatialSearchToken#POI_TYPE}, {@link SpatialSearchToken#POI_REF_TYPE},
	 *             {@link SpatialSearchToken#BUILDING_TYPE}; a building has the name of its street
	 */
	public static String ofAtomType(int type) {
		if (type == CityBlocks.STREET_TYPE.index || type == SpatialSearchToken.BUILDING_TYPE) {
			return STREET;
		} else if (type == SpatialSearchToken.POI_TYPE || type == SpatialSearchToken.POI_REF_TYPE) {
			return POI;
		} else if (type == CityBlocks.BOUNDARY_TYPE.index) {
			return BOUNDARY;
		} else if (type == CityBlocks.POSTCODES_TYPE.index) {
			return POSTCODE;
		}
		return LOCALITY;
	}
}
