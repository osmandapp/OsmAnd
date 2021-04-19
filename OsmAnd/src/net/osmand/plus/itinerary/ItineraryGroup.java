package net.osmand.plus.itinerary;

import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.osmand.plus.itinerary.ItineraryHelper.CATEGORIES_SPLIT;

public class ItineraryGroup {

	public String id;
	public String name;
	public ItineraryType type = ItineraryType.MARKERS;
	public Set<String> wptCategories;
	public List<ItineraryItem> itineraryItems = new ArrayList<>();

	public ItineraryGroup() {

	}

	public ItineraryGroup(ItineraryGroup group) {
		this.id = group.id;
		this.name = group.name;
		this.type = group.type;
		this.wptCategories = group.wptCategories;
		this.itineraryItems.addAll(group.itineraryItems);
	}

	public static ItineraryGroup createGroup(MapMarkersGroup markersGroup) {
		ItineraryGroup group = new ItineraryGroup();
		group.id = markersGroup.getId();
		group.name = markersGroup.getName();
		group.type = markersGroup.getType();
		group.wptCategories = markersGroup.getWptCategories();
		return group;
	}

	public static ItineraryGroup createGroup(ItineraryGroupInfo groupInfo) {
		ItineraryGroup group = new ItineraryGroup();
		group.name = groupInfo.name;
		group.type = ItineraryType.findTypeForName(groupInfo.type);
		group.id = group.type == ItineraryType.TRACK ? groupInfo.path : groupInfo.name;
		group.wptCategories = Algorithms.decodeStringSet(groupInfo.categories, CATEGORIES_SPLIT);
		return group;
	}

	public ItineraryGroupInfo convertToGroupInfo() {
		ItineraryGroupInfo groupInfo = new ItineraryGroupInfo();
		groupInfo.name = name;
		groupInfo.type = type.name().toLowerCase();
		groupInfo.path = type == ItineraryType.TRACK ? id : null;
		groupInfo.alias = groupInfo.type + ":" + (name != null ? name : "");
		if (!Algorithms.isEmpty(wptCategories)) {
			groupInfo.categories = Algorithms.encodeStringSet(wptCategories, CATEGORIES_SPLIT);
		}
		return groupInfo;
	}

	public static class ItineraryGroupInfo {
		public String name;
		public String type;
		public String path;
		public String alias;
		public String categories;
	}
}
