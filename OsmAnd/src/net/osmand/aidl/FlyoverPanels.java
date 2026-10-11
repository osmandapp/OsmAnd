package net.osmand.aidl;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.mapwidgets.OutlinedTextContainer;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceApplier;
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance;
import net.osmand.plus.views.mapwidgets.appearance.WidgetBackgroundShape;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Panels of a flyover video, placed like a FrameLayout and drawn like OsmAnd's map widgets: the real widget
 * layout and the user's widget appearance (background, text colours, dividers). Laid out in the phone's dp and
 * drawn scaled to the video with the overlay's dp size.
 */
class FlyoverPanels {

	private static final float DEFAULT_WIDTH_DP = 180;
	private static final float CHART_HEIGHT_DP = 56;

	private final OsmandApplication app;
	private final FlyoverValues values;
	private final Context context;
	private final ResolvedPanelAppearance appearance;
	private final float scale;
	private final int frameWidth;
	private final int frameHeight;
	private final List<Panel> panels = new ArrayList<>();

	private class Panel {
		final JSONObject spec;
		final LinearLayout root;
		final List<Item> items = new ArrayList<>();

		Panel(@NonNull JSONObject spec) {
			this.spec = spec;
			root = new LinearLayout(context);
			boolean horizontal = "horizontal".equals(spec.optString("orientation"));
			root.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
			PanelAppearanceApplier.applyBackground(root, appearance, WidgetBackgroundShape.ROUND);
			JSONArray widgets = spec.optJSONArray("widgets");
			for (int i = 0; widgets != null && i < widgets.length(); i++) {
				JSONObject w = widgets.optJSONObject(i);
				if (w == null) continue;
				if (i > 0) {
					View divider = new View(context);
					divider.setBackgroundColor(appearance.getDividerColor());
					int px = AndroidUtils.dpToPx(context, 1);
					root.addView(divider, horizontal ? new LinearLayout.LayoutParams(px, ViewGroup.LayoutParams.MATCH_PARENT)
							: new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px));
				}
				Item item = "chart".equals(w.optString("type")) ? new ChartItem(w) : new WidgetItem(w);
				items.add(item);
				root.addView(item.view, horizontal
						? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1)
						: new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			}
		}

		float opacity(double t) {
			double from = spec.optDouble("from_s", 0);
			double to = spec.optDouble("to_s", Double.MAX_VALUE);
			if (t < from || t > to) return 0;
			double fade = spec.optDouble("fade_s", 0);
			return fade <= 0 ? 1 : (float) Math.min(1, Math.min((t - from) / fade, (to - t) / fade));
		}

		void draw(@NonNull Canvas canvas, @NonNull FlyoverValues.Frame frame) {
			float opacity = opacity(frame.t);
			if (opacity <= 0) {
				return;
			}
			for (Item item : items) {
				item.update(frame);
			}
			// sizes in phone pixels; the frame is frameWidth / scale of them wide
			float frameW = frameWidth / scale, frameH = frameHeight / scale;
			double w = spec.optDouble("width", 0);
			int width = Math.round(w > 1 ? (float) w / scale : w > 0 ? (float) w * frameW : AndroidUtils.dpToPx(context, DEFAULT_WIDTH_DP));
			root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
					View.MeasureSpec.makeMeasureSpec((int) frameH, View.MeasureSpec.AT_MOST));
			root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());

			float margin = (float) spec.optDouble("margin", 16) / scale;
			int gravity = parseGravity(spec.optString("gravity", "top|left"));
			float x, y;
			int horizontal = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
			int vertical = gravity & Gravity.VERTICAL_GRAVITY_MASK;
			x = horizontal == Gravity.RIGHT ? frameW - margin - root.getWidth()
					: horizontal == Gravity.CENTER_HORIZONTAL ? (frameW - root.getWidth()) / 2 : margin;
			y = vertical == Gravity.BOTTOM ? frameH - margin - root.getHeight()
					: vertical == Gravity.CENTER_VERTICAL ? (frameH - root.getHeight()) / 2 : margin;

			int save = opacity < 1 ? canvas.saveLayerAlpha(null, Math.round(opacity * 255)) : canvas.save();
			canvas.scale(scale, scale);
			canvas.translate(x, y);
			root.draw(canvas);
			canvas.restoreToCount(save);
		}
	}

	private abstract static class Item {
		View view;

		abstract void update(@NonNull FlyoverValues.Frame frame);
	}

	/** An attribute or a text in OsmAnd's simple widget layout: title, value, unit and icon. */
	private class WidgetItem extends Item {
		final JSONObject spec;
		final OutlinedTextContainer title;
		final OutlinedTextContainer value;
		final OutlinedTextContainer unit;

		@SuppressLint("InflateParams")
		WidgetItem(@NonNull JSONObject spec) {
			this.spec = spec;
			view = LayoutInflater.from(context).inflate(R.layout.simple_map_widget_medium, null);
			view.findViewById(R.id.widget_bg).setBackground(null);
			title = view.findViewById(R.id.widget_name);
			value = view.findViewById(R.id.widget_text);
			unit = view.findViewById(R.id.widget_text_small);
			PanelAppearanceApplier.applyPrimaryText(value, appearance);
			PanelAppearanceApplier.applySecondaryText(title, appearance);
			PanelAppearanceApplier.applySecondaryText(unit, appearance);
			title.setText(spec.optString("title"));
			ImageView icon = view.findViewById(R.id.widget_icon);
			Drawable drawable = getIcon(spec);
			icon.setImageDrawable(drawable);
			icon.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
		}

		@Override
		void update(@NonNull FlyoverValues.Frame frame) {
			String type = spec.optString("type", "attribute");
			String unitSpec = spec.optString("unit", "auto");
			int digits = spec.has("digits") ? spec.optInt("digits") : -1;
			if ("text".equals(type)) {
				value.setText(spec.optString("text"));
				unit.setText("");
			} else if (spec.has("template")) {
				value.setText(values.template(spec.optString("template"), unitSpec, digits, frame));
				unit.setText("");
			} else {
				String[] f = values.format(values.resolve(spec.optString("value"), frame), unitSpec, digits);
				value.setText(f[0]);
				unit.setText(f[1]);
			}
		}
	}

	/** A value drawn over its whole track, the part already passed highlighted, with a cursor at the current point. */
	private class ChartItem extends Item {
		final JSONObject spec;
		final ChartView chart;

		ChartItem(@NonNull JSONObject spec) {
			this.spec = spec;
			LinearLayout box = new LinearLayout(context);
			box.setOrientation(LinearLayout.VERTICAL);
			int pad = AndroidUtils.dpToPx(context, 12);
			box.setPadding(pad, AndroidUtils.dpToPx(context, 8), pad, AndroidUtils.dpToPx(context, 8));
			TextView title = new TextView(context);
			title.setText(spec.optString("title").toUpperCase());
			title.setTextSize(11);
			title.setLetterSpacing(0.06f);
			title.setTextColor(appearance.getSecondaryTextColor());
			box.addView(title);
			String expr = spec.optString("value");
			FlyoverTrack track = values.trackOf(expr);
			chart = new ChartView(context, track, values.seriesOf(expr));
			box.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					AndroidUtils.dpToPx(context, (float) spec.optDouble("height_dp", CHART_HEIGHT_DP))));
			view = box;
		}

		@Override
		void update(@NonNull FlyoverValues.Frame frame) {
			chart.cursor = chart.track != null ? chart.track.at(frame.t).distance : Double.NaN;
		}
	}

	private class ChartView extends View {
		final FlyoverTrack track;
		final double[] series;
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		final Path path = new Path();
		double cursor = Double.NaN;

		ChartView(@NonNull Context context, @Nullable FlyoverTrack track, @Nullable double[] series) {
			super(context);
			this.track = track;
			this.series = series;
		}

		@Override
		protected void onDraw(@NonNull Canvas canvas) {
			if (track == null || series == null) {
				return;
			}
			double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
			for (double v : series) {
				if (Double.isNaN(v)) continue;
				min = Math.min(min, v);
				max = Math.max(max, v);
			}
			if (min > max) {
				return;
			}
			if (max - min < 1e-6) {
				max = min + 1;
			}
			float w = getWidth(), h = getHeight();
			double length = track.getLength();
			int primary = appearance.getPrimaryTextColor();
			int active = ColorUtilities.getActiveColor(app, appearance.getNightMode());
			path.reset();
			boolean started = false;
			for (int i = 0; i < series.length; i++) {
				if (Double.isNaN(series[i])) continue;
				float x = (float) (track.dist[i] / length * w);
				float y = (float) (h - (series[i] - min) / (max - min) * (h * 0.9));
				if (!started) {
					path.moveTo(x, h);
					path.lineTo(x, y);
					started = true;
				} else {
					path.lineTo(x, y);
				}
			}
			path.lineTo(w, h);
			path.close();
			paint.setStyle(Paint.Style.FILL);
			paint.setColor(primary);
			paint.setAlpha(40);
			canvas.drawPath(path, paint);
			if (!Double.isNaN(cursor)) {
				float cx = (float) (cursor / length * w);
				canvas.save();
				canvas.clipRect(0, 0, cx, h);
				paint.setColor(active);
				paint.setAlpha(120);
				canvas.drawPath(path, paint);
				canvas.restore();
				paint.setAlpha(255);
				paint.setStrokeWidth(AndroidUtils.dpToPx(getContext(), 2));
				canvas.drawLine(cx, 0, cx, h, paint);
			}
		}
	}

	FlyoverPanels(@NonNull OsmandApplication app, @NonNull FlyoverValues values, @NonNull JSONObject spec,
	              int frameWidth, int frameHeight) {
		this.app = app;
		this.values = values;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
		boolean night = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		context = new ContextThemeWrapper(app, night ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme);
		float density = app.getResources().getDisplayMetrics().density;
		appearance = app.getPanelAppearanceSettingsManager()
				.resolveCommitted(WidgetsPanel.LEFT, null, night, false, density, true);
		scale = FlyoverOverlay.uiDensity(spec, frameWidth, frameHeight) / density;
		JSONArray list = spec.optJSONArray("panels");
		for (int i = 0; list != null && i < list.length(); i++) {
			JSONObject p = list.optJSONObject(i);
			if (p != null) {
				panels.add(new Panel(p));
			}
		}
	}

	boolean isEmpty() {
		return panels.isEmpty();
	}

	void draw(@NonNull Canvas canvas, @NonNull FlyoverValues.Frame frame) {
		for (Panel panel : panels) {
			panel.draw(canvas, frame);
		}
	}

	@Nullable
	private Drawable getIcon(@NonNull JSONObject spec) {
		boolean night = appearance.getNightMode();
		String name = spec.optString("icon");
		if (Algorithms.isEmpty(name)) {
			name = defaultIcon(spec.optString("value"));
		}
		if (Algorithms.isEmpty(name)) {
			return null;
		}
		int id = AndroidUtils.getDrawableId(app, name + (night ? "_night" : "_day"));
		if (id == 0) {
			id = AndroidUtils.getDrawableId(app, name);
		}
		return id != 0 ? ContextCompat.getDrawable(context, id) : null;
	}

	/** The icon OsmAnd's own widget for the same value has (see WidgetType). */
	@Nullable
	private static String defaultIcon(@NonNull String expr) {
		String name = expr.replaceFirst("^gpx\\[\\d+]\\.", "");
		switch (name) {
			case "speed": case "speed_max": case "max_speed_so_far": return name.contains("max") ? "widget_max_speed" : "widget_speed";
			case "speed_avg": case "avg_speed_so_far": return "widget_average_speed";
			case "ele": case "ele_min": case "ele_max": case "ele_avg": return "widget_altitude_location";
			case "progress_dist": case "dist": case "dist_moving": return "widget_trip_recording";
			case "remaining_dist": case "next_wpt.dist": return "widget_target";
			case "elapsed": case "duration": case "duration_moving": case "moving_elapsed": return "widget_track_recording_duration";
			case "remaining_time": return "widget_time_to_distance";
			case "climb": case "climb_so_far": return "widget_track_recording_uphill";
			case "descent": case "descent_so_far": return "widget_track_recording_downhill";
			case "hr": case "hr_avg": case "hr_max": case "hr_min": return "widget_sensor_heart_rate";
			case "cadence": case "cadence_avg": case "cadence_max": return "widget_sensor_cadence";
			case "power": case "power_avg": case "power_max": return "widget_sensor_bicycle_power";
			case "temp_air": case "temp_water": return "widget_weather_temperature";
			case "sensor_speed": return "widget_sensor_speed";
			case "bike_distance": return "widget_sensor_distance";
			case "time": case "start_time": case "end_time": return "widget_time";
			default: return null;
		}
	}

	/** FrameLayout-like gravity: top|bottom|center for rows, left|right|center for columns; "center" alone is both. */
	static int parseGravity(@NonNull String gravity) {
		int vertical = 0, horizontal = 0;
		boolean center = false;
		for (String part : gravity.split("\\|")) {
			switch (part.trim()) {
				case "top": vertical = Gravity.TOP; break;
				case "bottom": vertical = Gravity.BOTTOM; break;
				case "center_vertical": vertical = Gravity.CENTER_VERTICAL; break;
				case "left": case "start": horizontal = Gravity.LEFT; break;
				case "right": case "end": horizontal = Gravity.RIGHT; break;
				case "center_horizontal": horizontal = Gravity.CENTER_HORIZONTAL; break;
				case "center": center = true; break;
			}
		}
		if (center) {
			if (vertical == 0) vertical = Gravity.CENTER_VERTICAL;
			if (horizontal == 0) horizontal = Gravity.CENTER_HORIZONTAL;
		}
		return (vertical != 0 ? vertical : Gravity.TOP) | (horizontal != 0 ? horizontal : Gravity.LEFT);
	}
}
