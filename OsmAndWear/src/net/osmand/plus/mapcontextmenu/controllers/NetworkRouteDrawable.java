package net.osmand.plus.mapcontextmenu.controllers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.widgets.StreetNameWidget;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

import static net.osmand.plus.render.TextRenderer.DROID_SERIF;
import static net.osmand.router.network.NetworkRouteSelector.RouteKey;

public class NetworkRouteDrawable extends Drawable {

	private final OsmandApplication app;
	private final RouteKey routeKey;

	private final Paint paint = new Paint();

	@Nullable
	private final String osmcText;
	@Nullable
	private final Drawable backgroundDrawable;

	private final int defaultIconSize;

	public NetworkRouteDrawable(@NonNull OsmandApplication app, @NonNull RouteKey routeKey, boolean nightMode) {
		this.app = app;
		this.routeKey = routeKey;
		backgroundDrawable = createBackgroundIcon();
		osmcText = routeKey.getValue("osmc_text");
		defaultIconSize = app.getResources().getDimensionPixelSize(R.dimen.standard_icon_size);
		setupTextPaint(nightMode);
	}

	@Nullable
	private Drawable createBackgroundIcon() {
		List<Drawable> icons = new ArrayList<>();
		for (OsmcIconParams iconParams : OsmcIconParams.values()) {
			Drawable icon = getIcon(iconParams.key, iconParams.prefix, iconParams.suffix);
			if (icon != null) {
				icons.add(icon);
			}
		}

		if (icons.isEmpty()) {
			return null;
		} else if (icons.size() == 1) {
			return icons.get(0);
		} else {
			return UiUtilities.getLayeredIcon(icons.toArray(new Drawable[0]));
		}
	}

	private void setupTextPaint(boolean nightMode) {
		paint.setStrokeWidth(1);
		paint.setAntiAlias(true);
		paint.setStyle(Style.FILL);
		paint.setColor(Color.BLACK);
		paint.setTextAlign(Align.CENTER);
		paint.setTextSize(AndroidUtils.spToPx(app, 13));
		paint.setTypeface(Typeface.create(DROID_SERIF, Typeface.NORMAL));
		updatePaint(nightMode);
	}

	private void updatePaint(boolean nightMode) {
		if (!Algorithms.isEmpty(osmcText)) {
			MapRenderRepositories renderer = app.getResourceManager().getRenderer();
			RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
			if (storage == null) {
				 return;
			}
			RenderingRuleSearchRequest request = renderer.getSearchRequestWithAppliedCustomRules(storage, nightMode);
			request.saveState();

			String tag = "route_" + routeKey.type.getName();
			String color = routeKey.getValue("osmc_textcolor");

			request.setInitialTagValueZoom(tag, null, 14, null);
			request.setIntFilter(request.ALL.R_TEXT_LENGTH, osmcText.length());
			request.setStringFilter(request.ALL.R_NAME_TAG, tag + "_1_osmc_text");
			request.setStringFilter(request.ALL.R_ADDITIONAL, tag + "_1_osmc_textcolor=" + color);
			request.search(RenderingRulesStorage.TEXT_RULES);

			StreetNameWidget.setupTextPaint(app, paint, request);
		}
	}

	@Nullable
	private Drawable getIcon(@NonNull String key, @NonNull String prefix, @NonNull String suffix) {
		String name = routeKey.getValue(key);
		String iconName = prefix + name + suffix;
		int iconRes = AndroidUtils.getDrawableId(app, iconName);
		if (iconRes != 0) {
			return app.getUIUtilities().getIcon(iconRes);
		}
		return null;
	}

	@Override
	public void draw(@NonNull Canvas canvas) {
		if (backgroundDrawable != null) {
			backgroundDrawable.draw(canvas);
		}

		if (!Algorithms.isEmpty(osmcText)) {
			Rect rect = getBounds();
			float x = rect.width() / 2f;
			float y = rect.height() / 2f - ((paint.descent() + paint.ascent()) / 2);
			canvas.drawText(osmcText, x, y, paint);
		}
	}

	@Override
	public int getMinimumHeight() {
		int height = getMaxTextHeight();
		if (backgroundDrawable != null) {
			height = Math.max(height, backgroundDrawable.getMinimumHeight());
		}
		return height;
	}

	@Override
	public int getMinimumWidth() {
		int width = getMaxTextWidth();
		if (backgroundDrawable != null) {
			width = Math.max(width, backgroundDrawable.getMinimumWidth());
		}
		return width;
	}

	@Override
	public int getIntrinsicHeight() {
		int height = getMaxTextHeight();
		if (backgroundDrawable != null) {
			height = Math.max(height, backgroundDrawable.getIntrinsicHeight());
		}
		return height;
	}

	@Override
	public int getIntrinsicWidth() {
		int width = getMaxTextWidth();
		if (backgroundDrawable != null) {
			width = Math.max(width, backgroundDrawable.getIntrinsicWidth());
		}
		return width;
	}

	private int getMaxTextHeight() {
		int height = defaultIconSize;
		if (!Algorithms.isEmpty(osmcText)) {
			height = Math.max(height, AndroidUtils.getTextHeight(paint));
		}
		return height;
	}

	private int getMaxTextWidth() {
		int width = defaultIconSize;
		if (!Algorithms.isEmpty(osmcText)) {
			width = Math.max(width, AndroidUtils.getTextWidth(paint.getTextSize(), osmcText));
		}
		return width;
	}

	@Override
	public void setChangingConfigurations(int configs) {
		super.setChangingConfigurations(configs);
		if (backgroundDrawable != null) {
			backgroundDrawable.setChangingConfigurations(configs);
		}
	}

	@Override
	public void setBounds(int left, int top, int right, int bottom) {
		super.setBounds(left, top, right, bottom);
		if (backgroundDrawable != null) {
			backgroundDrawable.setBounds(left, top, right, bottom);
		}
	}

	@Override
	public void setAlpha(int alpha) {
		paint.setAlpha(alpha);
		if (backgroundDrawable != null) {
			backgroundDrawable.setAlpha(alpha);
		}
	}

	@Override
	public void setColorFilter(@Nullable ColorFilter filter) {
		paint.setColorFilter(filter);
		if (backgroundDrawable != null) {
			backgroundDrawable.setColorFilter(filter);
		}
	}

	@Override
	public int getOpacity() {
		return backgroundDrawable != null ? backgroundDrawable.getOpacity() : PixelFormat.UNKNOWN;
	}

	private enum OsmcIconParams {
		BACKGROUND("osmc_background", "h_osmc_", "_bg"),
		FOREGROUND("osmc_foreground", "mm_osmc_", ""),
		FOREGROUND_2("osmc_foreground2", "mm_osmc_", "");

		@NonNull
		final String key;
		@NonNull
		final String prefix;
		@NonNull
		final String suffix;

		OsmcIconParams(@NonNull String key, @NonNull String prefix, @NonNull String suffix) {
			this.key = key;
			this.prefix = prefix;
			this.suffix = suffix;
		}
	}
}