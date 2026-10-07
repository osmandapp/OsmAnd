package net.osmand.binary;

import java.util.Set;

/**
 * The owner of a name for the {@code object} of {@code rules*.xml} (rules-spec.md, 3.2): street, locality, boundary,
 * postcode or poi.
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
}
