package net.osmand.core.samples.android.sample1.search.tokens;

import java.math.BigInteger;

public class PostcodeSearchToken extends SearchToken {
	private BigInteger obfId;

	public PostcodeSearchToken(BigInteger obfId, int startIndex, String queryText, String name) {
		super(TokenType.POSTCODE, startIndex, queryText, name);
		this.obfId = obfId;
	}

	public BigInteger getObfId() {
		return obfId;
	}
}
