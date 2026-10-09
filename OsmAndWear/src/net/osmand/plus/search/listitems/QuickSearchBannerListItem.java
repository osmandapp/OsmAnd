package net.osmand.plus.search.listitems;

import android.view.View;

import net.osmand.plus.OsmandApplication;

import java.util.ArrayList;
import java.util.List;

public class QuickSearchBannerListItem extends QuickSearchListItem {

	public static final int INVALID_ID = -1;

	private final List<ButtonItem> buttons;

	public QuickSearchBannerListItem(OsmandApplication app) {
		super(app, null);
		buttons = new ArrayList<>();
	}

	public QuickSearchListItemType getType() {
		return QuickSearchListItemType.BANNER;
	}

	public void addButton(String title, String description, int iconId,
	                      View.OnClickListener listener) {
		buttons.add(new ButtonItem(title, description, iconId, listener));
	}

	public List<ButtonItem> getButtonItems() {
		return buttons;
	}

	public static class ButtonItem {
		private final String title;
		private final String description;
		private final int iconId;
		private final View.OnClickListener listener;

		public ButtonItem(String title, String description, int iconId,
		                  View.OnClickListener listener) {
			this.title = title;
			this.description = description;
			this.iconId = iconId;
			this.listener = listener;
		}

		public String getTitle() {
			return title;
		}

		public String getDescription() {
			return description;
		}

		public int getIconId() {
			return iconId;
		}

		public View.OnClickListener getListener() {
			return listener;
		}
	}
}
