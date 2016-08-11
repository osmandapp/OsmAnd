package net.osmand.plus.search;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.dashboard.DashLocationFragment;
import net.osmand.search.core.SearchPhrase;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class QuickSearchListAdapter extends ArrayAdapter<QuickSearchListItem> {

	private OsmandApplication app;
	private Activity activity;

	private LatLon location;
	private Float heading;
	private boolean useMapCenter;

	private int searchMoreItemPosition;
	private int selectAllItemPosition;

	private int screenOrientation;
	private int dp56;
	private int dp1;

	private OnSelectionListener selectionListener;
	private boolean selectionMode;
	private boolean selectAll;
	private List<QuickSearchListItem> selectedItems = new ArrayList<>();

	private static final int ITEM_TYPE_REGULAR = 0;
	private static final int ITEM_TYPE_SEARCH_MORE = 1;
	private static final int ITEM_TYPE_SELECT_ALL = 2;

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
		for (QuickSearchListItem item : items) {
			add(item);
		}
		acquireAdditionalItemsPositions();
		setNotifyOnChange(true);
		notifyDataSetChanged();
	}

	public void addListItem(QuickSearchListItem item) {
		setNotifyOnChange(false);
		add(item);
		acquireAdditionalItemsPositions();
		setNotifyOnChange(true);
		notifyDataSetChanged();
	}

	public void insertListItem(QuickSearchListItem item, int index) {
		setNotifyOnChange(false);
		insert(item, index);
		acquireAdditionalItemsPositions();
		setNotifyOnChange(true);
		notifyDataSetChanged();
	}

	private void acquireAdditionalItemsPositions() {
		selectAllItemPosition = -1;
		searchMoreItemPosition = -1;
		if (getCount() > 0) {
			QuickSearchListItem first = getItem(0);
			QuickSearchListItem last = getItem(getCount() - 1);
			selectAllItemPosition = first instanceof QuickSearchSelectAllListItem ? 0 : -1;
			searchMoreItemPosition = last instanceof QuickSearchMoreListItem ? getCount() - 1 : -1;
		}
	}

	@Override
	public QuickSearchListItem getItem(int position) {
		return super.getItem(position);
	}

	@Override
	public int getItemViewType(int position) {
		if (position == searchMoreItemPosition) {
			return ITEM_TYPE_SEARCH_MORE;
		} else if (position == selectAllItemPosition) {
			return ITEM_TYPE_SELECT_ALL;
		} else {
			return ITEM_TYPE_REGULAR;
		}
	}

	@Override
	public int getViewTypeCount() {
		return 3;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final QuickSearchListItem listItem = getItem(position);
		int viewType = getItemViewType(position);
		LinearLayout view;
		if (viewType == ITEM_TYPE_SEARCH_MORE) {
			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) app
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = (LinearLayout) inflater.inflate(
						R.layout.search_more_list_item, null);
			} else {
				view = (LinearLayout) convertView;
			}

			((TextView) view.findViewById(R.id.title)).setText(listItem.getName());
		} else if (viewType == ITEM_TYPE_SELECT_ALL) {
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
			title.setText(name);

			String desc = listItem.getTypeName();
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

					timeIcon.setVisibility(View.VISIBLE);
					timeText.setVisibility(View.VISIBLE);
					timeIcon.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_small_time, colorId));
					timeText.setTextColor(app.getResources().getColor(colorId));
					String rt = rs.getCurrentRuleTime(inst);
					timeText.setText(rt == null ? "" : rt);
				} else {
					timeIcon.setVisibility(View.GONE);
					timeText.setVisibility(View.GONE);
				}
			} else {
				timeIcon.setVisibility(View.GONE);
				timeText.setVisibility(View.GONE);
			}

			updateCompassVisibility(view, listItem);
		}
		view.setBackgroundColor(app.getResources().getColor(
						app.getSettings().isLightContent() ? R.color.bg_color_light : R.color.bg_color_dark));
		View divider = view.findViewById(R.id.divider);
		if (divider != null) {
			if (position == getCount() - 1) {
				divider.setVisibility(View.GONE);
			} else {
				divider.setVisibility(View.VISIBLE);
				if (position + 1 == searchMoreItemPosition || position == selectAllItemPosition) {
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
		return view;
	}

	public void toggleCheckbox(int position, CheckBox ch) {
		int viewType = getItemViewType(position);
		if (viewType == ITEM_TYPE_SELECT_ALL) {
			selectAll = ch.isChecked();
			if (ch.isChecked()) {
				selectedItems.clear();
				for (int i = 0; i < getCount(); i++) {
					if (getItemViewType(i) == ITEM_TYPE_REGULAR) {
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
		boolean gpsFixed = ll != null && System.currentTimeMillis() - ll.getTime() < 1000 * 60 * 60 * 20;
		if ((gpsFixed || useMapCenter) && showCompass) {
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
