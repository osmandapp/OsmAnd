package net.osmand.core.samples.android.sample1.search.tokens;

public class NameFilterSearchToken extends SearchToken {

	public NameFilterSearchToken(int startIndex, String plainText) {
		super(TokenType.NAME_FILTER, startIndex, plainText);
	}
}
