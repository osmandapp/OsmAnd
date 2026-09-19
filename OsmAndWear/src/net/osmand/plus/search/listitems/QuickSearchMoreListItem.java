package net.osmand.plus.search.listitems;

import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class QuickSearchMoreListItem extends QuickSearchListItem {

	private final String name;
	private final SearchMoreItemOnClickListener onClickListener;
	private boolean emptySearch;
	private boolean searchMoreAvailable;
	private boolean interruptedSearch;
	private final String findMore;
	private final String restartSearch;
	private final String increaseRadius;
	private boolean secondaryButtonVisibility;

	public QuickSearchMoreListItem(OsmandApplication app, String name,
	                               @Nullable SearchMoreItemOnClickListener onClickListener) {
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

	public boolean isSearchMoreAvailable() {
		return searchMoreAvailable;
	}

	public void setSearchMoreAvailable(boolean searchMoreAvailable) {
		this.searchMoreAvailable = searchMoreAvailable;
	}

	public void onPrimaryButtonClick() {
		if (onClickListener != null) {
			onClickListener.onPrimaryButtonClick();
		}
	}

	public void onSecondaryButtonClick() {
		if (onClickListener != null) {
			onClickListener.onSecondaryButtonClick();
		}
	}

	public void setSecondaryButtonVisible(boolean secondaryButtonVisibility) {
		this.secondaryButtonVisibility = secondaryButtonVisibility;
	}

	public boolean isSecondaryButtonVisible() {
		return secondaryButtonVisibility;
	}

	public interface SearchMoreItemOnClickListener {

		void onPrimaryButtonClick();

		void onSecondaryButtonClick();

	}
}
