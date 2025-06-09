package net.osmand.plus.search.listitems;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.search.core.SearchCoreFactory;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;

public class QuickSearchWikiItem extends QuickSearchListItem {

	private final Amenity amenity;
	private final String title;
	private final String description;
	private final String type;
	private final Drawable icon;
	private final String imageUrl;
	private final LatLon location;

	public QuickSearchWikiItem(@NonNull OsmandApplication app, @NonNull SearchResult searchResult) {
		super(app, searchResult);

		amenity = (Amenity) searchResult.object;
		String preferredMapLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
		if (Algorithms.isEmpty(preferredMapLang)) {
			preferredMapLang = app.getLanguage();
		}
		String articleLang = PluginsHelper.onGetMapObjectsLocale(amenity, preferredMapLang);
		String lang = amenity.getContentLanguage("content", articleLang, "en");
		this.title = amenity.getName();

		String text = amenity.getDescription(lang);
		boolean html = !Algorithms.isEmpty(text) && Algorithms.isHtmlText(text);
		this.description = html ? WikiArticleHelper.getPartialContent(text) : text;
		this.type = getPoiTypeTranslation(app, amenity);
		this.icon = getPoiTypeIcon(app, amenity);
		this.imageUrl = amenity.getWikiImageStubUrl();
		this.location = new LatLon(amenity.getLocation().getLatitude(), amenity.getLocation().getLongitude());
	}

	@NonNull
	public Amenity getAmenity() {
		return amenity;
	}

	@NonNull
	private String getPoiTypeTranslation(@NonNull OsmandApplication app, @NonNull Amenity amenity) {
		String itemType = getPoiTypeKey(amenity);
		PoiType subType = app.getPoiTypes().getPoiTypeByKey(itemType);
		return subType != null ? subType.getTranslation() : "";
	}

	@NonNull
	private String getPoiTypeKey(@NonNull Amenity amenity) {
		String itemType = amenity.getOsmandPoiKey();
		if (itemType == null) {
			itemType = amenity.getSubType();
		}
		return itemType;
	}

	@NonNull
	private Drawable getPoiTypeIcon(@NonNull OsmandApplication app, @NonNull Amenity amenity) {
		boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
		Drawable drawable = app.getUIUtilities().getIcon(R.drawable.ic_action_info_dark, nightMode);
		PoiType subType = app.getPoiTypes().getPoiTypeByKey(getPoiTypeKey(amenity));
		if (subType != null) {
			Drawable renderingIcon = app.getUIUtilities().getRenderingIcon(app, subType.getKeyName(), nightMode);
			if (renderingIcon != null) {
				drawable = renderingIcon;
			}
		}
		return drawable;
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

	@Nullable
	public String getImage() {
		return imageUrl;
	}

	@NonNull
	public LatLon getLocation() {
		return location;
	}

	public static PointDescription getPointDescription(@NonNull OsmandApplication app, @NonNull Amenity amenity) {
		SearchPhrase phrase = SearchPhrase.emptyPhrase(app.getSearchUICore().getCore().getSearchSettings());
		SearchResult sr = SearchCoreFactory.createSearchResult(amenity, phrase, MapPoiTypes.getDefault());
		return getPointDescriptionObject(app, sr).first;
	}
}