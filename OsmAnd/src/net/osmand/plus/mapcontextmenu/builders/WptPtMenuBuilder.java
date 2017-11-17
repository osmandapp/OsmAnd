package net.osmand.plus.mapcontextmenu.builders;

import android.view.View;

import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.Date;

public class WptPtMenuBuilder extends SyncedItemMenuBuilder {

	public WptPtMenuBuilder(MapActivity mapActivity, final WptPt wpt) {
		super(mapActivity);
		this.wptPt = wpt;
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	public void buildInternal(View view) {
		buildWptPtInternal(view);

		buildPlainMenuItems(view);
	}
}

