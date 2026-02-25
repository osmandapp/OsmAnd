package net.osmand.plus.views.mapwidgets.widgets;

import static net.osmand.plus.utils.OsmAndFormatterParams.NO_TRAILING_ZEROS;

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
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.controls.ViewChangeProvider;
import net.osmand.plus.widgets.FrameLayoutEx;

public class RulerWidget extends FrameLayoutEx implements ViewChangeProvider {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final OsmandMapTileView mapTileView;

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

	public RulerWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		this.app = AndroidUtils.getApp(context);
		this.settings = app.getSettings();
		this.mapTileView = app.getOsmandMap().getMapView();
		this.cacheMapDensity = settings.MAP_DENSITY.get();
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

	public void updateTextSize(boolean isNight, int textColor, int textShadowColor, int shadowRadius) {
		TextInfoWidget.updateTextColor(text, textShadow, textColor, textShadowColor, false, shadowRadius);
		icon.setBackgroundResource(isNight ? R.drawable.ruler_night : R.drawable.ruler);
	}

	public boolean updateInfo(@NonNull RotatedTileBox tileBox) {
		boolean visible = true;
		float mapDensity = settings.MAP_DENSITY.get();
		// update cache
		if (mapTileView.isZooming() || mapTileView.isCarView()) {
			visible = false;
		} else if ((tileBox.getZoom() + tileBox.getZoomFloatPart() != cacheRulerZoom
				|| Math.abs(tileBox.getCenterTileX() - cacheRulerTileX) > 1
				|| Math.abs(tileBox.getCenterTileY() - cacheRulerTileY) > 1
				|| mapDensity != cacheMapDensity)
				&& tileBox.getPixWidth() > 0 && maxWidth > 0) {
			cacheRulerZoom = (float) (tileBox.getZoom() + tileBox.getZoomFloatPart());
			cacheRulerTileX = tileBox.getCenterTileX();
			cacheRulerTileY = tileBox.getCenterTileY();
			cacheMapDensity = mapDensity;

			double pixDensity = tileBox.getPixDensity();
			double roundedDist = OsmAndFormatter.calculateRoundedDist(maxWidth / pixDensity, app);
			int cacheRulerDistPix = (int) (pixDensity * roundedDist);

			String distance = OsmAndFormatter.getFormattedDistance((float) roundedDist, app, NO_TRAILING_ZEROS);
			text.setText(distance);
			textShadow.setText(distance);

			ViewGroup.LayoutParams params = layout.getLayoutParams();
			params.width = cacheRulerDistPix;
			layout.setLayoutParams(params);
			layout.requestLayout();
		}
		AndroidUiHelper.updateVisibility(layout, visible);

		return true;
	}
}