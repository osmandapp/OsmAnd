package net.osmand.plus.sherpafy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.osmand.data.LocationPoint;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.sherpafy.TourInformation.StageFavorite;
import net.osmand.plus.sherpafy.TourInformation.StageInformation;
import net.osmand.plus.views.FavoritesLayer;
import android.graphics.PointF;
import android.support.v4.app.FragmentActivity;

public class StageFavoritesLayer extends FavoritesLayer {
	
	private OsmandApplication app;
	private StageInformation givenStage;
	private ArrayList<StageFavorite> cachedFavorites;

	public StageFavoritesLayer(OsmandApplication app, StageInformation givenStage) {
		this.app = app;
		this.givenStage = givenStage;
		if (givenStage != null) {
			cachedFavorites = new ArrayList<StageFavorite>();
			for (Object o : givenStage.getFavorites()) {
				if (o instanceof StageFavorite) {
					StageFavorite sf = (StageFavorite) o;
					cachedFavorites.add(sf);
				}
			}
		}
	}
	
	protected Class<? extends LocationPoint> getFavoriteClass() {
		return (Class<? extends LocationPoint>) StageFavorite.class;
	}
	
	protected String getObjName() {
		return view.getContext().getString(R.string.gpx_wpt);
	}
	
	protected List<? extends LocationPoint> getPoints() {
		if(cachedFavorites != null) {
			return cachedFavorites;
					
		}
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
			if (view.getContext() instanceof FragmentActivity) {
				customization
						.showFavoriteDialog((FragmentActivity) view.getContext(),
								givenStage != null ? givenStage : customization.getSelectedStage(),
								(StageFavorite) favs.get(0));
				return true;
			}
		}
		 return false;
	}
}
