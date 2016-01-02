package net.osmand.binary;

import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Building;
import net.osmand.data.Building.BuildingInterpolation;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import gnu.trove.set.hash.TLongHashSet;

public class GeocodingUtilities {

	private static final Log log = PlatformUtil.getLog(GeocodingUtilities.class);
	
	public static final float THRESHOLD_MULTIPLIER_SKIP_STREETS_AFTER = 4;
	public static final float STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS = 100;
	public static final float STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS = 400;
	
	public static final float DISTANCE_STREET_NAME_PROXIMITY_BY_NAME = 15000;
	public static final float DISTANCE_STREET_FROM_CLOSEST_WITH_SAME_NAME = 1000;
	
	public static final float THRESHOLD_MULTIPLIER_SKIP_BUILDINGS_AFTER = 1.5f;
	public static final float DISTANCE_BULDING_PROXIMITY = 100;
	
	public static final String[] SUFFIXES = new String[] {
		"av.", "avenue", "просп.", "пер.", "пр.","заул.", "проспект", "переул.", "бул.", "бульвар", "тракт"};
	public static final String[] DEFAULT_SUFFIXES = new String[] {
		"str.", "street", "улица", "ул.", "вулица", "вул.", "вулиця"};
	private static Set<String> SET_DEF_SUFFIXES = null;
	private static Set<String> SET_SUFFIXES = null;
	
	public static Set<String> getDefSuffixesSet() {
		if(SET_DEF_SUFFIXES == null) {
			SET_DEF_SUFFIXES = new TreeSet<String>();
			SET_DEF_SUFFIXES.addAll(Arrays.asList(DEFAULT_SUFFIXES));
		}
		return SET_DEF_SUFFIXES;
	}
	
	public static Set<String> getSuffixesSet() {
		if(SET_SUFFIXES == null) {
			SET_SUFFIXES = new TreeSet<String>();
			SET_SUFFIXES.addAll(Arrays.asList(SUFFIXES));
		}
		return SET_SUFFIXES;
	}

	public static final Comparator<GeocodingResult> DISTANCE_COMPARATOR = new Comparator<GeocodingResult>() {

		@Override
		public int compare(GeocodingResult o1, GeocodingResult o2) {
			LatLon l1 = o1.getLocation();
			LatLon l2 = o2.getLocation();
			if(l1 == null || l2 == null){
				return l2 == l1 ? 0 : (l1 == null ? -1 : 1);
			}
			return Double.compare(MapUtils.getDistance(l1, o1.searchPoint),
					MapUtils.getDistance(l2, o2.searchPoint));
		}
	};
	
	public static class GeocodingResult {
		public GeocodingResult(){
		}
		
		public GeocodingResult(GeocodingResult r){
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
		public int regionFP;
		public int regionLen;
		public RouteSegmentPoint point;
		public String streetName;
		// justification
		public Building building;
		public String buildingInterpolation;
		public Street street;
		public City city;
		private double dist = -1;
		
		public LatLon getLocation() {
			return connectionPoint;
		}
		
		public double getDistance() {
			if(dist == -1 && connectionPoint != null && searchPoint != null) {
				dist = MapUtils.getDistance(connectionPoint, searchPoint);
			}
			return dist;
		}

		public double getDistanceP() {
			if(point != null && searchPoint != null) {
				// Need distance between searchPoint and nearest RouteSegmentPoint here, to approximate distance from neareest named road
				return Math.sqrt(point.distSquare);
			} else {
				return -1;
			}
		}

		@Override
		public String toString() {
			StringBuilder bld = new StringBuilder();
			if(building != null) {
				bld.append(building.getName());
			}
			if(street != null) {
				bld.append(" str. ").append(street.getName()).append(" city ").append(city.getName());
			} else if(streetName != null) {
				bld.append(" str. ").append(streetName);
			} else if(city != null) {
				bld.append(" city ").append(city.getName());
			}
			if(connectionPoint != null && searchPoint != null) {
				
				bld.append(" dist=").append((int) getDistance());
			}
			return bld.toString();
		}
	}

	
	
	public List<GeocodingResult> reverseGeocodingSearch(RoutingContext ctx, double lat, double lon) throws IOException {
		RoutePlannerFrontEnd rp = new RoutePlannerFrontEnd(false);
		List<GeocodingResult> lst = new ArrayList<GeocodingUtilities.GeocodingResult>();
		List<RouteSegmentPoint> listR = new ArrayList<BinaryRoutePlanner.RouteSegmentPoint>();
		rp.findRouteSegment(lat, lon, ctx, listR);
		double distSquare = 0;
		TLongHashSet set = new TLongHashSet();
		Set<String> streetNames = new HashSet<String>();
		for(RouteSegmentPoint p : listR) {
			RouteDataObject road = p.getRoad();
			if(!set.add(road.getId())) {
				continue;
			}
//			System.out.println(road.toString() +  " " + Math.sqrt(p.distSquare));
			boolean emptyName = Algorithms.isEmpty(road.getName()) && Algorithms.isEmpty(road.getRef()) ;
			if(!emptyName) {
				if(distSquare == 0 || distSquare > p.distSquare) {
					distSquare = p.distSquare;
				}
				GeocodingResult sr = new GeocodingResult();
				sr.searchPoint = new LatLon(lat, lon);
				sr.streetName = Algorithms.isEmpty(road.getName())? road.getRef() : road.getName();
				sr.point = p;
				sr.connectionPoint = new LatLon(MapUtils.get31LatitudeY(p.preciseY), MapUtils.get31LongitudeX(p.preciseX));
				sr.regionFP = road.region.getFilePointer();
				sr.regionLen = road.region.getLength();
				if(streetNames.add(sr.streetName)) {
					lst.add(sr);
				}
			}
			if(p.distSquare > STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS * STOP_SEARCHING_STREET_WITH_MULTIPLIER_RADIUS && 
					distSquare != 0 && p.distSquare > THRESHOLD_MULTIPLIER_SKIP_STREETS_AFTER * distSquare ) {
				break;
			}
			if(p.distSquare > STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS*STOP_SEARCHING_STREET_WITHOUT_MULTIPLIER_RADIUS) {
				break;
			}
		}
		Collections.sort(lst, GeocodingUtilities.DISTANCE_COMPARATOR);
		return lst;
	}
	
	public List<String> prepareStreetName(String s) {
		List<String> ls = new ArrayList<String>();
		int beginning = 0;
		for (int i = 1; i < s.length(); i++) {
			if (s.charAt(i) == ' ') {
				addWord(ls, s.substring(beginning, i));
				beginning = i;
			} else if (s.charAt(i) == '(') {
				addWord(ls, s.substring(beginning, i));
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
			addWord(ls, lastWord);
		}
		Collections.sort(ls, Collator.getInstance());
		return ls;
	}

	private void addWord(List<String> ls, String word) {
		String w = word.trim().toLowerCase();
		if (!Algorithms.isEmpty(w) && !getDefSuffixesSet().contains(w)) {
			ls.add(w);
		}
	}
	
	public List<GeocodingResult> justifyReverseGeocodingSearch(final GeocodingResult road, BinaryMapIndexReader reader,
			double knownMinBuidlingDistance, final ResultMatcher<GeocodingResult> result) throws IOException {
		// test address index search
		final List<GeocodingResult> streetsList = new ArrayList<GeocodingResult>();
		final List<String> streetNamePacked = prepareStreetName(road.streetName);
		if (streetNamePacked.size() > 0) {
			log.info("Search street by name " + road.streetName + " " + streetNamePacked);
			String mainWord = "";
			for(int i = 0; i < streetNamePacked.size(); i++) {
				String s = streetNamePacked.get(i);
				if(!getSuffixesSet().contains(s) && s.length() > mainWord.length()) {
					mainWord = s;
				}
			}
			if(Algorithms.isEmpty(mainWord)) {
				mainWord = streetNamePacked.get(0);
			}
			SearchRequest<MapObject> req = BinaryMapIndexReader.buildAddressByNameRequest(
					new ResultMatcher<MapObject>() {
						@Override
						public boolean publish(MapObject object) {
							if (object instanceof Street
									&& prepareStreetName(object.getName()).equals(streetNamePacked)) {
								double d = MapUtils.getDistance(object.getLocation(), road.searchPoint.getLatitude(),
										road.searchPoint.getLongitude());
								if (d < DISTANCE_STREET_NAME_PROXIMITY_BY_NAME) {
									GeocodingResult rs = new GeocodingResult(road);
									rs.street = (Street) object;
									// set connection point to sort
									rs.connectionPoint = rs.street.getLocation();
									rs.city = rs.street.getCity();
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
					}, mainWord);
			reader.searchAddressDataByName(req);
		}
		
		final List<GeocodingResult> res = new ArrayList<GeocodingResult>();
		if(streetsList.size() == 0) {
			res.add(road);
		} else {
			Collections.sort(streetsList, DISTANCE_COMPARATOR);
			double streetDistance = 0;
			for (GeocodingResult street : streetsList) {
				if(streetDistance == 0) {
					streetDistance = street.getDistance();
				} else if(street.getDistance() > streetDistance + DISTANCE_STREET_FROM_CLOSEST_WITH_SAME_NAME) {
					continue;
				}
				street.connectionPoint = road.connectionPoint;
				final List<GeocodingResult> streetBuildings = loadStreetBuildings(road, reader, street);
				Collections.sort(streetBuildings, DISTANCE_COMPARATOR);
				if (streetBuildings.size() > 0) {
					Iterator<GeocodingResult> it = streetBuildings.iterator();
					if (knownMinBuidlingDistance == 0) {
						GeocodingResult firstBld = it.next();
						knownMinBuidlingDistance = firstBld.getDistance();
						res.add(firstBld);
					}
					while (it.hasNext()) {
						GeocodingResult nextBld = it.next();
						if (nextBld.getDistance() > knownMinBuidlingDistance
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

	private List<GeocodingResult> loadStreetBuildings(final GeocodingResult road, BinaryMapIndexReader reader,
			GeocodingResult street) throws IOException {
		final List<GeocodingResult> streetBuildings = new ArrayList<GeocodingResult>();
		reader.preloadBuildings(street.street, null);
		log.info("Preload buildings " + street.street.getName() + " " + street.city.getName() + " " + street.street.getId());
		for (Building b : street.street.getBuildings()) {
			if(b.getLatLon2() != null) {
				double slat = b.getLocation().getLatitude();
				double slon = b.getLocation().getLongitude();
				double tolat = b.getLatLon2().getLatitude();
				double tolon = b.getLatLon2().getLongitude();
				double coeff = MapUtils.getProjectionCoeff(road.searchPoint.getLatitude(), road.searchPoint.getLongitude(),
						slat, slon, tolat, tolon);
				double plat = slat + (tolat - slat) * coeff;
				double plon = slon + (tolon - slon) * coeff;
				if (MapUtils.getDistance(road.searchPoint, plat, plon) < DISTANCE_BULDING_PROXIMITY) {
					GeocodingResult bld = new GeocodingResult(street);
					bld.building = b;
					bld.connectionPoint = b.getLocation();
					streetBuildings.add(bld);	
					if(!Algorithms.isEmpty(b.getName2())) {
						int fi = Algorithms.extractFirstIntegerNumber(b.getName());
						int si = Algorithms.extractFirstIntegerNumber(b.getName2());
						if(si != 0 && fi != 0) {
							int num = (int) (fi + (si - fi) * coeff);
							BuildingInterpolation type = b.getInterpolationType();
							if(type == BuildingInterpolation.EVEN || type == BuildingInterpolation.ODD) {
								if(num % 2 == (type == BuildingInterpolation.EVEN ? 1 : 0)) {
									num --;
								}
							} else if(b.getInterpolationInterval() > 0){
								int intv = b.getInterpolationInterval();
								if ((num - fi) % intv != 0) {
									num = ((num - fi) / intv) * intv + fi;
								}
							}
							bld.buildingInterpolation = num +"";
						}
					}
				}
				
			} else if (MapUtils.getDistance(b.getLocation(), road.searchPoint) < DISTANCE_BULDING_PROXIMITY) {
				GeocodingResult bld = new GeocodingResult(street);
				bld.building = b;
				bld.connectionPoint = b.getLocation();
				streetBuildings.add(bld);
			}
		}
		return streetBuildings;
	}
}
