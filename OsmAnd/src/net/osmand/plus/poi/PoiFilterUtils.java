package net.osmand.plus.poi;

import static net.osmand.plus.poi.PoiUIFilter.STD_PREFIX;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.render.RenderingIcons;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PoiFilterUtils {

	public static void combineStandardPoiFilters(@NonNull Set<PoiUIFilter> filters, @NonNull OsmandApplication app) {
		Set<PoiUIFilter> standardFilters = new TreeSet<>();
		for (PoiUIFilter filter : filters) {
			if (((filter.isStandardFilter() && filter.filterId.startsWith(STD_PREFIX) && !filter.isTopWikiFilter())
					|| filter.isCustomPoiFilter())
					&& (filter.getFilterByName() == null)
					&& (filter.getSavedFilterByName() == null)) {
				standardFilters.add(filter);
			}
		}
		if (standardFilters.size() > 1) {
			PoiUIFilter standardFiltersCombined = new PoiUIFilter(standardFilters, app);
			filters.removeAll(standardFilters);
			filters.add(standardFiltersCombined);
		}
	}

	public static String getCustomFilterIconName(@Nullable PoiUIFilter filter) {
		if (filter != null) {
			Map<PoiCategory, LinkedHashSet<String>> acceptedTypes = filter.getAcceptedTypes();
			List<PoiCategory> categories = new ArrayList<>(acceptedTypes.keySet());
			if (categories.size() == 1) {
				PoiCategory category = categories.get(0);
				LinkedHashSet<String> filters = acceptedTypes.get(category);
				if (filters == null || filters.size() > 1) {
					return category.getIconKeyName();
				} else {
					return getPoiTypeIconName(category.getPoiTypeByKeyName(filters.iterator().next()));
				}
			}
		}
		return null;
	}

	@Nullable
	public static String getPoiTypeIconName(@Nullable AbstractPoiType abstractPoiType) {
		if (abstractPoiType != null && RenderingIcons.containsBigIcon(abstractPoiType.getIconKeyName())) {
			return abstractPoiType.getIconKeyName();
		} else if (abstractPoiType instanceof PoiType) {
			PoiType poiType = (PoiType) abstractPoiType;
			String iconId = poiType.getOsmTag() + "_" + poiType.getOsmValue();
			if (RenderingIcons.containsBigIcon(iconId)) {
				return iconId;
			} else if (poiType.getParentType() != null) {
				return getPoiTypeIconName(poiType.getParentType());
			}
		}
		return null;
	}

	// an attribute is declared by several types, its parent type is only the first of them (#24941)
	@NonNull
	public static List<PoiType> getTypesWithAdditional(@NonNull MapPoiTypes poiTypes, @NonNull AbstractPoiType additional) {
		List<PoiType> types = new ArrayList<>();
		if (additional instanceof PoiType poiType && poiType.getParentType() instanceof PoiType parent) {
			types.add(parent);
		}
		for (PoiCategory category : poiTypes.getCategories(false)) {
			for (PoiType type : category.getPoiTypes()) {
				if (!types.contains(type) && type.getPoiAdditionalByKeyName(additional.getKeyName()) != null) {
					types.add(type);
				}
			}
		}
		return types;
	}

	@NonNull
	public static String getTypesName(@NonNull List<PoiType> types, int maxChars) {
		StringBuilder sb = new StringBuilder();
		for (PoiType type : types) {
			String name = type.getTranslation();
			if (sb.length() > 0 && sb.length() + name.length() > maxChars) {
				sb.append("\u2026");
				break;
			}
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(name);
		}
		return sb.toString();
	}

	public static void sortByElo(@NonNull List<Amenity> amenities) {
		amenities.sort((a1, a2) -> {
			int cmp = Integer.compare(a2.getTravelEloNumber(), a1.getTravelEloNumber());
			if (cmp != 0) return cmp;
			return a1.getId() < a2.getId() ? -1 : (a1.getId().longValue() == a2.getId().longValue() ? 0 : 1);
		});
	}

	public interface AmenityNameFilter {

		boolean accept(Amenity a);
	}
}
