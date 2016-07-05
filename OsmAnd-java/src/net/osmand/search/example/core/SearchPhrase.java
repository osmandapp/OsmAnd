package net.osmand.search.example.core;

import java.util.ArrayList;
import java.util.List;

import net.osmand.CollatorStringMatcher;
import net.osmand.StringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.data.LatLon;

public class SearchPhrase {
	
	private List<SearchWord> words = new ArrayList<>();
	private LatLon originalLocation;
	private String text = "";
	private String lastWord = "";
	private CollatorStringMatcher sm;
	
	public SearchPhrase(LatLon location) {
		this.originalLocation = location;
	}
	
	public List<SearchWord> getWords() {
		return words;
	}
	
	public SearchPhrase generateNewPhrase(String text) {
		SearchPhrase sp = new SearchPhrase(originalLocation);
		String atext = text;
		if (text.startsWith((this.text + this.lastWord).trim())) {
			// string is longer
			atext = text.substring(this.text.length());
			sp.text = this.text;
			sp.words = new ArrayList<>(this.words);
		} else {
			sp.text = "";
		}
		// TODO Reuse previous search words if it is shorter
		if (!atext.contains(",")) {
			sp.lastWord = atext;
		} else {
			String[] ws = atext.split(",");
			for (int i = 0; i < ws.length - 1; i++) {
				if (ws[i].trim().length() > 0) {
					sp.words.add(new SearchWord(ws[i].trim()));
				}
				sp.text += ws[i] + ",";
			}
		}
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
	
	public boolean isLastWord(ObjectType p) {
		for(int i = words.size() - 1; i >= 0; i--) {
			SearchWord sw = words.get(i);
			if(sw.getType() == ObjectType.POI) {
				return true;
			} else if(sw.getType() != ObjectType.UNKNOWN_NAME_FILTER) {
				return false;
			}
		}
		return false;
	}
	
	public StringMatcher getNameStringMatcher() {
		if(sm != null) {
			return sm;
		}
		sm = new CollatorStringMatcher(lastWord, StringMatcherMode.CHECK_STARTS_FROM_SPACE);
		return sm;
	}
	
	public boolean hasSameConstantWords(SearchPhrase p) {
		return excludefilterWords().equals(p.excludefilterWords());
	}
	
	public String getStringRerpresentation() {
		StringBuilder sb = new StringBuilder();
		for(SearchWord s : words) {
			sb.append(s.getWord()).append(", ");
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
		return lastWord;
	}

	public LatLon getLastTokenLocation() {
		for(int i = words.size() - 1; i >= 0; i--) {
			SearchWord sw = words.get(i);
			if(sw.getLocation() != null) {
				return sw.getLocation();
			}
		}
		// last token or myLocationOrVisibleMap if not selected 
		return originalLocation;
	}

	

}
