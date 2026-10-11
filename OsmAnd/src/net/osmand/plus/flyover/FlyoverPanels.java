package net.osmand.plus.flyover;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.settings.enums.PanelBackgroundMode;
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceApplier;
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceResolver;
import net.osmand.plus.views.mapwidgets.appearance.PanelAppearanceState;
import net.osmand.plus.views.mapwidgets.appearance.ResolvedPanelAppearance;
import net.osmand.plus.views.mapwidgets.appearance.WidgetBackgroundShape;
import net.osmand.util.Algorithms;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Panels of a flyover video, placed like a FrameLayout and drawn like OsmAnd's map widgets: the real widget
 * layout and the user's widget appearance (background, text colours, dividers). Laid out in the phone's dp and
 * drawn scaled to the video with the overlay's dp size.
 */
class FlyoverPanels {

	private static final float CHART_HEIGHT_DP = 56;

	private final OsmandApplication app;
	private final FlyoverValues values;
	private final Context context;
	private final boolean nightMode;
	private final float density;
	private final float scale;
	private final int frameWidth;
	private final int frameHeight;
	private final List<Panel> panels = new ArrayList<>();

	private class Panel {
		final JSONObject spec;
		final LinearLayout root;
		final ResolvedPanelAppearance appearance;
		final List<Item> items = new ArrayList<>();

		Panel(@NonNull JSONObject spec) {
			this.spec = spec;
			root = new LinearLayout(context);
			boolean horizontal = "horizontal".equals(spec.optString("orientation"));
			root.setOrientation(horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
			// "background": default (the user's widget look), transparent (OsmAnd's transparent widgets) or 0..1
			Object background = spec.opt("background");
			appearance = resolveAppearance("transparent".equals(background));
			if (!"transparent".equals(background)) {
				PanelAppearanceApplier.applyBackground(root, appearance, WidgetBackgroundShape.ROUND);
				if (background instanceof Number && root.getBackground() != null) {
					float alpha = Math.max(0, Math.min(1, ((Number) background).floatValue()));
					root.setBackground(root.getBackground().mutate());
					root.getBackground().setAlpha(Math.round(alpha * 255));
				}
			}
			String panelSize = spec.optString("size", "medium");
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
				Item item = "chart".equals(w.optString("type")) ? new ChartItem(w, appearance)
						: new WidgetItem(w, appearance, w.optString("size", panelSize));
				items.add(item);
				root.addView(item.view, horizontal
						? new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1)
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

		// the widest the panel gets over the whole video: a panel as wide as each frame's text would jump
		int stableWidth;
		int stableHeight;

		private int requestedWidth() {
			double w = spec.optDouble("width", 0);
			return Math.round(w > 1 ? (float) w / scale : w > 0 ? (float) w * frameWidth / scale : 0);
		}

		void prepare(double videoS) {
			int width = requestedWidth();
			int samples = 60;
			for (int k = 0; k <= samples; k++) {
				FlyoverValues.Frame frame = new FlyoverValues.Frame(videoS * k / samples, videoS, new double[5]);
				for (Item item : items) {
					item.update(frame);
				}
				measure(width, frameHeight / scale);
				stableWidth = Math.max(stableWidth, root.getMeasuredWidth());
				stableHeight = Math.max(stableHeight, root.getMeasuredHeight());
			}
		}

		/** Where the panel is on the video frame, in video pixels. */
		@NonNull
		RectF bounds() {
			PointF p = position(stableWidth, stableHeight);
			return new RectF(p.x * scale, p.y * scale, (p.x + stableWidth) * scale, (p.y + stableHeight) * scale);
		}

		@NonNull
		private PointF position(int width, int height) {
			float frameW = frameWidth / scale, frameH = frameHeight / scale;
			float margin = (float) spec.optDouble("margin", 16) / scale;
			int gravity = parseGravity(spec.optString("gravity", "top|left"));
			int horizontal = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
			int vertical = gravity & Gravity.VERTICAL_GRAVITY_MASK;
			float x = horizontal == Gravity.RIGHT ? frameW - margin - width
					: horizontal == Gravity.CENTER_HORIZONTAL ? (frameW - width) / 2 : margin;
			float y = vertical == Gravity.BOTTOM ? frameH - margin - height
					: vertical == Gravity.CENTER_VERTICAL ? (frameH - height) / 2 : margin;
			return new PointF(x, y);
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
			float frameH = frameHeight / scale;
			measure(Math.max(stableWidth, requestedWidth()), frameH);

			PointF at = position(root.getWidth(), Math.max(stableHeight, root.getHeight()));
			float x = at.x, y = at.y;

			int save = opacity < 1 ? canvas.saveLayerAlpha(null, Math.round(opacity * 255)) : canvas.save();
			canvas.scale(scale, scale);
			canvas.translate(x, y);
			root.draw(canvas);
			canvas.restoreToCount(save);
		}

		private void measure(int width, float frameH) {
			// no width given: as wide as the widgets need
			root.measure(width > 0 ? View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
							: View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
					View.MeasureSpec.makeMeasureSpec((int) frameH, View.MeasureSpec.AT_MOST));
			root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());
		}
	}

	private abstract static class Item {
		View view;

		abstract void update(@NonNull FlyoverValues.Frame frame);

	}

	/**
	 * An attribute or a text, drawn with OsmAnd's widget look (icon, colours, outline of transparent widgets) in a
	 * layout made for video: icon, then the title above the value and its unit. Sized by its content, so nothing is
	 * cut or overlaps at any panel width; a value too wide for a fixed panel width gets a smaller font.
	 */
	private class WidgetItem extends Item {
		final JSONObject spec;
		final WidgetView widget;

		WidgetItem(@NonNull JSONObject spec, @NonNull ResolvedPanelAppearance appearance, @NonNull String size) {
			this.spec = spec;
			widget = new WidgetView(context, appearance, size, getIcon(spec), spec.optString("title"));
			view = widget;
		}

		@Override
		void update(@NonNull FlyoverValues.Frame frame) {
			frame = values.textFrame(frame);
			String type = spec.optString("type", "attribute");
			String unitSpec = spec.optString("unit", "auto");
			int digits = spec.has("digits") ? spec.optInt("digits") : -1;
			if ("text".equals(type)) {
				widget.set(spec.optString("text"), "");
			} else if (spec.has("template")) {
				widget.set(values.template(spec.optString("template"), unitSpec, digits, frame), "");
			} else {
				String[] f = values.format(values.resolve(spec.optString("value"), frame), unitSpec, digits);
				widget.set(f[0], f[1]);
			}
		}
	}

	private static class WidgetView extends View {
		// dp: icon, value text, title text, padding horizontal and vertical; per size small / medium / large
		private static final float[][] SIZES = {{20, 20, 10, 10, 6}, {24, 28, 11, 12, 8}, {32, 44, 12, 14, 10}};

		final ResolvedPanelAppearance appearance;
		@Nullable
		final Drawable icon;
		final String title;
		final float[] size;
		final float density;
		final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		final Paint smallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		String value = "";
		String unit = "";
		float valueScale = 1;

		WidgetView(@NonNull Context context, @NonNull ResolvedPanelAppearance appearance, @NonNull String sizeName,
		           @Nullable Drawable icon, @NonNull String title) {
			super(context);
			this.appearance = appearance;
			this.icon = icon;
			this.title = title.toUpperCase();
			this.size = SIZES["small".equals(sizeName) ? 0 : "large".equals(sizeName) ? 2 : 1];
			this.density = context.getResources().getDisplayMetrics().density;
			valuePaint.setTypeface(Typeface.create("sans-serif-medium", appearance.getBoldText() ? Typeface.BOLD : Typeface.NORMAL));
			smallPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
			smallPaint.setLetterSpacing(0.06f);
		}

		void set(@NonNull String value, @NonNull String unit) {
			this.value = value;
			this.unit = unit;
			valueScale = 1;
			requestLayout();
		}

		private float dp(float v) {
			return v * density;
		}

		private float textStart() {
			return dp(size[3]) + (icon != null ? dp(size[0]) + dp(8) : 0);
		}

		private float valueWidth(float scale) {
			valuePaint.setTextSize(dp(size[1]) * scale);
			smallPaint.setTextSize(dp(size[2]));
			float w = valuePaint.measureText(value);
			if (!unit.isEmpty()) {
				w += dp(4) + smallPaint.measureText(unit);
			}
			return w;
		}

		@Override
		protected void onMeasure(int widthSpec, int heightSpec) {
			smallPaint.setTextSize(dp(size[2]));
			float content = Math.max(valueWidth(1), smallPaint.measureText(title));
			int wanted = Math.round(textStart() + content + dp(size[3]));
			int width = MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED ? wanted
					: MeasureSpec.getMode(widthSpec) == MeasureSpec.EXACTLY ? MeasureSpec.getSize(widthSpec)
					: Math.min(wanted, MeasureSpec.getSize(widthSpec));
			float available = width - textStart() - dp(size[3]);
			float full = valueWidth(1);
			valueScale = full > available && full > 0 ? Math.max(0.45f, available / full) : 1;
			float height = 2 * dp(size[4]) + (title.isEmpty() ? 0 : dp(size[2]) * 1.4f) + dp(size[1]) * valueScale * 1.15f;
			setMeasuredDimension(width, Math.round(Math.max(height, dp(size[0]) + 2 * dp(size[4]))));
		}

		@Override
		protected void onDraw(@NonNull Canvas canvas) {
			boolean outline = appearance.getBackground().getMode() == PanelBackgroundMode.TRANSPARENT;
			float pad = dp(size[4]);
			if (icon != null) {
				int s = Math.round(dp(size[0]));
				int top = Math.round((getHeight() - s) / 2f);
				int left = Math.round(dp(size[3]));
				icon.setBounds(left, top, left + s, top + s);
				icon.draw(canvas);
			}
			float x = textStart();
			float y = pad;
			smallPaint.setTextSize(dp(size[2]));
			if (!title.isEmpty()) {
				y += dp(size[2]);
				drawText(canvas, title, x, y, smallPaint, appearance.getSecondaryTextColor(), outline);
				y += dp(size[2]) * 0.4f;
			}
			valuePaint.setTextSize(dp(size[1]) * valueScale);
			y += dp(size[1]) * valueScale * 0.95f;
			drawText(canvas, value, x, y, valuePaint, appearance.getPrimaryTextColor(), outline);
			if (!unit.isEmpty()) {
				float ux = x + valuePaint.measureText(value) + dp(4);
				drawText(canvas, unit, ux, y, smallPaint, appearance.getSecondaryTextColor(), outline);
			}
		}

		private void drawText(@NonNull Canvas canvas, @NonNull String text, float x, float y, @NonNull Paint paint,
		                      int color, boolean outline) {
			if (outline) {
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(Math.max(dp(3), appearance.getTextShadowRadius()));
				paint.setColor(appearance.getTextShadowColor());
				canvas.drawText(text, x, y, paint);
				paint.setStyle(Paint.Style.FILL);
			}
			paint.setColor(color);
			canvas.drawText(text, x, y, paint);
		}
	}

	/** A value drawn over its whole track, the part already passed highlighted, with a cursor at the current point. */
	private class ChartItem extends Item {
		final JSONObject spec;
		final ChartView chart;

		ChartItem(@NonNull JSONObject spec, @NonNull ResolvedPanelAppearance appearance) {
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
			chart = new ChartView(context, track, smooth(track, values.seriesOf(expr)), appearance);
			// a second value as a line over the first one's area, on its own scale (altitude and speed in one chart)
			String expr2 = spec.optString("value2");
			if (!Algorithms.isEmpty(expr2)) {
				chart.track2 = values.trackOf(expr2);
				chart.series2 = smooth(chart.track2, values.seriesOf(expr2));
				chart.color2 = parseColor(spec.optString("color2"), 0xFFFF8800);
			}
			// a panel without a width is as wide as its widgets; a chart has no width of its own
			chart.setMinimumWidth(AndroidUtils.dpToPx(context, (float) spec.optDouble("width_dp", 280)));
			box.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					AndroidUtils.dpToPx(context, (float) spec.optDouble("height_dp", CHART_HEIGHT_DP))));
			view = box;
		}

		@Override
		void update(@NonNull FlyoverValues.Frame frame) {
			chart.cursor = chart.track != null ? chart.track.at(frame.t).distance : Double.NaN;
			chart.cursor2 = chart.track2 != null ? chart.track2.at(frame.t).distance : Double.NaN;
		}
	}

	private static int parseColor(@Nullable String color, int defaultColor) {
		try {
			return Algorithms.isEmpty(color) ? defaultColor : Algorithms.parseColor(color);
		} catch (IllegalArgumentException e) {
			return defaultColor;
		}
	}

	/** Moving average over 2% of the track: whole-number values (heart rate) would draw as steps. */
	@Nullable
	private static double[] smooth(@Nullable FlyoverTrack track, @Nullable double[] series) {
		if (track == null || series == null) {
			return series;
		}
		double window = track.getLength() * 0.01;
		double[] out = new double[series.length];
		int a = 0, b = 0;
		double sum = 0;
		int count = 0;
		for (int i = 0; i < series.length; i++) {
			while (b < series.length && track.dist[b] - track.dist[i] <= window) {
				if (!Double.isNaN(series[b])) { sum += series[b]; count++; }
				b++;
			}
			while (track.dist[i] - track.dist[a] > window) {
				if (!Double.isNaN(series[a])) { sum -= series[a]; count--; }
				a++;
			}
			out[i] = count > 0 && !Double.isNaN(series[i]) ? sum / count : Double.NaN;
		}
		return out;
	}

	private class ChartView extends View {
		final FlyoverTrack track;
		final double[] series;
		final ResolvedPanelAppearance appearance;
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		final Path path = new Path();
		double cursor = Double.NaN;
		@Nullable
		FlyoverTrack track2;
		@Nullable
		double[] series2;
		int color2;
		double cursor2 = Double.NaN;

		ChartView(@NonNull Context context, @Nullable FlyoverTrack track, @Nullable double[] series,
		          @NonNull ResolvedPanelAppearance appearance) {
			super(context);
			this.track = track;
			this.series = series;
			this.appearance = appearance;
		}

		@Override
		protected void onDraw(@NonNull Canvas canvas) {
			float w = getWidth(), h = getHeight();
			int primary = appearance.getPrimaryTextColor();
			int active = ColorUtilities.getActiveColor(app, appearance.getNightMode());
			if (buildPath(track, series, w, h, true)) {
				paint.setStyle(Paint.Style.FILL);
				paint.setColor(primary);
				paint.setAlpha(40);
				canvas.drawPath(path, paint);
				if (!Double.isNaN(cursor)) {
					float cx = (float) (cursor / track.getLength() * w);
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
			if (buildPath(track2, series2, w, h, false)) {
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(AndroidUtils.dpToPx(getContext(), 2));
				paint.setColor(color2);
				paint.setAlpha(110);
				canvas.drawPath(path, paint);
				if (!Double.isNaN(cursor2)) {
					float cx = (float) (cursor2 / track2.getLength() * w);
					canvas.save();
					canvas.clipRect(0, 0, cx, h);
					paint.setAlpha(255);
					canvas.drawPath(path, paint);
					canvas.restore();
				}
				paint.setStyle(Paint.Style.FILL);
			}
		}

		/** The series as a smooth line, or as an area down to the bottom; false if there is nothing to draw. */
		private boolean buildPath(@Nullable FlyoverTrack track, @Nullable double[] series, float w, float h, boolean area) {
			path.reset();
			if (track == null || series == null) {
				return false;
			}
			double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
			for (double v : series) {
				if (Double.isNaN(v)) continue;
				min = Math.min(min, v);
				max = Math.max(max, v);
			}
			if (min > max) {
				return false;
			}
			if (max - min < 1e-6) {
				max = min + 1;
			}
			double length = track.getLength();
			float px = 0, py = 0;
			boolean started = false;
			for (int i = 0; i < series.length; i++) {
				if (Double.isNaN(series[i])) continue;
				float x = (float) (track.dist[i] / length * w);
				float y = (float) (h - (series[i] - min) / (max - min) * (h * 0.9));
				if (!started) {
					if (area) {
						path.moveTo(x, h);
						path.lineTo(x, y);
					} else {
						path.moveTo(x, y);
					}
					started = true;
				} else {
					// through the midpoints: a smooth line instead of a polyline
					path.quadTo(px, py, (px + x) / 2, (py + y) / 2);
				}
				px = x;
				py = y;
			}
			path.lineTo(px, py);
			if (area) {
				path.lineTo(w, h);
				path.close();
			}
			return true;
		}
	}

	FlyoverPanels(@NonNull OsmandApplication app, @NonNull FlyoverValues values, @NonNull JSONObject spec,
	              int frameWidth, int frameHeight) {
		this.app = app;
		this.values = values;
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
		nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		context = new ContextThemeWrapper(app, nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme);
		density = app.getResources().getDisplayMetrics().density;
		scale = FlyoverOverlay.uiDensity(spec, frameWidth, frameHeight) / density;
		JSONArray list = spec.optJSONArray("panels");
		for (int i = 0; list != null && i < list.length(); i++) {
			JSONObject p = list.optJSONObject(i);
			if (p != null) {
				panels.add(new Panel(p));
			}
		}
	}

	/** The user's widget look; transparent is OsmAnd's own transparent widget mode with outlined text. */
	@NonNull
	private ResolvedPanelAppearance resolveAppearance(boolean transparent) {
		if (!transparent) {
			return app.getPanelAppearanceSettingsManager()
					.resolveCommitted(WidgetsPanel.LEFT, null, nightMode, false, density, true);
		}
		PanelAppearanceState s = app.getPanelAppearanceSettingsManager().get(WidgetsPanel.LEFT)
				.readState(app.getSettings().getApplicationMode(), null);
		PanelAppearanceState t = new PanelAppearanceState(s.getSizeMode(), s.getIconMode(), s.getTextColorMode(),
				s.getSecondaryTextColorMode(), PanelBackgroundMode.TRANSPARENT, s.getTextColorDay(), s.getTextColorNight(),
				s.getSecondaryTextColorDay(), s.getSecondaryTextColorNight(), s.getBackgroundColorDay(), s.getBackgroundColorNight());
		return PanelAppearanceResolver.resolve(app, t, WidgetsPanel.LEFT, nightMode, false, density, true);
	}

	boolean isEmpty() {
		return panels.isEmpty();
	}

	void draw(@NonNull Canvas canvas, @NonNull FlyoverValues.Frame frame) {
		for (Panel panel : panels) {
			panel.draw(canvas, frame);
		}
	}

	/** Fixes every panel's size to its widest over the video, so panels do not jump as their values change. */
	void prepare(double videoS) {
		for (Panel panel : panels) {
			panel.prepare(videoS);
		}
	}

	/** Panels that cover each other or one of the elements OsmAnd draws, for the request's warnings. */
	@NonNull
	String overlaps(@NonNull Map<String, RectF> taken) {
		List<String> found = new ArrayList<>();
		for (int i = 0; i < panels.size(); i++) {
			RectF a = panels.get(i).bounds();
			for (int j = i + 1; j < panels.size(); j++) {
				if (RectF.intersects(a, panels.get(j).bounds())) {
					found.add("panels " + (i + 1) + " and " + (j + 1));
				}
			}
			for (Map.Entry<String, RectF> e : taken.entrySet()) {
				if (RectF.intersects(a, e.getValue())) {
					found.add("panel " + (i + 1) + " and the " + e.getKey());
				}
			}
		}
		return found.isEmpty() ? "" : "Overlapping on the frame: " + String.join(", ", found)
				+ "; move them with gravity and margin";
	}

	@Nullable
	private Drawable getIcon(@NonNull JSONObject spec) {
		boolean night = nightMode;
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
