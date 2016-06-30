package net.osmand.core.samples.android.sample1.search.tokens;

import net.osmand.core.samples.android.sample1.search.objects.SearchObject;

public class ObjectToken extends SearchToken {

	private boolean suggestion = true;

	public ObjectToken(SearchToken searchToken, SearchObject searchObject, boolean suggestion) {
		super(TokenType.SEARCH_OBJECT, searchToken.getStartIndex(), searchToken.getPlainText(), searchObject);
		this.suggestion = suggestion;
	}

	public ObjectToken(int startIndex, String plainText, SearchObject searchObject, boolean suggestion) {
		super(TokenType.SEARCH_OBJECT, startIndex, plainText, searchObject);
		this.suggestion = suggestion;
	}

	public boolean isSuggestion() {
		return suggestion;
	}

	public void applySuggestion() {
		this.suggestion = false;
	}
}
