package net.osmand.search.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.osmand.CollatorStringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.StringMatcher;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.binary.BinaryMapIndexReader.SearchRequest;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.util.MapUtils;

//immutable object
public class SearchPhrase {
	
	private List<SearchWord> words = new ArrayList<>();
	private String lastWord = "";
	private NameStringMatcher sm;
	private SearchSettings settings;
	private List<BinaryMapIndexReader> indexes;
	private String lastWordTrim;
	private QuadRect cache1kmRect;
	
	public enum SearchPhraseDataType {
		MAP, ADDRESS, ROUTING, POI
	}
	
	
	public SearchPhrase(SearchSettings settings) {
		this.settings = settings;
	}
	
	public SearchPhrase generateNewPhrase(String text, SearchSettings settings) {
		SearchPhrase sp = new SearchPhrase(settings);
		String atext = text;
		List<SearchWord> leftWords = this.words;
		String thisTxt = getText(true);
		if (text.startsWith(thisTxt)) {
			// string is longer
			atext = text.substring(getText(false).length());
			sp.words = new ArrayList<>(this.words);
			leftWords = leftWords.subList(leftWords.size(), leftWords.size());
		}
		if (!atext.contains(",")) {
			sp.lastWord = atext;
			
		} else {
			String[] ws = atext.split(",");
			for (int i = 0; i < ws.length - 1; i++) {
				boolean unknown = true;
				if (ws[i].trim().length() > 0) {
					if (leftWords.size() > 0) {
						if (leftWords.get(0).getWord().equalsIgnoreCase(ws[i].trim())) {
							sp.words.add(leftWords.get(0));
							leftWords = leftWords.subList(1, leftWords.size());
							unknown = false;
						}
					}
					if(unknown) {
						sp.words.add(new SearchWord(ws[i].trim()));
					}
				}
			}
			sp.lastWord = ws[ws.length - 1];
		}
		sp.lastWordTrim = sp.lastWord.trim();
		return sp;
	}
	

	public List<SearchWord> getWords() {
		return words;
	}
	
	
	
	
	public QuadRect getRadiusBBoxToSearch(int radius) {
		int radiusInMeters = getRadiusSearch(radius);
		QuadRect cache1kmRect = get1km31Rect();
		if(cache1kmRect == null) {
			return null;
		}
		int max = (1 << 31) - 1;
		double dx = (cache1kmRect.width() / 2) * radiusInMeters / 1000;
		double dy = (cache1kmRect.height() / 2) * radiusInMeters / 1000;
		double topLeftX = Math.max(0, cache1kmRect.left - dx);
		double topLeftY = Math.max(0, cache1kmRect.top - dy);
		double bottomRightX = Math.min(max, cache1kmRect.right + dx);
		double bottomRightY = Math.min(max, cache1kmRect.bottom + dy);
		return new QuadRect(topLeftX, topLeftY, bottomRightX, bottomRightY);
	}
	
	public QuadRect get1km31Rect() {
		if(cache1kmRect != null) {
			return cache1kmRect;
		}
		LatLon l = getLastTokenLocation();
		if (l == null) {
			return null;
		}
		float coeff = (float) (1000 / MapUtils.getTileDistanceWidth(SearchRequest.ZOOM_TO_SEARCH_POI));
		double tx = MapUtils.getTileNumberX(SearchRequest.ZOOM_TO_SEARCH_POI, l.getLongitude());
		double ty = MapUtils.getTileNumberY(SearchRequest.ZOOM_TO_SEARCH_POI, l.getLatitude());
		double topLeftX = Math.max(0, tx - coeff);
		double topLeftY = Math.max(0, ty - coeff);
		int max = (1 << SearchRequest.ZOOM_TO_SEARCH_POI)  - 1;
		double bottomRightX = Math.min(max, tx + coeff);
		double bottomRightY = Math.min(max, ty + coeff);
		double pw = MapUtils.getPowZoom(31 - SearchRequest.ZOOM_TO_SEARCH_POI);
		cache1kmRect = new QuadRect(topLeftX * pw, topLeftY * pw, bottomRightX * pw, bottomRightY * pw);
		return cache1kmRect;
	}
	
	
	public Iterator<BinaryMapIndexReader> getRadiusOfflineIndexes(int meters, final SearchPhraseDataType dt) {
		final QuadRect rect = meters > 0 ? getRadiusBBoxToSearch(meters) : null;
		return getOfflineIndexes(rect, dt);
		
	}

	public Iterator<BinaryMapIndexReader> getOfflineIndexes(final QuadRect rect, final SearchPhraseDataType dt) {
		List<BinaryMapIndexReader> list = indexes != null ? indexes : settings.getOfflineIndexes();
		final Iterator<BinaryMapIndexReader> lit = list.iterator();
		return new Iterator<BinaryMapIndexReader>() {
			BinaryMapIndexReader next = null;
			@Override
			public boolean hasNext() {
				while (lit.hasNext()) {
					next = lit.next();
					if(rect != null) {
						if(dt == SearchPhraseDataType.POI) {
							if(next.containsPoiData((int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom)) {
								return true;
							}
						} else if(dt == SearchPhraseDataType.ADDRESS) {
							// containsAddressData not all maps supported
							if(next.containsPoiData((int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom) && 
									next.containsAddressData()) {
								return true;
							}
						} else if(dt == SearchPhraseDataType.ROUTING) {
							if(next.containsRouteData((int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom, 15)) {
								return true;
							}
						} else {
							if(next.containsMapData((int)rect.left, (int)rect.top, (int)rect.right, (int)rect.bottom, 15)) {
								return true;
							}
						}
					} else {
						return true;
					}
				}
				return false;
			}

			@Override
			public BinaryMapIndexReader next() {
				return next;
			}

			@Override
			public void remove() {
			}
		};
	}
	
	public List<BinaryMapIndexReader> getOfflineIndexes() {
		if(indexes != null) {
			return indexes; 
		}
		return settings.getOfflineIndexes();
	}
	
	public SearchSettings getSettings() {
		return settings;
	}
	
	
	public int getRadiusLevel() {
		return settings.getRadiusLevel();
	}
	
	public SearchPhrase selectWord(SearchResult res) {
		SearchPhrase sp = new SearchPhrase(this.settings);
		sp.words.addAll(this.words);
		while(res.wordsSpan > 1) {
			if(sp.words.size() > 0) {
				sp.words.remove(sp.words.size() - 1);
			}
			res.wordsSpan--;
		}
		SearchWord sw = new SearchWord(res.localeName.trim(), res);
		sp.words.add(sw);
		return sp;
	}
	
	
	
	public List<SearchWord> excludefilterWords() {
		 List<SearchWord> w = new ArrayList<>();
		 for(SearchWord s : words) {
			 if(s.getResult() == null) {
				 w.add(s);
			 }
		 }
		 return w;
	}
	
	
	
	public boolean isLastWord(ObjectType... p) {
		for (int i = words.size() - 1; i >= 0; i--) {
			SearchWord sw = words.get(i);
			for(ObjectType o : p) {
				if (sw.getType() == o) {
					return true;
				}
			}
			if (sw.getType() != ObjectType.UNKNOWN_NAME_FILTER) {
				return false;
			}
		}
		return false;
	}
	
	
	
	 
	
	
	public NameStringMatcher getNameStringMatcher() {
		if(sm != null) {
			return sm;
		}
		sm = new NameStringMatcher(lastWordTrim, StringMatcherMode.CHECK_STARTS_FROM_SPACE);
		return sm;
	}
	
	public boolean hasSameConstantWords(SearchPhrase p) {
		return excludefilterWords().equals(p.excludefilterWords());
	}
	
	public boolean hasObjectType(ObjectType p) {
		for(SearchWord s : words) {
			if(s.getType() == p) {
				return true;
			}
		}
		return false;
	}
	
	public String getText(boolean includeLastWord) {
		StringBuilder sb = new StringBuilder();
		for(SearchWord s : words) {
			sb.append(s.getWord()).append(", ");
		}
		if(includeLastWord) {
			sb.append(lastWord);
		}
		return sb.toString();
	}
	
	public String getStringRerpresentation() {
		StringBuilder sb = new StringBuilder();
		for(SearchWord s : words) {
			sb.append(s.getWord()).append(" [" + s.getType() + "], ");
		}
		sb.append(lastWord);
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return getStringRerpresentation();
	}

	public boolean isNoSelectedType() {
		return words.isEmpty();
	}

	public boolean isEmpty() {
		return words.isEmpty() && lastWord.isEmpty();
	}
	
	
	public String getLastWord() {
		return lastWordTrim;
	}
	
	
	public SearchWord getLastSelectedWord() {
		if(words.isEmpty()) {
			return null;
		}
		return words.get(words.size() - 1);
	}

	
	public LatLon getWordLocation() {
		for(int i = words.size() - 1; i >= 0; i--) {
			SearchWord sw = words.get(i);
			if(sw.getLocation() != null) {
				return sw.getLocation();
			}
		}
		return null;
	}
	
	public LatLon getLastTokenLocation() {
		for(int i = words.size() - 1; i >= 0; i--) {
			SearchWord sw = words.get(i);
			if(sw.getLocation() != null) {
				return sw.getLocation();
			}
		}
		// last token or myLocationOrVisibleMap if not selected 
		return settings.getOriginalLocation();
	}


	public void selectFile(BinaryMapIndexReader object) {
		if(indexes == null) {
			indexes = new ArrayList<>();
		}
		if(!this.indexes.contains(object)) {
			this.indexes.add(object);
		}
	}

	public void sortFiles() {
		if(indexes == null) {
			indexes = new ArrayList<>(getOfflineIndexes());
		}
		final LatLon ll = getLastTokenLocation();
		if(ll != null) {
			Collections.sort(indexes, new Comparator<BinaryMapIndexReader>() {

				@Override
				public int compare(BinaryMapIndexReader o1, BinaryMapIndexReader o2) {
					LatLon rc1 = o1.getRegionCenter();
					LatLon rc2 = o2.getRegionCenter();
					double d1 = rc1 == null ? 10000000d : MapUtils.getDistance(rc1, ll);
					double d2 = rc2 == null ? 10000000d : MapUtils.getDistance(rc2, ll);
					return Double.compare(d1, d2);
				}
			});
		}
	}
	
	public static class NameStringMatcher implements StringMatcher {

		private CollatorStringMatcher sm;

		public NameStringMatcher(String lastWordTrim, StringMatcherMode checkStartsFromSpace) {
			sm = new CollatorStringMatcher(lastWordTrim, StringMatcherMode.CHECK_STARTS_FROM_SPACE);
		}
		
		public boolean matches(Collection<String> map) {
			if(map == null) {
				return false;
			}
			for(String v : map) {
				if(sm.matches(v)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public boolean matches(String name) {
			return sm.matches(name);
		}
		
	}

	public int getRadiusSearch(int meters) {
		return (1 << (getRadiusLevel() - 1)) * meters;
	}
}
