package net.osmand.core.samples.android.sample1.search.tokens;

import net.osmand.core.samples.android.sample1.search.objects.SearchObject;

public abstract class SearchToken {

	public enum TokenType {
		SEARCH_OBJECT,
		NAME_FILTER
	}

	private TokenType type;
	private SearchObject searchObject;
	private int startIndex;
	protected String queryText;

	public SearchToken(TokenType type, int startIndex, String queryText, SearchObject searchObject) {
		this.type = type;
		this.startIndex = startIndex;
		this.queryText = queryText;
		this.searchObject = searchObject;
	}

	public TokenType getType() {
		return type;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public String getQueryText() {
		return queryText;
	}

	public int getLastIndex() {
		return startIndex + queryText.length() - 1;
	}

	public int getQueryTextLenght() {
		return queryText.length();
	}

	public SearchObject getSearchObject() {
		return searchObject;
	}
}
