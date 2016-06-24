package net.osmand.core.samples.android.sample1.search.tokens;

import net.osmand.core.samples.android.sample1.search.objects.SearchObject;

public class ObjectSearchToken extends SearchToken {

	public ObjectSearchToken(SearchToken searchToken, SearchObject searchObject) {
		super(TokenType.SEARCH_OBJECT, searchToken.getStartIndex(), searchToken.getQueryText(), searchObject);
	}

	public ObjectSearchToken(int startIndex, String queryText, SearchObject searchObject) {
		super(TokenType.SEARCH_OBJECT, startIndex, queryText, searchObject);
	}
}
