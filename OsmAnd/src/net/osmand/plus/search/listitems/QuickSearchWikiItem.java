package net.osmand.plus.search.listitems;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.search.core.SearchResult;

public class QuickSearchWikiItem extends QuickSearchListItem {
	private final String title;
	private final String description;
	private final String type;
	private final Drawable icon;
	private final String imageUrl;
	private final LatLon location;

	public QuickSearchWikiItem(OsmandApplication app, SearchResult searchResult) {
		super(app, searchResult);
		Amenity amenity = (Amenity) searchResult.object;
		this.title = amenity.getName();
		this.description = amenity.getDescription(null);
		this.type = getPoiTypeTranslation(app, amenity);
		this.icon = getPoiTypeIcon(app, amenity);
		this.imageUrl = amenity.getWikiImageStubUrl();
		this.location = new LatLon(amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude());
	}

	private String getPoiTypeTranslation(OsmandApplication app, Amenity amenity) {
		PoiType subType = app.getPoiTypes().getPoiTypeByKey(amenity.getSubType());
		return subType != null ? subType.getTranslation() : "";
	}

	@NonNull
	private Drawable getPoiTypeIcon(OsmandApplication app, Amenity amenity) {
		Drawable resIcon = app.getUIUtilities().getIcon(R.drawable.ic_action_info_dark, app.getDaynightHelper().isNightMode());
		;
		PoiType subType = app.getPoiTypes().getPoiTypeByKey(amenity.getSubType());
		if (subType != null) {
			Drawable renderingIcon = app.getUIUtilities().getRenderingIcon(app, subType.getKeyName(), app.getDaynightHelper().isNightMode());
			if (renderingIcon != null) {
				resIcon = renderingIcon;
			}
		}
		return resIcon;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getTypeName() {
		return type;
	}

	public Drawable getIcon() {
		return icon;
	}

	public String getImage() {
		return imageUrl;
	}

	public LatLon getLocation() {
		return location;
	}
}