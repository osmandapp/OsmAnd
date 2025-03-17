package net.osmand.plus.search.listitems;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import net.osmand.data.Amenity;
import net.osmand.data.DataSourceType;
import net.osmand.data.LatLon;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

public class QuickSearchWikiItem extends QuickSearchListItem {
	private final String title;
	private final String description;
	private final String type;
	private final Drawable icon;
	private final String imageUrl;
	private final LatLon location;

	public QuickSearchWikiItem(@NonNull OsmandApplication app, @NonNull QuickSearchListItem quickSearchListItem) {
		this(app, quickSearchListItem.getSearchResult());
	}

	public QuickSearchWikiItem(@NonNull OsmandApplication app, @NonNull SearchResult searchResult) {
		super(app, searchResult);
		Amenity amenity = (Amenity) searchResult.object;
		String preferredMapLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		if (Algorithms.isEmpty(preferredMapLang)) {
			preferredMapLang = app.getLanguage();
		}
		String articleLang = PluginsHelper.onGetMapObjectsLocale(amenity, preferredMapLang);
		String lng = amenity.getContentLanguage("content", articleLang, "en");
		boolean onlineDataSource = app.getSettings().WIKI_DATA_SOURCE_TYPE.get() == DataSourceType.ONLINE;
		this.title = amenity.getName();
		String descriptionText = amenity.getDescription(lng);
		this.description = onlineDataSource ? descriptionText : WikiArticleHelper.getPartialContent(descriptionText);
		this.type = getPoiTypeTranslation(app, amenity);
		this.icon = getPoiTypeIcon(app, amenity);
		this.imageUrl = amenity.getWikiImageStubUrl();
		this.location = new LatLon(amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude());
	}

	@NonNull
	private String getPoiTypeTranslation(@NonNull OsmandApplication app, @NonNull Amenity amenity) {
		PoiType subType = app.getPoiTypes().getPoiTypeByKey(amenity.getSubType());
		return subType != null ? subType.getTranslation() : "";
	}

	@NonNull
	private Drawable getPoiTypeIcon(@NonNull OsmandApplication app, @NonNull Amenity amenity) {
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

	@NonNull
	public String getTitle() {
		return title;
	}

	@NonNull
	public String getDescription() {
		return description;
	}

	@NonNull
	public String getTypeName() {
		return type;
	}

	@NonNull
	public Drawable getIcon() {
		return icon;
	}

	@NonNull
	public String getImage() {
		return imageUrl;
	}

	@NonNull
	public LatLon getLocation() {
		return location;
	}
}