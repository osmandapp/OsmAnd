package net.osmand.plus.plugins.rastermaps;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.slider.RangeSlider;

import net.osmand.IndexConstants;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.ITileSource;
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

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class DownloadTilesFragment extends BaseOsmAndFragment implements IMapLocationListener {

	public static final String TAG = DownloadTilesFragment.class.getSimpleName();

	public static final Uri HELP_URI = Uri.parse("https://docs.osmand.net/en/main@latest/osmand/map/raster-maps#download--update-tiles");

	private static final String KEY_SELECTED_MIN_ZOOM = "selected_min_zoom";
	private static final String KEY_SELECTED_MAX_ZOOM = "selected_max_zoom";

	private OsmandApplication app;
	private OsmandSettings settings;
	private boolean nightMode;

	private UpdateTilesHandler handler;

	private OsmandMapTileView mapView;
	private TilesPreviewDrawer tilesPreviewDrawer;
	private ITileSource tileSource;

	private View view;
	private View mapWindow;
	private boolean mapWindowTouched = false;
	private boolean wasDrawerDisabled;

	private AppCompatImageView minZoomPreviewImage;
	private AppCompatImageView maxZoomPreviewImage;

	private TextView minZoomPreviewText;
	private TextView maxZoomPreviewText;

	private TextView selectedMinZoomText;
	private TextView selectedMaxZoomText;

	private RangeSlider slider;

	private TextView tilesNumberText;
	private TextView estimatedDownloadSizeText;

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
		tileSource = settings.getMapTileSource(false);
		handler = new UpdateTilesHandler(() -> {
			setupTilesDownloadInfo();
			updateTilesPreview();
		});

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

		mapWindow = view.findViewById(R.id.map_window);

		View minZoomPreviewContainer = view.findViewById(R.id.min_zoom_tile_preview);
		View maxZoomPreviewContainer = view.findViewById(R.id.max_zoom_tile_preview);
		minZoomPreviewImage = minZoomPreviewContainer.findViewById(R.id.tile_image);
		maxZoomPreviewImage = maxZoomPreviewContainer.findViewById(R.id.tile_image);
		minZoomPreviewText = minZoomPreviewContainer.findViewById(R.id.tile_zoom);
		maxZoomPreviewText = maxZoomPreviewContainer.findViewById(R.id.tile_zoom);

		selectedMinZoomText = view.findViewById(R.id.min_zoom);
		selectedMaxZoomText = view.findViewById(R.id.max_zoom);

		slider = view.findViewById(R.id.zooms_range_slider);

		tilesNumberText = view.findViewById(R.id.tiles_number);
		estimatedDownloadSizeText = view.findViewById(R.id.estimated_download_size);

		mapView.rotateToAnimate(0);

		setupToolbar();
		view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				moveMapCenterToMapWindow();
				updateTileSourceContent();
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
				AndroidUtils.openUrl(context, HELP_URI, nightMode);
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

	private void updateTileSourceContent() {
		setupMapSourceSetting();
		updateDownloadContent(mapView.getZoom());
	}

	private void updateDownloadContent(int currentZoom) {
		updateTilesPreviewZooms();
		setupMinMaxZoom();
		setupSlider(currentZoom);
		setupTilesDownloadInfo();
	}

	private void setupMapSourceSetting() {
		View mapSourceContainer = view.findViewById(R.id.map_source_container);
		mapSourceContainer.setOnClickListener(v -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getMapLayers().selectMapLayer(mapActivity, false, mapSourceName -> {
					if (shouldShowDialog(app)) {
						tileSource = settings.getMapTileSource(false);
						int currentZoom = mapView.getZoom();
						selectedMaxZoom = tileSource.getMaximumZoomSupported();
						selectedMinZoom = Math.min(currentZoom, selectedMaxZoom);
						updateTileSourceContent();
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

	private void setupSlider(int currentZoom) {
		int maxZoom = tileSource.getMaximumZoomSupported();
		int minZoom = Math.min(currentZoom, maxZoom);

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
				updateTilesPreviewZooms();
				setupMinMaxZoom();
				setupTilesDownloadInfo();
			});
		}
	}

	private void updateTilesPreviewZooms() {
		minZoomPreviewText.setText(String.valueOf(selectedMinZoom));
		maxZoomPreviewText.setText(String.valueOf(selectedMaxZoom));
	}

	private void setupMinMaxZoom() {
		selectedMinZoomText.setText(String.valueOf(selectedMinZoom));
		selectedMaxZoomText.setText(String.valueOf(selectedMaxZoom));
	}

	@SuppressLint("StringFormatMatches")
	private void setupTilesDownloadInfo() {
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
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
		}
		mapView.addMapLocationListener(this);
		handler.startUpdatesIfNotRunning();
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !wasDrawerDisabled) {
			mapActivity.enableDrawer();
		}
		mapView.removeMapLocationListener(this);
		handler.stopUpdates();
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
			int maxZoom = tileSource.getMaximumZoomSupported();
			int currentZoom = mapView.getZoom();
			if (currentZoom > selectedMinZoom && currentZoom <= maxZoom) {
				selectedMinZoom = currentZoom;
			}

			updateTilesPreview();
			updateDownloadContent(currentZoom);
		});
	}

	private void updateTilesPreview() {
		QuadRect latLonRectOfMapWindow = getLatLonRectOfMapWindow();
		LatLon mapWindowCenter = new LatLon(latLonRectOfMapWindow.centerY(), latLonRectOfMapWindow.centerX());
		Pair<Bitmap, Bitmap> bitmaps = tilesPreviewDrawer.drawTilesPreview(mapWindowCenter, selectedMinZoom, selectedMaxZoom);

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

	@SuppressWarnings("deprecation")
	private static class UpdateTilesHandler extends Handler {

		private static final int UPDATE_TILES_MESSAGE_ID = 0;
		private static final long UPDATE_TILES_PREVIEW_INTERVAL = 500;

		private final Runnable updateTilesTask;

		public UpdateTilesHandler(@NonNull Runnable updateTilesTask) {
			this.updateTilesTask = updateTilesTask;
		}

		public void startUpdatesIfNotRunning() {
			if (!hasMessages(UPDATE_TILES_MESSAGE_ID)) {
				sendEmptyMessage(UPDATE_TILES_MESSAGE_ID);
			}
		}

		public void stopUpdates() {
			removeMessages(UPDATE_TILES_MESSAGE_ID);
		}

		@Override
		public void handleMessage(@NonNull Message message) {
			if (message.what == UPDATE_TILES_MESSAGE_ID) {
				updateTilesTask.run();
				sendEmptyMessageDelayed(UPDATE_TILES_MESSAGE_ID, UPDATE_TILES_PREVIEW_INTERVAL);
			}
		}
	}
}