package net.osmand.plus.sherpafy;

import java.util.Collections;
import java.util.List;

import net.osmand.data.FavouritePoint;
import net.osmand.data.LocationPoint;
import net.osmand.plus.R;
import net.osmand.plus.sherpafy.TourInformation.StageFavorite;
import net.osmand.plus.views.FavoritesLayer;

public class StageFavoritesLayer extends FavoritesLayer {
	
	private SherpafyCustomization customization;

	public StageFavoritesLayer(SherpafyCustomization customization){
		this.customization = customization;
	}

	protected Class<? extends LocationPoint> getFavoriteClass() {
		return (Class<? extends LocationPoint>) StageFavorite.class;
	}
	
	protected String getObjName() {
		return view.getContext().getString(R.string.gpx_wpt);
	}
	
	protected List<? extends LocationPoint> getPoints() {
		List<StageFavorite> fs = customization.getWaypoints();
		if(fs == null) {
			return Collections.emptyList();
		}
		return fs;
	}
}
