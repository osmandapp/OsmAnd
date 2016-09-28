package net.osmand.search.core;

import net.osmand.data.LatLon;

public class SearchWord {
	private String word;
	private SearchResult result;
	
	public SearchWord(String word, SearchResult res) {
		this.word = word.trim();
		this.result = res;
	}
	
	public ObjectType getType() {
		return result == null ? ObjectType.UNKNOWN_NAME_FILTER : result.objectType;
	}
	
	public String getWord() {
		return word;
	}
	
	public SearchResult getResult() {
		return result;
	}

	public void syncWordWithResult() {
		word = result.wordsSpan != null ? result.wordsSpan : result.localeName.trim();
	}
	
	public LatLon getLocation() {
		return result == null ? null : result.location;
	}
	
	@Override
	public String toString() {
		return word;
	}
}