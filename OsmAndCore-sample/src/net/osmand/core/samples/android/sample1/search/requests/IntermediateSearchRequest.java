package net.osmand.core.samples.android.sample1.search.requests;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.osmand.core.samples.android.sample1.search.SearchAPI.SearchApiCallback;
import net.osmand.core.samples.android.sample1.search.SearchScope;
import net.osmand.core.samples.android.sample1.search.objects.SearchObject;
import net.osmand.core.samples.android.sample1.search.tokens.SearchToken;

import java.util.ArrayList;
import java.util.List;

public class IntermediateSearchRequest extends SearchRequest {

	private List<SearchObject> searchObjects;
	protected String keyword = "";

	public IntermediateSearchRequest(@NonNull SearchScope searchScope, List<SearchObject> searchObjects,
									 int maxSearchResults, @Nullable SearchApiCallback searchCallback) {
		super(searchScope, maxSearchResults, searchCallback);
		this.searchObjects = searchObjects;

		SearchToken token = searchScope.getSearchString().getLastToken();
		if (token != null && token.getType() == SearchToken.TokenType.NAME_FILTER) {
			keyword = token.getPlainText().toLowerCase();
		}
	}

	@Override
	protected List<SearchObject> doSearch() {
		List<SearchObject> res = new ArrayList<>();
		for (SearchObject item : searchObjects) {
			if (cancelled) {
				break;
			}
			if (keyword.isEmpty() || item.getName(searchScope.getLang()).toLowerCase().contains(keyword)
					|| item.getNativeName().toLowerCase().contains(keyword)) {
				res.add(item);
			}
		}
		return res;
	}
}