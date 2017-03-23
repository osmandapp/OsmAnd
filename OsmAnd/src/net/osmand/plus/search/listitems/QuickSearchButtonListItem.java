package net.osmand.plus.search.listitems;

import android.graphics.drawable.Drawable;
import android.view.View;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

public class QuickSearchButtonListItem extends QuickSearchListItem {

	private int iconId;
	private String title;
	private View.OnClickListener onClickListener;
	private int colorId;

	public QuickSearchButtonListItem(OsmandApplication app, int iconId, String title, View.OnClickListener onClickListener) {
		super(app, null);
		this.iconId = iconId;
		this.title = title;
		this.onClickListener = onClickListener;
		this.colorId = app.getSettings().isLightContent() ? R.color.color_dialog_buttons_light : R.color.color_dialog_buttons_dark;
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.BUTTON;
	}

	@Override
	public Drawable getIcon() {
		if (iconId != 0) {
			return app.getIconsCache().getIcon(iconId, colorId);
		} else {
			return null;
		}
	}

	@Override
	public String getName() {
		return title;
	}

	public View.OnClickListener getOnClickListener() {
		return onClickListener;
	}
}
