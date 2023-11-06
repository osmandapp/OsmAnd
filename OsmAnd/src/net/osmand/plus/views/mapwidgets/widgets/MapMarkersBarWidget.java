package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.views.mapwidgets.WidgetType.MARKERS_TOP_BAR;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.mapmarkers.MapMarkersDialogFragment;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.DirectionDrawable;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MarkersWidgetsHelper;
import net.osmand.plus.views.mapwidgets.MarkersWidgetsHelper.CustomLatLonListener;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.WidgetsVisibilityHelper;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.List;

public class MapMarkersBarWidget extends MapWidget implements CustomLatLonListener {

	public static final int MIN_METERS_OK_VISIBLE = 40;
	public static final int MIN_METERS_2ND_ROW_SHOW = 150;

	private final MapMarkersHelper markersHelper;
	private final boolean portraitMode;
	private final String customId;

	private final View markerContainer2nd;
	private final ImageView arrowImg;
	private final ImageView arrowImg2nd;
	private final TextView distText;
	private final TextView distText2nd;
	private final TextView addressText;
	private final TextView addressText2nd;
	private final ImageButton okButton;
	private final ImageButton okButton2nd;

	private LatLon customLatLon;

	@Override
	protected int getLayoutId() {
		return R.layout.map_markers_widget;
	}

	public MapMarkersBarWidget(@NonNull MapActivity mapActivity, String customId) {
		super(mapActivity, MARKERS_TOP_BAR);
		this.customId = customId;
		markersHelper = app.getMapMarkersHelper();
		portraitMode = AndroidUiHelper.isOrientationPortrait(mapActivity);

		markerContainer2nd = view.findViewById(R.id.map_markers_top_bar_2nd);
		arrowImg = view.findViewById(R.id.map_marker_arrow);
		arrowImg2nd = view.findViewById(R.id.map_marker_arrow_2nd);
		distText = view.findViewById(R.id.map_marker_dist);
		distText2nd = view.findViewById(R.id.map_marker_dist_2nd);
		addressText = view.findViewById(R.id.map_marker_address);
		addressText2nd = view.findViewById(R.id.map_marker_address_2nd);
		okButton = view.findViewById(R.id.marker_btn_ok);
		okButton2nd = view.findViewById(R.id.marker_btn_ok_2nd);

		setupMarkersClickListeners();
		setupMoreButtons();
		setupOkButtons();

		updateVisibility(false);
	}

	private void setupMarkersClickListeners() {
		View rowView = view.findViewById(R.id.map_marker_row);
		View rowView2nd = view.findViewById(R.id.map_marker_row_2nd);
		rowView.setOnClickListener(v -> MarkersWidgetsHelper.showMarkerOnMap(mapActivity, 0));
		rowView2nd.setOnClickListener(v -> MarkersWidgetsHelper.showMarkerOnMap(mapActivity, 1));
	}

	private void setupMoreButtons() {
		ImageButton moreButton = view.findViewById(R.id.marker_btn_more);
		ImageButton moreButton2nd = view.findViewById(R.id.marker_btn_more_2nd);

		boolean twoMarkersForWidget = settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 2;
		if (!portraitMode && markersHelper.getMapMarkers().size() > 1 && twoMarkersForWidget) {
			AndroidUiHelper.updateVisibility(moreButton, false);
		} else {
			moreButton.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_markers_list, R.color.marker_top_2nd_line_color));
			moreButton.setOnClickListener(v -> showMarkersFragment());
		}
		if (moreButton2nd != null) {
			moreButton2nd.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_markers_list, R.color.marker_top_2nd_line_color));
			moreButton2nd.setOnClickListener(v -> showMarkersFragment());
		}
	}

	private void showMarkersFragment() {
		MapActivity.clearPrevActivityIntent();
		MapMarkersDialogFragment.showInstance(mapActivity);
	}

	private void setupOkButtons() {
		okButton.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_marker_passed, R.color.card_and_list_background_light));
		okButton.setOnClickListener(v -> removeMarker(0));
		okButton2nd.setImageDrawable(iconsCache.getIcon(R.drawable.ic_action_marker_passed, R.color.card_and_list_background_light));
		okButton2nd.setOnClickListener(v -> removeMarker(1));
	}

	private void removeMarker(int index) {
		if (markersHelper.getMapMarkers().size() > index) {
			markersHelper.moveMapMarkerToHistory(markersHelper.getMapMarkers().get(index));
		}
	}

	@Override
	public void setCustomLatLon(@Nullable LatLon customLatLon) {
		this.customLatLon = customLatLon;
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		List<MapMarker> markers = markersHelper.getMapMarkers();
		int zoom = mapActivity.getMapView().getZoom();
		WidgetsVisibilityHelper widgetsVisibilityHelper = mapActivity.getWidgetsVisibilityHelper();
		if (markers.size() == 0 || zoom < 3 || widgetsVisibilityHelper.shouldHideMapMarkersWidget()) {
			updateVisibility(false);
			return;
		}

		showMarkers(markers);
	}

	public void showMarkers(@NonNull List<MapMarker> markers) {
		LatLon latLon = customLatLon != null ? customLatLon : app.getMapViewTrackingUtilities().getDefaultLocation();
		boolean defaultLatLon = customLatLon == null;
		Float heading = mapActivity.getMapViewTrackingUtilities().getHeading();

		updateFirstMarker(latLon, markers.get(0), heading, !defaultLatLon);
		updateSecondMarker(latLon, markers, heading, !defaultLatLon);
		updateVisibility(true);
	}

	public void updateFirstMarker(@NonNull LatLon center, @NonNull MapMarker marker,
	                              @Nullable Float heading, boolean customLocation) {
		updateMarker(center, heading, marker, arrowImg, distText, okButton, addressText, true, customLocation);
	}

	public void updateSecondMarker(@NonNull LatLon center, @NonNull List<MapMarker> markers,
	                               @Nullable Float heading, boolean customLocation) {
		if (markers.size() > 1 && settings.DISPLAYED_MARKERS_WIDGETS_COUNT.get() == 2) {
			MapMarker secondMarker = markers.get(1);
			if (!customLocation) {
				for (int i = 1; i < markers.size(); i++) {
					MapMarker m = markers.get(i);
					m.dist = (int) (MapUtils.getDistance(center, m.getLatitude(), m.getLongitude()));
					if (m.dist < MIN_METERS_2ND_ROW_SHOW && secondMarker.dist > m.dist) {
						secondMarker = m;
					}
				}
			}
			updateMarker(center, heading, secondMarker, arrowImg2nd, distText2nd, okButton2nd, addressText2nd,
					false, customLocation);
			AndroidUiHelper.updateVisibility(markerContainer2nd, true);
		} else {
			AndroidUiHelper.updateVisibility(markerContainer2nd, false);
		}
	}

	private void updateMarker(@NonNull LatLon latlon, @Nullable Float heading, @NonNull MapMarker marker,
	                          @NonNull ImageView arrowImg, @NonNull TextView distText,
	                          @NonNull ImageButton okButton, @NonNull TextView addressText,
	                          boolean firstMarker, boolean customLocation) {
		float[] distInfo = new float[2];
		if (marker.point != null) {
			Location.distanceBetween(marker.getLatitude(), marker.getLongitude(),
					latlon.getLatitude(), latlon.getLongitude(), distInfo);
		}

		if (customLocation) {
			heading = 0f;
		}

		boolean newImage = false;
		DirectionDrawable dd;
		if (arrowImg.getDrawable() instanceof DirectionDrawable) {
			dd = (DirectionDrawable) arrowImg.getDrawable();
		} else {
			newImage = true;
			dd = new DirectionDrawable(app, arrowImg.getWidth(), arrowImg.getHeight());
		}
		dd.setImage(R.drawable.ic_arrow_marker_diretion, MapMarker.getColorId(marker.colorIndex));
		if (heading != null) {
			dd.setAngle(distInfo[1] - heading + 180);
		}
		if (newImage) {
			arrowImg.setImageDrawable(dd);
		}
		arrowImg.invalidate();

		int dist = (int) distInfo[0];
		String formattedDist = OsmAndFormatter.getFormattedDistance(dist, app);
		distText.setText(formattedDist);
		AndroidUiHelper.updateVisibility(okButton, !customLocation && dist < MIN_METERS_OK_VISIBLE);

		PointDescription pd = marker.getPointDescription(mapActivity);
		String descr = Algorithms.isEmpty(pd.getName()) ? pd.getTypeName() : pd.getName();
		if (!firstMarker && portraitMode) {
			descr = "  •  " + descr;
		}

		addressText.setText(descr);
	}


	@Override
	protected boolean updateVisibility(boolean visible) {
		boolean updatedVisibility = super.updateVisibility(visible);
		if (updatedVisibility && widgetType.getPanel(settings) == WidgetsPanel.TOP) {
			mapActivity.updateStatusBarColor();
		}
		return updatedVisibility;
	}

	@Override
	public void attachView(@NonNull ViewGroup container, @NonNull WidgetsPanel panel,
	                       @NonNull List<MapWidget> followingWidgets) {
		super.attachView(container, panel, followingWidgets);
		View bottomShadow = view.findViewById(R.id.bottom_shadow);
		AndroidUiHelper.updateVisibility(bottomShadow, followingWidgets.isEmpty());
	}
}