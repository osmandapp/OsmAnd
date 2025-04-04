package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NEAREST_POI_KEY;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NEAREST_WIKI_KEY;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.TAG_URL;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.WPT_EXTRA_TAGS;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class AmenityMenuBuilder extends MenuBuilder {

	public static final Log LOG = PlatformUtil.getLog(AmenityMenuBuilder.class);
	public static final String WIKIPEDIA_ORG_WIKI_URL_PART = ".wikipedia.org/wiki/";

	protected Amenity amenity;
	private AmenityUIHelper rowsBuilder;
	protected Map<String, String> additionalInfo;
	boolean descriptionCollapsed = true;
	boolean hasDescriptionData;

	public AmenityMenuBuilder(@NonNull MapActivity mapActivity, @NonNull Amenity amenity) {
		super(mapActivity);
		this.amenity = amenity;
		setAmenity(amenity);
		setShowNearestWiki(true);
		setShowNearestPoi(!amenity.getType().isWiki());
		additionalInfo = amenity.getAmenityExtensions(app.getPoiTypes(), false);
		if (additionalInfo.containsKey(Amenity.WIKIDATA)) {
			setCustomOnlinePhotosPosition(true);
		}
	}

	@Override
	protected void buildNearestWikiRow(ViewGroup view) {
	}

	@Override
	protected void buildNearestPoiRow(ViewGroup view) {
	}

	@Override
	protected void buildDescription(View view) {
		if (amenity != null) {
			hasDescriptionData = true;
			Locale prefferedLocale = null;
			AdditionalInfoBundle additionalInfoBundle = new AdditionalInfoBundle(app, amenity.getAmenityExtensions(app.getPoiTypes(), false));
			Map<String, Object> filteredInfo = additionalInfoBundle.getFilteredLocalizedInfo();
			String description = amenity.getAdditionalInfo(Amenity.SHORT_DESCRIPTION);
			if (description == null) {
				Object descriptionMapObject = filteredInfo.get(Amenity.SHORT_DESCRIPTION);
				if (descriptionMapObject instanceof Map<?, ?>) {
					Map<String, Object> descriptionMAp = (Map<String, Object>) descriptionMapObject;
					Map<String, String> localizedAdditionalInfo = (Map<String, String>) descriptionMAp.get("localizations");
					Collection<String> availableLocales = AmenityUIHelper.collectAvailableLocalesFromTags(localizedAdditionalInfo.keySet());
					prefferedLocale = LocaleHelper.getPreferredNameLocale(app, availableLocales);
					String descriptionLocalizedKey = prefferedLocale != null ? Amenity.SHORT_DESCRIPTION + ":" + prefferedLocale.getLanguage() : Amenity.SHORT_DESCRIPTION;
					description = localizedAdditionalInfo.get(descriptionLocalizedKey);
					if (description == null) {
						Map.Entry<String, String> entry = new ArrayList<>(localizedAdditionalInfo.entrySet()).get(0);
						description = entry.getValue();
					}
				}
			}
			if (Algorithms.isEmpty(description)) {
				hasDescriptionData = false;
				description = createWikipediaArticleList(filteredInfo);
			}
			if (!Algorithms.isEmpty(description)) {
				View rowView = buildRow(view, 0, null, description, 0, true,
						null, false, 0, false, null, false);
				TextViewEx textView = rowView.findViewById(R.id.text);
				final String descriptionToSet = description;
				textView.setOnClickListener(v -> {
					descriptionCollapsed = !descriptionCollapsed;
					updateDescriptionState(textView, descriptionToSet);
				});
				updateDescriptionState(textView, descriptionToSet);
				String btnText = app.getString(hasDescriptionData ? R.string.context_menu_read_full_article : R.string.read_on_wiki);
				Locale finalPrefferedLocale = prefferedLocale;
				buildReadFullButton((LinearLayout) view, btnText, (v) -> {
					if (hasDescriptionData) {
						WikipediaDialogFragment.showInstance(mapActivity, amenity, null);
					} else {
						String wikipediaUrl = amenity.getAdditionalInfo(Amenity.WIKIPEDIA);
						if (wikipediaUrl == null && finalPrefferedLocale != null) {
							String title = amenity.getName(finalPrefferedLocale.getLanguage());
							wikipediaUrl = "https://" + finalPrefferedLocale.getLanguage() + WIKIPEDIA_ORG_WIKI_URL_PART + title.replace(' ', '_');
						}
						MapActivity activity = app.getOsmandMap().getMapView().getMapActivity();
						if (activity != null) {
							AndroidUtils.openUrl(activity, Uri.parse(wikipediaUrl), app.getDaynightHelper().isNightMode());
						}
					}
				});
			}
			if (isCustomOnlinePhotosPosition()) {
				buildNearestPhotos((ViewGroup) view, amenity);
			}
		}
	}

	@Nullable
	private String createWikipediaArticleList(Map<String, Object> filteredInfo) {
		Object value = filteredInfo.get(Amenity.WIKIPEDIA);
		if (value != null) {
			if (value instanceof String url) {
				if (url.contains(WIKIPEDIA_ORG_WIKI_URL_PART)) {
					return url.substring(url.lastIndexOf(WIKIPEDIA_ORG_WIKI_URL_PART) + WIKIPEDIA_ORG_WIKI_URL_PART.length());
				}
			} else {
				Map<String, Object> map = (Map<String, Object>) value;
				Map<String, String> localizedAdditionalInfo = (Map<String, String>) map.get("localizations");
				if (Algorithms.isEmpty(localizedAdditionalInfo)) {
					return null;
				}
				Collection<String> availableLocales = AmenityUIHelper.collectAvailableLocalesFromTags(localizedAdditionalInfo.keySet());
				StringJoiner joiner = new StringJoiner(", ");
				for (String key : availableLocales) {
					String localizedKey = Amenity.WIKIPEDIA + ":" + key;
					String name = String.format(app.getString(R.string.wikipedia_names_pattern), localizedAdditionalInfo.get(localizedKey), key);
					joiner.add(name);
				}
				return joiner.toString();
			}
		}
		return null;
	}

	private void updateDescriptionState(TextViewEx textView, String description) {
		String text = description;
		if (descriptionCollapsed) {
			text = description.substring(0, Math.min(description.length(), 200));
			if (description.length() > text.length()) {
				text += app.getString(R.string.shared_string_ellipsis);
			}
		}
		textView.setText(text);
	}

	@Override
	public void buildInternal(View view) {
		if (amenity.isRoutePoint()) {
			processRoutePointAmenityTags(view);
		}

		rowsBuilder = new AmenityUIHelper(mapActivity, getPreferredMapAppLang(), additionalInfo);
		rowsBuilder.setLight(isLightContent());
		rowsBuilder.setLatLon(getLatLon());
		rowsBuilder.setCollapseExpandListener(getCollapseExpandListener());
		rowsBuilder.buildInternal(view);
		if (PluginsHelper.getActivePlugin(OsmEditingPlugin.class) != null) {
			rowsBuilder.buildWikiDataRow(view);
		}

		buildNearestRows((ViewGroup) view);
		buildAltNamesRow((ViewGroup) view);
		buildNamesRow((ViewGroup) view);
		if (!rowsBuilder.isFirstRow()) {
			firstRow = rowsBuilder.isFirstRow();
		}
	}

	private void buildNamesRow(ViewGroup view) {
		HashMap<String, String> names = new HashMap<>();
		if(amenity.getName() != null) {
			names.put("", amenity.getName());
		}
		names.putAll(amenity.getNamesMap(true));
		rowsBuilder.buildNamesRow(view, names, false);
	}

	private void buildAltNamesRow(ViewGroup view) {
		rowsBuilder.buildNamesRow(view, amenity.getAltNamesMap(), true);
	}

	private void processRoutePointAmenityTags(View view) {
		final String wptExtraTags = additionalInfo.get(WPT_EXTRA_TAGS);
		if (!Algorithms.isEmpty(wptExtraTags)) {
			Gson gson = new Gson();
			Type type = new TypeToken<Map<String, String>>() {}.getType();
			additionalInfo.putAll(gson.fromJson(wptExtraTags, type));
			additionalInfo.remove(WPT_EXTRA_TAGS);
		}
		final String url = additionalInfo.get(TAG_URL);
		if (PicassoUtils.isImageUrl(url)) {
			AppCompatImageView imageView = inflateAndGetMainImageView(view);
			PicassoUtils.setupImageViewByUrl(app, imageView, url, true);
		}
		final String description = additionalInfo.get(Amenity.DESCRIPTION);
		if (!Algorithms.isEmpty(description)) {
			buildDescriptionRow(view, description);
			additionalInfo.remove(Amenity.DESCRIPTION);
		}
	}

	private void buildNearestRows(ViewGroup viewGroup) {
		buildNearestWiki(viewGroup);
		if (!OSMSettings.OSMTagKey.ADMINISTRATIVE.getValue().equals(amenity.getType().getKeyName())) {
			buildNearestPoi(viewGroup);
		}
	}

	private void buildNearestWiki(ViewGroup viewGroup) {
		int position = viewGroup.getChildCount();
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
		buildNearestWikiRow(viewGroup, amenities -> {
			ViewGroup group = viewGroupRef.get();
			if (group == null || Algorithms.isEmpty(amenities)) {
				return;
			}
			String title = app.getString(R.string.wiki_around);
			String count = "(" + amenities.size() + ")";
			String text = app.getString(R.string.ltr_or_rtl_combine_via_space, title, count);

			Context context = group.getContext();
			AmenityInfoRow wikiInfo = new AmenityInfoRow(
					NEAREST_WIKI_KEY, R.drawable.ic_action_popular_places, null, text,
					null, true, getCollapsableView(context, true, amenities, NEAREST_WIKI_KEY),
					0, false, false, false, 1000, null, false, false, false, 0);

			View amenitiesRow = createRowContainer(context, NEAREST_WIKI_KEY);

			firstRow = position == 0 || isDividerAtPosition(group, position - 1);
			rowsBuilder.buildAmenityRow(amenitiesRow, wikiInfo);
			group.addView(amenitiesRow, position);

			buildNearestRowDividerIfMissing(group, position);
		});
	}

	private void buildNearestPoi(ViewGroup viewGroup) {
		int position = viewGroup.getChildCount();
		WeakReference<ViewGroup> viewGroupRef = new WeakReference<>(viewGroup);
		buildNearestPoiRow(amenities -> {
			ViewGroup group = viewGroupRef.get();
			if (group == null) {
				return;
			}
			String title = app.getString(R.string.speak_poi);
			String type = "\"" + AmenityMenuController.getTypeStr(amenity) + "\"";
			String count = "(" + amenities.size() + ")";
			String text = app.getString(R.string.ltr_or_rtl_triple_combine_via_space, title, type, count);

			Context context = group.getContext();
			AmenityInfoRow poiInfo = new AmenityInfoRow(
					NEAREST_POI_KEY, AmenityMenuController.getRightIconId(app, amenity), null, text,
					null, true, getCollapsableView(context, true, amenities, NEAREST_POI_KEY),
					0, false, false, false, 1000, null, false, false, false, 0);

			View wikiRow = group.findViewWithTag(NEAREST_WIKI_KEY);
			int insertIndex = wikiRow != null
					? group.indexOfChild(wikiRow) + 1
					: position;

			View amenitiesRow = createRowContainer(context, NEAREST_POI_KEY);
			firstRow = insertIndex == 0 || isDividerAtPosition(group, insertIndex - 1);
			rowsBuilder.buildAmenityRow(amenitiesRow, poiInfo);
			group.addView(amenitiesRow, insertIndex);

			buildNearestRowDividerIfMissing(group, insertIndex);
		});
	}

	@Override
	protected Map<String, String> getAdditionalCardParams() {
		return AmenityExtensionsHelper.getImagesParams(additionalInfo);
	}
}