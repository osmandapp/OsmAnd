package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.NativeLibrary.RenderedObject;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapLayers;

import java.lang.ref.WeakReference;

public class RenderedObjectMenuBuilder extends AmenityMenuBuilder {

	private final RenderedObject renderedObject;

	public RenderedObjectMenuBuilder(@NonNull MapActivity mapActivity,
			@NonNull RenderedObject renderedObject) {
		super(mapActivity, BaseDetailsObject.convertToSyntheticAmenity(renderedObject));
		this.renderedObject = renderedObject;
	}

	@Override
	public void build(@NonNull ViewGroup view, @Nullable Object object) {
		searchAmenity(view, object);
	}

	private void searchAmenity(@NonNull ViewGroup view, @Nullable Object object) {
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(view);
		app.getResourceManager().getAmenitySearcher().searchBaseDetailsObjectAsync(renderedObject, detailsObject -> {
			app.runInUIThread(() -> {
				ViewGroup viewGroup = viewGroupRef.get();
				if (viewGroup == null || mapContextMenu == null) {
					return;
				}
				if (detailsObject != null) {
					LatLon latLon = getLatLon();
					MapLayers mapLayers = mapActivity.getMapLayers();
					PointDescription description = mapLayers.getPoiMapLayer().getObjectName(detailsObject);
					mapContextMenu.update(latLon, description, detailsObject);
				} else {
					super.build(viewGroup, object);
				}
			});
			return true;
		});
	}
}
