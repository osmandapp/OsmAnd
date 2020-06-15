package net.osmand.binary;

import net.osmand.binary.BinaryMapPoiReaderAdapter.PoiRegion;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.MapObject;
import net.osmand.data.Street;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.osmand.binary.BinaryMapAddressReaderAdapter.AddressRegion;

public class BinaryMapIndexTestReader extends BinaryMapIndexReader {

	private List<Amenity> amenities = Collections.emptyList();
	private List<City> cities = Collections.emptyList();
	private List<City> initCities = Collections.emptyList();
	private List<City> matchedCities = Collections.emptyList();
	private List<City> streetCities = Collections.emptyList();

	private BinaryMapIndexTestReader() throws IOException {
		super(null, null, false);
		version = 2;
		dateCreated = System.currentTimeMillis();
	}

	public static BinaryMapIndexReader buildTestReader(File jsonFile) throws IOException {
		String sourceJsonText = Algorithms.getFileAsString(jsonFile);
		JSONObject sourceJson = null;
		if (!Algorithms.isEmpty(sourceJsonText)) {
			sourceJson = new JSONObject(sourceJsonText);
		}
		if (sourceJson == null) {
			return null;
		}
		BinaryMapIndexTestReader reader = new BinaryMapIndexTestReader();
		if (sourceJson.has("amenities")) {
			JSONArray amenitiesArr = sourceJson.getJSONArray("amenities");
			List<Amenity> amenities = new ArrayList<>();
			for (int i = 0; i < amenitiesArr.length(); i++) {
				JSONObject amenityObj = amenitiesArr.getJSONObject(i);
				amenities.add(Amenity.parseJSON(amenityObj));
			}
			reader.amenities = amenities;
			PoiRegion region = new PoiRegion();
			region.name = Algorithms.getFileNameWithoutExtension(jsonFile);
			region.left31 = 0;
			region.top31 = 0;
			region.right31 = Integer.MAX_VALUE;
			region.bottom31 = Integer.MAX_VALUE;
			reader.poiIndexes.add(region);
			reader.indexes.add(region);
		}
		if (sourceJson.has("cities")) {
			JSONArray citiesArr = sourceJson.getJSONArray("cities");
			Set<String> attributeTagsTable = new HashSet<>();
			List<City> cities = new ArrayList<>();
			List<City> initCities = new ArrayList<>();
			List<City> matchedCities = new ArrayList<>();
			List<City> streetCities = new ArrayList<>();
			for (int i = 0; i < citiesArr.length(); i++) {
				JSONObject cityObj = citiesArr.getJSONObject(i);
				final City city = City.parseJSON(cityObj);
				cities.add(city);
				if (cityObj.has("init")) {
					initCities.add(city);
				}
				if (cityObj.has("matchCity")) {
					matchedCities.add(city);
				}
				if (cityObj.has("matchStreet")) {
					streetCities.add(city);
				}
				Set<String> names = city.getNamesMap(false).keySet();
				for (String name : names) {
					attributeTagsTable.add("name:" + name);
				}
				for (Street street : city.getStreets()) {
					names = street.getNamesMap(false).keySet();
					for (String name : names) {
						attributeTagsTable.add("name:" + name);
					}
				}
//				attributeTagsTable.add("name");
//				attributeTagsTable.add("name:en");
			}
			reader.cities = cities;
			reader.initCities = initCities;
			reader.matchedCities = matchedCities;
			reader.streetCities = streetCities;

			AddressRegion region = new AddressRegion();
			region.name = Algorithms.getFileNameWithoutExtension(jsonFile);
			region.left31 = 0;
			region.top31 = 0;
			region.right31 = Integer.MAX_VALUE;
			region.bottom31 = Integer.MAX_VALUE;
			region.attributeTagsTable = new ArrayList<>(attributeTagsTable);
			reader.addressIndexes.add(region);
			reader.indexes.add(region);
		}
		return reader;
	}

	@Override
	public List<Amenity> searchPoiByName(SearchRequest<Amenity> req) throws IOException {
		for (Amenity amenity : amenities) {
			req.publish(amenity);
		}
		return req.getSearchResults();
	}

	@Override
	public List<Amenity> searchPoi(SearchRequest<Amenity> req) throws IOException {
		for (Amenity amenity : amenities) {
			req.publish(amenity);
		}
		return req.getSearchResults();
	}

	@Override
	public List<City> getCities(AddressRegion region, SearchRequest<City> resultMatcher, int cityType) throws IOException {
		return getCities(resultMatcher, cityType);
	}

	@Override
	public List<City> getCities(SearchRequest<City> resultMatcher, int cityType) throws IOException {
		for (City city : cities) {
			if (resultMatcher != null) {
				resultMatcher.publish(city);
			}
		}
		return cities;
	}

	@Override
	public int preloadStreets(City c, SearchRequest<Street> resultMatcher) throws IOException {
		return 0;
	}

	@Override
	public void preloadBuildings(Street s, SearchRequest<Building> resultMatcher) throws IOException {
		// cities must be filled with streets and buildings
	}

	@Override
	public List<MapObject> searchAddressDataByName(SearchRequest<MapObject> req) throws IOException {
		for (City city : streetCities) {
			for (Street street : city.getStreets()) {
				req.publish(street);
			}
		}
		for (City city : matchedCities) {
			req.publish(city);
		}
		return req.getSearchResults();
	}

	@Override
	public String getRegionName() {
		return "Test region";
	}

	@Override
	public boolean containsPoiData(int left31x, int top31y, int right31x, int bottom31y) {
		return true;
	}

	@Override
	public boolean containsMapData() {
		return true;
	}

	@Override
	public boolean containsPoiData() {
		return true;
	}

	@Override
	public boolean containsRouteData() {
		return true;
	}

	@Override
	public boolean containsRouteData(int left31x, int top31y, int right31x, int bottom31y, int zoom) {
		return true;
	}

	@Override
	public boolean containsAddressData(int left31x, int top31y, int right31x, int bottom31y) {
		return true;
	}

	@Override
	public boolean containsMapData(int tile31x, int tile31y, int zoom) {
		return true;
	}

	@Override
	public boolean containsMapData(int left31x, int top31y, int right31x, int bottom31y, int zoom) {
		return true;
	}

	@Override
	public boolean containsAddressData() {
		return true;
	}
}
