package net.osmand.plus.mapcontextmenu.details;

import android.view.View;

import net.osmand.data.FavouritePoint;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.util.Algorithms;

public class FavouritePointMenuBuilder extends MenuBuilder {

	private final FavouritePoint fav;

	public FavouritePointMenuBuilder(OsmandApplication app, final FavouritePoint fav) {
		super(app);
		this.fav = fav;
	}

	private void buildRow(View view, int iconId, String text, int textColor) {
		buildRow(view, getRowIcon(iconId), text, textColor);
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	public void build(View view) {
		super.build(view);

		if (!Algorithms.isEmpty(fav.getDescription())) {
			buildRow(view, R.drawable.ic_action_note_dark, fav.getDescription(), 0);
		}

		buildPlainMenuItems(view);
	}
}
