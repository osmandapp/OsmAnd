package net.osmand.plus.search.listitems;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.widgets.callback.OnClickListenerContainer;

public class QuickSearchDisabledHistoryItem extends QuickSearchListItem implements OnClickListenerContainer {

	private final View.OnClickListener listener;

	public QuickSearchDisabledHistoryItem(@NonNull OsmandApplication app, @Nullable View.OnClickListener listener) {
		super(app, null);
		this.listener = listener;
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.DISABLED_HISTORY;
	}

	@Override
	public View.OnClickListener getOnClickListener() {
		return listener;
	}
}