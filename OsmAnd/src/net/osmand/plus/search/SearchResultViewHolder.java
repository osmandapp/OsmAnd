package net.osmand.plus.search;

import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE;

import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.StringMatcher;
import net.osmand.data.Amenity;
import net.osmand.osm.AbstractPoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.search.dialogs.QuickSearchListAdapter;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.OpeningHours;

import java.util.Calendar;

public class SearchResultViewHolder extends RecyclerView.ViewHolder {

	public final OsmandApplication app;
	public final UpdateLocationViewCache locationViewCache;

	public final boolean nightMode;

	public SearchResultViewHolder(@NonNull View view,
			@NonNull UpdateLocationViewCache locationViewCache, boolean nightMode) {
		super(view);

		this.app = AndroidUtils.getApp(view.getContext());
		this.locationViewCache = locationViewCache;
		this.nightMode = nightMode;
		itemView.setBackgroundColor(ColorUtilities.getCardAndListBackgroundColor(app, nightMode));
		AndroidUtils.setBackground(itemView.findViewById(R.id.searchListItemLayout), UiUtilities.getSelectableDrawable(app));
	}

	public void bindItem(@NonNull QuickSearchListItem item, boolean useMapCenter) {
		bindSearchResult(itemView, item);
		QuickSearchListAdapter.updateCompass(itemView, item, locationViewCache, useMapCenter);

	}

	public static void bindSearchResult(@NonNull View view, @NonNull QuickSearchListItem item) {
		TextView title = view.findViewById(R.id.title);
		TextView subtitle = view.findViewById(R.id.subtitle);
		ImageView imageView = view.findViewById(R.id.imageView);

		imageView.setImageDrawable(item.getIcon());
		String name = item.getName();
		if (item.getSpannableName() != null) {
			title.setText(item.getSpannableName());
		} else {
			title.setText(name);
		}

		OsmandApplication app = (OsmandApplication) view.getContext().getApplicationContext();
		String desc = item.getTypeName();
		Object searchResultObject = item.getSearchResult().object;
		if (searchResultObject instanceof AbstractPoiType) {
			AbstractPoiType abstractPoiType = (AbstractPoiType) searchResultObject;
			String[] synonyms = abstractPoiType.getSynonyms().split(";");
			QuickSearchHelper searchHelper = app.getSearchUICore();
			SearchUICore searchUICore = searchHelper.getCore();
			String searchPhrase = searchUICore.getPhrase().getText(true);
			StringMatcher matcher = new NameStringMatcher(searchPhrase, CHECK_STARTS_FROM_SPACE);

			if (!searchPhrase.isEmpty() && !matcher.matches(abstractPoiType.getTranslation())) {
				if (matcher.matches(abstractPoiType.getEnTranslation())) {
					desc = item.getTypeName() + " (" + abstractPoiType.getEnTranslation() + ")";
				} else {
					for (String syn : synonyms) {
						if (matcher.matches(syn)) {
							desc = item.getTypeName() + " (" + syn + ")";
							break;
						}
					}
				}
			}
		}

		boolean hasDesc = false;
		if (subtitle != null) {
			if (!Algorithms.isEmpty(desc) && !desc.equals(name)) {
				subtitle.setText(desc);
				subtitle.setVisibility(View.VISIBLE);
				hasDesc = true;
			} else {
				subtitle.setVisibility(View.GONE);
			}
		}

		Drawable typeIcon = item.getTypeIcon();
		ImageView groupIcon = view.findViewById(R.id.type_name_icon);
		if (groupIcon != null) {
			if (typeIcon != null && hasDesc) {
				groupIcon.setImageDrawable(typeIcon);
				groupIcon.setVisibility(View.VISIBLE);
			} else {
				groupIcon.setVisibility(View.GONE);
			}
		}

		LinearLayout timeLayout = view.findViewById(R.id.time_layout);
		if (timeLayout != null) {
			if (item.getSearchResult().object instanceof Amenity
					&& ((Amenity) item.getSearchResult().object).getOpeningHours() != null) {
				Amenity amenity = (Amenity) item.getSearchResult().object;
				OpeningHours rs = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
				if (rs != null && rs.getInfo() != null) {
					int colorOpen = R.color.text_color_positive;
					int colorClosed = R.color.text_color_negative;
					SpannableString openHours = MenuController.getSpannableOpeningHours(
							rs.getInfo(),
							ContextCompat.getColor(app, colorOpen),
							ContextCompat.getColor(app, colorClosed));
					int colorId = rs.isOpenedForTime(Calendar.getInstance()) ? colorOpen : colorClosed;
					timeLayout.setVisibility(View.VISIBLE);

					TextView timeText = view.findViewById(R.id.time);
					ImageView timeIcon = view.findViewById(R.id.time_icon);
					timeText.setText(openHours);
					timeIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_opening_hour_16, colorId));
				} else {
					timeLayout.setVisibility(View.GONE);
				}
			} else {
				timeLayout.setVisibility(View.GONE);
			}
		}
	}

	public static void bindPOISearchResult(@NonNull View view, @NonNull QuickSearchListItem item, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) view.getContext().getApplicationContext();
		TextView title = view.findViewById(R.id.title);
		TextView subtitle = view.findViewById(R.id.subtitle);
		TextView addressTv = view.findViewById(R.id.address);
		ImageView imageView = view.findViewById(R.id.imageView);
		LinearLayout timeLayout = view.findViewById(R.id.time_layout);
		TextView descriptionTv = view.findViewById(R.id.description);
		View dotDivider = view.findViewById(R.id.dot_divider);
		FrameLayout imageContainer = view.findViewById(R.id.image_container);
		boolean hasRouteShield = false;

		//todo here altName can contain whether address or altName. need to separate them
		String address = item.getAltName();
		String name = item.getName();
		String altName = null;
		String typeName = QuickSearchListItem.getTypeName(app, item.getSearchResult());
		Amenity amenity = (Amenity) item.getSearchResult().object;
		String description = null;
		if (amenity != null) {
			String preferredMapLang = app.getSettings().MAP_PREFERRED_LOCALE.get();
			if (Algorithms.isEmpty(preferredMapLang)) {
				preferredMapLang = app.getLanguage();
			}
			String articleLang = PluginsHelper.onGetMapObjectsLocale(amenity, preferredMapLang);
			String lang = amenity.getContentLanguage("content", articleLang, "en");
			String text = amenity.getDescription(lang);
			boolean html = !Algorithms.isEmpty(text) && Algorithms.isHtmlText(text);
			description = html ? WikiArticleHelper.getPartialContent(text) : text;
			if (amenity.isRouteTrack()) {
				typeName = amenity.getRouteActivityType();
				hasRouteShield = QuickSearchListItem.getRouteShieldDrawable(app, amenity) != null;
				address = String.format("%s • %s", AmenityExtensionsHelper.getAmenityMetricsFormatted(amenity, app), address);
			}
		}

		if (altName != null) {
			name = String.format("%s %s", name, altName);
			int textColor = nightMode ? R.color.text_color_secondary_dark : R.color.text_color_secondary_light;
			SpannableString spannableName = UiUtilities.createColorSpannable(name, view.getContext().getColor(textColor), altName);
			title.setText(spannableName);
		} else {
			if (item.getSpannableName() != null) {
				title.setText(item.getSpannableName());
			} else {
				title.setText(name);
			}
		}

		subtitle.setText(typeName);
		if (addressTv != null) {
			addressTv.setText(address);
			addressTv.setVisibility(Algorithms.isEmpty(address) ? View.GONE : View.VISIBLE);
		}


		if (timeLayout != null) {
			if (amenity != null && ((Amenity) item.getSearchResult().object).getOpeningHours() != null) {
				OpeningHours rs = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
				if (rs != null && rs.getInfo() != null) {
					int colorOpen = R.color.text_color_positive;
					int colorClosed = R.color.text_color_negative;
					SpannableString openHours = MenuController.getSpannableOpeningHours(
							rs.getInfo(),
							ContextCompat.getColor(app, colorOpen),
							ContextCompat.getColor(app, colorClosed));
					int colorId = rs.isOpenedForTime(Calendar.getInstance()) ? colorOpen : colorClosed;
					timeLayout.setVisibility(View.VISIBLE);

					TextView timeText = view.findViewById(R.id.time);
					ImageView timeIcon = view.findViewById(R.id.time_icon);
					timeText.setText(openHours);
					timeIcon.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_opening_hour_16, colorId));
				} else {
					timeLayout.setVisibility(View.GONE);
				}
			} else {
				timeLayout.setVisibility(View.GONE);
			}
		}

		ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) imageView.getLayoutParams();
		TypedValue typedValue = new TypedValue();
		boolean resolved = app.getTheme().resolveAttribute(R.attr.activity_background_color, typedValue, true);
		int margin;
		if (hasRouteShield) {
			params.width = AndroidUtils.dpToPx(app, 72);
			params.height = AndroidUtils.dpToPx(app, 36);
			margin = 0;
		} else {
			margin = AndroidUtils.dpToPx(app, 6);
			params.width = AndroidUtils.dpToPx(app, 24);
			params.height = AndroidUtils.dpToPx(app, 24);
		}
		if (imageContainer != null) {
			imageContainer.setPadding(margin, margin, margin, margin);
			if (!hasRouteShield && resolved) {
				if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
					int color = typedValue.data;
					imageContainer.setBackgroundColor(color);
				} else {
					int colorResId = typedValue.resourceId;
					if (colorResId != 0) {
						int color = ContextCompat.getColor(app, colorResId);
						imageContainer.setBackgroundColor(color);
					}
				}
			} else {
				imageContainer.setBackground(null);
			}
		}
		imageView.setLayoutParams(params);
		imageView.setImageDrawable(item.getIcon());
		if (descriptionTv != null) {
			descriptionTv.setText(description);
			if (!Algorithms.isEmpty(description)) {
				descriptionTv.setVisibility(View.VISIBLE);
			} else {
				descriptionTv.setVisibility(View.GONE);
			}
		}
		if (dotDivider != null) {
			dotDivider.setVisibility(!Algorithms.isEmpty(typeName) ? View.VISIBLE : View.GONE);
		}
	}
}

