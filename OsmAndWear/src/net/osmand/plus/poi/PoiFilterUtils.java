package net.osmand.plus.poi;

import static net.osmand.plus.poi.PoiUIFilter.STD_PREFIX;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.osm.AbstractPoiType;
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
			if (((filter.isStandardFilter() && filter.filterId.startsWith(STD_PREFIX))
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

	public interface AmenityNameFilter {

		boolean accept(Amenity a);
	}
}
