package net.osmand.plus.plugins.rastermaps;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.IMapLocationListener;
import net.osmand.map.ITileSource;
import net.osmand.plus.OsmAndConstants;
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
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.util.MapUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class DownloadTilesFragment extends BaseOsmAndFragment implements IMapLocationListener {

	public static final String TAG = DownloadTilesFragment.class.getSimpleName();

	private static final Uri HELP_URI = Uri.parse("https://docs.osmand.net/en/main@latest/osmand/map/raster-maps#download--update-tiles");

	private static final int UPDATE_CONTENT_MESSAGE = OsmAndConstants.UI_HANDLER_MAP_VIEW + 7;

	private OsmandApplication app;
	private OsmandSettings settings;
	private boolean nightMode;

	private OsmandMapTileView mapView;

	private boolean mapWindowTouched = false;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = requireSettings();
		nightMode = isNightMode(true);
		mapView = requireMapActivity().getMapView();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MapActivity mapActivity = requireMapActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(mapActivity, nightMode);
		View view = themedInflater.inflate(R.layout.download_tiles_fragment, container, false);

		mapView.addMapLocationListener(this);
		mapView.rotateToAnimate(0);

		setupToolbar(view);
		view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				moveMapCenterToMapWindow(view);
				updateContent(view);
			}
		});
		setupDownloadButton(view);
		showHideMapControls(false);
		restrictMapMovableArea(view);

		return view;
	}

	private void setupToolbar(@NonNull View view) {
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

	private void moveMapCenterToMapWindow(@NonNull View view) {
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

	private void updateContent(@NonNull View view) {
		ITileSource tileSource = settings.getMapTileSource(false);
		int currentZoom = mapView.getZoom();
		int maxZoom = tileSource.getMaximumZoomSupported();
		int minZoom = Math.min(currentZoom, maxZoom);

		setupMapSourceSetting(view, tileSource);
		setupTilesPreview(view);
		setupMinMaxZoom(view, minZoom, maxZoom);
		setupSlider(view, tileSource);
		setupTilesDownloadInfo(view, tileSource, minZoom, maxZoom);
	}

	private void setupMapSourceSetting(@NonNull View view, @NonNull ITileSource tileSource) {
		View mapSourceContainer = view.findViewById(R.id.map_source_container);
		mapSourceContainer.setOnClickListener(v -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.getMapLayers().selectMapLayer(mapActivity, false, mapSourceName -> {
					updateContent(view);
					return true;
				});
			}
		});

		TextView selectedMapSourceText = view.findViewById(R.id.selected_map_source);
		String selectedMapSource = tileSource.getName()
				.replace(IndexConstants.SQLITE_EXT, "");
		selectedMapSourceText.setText(selectedMapSource);
	}

	private void setupSlider(@NonNull View view, @NonNull ITileSource tileSource) {
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
			slider.setValues(((float) minZoom), ((float) maxZoom));
			slider.addOnChangeListener((slider1, value, fromUser) -> {
				List<Float> minMax = slider.getValues();
				int updatedMinZoom = minMax.get(0).intValue();
				int updatedMaxZoom = minMax.get(1).intValue();
				setupTilesPreview(view);
				setupMinMaxZoom(view, updatedMinZoom, updatedMaxZoom);
				setupTilesDownloadInfo(view, tileSource, updatedMinZoom, updatedMaxZoom);
			});
		}
	}

	private void setupTilesPreview(@NonNull View view) {
		View minZoomPreviewContainer = view.findViewById(R.id.min_zoom_tile_preview);
		View maxZoomPreviewContainer = view.findViewById(R.id.max_zoom_tile_preview);
		// todo
	}

	private void setupMinMaxZoom(@NonNull View view, int minZoom, int maxZoom) {
		TextView minZoomText = view.findViewById(R.id.min_zoom);
		TextView maxZoomText = view.findViewById(R.id.max_zoom);

		minZoomText.setText(String.valueOf(minZoom));
		maxZoomText.setText(String.valueOf(maxZoom));
	}

	private void setupTilesDownloadInfo(@NonNull View view, @NonNull ITileSource tileSource, int minZoom, int maxZoom) {
		TextView tilesNumberText = view.findViewById(R.id.tiles_number);
		TextView estimatedDownloadSizeText = view.findViewById(R.id.estimated_download_size);

		QuadRect latLonRect = getLatLonRectOfMapWindow(view);

		int tilesNumber = getTilesNumber(minZoom, maxZoom, latLonRect, tileSource.isEllipticYTile());
		float estimatedDownloadSizeMB = (float) tilesNumber * 12 / 1000;

		String formattedTilesNumber = OsmAndFormatter.formatIntegerValue(tilesNumber, "", app).value;
		String formattedSizeWithUnit = OsmAndFormatter.formatValue(estimatedDownloadSizeMB,
				R.string.shared_sting_megabyte, false, 2, app).format(app);
		String formattedDownloadSize = app.getString(R.string.ltr_or_rtl_combine_via_space, "~",
				formattedSizeWithUnit);

		tilesNumberText.setText(formattedTilesNumber);
		estimatedDownloadSizeText.setText(formattedDownloadSize);
	}

	private int getTilesNumber(int minZoom, int maxZoom, @NonNull QuadRect latLonRect, boolean ellipticYTile) {
		int tilesNumber = 0;
		for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
			int leftTileX = (int) MapUtils.getTileNumberX(zoom, latLonRect.left);
			int rightTileX = (int) MapUtils.getTileNumberX(zoom, latLonRect.right);
			int topTileY;
			int bottomTileY;
			if (ellipticYTile) {
				topTileY = (int) MapUtils.getTileEllipsoidNumberY(zoom, latLonRect.top);
				bottomTileY = (int) MapUtils.getTileEllipsoidNumberY(zoom, latLonRect.bottom);
			} else {
				topTileY = (int) MapUtils.getTileNumberY(zoom, latLonRect.top);
				bottomTileY = (int) MapUtils.getTileNumberY(zoom, latLonRect.bottom);
			}
			tilesNumber += (rightTileX - leftTileX + 1) * (bottomTileY - topTileY + 1);
		}
		return tilesNumber;
	}

	private void setupDownloadButton(@NonNull View view) {
		View downloadButton = view.findViewById(R.id.dismiss_button);
		UiUtilities.setupDialogButton(nightMode, downloadButton, DialogButtonType.PRIMARY,
				R.string.shared_string_download);
		downloadButton.setOnClickListener(v -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				// todo: show download dialog
			}
		});
	}

	@SuppressLint("ClickableViewAccessibility")
	private void restrictMapMovableArea(@NonNull View view) {
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
	public void onDestroyView() {
		super.onDestroyView();
		mapView.removeMapLocationListener(this);
		showHideMapControls(true);
		returnMapCenterToInitialPosition();
	}

	private void returnMapCenterToInitialPosition() {
		// todo
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
	private QuadRect getLatLonRectOfMapWindow(@NonNull View view) {
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
		app.runMessageInUIThreadAndCancelPrevious(UPDATE_CONTENT_MESSAGE, () -> {
			View view = getView();
			if (view != null) {
				updateContent(view);
			}
		}, 100);
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