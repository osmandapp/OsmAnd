package net.osmand.plus.search.dialogs;

import static net.osmand.plus.search.listitems.QuickSearchBannerListItem.ButtonItem;
import static net.osmand.plus.search.listitems.QuickSearchBannerListItem.INVALID_ID;
import static net.osmand.search.core.ObjectType.POI_TYPE;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.accessibility.AccessibilityAssistant;
import net.osmand.plus.poi.PoiUIFilter;
import net.osmand.plus.search.SearchResultViewHolder;
import net.osmand.plus.search.WikiItemViewHolder;
import net.osmand.plus.search.listitems.*;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.*;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.ObjectType;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.search.core.SearchWord;
import net.osmand.shared.gpx.GpxHelper;
import net.osmand.util.Algorithms;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class QuickSearchListAdapter extends ArrayAdapter<QuickSearchListItem> {

	private final OsmandApplication app;
	private final FragmentActivity activity;
	private AccessibilityAssistant accessibilityAssistant;
	private final LayoutInflater inflater;
	private final boolean nightMode;
	@Nullable
	private PoiUIFilter poiUIFilter;

	private boolean useMapCenter;

	private final int dp56;
	private final int dp1;

	private boolean hasSearchMoreItem;

	private OnSelectionListener selectionListener;
	private boolean selectionMode;
	private boolean selectAll;
	private final List<QuickSearchListItem> selectedItems = new ArrayList<>();
	private final UpdateLocationViewCache updateLocationViewCache;

	public interface OnSelectionListener {

		void onUpdateSelectionMode(List<QuickSearchListItem> selectedItems);

		void reloadData();
	}

	public QuickSearchListAdapter(@NonNull OsmandApplication app,
	                              @NonNull FragmentActivity activity, boolean nightMode) {
		super(activity, R.layout.search_list_item);
		this.app = app;
		this.activity = activity;
		this.nightMode = nightMode;
		this.inflater = UiUtilities.getInflater(activity, nightMode);

		dp56 = AndroidUtils.dpToPx(app, 56f);
		dp1 = AndroidUtils.dpToPx(app, 1f);
		updateLocationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(activity);
	}

	public void setAccessibilityAssistant(AccessibilityAssistant accessibilityAssistant) {
		this.accessibilityAssistant = accessibilityAssistant;
	}

	public OnSelectionListener getSelectionListener() {
		return selectionListener;
	}

	public void setSelectionListener(OnSelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

	public boolean isUseMapCenter() {
		return useMapCenter;
	}

	public void setUseMapCenter(boolean useMapCenter) {
		this.useMapCenter = useMapCenter;
	}

	public boolean isSelectionMode() {
		return selectionMode;
	}

	public void setSelectionMode(boolean selectionMode, int position) {
		this.selectionMode = selectionMode;
		selectAll = false;
		selectedItems.clear();
		if (position != -1) {
			QuickSearchListItem item = getItem(position);
			selectedItems.add(item);
		}
		if (selectionMode) {
			QuickSearchSelectAllListItem selectAllListItem = new QuickSearchSelectAllListItem(app, null, null);
			insertListItem(selectAllListItem, 0);
			if (selectionListener != null) {
				selectionListener.onUpdateSelectionMode(selectedItems);
			}
		} else {
			if (selectionListener != null) {
				selectionListener.reloadData();
			}
		}
		//notifyDataSetInvalidated();
	}

	public List<QuickSearchListItem> getSelectedItems() {
		return selectedItems;
	}

	public void setListItems(List<QuickSearchListItem> items) {
		setNotifyOnChange(false);
		clear();
		hasSearchMoreItem = false;
		for (QuickSearchListItem item : items) {
			add(item);
			if (!hasSearchMoreItem && item.getType() == QuickSearchListItemType.SEARCH_MORE) {
				hasSearchMoreItem = true;
			}
		}
		setNotifyOnChange(true);
		notifyDataSetChanged();
	}

	public void addListItem(@NonNull QuickSearchListItem item) {
		if (hasSearchMoreItem && item.getType() == QuickSearchListItemType.SEARCH_MORE) {
			return;
		}
		setNotifyOnChange(false);
		add(item);
		if (item.getType() == QuickSearchListItemType.SEARCH_MORE) {
			hasSearchMoreItem = true;
		}
		setNotifyOnChange(true);
		notifyDataSetChanged();
	}

	public void insertListItem(@NonNull QuickSearchListItem item, int index) {
		if (hasSearchMoreItem && item.getType() == QuickSearchListItemType.SEARCH_MORE) {
			return;
		}
		setNotifyOnChange(false);
		insert(item, index);
		if (item.getType() == QuickSearchListItemType.SEARCH_MORE) {
			hasSearchMoreItem = true;
		}
		setNotifyOnChange(true);
		notifyDataSetChanged();
	}

	@Override
	public boolean isEnabled(int position) {
		QuickSearchListItemType type = getItem(position).getType();
		return type != QuickSearchListItemType.HEADER
				&& type != QuickSearchListItemType.TOP_SHADOW
				&& type != QuickSearchListItemType.BOTTOM_SHADOW
				&& type != QuickSearchListItemType.SEARCH_MORE;
	}

	@Override
	public int getItemViewType(int position) {
		return getItem(position).getType().ordinal();
	}

	@Override
	public int getViewTypeCount() {
		return QuickSearchListItemType.values().length;
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		QuickSearchListItem listItem = getItem(position);
		QuickSearchListItemType type = listItem.getType();

		LinearLayout view;
		if (type == QuickSearchListItemType.BANNER) {
			view = bindBannerItem(convertView, listItem);
		} else if (type == QuickSearchListItemType.FREE_VERSION_BANNER) {
			view = bindFreeVersionBannerItem(convertView);
		} else if (type == QuickSearchListItemType.SEARCH_MORE) {
			view = bindSearchMoreItem(convertView, listItem);
		} else if (type == QuickSearchListItemType.BUTTON) {
			view = bindButtonItem(convertView, listItem);
		} else if (type == QuickSearchListItemType.SELECT_ALL) {
			view = bindSelectAllItem(position, convertView);
		} else if (type == QuickSearchListItemType.HEADER) {
			view = bindHeaderItem(convertView, listItem);
		} else if (type == QuickSearchListItemType.TOP_SHADOW) {
			return bindTopShadowItem(convertView);
		} else if (type == QuickSearchListItemType.BOTTOM_SHADOW) {
			return bindBottomShadowItem(convertView);
		} else if (type == QuickSearchListItemType.SEARCH_RESULT &&
				poiUIFilter != null && poiUIFilter.isWikiFilter()) {
			return bindWikiItem(convertView, listItem);
		} else if (type == QuickSearchListItemType.DISABLED_HISTORY) {
			view = bindDisabledHistoryItem(listItem, convertView);
		} else {
			view = bindSearchResultItem(position, convertView, listItem);
		}

		setupBackground(view);
		setupDivider(position, view, listItem);
		ViewCompat.setAccessibilityDelegate(view, accessibilityAssistant);
		return view;
	}

	private LinearLayout bindBannerItem(@Nullable View convertView,
	                                    @NonNull QuickSearchListItem listItem) {
		QuickSearchBannerListItem banner = (QuickSearchBannerListItem) listItem;
		LinearLayout view = getLinearLayout(convertView, R.layout.search_banner_list_item);
		((TextView) view.findViewById(R.id.empty_search_description)).setText(R.string.nothing_found_descr);

		SearchUICore searchUICore = app.getSearchUICore().getCore();
		SearchPhrase searchPhrase = searchUICore.getPhrase();

		String textTitle;
		int minimalSearchRadius = searchUICore.getMinimalSearchRadius(searchPhrase);
		if (searchUICore.isSearchMoreAvailable(searchPhrase) && minimalSearchRadius != Integer.MAX_VALUE) {
			double rd = OsmAndFormatter.calculateRoundedDist(minimalSearchRadius, app);
			textTitle = app.getString(R.string.nothing_found_in_radius) + " "
					+ OsmAndFormatter.getFormattedDistance((float) rd, app, OsmAndFormatterParams.NO_TRAILING_ZEROS);
		} else {
			textTitle = app.getString(R.string.search_nothing_found);
		}
		((TextView) view.findViewById(R.id.empty_search_title)).setText(textTitle);

		ViewGroup buttonContainer = view.findViewById(R.id.buttons_container);
		if (buttonContainer != null) {
			buttonContainer.removeAllViews();
			for (ButtonItem button : banner.getButtonItems()) {
				View v = inflater.inflate(R.layout.search_banner_button_list_item, null);
				TextView title = v.findViewById(R.id.title);
				title.setText(button.getTitle());
				ImageView icon = v.findViewById(R.id.icon);
				if (button.getIconId() != INVALID_ID) {
					icon.setImageResource(button.getIconId());
					icon.setVisibility(View.VISIBLE);
				} else {
					icon.setVisibility(View.GONE);
				}
				v.setOnClickListener(button.getListener());
				buttonContainer.addView(v);
			}
		}
		return view;
	}

	private LinearLayout bindFreeVersionBannerItem(@Nullable View convertView) {
		LinearLayout view = getLinearLayout(convertView, R.layout.read_wikipedia_ofline_banner);
		View btnGet = view.findViewById(R.id.btn_get);
		if (btnGet != null) {
			btnGet.setOnClickListener(
					v -> ChoosePlanFragment.showInstance(activity, OsmAndFeature.WIKIPEDIA));
		}
		return view;
	}

	private LinearLayout bindDisabledHistoryItem(@NonNull QuickSearchListItem listItem, @Nullable View convertView) {
		QuickSearchDisabledHistoryItem disabledHistoryItem = (QuickSearchDisabledHistoryItem) listItem;

		LinearLayout view = getLinearLayout(convertView, R.layout.disabled_history_card);

		TextView title = view.findViewById(R.id.title);
		title.setText(app.getString(R.string.is_disabled, app.getString(R.string.shared_string_search_history)));

		TextView description = view.findViewById(R.id.description);
		description.setText(R.string.search_history_is_disabled_descr);

		int color = ColorUtilities.getActivityBgColor(app, nightMode);
		View cardContainer = view.findViewById(R.id.card_container);
		AndroidUtils.setBackground(cardContainer, new ColorDrawable(color));

		TextView analyseButtonDescr = view.findViewById(R.id.settings_button);
		FrameLayout analyseButton = view.findViewById(R.id.settings_button_container);
		AndroidUtils.setBackground(app, analyseButton, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
		AndroidUtils.setBackground(app, analyseButtonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		analyseButton.setOnClickListener(disabledHistoryItem.getOnClickListener());

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.top_divider), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), false);

		return view;
	}

	private LinearLayout bindSearchMoreItem(@Nullable View convertView,
	                                        @NonNull QuickSearchListItem listItem) {
		LinearLayout view = getLinearLayout(convertView, R.layout.search_more_list_item);

		if (listItem.getSpannableName() != null) {
			((TextView) view.findViewById(R.id.title)).setText(listItem.getSpannableName());
		} else {
			((TextView) view.findViewById(R.id.title)).setText(listItem.getName());
		}

		QuickSearchMoreListItem searchMoreItem = (QuickSearchMoreListItem) listItem;
		int emptyDescId = searchMoreItem.isSearchMoreAvailable() ? R.string.nothing_found_descr : R.string.modify_the_search_query;
		((TextView) view.findViewById(R.id.empty_search_description)).setText(emptyDescId);

		boolean emptySearchVisible = searchMoreItem.isEmptySearch() && !searchMoreItem.isInterruptedSearch();
		boolean moreDividerVisible = emptySearchVisible && searchMoreItem.isSearchMoreAvailable();
		view.findViewById(R.id.empty_search).setVisibility(emptySearchVisible ? View.VISIBLE : View.GONE);
		view.findViewById(R.id.more_divider).setVisibility(moreDividerVisible ? View.VISIBLE : View.GONE);
		SearchUICore searchUICore = app.getSearchUICore().getCore();
		SearchPhrase searchPhrase = searchUICore.getPhrase();

		String textTitle;
		int minimalSearchRadius = searchUICore.getMinimalSearchRadius(searchPhrase);
		if (searchUICore.isSearchMoreAvailable(searchPhrase) && minimalSearchRadius != Integer.MAX_VALUE) {
			double rd = OsmAndFormatter.calculateRoundedDist(minimalSearchRadius, app);
			textTitle = app.getString(R.string.nothing_found_in_radius) + " "
					+ OsmAndFormatter.getFormattedDistance((float) rd, app, OsmAndFormatterParams.NO_TRAILING_ZEROS);
		} else {
			textTitle = app.getString(R.string.search_nothing_found);
		}
		((TextView) view.findViewById(R.id.empty_search_title)).setText(textTitle);
		View primaryButton = view.findViewById(R.id.primary_button);

		((TextView) view.findViewById(R.id.title)).setText(getIncreaseSearchButtonTitle(app, searchPhrase));

		primaryButton.setVisibility(searchMoreItem.isSearchMoreAvailable() ? View.VISIBLE : View.GONE);
		primaryButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				((QuickSearchMoreListItem) listItem).onPrimaryButtonClick();
			}
		});

		View secondaryButton = view.findViewById(R.id.secondary_button);
		secondaryButton.setVisibility(searchMoreItem.isSecondaryButtonVisible() ?
				View.VISIBLE : View.GONE);
		secondaryButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				searchMoreItem.onSecondaryButtonClick();
			}
		});
		return view;
	}

	private LinearLayout bindButtonItem(@Nullable View convertView,
	                                    @NonNull QuickSearchListItem listItem) {
		LinearLayout view = getLinearLayout(convertView, R.layout.search_custom_list_item);
		((ImageView) view.findViewById(R.id.imageView)).setImageDrawable(listItem.getIcon());
		if (listItem.getSpannableName() != null) {
			((TextView) view.findViewById(R.id.title)).setText(listItem.getSpannableName());
		} else {
			((TextView) view.findViewById(R.id.title)).setText(listItem.getName());
		}
		return view;
	}

	private LinearLayout bindSelectAllItem(int position,
	                                       @Nullable View convertView) {
		LinearLayout view = getLinearLayout(convertView, R.layout.select_all_list_item);
		CheckBox ch = view.findViewById(R.id.toggle_item);
		ch.setVisibility(View.VISIBLE);
		ch.setChecked(selectAll);
		ch.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				toggleCheckbox(position, ch);
			}
		});
		return view;
	}

	private LinearLayout bindHeaderItem(@Nullable View convertView,
	                                    @NonNull QuickSearchListItem listItem) {
		LinearLayout view = getLinearLayout(convertView, R.layout.search_header_list_item);
		view.findViewById(R.id.top_divider)
				.setVisibility(((QuickSearchHeaderListItem) listItem).isShowTopDivider() ? View.VISIBLE : View.GONE);
		if (listItem.getSpannableName() != null) {
			((TextView) view.findViewById(R.id.title)).setText(listItem.getSpannableName());
		} else {
			((TextView) view.findViewById(R.id.title)).setText(listItem.getName());
		}
		return view;
	}

	private LinearLayout bindTopShadowItem(@Nullable View convertView) {
		return getLinearLayout(convertView, R.layout.list_shadow_header);
	}

	private LinearLayout bindBottomShadowItem(@Nullable View convertView) {
		return getLinearLayout(convertView, R.layout.list_shadow_footer);
	}

	@NonNull
	private LinearLayout bindWikiItem(@Nullable View convertView, @NonNull QuickSearchListItem item) {
		QuickSearchWikiItem wikiItem = new QuickSearchWikiItem(app, item.getSearchResult());
		LinearLayout view = getLinearLayout(convertView, R.layout.search_nearby_item_vertical);
		WikiItemViewHolder holder = new WikiItemViewHolder(view, updateLocationViewCache, nightMode);
		holder.bindItem(wikiItem, poiUIFilter, useMapCenter);
		return view;
	}

	private LinearLayout bindSearchResultItem(int position, @Nullable View convertView,
	                                          @NonNull QuickSearchListItem listItem) {
		LinearLayout view;
		SearchResult searchResult = listItem.getSearchResult();
		if (searchResult != null && searchResult.objectType == ObjectType.INDEX_ITEM) {
			view = getLinearLayout(convertView, R.layout.search_download_map_list_item);
			IndexItem indexItem = (IndexItem) searchResult.relatedObject;
			if (indexItem.isDownloaded()) {
				// remove item after downloading
				remove(listItem);
			} else {
				bindIndexItem(view, indexItem, activity, nightMode);
			}
		} else if (searchResult != null && searchResult.objectType == ObjectType.GPX_TRACK) {
			view = getLinearLayout(convertView, R.layout.search_gpx_list_item);
			bindGpxTrack(view, listItem, (GPXInfo) searchResult.relatedObject);
			setupCheckBox(position, view, listItem);
		} else {
			view = getLinearLayout(convertView, R.layout.search_list_item);
			SearchResultViewHolder.bindSearchResult(view, listItem);
			updateCompass(view, listItem, updateLocationViewCache, useMapCenter);
			setupCheckBox(position, view, listItem);
		}
		return view;
	}

	public static void bindIndexItem(@NonNull View view,
	                                 @NonNull IndexItem indexItem,
	                                 FragmentActivity activity,
	                                 boolean nightMode) {
		OsmandApplication app = (OsmandApplication) view.getContext().getApplicationContext();
		UiUtilities iconsCache = app.getUIUtilities();
		DownloadIndexesThread thread = app.getDownloadThread();

		DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(app);
		TextView tvName = view.findViewById(R.id.title);
		TextView tvDesc = view.findViewById(R.id.description);
		ImageView ivButton = view.findViewById(R.id.secondaryIcon);
		ProgressBar pbProgress = view.findViewById(R.id.progressBar);

		int activeColorId = ColorUtilities.getActiveColorId(nightMode);
		int defaultIconColorId = ColorUtilities.getDefaultIconColorId(nightMode);

		String name = indexItem.getVisibleName(app, app.getRegions(), false);
		tvName.setText(name);

		Drawable buttonDrawable = null;
		boolean isDownloading = indexItem.isDownloading(thread);
		if (!isDownloading) {
			pbProgress.setVisibility(View.GONE);
			tvDesc.setVisibility(View.VISIBLE);

			String pattern = app.getString(R.string.ltr_or_rtl_combine_via_bold_point);
			String size = indexItem.getSizeDescription(app);
			String type = indexItem.getType().getString(app);
			String date = indexItem.getDate(dateFormat, true);
			String description = String.format(pattern, type, date);
			description = String.format(pattern, size, description);
			tvDesc.setText(description);
			buttonDrawable = iconsCache.getIcon(R.drawable.ic_action_gsave_dark, activeColorId);
		} else {
			pbProgress.setVisibility(View.VISIBLE);
			tvDesc.setVisibility(View.GONE);

			int progress = -1;
			if (thread.getCurrentDownloadingItem() == indexItem) {
				progress = (int) thread.getCurrentDownloadProgress();
			}
			pbProgress.setIndeterminate(progress < 0);
			pbProgress.setProgress(progress);
			buttonDrawable = iconsCache.getIcon(R.drawable.ic_action_remove_dark, defaultIconColorId);
		}
		ivButton.setImageDrawable(buttonDrawable);
	}

	public static void bindGpxTrack(@NonNull View view, @NonNull QuickSearchListItem listItem, @Nullable GPXInfo gpxInfo) {
		SearchResult searchResult = listItem.getSearchResult();
		String gpxTitle = GpxHelper.INSTANCE.getGpxTitle(searchResult.localeName);
		OsmandApplication app = (OsmandApplication) view.getContext().getApplicationContext();
		GpxUiHelper.updateGpxInfoView(app, view, gpxTitle, listItem.getIcon(), gpxInfo);
	}


	private LinearLayout getLinearLayout(@Nullable View convertView, int layoutId) {
		if (convertView == null || isLayoutIdChanged(convertView, layoutId)) {
			convertView = inflater.inflate(layoutId, null);
			convertView.setTag(layoutId);
		}
		return (LinearLayout) convertView;
	}

	private boolean isLayoutIdChanged(@NonNull View view, int layoutId) {
		return !Algorithms.objectEquals(view.getTag(), layoutId);
	}

	private void setupCheckBox(int position,
	                           @NonNull View rootView,
	                           @NonNull QuickSearchListItem listItem) {
		CheckBox ch = rootView.findViewById(R.id.toggle_item);
		if (selectionMode) {
			ch.setVisibility(View.VISIBLE);
			ch.setChecked(selectedItems.contains(listItem));
			ch.setOnClickListener(v -> toggleCheckbox(position, ch));
		} else {
			ch.setVisibility(View.GONE);
		}
	}

	private void setupBackground(View view) {
		view.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
	}

	private void setupDivider(int position,
	                          @NonNull View view,
	                          @NonNull QuickSearchListItem listItem) {
		View divider = view.findViewById(R.id.divider);
		if (divider != null) {
			if (position == getCount() - 1 || getItem(position + 1).getType() == QuickSearchListItemType.HEADER
					|| getItem(position + 1).getType() == QuickSearchListItemType.BOTTOM_SHADOW) {
				divider.setVisibility(View.GONE);
			} else {
				divider.setVisibility(View.VISIBLE);
				if (getItem(position + 1).getType() == QuickSearchListItemType.SEARCH_MORE
						|| listItem.getType() == QuickSearchListItemType.SELECT_ALL) {
					LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp1);
					p.setMargins(0, 0, 0, 0);
					divider.setLayoutParams(p);
				} else {
					LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp1);
					AndroidUtils.setMargins(p, dp56, 0, 0, 0);
					divider.setLayoutParams(p);
				}
			}
		}
	}

	public static String getIncreaseSearchButtonTitle(OsmandApplication app, SearchPhrase searchPhrase) {
		SearchWord word = searchPhrase.getLastSelectedWord();
		SearchUICore searchUICore = app.getSearchUICore().getCore();
		if (word != null && word.getType() != null && word.getType().equals(POI_TYPE)) {
			float rd = (float) OsmAndFormatter.calculateRoundedDist(
					searchUICore.getNextSearchRadius(searchPhrase), app);
			return app.getString(R.string.increase_search_radius_to,
					OsmAndFormatter.getFormattedDistance(rd, app, OsmAndFormatterParams.NO_TRAILING_ZEROS));
		} else {
			return app.getString(R.string.increase_search_radius);
		}
	}

	public void toggleCheckbox(int position, CheckBox ch) {
		QuickSearchListItemType type = getItem(position).getType();
		if (type == QuickSearchListItemType.SELECT_ALL) {
			selectAll = ch.isChecked();
			if (ch.isChecked()) {
				selectedItems.clear();
				for (int i = 0; i < getCount(); i++) {
					QuickSearchListItemType t = getItem(i).getType();
					if (t == QuickSearchListItemType.SEARCH_RESULT) {
						selectedItems.add(getItem(i));
					}
				}
			} else {
				selectedItems.clear();
			}
			notifyDataSetChanged();
		} else {
			QuickSearchListItem listItem = getItem(position);
			if (ch.isChecked()) {
				selectedItems.add(listItem);
			} else {
				selectedItems.remove(listItem);
			}
		}
		if (selectionListener != null) {
			selectionListener.onUpdateSelectionMode(selectedItems);
		}
	}

	public static void updateCompass(@NonNull View view, @NonNull QuickSearchListItem item,
			@NonNull UpdateLocationViewCache updateLocationViewCache, boolean useMapCenter) {
		boolean showCompass = item.getSearchResult().location != null;
		if (showCompass) {
			updateLocationView(view, item, updateLocationViewCache, useMapCenter);
		}
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.compass_layout), showCompass);
	}

	public static void updateLocationView(@NonNull View view, @NonNull QuickSearchListItem item,
			@NonNull UpdateLocationViewCache updateLocationViewCache, boolean useMapCenter) {
		OsmandApplication app = AndroidUtils.getApp(view.getContext());
		TextView distanceText = view.findViewById(R.id.distance);
		ImageView direction = view.findViewById(R.id.direction);
		SearchPhrase phrase = item.getSearchResult().requiredSearchPhrase;
		updateLocationViewCache.specialFrom = null;
		if (phrase != null && useMapCenter) {
			updateLocationViewCache.specialFrom = phrase.getSettings().getOriginalLocation();
		}
		LatLon toloc = null;
		if (item.getSearchResult() != null) {
			toloc = item.getSearchResult().location;
		}
		UpdateLocationUtils.updateLocationView(app, updateLocationViewCache, direction, distanceText, toloc);
	}

	public void setPoiUIFilter(@Nullable PoiUIFilter poiUIFilter) {
		this.poiUIFilter = poiUIFilter;
	}

	@Nullable
	public PoiUIFilter getPoiUIFilter() {
		return poiUIFilter;
	}

	@Override
	public void clear() {
		super.clear();
		setPoiUIFilter(null);
	}
}
