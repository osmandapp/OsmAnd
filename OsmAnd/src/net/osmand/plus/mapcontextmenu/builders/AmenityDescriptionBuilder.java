package net.osmand.plus.mapcontextmenu.builders;

import static net.osmand.data.Amenity.DESCRIPTION;
import static net.osmand.data.Amenity.SHORT_DESCRIPTION;
import static net.osmand.data.Amenity.WIKIPEDIA;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import net.osmand.data.AdditionalInfoBundle;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BuildRowAttrs;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

final class AmenityDescriptionBuilder {

	private static final String WIKIPEDIA_ORG_WIKI_URL_PART = ".wikipedia.org/wiki/";

	private final MenuBuilder menuBuilder;
	private final MapActivity mapActivity;
	private final OsmandApplication app;
	private final Amenity amenity;
	private final AdditionalInfoBundle infoBundle;
	private final boolean lightContent;

	AmenityDescriptionBuilder(@NonNull MenuBuilder menuBuilder, @NonNull Amenity amenity,
	                          @NonNull AdditionalInfoBundle infoBundle, boolean lightContent) {
		this.menuBuilder = menuBuilder;
		this.mapActivity = menuBuilder.getMapActivity();
		this.app = menuBuilder.getApplication();
		this.amenity = amenity;
		this.infoBundle = infoBundle;
		this.lightContent = lightContent;
	}

	/**
	 * @return true when a short or regular amenity description was built
	 * and the generic description row should be hidden.
	 */
	boolean buildDescription(@NonNull View view) {
		Map<String, Object> filteredInfo = infoBundle.getFilteredLocalizedInfo();

		if (buildShortWikiDescription(view, filteredInfo, true)) {
			return true;
		}

		Pair<String, Locale> pair = AmenityUIHelper.getDescriptionWithPreferredLang(
				app, amenity, DESCRIPTION, filteredInfo);
		if (pair == null) {
			return false;
		}

		menuBuilder.buildDescriptionRow(view, pair.first);
		infoBundle.setCustomHiddenExtensions(Collections.singletonList(DESCRIPTION));
		return true;
	}

	/**
	 * @return true only when a short description was built. An online Wikipedia link does
	 * not consume the regular description fallback.
	 */
	boolean buildShortWikiDescription(@NonNull View view,
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
		boolean[] descriptionCollapsed = {true};
		if (!Algorithms.isEmpty(description)) {
			View rowView = menuBuilder.buildRow(view,
					new BuildRowAttrs.Builder().setText(description).setCollapsable(true).build());
			TextViewEx textView = rowView.findViewById(R.id.text);
			String descriptionToSet = description;
			textView.setOnClickListener(v -> {
				boolean collapsed = !descriptionCollapsed[0];
				descriptionCollapsed[0] = collapsed;
				updateDescriptionState(textView, descriptionToSet, collapsed);
			});
			updateDescriptionState(textView, descriptionToSet, descriptionCollapsed[0]);
			buildReadFullWikiButton((ViewGroup) view, locale, hasShortDescription);
		}
		return hasShortDescription;
	}

	private void buildReadFullWikiButton(@NonNull ViewGroup container, @Nullable Locale locale,
	                                     boolean hasShortDescription) {
		Context context = container.getContext();
		int activeColor = ColorUtilities.getActiveColor(context, !lightContent);

		LayoutInflater themedInflater = UiUtilities.getInflater(mapActivity, !lightContent);
		DialogButton button = (DialogButton) themedInflater.inflate(R.layout.context_menu_read_wiki_button, container, false);
		if (hasShortDescription) {
			String text = app.getString(R.string.context_menu_read_full_article);
			button.setTitle(UiUtilities.createColorSpannable(text, activeColor, text));
		} else {
			String wikipedia = app.getString(R.string.shared_string_wikipedia);
			String text = app.getString(R.string.read_on, wikipedia);
			button.setTitle(UiUtilities.createColorSpannable(text, activeColor, wikipedia));
		}

		Resources resources = context.getResources();
		int size = resources.getDimensionPixelSize(R.dimen.small_icon_size);
		Drawable drawable = app.getUIUtilities().getIcon(R.drawable.ic_plugin_wikipedia, lightContent);
		drawable = new BitmapDrawable(resources, AndroidUtils.drawableToBitmap(drawable, size, size, true));

		TextViewEx textView = button.findViewById(R.id.button_text);
		textView.setTypeface(FontCache.getNormalFont());
		textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);

		button.setOnClickListener(v -> {
			if (hasShortDescription) {
				WikipediaDialogFragment.showInstance(mapActivity, amenity, null);
			} else {
				String wikipediaUrl = amenity.getAdditionalInfo(WIKIPEDIA);
				if (Algorithms.isEmpty(wikipediaUrl) && locale != null) {
					String title = amenity.getName(locale.getLanguage());
					if (!Algorithms.isEmpty(title)) {
						wikipediaUrl = "https://" + locale.getLanguage()
								+ WIKIPEDIA_ORG_WIKI_URL_PART + title.replace(' ', '_');
					}
				}
				if (!Algorithms.isEmpty(wikipediaUrl)) {
					LatLon location = amenity.getLocation() != null ? amenity.getLocation() : menuBuilder.getLatLon();
					boolean nightMode = app.getDaynightHelper().isNightMode(app.getSettings().getApplicationMode(), ThemeUsageContext.MAP);
					WikiArticleHelper.askShowArticle(mapActivity, nightMode, location, wikipediaUrl);
				}
			}
		});
		container.addView(button);
	}

	@Nullable
	private String createWikipediaArticleList(@NonNull Map<String, Object> filteredInfo) {
		Object value = filteredInfo.get(WIKIPEDIA);
		if (value instanceof String url) {
			if (url.contains(WIKIPEDIA_ORG_WIKI_URL_PART)) {
				return url.substring(url.lastIndexOf(WIKIPEDIA_ORG_WIKI_URL_PART) + WIKIPEDIA_ORG_WIKI_URL_PART.length());
			}
		} else if (value instanceof Map<?, ?> map) {
			Object localizationsValue = map.get("localizations");
			if (!(localizationsValue instanceof Map<?, ?> localizations)
					|| localizations.isEmpty()) {
				return null;
			}
			List<String> localizationKeys = new ArrayList<>();
			for (Object key : localizations.keySet()) {
				if (key instanceof String stringKey) {
					localizationKeys.add(stringKey);
				}
			}
			Collection<String> availableLocales = AmenityUIHelper.collectAvailableLocalesFromTags(localizationKeys);
			StringJoiner joiner = new StringJoiner(", ");
			for (String key : availableLocales) {
				String localizedKey = WIKIPEDIA + ":" + key;
				Object localizedValue = localizations.get(localizedKey);
				if (localizedValue instanceof String localizedString
						&& !Algorithms.isEmpty(localizedString)) {
					String name = app.getString(
							R.string.wikipedia_names_pattern, localizedString, key);
					joiner.add(name);
				}
			}
			return joiner.toString();
		}
		return null;
	}

	private void updateDescriptionState(@NonNull TextView textView,
	                                    @NonNull String description, boolean collapsed) {
		String text = description;
		if (collapsed) {
			text = description.substring(0, Math.min(description.length(), 200));
			if (description.length() > text.length()) {
				int color = ColorUtilities.getActiveColor(app, !lightContent);
				String ellipsis = app.getString(R.string.shared_string_ellipsis);
				text += ellipsis;
				textView.setText(UiUtilities.createColorSpannable(text, color, ellipsis));
				return;
			}
		}
		textView.setText(text);
	}
}
