package net.osmand.plus.mapcontextmenu.builders;

import android.view.View;

import net.osmand.data.Amenity;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.MapMarkersHelper;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.POIMapLayer;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.Date;

public class MapMarkerMenuBuilder extends SyncedItemMenuBuilder {

	public MapMarkerMenuBuilder(MapActivity mapActivity, final MapMarkersHelper.MapMarker marker) {
		super(mapActivity);
		this.favouritePoint = marker.favouritePoint;
		this.wptPt = marker.wptPt;
		if (favouritePoint != null) {
			acquireOriginObject();
		}
	}

	@Override
	protected boolean needBuildPlainMenuItems() {
		return false;
	}

	@Override
	protected void buildNearestWikiRow(View view) {
		if (originObject == null || !(originObject instanceof Amenity)) {
			super.buildNearestWikiRow(view);
		}
	}

	@Override
	protected void buildInternal(View view) {
		if (favouritePoint != null) {
			buildFavouriteInternal(view);
		}
		if (wptPt != null) {
			buildWptPtInternal(view);
		}

		buildPlainMenuItems(view);
	}
}
