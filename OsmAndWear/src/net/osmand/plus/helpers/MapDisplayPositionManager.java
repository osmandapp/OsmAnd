package net.osmand.plus.helpers;

import android.graphics.PointF;
import android.graphics.Rect;
import android.view.View;

import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.MapPosition;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.OsmandMapTileView.ViewportListener;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MapDisplayPositionManager implements ViewportListener {

	private OsmandMapTileView mapView;
	private final OsmandApplication app;
	private final OsmandSettings settings;
	private List<IMapDisplayPositionProvider> displayPositionProviders = new ArrayList<>();
	private List<ICoveredScreenRectProvider> coveredScreenRectProviders = new ArrayList<>();

	@NonNull
	private MapPosition mapPosition = MapPosition.CENTER;
	@NonNull
	private PointF actualMapRatio;
	private float customMapRatioX;
	private float customMapRatioY;
	private boolean shiftedX;

	@Nullable
	private Rect visibleMapRect;
	@Nullable
	private PointF projectedMapRatio;

	public MapDisplayPositionManager(@NonNull OsmandApplication app) {
		this.app = app;
		this.settings = app.getSettings();
		this.actualMapRatio = mapPosition.getRatio(false, isRtl());
	}

	@NonNull
	public PointF getMapRatio() {
		return actualMapRatio;
	}

	public boolean hasCustomMapRatio() {
		return customMapRatioX != 0 || customMapRatioY != 0;
	}

	public void restoreMapRatio() {
		setCustomMapRatio(0, 0);
	}

	public void setCustomMapRatio(float ratioX, float ratioY) {
		customMapRatioX = ratioX;
		customMapRatioY = ratioY;
		updateMapDisplayPosition();
	}

	public void setMapPositionShiftedX(boolean shifted) {
		shiftedX = shifted;
		updateMapDisplayPosition();
	}

	@NonNull
	public MapPosition getNavigationMapPosition() {
		return getPositionFromPreferences();
	}

	@Nullable
	public PointF projectRatioToVisibleMapRect(@NonNull PointF ratio) {
		if (visibleMapRect == null || mapView == null) {
			return null;
		}

		RotatedTileBox tileBox = mapView.getRotatedTileBox();
		float projectedRatioX = (visibleMapRect.left + visibleMapRect.width() * ratio.x) / tileBox.getPixWidth();
		float projectedRatioY = (visibleMapRect.top + visibleMapRect.height() * ratio.y) / tileBox.getPixHeight();
		return new PointF(projectedRatioX, projectedRatioY);
	}

	public void setMapView(@Nullable OsmandMapTileView mapView) {
		if (this.mapView != null) {
			this.mapView.removeViewportListener(this);
		}
		this.mapView = mapView;
		if (mapView != null) {
			mapView.addViewportListener(this);
		}
	}

	public void updateMapPositionProviders(@NonNull IMapDisplayPositionProvider provider, boolean shouldRegister) {
		if (shouldRegister) {
			registerMapPositionProvider(provider);
		} else {
			unregisterMapPositionProvider(provider);
		}
	}

	public void registerMapPositionProvider(@NonNull IMapDisplayPositionProvider provider) {
		if (!displayPositionProviders.contains(provider)) {
			displayPositionProviders = CollectionUtils.addToList(displayPositionProviders, provider);
		}
	}

	public void unregisterMapPositionProvider(@NonNull IMapDisplayPositionProvider provider) {
		displayPositionProviders = CollectionUtils.removeFromList(displayPositionProviders, provider);
	}

	public void updateCoveredScreenRectProvider(@NonNull ICoveredScreenRectProvider provider, boolean register) {
		if (register) {
			registerCoveredScreenRectProvider(provider);
		} else {
			unregisterCoveredScreenRectProvider(provider);
		}
	}

	public void registerCoveredScreenRectProvider(@NonNull ICoveredScreenRectProvider provider) {
		if (!coveredScreenRectProviders.contains(provider)) {
			coveredScreenRectProviders = CollectionUtils.addToList(coveredScreenRectProviders, provider);
		}
	}

	public void unregisterCoveredScreenRectProvider(@NonNull ICoveredScreenRectProvider provider) {
		coveredScreenRectProviders = CollectionUtils.removeFromList(coveredScreenRectProviders, provider);
	}

	public void updateMapDisplayPosition() {
		updateMapDisplayPosition(false);
	}

	public void updateMapDisplayPosition(boolean refreshMap) {
		if (mapView != null) {
			updateMapDisplayPositionImpl(refreshMap);
		}
	}

	private void updateMapDisplayPositionImpl(boolean shouldRefreshMap) {
		if (hasCustomMapRatio()) {
			clearVisibleMapRectData();
		} else {
			MapPosition positionFromProviders = getPositionFromProviders();
			if (positionFromProviders != null) {
				mapPosition = positionFromProviders;
				clearVisibleMapRectData();
			} else {
				mapPosition = getPositionFromPreferences();
				visibleMapRect = calculateVisibleMapRect();
				projectedMapRatio = projectRatioToVisibleMapRect(mapPosition.getRatio(shiftedX, isRtl()));
			}
		}

		PointF previousActualMapRatio = actualMapRatio;
		actualMapRatio = defineActualMapRatio();
		boolean updated = !previousActualMapRatio.equals(actualMapRatio);
		refreshMapIfNeeded(updated && shouldRefreshMap);
	}

	@Nullable
	private Rect calculateVisibleMapRect() {
		RotatedTileBox tileBox = mapView.getRotatedTileBox();
		int left = 0;
		int top = 0;
		int right = tileBox.getPixWidth();
		int bottom = tileBox.getPixHeight();

		for (ICoveredScreenRectProvider provider : coveredScreenRectProviders) {
			List<Rect> rects = provider.getCoveredScreenRects();
			for (Rect rect : rects) {
				if (rect.isEmpty()) {
					continue;
				}

				int width = right - left;
				int height = bottom - top;

				boolean leftHalf = rect.exactCenterX() < width / 2f;
				boolean topHalf = rect.exactCenterY() < height / 2f;

				int shrinkWidth = leftHalf
						? Math.max(left, rect.right) - left
						: right - Math.min(right, rect.left);
				int shrinkHeight = topHalf
						? Math.max(top, rect.bottom) - top
						: bottom - Math.min(bottom, rect.top);

				int lostAreaByWidth = shrinkWidth * height;
				int lostAreaByHeight = shrinkHeight * width;

				if (lostAreaByWidth < lostAreaByHeight) {
					if (leftHalf) {
						left += shrinkWidth;
					} else {
						right -= shrinkWidth;
					}
				} else {
					if (topHalf) {
						top += shrinkHeight;
					} else {
						bottom -= shrinkHeight;
					}
				}

				if (left >= right || top >= bottom) {
					return null;
				}
			}
		}

		return new Rect(left, top, right, bottom);
	}

	private void refreshMapIfNeeded(boolean shouldRefreshMap) {
		if (!shouldRefreshMap) {
			return;
		}
		if (mapView != null) {
			MapActivity mapActivity = mapView.getMapActivity();
			if (mapActivity != null) {
				mapActivity.refreshMap();
			}
		}
	}

	@Nullable
	private MapPosition getPositionFromProviders() {
		for (IMapDisplayPositionProvider provider : displayPositionProviders) {
			MapPosition position = provider.getMapDisplayPosition();
			if (position != null) {
				return position;
			}
		}
		return null;
	}

	@NonNull
	private MapPosition getPositionFromPreferences() {
		if (useCenterByDefault() || (useAutomaticByDefault() && useCenterForAutomatic())) {
			return MapPosition.CENTER;
		} else {
			return MapPosition.BOTTOM;
		}
	}

	private boolean useCenterByDefault() {
		return settings.POSITION_PLACEMENT_ON_MAP.get() == OsmandSettings.POSITION_PLACEMENT_CENTER;
	}

	private boolean useAutomaticByDefault() {
		return settings.POSITION_PLACEMENT_ON_MAP.get() == OsmandSettings.POSITION_PLACEMENT_AUTOMATIC;
	}

	private boolean useCenterForAutomatic() {
		return settings.ROTATE_MAP.get() != OsmandSettings.ROTATE_MAP_BEARING;
	}

	@NonNull
	private PointF defineActualMapRatio() {
		if (hasCustomMapRatio()) {
			float ratioX = customMapRatioX != 0 ? customMapRatioX : mapPosition.getRatioX(shiftedX, isRtl());
			float ratioY = customMapRatioY != 0 ? customMapRatioY : mapPosition.getRatioY();
			return new PointF(ratioX, ratioY);
		} else if (projectedMapRatio != null) {
			return projectedMapRatio;
		} else {
			return mapPosition.getRatio(shiftedX, isRtl());
		}
	}

	private void clearVisibleMapRectData() {
		visibleMapRect = null;
		projectedMapRatio = null;
	}

	private boolean isRtl() {
		return AndroidUtils.isLayoutRtl(app);
	}

	@Override
	public void onViewportChanged() {
		updateMapDisplayPosition(true);
	}

	public interface IMapDisplayPositionProvider {
		@Nullable
		MapPosition getMapDisplayPosition();
	}

	public interface ICoveredScreenRectProvider {

		@NonNull
		List<Rect> getCoveredScreenRects();
	}

	public static class BoundsChangeListener implements View.OnLayoutChangeListener {

		@NonNull
		private final MapDisplayPositionManager displayPositionManager;
		private final boolean refreshMap;

		public BoundsChangeListener(@NonNull MapDisplayPositionManager displayPositionManager, boolean refreshMap) {
			this.displayPositionManager = displayPositionManager;
			this.refreshMap = refreshMap;
		}

		@Override
		public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
			boolean boundsUpdated = left != oldLeft
					|| top != oldTop
					|| right != oldRight
					|| bottom != oldBottom;
			if (boundsUpdated) {
				displayPositionManager.updateMapDisplayPosition(refreshMap);
			}
		}
	}
}
