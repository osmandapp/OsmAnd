package net.osmand.core.samples.android.sample1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
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

import net.osmand.Location;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.core.jni.AmenitySymbolsProvider.AmenitySymbolsGroup;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.FColorRGB;
import net.osmand.core.jni.IBillboardMapSymbol;
import net.osmand.core.jni.IMapLayerProvider;
import net.osmand.core.jni.IMapRenderer.MapSymbolInformation;
import net.osmand.core.jni.IMapStylesCollection;
import net.osmand.core.jni.Logger;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarker.SymbolsGroup;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.MapObject;
import net.osmand.core.jni.MapObjectsSymbolsProvider;
import net.osmand.core.jni.MapObjectsSymbolsProvider.MapObjectSymbolsGroup;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.MapPrimitivesProvider;
import net.osmand.core.jni.MapPrimitiviser;
import net.osmand.core.jni.MapRasterLayerProvider_Software;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.MapSymbolInformationList;
import net.osmand.core.jni.MapSymbolsGroup.AdditionalBillboardSymbolInstanceParameters;
import net.osmand.core.jni.ObfMapObject;
import net.osmand.core.jni.ObfMapObjectsProvider;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QIODeviceLogSink;
import net.osmand.core.jni.ResolvedMapStyle;
import net.osmand.core.jni.SwigUtilities;
import net.osmand.core.jni.Utilities;
import net.osmand.core.samples.android.sample1.MultiTouchSupport.MultiTouchZoomListener;
import net.osmand.core.samples.android.sample1.SampleLocationProvider.SampleCompassListener;
import net.osmand.core.samples.android.sample1.SampleLocationProvider.SampleLocationListener;
import net.osmand.core.samples.android.sample1.data.PointDescription;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MapContextMenu;
import net.osmand.core.samples.android.sample1.mapcontextmenu.MenuController;
import net.osmand.core.samples.android.sample1.search.QuickSearchDialogFragment;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.PoiCategory;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.core.samples.android.sample1.SampleApplication.PERMISSION_REQUEST_LOCATION_ON_BUTTON;
import static net.osmand.core.samples.android.sample1.SampleApplication.PERMISSION_REQUEST_LOCATION_ON_RESUME;
import static net.osmand.core.samples.android.sample1.SampleApplication.PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements SampleLocationListener, SampleCompassListener {
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
	private ImageButton compassButton;
	private CompassDrawable compassDrawable;

	private GestureDetector gestureDetector;
	private PointI target31;
	private float zoom;
	private float azimuth = 0;
	private float elevationAngle;
	private MultiTouchSupport multiTouchSupport;

	private MapContextMenu menu;

	// Context pin marker
	private MapMarkersCollection contextPinMarkersCollection;
	private MapMarker contextPinMarker;
	private static final int MARKER_ID_CONTEXT_PIN = 1;

	// "My location" marker, "My course" marker and collection
	private MapMarkersCollection myMarkersCollection;
	private MapMarker myLocationMarker;
	private static final int MARKER_ID_MY_LOCATION = 2;

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
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			switch (requestCode) {
				case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE:
					if (!InstallOsmandAppDialog.wasShown()) {
						checkMapsInstalled();
					}
					getMyApplication().initPoiTypes();
					break;
				case PERMISSION_REQUEST_LOCATION_ON_BUTTON:
					goToLocation();
				case PERMISSION_REQUEST_LOCATION_ON_RESUME:
					getMyApplication().getLocationProvider().resumeAllUpdates();
					break;
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SampleApplication app = getMyApplication();

		gestureDetector = new GestureDetector(this, new MapViewOnGestureListener());
		multiTouchSupport = new MultiTouchSupport(this, new MapViewMultiTouchZoomListener());

		// Inflate views
		setContentView(R.layout.activity_main);

		boolean externalStoragePermissionGranted = ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
		if (!externalStoragePermissionGranted) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
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

		compassButton = (ImageButton) findViewById(R.id.map_compass_button);
		compassButton.setContentDescription(app.getString("rotate_map_compass_opt"));
		compassDrawable = new CompassDrawable(app.getIconsCache().getIcon(R.drawable.map_compass));
		compassButton.setImageDrawable(compassDrawable);
		compassButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setAzimuth(0f);
			}
		});

		ImageButton myLocationButton = (ImageButton) findViewById(R.id.map_my_location_button);
		myLocationButton.setImageDrawable(app.getIconsCache().getIcon("map_my_location", R.color.color_myloc_distance));
		myLocationButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (SampleLocationProvider.isLocationPermissionAvailable(MainActivity.this)) {
					goToLocation();
				} else {
					ActivityCompat.requestPermissions(MainActivity.this,
							new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
							PERMISSION_REQUEST_LOCATION_ON_BUTTON);
				}
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

		initMapMarkers();

		menu = new MapContextMenu();
		menu.setMainActivity(this);

		if (!InstallOsmandAppDialog.show(getSupportFragmentManager(), this)
				&& externalStoragePermissionGranted) {
			checkMapsInstalled();
		}
	}

	private void checkMapsInstalled() {
		File mapsDir = new File(getMyApplication().getAbsoluteAppPath());
		boolean noMapsFound;
		if (mapsDir.exists()) {
			File[] maps = mapsDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String filename) {
					return filename.toLowerCase().endsWith(".obf");
				}
			});
			noMapsFound = maps == null || maps.length == 0;
		} else {
			noMapsFound = true;
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

	public void initMapMarkers() {

		// Create my location marker
		myMarkersCollection = new MapMarkersCollection();
		MapMarkerBuilder myLocMarkerBuilder = new MapMarkerBuilder();
		myLocMarkerBuilder.setMarkerId(MARKER_ID_MY_LOCATION);
		myLocMarkerBuilder.setIsAccuracyCircleSupported(true);
		myLocMarkerBuilder.setAccuracyCircleBaseColor(new FColorRGB(32/255f, 173/255f, 229/255f));
		myLocMarkerBuilder.setBaseOrder(-206000);
		myLocMarkerBuilder.setIsHidden(true);
		Bitmap myLocationBitmap = OsmandResources.getBitmap("map_pedestrian_location");
		if (myLocationBitmap != null) {
			myLocMarkerBuilder.setPinIcon(SwigUtilities.createSkBitmapARGB888With(
					myLocationBitmap.getWidth(), myLocationBitmap.getHeight(),
					SampleUtils.getBitmapAsByteArray(myLocationBitmap)));
		}
		myLocationMarker = myLocMarkerBuilder.buildAndAddToCollection(myMarkersCollection);

		mapView.addSymbolsProvider(myMarkersCollection);

		// Create context pin marker
		contextPinMarkersCollection = new MapMarkersCollection();
		MapMarkerBuilder contextMarkerBuilder = new MapMarkerBuilder();
		contextMarkerBuilder.setMarkerId(MARKER_ID_CONTEXT_PIN);
		contextMarkerBuilder.setIsAccuracyCircleSupported(false);
		contextMarkerBuilder.setBaseOrder(-210000);
		contextMarkerBuilder.setIsHidden(true);
		Bitmap pinBitmap = OsmandResources.getBitmap("map_pin_context_menu");
		if (pinBitmap != null) {
			contextMarkerBuilder.setPinIcon(SwigUtilities.createSkBitmapARGB888With(
					pinBitmap.getWidth(), pinBitmap.getHeight(),
					SampleUtils.getBitmapAsByteArray(pinBitmap)));
			contextMarkerBuilder.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.Top);
			contextMarkerBuilder.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal);
		}
		contextPinMarker = contextMarkerBuilder.buildAndAddToCollection(contextPinMarkersCollection);

		mapView.addSymbolsProvider(contextPinMarkersCollection);
	}

	public void showContextMarker(@NonNull LatLon location) {
		mapView.suspendSymbolsUpdate();
		PointI locationI = Utilities.convertLatLonTo31(new net.osmand.core.jni.LatLon(location.getLatitude(), location.getLongitude()));
		contextPinMarker.setPosition(locationI);
		contextPinMarker.setIsHidden(false);
		mapView.resumeSymbolsUpdate();
	}

	public void hideContextMarker() {
		mapView.suspendSymbolsUpdate();
		contextPinMarker.setIsHidden(true);
		mapView.resumeSymbolsUpdate();
	}

	public void goToLocation() {
		if (mapView != null) {
			SampleLocationProvider locationProvider = getMyApplication().getLocationProvider();
			if (locationProvider.getLastKnownLocation() != null) {
				net.osmand.Location lastKnownLocation = locationProvider.getLastKnownLocation();
				int fZoom = zoom < 15 ? 15 : (int)zoom;
				showOnMap(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), fZoom);
				//todo animation
				//AnimateDraggingMapThread thread = mapView.getAnimatedDraggingThread();
				//thread.startMoving(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), fZoom, false);
			}
			if (locationProvider.getLastKnownLocation() == null) {
				getMyApplication().showToastMessage(getMyApplication().getString("unknown_location"));
			}
		}
	}

	@Override
	public void updateLocation(Location location) {
		final SampleApplication app = getMyApplication();
		final Location lastKnownLocation = app.getLocationProvider().getLastKnownLocation();
		if (lastKnownLocation == null || mapView == null){
			app.runInUIThread(new Runnable() {
				@Override
				public void run() {
					if (!myLocationMarker.isHidden()) {
						mapView.suspendSymbolsUpdate();
						myLocationMarker.setIsHidden(true);
						mapView.resumeSymbolsUpdate();
					}
				}
			});
			return;
		}

		final PointI target31 = Utilities.convertLatLonTo31(
				new net.osmand.core.jni.LatLon(location.getLatitude(), location.getLongitude()));

		app.runInUIThread(new Runnable() {
			@Override
			public void run() {
				mapView.suspendSymbolsUpdate();
				myLocationMarker.setIsHidden(false);
				myLocationMarker.setPosition(target31);
				myLocationMarker.setIsAccuracyCircleVisible(true);
				myLocationMarker.setAccuracyCircleRadius(lastKnownLocation.getAccuracy());
				mapView.resumeSymbolsUpdate();
			}
		});

		if (menu != null) {
			menu.updateMyLocation(location);
		}
	}

	@Override
	public void updateCompassValue(float value) {
		if (menu != null) {
			menu.updateCompassValue(value);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		SampleApplication app = getMyApplication();
		app.getLocationProvider().checkIfLastKnownLocationIsValid();
		app.getLocationProvider().addLocationListener(this);
		if (SampleLocationProvider.isLocationPermissionAvailable(this)) {
			app.getLocationProvider().resumeAllUpdates();
		} else {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					PERMISSION_REQUEST_LOCATION_ON_RESUME);
		}
		mapView.handleOnResume();
	}

	@Override
	protected void onPause() {
		SampleApplication app = getMyApplication();
		app.getLocationProvider().pauseAllUpdates();
		app.getLocationProvider().removeLocationListener(this);
		saveMapState();
		mapView.handleOnPause();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mapView.handleOnDestroy();
		super.onDestroy();
	}

	public AtlasMapRendererView getMapView() {
		return mapView;
	}

	public void showOnMap(LatLon latLon, int zoom) {
		if (latLon != null) {
			showOnMap(latLon.getLatitude(), latLon.getLongitude(), zoom);
		}
	}

	public void showOnMap(double latitude, double longitude, int zoom) {
		PointI target = Utilities.convertLatLonTo31(
				new net.osmand.core.jni.LatLon(latitude, longitude));
		setTarget(target);
		setZoom(zoom);
	}

	public void refreshMap() {
	}

	public MapContextMenu getContextMenu() {
		return menu;
	}

	public boolean showContextMenu(@NonNull LatLon latLon,
								   @Nullable PointDescription pointDescription,
								   @Nullable Object object) {
		if (menu.getMultiSelectionMenu().isVisible()) {
			menu.getMultiSelectionMenu().hide();
		}
		if (!getBox().containsLatLon(latLon)) {
			menu.setMapCenter(latLon);
			menu.setCenterMarker(true);
		}
		menu.show(latLon, pointDescription, object);
		return true;
	}

	private void showContextMenuForSelectedObjects(final LatLon latLon, final List<Object> selectedObjects) {
		menu.getMultiSelectionMenu().show(latLon, selectedObjects);
	}

	private void hideMultiContextMenu() {
		if (menu.getMultiSelectionMenu().isVisible()) {
			menu.getMultiSelectionMenu().hide();
		}
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
		azimuth = angle;
		mapView.setAzimuth(angle);

		if (angle == 0f && compassButton.getVisibility() == View.VISIBLE) {
			compassButton.setVisibility(View.INVISIBLE);
		} else if (angle != 0f) {
			if (compassButton.getVisibility() != View.VISIBLE) {
				compassButton.setVisibility(View.VISIBLE);
			}
			compassButton.invalidate();
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
		return multiTouchSupport.onTouchEvent(event) || gestureDetector.onTouchEvent(event);
	}

	private class MapViewOnGestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			PointI point31 = new PointI();
			int[] offset = new int[]{0, 0};
			mapView.getLocationInWindow(offset);
			PointI touchPoint = new PointI((int) e.getX() - offset[0], (int) e.getY() - offset[1]);
			mapView.getLocationFromScreenPoint(touchPoint, point31);
			net.osmand.core.jni.LatLon jniLatLon = Utilities.convert31ToLatLon(point31);
			double lat = jniLatLon.getLatitude();
			double lon = jniLatLon.getLongitude();

			int delta = 20;
			AreaI area = new AreaI(new PointI(touchPoint.getX() - delta, touchPoint.getY() - delta),
					new PointI(touchPoint.getX() + delta, touchPoint.getY() + delta));

			List<Object> selectedObjects = new ArrayList<>();

			MapSymbolInformationList symbolInfos = mapView.getSymbolsIn(area, false);
			for (int i = 0; i < symbolInfos.size(); i++) {
				MapSymbolInformation symbolInfo = symbolInfos.get(i);

				IBillboardMapSymbol billboardMapSymbol;
				try {
					billboardMapSymbol = IBillboardMapSymbol.dynamic_pointer_cast(symbolInfo.getMapSymbol());
				} catch (Exception eBillboard) {
					billboardMapSymbol = null;
				}

				if (billboardMapSymbol != null) {
					lon = Utilities.get31LongitudeX(billboardMapSymbol.getPosition31().getX());
					lat = Utilities.get31LatitudeY(billboardMapSymbol.getPosition31().getY());

					AdditionalBillboardSymbolInstanceParameters billboardAdditionalParams;
					try {
						billboardAdditionalParams = AdditionalBillboardSymbolInstanceParameters.dynamic_pointer_cast(symbolInfo.getInstanceParameters());
					} catch (Exception eBillboardParams) {
						billboardAdditionalParams = null;
					}
					if (billboardAdditionalParams != null) {
						if (billboardAdditionalParams.getOverridesPosition31()) {
							lon = Utilities.get31LongitudeX(billboardAdditionalParams.getPosition31().getX());
							lat = Utilities.get31LatitudeY(billboardAdditionalParams.getPosition31().getY());
						}
					}

					String name = null;
					MapMarker mapMarker;
					try {
						SymbolsGroup markerSymbolsGroup = SymbolsGroup.dynamic_cast(symbolInfo.getMapSymbol().getGroupPtr());
						mapMarker = markerSymbolsGroup.getMapMarker();
					} catch (Exception eMapMarker) {
						mapMarker = null;
					}
					if (mapMarker != null) {
						if (mapMarker.getMarkerId() == MARKER_ID_CONTEXT_PIN) {
							hideMultiContextMenu();
							menu.show();
						} else if (mapMarker.getMarkerId() == MARKER_ID_MY_LOCATION) {
							hideMultiContextMenu();
							LatLon latLon = new LatLon(lat, lon);
							showContextMenu(latLon, new PointDescription(
									PointDescription.POINT_TYPE_MY_LOCATION,
									getMyApplication().getString("shared_string_my_location"), ""), latLon);
						}
						return true;
					} else {
						net.osmand.core.jni.Amenity amenity;
						try {
							AmenitySymbolsGroup amenitySymbolGroup =
									AmenitySymbolsGroup.dynamic_cast(symbolInfo.getMapSymbol().getGroupPtr());
							amenity = amenitySymbolGroup.getAmenity();
						} catch (Exception eAmenity) {
							amenity = null;
						}
						if (amenity != null) {
							name = amenity.getNativeName();
							net.osmand.core.jni.LatLon aLatLon = Utilities.convert31ToLatLon(amenity.getPosition31());
							Amenity osmandAmenity = findAmenity(amenity.getId().getId().longValue() >> 7,
									aLatLon.getLatitude(), aLatLon.getLongitude(), name);
							if (osmandAmenity != null) {
								if (!selectedObjects.contains(osmandAmenity)) {
									selectedObjects.add(osmandAmenity);
								}
								continue;
							}
						} else {
							MapObject mapObject;
							try {
								MapObjectSymbolsGroup objSymbolGroup =
										MapObjectSymbolsGroup.dynamic_cast(symbolInfo.getMapSymbol().getGroupPtr());
								mapObject = objSymbolGroup.getMapObject();
							} catch (Exception eMapObject) {
								mapObject = null;
							}
							ObfMapObject obfMapObject;
							if (mapObject != null) {
								name = mapObject.getCaptionInNativeLanguage();
								try {
									obfMapObject = ObfMapObject.dynamic_pointer_cast(mapObject);
								} catch (Exception eObfMapObject) {
									obfMapObject = null;
								}
								if (obfMapObject != null) {
									name = obfMapObject.getCaptionInNativeLanguage();
									Amenity osmandAmenity = findAmenity(
											obfMapObject.getId().getId().longValue() >> 7, lat, lon, name);
									if (osmandAmenity != null) {
										if (!selectedObjects.contains(osmandAmenity)) {
											selectedObjects.add(osmandAmenity);
										}
										continue;
									}
								}
							}
						}
						if (name != null && name.trim().length() > 0) {
							selectedObjects.add(new PointDescription("", name));
						} else {
							selectedObjects.add(new PointDescription(lat, lon));
						}
					}
				}
			}

			if (selectedObjects.size() == 1) {
				Object selectedObj = selectedObjects.get(0);
				LatLon latLon = new LatLon(lat, lon); //MenuController.getObjectLocation(selectedObj);
				PointDescription pointDescription = MenuController.getObjectName(selectedObj);
				//if (latLon == null) {
				//	latLon = new LatLon(lat, lon);
				//}
				showContextMenu(latLon, pointDescription, selectedObj);
				return true;

			} else if (selectedObjects.size() > 1) {
				showContextMenuForSelectedObjects(new LatLon(lat, lon), selectedObjects);
				return true;
			}

			hideMultiContextMenu();
			hideContextMenu();

			return true;
		}

		private Amenity findAmenity(long id, double lat, double lon, String name) {
			QuadRect rect = MapUtils.calculateLatLonBbox(lat, lon, 50);
			List<Amenity> amenities = getMyApplication().getResourceManager().searchAmenities(
					new BinaryMapIndexReader.SearchPoiTypeFilter() {
						@Override
						public boolean accept(PoiCategory type, String subcategory) {
							return true;
						}

						@Override
						public boolean isEmpty() {
							return false;
						}
					}, rect.top, rect.left, rect.bottom, rect.right, -1, null);

			Amenity res = null;
			for (Amenity amenity : amenities) {
				Long amenityId = amenity.getId() >> 1;
				if (amenityId == id) {
					res = amenity;
					break;
				}
			}
			if (res == null && name != null && name.length() > 0) {
				for (Amenity amenity : amenities) {
					if (name.equals(amenity.getName())) {
						res = amenity;
					}
					if (res != null) {
						break;
					}
				}
			}
			return res;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			if (!multiTouchSupport.isInMultiTouch()) {
				PointI point31 = new PointI();
				int[] offset = new int[]{0, 0};
				mapView.getLocationInWindow(offset);
				mapView.getLocationFromScreenPoint(new PointI((int) e.getX() - offset[0], (int) e.getY() - offset[1]), point31);
				net.osmand.core.jni.LatLon jniLatLon = Utilities.convert31ToLatLon(point31);
				showContextMenu(new LatLon(jniLatLon.getLatitude(), jniLatLon.getLongitude()),
						new PointDescription(jniLatLon.getLatitude(), jniLatLon.getLongitude()), null);
			}
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
			hideContextMenu(false);
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
		hideContextMenu(true);
	}

	private void hideContextMenu(boolean restorePosition) {
		if (menu.isVisible()) {
			if (!restorePosition) {
				menu.updateMapCenter(null);
			}
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

	private class CompassDrawable extends Drawable {

		private Drawable original;

		public CompassDrawable(Drawable original) {
			this.original = original;
		}

		@Override
		public void draw(@NonNull Canvas canvas) {
			canvas.save();
			canvas.rotate(azimuth, getIntrinsicWidth() / 2, getIntrinsicHeight() / 2);
			original.draw(canvas);
			canvas.restore();
		}

		@Override
		public int getMinimumHeight() {
			return original.getMinimumHeight();
		}

		@Override
		public int getMinimumWidth() {
			return original.getMinimumWidth();
		}

		@Override
		public int getIntrinsicHeight() {
			return original.getIntrinsicHeight();
		}

		@Override
		public int getIntrinsicWidth() {
			return original.getIntrinsicWidth();
		}

		@Override
		public void setChangingConfigurations(int configs) {
			super.setChangingConfigurations(configs);
			original.setChangingConfigurations(configs);
		}

		@Override
		public void setBounds(int left, int top, int right, int bottom) {
			super.setBounds(left, top, right, bottom);
			original.setBounds(left, top, right, bottom);
		}

		@Override
		public void setAlpha(int alpha) {
			original.setAlpha(alpha);
		}

		@Override
		public void setColorFilter(ColorFilter cf) {
			original.setColorFilter(cf);
		}

		@Override
		public int getOpacity() {
			return original.getOpacity();
		}
	}
}
