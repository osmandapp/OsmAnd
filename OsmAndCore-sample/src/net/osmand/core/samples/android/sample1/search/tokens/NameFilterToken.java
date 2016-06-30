package net.osmand.core.samples.android.sample1.search.tokens;

public class NameFilterToken extends SearchToken {

	public NameFilterToken(int startIndex, String plainText) {
		super(TokenType.NAME_FILTER, startIndex, plainText);
	}
}
