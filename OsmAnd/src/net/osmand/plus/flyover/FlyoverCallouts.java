package net.osmand.plus.flyover;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Labels and bubbles over the waypoints of the flyover's tracks. When and how each one shows is read from
 * {@code osmand:flyover_*} extensions of the waypoint, see the flyover guide.
 */
class FlyoverCallouts {

	// a reached waypoint shows from 2 s before the track gets there, 5 s in all
	private static final double DEFAULT_DURATION_S = 5;
	private static final double DEFAULT_LEAD_S = 2;
	private static final double DEFAULT_FADE_S = 0.4;
	private static final int LABEL_MAX_CHARS = 24;
	// a waypoint further than this from its track is not reached by the track's current point
	private static final double ON_TRACK_M = 300;

	interface Projection {
		/** Screen point of a waypoint in the video's pixels, null when it is not on the screen. */
		@Nullable
		PointF toScreen(@NonNull WptPt point);
	}

	private static class Callout {
		WptPt point;
		String title;
		String text;
		boolean bubble;
		double fromS;
		double toS;
		double fadeS;
	}

	private final List<Callout> callouts = new ArrayList<>();
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final float density;
	private final boolean nightMode;
	private final int cardColor;
	private final int textColor;
	private final int secondaryColor;

	FlyoverCallouts(@NonNull OsmandApplication app, @NonNull List<FlyoverTrack> tracks, @NonNull JSONObject spec,
	                int width, int height, double videoS, int fps) {
		density = FlyoverOverlay.uiDensity(spec, width, height);
		nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		cardColor = ColorUtilities.getMapButtonBackgroundColor(app, nightMode);
		textColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		secondaryColor = ColorUtilities.getSecondaryTextColor(app, nightMode);
		for (FlyoverTrack track : tracks) {
			for (WptPt w : track.gpx.getPointsList()) {
				Callout c = create(track, w, videoS, fps);
				if (c != null) {
					callouts.add(c);
				}
			}
		}
	}

	@Nullable
	private Callout create(@NonNull FlyoverTrack track, @NonNull WptPt w, double videoS, int fps) {
		// settings of a waypoint: "wpt": {"<name>": {...}} in the request, then its osmand:flyover_* tags,
		// then "wpt_defaults" in the request
		JSONObject byName = track.spec.optJSONObject("wpt");
		JSONObject own = byName != null && w.getName() != null ? byName.optJSONObject(w.getName()) : null;
		JSONObject defaults = track.spec.optJSONObject("wpt_defaults");
		Map<String, String> ext = new java.util.HashMap<>();
		if (defaults != null) copyInto(defaults, ext);
		for (Map.Entry<String, String> e : w.getExtensionsToRead().entrySet()) {
			ext.put(e.getKey().replaceFirst("^osmand:", ""), e.getValue());
		}
		if (own != null) copyInto(own, ext);
		String show = tag(ext, "show", "auto");
		if ("never".equals(show)) {
			return null;
		}
		Callout c = new Callout();
		c.point = w;
		String custom = tag(ext, "text", null);
		c.title = custom != null ? custom : w.getName();
		c.text = custom != null ? null : w.getDesc();
		String fields = tag(ext, "fields", null);
		if (fields != null && custom == null) {
			c.text = fieldsText(w, fields);
		}
		if (Algorithms.isEmpty(c.title) && Algorithms.isEmpty(c.text)) {
			return null;
		}
		String style = tag(ext, "style", "auto");
		c.bubble = "bubble".equals(style) || ("auto".equals(style)
				&& (!Algorithms.isEmpty(c.text) || c.title.length() > LABEL_MAX_CHARS));
		c.fadeS = number(ext, "fade_s", DEFAULT_FADE_S);
		double duration = number(ext, "duration_s", DEFAULT_DURATION_S);
		double at = number(ext, "show_s", Double.NaN);
		if ("always".equals(show)) {
			c.fromS = 0;
			c.toS = videoS;
		} else if (!Double.isNaN(at)) {
			c.fromS = at;
			c.toS = at + duration;
		} else {
			double reachedS = reachedAt(track, w, videoS, fps);
			if (Double.isNaN(reachedS)) {
				// away from the track: shown while it is on the screen
				c.fromS = 0;
				c.toS = videoS;
				return c;
			}
			double lead = Math.min(number(ext, "lead_s", DEFAULT_LEAD_S), duration);
			c.fromS = Math.max(0, reachedS - lead);
			c.toS = c.fromS + duration;
		}
		return c;
	}

	/** Video time when the track's current point passes the waypoint, NaN if the waypoint is off the track. */
	private static double reachedAt(@NonNull FlyoverTrack track, @NonNull WptPt w, double videoS, int fps) {
		int nearest = -1;
		double best = ON_TRACK_M;
		for (int i = 0; i < track.points.size(); i++) {
			WptPt p = track.points.get(i);
			double d = MapUtils.getDistance(p.getLat(), p.getLon(), w.getLat(), w.getLon());
			if (d < best) {
				best = d;
				nearest = i;
			}
		}
		if (nearest < 0) {
			return Double.NaN;
		}
		double target = track.dist[nearest];
		for (int f = 0; f <= videoS * fps; f++) {
			double t = (double) f / fps;
			if (track.at(t).distance >= target) {
				return t;
			}
		}
		return Double.NaN;
	}

	private static void copyInto(@NonNull JSONObject json, @NonNull Map<String, String> ext) {
		for (java.util.Iterator<String> it = json.keys(); it.hasNext(); ) {
			String key = it.next();
			ext.put("flyover_" + key, json.optString(key));
		}
	}

	@Nullable
	private static String tag(@NonNull Map<String, String> ext, @NonNull String name, @Nullable String def) {
		String v = ext.get("flyover_" + name);
		return v != null ? v.trim() : def;
	}

	private static double number(@NonNull Map<String, String> ext, @NonNull String name, double def) {
		String v = tag(ext, name, null);
		if (v == null) return def;
		try {
			return Double.parseDouble(v);
		} catch (NumberFormatException e) {
			return def;
		}
	}

	@Nullable
	private static String fieldsText(@NonNull WptPt w, @NonNull String fields) {
		List<String> parts = new ArrayList<>();
		for (String f : fields.split(",")) {
			switch (f.trim()) {
				case "desc": if (!Algorithms.isEmpty(w.getDesc())) parts.add(w.getDesc()); break;
				case "address": {
					String a = w.getExtensionsToRead().get("address");
					if (!Algorithms.isEmpty(a)) parts.add(a);
					break;
				}
				case "ele": if (!Double.isNaN(w.getEle())) parts.add(Math.round(w.getEle()) + " m"); break;
				case "time": if (w.getTime() > 0) parts.add(new java.text.SimpleDateFormat("HH:mm",
						java.util.Locale.getDefault()).format(new java.util.Date(w.getTime()))); break;
			}
		}
		return parts.isEmpty() ? null : TextUtils.join(" · ", parts);
	}

	boolean isEmpty() {
		return callouts.isEmpty();
	}

	/** Callouts shown at video time t, for projecting only those. */
	@NonNull
	List<WptPt> visibleAt(double t) {
		List<WptPt> list = new ArrayList<>();
		for (Callout c : callouts) {
			if (t >= c.fromS && t <= c.toS) {
				list.add(c.point);
			}
		}
		return list;
	}

	void draw(@NonNull Canvas canvas, double t, @NonNull Projection projection) {
		for (Callout c : callouts) {
			if (t < c.fromS || t > c.toS) continue;
			float opacity = c.fadeS <= 0 ? 1 : (float) Math.min(1, Math.min((t - c.fromS) / c.fadeS, (c.toS - t) / c.fadeS));
			PointF at = projection.toScreen(c.point);
			if (at == null || opacity <= 0) continue;
			int save = canvas.saveLayerAlpha(null, Math.round(opacity * 255));
			if (c.bubble) {
				drawBubble(canvas, c, at);
			} else {
				drawLabel(canvas, c.title, at);
			}
			canvas.restoreToCount(save);
		}
	}

	private void drawLabel(@NonNull Canvas canvas, @NonNull String text, @NonNull PointF at) {
		paint.setTextSize(dp(14));
		paint.setTypeface(Typeface.DEFAULT_BOLD);
		paint.setTextAlign(Paint.Align.CENTER);
		float y = at.y - dp(18);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(dp(3));
		paint.setColor(nightMode ? Color.BLACK : Color.WHITE);
		canvas.drawText(text, at.x, y, paint);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(nightMode ? Color.WHITE : 0xFF1A1A1A);
		canvas.drawText(text, at.x, y, paint);
	}

	private void drawBubble(@NonNull Canvas canvas, @NonNull Callout c, @NonNull PointF at) {
		float pad = dp(10), arrow = dp(8), maxWidth = dp(240);
		paint.setStyle(Paint.Style.FILL);
		paint.setTextAlign(Paint.Align.LEFT);
		paint.setTypeface(Typeface.DEFAULT_BOLD);
		paint.setTextSize(dp(14));
		String title = ellipsize(c.title, maxWidth);
		float width = paint.measureText(title);
		List<String> lines = new ArrayList<>();
		paint.setTypeface(Typeface.DEFAULT);
		paint.setTextSize(dp(12));
		if (!Algorithms.isEmpty(c.text)) {
			lines = wrap(c.text, maxWidth, 3);
			for (String line : lines) {
				width = Math.max(width, paint.measureText(line));
			}
		}
		float height = dp(18) + lines.size() * dp(16);
		float left = at.x - width / 2 - pad;
		float bottom = at.y - dp(14) - arrow;
		RectF card = new RectF(left, bottom - height - 2 * pad + dp(4), left + width + 2 * pad, bottom);
		paint.setColor(cardColor);
		paint.setShadowLayer(dp(4), 0, dp(1), 0x40000000);
		canvas.drawRoundRect(card, dp(8), dp(8), paint);
		Path tail = new Path();
		tail.moveTo(at.x - arrow, bottom - 1);
		tail.lineTo(at.x + arrow, bottom - 1);
		tail.lineTo(at.x, bottom + arrow);
		tail.close();
		canvas.drawPath(tail, paint);
		paint.clearShadowLayer();
		float x = card.left + pad;
		float y = card.top + pad + dp(13);
		paint.setTypeface(Typeface.DEFAULT_BOLD);
		paint.setTextSize(dp(14));
		paint.setColor(textColor);
		canvas.drawText(title, x, y, paint);
		paint.setTypeface(Typeface.DEFAULT);
		paint.setTextSize(dp(12));
		paint.setColor(secondaryColor);
		for (String line : lines) {
			y += dp(16);
			canvas.drawText(line, x, y, paint);
		}
	}

	@NonNull
	private String ellipsize(@NonNull String text, float maxWidth) {
		if (paint.measureText(text) <= maxWidth) return text;
		int n = paint.breakText(text, true, maxWidth - paint.measureText("…"), null);
		return text.substring(0, Math.max(0, n)) + "…";
	}

	@NonNull
	private List<String> wrap(@NonNull String text, float maxWidth, int maxLines) {
		List<String> lines = new ArrayList<>();
		String rest = text.trim();
		while (!rest.isEmpty() && lines.size() < maxLines) {
			int n = paint.breakText(rest, true, maxWidth, null);
			if (n < rest.length()) {
				int space = rest.lastIndexOf(' ', n);
				if (space > 0) n = space;
			}
			String line = rest.substring(0, n).trim();
			rest = rest.substring(n).trim();
			if (lines.size() == maxLines - 1 && !rest.isEmpty()) {
				line = ellipsize(line + " " + rest, maxWidth);
				rest = "";
			}
			lines.add(line);
		}
		return lines;
	}

	private float dp(float value) {
		return value * density;
	}
}
