package net.osmand.search.example.core;

import java.util.ArrayList;
import java.util.List;

import net.osmand.CollatorStringMatcher;
import net.osmand.StringMatcher;
import net.osmand.CollatorStringMatcher.StringMatcherMode;
import net.osmand.data.LatLon;

public class SearchPhrase {
	
	public static class SearchWord {
		public String word;
		public ObjectType type;
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + ((word == null) ? 0 : word.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SearchWord other = (SearchWord) obj;
			if (type != other.type)
				return false;
			if (word == null) {
				if (other.word != null)
					return false;
			} else if (!word.equals(other.word))
				return false;
			return true;
		}


		@Override
		public String toString() {
			return word;
		}
	}
	
	public List<SearchWord> words = new ArrayList<>();
	
	public LatLon myLocationOrVisibleMap = new LatLon(0, 0); 
	
	public List<SearchWord> excludefilterWords() {
		 List<SearchWord> w = new ArrayList<>();
		 for(SearchWord s : words) {
			 if(s.type != ObjectType.NAME_FILTER) {
				 w.add(s);
			 }
		 }
		 return w;
	}
	
	public boolean isLastWord(ObjectType p) {
		return true; // TODO
	}
	
	public StringMatcher getNameStringMatcher() {
		return new CollatorStringMatcher("NameFitler", StringMatcherMode.CHECK_STARTS_FROM_SPACE);
	}
	
	public boolean hasSameConstantWords(SearchPhrase p) {
		return excludefilterWords().equals(p.excludefilterWords());
	}
	
	@Override
	public String toString() {
		String w = words.toString();
		return w.substring(1, w.length() - 1);
	}

	public boolean isNoSelectedType() {
		return true;
	}

	public boolean isEmpty() {
		return false;
	}

	public LatLon getLastTokenLocation() {
		// last token or myLocationOrVisibleMap if not selected 
		return myLocationOrVisibleMap;
	}

}
