package net.osmand.router;

import net.osmand.binary.BinaryMapRouteReaderAdapter;
import net.osmand.binary.RouteDataObject;

import java.util.HashMap;
import java.util.Map;

public class RouteConditionalHelper {
	public static final String RULE_INT_MAX = "RULE_INT_MAX";

	public void resolveAmbiguousConditionalTags(RouteDataObject rdo, Map<String, String> ambiguousConditionalTags) {
		Map<String, Integer> existingIntValues = new HashMap<>();

		// Find corresponding non-conditional tags and save their existing int-values.
		// Example: maxspeed:conditional (RULE_INT_MAX) will save the value of maxspeed.
		for (int type : rdo.types) {
			BinaryMapRouteReaderAdapter.RouteTypeRule r = rdo.region.quickGetEncodingRule(type);
			if (r != null && !r.conditional()) {
				String key = r.getTag() + ":conditional";
				String rule = ambiguousConditionalTags.get(key);
				if (rule != null && RULE_INT_MAX.equals(rule)) {
					try {
						Integer newValue = Integer.parseInt(r.getValue());
						Integer oldValue = existingIntValues.get(key);
						if (oldValue == null || newValue > oldValue) {
							existingIntValues.put(key, newValue);
						}
					} catch (NumberFormatException e) {
						continue;
					}
				}
			}
		}

		// Find conditionals and update their non-conditionals by the rules.
		// Example: access:conditional ("yes") will always set "access" = "yes"
		// Example: maxspeed:conditional (RULE_INT_MAX) might set maxspeed = max(existing, conditional)
		for (int type : rdo.types) {
			BinaryMapRouteReaderAdapter.RouteTypeRule r = rdo.region.quickGetEncodingRule(type);
			if (r != null && r.conditional()) {
				String key = r.getTag();
				String rule = ambiguousConditionalTags.get(key);
				if (rule != null && RULE_INT_MAX.equals(rule)) {
					Integer existingValue = existingIntValues.get(key);
					Integer newValue = r.getMaxIntegerConditionalValue();
					if (newValue != null && (existingValue == null || newValue > existingValue)) {
						updateTypesByTagValue(rdo, r.getNonConditionalTag(), newValue.toString()); // Math.max value
					}
				} else if (rule != null) {
					updateTypesByTagValue(rdo, r.getNonConditionalTag(), rule); // Default rule: set the string value
				}
			}
		}
	}

	public void processConditionalTags(RouteDataObject rdo, long conditionalTime) {
		int sz = rdo.types.length;
		for (int i = 0; i < sz; i++) {
			BinaryMapRouteReaderAdapter.RouteTypeRule r = rdo.region.quickGetEncodingRule(rdo.types[i]);
			if (r != null && r.conditional()) {
				int vl = r.conditionalValue(conditionalTime);
				if (vl != 0) {
					String nonCondTag = rdo.region.quickGetEncodingRule(vl).getTag();
					updateTypesByTagRuleId(rdo, nonCondTag, vl);
				}
			}
		}

		if (rdo.pointTypes != null) {
			for (int i = 0; i < rdo.pointTypes.length; i++) {
				if (rdo.pointTypes[i] != null) {
					int[] pTypes = rdo.pointTypes[i];
					int pSz = pTypes.length;
					if (pSz > 0) {
						for (int j = 0; j < pSz; j++) {
							BinaryMapRouteReaderAdapter.RouteTypeRule r = rdo.region.quickGetEncodingRule(pTypes[j]);
							if (r != null && r.conditional()) {
								int vl = r.conditionalValue(conditionalTime);
								if (vl != 0) {
									BinaryMapRouteReaderAdapter.RouteTypeRule rtr = rdo.region.quickGetEncodingRule(vl);
									String nonCondTag = rtr.getTag();
									int ks;
									for (ks = 0; ks < rdo.pointTypes[i].length; ks++) {
										BinaryMapRouteReaderAdapter.RouteTypeRule toReplace = rdo.region.quickGetEncodingRule(rdo.pointTypes[i][ks]);
										if (toReplace != null && toReplace.getTag().contentEquals(nonCondTag)) {
											break;
										}
									}
									if (ks == pTypes.length) {
										int[] ntypes = new int[pTypes.length + 1];
										System.arraycopy(pTypes, 0, ntypes, 0, pTypes.length);
										pTypes = ntypes;
									}
									pTypes[ks] = vl;

								}
							}
						}
					}
					rdo.pointTypes[i] = pTypes;
				}
			}
		}
	}

	public void updateTypesByTagValue(RouteDataObject rdo, String tag, String value) {
		int ruleId = rdo.region.searchRouteEncodingRule(tag, value);
		if (ruleId > 0) {
			updateTypesByTagRuleId(rdo, tag, ruleId);
		} else {
			System.err.printf("updateTypesByTagValue(%s,%s): searchRouteEncodingRule failed\n", tag, value);
		}
	}

	public void updateTypesByTagRuleId(RouteDataObject rdo, String tag, int ruleId) {
		if (ruleId > 0) {
			int ks;
			for (ks = 0; ks < rdo.types.length; ks++) {
				BinaryMapRouteReaderAdapter.RouteTypeRule toReplace = rdo.region.quickGetEncodingRule(rdo.types[ks]);
				if (toReplace != null && toReplace.getTag().equals(tag)) {
					break;
				}
			}
			if (ks == rdo.types.length) {
				int[] ntypes = new int[rdo.types.length + 1];
				System.arraycopy(rdo.types, 0, ntypes, 0, rdo.types.length);
				rdo.types = ntypes;
			}
			rdo.types[ks] = ruleId;
		}
	}
}
