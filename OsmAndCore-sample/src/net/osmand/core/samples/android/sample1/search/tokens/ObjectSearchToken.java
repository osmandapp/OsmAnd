package net.osmand.core.samples.android.sample1.search.tokens;

import net.osmand.core.samples.android.sample1.search.objects.SearchObject;

public class ObjectSearchToken extends SearchToken {

	private SearchObject searchObject;
	private boolean suggestion = true;

	public ObjectSearchToken(SearchToken searchToken, SearchObject searchObject, boolean suggestion) {
		super(TokenType.OBJECT, searchToken.getStartIndex(), searchToken.getPlainText());
		this.searchObject = searchObject;
		this.suggestion = suggestion;
	}

	public ObjectSearchToken(int startIndex, String plainText, SearchObject searchObject, boolean suggestion) {
		super(TokenType.OBJECT, startIndex, plainText);
		this.searchObject = searchObject;
		this.suggestion = suggestion;
	}

	public SearchObject getSearchObject() {
		return searchObject;
	}

	public boolean isSuggestion() {
		return suggestion;
	}

	public void applySuggestion() {
		this.suggestion = false;
	}
}
