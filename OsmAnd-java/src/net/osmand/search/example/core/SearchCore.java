package net.osmand.search.example.core;

import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.util.MapUtils;


public class SearchCore {
	List<SearchCoreAPI> apis = new ArrayList<>();
	
	public static abstract class SearchBaseAPI implements SearchCoreAPI {
		@Override
		public List<SearchResult> search(SearchPhrase phrase, int radiusLevel, SearchCallback callback,
				List<SearchResult> existingSearchResults) {
			return Collections.emptyList();
		}
		
		public QuadRect getBBoxToSearch(int radiusInMeters, int radiusLevel, LatLon loc) {
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
	
	public static class SearchAmenityByNameAPI extends SearchBaseAPI {
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
		public List<SearchResult> search(SearchPhrase phrase, int radiusLevel, SearchCallback callback,
				List<SearchResult> existingSearchResults) {
			if(isLastWordPoi(phrase)) {
				QuadRect bbox = getBBoxToSearch(10000, radiusLevel, phrase.getLastTokenLocation());
				// TODO NO LIMIT , BBOX - result priority 5, distPriority 1
			}
			return Collections.emptyList();
		}
	
		@Override
		public int getSearchPriority(SearchPhrase p) {
			if(isLastWordPoi(p)) {
				return 1;
			}
			return 10;
		}
	}
	
	public static class SearchAddressByNameAPI extends SearchBaseAPI {
		
		public boolean isLastWordPoi(SearchPhrase p ) {
			return p.isLastWord(ObjectType.POI);
		}
		
		public boolean isNoSelectedType(SearchPhrase p ) {
			return p.isNoSelectedType();
		}
		
		@Override
		public List<SearchResult> search(SearchPhrase phrase, int radiusLevel, SearchCallback callback,
				List<SearchResult> existingSearchResults) {
			//  (search streets in neighboor cities for radiusLevel > 2)
			if((isLastWordPoi(phrase) || isNoSelectedType(phrase) || radiusLevel >= 2
				) && !(phrase.isEmpty())) {
				int typedLettersInStreet = 1;
				QuadRect bbox = getBBoxToSearch(20000 * typedLettersInStreet, radiusLevel, phrase.getLastTokenLocation());
				int priority = isNoSelectedType(phrase) ? 1 : 3;
				// priority 
				//  LIMIT 100 * radiusLevel
				//  BBOX streets  - result priority 5, distPriority ${priority} 
				//  BBOX postcodes  - result priority 5, distPriority ${priority} 
				//  BBOX / 3 (3 times smaller) villages - result priority 5, distPriority ${priority} 
				//  BBOX * 4 (3 times smaller) cities/towns - result priority 5, distPriority (${priority}/10) 
			}
			return Collections.emptyList();
		}
		
		@Override
		public int getSearchPriority(SearchPhrase p) {
			if(isNoSelectedType(p)) {
				return 5;
			}
			if(isLastWordPoi(p)) {
				return 8;
			}
			return 10;
		}
		
	}
	
	public static class SearchStreetByCityAPI extends SearchBaseAPI {
		@Override
		public List<SearchResult> search(SearchPhrase phrase, int radiusLevel, SearchCallback callback,
				List<SearchResult> existingSearchResults) {
			if(isLastWordCityGroup(phrase)) {
				// search all streets 
				// TODO LIMIT 100 * radiusLevel -  priority 1, distPriority 0 (alphabetic)
			}
			return Collections.emptyList();
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
		public List<SearchResult> search(SearchPhrase phrase, int radiusLevel, SearchCallback callback,
				List<SearchResult> existingSearchResults) {
			if(isLastWordStreet(phrase)) {
				// search all buildings 
				// TODO NO LIMIT - priority 1, distPriority 0 (alphabetic)
			}
			return Collections.emptyList();
		}

		public boolean isLastWordStreet(SearchPhrase p ) {
			return p.isLastWord(ObjectType.STREET);
		}
		
		@Override
		public int getSearchPriority(SearchPhrase p) {
			return 1;
		}
	}
	
	
	public void init() {
		apis.add(new SearchAmenityByNameAPI());
		apis.add(new SearchAmenityByTypeAPI());
		apis.add(new SearchAddressByNameAPI());
		apis.add(new SearchStreetByCityAPI());
		apis.add(new SearchBuildingAndIntersectionsByStreetAPI());
	}
	
	private void callToSearchCoreAPI(
			SearchPhrase p,
			ResultMatcher<List<SearchResult<?>>> publisher,
			ResultMatcher<SearchResult<?>> visitor) {
		// sort apis by prioirity to search phrase
		// call apis in for loop
	}
	
	
}
