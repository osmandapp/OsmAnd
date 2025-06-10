package net.osmand.plus.views.layers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.LatLon;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;

public interface ChangeMarkerPositionHandler {
	@Nullable
	Object getChangeMarkerPositionObject();

	@Nullable
	IContextMenuProvider getSelectedObjectContextMenuProvider();

	void applyNewMarkerPosition(@NonNull LatLon location);

	void cancelMovingMarker();
}
