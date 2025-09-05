package net.osmand.binary;

import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.binary.BinaryMapRouteReaderAdapter.RouteRegion;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.router.BinaryRoutePlanner;
import net.osmand.router.BinaryRoutePlanner.RouteSegmentPoint;
import net.osmand.router.RoutePlannerFrontEnd;
import net.osmand.router.RoutingContext;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GeocodingUtilities {

	private static final Log log = PlatformUtil.getLog(GeocodingUtilities.class);

	// Location to test parameters https://www.openstreetmap.org/#map=18/53.896473/27.540071 (hno 44)
	// BUG https://www.openstreetmap.org/#map=19/50.9356/13.35348 (hno 26) street is 
	public static final float THRESHOLD_MULTIPLIER_SKIP_STREETS_AFTER = 5;
	public static final float STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS = 250;
	public static final float STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS = 400;

	public static final int DISTANCE_STREET_NAME_PROXIMITY_BY_NAME = 15000;
	public static final float DISTANCE_STREET_FROM_CLOSEST_WITH_SAME_NAME = 1000;

	public static final float THRESHOLD_MULTIPLIER_SKIP_BUILDINGS_AFTER = 1.5f;
	public static final float DISTANCE_BUILDING_PROXIMITY = 100;


	public static final Comparator<GeocodingResult> DISTANCE_COMPARATOR = new Comparator<GeocodingResult>() {

		@Override
		public int compare(GeocodingResult o1, GeocodingResult o2) {
			if ((int) o1.getDistance() == (int) o2.getDistance()) {
				return Double.compare(o1.getCityDistance(), o2.getCityDistance());
			}
			return Double.compare(o1.getDistance(), o2.getDistance());
		}
	};

	public static class GeocodingResult {
		public GeocodingResult() {
		}

		public GeocodingResult(GeocodingResult r) {
			this.searchPoint = r.searchPoint;
			this.regionFP = r.regionFP;
			this.regionLen = r.regionLen;
			this.connectionPoint = r.connectionPoint;
			this.streetName = r.streetName;
			this.point = r.point;
			this.building = r.building;
			this.city = r.city;
			this.street = r.street;
		}

		// input
		public LatLon searchPoint;
		// 1st step
		public LatLon connectionPoint;
		public long regionFP;
		public long regionLen;
		public RouteSegmentPoint point;
		public String streetName;
		// justification
		public Building building;
		public String buildingInterpolation;
		public Street street;
		public City city;
		private double dist = -1;
		private double cityDist = -1;

		public LatLon getLocation() {
			return connectionPoint;
		}

		public double getSortDistance() {
			double dist = getDistance();
			if (dist > 0 && building == null) {
				// add extra distance to match buildings first 
				return dist + 50;
			}
			return dist;
		}
		public double getDistance() {
			if (dist == -1 && searchPoint != null) {
				if (building == null && point != null) {
					// Need distance between searchPoint and nearest RouteSegmentPoint here, to approximate distance from neareest named road
					dist = Math.sqrt(point.distToProj);
				} else if (connectionPoint != null) {
					dist = MapUtils.getDistance(connectionPoint, searchPoint);
				}
			}
			return dist;
		}

		public void resetDistance() {
			dist = -1;
			getDistance();
		}

		public double getCityDistance() {
			if (cityDist == -1 && city != null && searchPoint != null) {
				cityDist = MapUtils.getDistance(city.getLocation(), searchPoint);
			}
			return cityDist;
		}

		@Override
		public String toString() {
			StringBuilder bld = new StringBuilder();
			if (building != null) {
				if(buildingInterpolation != null) {
					bld.append(buildingInterpolation);
				} else {
					bld.append(building.getName());
				}
			}
			if (street != null) {
				bld.append(" str. ").append(street.getName()).append(" city ").append(city.getName());
			} else if (streetName != null) {
				bld.append(" str. ").append(streetName);
			} else if (city != null) {
				bld.append(" city ").append(city.getName());
			}
			if (getDistance() > 0) {
				bld.append(" dist=").append((int) getDistance());
			}
			return bld.toString();
		}
	}


	public List<GeocodingResult> reverseGeocodingSearch(RoutingContext ctx, double lat, double lon, boolean allowEmptyNames) throws IOException {
		RoutePlannerFrontEnd rp = new RoutePlannerFrontEnd();
		List<GeocodingResult> lst = new ArrayList<GeocodingUtilities.GeocodingResult>();
		List<RouteSegmentPoint> listR = new ArrayList<BinaryRoutePlanner.RouteSegmentPoint>();
		// we allow duplications to search in both files for boundary regions 
		// here we use same code as for normal routing, so we take into account current profile and sort by priority & distance
		rp.findRouteSegment(lat, lon, ctx, listR, false, true);
		double distSquare = 0;
		Map<String, List<RouteRegion>> streetNames = new HashMap<>();
		for (RouteSegmentPoint p : listR) {
			RouteDataObject road = p.getRoad();
//			System.out.println(road.toString() +  " " + Math.sqrt(p.distSquare));
			String name = Algorithms.isEmpty(road.getName()) ? road.getRef("", false, true) : road.getName();
			if (allowEmptyNames || !Algorithms.isEmpty(name)) {
				if (distSquare == 0 || distSquare > p.distToProj) {
					distSquare = p.distToProj;
				}
				GeocodingResult sr = new GeocodingResult();
				sr.searchPoint = new LatLon(lat, lon);
				sr.streetName = name == null ? "" : name;
				sr.point = p;
				sr.connectionPoint = new LatLon(MapUtils.get31LatitudeY(p.preciseY), MapUtils.get31LongitudeX(p.preciseX));
				sr.regionFP = road.region.getFilePointer();
				sr.regionLen = road.region.getLength();
				List<RouteRegion> plst = streetNames.get(sr.streetName);
				if (plst == null) {
					plst = new ArrayList<BinaryMapRouteReaderAdapter.RouteRegion>();
					streetNames.put(sr.streetName, plst);
				}
				if (!plst.contains(road.region)) {
					plst.add(road.region);
					lst.add(sr);
				}
			}
			if (p.distToProj > STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS * STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS &&
					distSquare != 0 && p.distToProj > THRESHOLD_MULTIPLIER_SKIP_STREETS_AFTER * distSquare) {
				break;
			}
			if (p.distToProj > STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS * STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS) {
				break;
			}
		}
		Collections.sort(lst, GeocodingUtilities.DISTANCE_COMPARATOR);
		return lst;
	}

	public List<String> prepareStreetName(String s, boolean addCommonWords) {
		List<String> ls = new ArrayList<String>();
		int beginning = 0;
		for (int i = 1; i < s.length(); i++) {
			if (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == '-') {
				addWord(ls, s.substring(beginning, i), addCommonWords);
				beginning = i + 1;
			} else if (s.charAt(i) == '(') {
				addWord(ls, s.substring(beginning, i), addCommonWords);
				while (i < s.length()) {
					char c = s.charAt(i);
					i++;
					beginning = i;
					if (c == ')')
						break;
				}

			}
		}
		if (beginning < s.length()) {
			String lastWord = s.substring(beginning, s.length());
			addWord(ls, lastWord, addCommonWords);
		}
		Collections.sort(ls, Collator.getInstance());
		return ls;
	}

	private void addWord(List<String> ls, String word, boolean addCommonWords) {
		String w = word.trim().toLowerCase();
		if (!Algorithms.isEmpty(w)) {
			if(!addCommonWords && CommonWords.getCommonGeocoding(w) != -1) {
				return;
			}
			ls.add(w);
		}
	}

	public List<GeocodingResult> justifyReverseGeocodingSearch(final GeocodingResult road, BinaryMapIndexReader reader,
			double knownMinBuildingDistance, final ResultMatcher<GeocodingResult> result) throws IOException {
		// test address index search
		final List<GeocodingResult> streetsList = new ArrayList<GeocodingResult>();
		boolean addCommonWords = false;
		List<String> streetNamesUsed = prepareStreetName(road.streetName, addCommonWords);
		if(streetNamesUsed.size() == 0) {
			addCommonWords = true;
			streetNamesUsed = prepareStreetName(road.streetName, addCommonWords);
		}
		final boolean addCommonWordsFinal = addCommonWords;
		final List<String> streetNamesUsedFinal = streetNamesUsed;
		if (streetNamesUsedFinal.size() > 0) {
//			log.info("Search street by name " + road.streetName + " " + streetNamesUsedFinal);
			String mainWord = "";
			for (int i = 0; i < streetNamesUsedFinal.size(); i++) {
				String s = streetNamesUsedFinal.get(i);
				if (s.length() > mainWord.length()) {
					mainWord = s;
				}
			}
			SearchRequest<MapObject> req = BinaryMapIndexReader.buildAddressByNameRequest(
					new ResultMatcher<MapObject>() {
						@Override
						public boolean publish(MapObject object) {
							if (object instanceof Street
									&& prepareStreetName(object.getName(), addCommonWordsFinal).equals(streetNamesUsedFinal)) {
								double d = MapUtils.getDistance(object.getLocation(), road.searchPoint.getLatitude(),
										road.searchPoint.getLongitude());
								// double check to suport old format
								if (d < DISTANCE_STREET_NAME_PROXIMITY_BY_NAME) {
									GeocodingResult rs = new GeocodingResult(road);
									rs.street = (Street) object;
									// set connection point to sort
									rs.connectionPoint = rs.street.getLocation();
									rs.city = rs.street.getCity();
									rs.dist = d;
									streetsList.add(rs);
									return true;
								}
								return false;
							}
							return false;
						}

						@Override
						public boolean isCancelled() {
							return result != null && result.isCancelled();
						}
					}, mainWord, StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
			req.setBBoxRadius(road.getLocation().getLatitude(), road.getLocation().getLongitude(), DISTANCE_STREET_NAME_PROXIMITY_BY_NAME);
			reader.searchAddressDataByName(req);
		}

		final List<GeocodingResult> res = new ArrayList<GeocodingResult>();
		if (streetsList.size() == 0) {
			res.add(road);
		} else {
			Collections.sort(streetsList, DISTANCE_COMPARATOR);
			double streetDistance = 0;
			boolean isBuildingFound = knownMinBuildingDistance > 0;
			for (GeocodingResult street : streetsList) {
				if (streetDistance == 0) {
					streetDistance = street.getDistance();
				} else if (isBuildingFound && street.getDistance() > streetDistance + DISTANCE_STREET_FROM_CLOSEST_WITH_SAME_NAME) {
					continue;
				}
				street.resetDistance();//reset to road projection
				street.connectionPoint = road.connectionPoint;
				final List<GeocodingResult> streetBuildings = loadStreetBuildings(road, reader, street);
				Collections.sort(streetBuildings, DISTANCE_COMPARATOR);
				if (streetBuildings.size() > 0) {
					Iterator<GeocodingResult> it = streetBuildings.iterator();
					if (knownMinBuildingDistance == 0) {
						GeocodingResult firstBld = it.next();
						knownMinBuildingDistance = firstBld.getDistance();
						isBuildingFound = true;  
						res.add(firstBld);
					}
					while (it.hasNext()) {
						GeocodingResult nextBld = it.next();
						if (nextBld.getDistance() > knownMinBuildingDistance
								* THRESHOLD_MULTIPLIER_SKIP_BUILDINGS_AFTER) {
							break;
						}
						res.add(nextBld);
					}
				}
				res.add(street);
			}
		}
		Collections.sort(res, DISTANCE_COMPARATOR);
		return res;
	}

	public void filterDuplicateRegionResults(final List<GeocodingResult> res) {
		Collections.sort(res, DISTANCE_COMPARATOR);
		// filter duplicate city results (when building is in both regions on boundary)
		for (int i = 0; i < res.size() - 1;) {
			int cmp = cmpResult(res.get(i), res.get(i + 1));
			if (cmp > 0) {
				res.remove(i);
			} else if (cmp < 0) {
				res.remove(i + 1);
			} else {
				// nothing to delete
				i++;
			}
		}
	}

	private int cmpResult(GeocodingResult gr1, GeocodingResult gr2) {
		boolean eqStreet = Algorithms.stringsEqual(gr1.streetName, gr2.streetName);
		if (eqStreet) {
			boolean sameObj = false;
			if (gr1.city != null && gr2.city != null) {
				if (gr1.building != null && gr2.building != null) {
					if (Algorithms.stringsEqual(gr1.building.getName(), gr2.building.getName())) {
						// same building
						sameObj = true;
					}
				} else if (gr1.building == null && gr2.building == null) {
					// same street
					sameObj = true;
				}
			}
			if (sameObj) {
				double cityDist1 = MapUtils.getDistance(gr1.searchPoint, gr1.city.getLocation());
				double cityDist2 = MapUtils.getDistance(gr2.searchPoint, gr2.city.getLocation());
				if (cityDist1 < cityDist2) {
					return -1;
				} else {
					return 1;
				}
			}
		}
		return 0;
	}

	private List<GeocodingResult> loadStreetBuildings(final GeocodingResult road, BinaryMapIndexReader reader,
			GeocodingResult street) throws IOException {
		final List<GeocodingResult> streetBuildings = new ArrayList<GeocodingResult>();
		reader.preloadBuildings(street.street, null);
//		log.info("Preload buildings " + street.street.getName() + " " + street.city.getName() + " " + street.street.getId());
		for (Building b : street.street.getBuildings()) {
			if (b.getLatLon2() != null) {
				double slat = b.getLocation().getLatitude();
				double slon = b.getLocation().getLongitude();
				double tolat = b.getLatLon2().getLatitude();
				double tolon = b.getLatLon2().getLongitude();
				double coeff = MapUtils.getProjectionCoeff(road.searchPoint.getLatitude(), road.searchPoint.getLongitude(),
						slat, slon, tolat, tolon);
				double plat = slat + (tolat - slat) * coeff;
				double plon = slon + (tolon - slon) * coeff;
				if (MapUtils.getDistance(road.searchPoint, plat, plon) < DISTANCE_BUILDING_PROXIMITY) {
					GeocodingResult bld = new GeocodingResult(street);
					bld.building = b;
					//bld.connectionPoint = b.getLocation();
					bld.connectionPoint = new LatLon(plat, plon);
					streetBuildings.add(bld);
					String nm = b.getInterpolationName(coeff);
					if(!Algorithms.isEmpty(nm)) {
						bld.buildingInterpolation = nm;
					}
				}

			} else if (MapUtils.getDistance(b.getLocation(), road.searchPoint) < DISTANCE_BUILDING_PROXIMITY) {
				GeocodingResult bld = new GeocodingResult(street);
				bld.building = b;
				bld.connectionPoint = b.getLocation();
				streetBuildings.add(bld);
			}
		}
		return streetBuildings;
	}

	public List<GeocodingResult> sortGeocodingResults(List<BinaryMapIndexReader> list, List<GeocodingResult> res) throws IOException {
		List<GeocodingResult> complete = new ArrayList<GeocodingUtilities.GeocodingResult>();
		double minBuildingDistance = 0;
		for (GeocodingResult r : res) {
			BinaryMapIndexReader reader = null;
			for (BinaryMapIndexReader b : list) {
				for (RouteRegion rb : b.getRoutingIndexes()) {
					if (r.regionFP == rb.getFilePointer() && r.regionLen == rb.getLength()) {
						reader = b;
						break;

					}
				}
				if (reader != null) {
					break;
				}
			}
			if (reader != null) {
				List<GeocodingResult> justified = justifyReverseGeocodingSearch(r, reader, minBuildingDistance, null);
				if (!justified.isEmpty()) {
					double md = justified.get(0).getDistance();
					if (minBuildingDistance == 0) {
						minBuildingDistance = md;
					} else {
						minBuildingDistance = Math.min(md, minBuildingDistance);
					}
					justified.get(0).dist = -1;//clear intermediate cached distance
					complete.addAll(justified);
				}
			} else {
				complete.add(r);
			}
		}
		filterDuplicateRegionResults(complete);
		Iterator<GeocodingResult> it = complete.iterator();
		while (it.hasNext()) {
			GeocodingResult r = it.next();
			if (r.building != null && r.getDistance() > minBuildingDistance
					* GeocodingUtilities.THRESHOLD_MULTIPLIER_SKIP_BUILDINGS_AFTER) {
				it.remove();
			}
		}
		Collections.sort(complete, DISTANCE_COMPARATOR);
		return complete;

	}
}
