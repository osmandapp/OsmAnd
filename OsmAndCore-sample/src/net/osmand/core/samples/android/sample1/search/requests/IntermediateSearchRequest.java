package net.osmand.core.samples.android.sample1.search.requests;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.core.samples.android.sample1.search.SearchAPI;
import net.osmand.core.samples.android.sample1.search.SearchAPI.SearchCallback;
import net.osmand.core.samples.android.sample1.search.items.SearchItem;
import net.osmand.core.samples.android.sample1.search.requests.SearchRequest;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken;

import java.util.ArrayList;
import java.util.List;

public class IntermediateSearchRequest extends SearchRequest {

	private List<SearchItem> searchItems;
	protected String keyword = "";

	public IntermediateSearchRequest(@NonNull SearchAPI searchAPI, int maxSearchResults, @Nullable SearchCallback searchCallback) {
		super(searchAPI, maxSearchResults, searchCallback);
		this.searchItems = new ArrayList<>(searchAPI.getSearchItems());

		SearchToken token = searchString.getLastToken();
		if (token != null && token.getType() == SearchToken.TokenType.NAME_FILTER) {
			keyword = token.getQueryText();
		}
	}

	@Override
	protected List<SearchItem> doSearch() {
		List<SearchItem> res = new ArrayList<>();
		for (SearchItem item : searchItems) {
			if (cancelled) {
				break;
			}
			if (keyword.isEmpty() || item.getName().contains(keyword)) {
				res.add(item);
			}
		}
		return res;
	}
}