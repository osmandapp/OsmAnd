package net.osmand.plus.views.mapwidgets.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatterParams;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.ViewChangeProvider;
import net.osmand.plus.widgets.FrameLayoutEx;

public class RulerWidget extends FrameLayoutEx implements ViewChangeProvider {

	private final OsmandApplication app;
	private final OsmandMap osmandMap;

	private View layout;
	private ImageView icon;
	private TextView text;
	private TextView textShadow;

	private int maxWidth;
	private float cacheRulerZoom;
	private float cacheMapDensity;
	private double cacheRulerTileX;
	private double cacheRulerTileY;

	public RulerWidget(@NonNull Context context) {
		this(context, null);
	}

	public RulerWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public RulerWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public RulerWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		this.app = (OsmandApplication) context.getApplicationContext();
		osmandMap = app.getOsmandMap();
		cacheMapDensity = osmandMap.getMapDensity();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		layout = findViewById(R.id.map_ruler_layout);
		icon = findViewById(R.id.map_ruler_image);
		text = findViewById(R.id.map_ruler_text);
		textShadow = findViewById(R.id.map_ruler_text_shadow);
		maxWidth = getResources().getDimensionPixelSize(R.dimen.map_ruler_width);
	}

	public void updateTextSize(boolean isNight, int textColor, int textShadowColor,
			int shadowRadius) {
		TextInfoWidget.updateTextColor(text, textShadow, textColor, textShadowColor, false, shadowRadius);
		icon.setBackgroundResource(isNight ? R.drawable.ruler_night : R.drawable.ruler);
	}

	public boolean updateInfo(@NonNull RotatedTileBox tb) {
		boolean visible = true;
		OsmandMapTileView view = osmandMap.getMapView();
		float mapDensity = osmandMap.getMapDensity();
		// update cache
		if (view.isZooming() || osmandMap.getMapView().isCarView()) {
			visible = false;
		} else if ((tb.getZoom() + tb.getZoomFloatPart() != cacheRulerZoom
				|| Math.abs(tb.getCenterTileX() - cacheRulerTileX) > 1
				|| Math.abs(tb.getCenterTileY() - cacheRulerTileY) > 1
				|| mapDensity != cacheMapDensity)
				&& tb.getPixWidth() > 0 && maxWidth > 0) {
			cacheRulerZoom = (float) (tb.getZoom() + tb.getZoomFloatPart());
			cacheRulerTileX = tb.getCenterTileX();
			cacheRulerTileY = tb.getCenterTileY();
			cacheMapDensity = mapDensity;

			double pixDensity = tb.getPixDensity();
			double roundedDist = OsmAndFormatter.calculateRoundedDist(maxWidth / pixDensity, app);

			int cacheRulerDistPix = (int) (pixDensity * roundedDist);
			String cacheRulerText = OsmAndFormatter.getFormattedDistance((float) roundedDist, app, OsmAndFormatterParams.NO_TRAILING_ZEROS);
			textShadow.setText(cacheRulerText);
			text.setText(cacheRulerText);
			ViewGroup.LayoutParams lp = layout.getLayoutParams();
			lp.width = cacheRulerDistPix;
			layout.setLayoutParams(lp);
			layout.requestLayout();
		}
		AndroidUiHelper.updateVisibility(layout, visible);
		return true;
	}

	public void setVisibility(boolean visibility) {
		AndroidUiHelper.updateVisibility(layout, visibility);
	}

	public static double getRulerDistance(@NonNull OsmandApplication app,
			@NonNull RotatedTileBox tileBox) {
		double pixDensity = tileBox.getPixDensity();
		int maxWidth = app.getResources().getDimensionPixelSize(R.dimen.map_ruler_width);
		return OsmAndFormatter.calculateRoundedDist(maxWidth / pixDensity, app);
	}
}