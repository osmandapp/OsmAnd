package net.osmand.plus.search.listitems;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.widgets.callback.OnClickListenerContainer;

public class QuickSearchSimpleButtonListItem extends QuickSearchListItem implements OnClickListenerContainer {

	private final String title;
	private final View.OnClickListener onClickListener;

	public QuickSearchSimpleButtonListItem(OsmandApplication app, @NonNull String title, View.OnClickListener onClickListener) {
		super(app, null);
		this.title = title;
		this.onClickListener = onClickListener;
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.BUTTON;
	}

	@Override
	public Drawable getIcon() {
		return null;
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
