package net.osmand.plus.plugins.rastermaps;

import static net.osmand.plus.plugins.rastermaps.DownloadTilesHelper.getApproxTilesSizeMb;
import static net.osmand.plus.plugins.rastermaps.DownloadTilesHelper.getTilesNumber;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.RangeSlider;

import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.ITileSource;
import net.osmand.plus.LockableScrollView;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.rastermaps.CalculateMissingTilesTask.MissingTilesInfo;
import net.osmand.plus.plugins.rastermaps.DownloadTilesHelper.DownloadType;
import net.osmand.plus.resources.BitmapTilesCache;
import net.osmand.plus.resources.SQLiteTileSource;
import net.osmand.plus.settings.enums.MapLayerType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView.TouchListener;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapTileLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.widgets.dialogbutton.DialogButton;

import java.text.MessageFormat;
import java.util.List;

public class DownloadTilesFragment extends BaseOsmAndFragment implements IMapLocationListener {

	public static final String TAG = DownloadTilesFragment.class.getSimpleName();

	public static final String KEY_DOWNLOAD_TYPE = "download_type";
	public static final String KEY_DOWNLOAD_LAYER = "download_layer";
	private static final String KEY_SELECTED_MIN_ZOOM = "selected_min_zoom";
	private static final String KEY_SELECTED_MAX_ZOOM = "selected_max_zoom";

	private DownloadTilesHelper downloadTilesHelper;

	private UpdateTilesHandler handler;

	private OsmandMapTileView mapView;
	private TilesPreviewDrawer tilesPreviewDrawer;
	private ITileSource tileSource;

	private View view;
	private View mapWindow;
	private boolean mapWindowTouched;
	private boolean wasDrawerDisabled;

	private TextView tvDownloadTilesDesc;

	private AppCompatImageView minZoomPreviewImage;
	private AppCompatImageView maxZoomPreviewImage;

	private TextView minZoomPreviewText;
	private TextView maxZoomPreviewText;

	private TextView selectedMinZoomText;
	private TextView selectedMaxZoomText;

	private RangeSlider slider;

	private TextView tilesNumberText;
	private TextView estimatedDownloadSizeText;

	private DialogButton downloadButton;

	private int selectedMinZoom;
	private int selectedMaxZoom;

	private DownloadType downloadType;
	private MissingTilesInfo missingTilesInfo;
	private CalculateMissingTilesTask calculateMissingTilesTask;

	private SelectTilesDownloadTypeAlertDialog alertDialog;
	private MapLayerType layerToDownload;

	private LockableScrollView scrollView;
	private IMapLocationListener mapLocationListener;
	private TouchListener touchListener;
	private double lat;
	private double lon;
	private boolean portraitMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			layerToDownload = AndroidUtils.getSerializable(args, KEY_DOWNLOAD_LAYER, MapLayerType.class);
		}
		downloadTilesHelper = app.getDownloadTilesHelper();
		mapView = requireMapActivity().getMapView();
		tilesPreviewDrawer = new TilesPreviewDrawer(app);
		tileSource = loadTileSource();
		handler = new UpdateTilesHandler(() -> {
			setupTilesDownloadInfo();
			updateTilesPreview();
		});

		if (savedInstanceState != null) {
			selectedMinZoom = savedInstanceState.getInt(KEY_SELECTED_MIN_ZOOM);
			selectedMaxZoom = savedInstanceState.getInt(KEY_SELECTED_MAX_ZOOM);
			downloadType = AndroidUtils.getSerializable(savedInstanceState, KEY_DOWNLOAD_TYPE, DownloadType.class);
		} else {
			selectedMaxZoom = tileSource.getMaximumZoomSupported();
			selectedMinZoom = Math.min(mapView.getZoom(), selectedMaxZoom);
			if (args != null) {
				downloadType = AndroidUtils.getSerializable(args, KEY_DOWNLOAD_TYPE, DownloadType.class);
			}
		}
	}

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		portraitMode = AndroidUiHelper.isOrientationPortrait(requireMapActivity());
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
		mapView.setElevationAngle(OsmandMapTileView.DEFAULT_ELEVATION_ANGLE);

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
		if (portraitMode) {
			setupScrollableMapView();
		}

		return view;
	}

	@SuppressLint("ClickableViewAccessibility")
	private void setupScrollableMapView() {
		scrollView = view.findViewById(R.id.scroll_view);
		mapLocationListener = getMapLocationListener();
		touchListener = getTouchListener();

		view.findViewById(R.id.map_window_container).setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN){
				scrollView.setScrollingEnabled(false);
			}
			return false;
		});
		scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
			OsmandMapTileView mapView = DownloadTilesFragment.this.mapView;
			if (mapView != null) {
				RotatedTileBox tileBox = mapView.getCurrentRotatedTileBox();
				View mapWindow = view.findViewById(R.id.map_window);
				int[] xy = new int[2];
				mapWindow.getLocationOnScreen(xy);
				int marginTop = xy[1];
				mapView.fitLocationToMap(lat, lon, tileBox.getZoom(), mapWindow.getWidth(), mapWindow.getHeight(),
						marginTop, false);
			}
		});
	}

	private IMapLocationListener getMapLocationListener(){
		return (v, v1, o) -> {
			QuadRect rect = getLatLonRectOfMapWindow();
			LatLon mapWindowCenter = new LatLon(rect.centerY(), rect.centerX());
			lat = mapWindowCenter.getLatitude();
			lon = mapWindowCenter.getLongitude();
		};
	}

	private TouchListener getTouchListener(){
		return event -> {
			if (event.getAction() == MotionEvent.ACTION_UP){
				scrollView.setScrollingEnabled(true);
			}
		};
	}

	private void setupToolbar() {
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		View toolbar = view.findViewById(R.id.toolbar);

		ImageButton backButton = toolbar.findViewById(R.id.back_button);
		backButton.setOnClickListener(v -> dismiss());
		UiUtilities.rotateImageByLayoutDirection(backButton);

		TextView tvTitle = toolbar.findViewById(R.id.title);
		int titleId = downloadType != DownloadType.ALL
				? R.string.context_menu_item_update_map
				: R.string.shared_string_download_map;
		tvTitle.setText(titleId);

		View helpButton = toolbar.findViewById(R.id.help_button);
		helpButton.setOnClickListener(v -> {
			Context context = getContext();
			if (context != null) {
				AndroidUtils.openUrl(context, R.string.docs_map_download_tiles, nightMode);
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
		if (downloadType != DownloadType.ALL) {
			setupTilesToDownloadSetting();
		}
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
				mapActivity.getMapLayers().selectMapLayer(mapActivity, false,
						layerToDownload.getMapLayerSettings(app), mapSourceName -> {
							if (shouldShowDialog(app)) {
								tileSource = loadTileSource();
								int currentZoom = mapView.getZoom();
								selectedMaxZoom = tileSource.getMaximumZoomSupported();
								selectedMinZoom = Math.min(currentZoom, selectedMaxZoom);
								if (downloadType == DownloadType.ONLY_MISSING) {
									missingTilesInfo = null;
									calculateMissingTiles();
								}
								updateTileSourceContent();
							} else {
								app.showToastMessage(R.string.maps_could_not_be_downloaded);
								dismiss();
							}
							return true;
						});
			}
		});

		ImageView ivIcon = mapSourceContainer.findViewById(R.id.icon);
		TextView tvMapSource = mapSourceContainer.findViewById(R.id.title);
		TextView tvSelectedMapSource = mapSourceContainer.findViewById(R.id.desc);

		tvMapSource.setText(layerToDownload.getNameId());
		ivIcon.setImageResource(layerToDownload.getIconId());
		tvSelectedMapSource.setText(settings.getTileSourceTitle(tileSource, tileSource.getName()));
	}

	private void setupTilesToDownloadSetting() {
		View container = view.findViewById(R.id.tiles_to_download_container);
		container.setOnClickListener(v -> {
			Activity activity = getActivity();
			if (activity == null) {
				return;
			}

			alertDialog = new SelectTilesDownloadTypeAlertDialog(app, nightMode,
					downloadType == DownloadType.FORCE_ALL, selectedDownloadType -> {
				downloadType = selectedDownloadType;
				updateTileSourceContent();
				enableDisableDownloadButton();
				alertDialog = null;
			});
			if (missingTilesInfo == null) {
				alertDialog.updateData(getAllTilesCount(), -1, false);
			} else {
				alertDialog.updateData(getAllTilesCount(), missingTilesInfo.getMissingTiles(), missingTilesInfo.isApproximate());
			}
			alertDialog.show(activity);
		});
		AndroidUiHelper.updateVisibility(container, true);

		ImageView ivIcon = container.findViewById(R.id.icon);
		TextView tvDownloadTiles = container.findViewById(R.id.title);
		tvDownloadTilesDesc = container.findViewById(R.id.desc);

		ivIcon.setImageResource(R.drawable.ic_action_map_update);
		tvDownloadTiles.setText(R.string.download_tiles);
		updateDownloadTilesDesc();
	}

	private void updateDownloadTilesDesc() {
		String desc;
		if (downloadType == DownloadType.ONLY_MISSING) {
			String onlyMissingStr = getString(R.string.shared_string_only_missing);
			if (missingTilesInfo == null) {
				desc = onlyMissingStr;
			} else {
				String formattedTilesCount = formatNumber(missingTilesInfo.getMissingTiles(),
						0, false);
				if (missingTilesInfo.isApproximate()) {
					formattedTilesCount = MessageFormat.format("~{0}", formattedTilesCount);
				}
				desc = getString(R.string.ltr_or_rtl_combine_via_comma, onlyMissingStr, formattedTilesCount);
			}
		} else {
			String allStr = getString(R.string.shared_string_all);
			long allTilesCount = getAllTilesCount();
			String formattedTilesCount = formatNumber(allTilesCount, 0, false);
			desc = getString(R.string.ltr_or_rtl_combine_via_comma, allStr, formattedTilesCount);
		}
		tvDownloadTilesDesc.setText(desc);
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
				if (downloadType == DownloadType.ONLY_MISSING) {
					calculateMissingTiles();
				}
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
		if (downloadType == DownloadType.ONLY_MISSING && missingTilesInfo == null) {
			tilesNumberText.setText("—");
			estimatedDownloadSizeText.setText("—");
		} else {
			boolean approximate;
			long tilesNumber;
			float estimatedDownloadSizeMB;

			if (downloadType == DownloadType.ONLY_MISSING) {
				approximate = missingTilesInfo.isApproximate();
				tilesNumber = missingTilesInfo.getMissingTiles();
				estimatedDownloadSizeMB = missingTilesInfo.getApproxMissingSizeMb();
			} else {
				BitmapTilesCache bitmapTilesCache = app.getResourceManager().getBitmapTilesCache();
				QuadRect latLonRect = getLatLonRectOfMapWindow();

				approximate = false;
				tilesNumber = getAllTilesCount();
				estimatedDownloadSizeMB = getApproxTilesSizeMb(selectedMinZoom, selectedMaxZoom, latLonRect,
						tileSource, bitmapTilesCache);
			}

			String formattedSize = formatNumber(estimatedDownloadSizeMB, 2, approximate || tilesNumber > 0);
			String sizeWithUnit = getString(R.string.shared_string_memory_mb_desc, formattedSize);

			tilesNumberText.setText(formatNumber(tilesNumber, 0, approximate));
			estimatedDownloadSizeText.setText(sizeWithUnit);
		}
	}

	@NonNull
	private String formatNumber(float number, int decimalPlacesNumber, boolean approximate) {
		String formatted = OsmAndFormatter.formatValue(number, "", false, decimalPlacesNumber, app).value;
		return approximate
				? getString(R.string.ltr_or_rtl_combine_via_space, "~", formatted)
				: formatted;
	}

	private void setupDownloadButton() {
		downloadButton = view.findViewById(R.id.dismiss_button);
		downloadButton.setButtonType(DialogButtonType.PRIMARY);
		downloadButton.setTitleId(R.string.shared_string_download);
		downloadButton.setOnClickListener(v -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				showDownloadFragment(mapActivity);
			}
		});
		enableDisableDownloadButton();
	}

	private void enableDisableDownloadButton() {
		boolean hasMissingTiles = missingTilesInfo == null
				|| missingTilesInfo.isApproximate()
				|| missingTilesInfo.getMissingTiles() > 0;
		downloadButton.setEnabled(downloadType != DownloadType.ONLY_MISSING || hasMissingTiles);
	}

	private void showDownloadFragment(@NonNull MapActivity mapActivity) {
		FragmentManager fragmentManager = mapActivity.getSupportFragmentManager();

		QuadRect latLonRect = getLatLonRectOfMapWindow();
		Bundle args = new Bundle();
		args.putDouble(TilesDownloadProgressFragment.KEY_LEFT_LON, latLonRect.left);
		args.putDouble(TilesDownloadProgressFragment.KEY_TOP_LAT, latLonRect.top);
		args.putDouble(TilesDownloadProgressFragment.KEY_RIGHT_LON, latLonRect.right);
		args.putDouble(TilesDownloadProgressFragment.KEY_BOTTOM_LAT, latLonRect.bottom);
		args.putInt(TilesDownloadProgressFragment.KEY_MIN_ZOOM, selectedMinZoom);
		args.putInt(TilesDownloadProgressFragment.KEY_MAX_ZOOM, selectedMaxZoom);
		args.putSerializable(KEY_DOWNLOAD_TYPE, downloadType);
		args.putSerializable(KEY_DOWNLOAD_LAYER, layerToDownload);
		if (downloadType == DownloadType.ONLY_MISSING && missingTilesInfo != null) {
			args.putLong(TilesDownloadProgressFragment.KEY_MISSING_TILES, missingTilesInfo.getMissingTiles());
			args.putFloat(TilesDownloadProgressFragment.KEY_MISSING_SIZE_MB, missingTilesInfo.getApproxMissingSizeMb());
		}

		TilesDownloadProgressFragment.showInstance(fragmentManager, args);
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
		showHideMapControls(false);
		if (portraitMode) {
			mapView.addMapLocationListener(mapLocationListener);
			mapView.addTouchListener(touchListener);
		}
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

		if (downloadType != DownloadType.ALL) {
			downloadTilesHelper.setListener(null);
		}
		if (portraitMode) {
			mapView.removeMapLocationListener(mapLocationListener);
			mapView.removeTouchListener(touchListener);
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(KEY_SELECTED_MIN_ZOOM, selectedMinZoom);
		outState.putInt(KEY_SELECTED_MAX_ZOOM, selectedMaxZoom);
		outState.putSerializable(KEY_DOWNLOAD_TYPE, downloadType);
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
			boolean activityDestroyed = !AndroidUtils.isActivityNotDestroyed(getActivity());
			if (activityDestroyed) {
				return;
			}

			int maxZoom = tileSource.getMaximumZoomSupported();
			int currentZoom = mapView.getZoom();
			if (currentZoom > selectedMinZoom && currentZoom <= maxZoom) {
				selectedMinZoom = currentZoom;
			}
			if (currentZoom > selectedMaxZoom && currentZoom <= maxZoom) {
				selectedMaxZoom = currentZoom;
			}

			if (downloadType != DownloadType.ALL) {
				updateDownloadTilesDesc();
				if (downloadType == DownloadType.ONLY_MISSING) {
					calculateMissingTiles();
				}
			}
			updateTilesPreview();
			updateDownloadContent(currentZoom);
		});
	}

	private void updateTilesPreview() {
		QuadRect rect = getLatLonRectOfMapWindow();
		LatLon mapWindowCenter = new LatLon(rect.centerY(), rect.centerX());
		Pair<Bitmap, Bitmap> bitmaps = tilesPreviewDrawer.drawTilesPreview(tileSource, mapWindowCenter, selectedMinZoom, selectedMaxZoom);

		minZoomPreviewImage.setImageBitmap(bitmaps.first);
		maxZoomPreviewImage.setImageBitmap(bitmaps.second);
	}

	private void calculateMissingTiles() {
		if (calculateMissingTilesTask != null) {
			calculateMissingTilesTask.cancel();
		}
		calculateMissingTilesTask = new CalculateMissingTilesTask(app, tileSource, selectedMinZoom,
				selectedMaxZoom, getLatLonRectOfMapWindow(), (missingTilesInfo) -> {
			if (!AndroidUtils.isActivityNotDestroyed(getActivity())) {
				return true;
			}

			this.missingTilesInfo = missingTilesInfo;
			updateDownloadTilesDesc();
			enableDisableDownloadButton();
			if (alertDialog != null) {
				alertDialog.updateData(getAllTilesCount(), missingTilesInfo.getMissingTiles(), missingTilesInfo.isApproximate());
			}
			return true;
		});
		calculateMissingTilesTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private long getAllTilesCount() {
		return getTilesNumber(selectedMinZoom, selectedMaxZoom, getLatLonRectOfMapWindow(), tileSource.isEllipticYTile());
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

	private ITileSource loadTileSource() {
		ITileSource tileSource = settings.getLayerTileSource(layerToDownload.getMapLayerSettings(app), false);
		if (tileSource instanceof SQLiteTileSource) {
			((SQLiteTileSource) tileSource).initDatabaseIfNeeded();
		}
		return tileSource;
	}

	public static boolean shouldShowDialog(@NonNull OsmandApplication app) {
		List<OsmandMapLayer> layers = app.getOsmandMap().getMapView().getLayers();
		for (OsmandMapLayer layer : layers) {
			if (layer instanceof MapTileLayer) {
				MapTileLayer mapTileLayer = (MapTileLayer) layer;
				if (mapTileLayer.isVisible() && mapTileLayer.getMap() != null && mapTileLayer.getMap().couldBeDownloadedFromInternet()) {
					return true;
				}
			}
		}
		return false;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, boolean updateTiles, @NonNull MapLayerType layerType) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			DownloadType downloadType = updateTiles ? DownloadType.ONLY_MISSING : DownloadType.ALL;
			args.putSerializable(KEY_DOWNLOAD_TYPE, downloadType);
			args.putSerializable(KEY_DOWNLOAD_LAYER, layerType);

			DownloadTilesFragment fragment = new DownloadTilesFragment();
			fragment.setArguments(args);
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