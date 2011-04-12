package net.osmand.plus;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;

import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.PostCode;
import net.osmand.data.Street;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapUtils;


public interface RegionAddressRepository {
	
	public static class MapObjectNameDistanceComparator implements Comparator<MapObject> {
		
		private final boolean useEnName;
		private Collator collator = Collator.getInstance();
		private final LatLon location;

		public MapObjectNameDistanceComparator(boolean useEnName, LatLon location){
			this.useEnName = useEnName;
			this.location = location;
		}

		@Override
		public int compare(MapObject object1, MapObject object2) {
			if(object1 == null || object2 == null){
				return object2 == object1 ? 0 : (object1 == null ? -1 : 1); 
			} else {
				int c = collator.compare(object1.getName(useEnName), object2.getName(useEnName));
				if(c == 0 && location != null){
					LatLon l1 = object1.getLocation();
					LatLon l2 = object2.getLocation();
					if(l1 == null || l2 == null){
						return l2 == l1 ? 0 : (l1 == null ? -1 : 1);
					}
					return Double.compare(MapUtils.getDistance(location, l1), MapUtils.getDistance(location, l2));
				}
				return c;
			}
		}
		
	}
	
	public String getName();
	
	public PostCode getPostcode(String name);
	
	public City getCityById(Long id);
	
	public Street getStreetByName(MapObject cityOrPostcode, String name);
	
	public Building getBuildingByName(Street street, String name);
	
	public boolean useEnglishNames();
	
	public void setUseEnglishNames(boolean useEnglishNames);
	
	public void fillWithSuggestedBuildings(PostCode postcode, Street street, String name, List<Building> buildingsToFill);
	
	public void fillWithSuggestedStreetsIntersectStreets(City city, Street st, List<Street> streetsToFill);
	
	public void fillWithSuggestedStreets(MapObject cityOrPostcode, String name, List<Street> streetsToFill);
	
	public void fillWithSuggestedCities(String name, List<MapObject> citiesToFill, LatLon currentLocation);

	public LatLon findStreetIntersection(Street street, Street street2);

	public boolean areCitiesPreloaded();

	public boolean arePostcodesPreloaded();
	
	public void addCityToPreloadedList(City city);
	
	
	public boolean isMapRepository();
	
	// is called on low memory
	public void clearCache();
	
	// called to close resources
	public void close();
	
}
