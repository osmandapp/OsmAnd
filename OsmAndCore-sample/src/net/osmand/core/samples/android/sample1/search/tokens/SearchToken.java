package net.osmand.core.samples.android.sample1.search.tokens;

import net.osmand.core.samples.android.sample1.search.objects.SearchObject;
import net.osmand.util.Algorithms;

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

	public String getQueryText() {
		return queryText;
	}

	public int getLastIndex() {
		return startIndex + queryText.length() - 1;
	}

	public SearchObject getSearchObject() {
		return searchObject;
	}

	public boolean hasEmptyQuery() {
		return Algorithms.isEmpty(queryText);
	}
}
