package net.osmand.plus.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.*;
import net.osmand.plus.base.FavoriteImageDrawable;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.OsmAndMapSurfaceView;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.MapUtils;

import java.util.List;

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
		setupMapView();
		setupButtons();
		setupFavorites();

	}

	private void setupFavorites(){
		final FavouritesDbHelper helper = getMyApplication().getFavorites();
		final List<FavouritePoint> points = helper.getFavouritePoints();
		ArrayAdapter<FavouritePoint> adapter = new ArrayAdapter<FavouritePoint>(this, R.layout.favourites_list_item, 0, points) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View view = convertView;
				if (view == null) {
					LayoutInflater inflater = getLayoutInflater();
					view = inflater.inflate(R.layout.favourites_list_item, parent, false);
				}

				TextView label = (TextView) view.findViewById(R.id.favourite_label);
				ImageView icon = (ImageView) view.findViewById(R.id.favourite_icon);
				final FavouritePoint model = points.get(position);
				view.setTag(model);
				icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(DashboardActivity.this, model.getColor()));
				LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
				int dist = (int) (MapUtils.getDistance(model.getLatitude(), model.getLongitude(),
						lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
				String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication()) + "  ";
				label.setText(distance + model.getName(), TextView.BufferType.SPANNABLE);
				label.setTypeface(Typeface.DEFAULT, model.isVisible() ? Typeface.NORMAL : Typeface.ITALIC);
				((Spannable) label.getText()).setSpan(
						new ForegroundColorSpan(getResources().getColor(R.color.color_distance)), 0, distance.length() - 1,
						0);
				final CheckBox ch = (CheckBox) view.findViewById(R.id.check_item);
				view.findViewById(R.id.favourite_icon).setVisibility(View.VISIBLE);
				ch.setVisibility(View.GONE);
				return view;
			}
		};
		((ListView) findViewById(R.id.list_favorites)).setAdapter(adapter);
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

	private void setupMapView() {
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
