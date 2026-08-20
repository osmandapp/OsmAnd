package net.osmand.plus.search;

import static net.osmand.CollatorStringMatcher.StringMatcherMode.CHECK_STARTS_FROM_SPACE;
import static net.osmand.plus.helpers.AmenityExtensionsHelper.MIN_UPHILL_DOWNHILL_FIXED_TO_SHOW;
import static net.osmand.plus.helpers.AmenityExtensionsHelper.MIN_UPHILL_DOWNHILL_PERCENT_TO_SHOW;
import static net.osmand.plus.utils.OsmAndFormatterParams.NO_TRAILING_ZEROS;

import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import net.osmand.StringMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.MapObject;
import net.osmand.osm.AbstractPoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AmenityExtensionsHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MenuController;
import net.osmand.plus.mapcontextmenu.builders.rows.PoiAdditionalUiRule;
import net.osmand.plus.mapcontextmenu.builders.rows.PoiAdditionalUiRules;
import net.osmand.plus.mapcontextmenu.controllers.NetworkRouteDrawable;
import net.osmand.plus.mapcontextmenu.other.TrimToBackgroundTextView;
import net.osmand.plus.search.dialogs.SearchScopeChip;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.track.clickable.ClickableWayHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.PicassoUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchPhrase.NameStringMatcher;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchResult;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.search.core.TopIndexFilter;
import net.osmand.search.core.spatial.SpatialSearchResult;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;
import net.osmand.util.OpeningHoursParser.OpeningHours;

import java.util.Calendar;
import java.util.List;

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

	public static void bindSearchResult(@NonNull View view, @NonNull QuickSearchListItem item, @NonNull Calendar calendar) {
		TextView title = view.findViewById(R.id.title);
		TextView subtitle = view.findViewById(R.id.subtitle);
		ImageView imageView = view.findViewById(R.id.imageView);

		OsmandApplication app = (OsmandApplication) view.getContext().getApplicationContext();
		imageView.setImageDrawable(item.getIcon());
		setupIconContainer(view, imageView, app);
		String name = item.getName();
		title.setText(item.getSpannableName());

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
		boolean groupIconVisible = typeIcon != null && hasDesc;
		if (groupIcon != null) {
			if (groupIconVisible) {
				groupIcon.setImageDrawable(typeIcon);
				groupIcon.setVisibility(View.VISIBLE);
			} else {
				groupIcon.setVisibility(View.GONE);
			}
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.dot_divider), hasDesc && !groupIconVisible);

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
							ContextCompat.getColor(app, colorClosed), true);
					int colorId = rs.isOpenedForTime(calendar) ? colorOpen : colorClosed;
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

	public static void bindSpatialCategorySearchResult(@NonNull View view, @NonNull QuickSearchListItem item) {
		TextView title = view.findViewById(R.id.title);
		TextView subtitle = view.findViewById(R.id.subtitle);
		ImageView imageView = view.findViewById(R.id.imageView);

		OsmandApplication app = (OsmandApplication) view.getContext().getApplicationContext();
		imageView.setImageDrawable(item.getIcon());
		setupIconContainer(view, imageView, app);
		title.setText(item.getSpannableName());
		bindSpatialCategoryPart(view, item, app, subtitle);

		LinearLayout timeLayout = view.findViewById(R.id.time_layout);
		if (timeLayout != null) {
			timeLayout.setVisibility(View.GONE);
		}
	}

	public static void bindSpatialCategoryPart(@NonNull View view, @NonNull QuickSearchListItem item,
	                                           @NonNull OsmandApplication app, @NonNull TextView subtitle) {
		SpatialSearchResult spatialSearchResult = item.getSpatialSearchResult();
		if (spatialSearchResult == null || !spatialSearchResult.isPoiCategory()) {
			return;
		}
		SearchScopeChip chip = view.findViewById(R.id.search_scope_chip);
		ImageView groupIcon = view.findViewById(R.id.type_name_icon);
		groupIcon.setVisibility(View.GONE);
		ApplicationMode applicationMode = app.getSettings().getApplicationMode();
		boolean nightMode = app.getDaynightHelper().isNightMode(applicationMode, ThemeUsageContext.APP);
		if (chip != null) {
			MapObject refObject = spatialSearchResult.getReferenceObject();
			if (refObject != null) {
				chip.setScopeName(refObject.getName(), nightMode);
			}
		}
		subtitle.setText(app.getString(R.string.shared_string_near).toLowerCase());
		subtitle.setVisibility(View.VISIBLE);

		if (item.getSearchResult().object instanceof TopIndexFilter topIndexFilter) {
			PoiAdditionalUiRule uiRule = PoiAdditionalUiRules.INSTANCE.findRule(topIndexFilter.getTag());
			if (uiRule.getCustomIconId() != null) {
				int iconColor = nightMode ? R.color.osmand_orange_dark : R.color.osmand_orange;
				Drawable icon = app.getUIUtilities().getIcon(uiRule.getCustomIconId(), iconColor);
				((ImageView)view.findViewById(R.id.imageView)).setImageDrawable(icon);
			}
		}
	}

	private static void setupIconContainer(@NonNull View view, @NonNull ImageView imageView,
	                                       @NonNull OsmandApplication app) {
		FrameLayout imageContainer = view.findViewById(R.id.image_container);
		if (imageContainer != null) {
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) imageView.getLayoutParams();
			params.width = AndroidUtils.dpToPx(app, 24);
			params.height = AndroidUtils.dpToPx(app, 24);
			params.gravity = Gravity.CENTER;
			imageView.setLayoutParams(params);
			imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

			int margin = AndroidUtils.dpToPx(app, 6);
			imageContainer.setPadding(margin, margin, margin, margin);
		}
	}

	public static void bindFullSearchResult(@NonNull View view, @NonNull QuickSearchListItem item) {
		OsmandApplication app = AndroidUtils.getApp(view.getContext());
		TextView title = view.findViewById(R.id.title);
		TextView subtitle = view.findViewById(R.id.subtitle);
		ImageView imageView = view.findViewById(R.id.imageView);

		cancelPhotoRequest(imageView);
		imageView.setImageDrawable(item.getIcon());
		AndroidUiHelper.updateVisibility(imageView, true);
		setupIconContainer(view, imageView, app);
		FrameLayout imageContainer = view.findViewById(R.id.image_container);
		if (imageContainer != null) {
			imageContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(),
					R.attr.activity_background_color));
		}
		title.setText(item.getSpannableName());
		String typeName = item.getTypeName();
		subtitle.setText(typeName);
		AndroidUiHelper.updateVisibility(subtitle, !Algorithms.isEmpty(typeName));
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.address), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.time_layout), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shieldSign), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.dot_divider), !Algorithms.isEmpty(typeName));
		resetTrackStatistics(view);
	}

	public static void bindTrackSearchResult(@NonNull View view, @NonNull QuickSearchListItem item,
	                                         @NonNull SearchTrackData trackData, boolean nightMode) {
		OsmandApplication app = AndroidUtils.getApp(view.getContext());
		TextView title = view.findViewById(R.id.title);
		TextView subtitle = view.findViewById(R.id.subtitle);
		TextView addressTv = view.findViewById(R.id.address);
		ImageView imageView = view.findViewById(R.id.imageView);

		title.setText(getTrackTitle(item));

		String typeName = app.getString(R.string.shared_string_gpx_track);
		String activityName = trackData.getActivityName();
		subtitle.setText(Algorithms.isEmpty(activityName) ? typeName
				: app.getString(R.string.ltr_or_rtl_combine_via_bold_point, typeName, activityName));
		AndroidUiHelper.updateVisibility(subtitle, true);

		cancelPhotoRequest(imageView);
		imageView.setImageDrawable(item.getIcon());
		AndroidUiHelper.updateVisibility(imageView, true);
		setupIconContainer(view, imageView, app);
		FrameLayout imageContainer = view.findViewById(R.id.image_container);
		if (imageContainer != null) {
			imageContainer.setBackgroundColor(ColorUtilities.getActivityBgColor(app, nightMode));
		}

		String address = trackData.getAddress();
		AndroidUiHelper.setTextAndChangeVisibility(addressTv, address);
		bindTrackStatistics(view, trackData.getLength(), trackData.getUphill(), trackData.getDownhill(),
				!Algorithms.isEmpty(address));

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.time_layout), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.description), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shieldSign), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.dot_divider), true);
	}

	@NonNull
	private static String getTrackTitle(@NonNull QuickSearchListItem item) {
		SearchResult searchResult = item.getSearchResult();
		String name = searchResult != null ? searchResult.localeName : null;
		if (Algorithms.isEmpty(name)) {
			name = item.getName();
		}
		String title = Algorithms.isEmpty(name) ? null : GpxHelper.INSTANCE.getGpxTitle(name);
		return title != null ? title.replace("/", " • ").trim() : "";
	}

	public static void bindTrackStatistics(@NonNull View view, float length, double uphill,
	                                       double downhill, boolean hasAddress) {
		View statistics = view.findViewById(R.id.track_statistics);
		if (statistics == null) {
			return;
		}
		boolean hasLength = length > 0;
		AndroidUiHelper.updateVisibility(statistics, hasLength);
		if (!hasLength) {
			return;
		}
		OsmandApplication app = AndroidUtils.getApp(view.getContext());
		TextView lengthText = view.findViewById(R.id.track_length);
		lengthText.setText(OsmAndFormatter.getFormattedDistance((float) length, app, NO_TRAILING_ZEROS));

		bindSlopeValue(view, R.id.track_uphill_icon, R.id.track_uphill, uphill, length, app);
		bindSlopeValue(view, R.id.track_downhill_icon, R.id.track_downhill, downhill, length, app);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.track_statistics_divider), hasAddress);
	}

	private static void bindSlopeValue(@NonNull View view, int iconId, int textId, double value,
	                                   float length, @NonNull OsmandApplication app) {
		boolean visible = value >= MIN_UPHILL_DOWNHILL_FIXED_TO_SHOW
				&& value / length * 100 > MIN_UPHILL_DOWNHILL_PERCENT_TO_SHOW;
		AndroidUiHelper.updateVisibility(view.findViewById(iconId), visible);
		TextView textView = view.findViewById(textId);
		AndroidUiHelper.updateVisibility(textView, visible);
		if (visible) {
			textView.setText(OsmAndFormatter.getFormattedDistance((float) value, app, NO_TRAILING_ZEROS));
		}
	}

	public static void resetTrackStatistics(@NonNull View view) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.track_statistics), false);
	}

	private static void cancelPhotoRequest(@NonNull ImageView imageView) {
		Picasso.get().cancelRequest(imageView);
		imageView.setTag(null);
	}

	public static void bindCoordinatesSearchResult(@NonNull View view, @NonNull QuickSearchListItem item) {
		OsmandApplication app = AndroidUtils.getApp(view.getContext());
		ImageView imageView = view.findViewById(R.id.imageView);

		imageView.setImageDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_coordinates_location));
		setupIconContainer(view, imageView, app);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.time_layout), false);
	}

	public static boolean isCoordinatesItem(@Nullable SearchResult searchResult) {
		return searchResult != null && searchResult.objectType == ObjectType.LOCATION;
	}

	public static void bindPOISearchResult(@NonNull View view, @NonNull QuickSearchListItem item,
	                                       boolean nightMode, Calendar calendar) {
		OsmandApplication app = (OsmandApplication) view.getContext().getApplicationContext();
		TextView titleTv = view.findViewById(R.id.title);
		TextView subtitle = view.findViewById(R.id.subtitle);
		TextView addressTv = view.findViewById(R.id.address);
		ImageView imageView = view.findViewById(R.id.imageView);
		TrimToBackgroundTextView shieldSign = view.findViewById(R.id.shieldSign);
		LinearLayout timeLayout = view.findViewById(R.id.time_layout);
		TextView descriptionTv = view.findViewById(R.id.description);
		View dotDivider = view.findViewById(R.id.dot_divider);
		FrameLayout imageContainer = view.findViewById(R.id.image_container);
		boolean hasRouteShield = false;

		String address = item.getAddress();
		CharSequence title = item.getMapObjectTitleWithAltName(app, nightMode);
		String typeName = QuickSearchListItem.getTypeName(app, item.getSearchResult());
		if (!Algorithms.isEmpty(typeName)) {
			int typenameComaPosition = typeName.indexOf(",");
			if (typenameComaPosition > 0) {
				typeName = typeName.substring(0, typenameComaPosition);
			}
		}
		Amenity amenity = (Amenity) item.getSearchResult().object;
		titleTv.setText(title);

		String description = null;
		String photoUrl = null;
		boolean routeTrack = false;
		if (amenity != null) {
			photoUrl = amenity.getWikiIconUrl();
			ClickableWayHelper clickableWayHelper = app.getClickableWayHelper();
			if (amenity.isRouteTrack() || clickableWayHelper.isClickableWayAmenity(amenity)) {
				routeTrack = true;
				typeName = amenity.getRouteActivityType();
				hasRouteShield = QuickSearchListItem.getRouteShieldDrawable(app, amenity) != null;
			}
		}

		AndroidUiHelper.setTextAndChangeVisibility(addressTv, address);
		if (routeTrack) {
			bindTrackStatistics(view, AmenityExtensionsHelper.getAmenityDistanceMeters(amenity),
					AmenityExtensionsHelper.getAmenityUphillMeters(amenity),
					AmenityExtensionsHelper.getAmenityDownhillMeters(amenity),
					!Algorithms.isEmpty(address));
		} else {
			resetTrackStatistics(view);
		}
		subtitle.setText(typeName);

		if (timeLayout != null) {
			if (amenity != null && amenity.getOpeningHours() != null) {
				OpeningHours rs = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
				List<OpeningHours.Info> openHourInfo = OpeningHoursParser.getInfo(amenity.getOpeningHours());
				if (openHourInfo != null) {
					int colorOpen = R.color.text_color_positive;
					int colorClosed = R.color.text_color_negative;
					int colorNearToOpen = R.color.icon_color_warning;
					SpannableString openHours = MenuController.getSpannableOpeningHours(
							openHourInfo,
							ContextCompat.getColor(app, colorOpen),
							ContextCompat.getColor(app, colorClosed), true);

					String nearToOpen = rs.getNearToOpeningTime(calendar, OpeningHours.ALL_SEQUENCES);
					boolean isNearToOpen = !Algorithms.isEmpty(nearToOpen);

					int colorId;
					if (rs.isOpenedForTime(calendar)) {
						colorId = colorOpen;
					} else if (isNearToOpen) {
						colorId = colorNearToOpen;
					} else {
						colorId = colorClosed;
					}
					if (Algorithms.isEmpty(openHours)) {
						String openHoursStr = rs.toLocalString();
						openHours = UiUtilities.createColorSpannable(openHoursStr, app.getColor(colorId), openHoursStr);
					}
					if (Algorithms.isEmpty(openHours)) {
						timeLayout.setVisibility(View.GONE);
					} else {
						timeLayout.setVisibility(View.VISIBLE);
						TextView timeText = view.findViewById(R.id.time);
						ImageView timeIcon = view.findViewById(R.id.time_icon);
						timeText.setText(openHours);
						timeIcon.setImageDrawable(app.getUIUtilities().getIcon(isNearToOpen ? R.drawable.ic_action_closed_hours_16 : R.drawable.ic_action_opening_hour_16, colorId));
					}
				} else {
					timeLayout.setVisibility(View.GONE);
				}
			} else {
				timeLayout.setVisibility(View.GONE);
			}
		}
		Drawable imageDrawable = item.getIcon();
		FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) imageView.getLayoutParams();
		int margin;
		if (hasRouteShield) {
			AndroidUiHelper.updateVisibility(shieldSign, true);
			AndroidUiHelper.updateVisibility(imageView, false);
			params.width = AndroidUtils.dpToPx(app, 72);
			params.height = AndroidUtils.dpToPx(app, 36);
			params.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
			imageView.setScaleType(ImageView.ScaleType.FIT_END);
			margin = 0;
			if (imageDrawable instanceof NetworkRouteDrawable networkRouteDrawable) {
				shieldSign.setDrawable(networkRouteDrawable);
			}
		} else {
			shieldSign.setDrawable(null);
			AndroidUiHelper.updateVisibility(imageView, true);
			AndroidUiHelper.updateVisibility(shieldSign, false);
			margin = AndroidUtils.dpToPx(app, 6);
			params.gravity = Gravity.CENTER;
			imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			if (Algorithms.isEmpty(photoUrl)) {
				imageView.setImageDrawable(imageDrawable);
				imageView.setTag(null);
				params.width = AndroidUtils.dpToPx(app, 24);
				params.height = AndroidUtils.dpToPx(app, 24);
			} else {
				params.width = AndroidUtils.dpToPx(app, 36);
				params.height = AndroidUtils.dpToPx(app, 36);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				if (!Algorithms.objectEquals(imageView.getTag(), photoUrl)) {
					imageView.setTag(photoUrl);
					PicassoUtils picasso = PicassoUtils.getPicasso(app);
					RequestCreator creator = Picasso.get().load(photoUrl);

					if (imageDrawable != null) {
						creator.error(imageDrawable);
					}
					final String loadPhotoKey = photoUrl;
					creator.into(imageView, new Callback() {
						@Override
						public void onSuccess() {
							AndroidUiHelper.updateVisibility(imageView, true);
							picasso.setResultLoaded(loadPhotoKey, true);
						}

						@Override
						public void onError(Exception e) {
							AndroidUiHelper.updateVisibility(imageView, false);
							picasso.setResultLoaded(loadPhotoKey, false);
						}
					});
				}
			}
			imageView.setLayoutParams(params);
		}
		if (imageContainer != null) {
			if (Algorithms.isEmpty(photoUrl) || hasRouteShield) {
				imageContainer.setBackground(null);
				imageContainer.setPadding(margin, margin, margin, margin);
			} else {
				int topPadding = titleTv.getLineCount() > 1 ? AndroidUtils.dpToPx(app, 8) : 0;
				imageContainer.setPadding(0, topPadding, 0, 0);
			}
			if (!hasRouteShield) {
				int colorId = nightMode ? R.color.activity_background_color_dark : R.color.activity_background_color_light;
				int color = ContextCompat.getColor(app, colorId);
				imageContainer.setBackgroundColor(color);
			} else {
				imageContainer.setBackground(null);
			}
		}
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
