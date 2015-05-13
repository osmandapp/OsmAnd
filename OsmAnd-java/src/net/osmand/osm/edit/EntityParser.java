package net.osmand.osm.edit;

import net.osmand.data.*;
import net.osmand.data.City.CityType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class EntityParser {
	
	public static void parseMapObject(MapObject mo, Entity e) {
		mo.setId(e.getId());
		if(mo instanceof Amenity) {
			mo.setId((e.getId() << 1) + ((e instanceof Node) ? 0 : 1));
		}
		if (mo.getName().length() == 0) {
			mo.setName(e.getTag(OSMTagKey.NAME));
		}
		if (mo.getEnName().length() == 0) {
			mo.setEnName(e.getTag(OSMTagKey.NAME_EN));
			if (mo.getName().length() == 0) {
				mo.setName(mo.getEnName());
			}
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
			setNameFromOperator(mo, e);
		}
		if (mo.getName().length() == 0) {
			setNameFromRef(mo, e);
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
			for (Entity member : ((Relation) e).getMembers(null)) {
				if (member instanceof Way) {
					nodes.addAll(((Way) member).getNodes());
				}
			}
		}
		if (nodes != null) {
			int entrancesCount = 0;
			Node mainEntrance = null;
			Node lastEntrance = null;

			for (Node node : nodes) {
				String entrance = node.getTag(OSMTagKey.ENTRANCE);
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

	private static void setNameFromRef(MapObject mo, Entity e) {
		String ref = e.getTag(OSMTagKey.REF);
		if(ref != null){
			mo.setName(ref);
		}
	}

	private static void setNameFromOperator(MapObject mo,Entity e) {
		String op = e.getTag(OSMTagKey.OPERATOR);
		if (op == null)
			return;
		String ref = e.getTag(OSMTagKey.REF);
		if (ref != null)
			op += " [" + ref + "]";
		mo.setName(op);
	}
	
	public static Amenity parseAmenity(Entity entity, PoiCategory type, String subtype, Map<String, String> tagValues,
			MapRenderingTypes types) {
		Amenity am = new Amenity();
		parseMapObject(am, entity);
		if(tagValues == null) {
			tagValues = entity.getTags();
		}
		am.setType(type);
		am.setSubType(subtype);
		AmenityType at = AmenityType.findOrCreateTypeNoReg(type.getKeyName());
		am.setAdditionalInfo(types.getAmenityAdditionalInfo(tagValues, at, subtype));
		String wbs = getWebSiteURL(entity);
		if(wbs != null) {
			am.setAdditionalInfo("website", wbs);
		}
		return am;
	}

	

	private static String getWebSiteURL(Entity entity) {
		String siteUrl = entity.getTag(OSMTagKey.WIKIPEDIA);
		if (siteUrl != null) {
			if (!siteUrl.startsWith("http://")) { //$NON-NLS-1$
				int i = siteUrl.indexOf(':');
				if (i == -1) {
					siteUrl = "http://en.wikipedia.org/wiki/" + siteUrl; //$NON-NLS-1$
				} else {
					siteUrl = "http://" + siteUrl.substring(0, i) + ".wikipedia.org/wiki/" + siteUrl.substring(i + 1); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} else {
			siteUrl = entity.getTag(OSMTagKey.WEBSITE);
			if (siteUrl == null) {
				siteUrl = entity.getTag(OSMTagKey.URL);
				if (siteUrl == null) {
					siteUrl = entity.getTag(OSMTagKey.CONTACT_WEBSITE);
				}
			}
			if (siteUrl != null && !siteUrl.startsWith("http://") && !siteUrl.startsWith("https://")) {
				siteUrl = "http://" + siteUrl;
			}
		}
		return siteUrl;
	}
	
	public static List<Amenity> parseAmenities(MapRenderingTypes renderingTypes,
			MapPoiTypes poiTypes, Entity entity, List<Amenity> amenitiesList){
		amenitiesList.clear();
		// it could be collection of amenities
		boolean relation = entity instanceof Relation;
		Collection<Map<String, String>> it = renderingTypes.splitTagsIntoDifferentObjects(entity.getTags());
		for(Map<String, String> tags : it) {
			if (!tags.isEmpty()) {
				boolean purerelation = relation && !"multipolygon".equals(tags.get("type"));
				boolean hasName = !Algorithms.isEmpty(tags.get("name"));
				for (Map.Entry<String, String> e : tags.entrySet()) {
					AmenityType type = purerelation ? renderingTypes.getAmenityTypeForRelation(e.getKey(), e.getValue(), hasName)
							: renderingTypes.getAmenityType(e.getKey(), e.getValue(), hasName );
					if (type != null) {
						String subtype = renderingTypes.getAmenitySubtype(e.getKey(), e.getValue());
						PoiCategory pc = poiTypes.getPoiCategoryByName(type.getCategoryName(), true);
						Amenity a = parseAmenity(entity, pc, subtype, tags, renderingTypes);
						if (checkAmenitiesToAdd(a, amenitiesList) && !"no".equals(subtype)) {
							amenitiesList.add(a);
						}
					}
				}
			}
		}
		return amenitiesList;
	}
	
	private static boolean checkAmenitiesToAdd(Amenity a, List<Amenity> amenitiesList){
		// check amenity for duplication
		for(Amenity b : amenitiesList){
			if(b.getType() == a.getType() && Algorithms.objectEquals(a.getSubType(), b.getSubType())){
				return false;
			}
		}
		return true;
		
	}
	
	public static Building parseBuilding(Entity e){
		Building b = new Building();
		parseMapObject(b, e);
		// try to extract postcode
		String p = e.getTag(OSMTagKey.ADDR_POSTCODE);
		if(p == null) {
			p = e.getTag(OSMTagKey.POSTAL_CODE);
		}
		b.setPostcode(p);
		return b;
	}
	
	public static City parseCity(Node el) {
		return parseCity(el, CityType.valueFromString(el.getTag(OSMTagKey.PLACE)));
	}
	
	public static City parseCity(Entity el, CityType t) {
		if(t == null) {
			return null;
		}
		City c = new City(t);
		parseMapObject(c, el);
		String isin = el.getTag(OSMTagKey.IS_IN);
		isin = isin != null ? isin.toLowerCase() : null;
		c.setIsin(isin);
		return c;
	}
	
	
	public static OsmTransportRoute parserRoute(Relation r, String ref){
		OsmTransportRoute rt = new OsmTransportRoute();
		parseMapObject(rt, r);
		rt.setRef(ref);
		return rt;
	}
	
	public static TransportStop parseTransportStop(Entity e){
		TransportStop st = new TransportStop();
		parseMapObject(st, e);
		return st;
	}

}
