package net.osmand.core.samples.android.sample1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.core.jni.IMapLayerProvider;
import net.osmand.core.jni.IMapStylesCollection;
import net.osmand.core.jni.Logger;
import net.osmand.core.jni.MapObjectsSymbolsProvider;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.MapPrimitivesProvider;
import net.osmand.core.jni.MapPrimitiviser;
import net.osmand.core.jni.MapRasterLayerProvider_Software;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfMapObjectsProvider;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QIODeviceLogSink;
import net.osmand.core.jni.ResolvedMapStyle;
import net.osmand.core.jni.Utilities;
import net.osmand.core.samples.android.sample1.MultiTouchSupport.MultiTouchZoomListener;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MapContextMenu;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MapMultiSelectionMenu;
import net.osmand.core.samples.android.sample1.search.QuickSearchDialogFragment;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.FilenameFilter;

public class MainActivity extends AppCompatActivity {
	private static final String TAG = "OsmAndCoreSample";

	private float displayDensityFactor;
	private int referenceTileSize;
	private int rasterTileSize;
	private IMapStylesCollection mapStylesCollection;
	private ResolvedMapStyle mapStyle;
	private ObfsCollection obfsCollection;
	private MapPresentationEnvironment mapPresentationEnvironment;
	private MapPrimitiviser mapPrimitiviser;
	private ObfMapObjectsProvider obfMapObjectsProvider;
	private MapPrimitivesProvider mapPrimitivesProvider;
	private MapObjectsSymbolsProvider mapObjectsSymbolsProvider;
	private IMapLayerProvider mapLayerProvider0;
	private QIODeviceLogSink fileLogSink;

	private AtlasMapRendererView mapView;
	private TextView textZoom;
	private ImageButton searchButton;
	private ImageButton azimuthNorthButton;

	private GestureDetector gestureDetector;
	private PointI target31;
	private float zoom;
	private float azimuth;
	private float elevationAngle;
	private MultiTouchSupport multiTouchSupport;

	private MapContextMenu menu;
	private MapMultiSelectionMenu multiMenu;

	private boolean noMapsFound;

	// Germany
	private final static float INIT_LAT = 49.353953f;
	private final static float INIT_LON = 11.214384f;
	// Kyiv
	//private final static float INIT_LAT = 50.450117f;
	//private final static float INIT_LON = 30.524142f;
	private final static float INIT_ZOOM = 6.0f;
	private final static float INIT_AZIMUTH = 0.0f;
	private final static float INIT_ELEVATION_ANGLE = 90.0f;
	private final static int MIN_ZOOM_LEVEL = 2;
	private final static int MAX_ZOOM_LEVEL = 22;

	private static final String PREF_MAP_CENTER_LAT = "MAP_CENTER_LAT";
	private static final String PREF_MAP_CENTER_LON = "MAP_CENTER_LON";
	private static final String PREF_MAP_AZIMUTH = "MAP_AZIMUTH";
	private static final String PREF_MAP_ZOOM = "MAP_ZOOM";
	private static final String PREF_MAP_ELEVATION_ANGLE = "MAP_ELEVATION_ANGLE";

	public SampleApplication getMyApplication() {
		return (SampleApplication) getApplication();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == SampleApplication.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE &&
				grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			if (!InstallOsmandAppDialog.wasShown()) {
				checkMapsInstalled();
			}
			getSampleApplication().initPoiTypes();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SampleApplication app = getSampleApplication();

		gestureDetector = new GestureDetector(this, new MapViewOnGestureListener());
		multiTouchSupport = new MultiTouchSupport(this, new MapViewMultiTouchZoomListener());

		// Inflate views
		setContentView(R.layout.activity_main);

		boolean externalStoragePermissionGranted = ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
		if (!externalStoragePermissionGranted) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					SampleApplication.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
		}

		// Get map view
		mapView = (AtlasMapRendererView) findViewById(R.id.mapRendererView);

		textZoom = (TextView) findViewById(R.id.text_zoom);

		searchButton = (ImageButton) findViewById(R.id.search_button);
		searchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false);
			}
		});

		azimuthNorthButton = (ImageButton) findViewById(R.id.map_azimuth_north_button);
		azimuthNorthButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setAzimuth(0f);
			}
		});

		findViewById(R.id.map_zoom_in_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setZoom(zoom + 1f);
			}
		});

		findViewById(R.id.map_zoom_out_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setZoom(zoom - 1f);
			}
		});

		// Additional log sink
		fileLogSink = QIODeviceLogSink.createFileLogSink(app.getAbsoluteAppPath() + "/osmandcore.log");
		Logger.get().addLogSink(fileLogSink);

		// Get device display density factor
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		displayDensityFactor = displayMetrics.densityDpi / 160.0f;
		referenceTileSize = (int) (256 * displayDensityFactor);
		rasterTileSize = Integer.highestOneBit(referenceTileSize - 1) * 2;
		Log.i(TAG, "displayDensityFactor = " + displayDensityFactor);
		Log.i(TAG, "referenceTileSize = " + referenceTileSize);
		Log.i(TAG, "rasterTileSize = " + rasterTileSize);

		Log.i(TAG, "Going to resolve default embedded style...");
		mapStylesCollection = new MapStylesCollection();
		mapStyle = mapStylesCollection.getResolvedStyleByName("default");
		if (mapStyle == null) {
			Log.e(TAG, "Failed to resolve style 'default'");
			System.exit(0);
		}

		Log.i(TAG, "Going to prepare OBFs collection");
		obfsCollection = new ObfsCollection();
		Log.i(TAG, "Will load OBFs from " + app.getAbsoluteAppPath());
		obfsCollection.addDirectory(app.getAbsoluteAppPath(), false);

		Log.i(TAG, "Going to prepare all resources for renderer");
		mapPresentationEnvironment = new MapPresentationEnvironment(
				mapStyle,
				displayDensityFactor,
				1.0f,
				1.0f,
				SampleApplication.LANGUAGE);
		//mapPresentationEnvironment->setSettings(configuration.styleSettings);
		mapPrimitiviser = new MapPrimitiviser(
				mapPresentationEnvironment);
		obfMapObjectsProvider = new ObfMapObjectsProvider(
				obfsCollection);
		mapPrimitivesProvider = new MapPrimitivesProvider(
				obfMapObjectsProvider,
				mapPrimitiviser,
				rasterTileSize);
		mapObjectsSymbolsProvider = new MapObjectsSymbolsProvider(
				mapPrimitivesProvider,
				rasterTileSize);

		mapView.setReferenceTileSizeOnScreenInPixels(referenceTileSize);
		mapView.addSymbolsProvider(mapObjectsSymbolsProvider);

		restoreMapState();

		mapLayerProvider0 = new MapRasterLayerProvider_Software(mapPrimitivesProvider);
		mapView.setMapLayerProvider(0, mapLayerProvider0);

		app.getIconsCache().setDisplayDensityFactor(displayDensityFactor);

		menu = new MapContextMenu();
		menu.setMainActivity(this);

		multiMenu = new MapMultiSelectionMenu(this);

		if (!InstallOsmandAppDialog.show(getSupportFragmentManager(), this)
				&& externalStoragePermissionGranted) {
			checkMapsInstalled();
		}
	}

	private void checkMapsInstalled() {
		File mapsDir = new File(getSampleApplication().getAbsoluteAppPath());
		if (mapsDir.exists()) {
			File[] maps = mapsDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					return filename.toLowerCase().endsWith(".obf");
				}
			});
			noMapsFound = maps == null || maps.length == 0;
		}

		if (noMapsFound) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.install_maps_title);
			builder.setMessage(R.string.install_maps_desc);
			builder.setNegativeButton(R.string.shared_string_cancel, null);
			builder.setPositiveButton(R.string.restart_app, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SampleUtils.doRestart(MainActivity.this);
				}
			});
			builder.create().show();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		mapView.handleOnResume();
	}

	@Override
	protected void onPause() {
		saveMapState();
		mapView.handleOnPause();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mapView.handleOnDestroy();
		super.onDestroy();
	}

	public SampleApplication getSampleApplication() {
		return (SampleApplication) getApplication();
	}

	public AtlasMapRendererView getMapView() {
		return mapView;
	}

	public void showOnMap(LatLon latLon, int zoom) {
		if (latLon != null) {
			PointI target = Utilities.convertLatLonTo31(
					new net.osmand.core.jni.LatLon(latLon.getLatitude(), latLon.getLongitude()));
			setTarget(target);
			setZoom(zoom);
		}
	}

	public void refreshMap() {
		//todo
	}

	public MapContextMenu getContextMenu() {
		return menu;
	}

	public boolean showContextMenu(@NonNull LatLon latLon,
								   @Nullable PointDescription pointDescription,
								   @Nullable Object object) {
		if (multiMenu.isVisible()) {
			multiMenu.hide();
		}
		if (!getBox().containsLatLon(latLon)) {
			menu.setMapCenter(latLon);
			menu.setCenterMarker(true);
		}
		menu.show(latLon, pointDescription, object);
		return true;
	}

	public RotatedTileBox getBox() {
		RotatedTileBox.RotatedTileBoxBuilder boxBuilder = new RotatedTileBox.RotatedTileBoxBuilder();
		LatLon screenCenter = getScreenCenter();
		boxBuilder.setLocation(screenCenter.getLatitude(), screenCenter.getLongitude());
		boxBuilder.setPixelDimensions(mapView.getWidth(), mapView.getHeight(), 0.5f, 0.5f);
		boxBuilder.setZoom((int)getZoom());
		boxBuilder.setRotate(mapView.getRotation());
		return boxBuilder.build();
	}

	public PointI getScreenCenter31() {
		PointI point = new PointI();
		mapView.getLocationFromScreenPoint(new PointI(mapView.getWidth() / 2, mapView.getHeight() / 2), point);
		return point;
	}

	public LatLon getScreenCenter() {
		PointI point = new PointI();
		mapView.getLocationFromScreenPoint(new PointI(mapView.getWidth() / 2, mapView.getHeight() / 2), point);
		net.osmand.core.jni.LatLon jniLatLon = Utilities.convert31ToLatLon(point);
		return new LatLon(jniLatLon.getLatitude(), jniLatLon.getLongitude());
	}

	public float getZoom() {
		return zoom;
	}

	public void saveMapState() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		Editor edit = prefs.edit();
		net.osmand.core.jni.LatLon latLon = Utilities.convert31ToLatLon(target31);
		edit.putFloat(PREF_MAP_CENTER_LAT, (float) latLon.getLatitude());
		edit.putFloat(PREF_MAP_CENTER_LON, (float) latLon.getLongitude());
		edit.putFloat(PREF_MAP_AZIMUTH, azimuth);
		edit.putFloat(PREF_MAP_ZOOM, zoom);
		edit.putFloat(PREF_MAP_ELEVATION_ANGLE, elevationAngle);
		edit.commit();
	}

	public void restoreMapState() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		float prefLat = prefs.getFloat(PREF_MAP_CENTER_LAT, INIT_LAT);
		float prefLon = prefs.getFloat(PREF_MAP_CENTER_LON, INIT_LON);
		float prefAzimuth = prefs.getFloat(PREF_MAP_AZIMUTH, INIT_AZIMUTH);
		float prefZoom = prefs.getFloat(PREF_MAP_ZOOM, INIT_ZOOM);
		float prefElevationAngle = prefs.getFloat(PREF_MAP_ELEVATION_ANGLE, INIT_ELEVATION_ANGLE);

		setAzimuth(prefAzimuth);
		setElevationAngle(prefElevationAngle);
		setTarget(Utilities.convertLatLonTo31(new net.osmand.core.jni.LatLon(prefLat, prefLon)));
		setZoom(prefZoom);
	}

	public boolean setTarget(PointI pointI) {
		target31 = pointI;
		return mapView.setTarget(pointI);
	}

	@SuppressLint("DefaultLocale")
	public boolean setZoom(float zoom) {
		if (zoom < MIN_ZOOM_LEVEL) {
			zoom = MIN_ZOOM_LEVEL;
		} else if (zoom > MAX_ZOOM_LEVEL) {
			zoom = MAX_ZOOM_LEVEL;
		}
		this.zoom = zoom;
		textZoom.setText(String.format("%.0f", zoom));
		return mapView.setZoom(zoom);
	}

	public void setAzimuth(float angle) {
		angle = MapUtils.unifyRotationTo360(angle);
		this.azimuth = angle;
		mapView.setAzimuth(angle);

		if (angle == 0f && azimuthNorthButton.getVisibility() == View.VISIBLE) {
			azimuthNorthButton.setVisibility(View.INVISIBLE);
		} else if (angle != 0f && azimuthNorthButton.getVisibility() == View.INVISIBLE) {
			azimuthNorthButton.setVisibility(View.VISIBLE);
		}
	}

	public void setElevationAngle(float angle) {
		if (angle < 35f) {
			angle = 35f;
		} else if (angle > 90f) {
			angle = 90f;
		}
		this.elevationAngle = angle;
		mapView.setElevationAngle(angle);
	}

	public boolean onTouchEvent(MotionEvent event) {
		return multiTouchSupport.onTouchEvent(event)
				|| gestureDetector.onTouchEvent(event);
	}

	private class MapViewOnGestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			mapView.requestFocus();
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			PointI point31 = new PointI();
			mapView.getLocationFromScreenPoint(new PointI((int) e.getX(), (int) e.getY()), point31);
			// geocode(point31);
			//Toast.makeText(MainActivity.this, "Geocoding...", Toast.LENGTH_SHORT).show();
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			float fromX = e2.getX() + distanceX;
			float fromY = e2.getY() + distanceY;
			float toX = e2.getX();
			float toY = e2.getY();

			float dx = (fromX - toX);
			float dy = (fromY - toY);

			PointI newTarget = new PointI();
			mapView.getLocationFromScreenPoint(new PointI(mapView.getWidth() / 2 + (int) dx, mapView.getHeight() / 2 + (int) dy), newTarget);

			setTarget(newTarget);

			mapView.requestFocus();
			return true;
		}
	}

	private class MapViewMultiTouchZoomListener implements MultiTouchZoomListener {

		private float initialZoom;
		private float initialAzimuth;
		private float initialElevation;
		private PointF centerPoint;

		@Override
		public void onGestureFinished(float scale, float rotation) {
		}

		@Override
		public void onGestureInit(float x1, float y1, float x2, float y2) {
		}

		@Override
		public void onZoomStarted(PointF centerPoint) {
			initialZoom = zoom;
			initialAzimuth = azimuth;
			this.centerPoint = centerPoint;
		}

		@Override
		public void onZoomingOrRotating(float scale, float rotation) {

			PointI centerLocationBefore = new PointI();
			mapView.getLocationFromScreenPoint(
					new PointI((int) centerPoint.x, (int) centerPoint.y), centerLocationBefore);

			// Change zoom
			setZoom(initialZoom + (float) (Math.log(scale) / Math.log(2)));

			// Adjust current target position to keep touch center the same
			PointI centerLocationAfter = new PointI();
			mapView.getLocationFromScreenPoint(
					new PointI((int) centerPoint.x, (int) centerPoint.y), centerLocationAfter);
			PointI centerLocationDelta = new PointI(
					centerLocationAfter.getX() - centerLocationBefore.getX(),
					centerLocationAfter.getY() - centerLocationBefore.getY());

			setTarget(new PointI(target31.getX() - centerLocationDelta.getX(), target31.getY() - centerLocationDelta.getY()));

			/*
			// Convert point from screen to location
			PointI centerLocation = new PointI();
			mapView.getLocationFromScreenPoint(
					new PointI((int)centerPoint.x, (int)centerPoint.y), centerLocation);

			// Rotate current target around center location
			PointI target = new PointI(xI - centerLocation.getX(), yI - centerLocation.getY());
			double cosAngle = Math.cos(-Math.toRadians(rotation));
			double sinAngle = Math.sin(-Math.toRadians(rotation));

			PointI newTarget = new PointI(
					(int)(target.getX() * cosAngle - target.getY() * sinAngle + centerLocation.getX()),
					(int)(target.getX() * sinAngle + target.getY() * cosAngle + centerLocation.getY()));

			setTarget(newTarget);
			*/

			// Set rotation
			setAzimuth(initialAzimuth - rotation);
		}

		@Override
		public void onChangeViewAngleStarted() {
			initialElevation = elevationAngle;
		}

		@Override
		public void onChangingViewAngle(float angle) {
			setElevationAngle(initialElevation - angle);
		}
	}

	public void showQuickSearch(double latitude, double longitude) {
		hideContextMenu();
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			fragment.dismiss();
			//refreshMap();
		}
		QuickSearchDialogFragment.showInstance(this, "", null, true, new LatLon(latitude, longitude));
	}

	public void showQuickSearch(Object object) {
		hideContextMenu();
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			fragment.dismiss();
			//refreshMap();
		}
		QuickSearchDialogFragment.showInstance(this, "", object, true, null);
	}

	public void showQuickSearch(ShowQuickSearchMode mode, boolean showCategories) {
		hideContextMenu();
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			if (mode == ShowQuickSearchMode.NEW || (mode == ShowQuickSearchMode.NEW_IF_EXPIRED && fragment.isExpired())) {
				fragment.dismiss();
				QuickSearchDialogFragment.showInstance(this, "", null, showCategories, null);
			} else {
				fragment.show();
			}
			refreshMap();
		} else {
			QuickSearchDialogFragment.showInstance(this, "", null, showCategories, null);
		}
	}

	public void closeQuickSearch() {
		QuickSearchDialogFragment fragment = getQuickSearchDialogFragment();
		if (fragment != null) {
			fragment.closeSearch();
			refreshMap();
		}
	}

	public QuickSearchDialogFragment getQuickSearchDialogFragment() {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(QuickSearchDialogFragment.TAG);
		return fragment != null && !fragment.isDetached() && !fragment.isRemoving() ? (QuickSearchDialogFragment) fragment : null;
	}

	private void hideContextMenu() {
		if (menu.isVisible()) {
			menu.hide();
		} else if (menu.getMultiSelectionMenu().isVisible()) {
			menu.getMultiSelectionMenu().hide();
		}
	}

	public enum ShowQuickSearchMode {
		NEW,
		NEW_IF_EXPIRED,
		CURRENT,
	}
}
