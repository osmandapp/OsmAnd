package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.NativeLibrary.RenderedObject;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapLayers;
import net.osmand.search.AmenitySearcher;

import java.lang.ref.WeakReference;

public class RenderedObjectMenuBuilder extends AmenityMenuBuilder {

	private RenderedObject renderedObject;

	public RenderedObjectMenuBuilder(@NonNull MapActivity mapActivity,
			@NonNull RenderedObject renderedObject) {
		super(mapActivity, getSyntheticAmenity(mapActivity, renderedObject));
		this.renderedObject = renderedObject;
	}

	public void updateRenderedObject(@NonNull RenderedObject renderedObject) {
		this.renderedObject = renderedObject;
		setAmenity(getSyntheticAmenity(mapActivity, renderedObject));
	}

	@Override
	public void build(@NonNull ViewGroup view, @Nullable Object object) {
		searchAmenity(view, object);
	}

	private void searchAmenity(@NonNull ViewGroup view, @Nullable Object object) {
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(view);
		AmenitySearcher searcher = app.getResourceManager().getAmenitySearcher();
		AmenitySearcher.Settings settings = app.getResourceManager().getDefaultAmenitySearchSettings();

		searcher.searchBaseDetailedObjectAsync(renderedObject, settings, detailsObject -> {
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
		}, new ResultMatcher<>() {
			@Override
			public boolean publish(Amenity object) {
				return true;
			}

			@Override
			public boolean isCancelled() {
				ViewGroup viewGroup = viewGroupRef.get();
				return viewGroup == null || mapContextMenu == null;
			}
		});
	}

	private static Amenity getSyntheticAmenity(@NonNull MapActivity mapActivity,
											   @NonNull RenderedObject renderedObject) {
		return BaseDetailsObject.convertRenderedObjectToAmenity(renderedObject, mapActivity.getApp().getPoiTypes());
	}

}
