package net.osmand.plus.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
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
	public static final boolean TIPS_AND_TRICKS = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dashboard);
		getSupportActionBar().setTitle(R.string.app_version);
		ColorDrawable color = new ColorDrawable(Color.parseColor("#FF9305"));
		getSupportActionBar().setBackgroundDrawable(color);
		getSupportActionBar().setIcon(android.R.color.transparent);
		prepareMapView();
		setupButtons();

	}

	private void setupButtons(){
		final Activity activity = this;
		final OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();

		Button showMap = (Button) findViewById(R.id.show_map);
		showMap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent mapIndent = new Intent(activity, appCustomization.getMapActivity());
				activity.startActivityForResult(mapIndent, 0);
			}
		});

		Button showFavorites = (Button) findViewById(R.id.show_all);
		showFavorites.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent favorites = new Intent(activity, appCustomization.getFavoritesActivity());
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(favorites);
			}
		});
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

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
}
