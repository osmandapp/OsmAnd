package net.osmand.search.example.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.OsmAndCollator;
import net.osmand.ResultMatcher;
import net.osmand.StringMatcher;
import net.osmand.binary.BinaryMapAddressReaderAdapter;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchPoiTypeFilter;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.Amenity;
import net.osmand.data.Building;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.Street;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiCategory;
import net.osmand.osm.PoiFilter;
import net.osmand.osm.PoiType;
import net.osmand.search.example.SearchUICore.SearchResultMatcher;
import net.osmand.search.example.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.example.core.SearchPhrase.SearchPhraseDataType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;


public class SearchCoreFactory {
	// TODO display closest city to villages (+)
	// TODO search only closest file (global bbox) (+)

	// TODO buildings interpolation
	// TODO limit to one file if city/street/village/poi selected
	
	// TODO fix search amenity by type (category/additional/filter)
	// TODO streets by city
	// TODO amenity by name
	
	// TODO add location parse
	// TODO add url parse (geo)

	// TODO show buildings if street is one or default ( <CITY>, CITY (den ilp), 1186RM)
	// TODO exclude duplicate streets/cities...

	// TODO MED display results momentarily
	// TODO MED add full text search with comma
	// TODO MED add full text search without comma
	
	// TODO LOW automatically increase radius if nothing found

	
	public static abstract class SearchBaseAPI implements SearchCoreAPI {
		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			return true;
		}
		
		public QuadRect getBBoxToSearch(SearchPhrase phrase, int radiusInMeters) {
			return phrase.getBBoxToSearch(phrase.getRadiusLevel() * radiusInMeters);
		}
		
		@Override
		public int getSearchPriority(SearchPhrase p) {
			return 1;
		}
	}
	
	public static class SearchRegionByNameAPI extends SearchBaseAPI {

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) {
			for (BinaryMapIndexReader bmir : phrase.getOfflineIndexes()) {
				if (bmir.getRegionCenter() != null) {
					SearchResult sr = new SearchResult(phrase);
					sr.localeName = bmir.getRegionName();
					sr.object = bmir;
					sr.file = bmir;
					sr.priority = 3;
					sr.objectType = ObjectType.REGION;
					sr.location = bmir.getRegionCenter();
					sr.preferredZoom = 6;
					if (phrase.getLastWord().length() <= 1 && phrase.isNoSelectedType()) {
						resultMatcher.publish(sr);
					} else if (phrase.getNameStringMatcher().matches(sr.localeName)) {
						resultMatcher.publish(sr);
					}
				}
			}
			return true;
		}
		
		@Override
		public int getSearchPriority(SearchPhrase p) {
			if(!p.isNoSelectedType()) {
				return -1;
			}
			return 1;
		}
	}
	
	public static class SearchAddressByNameAPI extends SearchBaseAPI {
		
		private static final int DEFAULT_ADDRESS_BBOX_RADIUS = 1000*1000;
		private static final int LIMIT = 10000;
		private Map<BinaryMapIndexReader, List<City>> townCities = new LinkedHashMap<>();
		private QuadTree<City> townCitiesQR = new QuadTree<City>(new QuadRect(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE),
				8, 0.55f);

		public boolean isLastWordPoi(SearchPhrase p) {
			return p.isLastWord(ObjectType.POI);
		}
		

		public boolean isNoSelectedType(SearchPhrase p) {
			return p.isNoSelectedType();
		}

		@Override
		public boolean search(final SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
//			if (!phrase.hasObjectType(ObjectType.REGION)) {
//				return false;
//			}
			if (phrase.getLastWord().isEmpty()) {
				return false;
			}
			// (search streets in neighbor cities for radiusLevel > 2)
			if (phrase.isNoSelectedType() ||
					phrase.isLastWord(ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE) ||  // ?
					phrase.isLastWord(ObjectType.REGION) || phrase.getRadiusLevel() >= 2) {
				final boolean locSpecified = phrase.getLastTokenLocation() != null;
				LatLon loc = phrase.getLastTokenLocation();
				final QuadRect streetBbox = getBBoxToSearch(phrase, DEFAULT_ADDRESS_BBOX_RADIUS);
				final QuadRect postcodeBbox = getBBoxToSearch(phrase, DEFAULT_ADDRESS_BBOX_RADIUS * 5);
				final QuadRect villagesBbox = getBBoxToSearch(phrase, DEFAULT_ADDRESS_BBOX_RADIUS * 3);
				final QuadRect cityBbox = getBBoxToSearch(phrase, DEFAULT_ADDRESS_BBOX_RADIUS * 10);
				final int priority = isNoSelectedType(phrase) ? 1 : 3;
				Iterator<BinaryMapIndexReader> offlineIndexes = phrase.getOfflineIndexes(DEFAULT_ADDRESS_BBOX_RADIUS * 10 *
						phrase.getRadiusLevel(), SearchPhraseDataType.ADDRESS);
				while(offlineIndexes.hasNext()) {
					BinaryMapIndexReader r = offlineIndexes.next();
					if(!townCities.containsKey(r)) {
						BinaryMapIndexReader.buildAddressRequest(null);
						List<City> l = r.getCities(null, BinaryMapAddressReaderAdapter.CITY_TOWN_TYPE);
						townCities.put(r, l);
						for(City c  : l) {
							LatLon cl = c.getLocation();
							int y = MapUtils.get31TileNumberY(cl.getLatitude());
							int x = MapUtils.get31TileNumberX(cl.getLongitude());
							QuadRect qr = new QuadRect(x, y, x, y);
							townCitiesQR.insert(c, qr);
						}
					}
				}
				
				
				final BinaryMapIndexReader[] currentFile = new BinaryMapIndexReader[1]; 
				ResultMatcher<MapObject> rm = new ResultMatcher<MapObject>() {
					int limit = 0;
					@Override
					public boolean publish(MapObject object) {
						if(isCancelled()) {
							return false;
						}
						SearchResult sr = new SearchResult(phrase);
						sr.object = object;
						sr.file = currentFile[0];
						sr.localeName = object.getName(phrase.getSettings().getLang(), true);
						sr.otherNames = object.getAllNames(true);
						sr.localeRelatedObjectName = sr.file.getRegionName();
						sr.location = object.getLocation();
						sr.priorityDistance = 1;
						sr.priority = priority;
						int y = MapUtils.get31TileNumberY(object.getLocation().getLatitude());
						int x = MapUtils.get31TileNumberX(object.getLocation().getLongitude());
						List<City> closestCities = null;
						if (object instanceof Street) {
							if(locSpecified && !streetBbox.contains(x, y, x, y)) {
								return false;
							}
							sr.objectType = ObjectType.STREET;
							sr.localeRelatedObjectName = ((Street)object).getCity().getName(phrase.getSettings().getLang(), true);
						} else if (object instanceof City) {
							CityType type = ((City)object).getType();
							if (type == CityType.CITY || type == CityType.TOWN) {
								if (locSpecified && !cityBbox.contains(x, y, x, y)) {
									return false;
								}
								sr.objectType = ObjectType.CITY;
								sr.priorityDistance = 0.1;
							} else if (((City)object).isPostcode()) {
								if (locSpecified && !postcodeBbox.contains(x, y, x, y)) {
									return false;
								}
								sr.objectType = ObjectType.POSTCODE;
							}  else {
								if (locSpecified && !villagesBbox.contains(x, y, x, y)) {
									return false;
								}
								City c = null;
								if(closestCities == null) {
									closestCities = townCitiesQR.queryInBox(villagesBbox, new ArrayList<City>());
								}
								double minDist = -1;
								for(City s : closestCities) {
									double ll = MapUtils.getDistance(s.getLocation(), object.getLocation());
									if(minDist == -1 || ll < minDist) {
										c = s;
										minDist = ll;
									}
								}
								if(c != null) {
									sr.localeRelatedObjectName = c.getName(phrase.getSettings().getLang(), true);
									sr.distRelatedObjectName = minDist; 
								}
								sr.objectType = ObjectType.VILLAGE;
							}
						} else {
							return false;
						}
						limit ++;
						resultMatcher.publish(sr);
						return false;
					}

					@Override
					public boolean isCancelled() {
						return limit > LIMIT * phrase.getRadiusLevel() || 
								resultMatcher.isCancelled();
					}
				};
				
				if(phrase.getRadiusLevel() > 1 || phrase.getLastWord().length() > 2) {
					Iterator<BinaryMapIndexReader> offlineIterator = phrase.getOfflineIndexes(DEFAULT_ADDRESS_BBOX_RADIUS * 10 * 
							phrase.getRadiusLevel(), SearchPhraseDataType.ADDRESS);
					while (offlineIterator.hasNext()) {
						BinaryMapIndexReader r = offlineIterator.next();
						currentFile[0] = r;
						SearchRequest<MapObject> req = BinaryMapIndexReader.buildAddressByNameRequest(rm, phrase
								.getLastWord().toLowerCase(), StringMatcherMode.CHECK_STARTS_FROM_SPACE);
						if (locSpecified) {
							req.setBBoxRadius(loc.getLatitude(), loc.getLongitude(), DEFAULT_ADDRESS_BBOX_RADIUS * 10 * phrase.getRadiusLevel());
						}
						r.searchAddressDataByName(req);
					}
				}
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (p.isNoSelectedType()) {
				return 5;
			}
			return 10;
		}

	}
	
	public static class SearchAmenityByNameAPI extends SearchBaseAPI {
		// TODO
		// BBOX QuadRect bbox = getBBoxToSearch(10000 * typedLettersInStreet, radiusLevel, phrase.getLastTokenLocation());
		// if(poiObjectNotSelected) {
		// 	LIMIT 100
		//      if(poiTypeSelected) { 
		//        BBOX - result priority 5, distPriority 1
		//      } else  {
		//	  BBOX - result priority 5, distPriority 4
		//    	}
		// }
		@Override
		public int getSearchPriority(SearchPhrase p) {
			return -1;
		}
	}
	
	
	public static class SearchAmenityTypesAPI extends SearchBaseAPI {

		
		private Map<String, PoiType> translatedNames;
		private List<PoiFilter> topVisibleFilters;
		private TreeSet<AbstractPoiType> results;

		public SearchAmenityTypesAPI(MapPoiTypes types) {
			translatedNames = types.getAllTranslatedNames(false);
			topVisibleFilters = types.getTopVisibleFilters();
			final net.osmand.Collator clt = OsmAndCollator.primaryCollator();
			results = new TreeSet<>(new Comparator<AbstractPoiType>() {

				@Override
				public int compare(AbstractPoiType o1, AbstractPoiType o2) {
					return clt.compare(o1.getTranslation(), o2.getTranslation());
				}
			});
		}
		
		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			results.clear();
			if(Algorithms.isEmpty(phrase.getLastWord())) {
				results.addAll(topVisibleFilters);
			} else {
				Iterator<Entry<String, PoiType>> it = translatedNames.entrySet().iterator();
				StringMatcher nm = phrase.getNameStringMatcher();
				while (it.hasNext()) {
					Entry<String, PoiType> e = it.next();
					if (nm.matches(e.getKey())) {
						results.add(e.getValue());
					}
				}
			}
			for(AbstractPoiType p : results) {
				SearchResult res = new SearchResult(phrase);
				res.localeName = p.getTranslation();
				res.object = p;
				res.priority = 2;
				res.objectType = ObjectType.POI_TYPE;
				resultMatcher.publish(res);
			}
			return true;
		}
		
		@Override
		public int getSearchPriority(SearchPhrase p) {
			if(p.hasObjectType(ObjectType.POI) ||p.hasObjectType(ObjectType.POI_TYPE)) {
				return -1;
			}
			return 1;
		}
	}
	
	public static class SearchAmenityByTypeAPI extends SearchBaseAPI {
		
		private MapPoiTypes types;

		public SearchAmenityByTypeAPI(MapPoiTypes types) {
			this.types = types;
		}
		public boolean isLastWordPoi(SearchPhrase p ) {
			return p.isLastWord(ObjectType.POI_TYPE);
		}
		
		@Override
		public boolean search(final SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
			if(phrase.isLastWord(ObjectType.POI_TYPE)) {
				final AbstractPoiType pt = (AbstractPoiType) phrase.getLastSelectedWord().getResult().object;
				QuadRect bbox = getBBoxToSearch(phrase, 10000);
				List<BinaryMapIndexReader> oo = phrase.getOfflineIndexes();
				final BinaryMapIndexReader[] selected = new BinaryMapIndexReader[1];
				final NameStringMatcher ns = phrase.getNameStringMatcher();
				SearchRequest<Amenity> req = BinaryMapIndexReader.buildSearchPoiRequest(
						(int)bbox.left, (int)bbox.right, 
						(int)bbox.top, (int)bbox.bottom, -1,
						new SearchPoiTypeFilter() {
							
							@Override
							public boolean isEmpty() {
								return false;
							}
							
							@Override
							public boolean accept(PoiCategory type, String subcategory) {
								PoiType st = types.getPoiTypeByKey(subcategory);
								if(st != null && pt.getKeyName().equals(st.getKeyName())) {
									return true;
								}
								return false;
							}
						}, 
						new ResultMatcher<Amenity>() {
					
					@Override
					public boolean publish(Amenity object) {
						SearchResult res = new SearchResult(phrase);
						res.localeName = object.getName(phrase.getSettings().getLang(), true);
						res.otherNames = object.getAllNames(true);
						if(!Algorithms.isEmpty(phrase.getLastWord()) &&
								!(ns.matches(res.localeName) || ns.matches(res.otherNames))) {
							return false;
						}
						if(Algorithms.isEmpty(res.localeName)) {
							AbstractPoiType st = types.getAnyPoiTypeByKey(object.getSubType());
							if(st != null) {
								res.localeName = st.getTranslation();
							} else {
								res.localeName = "sub:" + object.getSubType();
							}
						}
						res.object = object;
						res.preferredZoom = 17;
						res.file = selected[0];
						res.location = object.getLocation();
						res.priority = 5;
						res.priorityDistance = 1;
						res.objectType = ObjectType.POI;
						resultMatcher.publish(res);
						return false;
					}
					
					@Override
					public boolean isCancelled() {
						return resultMatcher.isCancelled();
					}
				});
				for (BinaryMapIndexReader o : oo) {
					selected[0] = o;
					o.searchPoi(req);
				}
			}
			return true;
		}
	
		@Override
		public int getSearchPriority(SearchPhrase p) {
			if(p.isLastWord(ObjectType.POI_TYPE) && 
					p.getLastTokenLocation() != null) {
				return 1;
			}
			return -1;
		}
	}
	
	
	
	public static class SearchStreetByCityAPI extends SearchBaseAPI {
		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) {
			if(isLastWordCityGroup(phrase)) {
				// search all streets 
				// LIMIT 100 * radiusLevel -  priority 1, distPriority 0 (alphabetic)
			}
			return true;
		}

		public boolean isLastWordCityGroup(SearchPhrase p ) {
			return p.isLastWord(ObjectType.CITY) || p.isLastWord(ObjectType.POSTCODE) || 
					p.isLastWord(ObjectType.VILLAGE);
		}
		
		@Override
		public int getSearchPriority(SearchPhrase p) {
			return 1;
		}
		
	}
	
	
	
	public static class SearchBuildingAndIntersectionsByStreetAPI extends SearchBaseAPI {
		Street cacheBuilding;
		
		@Override
		public boolean search(SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
			if(phrase.isLastWord(ObjectType.STREET)) {
				Street s =  (Street) phrase.getLastSelectedWord().getResult().object;
				BinaryMapIndexReader file = phrase.getLastSelectedWord().getResult().file;
				String lw = phrase.getLastWord();
				NameStringMatcher sm = phrase.getNameStringMatcher();
				if (cacheBuilding != s) {
					cacheBuilding = s;
					SearchRequest<Building> sr = BinaryMapIndexReader
							.buildAddressRequest(new ResultMatcher<Building>() {

								@Override
								public boolean publish(Building object) {
									return true;
								}

								@Override
								public boolean isCancelled() {
									return resultMatcher.isCancelled();
								}
							});
					file.preloadBuildings(s, sr);
					Collections.sort(s.getBuildings(), new Comparator<Building>() {

						@Override
						public int compare(Building o1, Building o2) {
							int i1 = Algorithms.extractFirstIntegerNumber(o1.getName());
							int i2 = Algorithms.extractFirstIntegerNumber(o2.getName());
							if (i1 == i2) {
								return 0;
							}
							return Algorithms.compare(i1, i2);
						}
					});
				}
				for(Building b : s.getBuildings()) {
					SearchResult res = new SearchResult(phrase);
					boolean interpolation = b.belongsToInterpolation(lw);
					if(!sm.matches(b.getName()) && !interpolation) {
						continue;
					}
					
					res.localeName = b.getName(phrase.getSettings().getLang(), true);
					res.otherNames = b.getAllNames(true);
					res.object = b;
					res.file = file;
					res.priorityDistance = 0;
					res.objectType = ObjectType.HOUSE;
					if(interpolation) {
						res.location = b.getLocation(b.interpolation(lw));
					} else {
						res.location = b.getLocation();
					}
					res.preferredZoom = 17;
					resultMatcher.publish(res);
				}
				if(!Algorithms.isEmpty(lw) && !Character.isDigit(lw.charAt(0))) {
					for(Street street : s.getIntersectedStreets()) {
						SearchResult res = new SearchResult(phrase);
						if(!sm.matches(street.getName()) && !sm.matches(street.getAllNames(true))) {
							continue;
						}
						res.otherNames = street.getAllNames(true);
						res.localeName = street.getName(phrase.getSettings().getLang(), true);
						res.object = street;
						res.file = file;
						res.priorityDistance = 0;
						res.objectType = ObjectType.STREET_INTERSECTION;
						res.location = street.getLocation();
						res.preferredZoom = 16;
						resultMatcher.publish(res);
					}
				}
				
				
				
			}
			return true;
		}

		
		@Override
		public int getSearchPriority(SearchPhrase p) {
			if(!p.isLastWord(ObjectType.STREET)) {
				return -1;
			}
			return 1;
		}
	}
	
	
	
	
	
	
}
