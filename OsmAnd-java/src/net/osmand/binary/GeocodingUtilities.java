package net.osmand.binary;

import gnu.trove.set.hash.TLongHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

public class GeocodingUtilities {

	private static final Log log = PlatformUtil.getLog(GeocodingUtilities.class);

	public static final float THRESHOLD_MULTIPLIER_SKIP_BUILDINGS_AFTER = 1.5f;
	public static final float THRESHOLD_MULTIPLIER_SKIP_STREETS_AFTER = 3;
	public static final float DISTANCE_STREET_NAME_PROXIMITY_BY_NAME = 20000;
	public static final float DISTANCE_BULDING_PROXIMITY = 100;
	public static final float THRESHOLD_STREET_CHANGE_CONNECTION_POINT = 400; // not important
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
			this.streetName = r.streetName;
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
			if(dist == -1) {
				dist = MapUtils.getDistance(connectionPoint, searchPoint);
			}
			return dist;
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
		double dist = 0;
		TLongHashSet set = new TLongHashSet();
		Set<String> streetNames = new HashSet<String>();
		for(RouteSegmentPoint p : listR) {
			RouteDataObject road = p.getRoad();
			if(!set.add(road.getId())) {
				continue;
			}
			boolean emptyName = Algorithms.isEmpty(road.getName()) && Algorithms.isEmpty(road.getRef()) ;
			if(!emptyName) {
				if(dist == 0) {
					dist = p.dist;
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
			if(p.dist > 100*100 && dist != 0 && p.dist > 4 * dist ) {
				break;
			}
			if(p.dist > 300*300) {
				break;
			}
		}
		Collections.sort(lst, GeocodingUtilities.DISTANCE_COMPARATOR);
		return lst;
	}
	
	public List<GeocodingResult> justifyReverseGeocodingSearch(final GeocodingResult r, BinaryMapIndexReader reader,
			double knownMinBuidlingDistance) throws IOException {
		// test address index search
		final List<GeocodingResult> streetsList = new ArrayList<GeocodingResult>();
		log.info("Search street by name " + r.streetName);
		SearchRequest<MapObject> req = BinaryMapIndexReader.buildAddressByNameRequest(new ResultMatcher<MapObject>() {
			@Override
			public boolean publish(MapObject object) {
				if(object instanceof Street && object.getName().equalsIgnoreCase(r.streetName)) {
					double d = MapUtils.getDistance(object.getLocation(), r.searchPoint.getLatitude(),
							r.searchPoint.getLongitude());
					if(d < DISTANCE_STREET_NAME_PROXIMITY_BY_NAME) {
						GeocodingResult rs = new GeocodingResult(r);
						rs.street = (Street) object;
						rs.city = rs.street.getCity();
						if(d < THRESHOLD_STREET_CHANGE_CONNECTION_POINT) {
							rs.connectionPoint = rs.street.getLocation();
						}
						streetsList.add(rs);
						return true;
					}
					return false;
				}
				return false;
			}
			@Override
			public boolean isCancelled() {
				return false;
			}
		}, r.streetName);
		reader.searchAddressDataByName(req);
		
		final List<GeocodingResult> res = new ArrayList<GeocodingResult>();
		if(streetsList.size() == 0) {
			res.add(r);
		} else {
			for (GeocodingResult s : streetsList) {
				final List<GeocodingResult> streetBuildings = new ArrayList<GeocodingResult>();
				reader.preloadBuildings(s.street, null);
				log.info("Preload buildings " + s.street.getName() + " " + s.city.getName() + " " + s.street.getId());
				for (Building b : s.street.getBuildings()) {
					if(b.getLatLon2() != null) {
						double slat = b.getLocation().getLatitude();
						double slon = b.getLocation().getLongitude();
						double tolat = b.getLatLon2().getLatitude();
						double tolon = b.getLatLon2().getLongitude();
						double coeff = MapUtils.getProjectionCoeff(r.searchPoint.getLatitude(), r.searchPoint.getLongitude(),
								slat, slon, tolat, tolon);
						double plat = slat + (tolat - slat) * coeff;
						double plon = slon + (tolon - slon) * coeff;
						if (MapUtils.getDistance(r.searchPoint, plat, plon) < DISTANCE_BULDING_PROXIMITY) {
							GeocodingResult bld = new GeocodingResult(s);
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
						
					} else if (MapUtils.getDistance(b.getLocation(), r.searchPoint) < DISTANCE_BULDING_PROXIMITY) {
						GeocodingResult bld = new GeocodingResult(s);
						bld.building = b;
						bld.connectionPoint = b.getLocation();
						streetBuildings.add(bld);
					}
				}
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
				res.add(s);
			}
		}
		Collections.sort(res, DISTANCE_COMPARATOR);
		return res;
	}
}
