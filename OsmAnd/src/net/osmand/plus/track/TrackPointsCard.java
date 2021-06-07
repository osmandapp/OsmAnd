package net.osmand.plus.track;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.Collator;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.OsmAndCollator;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.base.PointImageDrawable;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.myplaces.DeletePointsTask;
import net.osmand.plus.myplaces.DeletePointsTask.OnPointsDeleteListener;
import net.osmand.plus.myplaces.EditTrackGroupDialogFragment;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TrackPointsCard extends MapBaseCard implements OnChildClickListener, OnPointsDeleteListener {

	public static final int ADD_WAYPOINT_INDEX = 0;
	public static final int DELETE_WAYPOINTS_INDEX = 1;
	public static final int OPEN_WAYPOINT_INDEX = 2;

	private final TrackDisplayHelper displayHelper;
	private final GpxDisplayItemType[] filterTypes = new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_POINTS, GpxDisplayItemType.TRACK_ROUTE_POINTS};

	private GpxDisplayGroup selectedGroup;
	private final Set<Integer> selectedGroups = new LinkedHashSet<>();
	private final LinkedHashMap<GpxDisplayItemType, Set<GpxDisplayItem>> selectedItems = new LinkedHashMap<>();
	private boolean selectionMode;

	private final PointGPXAdapter adapter;
	private ExpandableListView listView;
	private View addActionsView;
	private View addWaypointActionView;
	private View deleteWaypointActionView;

	public TrackPointsCard(@NonNull MapActivity mapActivity, @NonNull TrackDisplayHelper displayHelper) {
		super(mapActivity);
		this.displayHelper = displayHelper;
		adapter = new PointGPXAdapter();
	}

	public boolean isSelectionMode() {
		return selectionMode;
	}

	public void setSelectionMode(boolean selectionMode) {
		this.selectionMode = selectionMode;
		adapter.notifyDataSetInvalidated();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.track_points_card;
	}

	@Override
	protected void updateContent() {
		listView = view.findViewById(android.R.id.list);
		listView.setOnChildClickListener(this);

		adapter.setFilterResults(null);
		adapter.synchronizeGroups(getDisplayGroups());
		if (listView.getAdapter() == null) {
			listView.setAdapter(adapter);
		}
		LayoutInflater inflater = UiUtilities.getInflater(mapActivity, nightMode);
		if (addActionsView == null && addWaypointActionView == null) {
			listView.addFooterView(inflater.inflate(R.layout.list_shadow_footer, listView, false));
			addActions(inflater);
			addWaypointAction(inflater);
		}
		if (!adapter.isEmpty() && deleteWaypointActionView == null){
			AndroidUiHelper.updateVisibility(addWaypointActionView.findViewById(R.id.divider), true);
			deleteWaypointAction(inflater);
		} else if (adapter.isEmpty() && deleteWaypointActionView != null) {
			AndroidUiHelper.updateVisibility(addWaypointActionView.findViewById(R.id.divider), false);
			listView.removeFooterView(deleteWaypointActionView);
			deleteWaypointActionView = null;
		}
		expandAllGroups();
	}

	public void setSelectedGroup(GpxDisplayGroup selectedGroup) {
		this.selectedGroup = selectedGroup;
	}

	public void updateGroups() {
		selectedItems.clear();
		selectedGroups.clear();
	}

	public List<GpxDisplayGroup> getGroups() {
		return adapter.groups;
	}

	private void addActions(LayoutInflater inflater) {
		addActionsView = inflater.inflate(R.layout.preference_category_with_descr, listView, false);
		TextView title = addActionsView.findViewById(android.R.id.title);
		title.setText(R.string.shared_string_actions);

		AndroidUiHelper.updateVisibility(addActionsView.findViewById(android.R.id.icon), false);
		AndroidUiHelper.updateVisibility(addActionsView.findViewById(android.R.id.summary), false);
		listView.addFooterView(addActionsView);
	}

	private void addWaypointAction(LayoutInflater inflater) {
		addWaypointActionView = inflater.inflate(R.layout.preference_button, listView, false);
		TextView addWaypointTitle = addWaypointActionView.findViewById(android.R.id.title);
		ImageView addWaypointIcon = addWaypointActionView.findViewById(android.R.id.icon);

		addWaypointTitle.setText(R.string.add_waypoint);
		addWaypointIcon.setImageDrawable(getContentIcon(R.drawable.ic_action_name_field));


		addWaypointActionView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(TrackPointsCard.this, ADD_WAYPOINT_INDEX);
				}
			}
		});
		listView.addFooterView(addWaypointActionView);
	}

	private void deleteWaypointAction(LayoutInflater inflater) {
		deleteWaypointActionView = inflater.inflate(R.layout.preference_button, listView, false);
		TextView deleteWaypointsTitle = deleteWaypointActionView.findViewById(android.R.id.title);
		ImageView deleteWaypointsIcon = deleteWaypointActionView.findViewById(android.R.id.icon);

		deleteWaypointsTitle.setText(R.string.delete_waypoints);
		deleteWaypointsIcon.setImageDrawable(getColoredIcon(R.drawable.ic_action_delete_dark, R.color.color_osm_edit_delete));

		deleteWaypointActionView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(TrackPointsCard.this, DELETE_WAYPOINTS_INDEX);
				}
			}
		});
		listView.addFooterView(deleteWaypointActionView);
	}

	private void expandAllGroups() {
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
	}

	private List<GpxDisplayGroup> getOriginalGroups() {
		return displayHelper.getOriginalGroups(filterTypes);
	}

	private List<GpxDisplayGroup> getDisplayGroups() {
		if (selectedGroup != null) {
			List<GpxDisplayGroup> res = new ArrayList<>();
			res.add(selectedGroup);
			return res;
		} else {
			return getOriginalGroups();
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		GpxDisplayItem item = adapter.getChild(groupPosition, childPosition);
		if (item != null && item.locationStart != null) {
			CardListener cardListener = getListener();
			if (cardListener != null) {
				cardListener.onCardButtonPressed(this, OPEN_WAYPOINT_INDEX);
			}

			LatLon location = new LatLon(item.locationStart.lat, item.locationStart.lon);
			PointDescription description = new PointDescription(PointDescription.POINT_TYPE_WPT, item.name);
			mapActivity.getContextMenu().show(location, description, item.locationStart);
		}
		return true;
	}

	public void deleteItemsAction() {
		int size = getSelectedItemsCount();
		if (size > 0) {
			AlertDialog.Builder b = new AlertDialog.Builder(mapActivity);
			b.setMessage(app.getString(R.string.points_delete_multiple, size));
			b.setPositiveButton(R.string.shared_string_delete, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					deleteItems();
					setSelectionMode(false);
					adapter.notifyDataSetInvalidated();
				}
			});
			b.setNegativeButton(R.string.shared_string_cancel, null);
			b.show();
		}
	}

	private void deleteItems() {
		new DeletePointsTask(app, displayHelper.getGpx(), getSelectedItems(), this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private Set<GpxDisplayItem> getSelectedItems() {
		Set<GpxDisplayItem> result = new LinkedHashSet<>();
		for (Set<GpxDisplayItem> set : selectedItems.values()) {
			if (set != null) {
				result.addAll(set);
			}
		}
		return result;
	}

	private void updateSelectionMode() {
		int size = getSelectedItemsCount();
		app.showToastMessage(size + " " + app.getString(R.string.shared_string_selected_lowercase));
	}

	private int getSelectedItemsCount() {
		int count = 0;
		for (Set<GpxDisplayItem> set : selectedItems.values()) {
			if (set != null) {
				count += set.size();
			}
		}
		return count;
	}

	@Override
	public void onPointsDeletionStarted() {

	}

	@Override
	public void onPointsDeleted() {
		updateGroups();
		update();
	}

	public void filter(String text) {
		adapter.getFilter().filter(text);
	}

	private class PointGPXAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

		private final List<GpxDisplayGroup> groups = new ArrayList<>();
		private final Map<GpxDisplayGroup, List<GpxDisplayItem>> itemGroups = new LinkedHashMap<>();
		private final Comparator<String> comparator;
		private Filter pointsFilter;
		private Set<?> filteredItems;

		PointGPXAdapter() {
			final Collator collator = OsmAndCollator.primaryCollator();
			comparator = new Comparator<String>() {
				@Override
				public int compare(String s1, String s2) {
					return collator.compare(s1, s2);
				}
			};
		}

		public void synchronizeGroups(@NonNull List<GpxDisplayGroup> displayGroups) {
			groups.clear();
			itemGroups.clear();
			Set<?> filtered = filteredItems;
			Collections.sort(displayGroups, new Comparator<GpxDisplayGroup>() {
				@Override
				public int compare(GpxDisplayGroup g1, GpxDisplayGroup g2) {
					int i1 = g1.getType().ordinal();
					int i2 = g2.getType().ordinal();
					return i1 < i2 ? -1 : (i1 == i2 ? 0 : 1);
				}
			});
			List<GpxDisplayGroup> trackPointsGroups = new ArrayList<>();
			List<GpxDisplayGroup> routePointsGroups = new ArrayList<>();
			for (GpxDisplayGroup group : displayGroups) {
				if (group.getType() == GpxDisplayItemType.TRACK_POINTS) {
					trackPointsGroups.add(group);
				} else if (group.getType() == GpxDisplayItemType.TRACK_ROUTE_POINTS) {
					routePointsGroups.add(group);
				}
			}
			processDisplayGroups(trackPointsGroups, filtered);
			processDisplayGroups(routePointsGroups, filtered);
			notifyDataSetChanged();
		}

		private void processDisplayGroups(List<GpxDisplayGroup> displayGroups, Set<?> filteredItems) {
			for (int i = 0; i < displayGroups.size(); i++) {
				GpxDisplayGroup group = displayGroups.get(i);
				if (group.getModifiableList().isEmpty()) {
					continue;
				}
				Map<String, List<GpxDisplayItem>> itemsMap = collectItemsByCategory(group, i);
				if (filteredItems != null) {
					itemsMap = filterItems(itemsMap, filteredItems);
				}
				if (!Algorithms.isEmpty(itemsMap)) {
					setCollectedItems(group, itemsMap);
				}
			}
		}

		private Map<String, List<GpxDisplayItem>> collectItemsByCategory(GpxDisplayGroup group, int index) {
			Map<String, List<GpxDisplayItem>> itemsMap = new HashMap<>();

			for (GpxDisplayItem item : group.getModifiableList()) {
				String category;
				if (item.locationStart != null) {
					if (group.getType() == GpxDisplayItemType.TRACK_POINTS) {
						category = item.locationStart.category;
						if (Algorithms.isEmpty(category)) {
							category = "";
						}
					} else {
						category = app.getString(R.string.route_points) + " " + (index + 1);
					}
				} else {
					category = "";
				}
				List<GpxDisplayItem> items = itemsMap.get(category);
				if (items == null) {
					items = new ArrayList<>();
					itemsMap.put(category, items);
				}
				items.add(item);
			}
			return itemsMap;
		}

		private Map<String, List<GpxDisplayItem>> filterItems(Map<String, List<GpxDisplayItem>> itemsMap, Set<?> filteredItems) {
			Map<String, List<GpxDisplayItem>> itemsMapFiltered = new HashMap<>();
			for (Entry<String, List<GpxDisplayItem>> e : itemsMap.entrySet()) {
				String category = e.getKey();
				List<GpxDisplayItem> items = e.getValue();
				if (filteredItems.contains(category)) {
					itemsMapFiltered.put(category, items);
				} else {
					for (GpxDisplayItem i : items) {
						if (filteredItems.contains(i)) {
							List<GpxDisplayItem> itemsFiltered = itemsMapFiltered.get(category);
							if (itemsFiltered == null) {
								itemsFiltered = new ArrayList<>();
								itemsMapFiltered.put(category, itemsFiltered);
							}
							itemsFiltered.add(i);
						}
					}
				}
			}
			return itemsMapFiltered;
		}

		private void setCollectedItems(GpxDisplayGroup group, Map<String, List<GpxDisplayItem>> itemsMap) {
			List<String> categories = new ArrayList<>(itemsMap.keySet());
			Collections.sort(categories, comparator);
			for (String category : categories) {
				List<GpxDisplayItem> values = itemsMap.get(category);
				GpxDisplayGroup headerGroup = group.cloneInstance();
				headerGroup.setName(category);
				for (GpxDisplayItem i : values) {
					if (i.locationStart != null && i.locationStart.getColor() != 0) {
						headerGroup.setColor(i.locationStart.getColor(group.getColor()));
						break;
					}
				}
				List<GpxDisplayItem> headerGroupItems = headerGroup.getModifiableList();
				headerGroupItems.clear();
				headerGroupItems.addAll(values);
				itemGroups.put(headerGroup, values);
				this.groups.add(headerGroup);
			}
		}

		@Override
		public int getGroupCount() {
			return groups.size();
		}

		@Override
		public GpxDisplayGroup getGroup(int groupPosition) {
			return groups.get(groupPosition);
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return itemGroups.get(groups.get(groupPosition)).size();
		}

		@Override
		public GpxDisplayItem getChild(int groupPosition, int childPosition) {
			return itemGroups.get(groups.get(groupPosition)).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return groupPosition * 10000 + childPosition;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public View getGroupView(final int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			final GpxDisplayGroup group = getGroup(groupPosition);
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = LayoutInflater.from(view.getContext());
				row = inflater.inflate(R.layout.wpt_list_item, parent, false);
			}

			row.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (listView.isGroupExpanded(groupPosition)) {
						listView.collapseGroup(groupPosition);
					} else {
						listView.expandGroup(groupPosition);
					}
				}
			});

			String categoryName = group.getName();
			if (TextUtils.isEmpty(categoryName)) {
				categoryName = app.getString(R.string.shared_string_gpx_points);
			}
			SpannableStringBuilder text = new SpannableStringBuilder(categoryName)
					.append(" – ")
					.append(String.valueOf(getChildrenCount(groupPosition)));
			text.setSpan(new ForegroundColorSpan(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.wikivoyage_primary_text_color)),
					0, categoryName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			text.setSpan(new ForegroundColorSpan(ContextCompat.getColor(app, R.color.wikivoyage_secondary_text)),
					categoryName.length() + 1, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

			TextView title = row.findViewById(R.id.label);
			title.setText(text);

			ImageView icon = row.findViewById(R.id.icon);
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_folder));

			boolean expanded = listView.isGroupExpanded(groupPosition);
			ImageView expandImage = row.findViewById(R.id.expand_image);
			expandImage.setImageDrawable(getContentIcon(expanded ? R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down));

			final CheckBox checkBox = (CheckBox) row.findViewById(R.id.toggle_item);
			if (selectionMode) {
				checkBox.setChecked(selectedGroups.contains(groupPosition));
				checkBox.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						List<GpxDisplayItem> items = itemGroups.get(group);
						setGroupSelection(items, groupPosition, checkBox.isChecked());
						adapter.notifyDataSetInvalidated();
						updateSelectionMode();
					}
				});
				AndroidUiHelper.updateVisibility(checkBox, true);
			} else {
				AndroidUiHelper.updateVisibility(checkBox, false);
			}

			ImageView options = (ImageView) row.findViewById(R.id.options);
			options.setImageDrawable(getContentIcon(R.drawable.ic_overflow_menu_white));
			options.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					EditTrackGroupDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
							group, mapActivity.getTrackMenuFragment());
				}
			});

			AndroidUiHelper.updateVisibility(expandImage, true);
			AndroidUiHelper.updateVisibility(row.findViewById(R.id.divider), true);
			AndroidUiHelper.updateVisibility(row.findViewById(R.id.description), false);
			AndroidUiHelper.updateVisibility(row.findViewById(R.id.list_divider), false);
			AndroidUiHelper.updateVisibility(row.findViewById(R.id.group_divider), true);

			return row;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				LayoutInflater inflater = LayoutInflater.from(view.getContext());
				row = inflater.inflate(R.layout.wpt_list_item, parent, false);
			}

			final GpxDisplayGroup group = getGroup(groupPosition);
			final GpxDisplayItem gpxItem = getChild(groupPosition, childPosition);

			TextView title = row.findViewById(R.id.label);
			title.setText(gpxItem.name);

			TextView description = row.findViewById(R.id.description);
			if (!Algorithms.isEmpty(gpxItem.description)) {
				description.setText(gpxItem.description);
				AndroidUiHelper.updateVisibility(description, true);
			} else {
				AndroidUiHelper.updateVisibility(description, false);
			}

			final CheckBox checkBox = (CheckBox) row.findViewById(R.id.toggle_item);
			if (selectionMode) {
				checkBox.setVisibility(View.VISIBLE);
				checkBox.setChecked(selectedItems.get(group.getType()) != null && selectedItems.get(group.getType()).contains(gpxItem));
				checkBox.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						if (checkBox.isChecked()) {
							Set<GpxDisplayItem> set = selectedItems.get(group.getType());
							if (set != null) {
								set.add(gpxItem);
							} else {
								set = new LinkedHashSet<>();
								set.add(gpxItem);
								selectedItems.put(group.getType(), set);
							}
						} else {
							Set<GpxDisplayItem> set = selectedItems.get(group.getType());
							if (set != null) {
								set.remove(gpxItem);
							}
						}
						updateSelectionMode();
					}
				});
				AndroidUiHelper.updateVisibility(checkBox, true);
				AndroidUiHelper.updateVisibility(row.findViewById(R.id.icon), false);
			} else {
				ImageView icon = row.findViewById(R.id.icon);
				if (GpxDisplayItemType.TRACK_POINTS == group.getType()) {
					WptPt wpt = gpxItem.locationStart;
					int groupColor = wpt.getColor(group.getColor());
					if (groupColor == 0) {
						groupColor = ContextCompat.getColor(app, R.color.gpx_color_point);
					}
					icon.setImageDrawable(PointImageDrawable.getFromWpt(app, groupColor, false, wpt));
				} else {
					icon.setImageDrawable(getContentIcon(R.drawable.ic_action_marker_dark));
				}
				AndroidUiHelper.updateVisibility(icon, true);
				AndroidUiHelper.updateVisibility(checkBox, false);
			}

			AndroidUiHelper.updateVisibility(row.findViewById(R.id.divider), false);
			AndroidUiHelper.updateVisibility(row.findViewById(R.id.options), false);
			AndroidUiHelper.updateVisibility(row.findViewById(R.id.list_divider), true);

			return row;
		}

		private void setGroupSelection(List<GpxDisplayItem> items, int groupPosition, boolean select) {
			GpxDisplayGroup group = groups.get(groupPosition);
			if (select) {
				selectedGroups.add(groupPosition);
				if (items != null) {
					Set<GpxDisplayItem> set = selectedItems.get(group.getType());
					if (set != null) {
						set.addAll(items);
					} else {
						set = new LinkedHashSet<>(items);
						selectedItems.put(group.getType(), set);
					}
				}
			} else {
				selectedGroups.remove(groupPosition);
				selectedItems.remove(group.getType());
			}
		}

		@Override
		public Filter getFilter() {
			if (pointsFilter == null) {
				pointsFilter = new PointsFilter();
			}
			return pointsFilter;
		}

		public void setFilterResults(Set<?> values) {
			this.filteredItems = values;
		}
	}

	public class PointsFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			if (constraint == null || constraint.length() == 0) {
				results.values = null;
				results.count = 1;
			} else {
				Set<Object> filter = new HashSet<>();
				String cs = constraint.toString().toLowerCase();
				for (GpxDisplayGroup g : getDisplayGroups()) {
					for (GpxDisplayItem i : g.getModifiableList()) {
						if (i.name.toLowerCase().contains(cs)) {
							filter.add(i);
						} else if (i.locationStart != null && !TextUtils.isEmpty(i.locationStart.category)
								&& i.locationStart.category.toLowerCase().contains(cs)) {
							filter.add(i.locationStart.category);
						}
					}
				}
				results.values = filter;
				results.count = filter.size();
			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			synchronized (adapter) {
				adapter.setFilterResults((Set<?>) results.values);
				adapter.synchronizeGroups(getDisplayGroups());
			}
			adapter.notifyDataSetChanged();
			expandAllGroups();
		}
	}
}