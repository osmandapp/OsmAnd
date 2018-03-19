package net.osmand.plus.search;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.CollatorStringMatcher;
import net.osmand.Location;
import net.osmand.ResultMatcher;
import net.osmand.StringMatcher;
import net.osmand.access.AccessibilityAssistant;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.osm.AbstractPoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.plus.search.listitems.QuickSearchHeaderListItem;
import net.osmand.plus.search.listitems.QuickSearchListItem;
import net.osmand.plus.search.listitems.QuickSearchListItemType;
import net.osmand.plus.search.listitems.QuickSearchMoreListItem;
import net.osmand.plus.search.listitems.QuickSearchSelectAllListItem;
import net.osmand.search.SearchUICore;
import net.osmand.search.core.SearchPhrase;
import net.osmand.search.core.SearchResult;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

public class QuickSearchListAdapter extends ArrayAdapter<QuickSearchListItem> {

	private OsmandApplication app;
	private Activity activity;
	private AccessibilityAssistant accessibilityAssistant;

	private LatLon location;
	private Float heading;
	private boolean useMapCenter;

	private int screenOrientation;
	private int dp56;
	private int dp1;

	private boolean hasSearchMoreItem;

	private OnSelectionListener selectionListener;
	private boolean selectionMode;
	private boolean selectAll;
	private List<QuickSearchListItem> selectedItems = new ArrayList<>();

	public interface OnSelectionListener {

		void onUpdateSelectionMode(List<QuickSearchListItem> selectedItems);

		void reloadData();
	}

	public QuickSearchListAdapter(OsmandApplication app, Activity activity) {
		super(app, R.layout.search_list_item);
		this.app = app;
		this.activity = activity;
		dp56 = AndroidUtils.dpToPx(app, 56f);
		dp1 = AndroidUtils.dpToPx(app, 1f);
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

	public int getScreenOrientation() {
		return screenOrientation;
	}

	public void setScreenOrientation(int screenOrientation) {
		this.screenOrientation = screenOrientation;
	}

	public LatLon getLocation() {
		return location;
	}

	public void setLocation(LatLon location) {
		this.location = location;
	}

	public Float getHeading() {
		return heading;
	}

	public void setHeading(Float heading) {
		this.heading = heading;
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
	public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
		final QuickSearchListItem listItem = getItem(position);
		QuickSearchListItemType type = listItem.getType();
		LinearLayout view;
		if (type == QuickSearchListItemType.SEARCH_MORE) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) app.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout) inflater.inflate(R.layout.search_more_list_item, null);
			} else {
				view = (LinearLayout) convertView;
			}

			if (listItem.getSpannableName() != null) {
				((TextView) view.findViewById(R.id.title)).setText(listItem.getSpannableName());
			} else {
				((TextView) view.findViewById(R.id.title)).setText(listItem.getName());
			}

			final QuickSearchMoreListItem searchMoreItem = (QuickSearchMoreListItem) listItem;
			int emptyDescId = searchMoreItem.isSearchMoreAvailable() ? R.string.nothing_found_descr : R.string.modify_the_search_query;
			((TextView) view.findViewById(R.id.empty_search_description)).setText(emptyDescId);

			boolean emptySearchVisible = searchMoreItem.isEmptySearch() && !searchMoreItem.isInterruptedSearch();
			boolean moreDividerVisible = emptySearchVisible && searchMoreItem.isSearchMoreAvailable();
			view.findViewById(R.id.empty_search).setVisibility(emptySearchVisible ? View.VISIBLE : View.GONE);
			view.findViewById(R.id.more_divider).setVisibility(moreDividerVisible ? View.VISIBLE : View.GONE);

			View increaseRadiusRow = view.findViewById(R.id.increase_radius_row);
			increaseRadiusRow.setVisibility(searchMoreItem.isSearchMoreAvailable() ? View.VISIBLE : View.GONE);
			increaseRadiusRow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					((QuickSearchMoreListItem) listItem).increaseRadiusOnClick();
				}
			});

			if (!searchMoreItem.isOnlineSearch()) {
				View onlineSearchRow = view.findViewById(R.id.online_search_row);
				onlineSearchRow.setVisibility(View.VISIBLE);
				onlineSearchRow.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						searchMoreItem.onlineSearchOnClick();
					}
				});
			}
		} else if (type == QuickSearchListItemType.BUTTON) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) app
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout) inflater.inflate(
						R.layout.search_custom_list_item, null);
			} else {
				view = (LinearLayout) convertView;
			}
			((ImageView) view.findViewById(R.id.imageView)).setImageDrawable(listItem.getIcon());
			if (listItem.getSpannableName() != null) {
				((TextView) view.findViewById(R.id.title)).setText(listItem.getSpannableName());
			} else {
				((TextView) view.findViewById(R.id.title)).setText(listItem.getName());
			}
		} else if (type == QuickSearchListItemType.SELECT_ALL) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) app
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout) inflater.inflate(
						R.layout.select_all_list_item, null);
			} else {
				view = (LinearLayout) convertView;
			}
			final CheckBox ch = (CheckBox) view.findViewById(R.id.toggle_item);
			ch.setVisibility(View.VISIBLE);
			ch.setChecked(selectAll);
			ch.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					toggleCheckbox(position, ch);
				}
			});
		} else if (type == QuickSearchListItemType.HEADER) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) app
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout) inflater.inflate(
						R.layout.search_header_list_item, null);
			} else {
				view = (LinearLayout) convertView;
			}
			view.findViewById(R.id.top_divider)
					.setVisibility(((QuickSearchHeaderListItem)listItem).isShowTopDivider() ? View.VISIBLE : View.GONE);
			if (listItem.getSpannableName() != null) {
				((TextView) view.findViewById(R.id.title)).setText(listItem.getSpannableName());
			} else {
				((TextView) view.findViewById(R.id.title)).setText(listItem.getName());
			}
		} else if (type == QuickSearchListItemType.TOP_SHADOW) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) app
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout) inflater.inflate(
						R.layout.list_shadow_header, null);
			} else {
				view = (LinearLayout) convertView;
			}
			return view;
		} else if (type == QuickSearchListItemType.BOTTOM_SHADOW) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) app
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout) inflater.inflate(
						R.layout.list_shadow_footer, null);
			} else {
				view = (LinearLayout) convertView;
			}
			return view;
		} else {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) app
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout) inflater.inflate(
						R.layout.search_list_item, null);
			} else {
				view = (LinearLayout) convertView;
			}

			final CheckBox ch = (CheckBox) view.findViewById(R.id.toggle_item);
			if (selectionMode) {
				ch.setVisibility(View.VISIBLE);
				ch.setChecked(selectedItems.contains(listItem));
				ch.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						toggleCheckbox(position, ch);
					}
				});
			} else {
				ch.setVisibility(View.GONE);
			}

			ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
			TextView title = (TextView) view.findViewById(R.id.title);
			TextView subtitle = (TextView) view.findViewById(R.id.subtitle);

			imageView.setImageDrawable(listItem.getIcon());
			String name = listItem.getName();
			if (listItem.getSpannableName() != null) {
				title.setText(listItem.getSpannableName());
			} else {
				title.setText(name);
			}

			String desc = listItem.getTypeName();
			if (listItem.getSearchResult().object instanceof AbstractPoiType) {
				AbstractPoiType abstractPoiType = (AbstractPoiType) listItem.getSearchResult().object;
				String synonyms[] = abstractPoiType.getSynonyms().split(";");
				QuickSearchHelper searchHelper = app.getSearchUICore();
				SearchUICore searchUICore = searchHelper.getCore();
				String searchPhrase = searchUICore.getPhrase().getText(true);
				SearchPhrase.NameStringMatcher nm = new SearchPhrase.NameStringMatcher(searchPhrase,
						CollatorStringMatcher.StringMatcherMode.CHECK_EQUALS_FROM_SPACE);
				for (int i = 0; i < synonyms.length; i++) {
					if (nm.matches(synonyms[i])) {
						desc = listItem.getTypeName() + " (" + synonyms[i] + ")";
					}
				}
			}

			boolean hasDesc = false;
			if (!Algorithms.isEmpty(desc) && !desc.equals(name)) {
				subtitle.setText(desc);
				subtitle.setVisibility(View.VISIBLE);
				hasDesc = true;
			} else {
				subtitle.setVisibility(View.GONE);
			}

			Drawable typeIcon = listItem.getTypeIcon();
			ImageView group = (ImageView) view.findViewById(R.id.type_name_icon);
			if (typeIcon != null && hasDesc) {
				group.setImageDrawable(typeIcon);
				group.setVisibility(View.VISIBLE);
			} else {
				group.setVisibility(View.GONE);
			}

			LinearLayout timeLayout = (LinearLayout) view.findViewById(R.id.time_layout);
			TextView timeText = (TextView) view.findViewById(R.id.time);
			ImageView timeIcon = (ImageView) view.findViewById(R.id.time_icon);
			if (listItem.getSearchResult().object instanceof Amenity
					&& ((Amenity) listItem.getSearchResult().object).getOpeningHours() != null) {
				Amenity amenity = (Amenity) listItem.getSearchResult().object;
				OpeningHoursParser.OpeningHours rs = OpeningHoursParser.parseOpenedHours(amenity.getOpeningHours());
				if (rs != null) {
					Calendar inst = Calendar.getInstance();
					inst.setTimeInMillis(System.currentTimeMillis());
					boolean worksNow = rs.isOpenedForTime(inst);
					inst.setTimeInMillis(System.currentTimeMillis() + 30 * 60 * 1000); // 30 minutes later
					boolean worksLater = rs.isOpenedForTime(inst);
					int colorId = worksNow ? worksLater ? R.color.color_ok : R.color.color_intermediate : R.color.color_warning;

					timeLayout.setVisibility(View.VISIBLE);
					timeIcon.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_small_time, colorId));
					timeText.setTextColor(app.getResources().getColor(colorId));
					String rt = rs.getCurrentRuleTime(inst);
					timeText.setText(rt == null ? "" : rt);
				} else {
					timeLayout.setVisibility(View.GONE);
				}
			} else {
				timeLayout.setVisibility(View.GONE);
			}

			updateCompassVisibility(view, listItem);
		}
		view.setBackgroundColor(app.getResources().getColor(
						app.getSettings().isLightContent() ? R.color.bg_color_light : R.color.bg_color_dark));
		View divider = view.findViewById(R.id.divider);
		if (divider != null) {
			if (position == getCount() - 1 || getItem(position + 1).getType() == QuickSearchListItemType.HEADER
					|| getItem(position + 1).getType() == QuickSearchListItemType.BOTTOM_SHADOW) {
				divider.setVisibility(View.GONE);
			} else {
				divider.setVisibility(View.VISIBLE);
				if (getItem(position + 1).getType() == QuickSearchListItemType.SEARCH_MORE
						|| type == QuickSearchListItemType.SELECT_ALL) {
					LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp1);
					p.setMargins(0, 0, 0 ,0);
					divider.setLayoutParams(p);
				} else {
					LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp1);
					p.setMargins(dp56, 0, 0 ,0);
					divider.setLayoutParams(p);
				}
			}
		}
		ViewCompat.setAccessibilityDelegate(view, accessibilityAssistant);
		return view;
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
			if (selectionListener != null) {
				selectionListener.onUpdateSelectionMode(selectedItems);
			}
		} else {
			QuickSearchListItem listItem = getItem(position);
			if (ch.isChecked()) {
				selectedItems.add(listItem);
			} else {
				selectedItems.remove(listItem);
			}
			if (selectionListener != null) {
				selectionListener.onUpdateSelectionMode(selectedItems);
			}
		}
	}

	private void updateCompassVisibility(View view, QuickSearchListItem listItem) {
		View compassView = view.findViewById(R.id.compass_layout);
		Location ll = app.getLocationProvider().getLastKnownLocation();
		boolean showCompass = location != null && listItem.getSearchResult().location != null;
		if ((ll != null || useMapCenter) && showCompass) {
			updateDistanceDirection(view, listItem);
			compassView.setVisibility(View.VISIBLE);
		} else {
			if (!showCompass) {
				compassView.setVisibility(View.GONE);
			} else {
				compassView.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void updateDistanceDirection(View view, QuickSearchListItem listItem) {
		TextView distanceText = (TextView) view.findViewById(R.id.distance);
		ImageView direction = (ImageView) view.findViewById(R.id.direction);
		SearchPhrase phrase = listItem.getSearchResult().requiredSearchPhrase;
		LatLon loc = location;
		if(phrase != null && useMapCenter) {
			LatLon ol = phrase.getSettings().getOriginalLocation();
			if(ol != null) {
				loc = ol;
			}
		}
		DashLocationFragment.updateLocationView(useMapCenter, loc,
				heading, direction, distanceText,
				listItem.getSearchResult().location.getLatitude(),
				listItem.getSearchResult().location.getLongitude(),
				screenOrientation, app, activity);
	}
}
