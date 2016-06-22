package net.osmand.core.samples.android.sample1.search.tokens;

public abstract class SearchToken {

	public enum TokenType {
		CITY,
		POSTCODE,
		STREET,
		BUILDING,
		POI_CATEGORY,
		POI_FILTER,
		POI_TYPE,
		LOCATION,
		NAME_FILTER
	}

	private TokenType type;
	private int startIndex;
	protected String queryText;
	protected String name;

	public SearchToken(TokenType type, int startIndex, String queryText, String name) {
		this.type = type;
		this.startIndex = startIndex;
		this.queryText = queryText;
		this.name = name;
	}

	public TokenType getType() {
		return type;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public String getQueryText() {
		return queryText;
	}

	public int getLastIndex() {
		return startIndex + queryText.length() - 1;
	}

	public int getQueryTextLenght() {
		return queryText.length();
	}

	public String getName() {
		return name;
	}
}
