package net.osmand.plus.views.mapwidgets.widgets;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;

public class RulerWidget {

	private MapActivity mapActivity;
	private OsmandPreference<Float> mapDensity;

	private View layout;
	private ImageView icon;
	private TextView text;
	private TextView textShadow;

	private String cacheRulerText;
	private int maxWidth;
	private int cacheRulerZoom;
	private float cacheMapDensity;
	private double cacheRulerTileX;
	private double cacheRulerTileY;

	public RulerWidget(MapActivity mapActivity, View view) {
		this.mapActivity = mapActivity;
		layout = view.findViewById(R.id.map_ruler_layout);
		icon = view.findViewById(R.id.map_ruler_image);
		text = view.findViewById(R.id.map_ruler_text);
		textShadow = view.findViewById(R.id.map_ruler_text_shadow);
		maxWidth = view.getResources().getDimensionPixelSize(R.dimen.map_ruler_width);
		mapDensity = mapActivity.getMyApplication().getSettings().MAP_DENSITY;
		cacheMapDensity = mapDensity.get();
	}

	public void updateTextSize(boolean isNight, int textColor, int textShadowColor, int shadowRadius) {
		TextInfoWidget.updateTextColor(text, textShadow, textColor, textShadowColor, false, shadowRadius);
		icon.setBackgroundResource(isNight ? R.drawable.ruler_night : R.drawable.ruler);
	}

	public boolean updateInfo(RotatedTileBox tb, DrawSettings nightMode) {
		boolean visible = true;
		OsmandMapTileView view = mapActivity.getMapView();
		// update cache
		if (view.isZooming()) {
			visible = false;
		} else if (!tb.isZoomAnimated() && (tb.getZoom() != cacheRulerZoom || Math.abs(tb.getCenterTileX() - cacheRulerTileX) > 1 || Math
				.abs(tb.getCenterTileY() - cacheRulerTileY) > 1 || mapDensity.get() != cacheMapDensity) &&
				tb.getPixWidth() > 0 && maxWidth > 0) {
			cacheRulerZoom = tb.getZoom();
			cacheRulerTileX = tb.getCenterTileX();
			cacheRulerTileY = tb.getCenterTileY();
			cacheMapDensity = mapDensity.get();
			double pixDensity = tb.getPixDensity();
			double roundedDist = OsmAndFormatter.calculateRoundedDist(maxWidth /
					pixDensity, view.getApplication());

			int cacheRulerDistPix = (int) (pixDensity * roundedDist);
			cacheRulerText = OsmAndFormatter.getFormattedDistance((float) roundedDist, view.getApplication(), false);
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
}