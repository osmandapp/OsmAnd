package net.osmand.plus.search.listitems;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.widgets.callback.OnClickListenerContainer;

public class QuickSearchSearchOnWebListItem extends QuickSearchListItem implements OnClickListenerContainer {

	private final String title;
	private final View.OnClickListener onClickListener;

	public QuickSearchSearchOnWebListItem(OsmandApplication app, @NonNull String title,
	                                      View.OnClickListener onClickListener) {
		super(app, null);
		this.title = title;
		this.onClickListener = onClickListener;
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.SEARCH_ON_WEB;
	}

	@Override
	public String getName() {
		return title;
	}

	@Override
	public View.OnClickListener getOnClickListener() {
		return onClickListener;
	}
}