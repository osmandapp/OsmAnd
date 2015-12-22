package net.osmand.plus.mapcontextmenu.builders;

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

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	public void buildInternal(View view) {
		if (!Algorithms.isEmpty(fav.getDescription())) {
			buildRow(view, R.drawable.ic_action_note_dark, fav.getDescription(), 0, true, 0, false);
		}

		buildPlainMenuItems(view);
	}
}
