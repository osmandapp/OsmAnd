/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.PointI;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapSelectionResult;
import net.osmand.plus.views.layers.MapSelectionRules;
import net.osmand.plus.views.layers.MapTextLayer;
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Draws planned Driver Break rest stops along the active route at nearby POI locations when
 * available, with labels and map context menu support.
 */
public class RestStopMapLayer extends OsmandMapLayer implements IContextMenuProvider, MapTextProvider<RestStopMapLayer.RestStopMarker> {

	private static final int START_ZOOM = 9;

	private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Bitmap restIcon;
	private List<RestStop> restStops = Collections.emptyList();
	private List<RestStopMarker> markers = Collections.emptyList();
	private MapTextLayer mapTextLayer;

	public RestStopMapLayer(@NonNull Context context) {
		super(context);
		bitmapPaint.setFilterBitmap(true);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);
		restIcon = UiUtilities.decodeResource(view.getResources(), R.drawable.ic_driver_break);
		mapTextLayer = view.getLayerByClass(MapTextLayer.class);
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		if (view != null) {
			restIcon = UiUtilities.decodeResource(view.getResources(), R.drawable.ic_driver_break);
		}
	}

	public void setRestStops(@NonNull List<RestStop> stops) {
		restStops = new ArrayList<>(stops);
		markers = buildMarkers(stops);
	}

	@NonNull
	public List<RestStop> getRestStops() {
		return Collections.unmodifiableList(restStops);
	}

	@NonNull
	private static List<RestStopMarker> buildMarkers(@NonNull List<RestStop> stops) {
		List<RestStopMarker> result = new ArrayList<>();
		for (RestStop stop : stops) {
			if (stop.getPois().isEmpty()) {
				result.add(new RestStopMarker(stop, null));
			} else {
				for (RestStop.NearbyPoi poi : stop.getPois()) {
					result.add(new RestStopMarker(stop, poi));
				}
			}
		}
		return result;
	}

	@Override
	public void onDraw(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox, @NonNull DrawSettings settings) {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public void onPrepareBufferImage(@NonNull Canvas canvas, @NonNull RotatedTileBox tileBox, @NonNull DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		if (tileBox.getZoom() < START_ZOOM || markers.isEmpty() || restIcon == null) {
			if (mapTextLayer != null) {
				mapTextLayer.putData(this, Collections.emptyList());
			}
			return;
		}
		if (mapTextLayer != null) {
			mapTextLayer.putData(this, markers);
		}
		float iconSize = restIcon.getWidth() * getTextScale();
		float half = iconSize / 2f;
		for (RestStopMarker marker : markers) {
			LatLon location = marker.getLocation();
			int x = (int) tileBox.getPixXFromLatLon(location.getLatitude(), location.getLongitude());
			int y = (int) tileBox.getPixYFromLatLon(location.getLatitude(), location.getLongitude());
			if (!tileBox.containsPoint(x, y, (int) iconSize)) {
				continue;
			}
			canvas.drawBitmap(restIcon, null,
					new android.graphics.RectF(x - half, y - half, x + half, y + half), bitmapPaint);
		}
	}

	@Override
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result, @NonNull MapSelectionRules rules) {
		if (markers.isEmpty()) {
			return;
		}
		RotatedTileBox tileBox = result.getTileBox();
		if (tileBox.getZoom() < START_ZOOM) {
			return;
		}
		PointF point = result.getPoint();
		MapRendererView mapRenderer = getMapRenderer();
		float radius = getScaledTouchRadius(getApplication(), tileBox.getDefaultRadiusPoi()) * TOUCH_RADIUS_MULTIPLIER;
		List<PointI> touchPolygon31 = null;
		if (mapRenderer != null) {
			touchPolygon31 = NativeUtilities.getPolygon31FromPixelAndRadius(mapRenderer, point, radius);
			if (touchPolygon31 == null) {
				return;
			}
		}
		for (RestStopMarker marker : markers) {
			LatLon location = marker.getLocation();
			boolean add = mapRenderer != null
					? NativeUtilities.isPointInsidePolygon(location.getLatitude(), location.getLongitude(), touchPolygon31)
					: tileBox.isLatLonNearPixel(location.getLatitude(), location.getLongitude(),
					point.x, point.y, radius);
			if (add) {
				result.collect(marker, this);
			}
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof RestStopMarker marker) {
			return marker.getLocation();
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (!(o instanceof RestStopMarker marker)) {
			return null;
		}
		String title = marker.getDisplayName(getContext());
		String typeName = getString(R.string.driver_break_rest_stop);
		if (marker.poi != null && !Algorithms.isEmpty(marker.poi.getCategory())) {
			return new PointDescription(typeName, marker.poi.getCategory(), title);
		}
		return new PointDescription(typeName, title);
	}

	@Override
	public LatLon getTextLocation(RestStopMarker marker) {
		return marker.getLocation();
	}

	@Override
	public int getTextShift(RestStopMarker marker, RotatedTileBox rb) {
		return (int) (restIcon != null ? restIcon.getHeight() * getTextScale() : 0);
	}

	@Override
	public String getText(RestStopMarker marker) {
		return marker.getDisplayName(getContext());
	}

	@Override
	public boolean isTextVisible() {
		return view != null && view.getZoom() >= START_ZOOM + 1;
	}

	@Override
	public boolean isFakeBoldText() {
		return false;
	}

	static final class RestStopMarker {
		final RestStop restStop;
		@Nullable
		final RestStop.NearbyPoi poi;

		RestStopMarker(@NonNull RestStop restStop, @Nullable RestStop.NearbyPoi poi) {
			this.restStop = restStop;
			this.poi = poi;
		}

		@NonNull
		LatLon getLocation() {
			if (poi != null) {
				return poi.getLocation();
			}
			return restStop.getCoordinate();
		}

		@NonNull
		String getDisplayName(@NonNull Context context) {
			if (poi != null && !Algorithms.isEmpty(poi.getName())) {
				return poi.getName();
			}
			return context.getString(R.string.driver_break_rest_stop);
		}
	}
}
