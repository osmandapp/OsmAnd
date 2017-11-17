package net.osmand.plus.mapcontextmenu.builders;

import android.view.View;

import net.osmand.data.Amenity;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.activities.MapActivity;

public class FavouritePointMenuBuilder extends SyncedItemMenuBuilder {

	public FavouritePointMenuBuilder(MapActivity mapActivity, final FavouritePoint fav) {
		super(mapActivity);
		this.favouritePoint = fav;
		acquireOriginObject();
	}

	@Override
	protected void buildNearestWikiRow(View view) {
		if (originObject == null || !(originObject instanceof Amenity)) {
			super.buildNearestWikiRow(view);
		}
	}

	@Override
	public void buildInternal(View view) {
		buildFavouriteInternal(view);
	}
}
