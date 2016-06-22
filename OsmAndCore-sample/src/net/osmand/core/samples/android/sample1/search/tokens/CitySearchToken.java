package net.osmand.core.samples.android.sample1.search.tokens;

import java.math.BigInteger;

public class CitySearchToken extends SearchToken {

	private BigInteger obfId;

	public CitySearchToken(BigInteger obfId, int startIndex, String queryText, String name) {
		super(TokenType.CITY, startIndex, queryText, name);
		this.obfId = obfId;
	}

	public BigInteger getObfId() {
		return obfId;
	}
}
