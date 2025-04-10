package net.osmand.plus.views.layers;

import android.graphics.PointF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapSelectionResult {

	private final PointF point;
	private final LatLon pointLatLon;
	private final RotatedTileBox tileBox;
	private final Map<Object, IContextMenuProvider> selectedObjects = new LinkedHashMap<>();

	protected LatLon objectLatLon;

	public MapSelectionResult(@NonNull OsmandApplication app,
			@NonNull RotatedTileBox tileBox, @NonNull PointF point) {
		this.point = point;
		this.tileBox = tileBox;
		this.pointLatLon = NativeUtilities.getLatLonFromElevatedPixel(
				app.getOsmandMap().getMapView().getMapRenderer(), tileBox, point);
	}

	@NonNull
	public PointF getPoint() {
		return point;
	}

	@NonNull
	public LatLon getPointLatLon() {
		return pointLatLon;
	}

	@NonNull
	public RotatedTileBox getTileBox() {
		return tileBox;
	}

	@NonNull
	public List<Object> getObjects() {
		return new ArrayList<>(selectedObjects.keySet());
	}

	@NonNull
	public Map<Object, IContextMenuProvider> getObjectsWithProviders() {
		return selectedObjects;
	}

	@Nullable
	public LatLon getObjectLatLon() {
		return objectLatLon;
	}

	public void setObjectLatLon(@Nullable LatLon objectLatLon) {
		this.objectLatLon = objectLatLon;
	}

	public void collect(@NonNull Object object, @Nullable IContextMenuProvider provider) {
		selectedObjects.put(object, provider);
	}
}
