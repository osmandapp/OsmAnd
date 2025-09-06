package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.data.Amenity.DESCRIPTION;
import static net.osmand.data.Amenity.SHORT_DESCRIPTION;
import static net.osmand.data.Amenity.WIKIDATA;
import static net.osmand.data.Amenity.WIKIPEDIA;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NEAREST_POI_KEY;
import static net.osmand.plus.mapcontextmenu.builders.MenuRowBuilder.NEAREST_WIKI_KEY;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.TAG_URL;
import static net.osmand.plus.wikivoyage.data.TravelObfHelper.WPT_EXTRA_TAGS;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.osmand.PlatformUtil;
import net.osmand.data.Amenity;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.builders.rows.AmenityInfoRow;
import net.osmand.plus.mapcontextmenu.controllers.AmenityMenuController;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.osmedit.OsmEditingPlugin;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class AmenityMenuBuilder extends MenuBuilder {

	public static final Log LOG = PlatformUtil.getLog(AmenityMenuBuilder.class);
	public static final String WIKIPEDIA_ORG_WIKI_URL_PART = ".wikipedia.org/wiki/";

	protected AmenityUIHelper amenityUIHelper;
	protected Map<String, String> extensions;
	protected AdditionalInfoBundle infoBundle;

	public AmenityMenuBuilder(@NonNull MapActivity mapActivity, @NonNull Amenity amenity) {
		super(mapActivity);
		setAmenity(amenity);
		setShowNearestWiki(true);
		setShowNearestPoi(!amenity.getType().isWiki());
	}

	@Override
	public void build(@NonNull ViewGroup view, @Nullable Object object) {
		extensions = amenity.getAmenityExtensions(app.getPoiTypes(), false);
		setCustomOnlinePhotosPosition(extensions.containsKey(WIKIDATA));
		infoBundle = new AdditionalInfoBundle(app, extensions);

		super.build(view, object);
	}

	@Override
	protected void buildNearestWikiRow(ViewGroup view) {
	}

	@Override
	protected void buildNearestPoiRow(ViewGroup view) {
	}

	@Override
	protected void buildDescription(View view) {
		Map<String, Object> filteredInfo = infoBundle.getFilteredLocalizedInfo();
		if (!buildShortWikiDescription(view, filteredInfo, true)) {
			Pair<String, Locale> pair = AmenityUIHelper.getDescriptionWithPreferredLang(app, amenity, DESCRIPTION, filteredInfo);
			if (pair != null) {
				buildDescriptionRow(view, pair.first);
				infoBundle.setCustomHiddenExtensions(Collections.singletonList(DESCRIPTION));
			}
		}
		if (isCustomOnlinePhotosPosition()) {
			buildPhotosRow((ViewGroup) view, amenity);
		}
	}

	protected boolean buildShortWikiDescription(@NonNull View view,
			@NonNull Map<String, Object> filteredInfo, boolean allowOnlineWiki) {
		Pair<String, Locale> pair = AmenityUIHelper.getDescriptionWithPreferredLang(app, amenity, SHORT_DESCRIPTION, filteredInfo);
		Locale locale = pair != null ? pair.second : null;
		String description = pair != null ? pair.first : null;

		boolean hasShortDescription = !Algorithms.isEmpty(description);
		if (hasShortDescription) {
			infoBundle.setCustomHiddenExtensions(Collections.singletonList(DESCRIPTION));
		}
		if (!hasShortDescription && allowOnlineWiki) {
			description = createWikipediaArticleList(filteredInfo);
		}
		boolean descriptionCollapsed[] = {true};
		if (!Algorithms.isEmpty(description)) {
			View rowView = buildRow(view, 0, null, description, 0, true,
					null, false, 0, false, null, false);
			TextViewEx textView = rowView.findViewById(R.id.text);
			final String descriptionToSet = description;
			textView.setOnClickListener(v -> {
				boolean collapsed = !descriptionCollapsed[0];
				descriptionCollapsed[0] = collapsed;
				updateDescriptionState(textView, descriptionToSet, collapsed);
			});
			updateDescriptionState(textView, descriptionToSet, descriptionCollapsed[0]);
			buildReadFullWikiButton((LinearLayout) view, locale, hasShortDescription);
		}
		return hasShortDescription;
	}

	protected void buildReadFullWikiButton(@NonNull ViewGroup container, @Nullable Locale locale,
			boolean hasShortDescription) {
		boolean light = isLightContent();
		Context ctx = container.getContext();
		int activeColor = ColorUtilities.getActiveColor(ctx, !light);

		DialogButton button = (DialogButton) themedInflater.inflate(R.layout.context_menu_read_wiki_button, container, false);
		if (hasShortDescription) {
			String text = app.getString(R.string.context_menu_read_full_article);
			button.setTitle(UiUtilities.createColorSpannable(text, activeColor, text));
		} else {
			String wikipedia = app.getString(R.string.shared_string_wikipedia);
			String text = app.getString(R.string.read_on, wikipedia);
			button.setTitle(UiUtilities.createColorSpannable(text, activeColor, wikipedia));
		}

		Resources resources = ctx.getResources();
		int size = resources.getDimensionPixelSize(R.dimen.small_icon_size);
		Drawable drawable = app.getUIUtilities().getIcon(R.drawable.ic_plugin_wikipedia, light);
		drawable = new BitmapDrawable(resources, AndroidUtils.drawableToBitmap(drawable, size, size, true));

		TextViewEx textView = button.findViewById(R.id.button_text);
		textView.setTypeface(FontCache.getNormalFont());
		textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);

		button.setOnClickListener((v) -> {
			if (hasShortDescription) {
				WikipediaDialogFragment.showInstance(mapActivity, amenity, null);
			} else {
				String wikipediaUrl = amenity.getAdditionalInfo(WIKIPEDIA);
				if (wikipediaUrl == null && locale != null) {
					String title = amenity.getName(locale.getLanguage());
					wikipediaUrl = "https://" + locale.getLanguage() + WIKIPEDIA_ORG_WIKI_URL_PART + title.replace(' ', '_');
				}
				MapActivity activity = app.getOsmandMap().getMapView().getMapActivity();
				if (activity != null) {
					boolean nightMode = app.getDaynightHelper().isNightMode(ThemeUsageContext.MAP);
					AndroidUtils.openUrl(activity, wikipediaUrl, nightMode);
				}
			}
		});
		container.addView(button);
	}

	@Nullable
	private String createWikipediaArticleList(Map<String, Object> filteredInfo) {
		Object value = filteredInfo.get(WIKIPEDIA);
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
					String localizedKey = WIKIPEDIA + ":" + key;
					String name = String.format(app.getString(R.string.wikipedia_names_pattern), localizedAdditionalInfo.get(localizedKey), key);
					joiner.add(name);
				}
				return joiner.toString();
			}
		}
		return null;
	}

	private void updateDescriptionState(TextView textView, String description, boolean collapsed) {
		String text = description;
		if (collapsed) {
			text = description.substring(0, Math.min(description.length(), 200));
			if (description.length() > text.length()) {
				int color = ColorUtilities.getActiveColor(app, !isLightContent());
				String ellipsis = app.getString(R.string.shared_string_ellipsis);
				text += ellipsis;
				textView.setText(UiUtilities.createColorSpannable(text, color, ellipsis));
				return;
			}
		}
		textView.setText(text);
	}

	@Override
	public void buildInternal(View view) {
		processRoutePointAmenityTags(view);
		buildInternalRows(view);

		if (PluginsHelper.getActivePlugin(OsmEditingPlugin.class) != null) {
			amenityUIHelper.buildWikiDataRow(view);
		}

		buildNearestRows((ViewGroup) view);
		buildAltNamesRow((ViewGroup) view);
		buildNamesRow((ViewGroup) view);
		if (!amenityUIHelper.isFirstRow()) {
			firstRow = amenityUIHelper.isFirstRow();
		}
	}

	public void buildInternalRows(@NonNull View view) {
		amenityUIHelper = new AmenityUIHelper(mapActivity, getPreferredMapAppLang(), infoBundle);
		amenityUIHelper.setLight(isLightContent());
		amenityUIHelper.setLatLon(getLatLon());
		amenityUIHelper.setCollapseExpandListener(getCollapseExpandListener());
		amenityUIHelper.buildInternal(view);
	}

	private void buildNamesRow(ViewGroup view) {
		HashMap<String, String> names = new HashMap<>();
		if (!Algorithms.isEmpty(amenity.getName())) {
			names.put("", amenity.getName());
		}
		names.putAll(amenity.getNamesMap(true));
		amenityUIHelper.buildNamesRow(view, names, false);
	}

	private void buildAltNamesRow(ViewGroup view) {
		amenityUIHelper.buildNamesRow(view, amenity.getAltNamesMap(), true);
	}

	private void processRoutePointAmenityTags(View view) {
		if (amenity.isRoutePoint()) {
			String wptExtraTags = extensions.get(WPT_EXTRA_TAGS);
			if (!Algorithms.isEmpty(wptExtraTags)) {
				Gson gson = new Gson();
				Type type = new TypeToken<Map<String, String>>() {}.getType();
				extensions.putAll(gson.fromJson(wptExtraTags, type));
				extensions.remove(WPT_EXTRA_TAGS);
			}
			String url = extensions.get(TAG_URL);
			if (PicassoUtils.isImageUrl(url)) {
				AppCompatImageView imageView = inflateAndGetMainImageView(view);
				PicassoUtils.setupImageViewByUrl(app, imageView, url, true);
			}
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
			AmenityInfoRow wikiInfo = new AmenityInfoRow.Builder(NEAREST_WIKI_KEY)
					.setIconId(R.drawable.ic_action_popular_places).setText(text)
					.setCollapsableView(getCollapsableView(context, true, amenities, NEAREST_WIKI_KEY))
					.setOrder(1000)
					.build();

			View amenitiesRow = createRowContainer(context, NEAREST_WIKI_KEY);

			firstRow = position == 0 || isDividerAtPosition(group, position - 1);
			amenityUIHelper.buildAmenityRow(amenitiesRow, wikiInfo);
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
			AmenityInfoRow poiInfo = new AmenityInfoRow.Builder(NEAREST_POI_KEY)
					.setIconId(AmenityMenuController.getRightIconId(app, amenity)).setText(text)
					.setCollapsableView(getCollapsableView(context, true, amenities, NEAREST_POI_KEY))
					.setOrder(1000)
					.build();

			View wikiRow = group.findViewWithTag(NEAREST_WIKI_KEY);
			int insertIndex = wikiRow != null
					? group.indexOfChild(wikiRow) + 1
					: position;

			View amenitiesRow = createRowContainer(context, NEAREST_POI_KEY);
			firstRow = insertIndex == 0 || isDividerAtPosition(group, insertIndex - 1);
			amenityUIHelper.buildAmenityRow(amenitiesRow, poiInfo);
			group.addView(amenitiesRow, insertIndex);

			buildNearestRowDividerIfMissing(group, insertIndex);
		});
	}

	@Override
	protected Map<String, String> getAdditionalCardParams() {
		return AmenityExtensionsHelper.getImagesParams(extensions);
	}
}