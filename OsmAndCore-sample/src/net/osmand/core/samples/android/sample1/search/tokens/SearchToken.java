package net.osmand.core.samples.android.sample1.search.tokens;

import net.osmand.util.Algorithms;

public abstract class SearchToken {

	public enum TokenType {
		OBJECT,
		NAME_FILTER
	}

	private TokenType type;
	private int startIndex;
	protected String plainText;

	public SearchToken(TokenType type, int startIndex, String plainText) {
		this.type = type;
		this.startIndex = startIndex;
		this.plainText = plainText;
	}

	public TokenType getType() {
		return type;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public String getPlainText() {
		return plainText;
	}

	public int getLastIndex() {
		return startIndex + plainText.length() - 1;
	}

	public boolean hasEmptyQuery() {
		return Algorithms.isEmpty(plainText);
	}
}
