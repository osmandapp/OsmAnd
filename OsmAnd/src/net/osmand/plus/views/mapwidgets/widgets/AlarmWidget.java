package net.osmand.plus.views.mapwidgets.widgets;

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static net.osmand.plus.routing.AlarmInfoType.BORDER_CONTROL;
import static net.osmand.plus.routing.AlarmInfoType.HAZARD;
import static net.osmand.plus.routing.AlarmInfoType.PEDESTRIAN;
import static net.osmand.plus.routing.AlarmInfoType.RAILWAY;
import static net.osmand.plus.routing.AlarmInfoType.SPEED_CAMERA;
import static net.osmand.plus.routing.AlarmInfoType.SPEED_LIMIT;
import static net.osmand.plus.routing.AlarmInfoType.STOP;
import static net.osmand.plus.routing.AlarmInfoType.TOLL_BOOTH;
import static net.osmand.plus.routing.AlarmInfoType.TRAFFIC_CALMING;
import static net.osmand.plus.routing.AlarmInfoType.TUNNEL;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.routing.AlarmInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.DrivingRegion;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.util.Algorithms;

public class AlarmWidget {

	private static final float WIDGET_BITMAP_SIZE_DP = 92f;
	private static final float WIDGET_BITMAP_TEXT_SIZE = 26f;
	private static final float WIDGET_BITMAP_TEXT_AMERICAN_SPEED_LIMIT_SHIFT_DP = 12f;
	private static final float WIDGET_BITMAP_BOTTOM_TEXT_SIZE = 16f;
	private static final float WIDGET_BITMAP_BOTTOM_TEXT_SIZE_SMALL = 12f;

	@Nullable
	private final View layout;
	@Nullable
	private final ImageView icon;
	@Nullable
	private final TextView widgetText;
	@Nullable
	private final TextView widgetBottomText;

	@Nullable
	private Bitmap widgetBitmap;

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final RoutingHelper routingHelper;
	private final MapViewTrackingUtilities trackingUtilities;
	private final OsmAndLocationProvider locationProvider;
	private final WaypointHelper wh;

	private int imgId;
	private String cachedText;
	private String cachedBottomText;
	private DrivingRegion cachedRegion;

	public static class AlarmWidgetInfo {
		public AlarmInfo alarm;
		public boolean americanType;
		public boolean isCanadianRegion;
		public int locImgId;
		public String text;
		public String bottomText;
		public DrivingRegion region;
	}

	public AlarmWidget(@NonNull OsmandApplication app, @Nullable MapActivity mapActivity) {
		if (mapActivity != null) {
			layout = mapActivity.findViewById(R.id.map_alarm_warning);
			icon = mapActivity.findViewById(R.id.map_alarm_warning_icon);
			widgetText = mapActivity.findViewById(R.id.map_alarm_warning_text);
			widgetBottomText = mapActivity.findViewById(R.id.map_alarm_warning_text_bottom);
		} else {
			layout = null;
			icon = null;
			widgetText = null;
			widgetBottomText = null;
		}
		this.app = app;
		routingHelper = app.getRoutingHelper();
		trackingUtilities = app.getMapViewTrackingUtilities();
		settings = app.getSettings();
		locationProvider = app.getLocationProvider();
		wh = app.getWaypointHelper();
	}

	@Nullable
	public Bitmap getWidgetBitmap() {
		return widgetBitmap;
	}

	public boolean updateInfo(DrawSettings drawSettings, boolean drawBitmap) {
		boolean showRoutingAlarms = settings.SHOW_ROUTING_ALARMS.get();
		boolean trafficWarnings = settings.SHOW_TRAFFIC_WARNINGS.get();
		boolean cams = settings.SHOW_CAMERAS.get();
		boolean browseMap = settings.APPLICATION_MODE.get() == ApplicationMode.DEFAULT;
		boolean visible = false;
		if ((routingHelper.isFollowingMode() || trackingUtilities.isMapLinkedToLocation() && !browseMap)
				&& showRoutingAlarms && (trafficWarnings || cams)) {
			AlarmInfo alarm;
			if (routingHelper.isFollowingMode() && !routingHelper.isDeviatedFromRoute()
					&& (routingHelper.getCurrentGPXRoute() == null || routingHelper.isCurrentGPXRouteV2())) {
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
			boolean changed = false;
			AlarmWidgetInfo info = null;
			if (alarm != null) {
				info = createWidgetInfo(alarm);
				if (info != null) {
					visible = true;
					if (layout != null) {
						layout.setContentDescription(alarm.getType().getVisualName(app));
					}
					if (info.locImgId != imgId) {
						changed = true;
						imgId = info.locImgId;
						if (icon != null) {
							icon.setImageResource(info.locImgId);
						}
					}
					if (!Algorithms.objectEquals(info.text, cachedText) || cachedRegion != info.region) {
						changed = true;
						cachedText = info.text;
						cachedRegion = info.region;
						if (layout != null && widgetText != null) {
							widgetText.setText(cachedText);
							Resources res = layout.getContext().getResources();
							if (alarm.getType() == SPEED_LIMIT && info.americanType && !info.isCanadianRegion) {
								int topPadding = res.getDimensionPixelSize(R.dimen.map_alarm_text_top_padding);
								widgetText.setPadding(0, topPadding, 0, 0);
							} else {
								widgetText.setPadding(0, 0, 0, 0);
							}
						}
					}
					if (!Algorithms.objectEquals(info.bottomText, cachedBottomText) || cachedRegion != info.region) {
						changed = true;
						cachedBottomText = info.bottomText;
						cachedRegion = info.region;
						if (layout != null && widgetBottomText != null) {
							widgetBottomText.setText(cachedBottomText);
							Resources res = layout.getContext().getResources();
							if (alarm.getType() == SPEED_LIMIT && info.isCanadianRegion) {
								int bottomPadding = res.getDimensionPixelSize(R.dimen.map_button_margin);
								widgetBottomText.setPadding(0, 0, 0, bottomPadding);
								widgetBottomText.setTextSize(COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.map_alarm_bottom_si_text_size));
							} else {
								widgetBottomText.setPadding(0, 0, 0, 0);
								widgetBottomText.setTextSize(COMPLEX_UNIT_PX, res.getDimensionPixelSize(R.dimen.map_alarm_bottom_text_size));
							}
							widgetBottomText.setTextColor(ContextCompat.getColor(layout.getContext(),
									info.americanType ? R.color.activity_background_color_dark : R.color.card_and_list_background_light));
						}
					}
				}
			}
			if (!visible && widgetBitmap != null) {
				changed = true;
			}
			if (visible && widgetBitmap == null && drawBitmap) {
				changed = true;
			}
			if (changed && icon == null) {
				if (info == null || drawSettings == null || !drawBitmap) {
					widgetBitmap = null;
				} else {
					float density = drawSettings.getDensity();
					widgetBitmap = createWidgetBitmap(info, density == 0 ? 1 : density);
				}
			}
		}
		if (layout != null) {
			AndroidUiHelper.updateVisibility(layout, visible);
		}
		if (!visible && drawBitmap) {
			widgetBitmap = null;
		}
		return true;
	}

	@NonNull
	private Bitmap createWidgetBitmap(@NonNull AlarmWidgetInfo info, float density) {
		Bitmap bitmap = Bitmap.createBitmap((int) (WIDGET_BITMAP_SIZE_DP * density),
				(int) (WIDGET_BITMAP_SIZE_DP * density), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		Drawable locImg = app.getUIUtilities().getIcon(info.locImgId);
		locImg.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		locImg.draw(canvas);

		if (!Algorithms.isEmpty(info.text)) {
			TextPaint textPaint = new TextPaint();
			textPaint.setAntiAlias(true);
			textPaint.setColor(Color.BLACK);
			textPaint.setTextSize(WIDGET_BITMAP_TEXT_SIZE * density);
			textPaint.setTextAlign(Paint.Align.CENTER);
			textPaint.setTypeface(Typeface.DEFAULT_BOLD);
			textPaint.setTextAlign(Paint.Align.CENTER);
			float x = canvas.getWidth() / 2f;
			float y = canvas.getHeight() / 2f - ((textPaint.descent() + textPaint.ascent()) / 2);
			if (info.alarm.getType() == SPEED_LIMIT && info.americanType && !info.isCanadianRegion) {
				y += WIDGET_BITMAP_TEXT_AMERICAN_SPEED_LIMIT_SHIFT_DP * density;
			}
			canvas.drawText(info.text, x, y, textPaint);
		}

		if (!Algorithms.isEmpty(info.bottomText)) {
			TextPaint textPaint = new TextPaint();
			textPaint.setAntiAlias(true);
			textPaint.setColor(Color.BLACK);
			textPaint.setColor(ContextCompat.getColor(app, info.americanType ? R.color.activity_background_color_dark : R.color.card_and_list_background_light));
			textPaint.setTextSize(WIDGET_BITMAP_BOTTOM_TEXT_SIZE * density);
			textPaint.setTextAlign(Paint.Align.CENTER);
			textPaint.setTypeface(Typeface.DEFAULT_BOLD);
			textPaint.setTextAlign(Paint.Align.CENTER);
			float x = canvas.getWidth() / 2f;
			float y = canvas.getHeight() - (textPaint.descent() - textPaint.ascent());
			if (info.alarm.getType() == SPEED_LIMIT && info.isCanadianRegion) {
				textPaint.setTextSize(WIDGET_BITMAP_BOTTOM_TEXT_SIZE_SMALL * density);
			}
			canvas.drawText(info.bottomText, x, y, textPaint);
		}
		return bitmap;
	}

	public AlarmWidgetInfo createWidgetInfo(@NonNull AlarmInfo alarm) {
		DrivingRegion region = settings.DRIVING_REGION.get();
		boolean trafficWarnings = settings.SHOW_TRAFFIC_WARNINGS.get();
		boolean cams = settings.SHOW_CAMERAS.get();
		boolean peds = settings.SHOW_PEDESTRIAN.get();
		boolean tunnels = settings.SHOW_TUNNELS.get();
		boolean americanType = region.isAmericanTypeSigns();

		int locImgId = R.drawable.warnings_limit;
		String text = "";
		String bottomText = "";
		boolean isCanadianRegion = region == DrivingRegion.CANADA;
		if (alarm.getType() == SPEED_LIMIT) {
			if (isCanadianRegion) {
				locImgId = R.drawable.warnings_speed_limit_ca;
				bottomText = settings.SPEED_SYSTEM.get().toShortString(settings.getContext());
			} else if (americanType) {
				locImgId = R.drawable.warnings_speed_limit_us;
				//else case is done by drawing red ring
			}
			text = String.valueOf(alarm.getIntValue());
		} else if (alarm.getType() == SPEED_CAMERA) {
			locImgId = R.drawable.warnings_speed_camera;
		} else if (alarm.getType() == BORDER_CONTROL) {
			locImgId = R.drawable.warnings_border_control;
		} else if (alarm.getType() == HAZARD) {
			if (americanType) {
				locImgId = R.drawable.warnings_hazard_us;
			} else {
				locImgId = R.drawable.warnings_hazard;
			}
		} else if (alarm.getType() == TOLL_BOOTH) {
			//image done by drawing red ring
			text = "$";
		} else if (alarm.getType() == TRAFFIC_CALMING) {
			if (americanType) {
				locImgId = R.drawable.warnings_traffic_calming_us;
			} else {
				locImgId = R.drawable.warnings_traffic_calming;
			}
		} else if (alarm.getType() == STOP) {
			locImgId = R.drawable.warnings_stop;
		} else if (alarm.getType() == RAILWAY) {
			if (isCanadianRegion) {
				locImgId = R.drawable.warnings_railways_ca;
			} else if (americanType) {
				locImgId = R.drawable.warnings_railways_us;
			} else {
				locImgId = R.drawable.warnings_railways;
			}
		} else if (alarm.getType() == PEDESTRIAN) {
			if (americanType) {
				locImgId = R.drawable.warnings_pedestrian_us;
			} else {
				locImgId = R.drawable.warnings_pedestrian;
			}
		} else if (alarm.getType() == TUNNEL) {
			if (americanType) {
				locImgId = R.drawable.warnings_tunnel_us;
			} else {
				locImgId = R.drawable.warnings_tunnel;
			}
			bottomText = OsmAndFormatter.getFormattedAlarmInfoDistance(settings.getContext(), alarm.getFloatValue());
		} else {
			text = null;
			bottomText = null;
		}
		boolean visible;
		if (alarm.getType() == SPEED_CAMERA) {
			visible = cams;
		} else if (alarm.getType() == PEDESTRIAN) {
			visible = peds;
		} else if (alarm.getType() == TUNNEL) {
			visible = tunnels;
		} else {
			visible = trafficWarnings;
		}
		if (visible) {
			AlarmWidgetInfo info = new AlarmWidgetInfo();
			info.alarm = alarm;
			info.americanType = americanType;
			info.isCanadianRegion = isCanadianRegion;
			info.locImgId = locImgId;
			info.text = text;
			info.bottomText = bottomText;
			info.region = region;
			return info;
		} else {
			return null;
		}
	}

	public void setVisibility(boolean visibility) {
		AndroidUiHelper.updateVisibility(layout, visibility);
	}
}