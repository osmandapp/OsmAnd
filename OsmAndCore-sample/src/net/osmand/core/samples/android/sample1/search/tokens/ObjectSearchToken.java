package net.osmand.core.samples.android.sample1.search.tokens;

import net.osmand.core.samples.android.sample1.search.objects.SearchObject;

public class ObjectSearchToken extends SearchToken {

	private boolean suggestion = true;

	public ObjectSearchToken(SearchToken searchToken, SearchObject searchObject, boolean suggestion) {
		super(TokenType.SEARCH_OBJECT, searchToken.getStartIndex(), searchToken.getQueryText(), searchObject);
		this.suggestion = suggestion;
	}

	public ObjectSearchToken(int startIndex, String queryText, SearchObject searchObject, boolean suggestion) {
		super(TokenType.SEARCH_OBJECT, startIndex, queryText, searchObject);
		this.suggestion = suggestion;
	}

	public boolean isSuggestion() {
		return suggestion;
	}

	public void applySuggestion() {
		this.suggestion = false;
	}
}
