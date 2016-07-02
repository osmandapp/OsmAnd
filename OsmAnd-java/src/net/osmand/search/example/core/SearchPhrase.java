package net.osmand.search.example.core;

import java.util.ArrayList;
import java.util.List;

import net.osmand.CollatorStringMatcher;
import net.osmand.StringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.data.LatLon;

public class SearchPhrase {
	
	private List<SearchWord> words = new ArrayList<>();
	private LatLon originalPhraseLocation;
	
	public SearchPhrase(LatLon location) {
		this.originalPhraseLocation = location;
	}
	
	public List<SearchWord> getWords() {
		return words;
	}
	
	public List<SearchWord> excludefilterWords() {
		 List<SearchWord> w = new ArrayList<>();
		 for(SearchWord s : words) {
			 if(s.getType() != ObjectType.NAME_FILTER) {
				 w.add(s);
			 }
		 }
		 return w;
	}
	
	public boolean isLastWord(ObjectType p) {
		for(int i = words.size() - 1; i >= 0; i--) {
			SearchWord sw = words.get(i);
			if(sw.getType() == ObjectType.POI) {
				return true;
			} else if(sw.getType() != ObjectType.NAME_FILTER) {
				return false;
			}
		}
		return false;
	}
	
	public StringMatcher getNameStringMatcher() {
		// TODO
		return new CollatorStringMatcher("NameFitler", StringMatcherMode.CHECK_STARTS_FROM_SPACE);
	}
	
	public boolean hasSameConstantWords(SearchPhrase p) {
		return excludefilterWords().equals(p.excludefilterWords());
	}
	
	public String getStringRerpresentation() {
		StringBuilder sb = new StringBuilder();
		for(SearchWord s : words) {
			sb.append(s).append(", ");
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return getStringRerpresentation();
	}

	public boolean isNoSelectedType() {
		return true;
	}

	public boolean isEmpty() {
		return words.isEmpty();
	}

	public LatLon getLastTokenLocation() {
		for(int i = words.size() - 1; i >= 0; i--) {
			SearchWord sw = words.get(i);
			if(sw.getLocation() != null) {
				return sw.getLocation();
			}
		}
		// last token or myLocationOrVisibleMap if not selected 
		return originalPhraseLocation;
	}

}
