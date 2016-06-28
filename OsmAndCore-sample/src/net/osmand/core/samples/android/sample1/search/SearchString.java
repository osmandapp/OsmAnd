package net.osmand.core.samples.android.sample1.search;

import android.support.annotation.NonNull;

import net.osmand.core.samples.android.sample1.MapUtils;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject.SearchObjectType;
import net.osmand.core.samples.android.sample1.search.tokens.NameFilterSearchToken;
import net.osmand.core.samples.android.sample1.search.tokens.ObjectSearchToken;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken.TokenType;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SearchString {

	private String queryText = "";
	private List<SearchToken> tokens = new ArrayList<>();
	private String lang;

	public SearchString(String lang) {
		this.lang = lang;
	}

	public SearchString copy() {
		SearchString res = new SearchString(lang);
		res.queryText = queryText;
		res.tokens = new ArrayList<>(tokens);
		return res;
	}

	public String getQueryText() {
		return queryText;
	}

	public void setQueryText(String queryText) {
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
						|| token.hasEmptyQuery()
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
			} else if (endWithDelimeter(queryText)) {
				SearchToken lastToken = getLastToken();
				if (lastToken.getType() == TokenType.SEARCH_OBJECT) {
					((ObjectSearchToken) lastToken).applySuggestion();
				}
				SearchToken token = new NameFilterSearchToken(firstWordIndex, "");
				tokens.add(token);
			}
		}

		this.queryText = queryText;
	}

	public void completeQuery(@NonNull SearchObject searchObject) {
		String newQueryText;
		String objectName = searchObject.getName(lang);
		int startIndex;
		SearchToken lastToken = getLastToken();
		if (lastToken == null || lastToken.hasEmptyQuery()) {
			startIndex = queryText.length();
			newQueryText = queryText + objectName + " ";
		} else {
			startIndex = lastToken.getStartIndex();
			newQueryText = queryText.substring(0, startIndex) + objectName + " ";
		}
		ObjectSearchToken token = new ObjectSearchToken(startIndex, objectName, searchObject, false);
		if (lastToken == null) {
			tokens.add(token);
		} else {
			tokens.set(tokens.size() - 1, token);
		}
		tokens.add(new NameFilterSearchToken(newQueryText.length(), ""));
		queryText = newQueryText;
	}

	private boolean endWithDelimeter(String text) {
		return !Algorithms.isEmpty(text) && isDelimiterChar(text.charAt(text.length() - 1));
	}

	private boolean startWithDelimiter(String text) {
		char firstChar = text.charAt(0);
		return isDelimiterChar(firstChar);
	}

	private boolean isDelimiterChar(char c) {
		return c == ',' || c == ' ';
	}

	public NameFilterSearchToken getNextNameFilterToken() {
		NameFilterSearchToken res = null;
		if (!tokens.isEmpty()) {
			for (int i = tokens.size() - 1; i >= 0; i--) {
			    SearchToken token = tokens.get(i);
				if (token.getType() == TokenType.NAME_FILTER) {
					res = (NameFilterSearchToken) token;
				} else {
					break;
				}
			}
		}
		return res;
	}

	public SearchToken getLastToken() {
		if (!tokens.isEmpty()) {
			return tokens.get(tokens.size() - 1);
		}
		return null;
	}

	public ObjectSearchToken getLastObjectToken() {
		ObjectSearchToken res = null;
		if (!tokens.isEmpty()) {
			for (int i = tokens.size() - 1; i >= 0; i--) {
				SearchToken token = tokens.get(i);
				if (token.getType() == TokenType.SEARCH_OBJECT) {
					res = (ObjectSearchToken) token;
					break;
				}
			}
		}
		return res;
	}

	public boolean replaceToken(SearchToken oldToken, SearchToken newToken) {
		int index = tokens.indexOf(oldToken);
		if (index != -1) {
			tokens.set(index, newToken);
			return true;
		}
		return false;
	}

	public Map<SearchObjectType, SearchToken> getCompleteObjectTokens() {
		Map<SearchObjectType, SearchToken> map = new LinkedHashMap<>();
		for (SearchToken token : tokens) {
			if (token.getType() == TokenType.SEARCH_OBJECT && !((ObjectSearchToken)token).isSuggestion()) {
				map.put(token.getSearchObject().getType(), token);
			}
		}
		return map;
	}

	public List<SearchObject> getCompleteObjects() {
		List<SearchObject> list = new ArrayList<>();
		for (SearchToken token : tokens) {
			if (token.getType() == TokenType.SEARCH_OBJECT && !((ObjectSearchToken)token).isSuggestion()) {
				list.add(token.getSearchObject());
			}
		}
		return list;
	}

	public static void main(String[] args){
		//test
		SearchString searchString = new SearchString(MapUtils.LANGUAGE);
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
