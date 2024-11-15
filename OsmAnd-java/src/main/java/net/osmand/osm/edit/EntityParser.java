package net.osmand.osm.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.edit.Entity.EntityType;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.osm.edit.Relation.RelationMember;
import net.osmand.util.Algorithms;

public class EntityParser {

	public static void parseMapObject(MapObject mo, Entity e, Map<String, String> tags) {
		mo.setId(e.getId());
		if(mo instanceof Amenity) {
			mo.setId((e.getId() << 1) + ((EntityType.valueOf(e) == EntityType.NODE) ? 0 : 1));
		}
		if (mo.getName().length() == 0) {
			mo.setName(tags.get(OSMTagKey.NAME.getValue()));
		}
		if (mo.getEnName(false).length() == 0) {
			mo.setEnName(tags.get(OSMTagKey.NAME_EN.getValue()));
		}
		for (Map.Entry<String, String> entry : tags.entrySet()) {
			String ts = entry.getKey();
			if (ts.startsWith("name:") && !ts.equals(OSMTagKey.NAME_EN.getValue())) {
				mo.setName(ts.substring(("name:").length()), entry.getValue());
			}
		}
		if (mo.getName().length() == 0) {
			mo.setName(mo.getEnName(false));
		}
		if (mo.getName().length() == 0 && tags.containsKey(OSMTagKey.LOCK_NAME.getValue())) {
			mo.setName(tags.get(OSMTagKey.LOCK_NAME.getValue()));
		}
		if (mo.getLocation() == null) {
			LatLon l = null;
			if (mo instanceof Building) {
				l = findOnlyOneEntrance(e);
			}
			if (l == null) {
				l = OsmMapUtils.getCenter(e);
			}
			if (l != null) {
				mo.setLocation(l.getLatitude(), l.getLongitude());
			}
		}
		if (mo.getName().length() == 0) {
			setNameFromRef(mo, tags);
		}
		if (mo.getName().length() == 0) {
			setNameFromBrand(mo, tags);
		}
	}

	/**
	 * Finds the LatLon of a main entrance point. Main entrance here is the only entrance with value 'main'. If there is
	 * no main entrances, but there is only one entrance of any kind in given building, it is returned.
	 *
	 * @param e building entity
	 * @return main entrance point location or {@code null} if no entrance found or more than one entrance
	 */
	private static LatLon findOnlyOneEntrance(Entity e) {
		if (e instanceof Node) {
			return e.getLatLon();
		}
		List<Node> nodes = null;
		if (e instanceof Way) {
			nodes = ((Way) e).getNodes();
		} else if (e instanceof Relation) {
			nodes = new ArrayList<Node>();
			for (RelationMember member : ((Relation) e).getMembers(null)) {
				if (member.getEntity() instanceof Way) {
					nodes.addAll(((Way) member.getEntity()).getNodes());
				}
			}
		}
		if (nodes != null) {
			int entrancesCount = 0;
			Node mainEntrance = null;
			Node lastEntrance = null;

			for (Node node : nodes) {
				String entrance = node.getTag(OSMTagKey.ENTRANCE.getValue());
				if (entrance != null && !"no".equals(entrance)) {
					if ("main".equals(entrance)) {
						// main entrance should be only one
						if (mainEntrance != null) {
							return null;
						}
						mainEntrance = node;
					}
					entrancesCount++;
					lastEntrance = node;
				}
			}
			if (mainEntrance != null) {
				return mainEntrance.getLatLon();
			}
			if (entrancesCount == 1) {
				return lastEntrance.getLatLon();
			}
		}

		return null;
	}

	private static void setNameFromRef(MapObject mo, Map<String, String> tags) {
		String ref = tags.get(OSMTagKey.REF.getValue());
		if(ref != null){
			mo.setName(ref);
		}
	}

	private static void setNameFromBrand(MapObject mo, Map<String, String> tags) {
		String ref = tags.get(OSMTagKey.BRAND.getValue());
		if(ref != null){
			mo.setName(ref);
		}
	}

	private static String getWebSiteURL(Map<String, String> tagValues) {
		String siteUrl = tagValues.get(OSMTagKey.WEBSITE.getValue());
		String url = tagValues.get(OSMTagKey.URL.getValue());
		if (siteUrl == null && url == null) {
			siteUrl = tagValues.get(OSMTagKey.CONTACT_WEBSITE.getValue());
		}
		if (siteUrl != null && !siteUrl.startsWith("http://") && !siteUrl.startsWith("https://")) {
			siteUrl = "http://" + siteUrl;
		}
		return siteUrl;
	}

	private static String getWikipediaURL(Map<String, String> tagValues) {
		String wikiUrl = tagValues.get(OSMTagKey.WIKIPEDIA.getValue());
		if (wikiUrl != null) {
			if (!wikiUrl.startsWith("http://")) {
				int i = wikiUrl.indexOf(':');
				if (i == -1) {
					wikiUrl = "http://en.wikipedia.org/wiki/" + wikiUrl;
				} else {
					wikiUrl = "http://" + wikiUrl.substring(0, i) + ".wikipedia.org/wiki/" + wikiUrl.substring(i + 1);
				}
			}
		}
		return wikiUrl;
	}


	public static List<Amenity> parseAmenities(MapPoiTypes poiTypes, Entity entity, Map<String, String> tags,
			List<Amenity> amenitiesList) {
		amenitiesList.clear();
		// it could be collection of amenities
		boolean relation = entity instanceof Relation;
		boolean purerelation = relation &&
				!("multipolygon".equals(tags.get("type")) || "boundary".equals(tags.get("type")));
		Collection<Map<String, String>> it = MapRenderingTypes.splitTagsIntoDifferentObjects(tags);
		for (Map<String, String> ts : it) {
			for (Map.Entry<String, String> e : ts.entrySet()) {
				String value = e.getValue();
				String key = e.getKey();
				if (value.indexOf(';') != -1) {
					String[] vls = value.split(";");
					Amenity multiAmenity = null;
					for(String v : vls) {
						v = v.trim();
						Amenity am = poiTypes.parseAmenity(key, v, purerelation, ts);
						if (am != null) {
							if (multiAmenity != null) {
								multiAmenity.setSubType(multiAmenity.getSubType() + ";" + am.getSubType());
							} else {
								multiAmenity = am;
							}
						}
					}
					addAmenity(entity, amenitiesList, ts, multiAmenity);
				} else {
					Amenity am = poiTypes.parseAmenity(key, value, purerelation, ts);
					addAmenity(entity, amenitiesList, ts, am);
				}
			}
		}
		return amenitiesList;
	}

	private static void addAmenity(Entity entity, List<Amenity> amenitiesList, Map<String, String> ts, Amenity am) {
		if (am != null && checkAmenitiesToAdd(am, amenitiesList)) {
			parseMapObject(am, entity, ts);
			setWebsiteUrl(am, ts);
			setWikipediaUrl(am, ts);
			amenitiesList.add(am);
		}
	}

	private static void setWikipediaUrl(Amenity am, Map<String, String> ts) {
		String wbs = getWikipediaURL(ts);
		if (wbs != null) {
			am.setAdditionalInfo("wikipedia", wbs);
		}
	}

	private static void setWebsiteUrl(Amenity am, Map<String, String> ts) {
		String wbs = getWebSiteURL(ts);
		if (wbs != null) {
			am.setSite(wbs);
		}
	}

	private static boolean checkAmenitiesToAdd(Amenity a, List<Amenity> amenitiesList){
		// check amenity for duplication
		for(Amenity b : amenitiesList){
			if(b.getType() == a.getType() && Algorithms.objectEquals(a.getSubType(), b.getSubType())){
				return false;
			}
		}
		return !"no".equals(a.getSubType());
	}

	public static Building parseBuilding(Entity e){
		Building b = new Building();
		parseMapObject(b, e, e.getTags());
		// try to extract postcode
		String p = e.getTag(OSMTagKey.ADDR_POSTCODE.getValue());
		if(p == null) {
			p = e.getTag(OSMTagKey.POSTAL_CODE.getValue());
		}
		b.setPostcode(p);
		if(e instanceof Way) {
			List<Node> nodes = ((Way) e).getNodes();
			for(int i = 0; i < nodes.size(); i++) {
				Node node = nodes.get(i);
				if(node != null && "yes".equals(node.getTag(OSMTagKey.ENTRANCE)) &&
						!Algorithms.isEmpty(node.getTag(OSMTagKey.REF))) {
					b.addEntrance(node.getTag(OSMTagKey.REF), node.getLatLon());
				}
			}
		}
		return b;
	}

	public static City parseCity(Node el) {
		return parseCity(el, CityType.valueFromString(el.getTag(OSMTagKey.PLACE.getValue())));
	}

	public static City parseCity(Entity el, CityType t) {
		if(t == null) {
			return null;
		}
		City c = new City(t);
		parseMapObject(c, el, el.getTags());
		String isin = el.getTag(OSMTagKey.IS_IN.getValue());
		isin = isin != null ? isin.toLowerCase() : null;
		c.setIsin(isin);
		return c;
	}


	public static TransportRoute parserRoute(Relation r, String ref){
		TransportRoute rt = new TransportRoute();
		parseMapObject(rt, r, r.getTags());
		rt.setRef(ref);
		return rt;
	}

	public static TransportStop parseTransportStop(Entity e){
		TransportStop st = new TransportStop();
		parseMapObject(st, e, e.getTags());
		return st;
	}

}
