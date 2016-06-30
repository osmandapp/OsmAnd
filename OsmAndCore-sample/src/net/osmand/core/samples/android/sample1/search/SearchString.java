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

	private String plainText = "";
	private List<SearchToken> tokens = new ArrayList<>();
	private String lang;

	public SearchString(String lang) {
		this.lang = lang;
	}

	public SearchString copy() {
		SearchString res = new SearchString(lang);
		res.plainText = plainText;
		res.tokens = new ArrayList<>(tokens);
		return res;
	}

	public String getPlainText() {
		return plainText;
	}

	public void setPlainText(String plainText) {
		int newTextLength = plainText.length();
		int currTextLength = this.plainText.length();
		boolean isNewText = currTextLength == 0
				|| newTextLength == 0
				|| !this.plainText.regionMatches(0, plainText, 0,
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
						|| (lastTokenIndex < newTextLength - 1 && !startWithDelimiter(plainText.substring(lastTokenIndex + 1)))) {
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
				char c = plainText.charAt(i);
				if (isDelimiterChar(c)) {
					if (i == firstWordIndex) {
						firstWordIndex++;
					} else {
						SearchToken token = new NameFilterSearchToken(firstWordIndex, plainText.substring(firstWordIndex, i));
						tokens.add(token);
						firstWordIndex = i + 1;
					}
				}
			}
			if (firstWordIndex <= newTextLength - 1) {
				SearchToken token = new NameFilterSearchToken(firstWordIndex, plainText.substring(firstWordIndex));
				tokens.add(token);
			} else if (endWithDelimeter(plainText)) {
				SearchToken lastToken = getLastToken();
				if (lastToken.getType() == TokenType.OBJECT) {
					((ObjectSearchToken) lastToken).applySuggestion();
				}
				SearchToken token = new NameFilterSearchToken(firstWordIndex, "");
				tokens.add(token);
			}
		}

		this.plainText = plainText;
	}

	public void completeQuery(@NonNull SearchObject searchObject) {
		String text;
		String objectName = searchObject.getName(lang);
		int startIndex;
		SearchToken lastToken = getLastToken();
		if (lastToken == null || lastToken.hasEmptyQuery()) {
			startIndex = plainText.length();
			text = plainText + objectName + " ";
		} else {
			startIndex = lastToken.getStartIndex();
			text = plainText.substring(0, startIndex) + objectName + " ";
		}
		ObjectSearchToken token = new ObjectSearchToken(startIndex, objectName, searchObject, false);
		if (lastToken == null) {
			tokens.add(token);
		} else {
			tokens.set(tokens.size() - 1, token);
		}
		tokens.add(new NameFilterSearchToken(text.length(), ""));
		plainText = text;
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
				if (token.getType() == TokenType.OBJECT) {
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

	public Map<SearchObjectType, ObjectSearchToken> getCompleteObjectTokens() {
		Map<SearchObjectType, ObjectSearchToken> map = new LinkedHashMap<>();
		for (SearchToken token : tokens) {
			if (token.getType() == TokenType.OBJECT && !((ObjectSearchToken)token).isSuggestion()) {
				map.put(((ObjectSearchToken)token).getSearchObject().getType(), (ObjectSearchToken)token);
			}
		}
		return map;
	}

	public List<SearchObject> getCompleteObjects() {
		List<SearchObject> list = new ArrayList<>();
		for (SearchToken token : tokens) {
			if (token.getType() == TokenType.OBJECT && !((ObjectSearchToken)token).isSuggestion()) {
				list.add(((ObjectSearchToken)token).getSearchObject());
			}
		}
		return list;
	}

	public static void main(String[] args){
		//test
		SearchString searchString = new SearchString(MapUtils.LANGUAGE);
		searchString.setPlainText("cit");
		searchString.setPlainText("city");
		searchString.setPlainText("city ");
		searchString.setPlainText("city s");
		searchString.setPlainText("city st");
		searchString.setPlainText("city street ");
		searchString.setPlainText("city street 8");
		searchString.setPlainText("new");
	}
}
