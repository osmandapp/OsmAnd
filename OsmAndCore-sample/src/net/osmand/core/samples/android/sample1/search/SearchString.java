package net.osmand.core.samples.android.sample1.search;

import net.osmand.core.samples.android.sample1.search.tokens.NameFilterSearchToken;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchString {

	private String queryText = "";
	private List<SearchToken> tokens = new ArrayList<>();

	public SearchString() {
	}

	public String getQueryText() {
		return queryText;
	}

	public synchronized void setQueryText(String queryText) {
		int newTextLength = queryText.length();
		int currTextLength = this.queryText.length();
		boolean isNewText = currTextLength == 0
				|| newTextLength == 0
				|| !this.queryText.regionMatches(0, queryText, 0,
					newTextLength > currTextLength ? currTextLength : newTextLength);

		int lastKnownTokenIndex = -1;
		if (isNewText) {
			tokens.clear();
		} else {
			int brokenTokenIndex = -1;
			for (int i = 0; i < tokens.size(); i++) {
				SearchToken token = tokens.get(i);
				int lastTokenIndex = token.getLastIndex();
				if (lastTokenIndex > newTextLength - 1
						|| (lastTokenIndex < newTextLength - 1 && !startWithDelimiter(queryText.substring(lastTokenIndex + 1)))) {
					brokenTokenIndex = i;
					break;
				}
				lastKnownTokenIndex = token.getLastIndex();
			}

			if (brokenTokenIndex != -1) {
				if (brokenTokenIndex == 0) {
					tokens.clear();
				} else {
					for (int i = tokens.size() - 1; i >= brokenTokenIndex; i--) {
						tokens.remove(i);
					}
				}
			}
		}

		if (newTextLength - 1 > lastKnownTokenIndex) {
			int firstWordIndex = lastKnownTokenIndex + 1;
			for (int i = lastKnownTokenIndex + 1; i < newTextLength; i++) {
				char c = queryText.charAt(i);
				if (isDelimiterChar(c)) {
					if (i == firstWordIndex) {
						firstWordIndex++;
					} else {
						SearchToken token = new NameFilterSearchToken(firstWordIndex, queryText.substring(firstWordIndex, i));
						tokens.add(token);
						firstWordIndex = i + 1;
					}
				}
			}
			if (firstWordIndex <= newTextLength - 1) {
				SearchToken token = new NameFilterSearchToken(firstWordIndex, queryText.substring(firstWordIndex));
				tokens.add(token);
			}
		}

		this.queryText = queryText;
	}

	private boolean startWithDelimiter(String text) {
		char firstChar = text.charAt(0);
		return isDelimiterChar(firstChar);
	}

	private boolean isDelimiterChar(char c) {
		return c == ',' || c == ' ';
	}

	public synchronized SearchToken getNextNameFilterToken() {
		SearchToken res = null;
		if (!tokens.isEmpty()) {
			for (int i = tokens.size() - 1; i >= 0; i--) {
			    SearchToken token = tokens.get(i);
				if (token.getType() == TokenType.NAME_FILTER) {
					res = token;
				} else {
					break;
				}
			}
		}
		return res;
	}

	public synchronized SearchToken getLastToken() {
		if (!tokens.isEmpty()) {
			return tokens.get(tokens.size() - 1);
		}
		return null;
	}

	public synchronized boolean hasNameFilterTokens() {
		return getNextNameFilterToken() != null;
	}

	public synchronized boolean replaceToken(SearchToken oldToken, SearchToken newToken) {
		int index = tokens.indexOf(oldToken);
		if (index != -1) {
			tokens.set(index, newToken);
			return true;
		}
		return false;
	}

	public synchronized Map<TokenType, SearchToken> getResolvedTokens() {
		Map<TokenType, SearchToken> map = new HashMap<>();
		for (SearchToken token : tokens) {
			if (token.getType() != SearchToken.TokenType.NAME_FILTER) {
				map.put(token.getType(), token);
			}
		}
		return map;
	}

	public static void main(String[] args){
		//test
		SearchString searchString = new SearchString();
		searchString.setQueryText("cit");
		searchString.setQueryText("city");
		searchString.setQueryText("city ");
		searchString.setQueryText("city s");
		searchString.setQueryText("city st");
		searchString.setQueryText("city street ");
		searchString.setQueryText("city street 8");
		searchString.setQueryText("new");
	}
}
