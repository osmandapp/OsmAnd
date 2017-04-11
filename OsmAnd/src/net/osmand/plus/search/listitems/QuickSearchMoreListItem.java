package net.osmand.plus.search.listitems;

import android.view.View.OnClickListener;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class QuickSearchMoreListItem extends QuickSearchListItem {

	private String name;
	private OnClickListener onClickListener;
	private boolean emptySearch;
	private boolean interruptedSearch;
	private String findMore;
	private String restartSearch;
	private String increaseRadius;

	public QuickSearchMoreListItem(OsmandApplication app, String name, OnClickListener onClickListener) {
		super(app, null);
		this.name = name;
		this.onClickListener = onClickListener;
		findMore = app.getString(R.string.search_POI_level_btn).toUpperCase();
		restartSearch = app.getString(R.string.restart_search).toUpperCase();
		increaseRadius = app.getString(R.string.increase_search_radius).toUpperCase();
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.SEARCH_MORE;
	}

	@Override
	public String getName() {
		if (name != null) {
			return name;
		} else if (interruptedSearch) {
			if (emptySearch) {
				return restartSearch;
			} else {
				return findMore;
			}
		} else {
			return increaseRadius;
		}
	}

	public boolean isInterruptedSearch() {
		return interruptedSearch;
	}

	public void setInterruptedSearch(boolean interruptedSearch) {
		this.interruptedSearch = interruptedSearch;
	}

	public boolean isEmptySearch() {
		return emptySearch;
	}

	public void setEmptySearch(boolean emptySearch) {
		this.emptySearch = emptySearch;
	}

	public OnClickListener getOnClickListener() {
		return onClickListener;
	}
}
