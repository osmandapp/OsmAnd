package net.osmand.plus.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.plus.*;
import net.osmand.plus.activities.search.SearchActivity;
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
		getSupportActionBar().setTitle(R.string.app_name_ver);
		ColorDrawable color = new ColorDrawable(Color.parseColor("#ff8f00"));
		getSupportActionBar().setBackgroundDrawable(color);
		getSupportActionBar().setIcon(android.R.color.transparent);
		setupMapView();
		setupButtons();
		setupFonts();
	}

	@Override
	protected void onResume() {
		super.onResume();
		setupFavorites();
	}

	private void setupFonts() {
		Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf");
		((TextView) findViewById(R.id.search_for)).setTypeface(typeface);
		((TextView) findViewById(R.id.map_text)).setTypeface(typeface);
		((TextView) findViewById(R.id.fav_text)).setTypeface(typeface);
		((Button) findViewById(R.id.show_map)).setTypeface(typeface);
		((Button) findViewById(R.id.show_all)).setTypeface(typeface);
	}

	private void setupFavorites(){
		final FavouritesDbHelper helper = getMyApplication().getFavorites();
		final List<FavouritePoint> points = helper.getFavouritePoints();
		LinearLayout favorites = (LinearLayout) findViewById(R.id.favorites);
		favorites.removeAllViews();
		if (points.size() == 0){
			(findViewById(R.id.main_fav)).setVisibility(View.GONE);
			return;
		}

		if (points.size() > 3){
			while (points.size() != 3){
				points.remove(3);
			}
		}


		for (final FavouritePoint point : points) {
			LayoutInflater inflater = getLayoutInflater();
			View view = inflater.inflate(R.layout.dash_fav_list, null, false);
			TextView name = (TextView) view.findViewById(R.id.name);
			TextView label = (TextView) view.findViewById(R.id.distance);
			ImageView icon = (ImageView) view.findViewById(R.id.icon);
			name.setText(point.getName());
			icon.setImageDrawable(FavoriteImageDrawable.getOrCreate(DashboardActivity.this, point.getColor()));
			LatLon lastKnownMapLocation = getMyApplication().getSettings().getLastKnownMapLocation();
			int dist = (int) (MapUtils.getDistance(point.getLatitude(), point.getLongitude(),
					lastKnownMapLocation.getLatitude(), lastKnownMapLocation.getLongitude()));
			String distance = OsmAndFormatter.getFormattedDistance(dist, getMyApplication()) + "  ";
			label.setText(distance, TextView.BufferType.SPANNABLE);
			label.setTypeface(Typeface.DEFAULT, point.isVisible() ? Typeface.NORMAL : Typeface.ITALIC);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					final Intent mapIndent = new Intent(DashboardActivity.this, getMyApplication().getAppCustomization().getMapActivity());
					mapIndent.putExtra(MapActivity.START_LAT, point.getLatitude());
					mapIndent.putExtra(MapActivity.START_LON, point.getLongitude());
					startActivityForResult(mapIndent, 0);
				}
			});
			int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, getResources().getDisplayMetrics());
			int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height);
			lp.setMargins(0, margin, 0, 0);
			view.setLayoutParams(lp);
			favorites.addView(view);
		}
	}

	private void setupButtons(){
		final Activity activity = this;
		final OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();

		(findViewById(R.id.show_map)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent mapIndent = new Intent(activity, appCustomization.getMapActivity());
				activity.startActivityForResult(mapIndent, 0);
			}
		});

		(findViewById(R.id.show_all)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent favorites = new Intent(activity, appCustomization.getFavoritesActivity());
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				activity.startActivity(favorites);
			}
		});

		(findViewById(R.id.poi)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.POI_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(findViewById(R.id.address)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.ADDRESS_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(findViewById(R.id.coord)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.LOCATION_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(findViewById(R.id.fav_btn)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.FAVORITES_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(findViewById(R.id.history)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.HISTORY_TAB_INDEX);
				activity.startActivity(search);
			}
		});

		(findViewById(R.id.transport)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Intent search = new Intent(activity, appCustomization.getSearchActivity());
				search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().SEARCH_TAB.set(SearchActivity.TRANSPORT_TAB_INDEX);
				activity.startActivity(search);
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
		net.osmand.Location location = getMyApplication().getLocationProvider().getFirstTimeRunDefaultLocation();
		if(location != null){
			osmandMapTileView.setLatLon(location.getLatitude(), location.getLongitude());
			osmandMapTileView.setIntZoom(14);
		}
		osmandMapTileView.refreshMap(true);
	}



	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.close).setIcon(R.drawable.ic_ac_help)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, 1, 0, R.string.settings).setIcon(R.drawable.ic_ac_settings)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, 2, 0, R.string.exit_Button).setIcon(R.drawable.ic_ac_close)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
		if (item.getItemId() == 0) {
			if(TIPS_AND_TRICKS) {
				TipsAndTricksActivity activity = new TipsAndTricksActivity(this);
				Dialog dlg = activity.getDialogToShowTips(false, true);
				dlg.show();
			} else {
				final Intent helpIntent = new Intent(this, HelpActivity.class);
				startActivity(helpIntent);
			}
		} else if (item.getItemId() == 1){
			final Intent settings = new Intent(this, appCustomization.getSettingsActivity());
			startActivity(settings);
		} else if (item.getItemId() == 2){
			Intent newIntent = new Intent(this, appCustomization.getMainMenuActivity());
			newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			newIntent.putExtra(MainMenuActivity.APP_EXIT_KEY, MainMenuActivity.APP_EXIT_CODE);
			startActivity(newIntent);
		}
		return true;
	}

	private OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}
}
