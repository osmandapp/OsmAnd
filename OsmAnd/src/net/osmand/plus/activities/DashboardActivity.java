package net.osmand.plus.activities;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import net.osmand.plus.R;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.sherpafy.StageFavoritesLayer;
import net.osmand.plus.views.GPXLayer;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.OsmAndMapSurfaceView;
import net.osmand.plus.views.OsmandMapTileView;

/**
 * Created by Denis on 05.11.2014.
 */
public class DashboardActivity extends SherlockFragmentActivity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.dashboard);
		prepareMapView();


	}

	private void prepareMapView() {
		OsmAndMapSurfaceView surf = (OsmAndMapSurfaceView) findViewById(R.id.MapView);
		OsmandMapTileView osmandMapTileView = surf.getMapView();
		osmandMapTileView.getView().setVisibility(View.VISIBLE);
		osmandMapTileView.removeAllLayers();
		MapVectorLayer mapVectorLayer = new MapVectorLayer(null);
		MapTextLayer mapTextLayer = new MapTextLayer();
		mapTextLayer.setAlwaysVisible(true);
		// 5.95 all labels
		osmandMapTileView.addLayer(mapTextLayer, 5.95f);
		osmandMapTileView.addLayer(mapVectorLayer, 0.5f);
		osmandMapTileView.setMainLayer(mapVectorLayer);
		mapVectorLayer.setVisible(true);
	}
}
