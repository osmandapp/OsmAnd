package net.osmand.plus.sherpafy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.PointF;
import net.osmand.data.LocationPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.sherpafy.TourInformation.StageFavorite;
import net.osmand.plus.views.FavoritesLayer;

public class StageFavoritesLayer extends FavoritesLayer {
	
	private OsmandApplication app;

	public StageFavoritesLayer(OsmandApplication app){
		this.app = app;
	}

	protected Class<? extends LocationPoint> getFavoriteClass() {
		return (Class<? extends LocationPoint>) StageFavorite.class;
	}
	
	protected String getObjName() {
		return view.getContext().getString(R.string.gpx_wpt);
	}
	
	protected List<? extends LocationPoint> getPoints() {
		List<StageFavorite> fs = ((SherpafyCustomization)app.getAppCustomization()).getWaypoints();
		if(fs == null) {
			return Collections.emptyList();
		}
		return fs;
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		List<LocationPoint> favs = new ArrayList<LocationPoint>();
		getFavoriteFromPoint(tileBox, point, favs);
		if (favs.size() > 0){
			SherpafyCustomization customization = (SherpafyCustomization) app.getAppCustomization();
			customization.showFavoriteDialog(app.getMapActivity(), customization.getSelectedStage(), (StageFavorite)favs.get(0) );
			return true;
		}
		 return false;
	}
}
