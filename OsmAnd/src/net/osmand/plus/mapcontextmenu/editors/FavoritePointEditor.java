package net.osmand.plus.mapcontextmenu.editors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.NativeLibrary.RenderedObject;
import net.osmand.binary.ObfConstants;
import net.osmand.data.Amenity;
import net.osmand.data.BaseDetailsObject;
import net.osmand.data.FavouritePoint;
import net.osmand.data.LatLon;
import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.Entity.EntityType;
import net.osmand.osm.edit.OSMSettings.OSMTagKey;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.myplaces.favorites.FavoriteGroup;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.plugins.osmedit.data.OpenstreetmapPoint;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.search.AmenitySearcher;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.util.Algorithms;

public class FavoritePointEditor extends PointEditor {

	public static final String TAG = FavoritePointEditor.class.getSimpleName();

	private FavouritePoint favorite;

	public FavoritePointEditor(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	public FavoritePointEditor(@NonNull OsmandApplication app) {
		super(app);
	}

	@Override
	public boolean isProcessingTemplate() {
		return false;
	}

	@Nullable
	@Override
	public String getPreselectedIconName() {
		return isNew && favorite != null ? RenderingIcons.getBigIconName(favorite.getIconId()) : null;
	}

	@Override
	public String getFragmentTag() {
		return TAG;
	}

	public FavouritePoint getFavorite() {
		return favorite;
	}

	public void add(LatLon latLon, String title, String address, Object object) {
		MapActivity mapActivity = getMapActivity();
		if (latLon == null || mapActivity == null) {
			return;
		}

		isNew = true;
		String lastCategory = app.getSettings().LAST_FAV_CATEGORY_ENTERED.get();
		if (!Algorithms.isEmpty(lastCategory) && !app.getFavoritesHelper().groupExists(lastCategory)) {
			lastCategory = "";
		}
		double altitude = Double.NaN;
		if (object instanceof WptPt) {
			altitude = ((WptPt) object).getEle();
		}
		favorite = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), title, lastCategory, altitude, 0);
		favorite.setDescription("");
		if (Algorithms.isEmpty(address) && object instanceof OpenstreetmapPoint point) {
			// use the address entered in the OSM editor, reverse geocoding
			// is not available for edited points yet
			address = getEntityAddress(point.getEntity());
		}
		favorite.setAddress(address.isEmpty() ? title : address);

		Amenity amenity = null;
		if (object instanceof Amenity) {
			amenity = (Amenity) object;
		} else if (object instanceof BaseDetailsObject detailsObject) {
			amenity = detailsObject.getSyntheticAmenity();
		} else if (object instanceof OpenstreetmapPoint point) {
			Entity entity = point.getEntity();
			AmenitySearcher searcher = app.getResourceManager().getAmenitySearcher();
			AmenitySearcher.Settings settings = app.getResourceManager().getDefaultAmenitySearchSettings();

			Amenity requestAmenity = new Amenity();
			requestAmenity.setLocation(latLon);
			requestAmenity.setId(ObfConstants.createMapObjectIdFromCleanOsmId(entity.getId(), EntityType.valueOf(entity)));

			AmenitySearcher.Request request = new AmenitySearcher.Request(requestAmenity);
			amenity = searcher.searchDetailedAmenity(request, settings);
		}
		if (amenity != null) {
			setAmenity(amenity);
		} else if (object instanceof RenderedObject renderedObject) {
			setMapObject(renderedObject);
		}
		if (object instanceof OpenstreetmapPoint point) {
			// the icon should reflect the possibly modified type of the edited POI,
			// map data may still contain the previous type (or none for created POIs)
			int iconId = OsmEditingPlugin.getPoiTypeIconId(app, point.getEntity());
			if (iconId != 0) {
				favorite.setIconId(iconId);
			}
		}
		FavoritePointEditorFragment.showInstance(mapActivity);
	}

	@NonNull
	private String getEntityAddress(@NonNull Entity entity) {
		String street = entity.getTag(OSMTagKey.ADDR_STREET);
		String houseNumber = entity.getTag(OSMTagKey.ADDR_HOUSE_NUMBER);
		if (!Algorithms.isEmpty(street)) {
			return Algorithms.isEmpty(houseNumber) ? street : street + " " + houseNumber;
		}
		return "";
	}

	private void setAmenity(@NonNull Amenity amenity) {
		favorite.setAmenityOriginName(amenity.toStringEn());
		favorite.setIconId(RenderingIcons.getPreselectedIconId(app, amenity));
		favorite.setAmenityExtensions(amenity.getAmenityExtensions(app.getPoiTypes(), true));
	}

	private void setMapObject(@NonNull RenderedObject renderedObject) {
		favorite.setAmenityOriginName(renderedObject.toStringEn());
		if (renderedObject.getIconRes() != null) {
			favorite.setIconId(RenderingIcons.getResId(renderedObject.getIconRes()));
		}
	}

	public void add(LatLon latLon, String title, String categoryName, int categoryColor, boolean autoFill) {
		MapActivity mapActivity = getMapActivity();
		if (latLon == null || mapActivity == null) {
			return;
		}
		isNew = true;
		if (categoryName != null && !categoryName.isEmpty()) {
			FavoriteGroup category = mapActivity.getApp().getFavoritesHelper()
					.getGroup(categoryName);
			if (category == null) {
				mapActivity.getApp().getFavoritesHelper().addFavoriteGroup(categoryName, categoryColor);
			}
		} else {
			categoryName = "";
		}

		favorite = new FavouritePoint(latLon.getLatitude(), latLon.getLongitude(), title, categoryName);
		favorite.setDescription("");
		favorite.setAddress("");
		FavoritePointEditorFragment.showInstance(mapActivity, autoFill);
	}

	public void edit(FavouritePoint favorite) {
		MapActivity mapActivity = getMapActivity();
		if (favorite == null || mapActivity == null) {
			return;
		}
		isNew = false;
		this.favorite = favorite;
		FavoritePointEditorFragment.showInstance(mapActivity);
	}

	public void edit(@Nullable FavouritePoint favorite, @NonNull FragmentActivity activity, @NonNull Fragment targetFragment) {
		if (favorite == null) {
			return;
		}
		isNew = false;
		this.favorite = favorite;
		FavoritePointEditorFragment.showInstance(this, activity, targetFragment, false);
	}
}
