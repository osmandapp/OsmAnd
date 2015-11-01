package net.osmand.plus.mapcontextmenu.builders;

import android.view.View;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.util.Algorithms;

public class WptPtMenuBuilder extends MenuBuilder {

	private final WptPt wpt;

	public WptPtMenuBuilder(OsmandApplication app, final WptPt wpt) {
		super(app);
		this.wpt = wpt;
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	public void build(View view) {
		super.build(view);

		if (!Algorithms.isEmpty(wpt.desc)) {
			buildRow(view, R.drawable.ic_action_note_dark, wpt.desc, 0);
		}

		buildPlainMenuItems(view);
	}
}

