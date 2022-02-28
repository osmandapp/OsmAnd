package net.osmand.plus.plugins.rastermaps;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.slider.RangeSlider;

import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.layers.base.BaseMapLayer;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class DownloadTilesFragment extends BaseOsmAndFragment implements IMapLocationListener, OsmAndLocationListener {

	public static final String TAG = DownloadTilesFragment.class.getSimpleName();

	public static final Uri HELP_URI = Uri.parse("https://docs.osmand.net/en/main@latest/osmand/map/raster-maps#download--update-tiles");

	private static final String KEY_SELECTED_MIN_ZOOM = "selected_min_zoom";
	private static final String KEY_SELECTED_MAX_ZOOM = "selected_max_zoom";

	private OsmandApplication app;
	private OsmandSettings settings;
	private boolean nightMode;

	private OsmandMapTileView mapView;
	private TilesPreviewDrawer tilesPreviewDrawer;

	private View view;
	private boolean mapWindowTouched = false;
	private boolean wasDrawerDisabled;

	private int selectedMinZoom;
	private int selectedMaxZoom;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = requireSettings();
		nightMode = isNightMode(true);
		mapView = requireMapActivity().getMapView();
		tilesPreviewDrawer = new TilesPreviewDrawer(app);
		if (savedInstanceState != null) {
			selectedMinZoom = savedInstanceState.getInt(KEY_SELECTED_MIN_ZOOM);
			selectedMaxZoom = savedInstanceState.getInt(KEY_SELECTED_MAX_ZOOM);
		} else {
			ITileSource tileSource = settings.getMapTileSource(false);
			selectedMaxZoom = tileSource.getMaximumZoomSupported();
			int currentZoom = mapView.getZoom();
			selectedMinZoom = Math.min(currentZoom, selectedMaxZoom);
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MapActivity mapActivity = requireMapActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(mapActivity, nightMode);
		view = themedInflater.inflate(R.layout.download_tiles_fragment, container, false);

		mapView.rotateToAnimate(0);

		setupToolbar();
		view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				moveMapCenterToMapWindow();
				updateContent();
			}
		});
		setupDownloadButton();
		showHideMapControls(false);
		restrictMapMovableArea();

		return view;
	}

	private void setupToolbar() {
		AndroidUtils.addStatusBarPadding21v(view.getContext(), view);
		View toolbar = view.findViewById(R.id.toolbar);

		ImageButton backButton = toolbar.findViewById(R.id.back_button);
		backButton.setOnClickListener(v -> dismiss());
		UiUtilities.rotateImageByLayoutDirection(backButton);

		View helpButton = toolbar.findViewById(R.id.help_button);
		helpButton.setOnClickListener(v -> {
			Context context = getContext();
			if (context != null) {
				WikipediaDialogFragment.showFullArticle(context, HELP_URI, nightMode);
			}
		});
	}

	private void moveMapCenterToMapWindow() {
		RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox();
		double lat = tileBox.getCenterLatLon().getLatitude();
		double lon = tileBox.getCenterLatLon().getLongitude();
		View mapWindow = view.findViewById(R.id.map_window);
		int[] xy = new int[2];
		mapWindow.getLocationOnScreen(xy);
		int marginTop = xy[1];

		mapView.fitLocationToMap(lat, lon, tileBox.getZoom(), mapWindow.getWidth(), mapWindow.getHeight(),
				marginTop, true);
	}

	private void updateContent() {
		ITileSource tileSource = settings.getMapTileSource(false);

		setupMapSourceSetting(tileSource);
		setupTilesPreview();
		setupMinMaxZoom();
		setupSlider(tileSource);
		setupTilesDownloadInfo(tileSource);
	}

	private void setupMapSourceSetting(@NonNull ITileSource tileSource) {
		View mapSourceContainer = view.findViewById(R.id.map_source_container);
		mapSourceContainer.setOnClickListener(v -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getMapLayers().selectMapLayer(mapActivity, false, mapSourceName -> {
					if (shouldShowDialog(app)) {
						ITileSource newTileSource = settings.getMapTileSource(false);
						int currentZoom = mapView.getZoom();
						selectedMaxZoom = newTileSource.getMaximumZoomSupported();
						selectedMinZoom = Math.min(currentZoom, selectedMaxZoom);
						updateContent();
					} else {
						app.showToastMessage(R.string.maps_could_not_be_downloaded);
						dismiss();
					}
					return true;
				});
			}
		});

		TextView selectedMapSourceText = view.findViewById(R.id.selected_map_source);
		String selectedMapSource = tileSource.getName()
				.replace(IndexConstants.SQLITE_EXT, "");
		selectedMapSourceText.setText(selectedMapSource);
	}

	private void setupSlider(@NonNull ITileSource tileSource) {
		int currentZoom = mapView.getZoom();
		int maxZoom = tileSource.getMaximumZoomSupported();
		int minZoom = Math.min(currentZoom, maxZoom);

		RangeSlider slider = view.findViewById(R.id.zooms_range_slider);
		UiUtilities.setupSlider(slider, nightMode, ColorUtilities.getActiveColor(app, nightMode), true);
		slider.clearOnChangeListeners();
		boolean multipleZoomsSupported = minZoom < maxZoom;
		slider.setEnabled(multipleZoomsSupported);

		if (multipleZoomsSupported) {
			slider.setValueFrom(minZoom);
			slider.setValueTo(maxZoom);
			slider.setValues(((float) selectedMinZoom), ((float) selectedMaxZoom));
			slider.addOnChangeListener((slider1, value, fromUser) -> {
				List<Float> minMax = slider.getValues();
				selectedMinZoom = minMax.get(0).intValue();
				selectedMaxZoom = minMax.get(1).intValue();
				setupTilesPreview();
				setupMinMaxZoom();
				setupTilesDownloadInfo(tileSource);
			});
		}
	}

	private void setupTilesPreview() {
		TextView minZoomText = view.findViewById(R.id.min_zoom_tile_preview).findViewById(R.id.tile_zoom);
		TextView maxZoomText = view.findViewById(R.id.max_zoom_tile_preview).findViewById(R.id.tile_zoom);

		minZoomText.setText(String.valueOf(selectedMinZoom));
		maxZoomText.setText(String.valueOf(selectedMaxZoom));
	}

	private void setupMinMaxZoom() {
		TextView minZoomText = view.findViewById(R.id.min_zoom);
		TextView maxZoomText = view.findViewById(R.id.max_zoom);

		minZoomText.setText(String.valueOf(selectedMinZoom));
		maxZoomText.setText(String.valueOf(selectedMaxZoom));
	}

	@SuppressLint("StringFormatMatches")
	private void setupTilesDownloadInfo(@NonNull ITileSource tileSource) {
		TextView tilesNumberText = view.findViewById(R.id.tiles_number);
		TextView estimatedDownloadSizeText = view.findViewById(R.id.estimated_download_size);

		QuadRect latLonRect = getLatLonRectOfMapWindow();
		boolean ellipticYTile = tileSource.isEllipticYTile();

		long tilesNumber = DownloadTilesHelper.getTilesNumber(selectedMinZoom, selectedMaxZoom, latLonRect, ellipticYTile);
		float estimatedDownloadSizeMB = DownloadTilesHelper.getApproxTilesSizeMb(selectedMinZoom,
				selectedMaxZoom, latLonRect, tileSource, app.getResourceManager().getBitmapTilesCache());

		String formattedTilesNumber = OsmAndFormatter.formatValue(tilesNumber, "",
				false, 0, app).value;
		String formattedSize = OsmAndFormatter.formatValue(estimatedDownloadSizeMB, "",
				false, 2, app).value;
		String formattedSizeWithUnit = getString(R.string.shared_string_memory_mb_desc, formattedSize);
		String formattedDownloadSize = app.getString(R.string.ltr_or_rtl_combine_via_space, "~",
				formattedSizeWithUnit);

		tilesNumberText.setText(formattedTilesNumber);
		estimatedDownloadSizeText.setText(formattedDownloadSize);
	}

	private void setupDownloadButton() {
		View downloadButton = view.findViewById(R.id.dismiss_button);
		UiUtilities.setupDialogButton(nightMode, downloadButton, DialogButtonType.PRIMARY,
				R.string.shared_string_download);
		downloadButton.setOnClickListener(v -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();
				QuadRect latLonRect = getLatLonRectOfMapWindow();
				TilesDownloadProgressFragment.showInstance(fragmentManager, selectedMinZoom, selectedMaxZoom,
						latLonRect);
			}
		});
	}

	@SuppressLint("ClickableViewAccessibility")
	private void restrictMapMovableArea() {
		view.setOnTouchListener((v, event) -> {
			boolean mapWindowTouched = this.mapWindowTouched;
			this.mapWindowTouched = false;
			return !mapWindowTouched;
		});
		view.findViewById(R.id.map_window_container).setOnTouchListener((v, event) -> {
			mapWindowTouched = true;
			return false;
		});
	}

	@Override
	public void onStart() {
		super.onStart();
		mapView.addMapLocationListener(this);
		app.getLocationProvider().addLocationListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !wasDrawerDisabled) {
			mapActivity.enableDrawer();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_SELECTED_MIN_ZOOM, selectedMinZoom);
		outState.putInt(KEY_SELECTED_MAX_ZOOM, selectedMaxZoom);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mapView.removeMapLocationListener(this);
		app.getLocationProvider().removeLocationListener(this);
		showHideMapControls(true);
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			if (!fragmentManager.isStateSaved()) {
				fragmentManager.popBackStack(TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
			}
		}
	}

	@NonNull
	private QuadRect getLatLonRectOfMapWindow() {
		View mapWindow = view.findViewById(R.id.map_window);

		int[] xy = new int[2];
		mapWindow.getLocationOnScreen(xy);

		int left = xy[0];
		int top = xy[1];
		int right = left + mapWindow.getWidth();
		int bottom = top + mapWindow.getHeight();

		RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox();
		double leftLon = tileBox.getLonFromPixel(left, top);
		double topLat = tileBox.getLatFromPixel(left, top);
		double rightLon = tileBox.getLonFromPixel(right, bottom);
		double bottomLat = tileBox.getLatFromPixel(right, bottom);

		return new QuadRect(leftLon, topLat, rightLon, bottomLat);
	}

	private void showHideMapControls(boolean show) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getWidgetsVisibilityHelper().updateControlsVisibility(show, show);
		}
	}

	@Override
	public void locationChanged(double newLatitude, double newLongitude, Object source) {
		app.runInUIThread(() -> {
			int currentZoom = mapView.getZoom();
			if (currentZoom > selectedMinZoom) {
				selectedMinZoom = currentZoom;
			}
			updateTilesPreview();
			updateContent();
		});
	}

	@Override
	public void updateLocation(Location location) {
		setupTilesDownloadInfo(settings.getMapTileSource(false));
		updateTilesPreview();
	}

	private void updateTilesPreview() {
		QuadRect latLonRectOfMapWindow = getLatLonRectOfMapWindow();
		LatLon mapWindowCenter = new LatLon(latLonRectOfMapWindow.centerY(), latLonRectOfMapWindow.centerX());
		Pair<Bitmap, Bitmap> bitmaps = tilesPreviewDrawer.drawTilesPreview(mapWindowCenter, selectedMinZoom, selectedMaxZoom);

		View minZoomPreviewContainer = view.findViewById(R.id.min_zoom_tile_preview);
		View maxZoomPreviewContainer = view.findViewById(R.id.max_zoom_tile_preview);

		AppCompatImageView minZoomPreviewImage = minZoomPreviewContainer.findViewById(R.id.tile_image);
		AppCompatImageView maxZoomPreviewImage = maxZoomPreviewContainer.findViewById(R.id.tile_image);

		minZoomPreviewImage.setImageBitmap(bitmaps.first);
		maxZoomPreviewImage.setImageBitmap(bitmaps.second);
	}

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@NonNull
	private MapActivity requireMapActivity() {
		return ((MapActivity) requireActivity());
	}

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		return activity == null ? null : ((MapActivity) activity);
	}

	public static boolean shouldShowDialog(@NonNull OsmandApplication app) {
		BaseMapLayer mainLayer = app.getOsmandMap().getMapView().getMainLayer();
		MapTileLayer mapTileLayer = mainLayer instanceof MapTileLayer ? ((MapTileLayer) mainLayer) : null;
		ITileSource tileSource = app.getSettings().getMapTileSource(false);
		return mapTileLayer != null && mapTileLayer.isVisible() && tileSource.couldBeDownloadedFromInternet();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			DownloadTilesFragment fragment = new DownloadTilesFragment();
			fragmentManager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}