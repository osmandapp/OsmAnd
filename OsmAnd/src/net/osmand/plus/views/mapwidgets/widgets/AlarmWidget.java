package net.osmand.plus.views.mapwidgets.widgets;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.enums.DrivingRegion;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.routing.AlarmInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.util.Algorithms;

import static android.util.TypedValue.COMPLEX_UNIT_PX;

public class AlarmWidget {

	private View layout;
	private ImageView icon;
	private TextView widgetText;
	private TextView widgetBottomText;
	private OsmandSettings settings;
	private RoutingHelper routingHelper;
	private MapViewTrackingUtilities trackingUtilities;
	private OsmAndLocationProvider locationProvider;
	private WaypointHelper wh;
	private int imgId;
	private String cachedText;
	private String cachedBottomText;
	private DrivingRegion cachedRegion;

	public AlarmWidget(final OsmandApplication app, MapActivity ma) {
		layout = ma.findViewById(R.id.map_alarm_warning);
		icon = ma.findViewById(R.id.map_alarm_warning_icon);
		widgetText = ma.findViewById(R.id.map_alarm_warning_text);
		widgetBottomText = ma.findViewById(R.id.map_alarm_warning_text_bottom);
		settings = app.getSettings();
		routingHelper = ma.getRoutingHelper();
		trackingUtilities = ma.getMapViewTrackingUtilities();
		locationProvider = app.getLocationProvider();
		wh = app.getWaypointHelper();
	}

	public boolean updateInfo(OsmandMapLayer.DrawSettings drawSettings) {
		boolean showRoutingAlarms = settings.SHOW_ROUTING_ALARMS.get();
		boolean trafficWarnings = settings.SHOW_TRAFFIC_WARNINGS.get();
		boolean cams = settings.SHOW_CAMERAS.get();
		boolean peds = settings.SHOW_PEDESTRIAN.get();
		boolean tunnels = settings.SHOW_TUNNELS.get();
		boolean browseMap = settings.APPLICATION_MODE.get() == ApplicationMode.DEFAULT;
		boolean visible = false;
		if ((routingHelper.isFollowingMode() || trackingUtilities.isMapLinkedToLocation() && !browseMap)
				&& showRoutingAlarms && (trafficWarnings || cams)) {
			AlarmInfo alarm;
			if (routingHelper.isFollowingMode() && !routingHelper.isDeviatedFromRoute() && (routingHelper.getCurrentGPXRoute() == null || routingHelper.isCurrentGPXRouteV2())) {
				alarm = wh.getMostImportantAlarm(settings.SPEED_SYSTEM.get(), cams);
			} else {
				RouteDataObject ro = locationProvider.getLastKnownRouteSegment();
				Location loc = locationProvider.getLastKnownLocation();
				if (ro != null && loc != null) {
					alarm = wh.calculateMostImportantAlarm(ro, loc, settings.METRIC_SYSTEM.get(),
							settings.SPEED_SYSTEM.get(), cams);
				} else {
					alarm = null;
				}
			}
			if (alarm != null) {
				int locimgId = R.drawable.warnings_limit;
				String text = "";
				String bottomText = "";
				DrivingRegion region = settings.DRIVING_REGION.get();
				boolean americanType = region.isAmericanTypeSigns();
				boolean isCanadianRegion = region == DrivingRegion.CANADA;
				if (alarm.getType() == AlarmInfo.AlarmInfoType.SPEED_LIMIT) {
					if (isCanadianRegion) {
						locimgId = R.drawable.warnings_speed_limit_ca;
						bottomText = settings.SPEED_SYSTEM.get().toShortString(settings.getContext());
					} else if (americanType) {
						locimgId = R.drawable.warnings_speed_limit_us;
						//else case is done by drawing red ring
					}
					text = alarm.getIntValue() + "";
				} else if (alarm.getType() == AlarmInfo.AlarmInfoType.SPEED_CAMERA) {
					locimgId = R.drawable.warnings_speed_camera;
				} else if (alarm.getType() == AlarmInfo.AlarmInfoType.BORDER_CONTROL) {
					locimgId = R.drawable.warnings_border_control;
				} else if (alarm.getType() == AlarmInfo.AlarmInfoType.HAZARD) {
					if (americanType) {
						locimgId = R.drawable.warnings_hazard_us;
					} else {
						locimgId = R.drawable.warnings_hazard;
					}
				} else if (alarm.getType() == AlarmInfo.AlarmInfoType.TOLL_BOOTH) {
					//image done by drawing red ring
					text = "$";
				} else if (alarm.getType() == AlarmInfo.AlarmInfoType.TRAFFIC_CALMING) {
					if (americanType) {
						locimgId = R.drawable.warnings_traffic_calming_us;
					} else {
						locimgId = R.drawable.warnings_traffic_calming;
					}
				} else if (alarm.getType() == AlarmInfo.AlarmInfoType.STOP) {
					locimgId = R.drawable.warnings_stop;
				} else if (alarm.getType() == AlarmInfo.AlarmInfoType.RAILWAY) {
					if (isCanadianRegion) {
						locimgId = R.drawable.warnings_railways_ca;
					} else if (americanType) {
						locimgId = R.drawable.warnings_railways_us;
					} else {
						locimgId = R.drawable.warnings_railways;
					}
				} else if (alarm.getType() == AlarmInfo.AlarmInfoType.PEDESTRIAN) {
					if (americanType) {
						locimgId = R.drawable.warnings_pedestrian_us;
					} else {
						locimgId = R.drawable.warnings_pedestrian;
					}
				} else if (alarm.getType() == AlarmInfo.AlarmInfoType.TUNNEL) {
					if (americanType) {
						locimgId = R.drawable.warnings_tunnel_us;
					} else {
						locimgId = R.drawable.warnings_tunnel;
					}
					bottomText = OsmAndFormatter.getFormattedAlarmInfoDistance(settings.getContext(), alarm.getFloatValue());
				} else {
					text = null;
					bottomText = null;
				}
				visible = (text != null && text.length() > 0) || (locimgId != 0);
				if (visible) {
					if (alarm.getType() == AlarmInfo.AlarmInfoType.SPEED_CAMERA) {
						visible = cams;
					} else if (alarm.getType() == AlarmInfo.AlarmInfoType.PEDESTRIAN) {
						visible = peds;
					} else if (alarm.getType() == AlarmInfo.AlarmInfoType.TUNNEL) {
						visible = tunnels;
					} else {
						visible = trafficWarnings;
					}
				}
				if (visible) {
					if (locimgId != imgId) {
						imgId = locimgId;
						icon.setImageResource(locimgId);
					}
					Resources res = layout.getContext().getResources();
					if (!Algorithms.objectEquals(text, cachedText) || cachedRegion != region) {
						cachedText = text;
						widgetText.setText(cachedText);
						if (alarm.getType() == AlarmInfo.AlarmInfoType.SPEED_LIMIT && americanType && !isCanadianRegion) {
							int topPadding = res.getDimensionPixelSize(R.dimen.map_alarm_text_top_padding);
							widgetText.setPadding(0, topPadding, 0, 0);
						} else {
							widgetText.setPadding(0, 0, 0, 0);
						}
					}
					if (!Algorithms.objectEquals(bottomText, cachedBottomText) || cachedRegion != region) {
						cachedBottomText = bottomText;
						widgetBottomText.setText(cachedBottomText);
						cachedRegion = region;
						if (alarm.getType() == AlarmInfo.AlarmInfoType.SPEED_LIMIT && isCanadianRegion) {
							int bottomPadding = res.getDimensionPixelSize(R.dimen.map_button_margin);
							widgetBottomText.setPadding(0, 0, 0, bottomPadding);
							widgetBottomText.setTextSize(COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.map_alarm_bottom_si_text_size));
						} else {
							widgetBottomText.setPadding(0, 0, 0, 0);
							widgetBottomText.setTextSize(COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.map_alarm_bottom_text_size));
						}
						widgetBottomText.setTextColor(ContextCompat.getColor(layout.getContext(),
								americanType ? R.color.color_black : R.color.color_white));
					}
				}
			}
		}
		AndroidUiHelper.updateVisibility(layout, visible);
		return true;
	}

	public void setVisibility(boolean visibility) {
		AndroidUiHelper.updateVisibility(layout, visibility);
	}
}