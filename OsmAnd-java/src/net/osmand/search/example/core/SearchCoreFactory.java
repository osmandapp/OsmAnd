package net.osmand.search.example.core;

import java.io.IOException;

import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.City;
import net.osmand.data.City.CityType;
import net.osmand.data.LatLon;
import net.osmand.data.MapObject;
import net.osmand.data.QuadRect;
import net.osmand.data.Street;
import net.osmand.search.example.SearchUICore.SearchResultMatcher;
import net.osmand.util.MapUtils;


public class SearchCoreFactory {
	// TODO sort offline indexes by location
	// TODO test add and delete characters
	// TODO add location parse
	// TODO add url parse (geo)
	// TODO ? boolean hasSameConstantWords = p.hasSameConstantWords(this.phrase);
	// TODO amenity types
	// TODO amenity by type
	// TODO streets by city
	// TODO buildings by street
	// TODO display closest city to villages
	// TODO automatically increase radius if nothing found
	
	public static abstract class SearchBaseAPI implements SearchCoreAPI {
		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) throws IOException {
			return true;
		}
		
		public QuadRect getBBoxToSearch(int radiusInMeters, int radiusLevel, LatLon loc) {
			if(loc == null) {
				return null;
			}
			float calcRadios = radiusLevel * radiusInMeters;
			float coeff = (float) (calcRadios / MapUtils.getTileDistanceWidth(SearchRequest.ZOOM_TO_SEARCH_POI));
			double tx = MapUtils.getTileNumberX(SearchRequest.ZOOM_TO_SEARCH_POI, loc.getLongitude());
			double ty = MapUtils.getTileNumberY(SearchRequest.ZOOM_TO_SEARCH_POI, loc.getLatitude());
			double topLeftX = tx - coeff;
			double topLeftY = ty - coeff;
			double bottomRightX = tx + coeff;
			double bottomRightY = ty + coeff;
			return new QuadRect(topLeftX, bottomRightY, bottomRightX, topLeftY);
		}
		
		@Override
		public int getSearchPriority(SearchPhrase p) {
			return 10;
		}
	}
	
	public static class SearchRegionByNameAPI extends SearchBaseAPI {

		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) {
			if(phrase.hasObjectType(ObjectType.REGION)) {
				return false;
			}
			for (BinaryMapIndexReader bmir : phrase.getOfflineIndexes()) {
				if (bmir.getRegionCenter() != null) {
					SearchResult sr = new SearchResult(phrase);
					sr.mainName = bmir.getRegionName();
					sr.object = bmir;
					sr.objectType = ObjectType.REGION;
					sr.location = bmir.getRegionCenter();
					sr.preferredZoom = 6;
					if (phrase.getLastWord().length() <= 2 && phrase.isNoSelectedType()) {
						resultMatcher.publish(sr);
					} else if (phrase.getNameStringMatcher().matches(sr.mainName)) {
						resultMatcher.publish(sr);
					}
				}
			}
			return true;
		}
		
		@Override
		public int getSearchPriority(SearchPhrase p) {
			return 1;
		}
	}
	
	public static class SearchAddressByNameAPI extends SearchBaseAPI {
		
		private static final int DEFAULT_BBOX_RADIUS = 1000*10000;
		private static final int LIMIT = 100;

		public boolean isLastWordPoi(SearchPhrase p) {
			return p.isLastWord(ObjectType.POI);
		}
		

		public boolean isNoSelectedType(SearchPhrase p) {
			return p.isNoSelectedType();
		}

		@Override
		public boolean search(final SearchPhrase phrase, final SearchResultMatcher resultMatcher) throws IOException {
			if (!phrase.hasObjectType(ObjectType.REGION)) {
				return false;
			}
			if (phrase.getLastWord().isEmpty()) {
				return false;
			}
			// (search streets in neighboor cities for radiusLevel > 2)
			if (isLastWordPoi(phrase) || isNoSelectedType(phrase) ||
					phrase.isLastWord(ObjectType.CITY, ObjectType.VILLAGE, ObjectType.POSTCODE) || 
					phrase.isLastWord(ObjectType.REGION) || phrase.getRadiusLevel() >= 2) {
				int letters = phrase.getLastWord().length() / 3 + 1;
				final boolean locSpecified = false; // phrase.getLastTokenLocation() != null;
				LatLon loc = phrase.getLastTokenLocation();
				final QuadRect streetBbox = getBBoxToSearch(DEFAULT_BBOX_RADIUS * letters, phrase.getRadiusLevel(),
						phrase.getLastTokenLocation());
				final QuadRect postcodeBbox = getBBoxToSearch(DEFAULT_BBOX_RADIUS * 2 * letters, phrase.getRadiusLevel(),
						phrase.getLastTokenLocation());
				final QuadRect villagesBbox = getBBoxToSearch(DEFAULT_BBOX_RADIUS * letters, phrase.getRadiusLevel(),
						phrase.getLastTokenLocation());
				final QuadRect cityBbox = getBBoxToSearch(DEFAULT_BBOX_RADIUS * 4 * letters, phrase.getRadiusLevel(),
						phrase.getLastTokenLocation());
				final int priority = isNoSelectedType(phrase) ? 1 : 3;
				ResultMatcher<MapObject> rm = new ResultMatcher<MapObject>() {
					int limit = 0;
					@Override
					public boolean publish(MapObject object) {
						if(isCancelled()) {
							return false;
						}
						SearchResult sr = new SearchResult(phrase);
						sr.object = object;
						sr.mainName = object.getName();
						sr.location = object.getLocation();
						sr.priorityDistance = 1;
						sr.priority = priority;
						int y = MapUtils.get31TileNumberY(object.getLocation().getLatitude());
						int x = MapUtils.get31TileNumberX(object.getLocation().getLongitude());
						if (object instanceof Street) {
							if(locSpecified && !streetBbox.contains(x, y, x, y)) {
								return false;
							}
							sr.objectType = ObjectType.STREET;
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
				for(BinaryMapIndexReader r : phrase.getOfflineIndexes()) {
					SearchRequest<MapObject> req = BinaryMapIndexReader.buildAddressByNameRequest(rm, 
							phrase.getLastWord().toLowerCase(), StringMatcherMode.CHECK_ONLY_STARTS_WITH);
					if(locSpecified) {
						req.setBBoxRadius(loc.getLatitude(), loc.getLongitude(), DEFAULT_BBOX_RADIUS * 4 * letters);
					}
					r.searchAddressDataByName(req);
				}
			}
			return true;
		}

		@Override
		public int getSearchPriority(SearchPhrase p) {
			if (isNoSelectedType(p)) {
				return 5;
			}
			if (isLastWordPoi(p)) {
				return 8;
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
			return 10;
		}
	}
	
	public static class SearchAmenityByTypeAPI extends SearchBaseAPI {
		public boolean isLastWordPoi(SearchPhrase p ) {
			return p.isLastWord(ObjectType.POI_TYPE);
		}
		
		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) {
			if(isLastWordPoi(phrase)) {
				QuadRect bbox = getBBoxToSearch(10000, phrase.getRadiusLevel(), phrase.getLastTokenLocation());
				// TODO NO LIMIT , BBOX - result priority 5, distPriority 1
			}
			return true;
		}
	
		@Override
		public int getSearchPriority(SearchPhrase p) {
			if(isLastWordPoi(p)) {
				return 1;
			}
			return 10;
		}
	}
	
	
	
	public static class SearchStreetByCityAPI extends SearchBaseAPI {
		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) {
			if(isLastWordCityGroup(phrase)) {
				// search all streets 
				// TODO LIMIT 100 * radiusLevel -  priority 1, distPriority 0 (alphabetic)
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
		
		@Override
		public boolean search(SearchPhrase phrase, SearchResultMatcher resultMatcher) {
			if(isLastWordStreet(phrase)) {
				// search all buildings 
				// TODO NO LIMIT - priority 1, distPriority 0 (alphabetic)
			}
			return true;
		}

		public boolean isLastWordStreet(SearchPhrase p ) {
			return p.isLastWord(ObjectType.STREET);
		}
		
		@Override
		public int getSearchPriority(SearchPhrase p) {
			return 1;
		}
	}
	
	
	
	
	
	
}
