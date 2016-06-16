package net.osmand.core.samples.android.sample1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.core.android.AtlasMapRendererView;
import net.osmand.core.android.CoreResourcesFromAndroidAssets;
import net.osmand.core.android.NativeCore;
import net.osmand.core.jni.AreaI;
import net.osmand.core.jni.IMapLayerProvider;
import net.osmand.core.jni.IMapStylesCollection;
import net.osmand.core.jni.LatLon;
import net.osmand.core.jni.LogSeverityLevel;
import net.osmand.core.jni.Logger;
import net.osmand.core.jni.MapObjectsSymbolsProvider;
import net.osmand.core.jni.MapPresentationEnvironment;
import net.osmand.core.jni.MapPrimitivesProvider;
import net.osmand.core.jni.MapPrimitiviser;
import net.osmand.core.jni.MapRasterLayerProvider_Software;
import net.osmand.core.jni.MapStylesCollection;
import net.osmand.core.jni.ObfMapObjectsProvider;
import net.osmand.core.jni.ObfsCollection;
import net.osmand.core.jni.OsmAndCore;
import net.osmand.core.jni.OsmAndCoreJNI;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.QIODeviceLogSink;
import net.osmand.core.jni.ResolvedMapStyle;
import net.osmand.core.jni.Utilities;
import net.osmand.core.samples.android.sample1.MultiTouchSupport.MultiTouchZoomListener;
import net.osmand.core.samples.android.sample1.SearchAPI.SearchAPICallback;
import net.osmand.core.samples.android.sample1.SearchAPI.SearchItem;
import net.osmand.core.samples.android.sample1.SearchUIHelper.SearchListAdapter;
import net.osmand.core.samples.android.sample1.SearchUIHelper.SearchRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
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
    private IMapLayerProvider mapLayerProvider1;
    private QIODeviceLogSink fileLogSink;

	private AtlasMapRendererView mapView;
	private TextView textZoom;
	private ImageButton azimuthNorthButton;

	private GestureDetector gestureDetector;
	private PointI target31;
	private float zoom;
	private float azimuth;
	private float elevationAngle;
	private MultiTouchSupport multiTouchSupport;

	private SearchAPI searchAPI;
	private ListView searchListView;
	private SearchListAdapter adapter;
	private final static int MAX_SEARCH_RESULTS = 50;

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

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		gestureDetector = new GestureDetector(this, new MapViewOnGestureListener());
		multiTouchSupport = new MultiTouchSupport(this, new MapViewMultiTouchZoomListener());

        long startTime = System.currentTimeMillis();

        // Initialize native core prior (if needed)
        if (NativeCore.isAvailable() && !NativeCore.isLoaded())
            NativeCore.load(CoreResourcesFromAndroidAssets.loadFromCurrentApplication(this));

		Logger.get().setSeverityLevelThreshold(LogSeverityLevel.Debug);

        // Inflate views
        setContentView(R.layout.activity_main);

        // Get map view
        mapView = (AtlasMapRendererView) findViewById(R.id.mapRendererView);

		textZoom = (TextView) findViewById(R.id.text_zoom);
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
        fileLogSink = QIODeviceLogSink.createFileLogSink(
                Environment.getExternalStorageDirectory() + "/osmand/osmandcore.log");
        Logger.get().addLogSink(fileLogSink);

        // Get device display density factor
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        displayDensityFactor = displayMetrics.densityDpi / 160.0f;
        referenceTileSize = (int)(256 * displayDensityFactor);
        rasterTileSize = Integer.highestOneBit(referenceTileSize - 1) * 2;
        Log.i(TAG, "displayDensityFactor = " + displayDensityFactor);
        Log.i(TAG, "referenceTileSize = " + referenceTileSize);
        Log.i(TAG, "rasterTileSize = " + rasterTileSize);

        Log.i(TAG, "Going to resolve default embedded style...");
        mapStylesCollection = new MapStylesCollection();
        mapStyle = mapStylesCollection.getResolvedStyleByName("default");
        if (mapStyle == null)
        {
            Log.e(TAG, "Failed to resolve style 'default'");
            System.exit(0);
        }

        Log.i(TAG, "Going to prepare OBFs collection");
        obfsCollection = new ObfsCollection();
        Log.i(TAG, "Will load OBFs from " + Environment.getExternalStorageDirectory() + "/osmand");
        obfsCollection.addDirectory(Environment.getExternalStorageDirectory() + "/osmand", false);

        Log.i(TAG, "Going to prepare all resources for renderer");
        mapPresentationEnvironment = new MapPresentationEnvironment(
				mapStyle,
				displayDensityFactor,
                1.0f,
                1.0f,
				MapUtils.LANGUAGE);
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

		System.out.println("NATIVE_INITIALIZED = " + (System.currentTimeMillis() - startTime) / 1000f);

		//Setup search

		searchAPI = new SearchAPI(obfsCollection);

		final EditText searchEditText = (EditText) findViewById(R.id.searchEditText);
		searchEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() > 2) {
					runSearch(getScreenBounds31(), s.toString());
				}
			}
		});
		searchEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && adapter.getCount() > 0 && isSearchListHidden()) {
					showSearchList();
					LatLon latLon = Utilities.convert31ToLatLon(target31);
					adapter.updateDistance(latLon.getLatitude(), latLon.getLongitude());
					adapter.notifyDataSetChanged();
				}
			}
		});

		ImageButton clearButton = (ImageButton) findViewById(R.id.clearButton);
		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				searchEditText.setText("");
				adapter.clear();
				adapter.notifyDataSetChanged();
				hideSearchList();
			}
		});

		searchListView = (ListView) findViewById(android.R.id.list);
		adapter = new SearchListAdapter(this);
		searchListView.setAdapter(adapter);
		searchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				hideSearchList();
				mapView.requestFocus();
				SearchRow item = adapter.getItem(position);
				PointI target = Utilities.convertLatLonTo31(new LatLon(item.getLatitude(), item.getLongitude()));
				setTarget(target);
				setZoom(17f);
			}
		});

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

	private AreaI getScreenBounds31() {
		PointI topLeftPoint = new PointI();
		PointI bottomRightPoint = new PointI();
		mapView.getLocationFromScreenPoint(new PointI(0, 0), topLeftPoint);
		mapView.getLocationFromScreenPoint(new PointI(mapView.getWidth(), mapView.getHeight()), bottomRightPoint);
		return new AreaI(topLeftPoint, bottomRightPoint);
	}

	private boolean isSearchListHidden() {
		return searchListView.getVisibility() != View.VISIBLE;
	}

	private void showSearchList() {
		if (isSearchListHidden()) {
			ViewCompat.setAlpha(searchListView, 0f);
			searchListView.setVisibility(View.VISIBLE);
			ViewCompat.animate(searchListView).alpha(1f).setListener(null);
		}
	}

	private void hideSearchList() {
		ViewCompat.animate(searchListView).alpha(0f).setListener(new ViewPropertyAnimatorListener() {
			@Override
			public void onAnimationStart(View view) {

			}

			@Override
			public void onAnimationEnd(View view) {
				searchListView.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationCancel(View view) {
				searchListView.setVisibility(View.GONE);
			}
		});
	}

	public void saveMapState() {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		Editor edit = prefs.edit();
		LatLon latLon = Utilities.convert31ToLatLon(target31);
		edit.putFloat(PREF_MAP_CENTER_LAT, (float)latLon.getLatitude());
		edit.putFloat(PREF_MAP_CENTER_LON, (float)latLon.getLongitude());
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
		setTarget(Utilities.convertLatLonTo31(new LatLon(prefLat, prefLon)));
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

	private void runSearch(AreaI bounds31, String keyword) {

		searchAPI.setObfAreaFilter(bounds31);
		searchAPI.startSearch(keyword, MAX_SEARCH_RESULTS, new SearchAPICallback() {
			@Override
			public void onSearchFinished(List<SearchItem> searchItems, boolean cancelled) {
				if (searchItems != null && !cancelled) {
					LatLon latLon = Utilities.convert31ToLatLon(target31);
					List<SearchRow> rows = new ArrayList<>();
					for (SearchItem item : searchItems) {
						SearchRow row = new SearchRow(item);
						rows.add(row);
					}

					adapter.clear();
					adapter.addAll(rows);
					adapter.updateDistance(latLon.getLatitude(), latLon.getLongitude());
					adapter.sort(new Comparator<SearchRow>() {
						@Override
						public int compare(SearchRow lhs, SearchRow rhs) {
							int res = Double.compare(lhs.getDistance(), rhs.getDistance());
							if (res == 0) {
								return lhs.getSearchItem().getLocalizedName().compareToIgnoreCase(rhs.getSearchItem().getLocalizedName());
							} else {
								return res;
							}
						}
					});
					adapter.notifyDataSetChanged();
					if (adapter.getCount() > 0) {
						searchListView.setSelection(0);
					}

					showSearchList();
				}
			}
		});
	}

	private class MapViewOnGestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			mapView.requestFocus();
			return true;
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
			mapView.getLocationFromScreenPoint(new PointI(mapView.getWidth() / 2 + (int)dx, mapView.getHeight() / 2 + (int)dy), newTarget);

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
					new PointI((int)centerPoint.x, (int)centerPoint.y), centerLocationBefore);

			// Change zoom
			setZoom(initialZoom + (float)(Math.log(scale) / Math.log(2)));

			// Adjust current target position to keep touch center the same
			PointI centerLocationAfter = new PointI();
			mapView.getLocationFromScreenPoint(
					new PointI((int)centerPoint.x, (int)centerPoint.y), centerLocationAfter);
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
}
