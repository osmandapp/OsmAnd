package net.osmand.plus.routepreparationmenu.cards;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.DirectionDrawable;

import java.util.List;

public class MapMarkersCard extends MapBaseCard {
	private final List<MapMarker> markers;
	private boolean showLimited = true;
	private LatLon loc;
	private Float heading;
	private boolean useCenter;
	private final int screenOrientation;


	public MapMarkersCard(@NonNull MapActivity mapActivity, @NonNull List<MapMarker> mapMarkers) {
		super(mapActivity);
		markers = mapMarkers;

		screenOrientation = AndroidUiHelper.getScreenOrientation(mapActivity);

		MapViewTrackingUtilities trackingUtils = mapActivity.getMapViewTrackingUtilities();
		if (trackingUtils != null) {
			Float head = trackingUtils.getHeading();
			float mapRotation = mapActivity.getMapRotate();
			LatLon mw = mapActivity.getMapLocation();
			boolean mapLinked = trackingUtils.isMapLinkedToLocation();
			useCenter = !mapLinked;
			loc = mw;
			if (useCenter) {
				heading = -mapRotation;
			} else {
				heading = head;
			}
		}
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_route_card;
	}

	public List<MapMarker> getMarkers() {
		return markers;
	}

	@SuppressLint("DefaultLocale")
	@Override
	protected void updateContent() {
		LinearLayout root = view.findViewById(R.id.items);
		root.removeAllViews();

		int minCardHeight = getDimen(R.dimen.route_info_card_item_height);
		int listTextPadding = getDimen(R.dimen.route_info_list_text_padding);

		List<MapMarker> markers = getMarkers();
		int i = 0;
		boolean showLimitExceeds = markers.size() > 4;

		int mainFontColor = getMainFontColor();
		int descriptionColor = getSecondaryColor();
		int activeColor = getActiveColor();

		Context context = UiUtilities.getThemedContext(mapActivity, nightMode);
		for (MapMarker marker : markers) {
			if (showLimitExceeds && i >= 3 && showLimited) {
				break;
			}
			View v = themedInflater.inflate(R.layout.map_marker_item, root, false);
			MapMarkerDialogHelper.updateMapMarkerInfo(context, v, loc, heading, useCenter, nightMode, screenOrientation, marker);
			View remove = v.findViewById(R.id.info_close);
			remove.setVisibility(View.GONE);

			((TextView) v.findViewById(R.id.waypoint_dist)).setTextColor(activeColor);
			((TextView) v.findViewById(R.id.waypoint_text)).setTextColor(mainFontColor);
			((TextView) v.findViewById(R.id.date_group_text)).setTextColor(descriptionColor);

			ImageView arrow = v.findViewById(R.id.direction);
			Drawable arrowIcon = arrow.getDrawable();
			if (arrowIcon instanceof DirectionDrawable) {
				((DirectionDrawable) arrowIcon).setImage(R.drawable.ic_direction_arrow, ColorUtilities.getActiveColorId(nightMode));
			}

			v.setBackgroundResource(AndroidUtils.resolveAttribute(context, android.R.attr.selectableItemBackground));
			v.setMinimumHeight(minCardHeight);
			v.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					LatLon point = new LatLon(marker.getLatitude(), marker.getLongitude());
					app.getTargetPointsHelper().navigateToPoint(point, true, -1, marker.getPointDescription(mapActivity));
					app.getRoutingHelper().onSettingsChanged(true);
				}
			});
			if (i > 0) {
				View div = new View(context);
				LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dpToPx(context, 1f));
				AndroidUtils.setMargins(p, listTextPadding, 0, 0, 0);
				div.setLayoutParams(p);
				AndroidUtils.setBackgroundColor(context, div, ColorUtilities.getDividerColorId(nightMode));
				div.setVisibility(View.VISIBLE);
				root.addView(div);
			}
			root.addView(v);
			i++;
		}

		View showAllButton = view.findViewById(R.id.show_all_button);
		if (showLimited && showLimitExceeds) {
			((TextView) view.findViewById(R.id.show_all_title)).setText(
					String.format("%s — %d", app.getString(R.string.shared_string_show_all).toUpperCase(), markers.size()));
			showAllButton.setVisibility(View.VISIBLE);
			showAllButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showLimited = false;
					updateContent();
					setLayoutNeeded();
				}
			});
		} else {
			showAllButton.setVisibility(View.GONE);
		}

		((TextView) view.findViewById(R.id.gpx_card_title)).setText(R.string.map_markers);
	}

	@Override
	public void applyState(@NonNull BaseCard card) {
		super.applyState(card);
		if (card instanceof MapMarkersCard) {
			showLimited = ((MapMarkersCard) card).showLimited;
		}
	}
}