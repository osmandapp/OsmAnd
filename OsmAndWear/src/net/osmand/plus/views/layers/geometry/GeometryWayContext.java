package net.osmand.plus.views.layers.geometry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import net.osmand.core.android.MapRendererView;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.views.layers.base.OsmandMapLayer.RenderingLineAttributes;

public abstract class GeometryWayContext {

	public static final int DEFAULT_SIMPLIFICATION_ZOOM = 16;

	private final Context ctx;

	private final float density;
	private boolean nightMode;
	private int simplificationZoom = DEFAULT_SIMPLIFICATION_ZOOM;
	private boolean mapRendererEnabled = true;

	private final Paint paintIcon;
	private final Paint paintIconCustom;

	private RenderingLineAttributes attrs;

	private final Bitmap arrowBitmap;

	public GeometryWayContext(@NonNull Context ctx, float density) {
		this.ctx = ctx;
		this.density = density;

		paintIcon = new Paint();
		paintIcon.setFilterBitmap(true);
		paintIcon.setAntiAlias(true);
		paintIcon.setColor(Color.BLACK);
		paintIcon.setStrokeWidth(density);

		paintIconCustom = new Paint();
		paintIconCustom.setFilterBitmap(true);
		paintIconCustom.setAntiAlias(true);
		paintIconCustom.setColor(Color.BLACK);
		paintIconCustom.setStrokeWidth(density);

		float scale = getApp().getOsmandMap().getCarDensityScaleCoef();
		arrowBitmap = RenderingIcons.getBitmapFromVectorDrawable(ctx, getArrowBitmapResId(), scale);
	}

	@NonNull
	public OsmandApplication getApp() {
		return (OsmandApplication) ctx.getApplicationContext();
	}

	@NonNull
	public Context getCtx() {
		return ctx;
	}

	public boolean hasMapRenderer() {
		return mapRendererEnabled && getMapRenderer() != null;
	}

	public void enableMapRenderer() {
		mapRendererEnabled = true;
	}

	public void disableMapRenderer() {
		mapRendererEnabled = false;
	}

	@Nullable
	public MapRendererView getMapRenderer() {
		return mapRendererEnabled ? getApp().getOsmandMap().getMapView().getMapRenderer() : null;
	}

	public RenderingLineAttributes getAttrs() {
		return attrs;
	}

	public float getDensity() {
		return density;
	}

	public boolean isNightMode() {
		return nightMode;
	}

	public int getSimplificationZoom() {
		return simplificationZoom;
	}

	public void setSimplificationZoom(int simplificationZoom) {
		this.simplificationZoom = simplificationZoom;
	}

	@DrawableRes
	protected abstract int getArrowBitmapResId();

	public boolean setNightMode(boolean nightMode) {
		boolean changed = this.nightMode != nightMode;
		this.nightMode = nightMode;
		return changed;
	}

	public void updatePaints(boolean nightMode, @NonNull RenderingLineAttributes attrs) {
		this.attrs = attrs;
		int color = attrs.paint2.getColor();
		paintIcon.setColor(color);
		paintIcon.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
		this.nightMode = nightMode;
		recreateBitmapsInternal();
	}

	protected boolean hasAttrs() {
		return attrs != null;
	}

	protected void recreateBitmaps() {
	}

	private void recreateBitmapsInternal() {
		if (hasAttrs()) {
			recreateBitmaps();
		}
	}

	public void clearCustomColor() {
		if (hasAttrs()) {
			attrs.customColor = 0;
		}
	}

	public void clearCustomShader() {
		if (hasAttrs()) {
			attrs.customColorPaint.setShader(null);
		}
	}

	public int getStrokeColor(int sourceColor) {
		return ColorUtils.blendARGB(sourceColor, Color.BLACK, 0.6f);
	}

	public Paint getPaintIcon() {
		return paintIcon;
	}

	public Paint getPaintIconCustom() {
		return paintIconCustom;
	}

	public Bitmap getArrowBitmap() {
		return arrowBitmap;
	}

}
