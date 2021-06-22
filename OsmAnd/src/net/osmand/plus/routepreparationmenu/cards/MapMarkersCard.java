package net.osmand.plus.routepreparationmenu.cards;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.plus.mapmarkers.MapMarker;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.MapMarkerDialogHelper;
import net.osmand.plus.views.DirectionDrawable;

import java.util.List;

public class MapMarkersCard extends MapBaseCard {
	private List<MapMarker> markers;
	private boolean showLimited = true;
	private LatLon loc;
	private Float heading;
	private boolean useCenter;
	private int screenOrientation;


	public MapMarkersCard(@NonNull MapActivity mapActivity, @NonNull List<MapMarker> mapMarkers) {
		super(mapActivity);
		markers = mapMarkers;

		screenOrientation = app.getUIUtilities().getScreenOrientation();

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
		LinearLayout root = (LinearLayout) view.findViewById(R.id.items);
		root.removeAllViews();

		int minCardHeight = app.getResources().getDimensionPixelSize(R.dimen.route_info_card_item_height);
		int listTextPadding = app.getResources().getDimensionPixelSize(R.dimen.route_info_list_text_padding);

		List<MapMarker> markers = getMarkers();
		int i = 0;
		boolean showLimitExceeds = markers.size() > 4;

		int mainFontColor = getMainFontColor();
		int descriptionColor = getSecondaryColor();
		int activeColor = getActiveColor();

		ContextThemeWrapper ctx = new ContextThemeWrapper(mapActivity, !nightMode ? R.style.OsmandLightTheme : R.style.OsmandDarkTheme);
		LayoutInflater inflater = LayoutInflater.from(ctx);
		for (final MapMarker marker : markers) {
			if (showLimitExceeds && i >= 3 && showLimited) {
				break;
			}
			View v = inflater.inflate(R.layout.map_marker_item, root, false);
			MapMarkerDialogHelper.updateMapMarkerInfo(ctx, v, loc, heading, useCenter, nightMode, screenOrientation, marker);
			final View remove = v.findViewById(R.id.info_close);
			remove.setVisibility(View.GONE);

			((TextView) v.findViewById(R.id.waypoint_dist)).setTextColor(activeColor);
			((TextView) v.findViewById(R.id.waypoint_text)).setTextColor(mainFontColor);
			((TextView) v.findViewById(R.id.date_group_text)).setTextColor(descriptionColor);

			ImageView arrow = (ImageView) v.findViewById(R.id.direction);
			Drawable arrowIcon = arrow.getDrawable();
			if (arrowIcon instanceof DirectionDrawable) {
				((DirectionDrawable) arrowIcon).setImage(R.drawable.ic_direction_arrow, nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light);
			}

			v.setBackgroundResource(AndroidUtils.resolveAttribute(ctx, android.R.attr.selectableItemBackground));
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
				View div = new View(ctx);
				LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtils.dpToPx(ctx, 1f));
				AndroidUtils.setMargins(p, listTextPadding, 0, 0, 0);
				div.setLayoutParams(p);
				AndroidUtils.setBackgroundColor(ctx, div, nightMode, R.color.divider_color_light, R.color.divider_color_dark);
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