package net.osmand.aidl;

import static net.osmand.plus.utils.OsmAndFormatterParams.NO_TRAILING_ZEROS;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * What a flyover video shows over the map: the OsmAnd elements asked for in the spec (profile icon, ruler, compass)
 * and the OpenStreetMap attribution. Drawn at the video's size; sizes are in dp, see {@link #uiDensity}.
 */
class FlyoverOverlay {

	static final String PROFILE_ICON = "profile_icon";
	static final String RULER = "ruler";
	static final String COMPASS = "compass";

	// short form of the attribution the OSM licence asks for, with the app that drew the map
	private static final String ATTRIBUTION = "OsmAnd · © OpenStreetMap";
	private static final float MARGIN_DP = 12;
	private static final float BUTTON_DP = 44;
	private static final float RULER_MAX_DP = 120;

	private final OsmandApplication app;
	private final float density;
	private final boolean nightMode;
	private final Set<String> elements = new HashSet<>();
	private final String attribution;
	private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	@Nullable
	private final Drawable compassIcon;
	@Nullable
	private final Drawable profileIcon;
	@Nullable
	private final Drawable rulerImage;
	@Nullable
	private final Drawable headIcon;

	FlyoverOverlay(@NonNull OsmandApplication app, @NonNull JSONObject spec, int width, int height) {
		this.app = app;
		this.density = uiDensity(spec, width, height);
		this.nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		attribution = spec.optString("attribution", ATTRIBUTION).trim().isEmpty()
				? ATTRIBUTION : spec.optString("attribution", ATTRIBUTION);
		JSONArray list = spec.optJSONArray("osmand_widgets");
		for (int i = 0; list != null && i < list.length(); i++) {
			elements.add(list.optString(i));
		}
		compassIcon = elements.contains(COMPASS)
				? ContextCompat.getDrawable(app, nightMode ? R.drawable.ic_compass_white : R.drawable.ic_compass) : null;
		ApplicationMode mode = getProfile(spec.optString("profile"));
		profileIcon = elements.contains(PROFILE_ICON) ? copy(app.getUIUtilities()
				.getPaintedIcon(mode.getIconRes(), mode.getProfileColor(nightMode))) : null;
		headIcon = copy(app.getUIUtilities().getPaintedIcon(mode.getIconRes(), mode.getProfileColor(nightMode)));
		rulerImage = elements.contains(RULER)
				? ContextCompat.getDrawable(app, nightMode ? R.drawable.ruler_night : R.drawable.ruler) : null;
	}

	// painted icons are cached and shared with the app's own views
	@Nullable
	private Drawable copy(@Nullable Drawable drawable) {
		Drawable.ConstantState state = drawable != null ? drawable.getConstantState() : null;
		return state != null ? state.newDrawable(app.getResources()).mutate() : drawable;
	}

	@NonNull
	private ApplicationMode getProfile(@Nullable String key) {
		ApplicationMode mode = key == null || key.isEmpty() ? null : ApplicationMode.valueOfStringKey(key, null);
		return mode != null ? mode : app.getSettings().getApplicationMode();
	}

	/**
	 * @param zoom   camera zoom, for the ruler
	 * @param latitude camera latitude, for the ruler
	 * @param rotation map rotation, degrees: the compass needle turns with it as in OsmAnd
	 */
	void draw(@NonNull Canvas canvas, double latitude, float zoom, float rotation) {
		float margin = dp(MARGIN_DP);
		float button = dp(BUTTON_DP);
		if (profileIcon != null) {
			drawButton(canvas, profileIcon, margin, margin, button, 0);
		}
		if (compassIcon != null) {
			drawButton(canvas, compassIcon, canvas.getWidth() - margin - button, margin, button, rotation);
		}
		if (rulerImage != null) {
			drawRuler(canvas, latitude, zoom, margin);
		}
		drawAttribution(canvas, margin);
	}

	/** The marker at a track's current point: a dot, an arrow along the track, or the profile icon. */
	void drawHead(@NonNull Canvas canvas, @NonNull PointF at, @Nullable PointF ahead, @NonNull String style,
	              @Nullable Integer color) {
		int c = color != null ? color | 0xFF000000 : 0xFFFF5A2B;
		if ("profile_icon".equals(style) && headIcon != null) {
			float size = dp(36);
			drawButton(canvas, headIcon, at.x - size / 2, at.y - size / 2, size, 0);
			return;
		}
		paint.setStyle(Paint.Style.FILL);
		if ("arrow".equals(style) && ahead != null && (ahead.x != at.x || ahead.y != at.y)) {
			float angle = (float) Math.toDegrees(Math.atan2(ahead.x - at.x, at.y - ahead.y));
			canvas.save();
			canvas.rotate(angle, at.x, at.y);
			Path arrow = new Path();
			arrow.moveTo(at.x, at.y - dp(13));
			arrow.lineTo(at.x + dp(10), at.y + dp(10));
			arrow.lineTo(at.x, at.y + dp(5));
			arrow.lineTo(at.x - dp(10), at.y + dp(10));
			arrow.close();
			paint.setColor(Color.WHITE);
			paint.setShadowLayer(dp(3), 0, dp(1), 0x60000000);
			canvas.drawPath(arrow, paint);
			paint.clearShadowLayer();
			canvas.scale(0.72f, 0.72f, at.x, at.y + dp(1));
			paint.setColor(c);
			canvas.drawPath(arrow, paint);
			canvas.restore();
			return;
		}
		paint.setColor(Color.WHITE);
		paint.setShadowLayer(dp(3), 0, dp(1), 0x60000000);
		canvas.drawCircle(at.x, at.y, dp(9), paint);
		paint.clearShadowLayer();
		paint.setColor(c);
		canvas.drawCircle(at.x, at.y, dp(6), paint);
	}

	private void drawButton(@NonNull Canvas canvas, @NonNull Drawable icon, float left, float top, float size, float rotation) {
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(ColorUtilities.getMapButtonBackgroundColor(app, nightMode));
		paint.setShadowLayer(dp(3), 0, dp(1), 0x40000000);
		canvas.drawCircle(left + size / 2, top + size / 2, size / 2, paint);
		paint.clearShadowLayer();
		int iconSize = Math.round(dp(24));
		int x = Math.round(left + (size - iconSize) / 2);
		int y = Math.round(top + (size - iconSize) / 2);
		icon.setBounds(x, y, x + iconSize, y + iconSize);
		canvas.save();
		canvas.rotate(rotation, left + size / 2, top + size / 2);
		icon.draw(canvas);
		canvas.restore();
	}

	/** As OsmAnd's ruler: the longest round distance up to 120 dp, measured at the camera center. */
	private void drawRuler(@NonNull Canvas canvas, double latitude, float zoom, float margin) {
		double metersPerPixel = 156543.03392 * Math.cos(Math.toRadians(latitude)) / Math.pow(2, zoom) / density;
		double roundedDist = OsmAndFormatter.calculateRoundedDist(dp(RULER_MAX_DP) * metersPerPixel, app);
		float widthPx = (float) (roundedDist / metersPerPixel);
		String text = OsmAndFormatter.getFormattedDistance((float) roundedDist, app, NO_TRAILING_ZEROS);

		float bottom = canvas.getHeight() - margin;
		float barHeight = dp(8);
		rulerImage.setBounds(Math.round(margin), Math.round(bottom - barHeight), Math.round(margin + widthPx), Math.round(bottom));
		rulerImage.draw(canvas);

		paint.setTextSize(dp(15));
		paint.setTypeface(Typeface.DEFAULT_BOLD);
		paint.setTextAlign(Paint.Align.CENTER);
		float textX = margin + widthPx / 2;
		float textY = bottom - barHeight - dp(4);
		int textColor = ColorUtilities.getColor(app, nightMode ? R.color.widgettext_night : R.color.widgettext_day);
		drawOutlinedText(canvas, text, textX, textY, textColor, nightMode ? Color.BLACK : Color.WHITE);
	}

	private void drawAttribution(@NonNull Canvas canvas, float margin) {
		paint.setTextSize(dp(8));
		paint.setTypeface(Typeface.DEFAULT);
		paint.setTextAlign(Paint.Align.RIGHT);
		float padding = dp(3);
		float textWidth = paint.measureText(attribution);
		float right = canvas.getWidth() - margin;
		float bottom = canvas.getHeight() - margin;
		RectF bg = new RectF(right - textWidth - 2 * padding, bottom - dp(8) - 2 * padding, right, bottom);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(nightMode ? 0x99000000 : 0x99FFFFFF);
		canvas.drawRoundRect(bg, padding, padding, paint);
		paint.setColor(nightMode ? 0xFFDDDDDD : 0xFF333333);
		canvas.drawText(attribution, right - padding, bottom - padding - paint.descent(), paint);
	}

	private void drawOutlinedText(@NonNull Canvas canvas, @NonNull String text, float x, float y, int color, int outline) {
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(dp(3));
		paint.setColor(outline);
		canvas.drawText(text, x, y, paint);
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(color);
		canvas.drawText(text, x, y, paint);
	}

	/** Video pixels per dp of the overlay: the frame's long side is 800 dp, like a tablet; ui_scale makes it all bigger. */
	static float uiDensity(@NonNull JSONObject spec, int width, int height) {
		return Math.max(width, height) / 800f * (float) spec.optDouble("ui_scale", 1);
	}

	private float dp(float value) {
		return value * density;
	}
}
