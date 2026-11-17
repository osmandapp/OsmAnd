package net.osmand.plus.plugins.development.widget;

import static net.osmand.plus.views.mapwidgets.WidgetType.DEV_ZOOM_LEVEL;

import android.util.DisplayMetrics;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.Zoom;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.ZoomLevelWidgetState;
import net.osmand.plus.views.mapwidgets.widgetstates.ZoomLevelWidgetState.ZoomLevelType;

public class ZoomLevelWidget extends SimpleWidget {

	private static final int ZOOM_OFFSET_FROM_31 = 17;
	private static final int MAX_RATIO_DIGITS = 3;

	private final OsmandMap osmandMap;
	private final OsmandMapTileView mapView;

	private final ZoomLevelWidgetState widgetState;

	@Nullable
	private ZoomLevelType cachedZoomLevelType;
	private int cachedBaseZoom;
	private int cachedZoom;
	private float cachedZoomFloatPart;
	private float cachedMapDensity;
	private int cachedCenterX = -1;
	private int cachedCenterY = -1;

	public ZoomLevelWidget(@NonNull MapActivity mapActivity, @NonNull ZoomLevelWidgetState widgetState,
	                       @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, DEV_ZOOM_LEVEL, customId, widgetsPanel);
		this.osmandMap = app.getOsmandMap();
		this.mapView = mapActivity.getMapView();
		this.widgetState = widgetState;
		updateInfo(null);
		setIcons(DEV_ZOOM_LEVEL);
		setOnClickListener(getOnClickListener());
	}

	@NonNull
	public OsmandPreference<ZoomLevelType> getZoomLevelTypePref() {
		return widgetState.getZoomLevelTypePref();
	}

	@Override
	protected OnClickListener getOnClickListener() {
		return v -> {
			widgetState.changeToNextState();
			updateInfo(null);
			updateWidgetName();
		};
	}

	@Nullable
	@Override
	protected String getWidgetName() {
		return widgetState != null ? widgetState.getTitle() : null;
	}

	@Nullable
	@Override
	public ZoomLevelWidgetState getWidgetState() {
		return widgetState;
	}

	@Override
	public void copySettingsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copySettingsFromMode(sourceAppMode, appMode, customId);
		widgetState.copyPrefsFromMode(sourceAppMode, appMode, customId);
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		RotatedTileBox tileBox = mapView.getRotatedTileBox();
		int newCenterX = tileBox.getCenter31X() >> ZOOM_OFFSET_FROM_31;
		int newCenterY = tileBox.getCenter31Y() >> ZOOM_OFFSET_FROM_31;
		ZoomLevelType newZoomLevelType = widgetState.getZoomLevelType();
		int baseZoom = mapView.getBaseZoom();
		int newZoom = mapView.getZoom();
		float newZoomFloatPart = mapView.getZoomFloatPart() + mapView.getZoomAnimation();
		float newMapDensity = osmandMap.getMapDensity();

		boolean update = isUpdateNeeded()
				|| newZoomLevelType != cachedZoomLevelType
				|| baseZoom != cachedBaseZoom
				|| newZoom != cachedZoom
				|| newZoomFloatPart != cachedZoomFloatPart
				|| newMapDensity != cachedMapDensity
				|| newZoomLevelType == ZoomLevelType.MAP_SCALE && (newCenterX != cachedCenterX || newCenterY != cachedCenterY);
		if (update) {
			cachedCenterX = newCenterX;
			cachedCenterY = newCenterY;
			cachedZoomLevelType = newZoomLevelType;
			cachedBaseZoom = baseZoom;
			cachedZoom = newZoom;
			cachedZoomFloatPart = newZoomFloatPart;
			cachedMapDensity = newMapDensity;

			switch (newZoomLevelType) {
				case MAP_SCALE:
					setMapScaleText();
					break;
				case ZOOM:
				default:
					setZoomLevelText(baseZoom, newZoom, newZoomFloatPart, newMapDensity);
					break;
			}
		}
	}

	private void setMapScaleText() {
		int mapScale = calculateMapScale();
		FormattedValue formattedMapScale = formatMapScale(mapScale);
		String mapScaleStr = getString(R.string.ltr_or_rtl_combine_via_colon_with_space, "1", formattedMapScale.value);
		setText(mapScaleStr, formattedMapScale.unit);
	}

	private int calculateMapScale() {
		DisplayMetrics metrics = new DisplayMetrics();
		AndroidUtils.getDisplay(mapActivity).getMetrics(metrics);

		RotatedTileBox tileBox = mapView.getRotatedTileBox();
		int pixWidth = tileBox.getPixWidth();
		int pixHeight = tileBox.getPixHeight();

		float averageRealDpi = (metrics.xdpi + metrics.ydpi) / 2.0f;
		float pixelsPerMeter = averageRealDpi * 100 / 2.54f;
		double realScreenWidthInMeters = (double) pixWidth / pixelsPerMeter;
		double mapScreenWidthInMeters = tileBox.getDistance(0, pixHeight / 2, pixWidth, pixHeight / 2);
		return (int) (mapScreenWidthInMeters / realScreenWidthInMeters);
	}

	@NonNull
	private FormattedValue formatMapScale(int mapScale) {
		int digitsCount = (int) (Math.log10(mapScale) + 1);
		if (digitsCount >= 7) {
			return formatBigMapScale(mapScale, digitsCount, 6, "M");
		} else if (digitsCount >= 4) {
			return formatBigMapScale(mapScale, digitsCount, 3, "K");
		} else {
			return OsmAndFormatter.formatIntegerValue(mapScale, "", app);
		}
	}

	@NonNull
	private FormattedValue formatBigMapScale(int mapScale, int digits, int insignificantDigits, @NonNull String unit) {
		int intDigits = digits - insignificantDigits;
		int fractionalDigits = Math.max(0, MAX_RATIO_DIGITS - intDigits);
		int removeExcessiveDigits = mapScale / (int) Math.pow(10, insignificantDigits - fractionalDigits);
		float roundedMapScale = (float) (removeExcessiveDigits / Math.pow(10, fractionalDigits));
		return OsmAndFormatter.formatValue(roundedMapScale, unit, true, fractionalDigits, app);
	}

	private void setZoomLevelText(int zoomBaseWithOffset, int zoomBase, float zoomFraction, float mapDensity) {
		float visualZoom = Zoom.floatPartToVisual(zoomFraction);
		float targetPixelScale = (float) Math.pow(2.0, zoomBase - zoomBaseWithOffset);
		float offsetFromLogicalZoom = getZoomDeltaFromMapScale(targetPixelScale * visualZoom * mapDensity);
		float preFormattedOffset = Math.round(Math.abs(offsetFromLogicalZoom) * 100) / 100.0f;
		String formattedOffset = OsmAndFormatter
				.formatValue(preFormattedOffset, "", true, 2, app)
				.value;
		String sign = offsetFromLogicalZoom < 0 ? "-" : "+";
		setText(String.valueOf(zoomBaseWithOffset), sign + formattedOffset);
	}

	private float getZoomDeltaFromMapScale(float mapScale) {
		double log2 = Math.log(mapScale) / Math.log(2);
		boolean powerOfTwo = Math.abs(log2 - (int) log2) < 0.001;

		if (powerOfTwo) {
			return (int) Math.round(log2);
		}

		int prevIntZoom;
		int nextIntZoom;
		if (mapScale >= 1.0f) {
			prevIntZoom = (int) log2;
			nextIntZoom = prevIntZoom + 1;
		} else {
			nextIntZoom = (int) log2;
			prevIntZoom = nextIntZoom - 1;
		}

		float prevPowZoom = (float) Math.pow(2, prevIntZoom);
		float nextPowZoom = (float) Math.pow(2, nextIntZoom);
		double zoomFloatPart = Math.abs(mapScale - prevPowZoom) / (nextPowZoom - prevPowZoom);
		return (float) (prevIntZoom + zoomFloatPart);
	}
}
