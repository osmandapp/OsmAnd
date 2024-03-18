package net.osmand.plus.track.cards;

import static net.osmand.plus.wikipedia.WikiArticleHelper.getPartialContent;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
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

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.OsmandBaseExpandableListAdapter;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.myplaces.tracks.tasks.DeletePointsTask;
import net.osmand.plus.myplaces.tracks.tasks.DeletePointsTask.OnPointsDeleteListener;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.track.fragments.DisplayGroupsBottomSheet.DisplayPointGroupsCallback;
import net.osmand.plus.track.fragments.EditTrackGroupDialogFragment;
import net.osmand.plus.track.helpers.DisplayPointsGroupsHelper;
import net.osmand.plus.track.helpers.DisplayPointsGroupsHelper.DisplayGroupsHolder;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrackPointsCard extends MapBaseCard implements OnChildClickListener, OnPointsDeleteListener,
		OsmAndCompassListener, OsmAndLocationListener {

	public static final int ADD_WAYPOINT_INDEX = 0;
	public static final int DELETE_WAYPOINTS_INDEX = 1;
	public static final int OPEN_WAYPOINT_INDEX = 2;

	private final TrackDisplayHelper displayHelper;
	private final SelectedGpxFile selectedGpxFile;

	private GpxDisplayGroup selectedGroup;
	private final Set<Integer> selectedGroups = new LinkedHashSet<>();
	private final LinkedHashMap<GpxDisplayItemType, Set<GpxDisplayItem>> selectedItems = new LinkedHashMap<>();
	private boolean selectionMode;

	private final PointGPXAdapter adapter;
	private ExpandableListView listView;
	private View actionsView;

	private Location lastLocation;
	private float lastHeading;
	private boolean locationDataUpdateAllowed = true;

	public TrackPointsCard(@NonNull MapActivity mapActivity,
	                       @NonNull TrackDisplayHelper displayHelper,
	                       @NonNull SelectedGpxFile selectedGpxFile) {
		super(mapActivity);
		this.displayHelper = displayHelper;
		this.selectedGpxFile = selectedGpxFile;
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
	public void updateContent() {
		listView = view.findViewById(android.R.id.list);
		listView.setOnChildClickListener(this);

		List<GpxDisplayGroup> displayGroups = getOriginalGroups();
		adapter.setFilterResults(null);
		adapter.synchronizeGroups(displayGroups);
		if (listView.getAdapter() == null) {
			listView.setAdapter(adapter);
		}

		listView.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				locationDataUpdateAllowed = scrollState == SCROLL_STATE_IDLE;
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			}
		});
		if (actionsView == null) {
			addActions();
		}
		expandAllGroups();
	}

	public void setSelectedGroup(GpxDisplayGroup selectedGroup) {
		this.selectedGroup = selectedGroup;
		onSelectedGroupChanged();
	}

	private void onSelectedGroupChanged() {
		if (selectedGroup != null) {
			scrollToGroup(selectedGroup);
		} else {
			scrollToInitialPosition();
		}
	}

	public void updateGroups() {
		selectedItems.clear();
		selectedGroups.clear();
	}

	private void scrollToGroup(@NonNull GpxDisplayGroup group) {
		int index = adapter.getGroupIndex(group);
		if (index >= 0) {
			listView.setSelectedGroup(index);
		}
	}

	private void scrollToInitialPosition() {
		if (listView.getCount() > 0) {
			listView.setSelectedGroup(0);
		}
	}

	public List<GpxDisplayGroup> getGroups() {
		return adapter.groups;
	}

	public void startListeningLocationUpdates() {
		OsmAndLocationProvider locationProvider = app.getLocationProvider();
		locationProvider.resumeAllUpdates();
		locationProvider.addCompassListener(this);
		locationProvider.addLocationListener(this);
		locationProvider.removeCompassListener(locationProvider.getNavigationInfo());
		onLocationDataUpdate();
	}

	public void stopListeningLocationUpdates() {
		OsmAndLocationProvider locationProvider = app.getLocationProvider();
		locationProvider.removeCompassListener(this);
		locationProvider.removeLocationListener(this);
		locationProvider.addCompassListener(locationProvider.getNavigationInfo());
	}

	private void addActions() {
		actionsView = themedInflater.inflate(R.layout.track_points_actions, listView, false);
		listView.addFooterView(actionsView);

		setupActionsHeader();
		setupAddWaypointAction();
		setupDeleteWaypointAction();

		View bottomMarginView = actionsView.findViewById(R.id.bottomMarginView);
		bottomMarginView.getLayoutParams().height = getDimen(R.dimen.card_row_min_height);
	}

	private void setupActionsHeader() {
		View header = actionsView.findViewById(R.id.header);
		TextView title = header.findViewById(android.R.id.title);
		title.setText(R.string.shared_string_actions);

		AndroidUiHelper.updateVisibility(header.findViewById(android.R.id.icon), false);
		AndroidUiHelper.updateVisibility(header.findViewById(android.R.id.summary), false);
	}

	private void setupAddWaypointAction() {
		View container = actionsView.findViewById(R.id.add_waypoint_button);
		TextView title = container.findViewById(android.R.id.title);
		ImageView icon = container.findViewById(android.R.id.icon);

		title.setText(R.string.add_waypoint);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_name_field));
		container.setOnClickListener(v -> notifyButtonPressed(ADD_WAYPOINT_INDEX));

		setupSelectableBackground(container);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.divider), !adapter.isEmpty());
	}

	private void setupDeleteWaypointAction() {
		View container = actionsView.findViewById(R.id.delete_waypoint_button);
		TextView title = container.findViewById(android.R.id.title);
		ImageView icon = container.findViewById(android.R.id.icon);

		title.setText(R.string.delete_waypoints);
		icon.setImageDrawable(getColoredIcon(R.drawable.ic_action_delete_dark, R.color.color_osm_edit_delete));
		container.setOnClickListener(v -> notifyButtonPressed(DELETE_WAYPOINTS_INDEX));
		setupSelectableBackground(container);
	}

	private void setupSelectableBackground(@NonNull View view) {
		Drawable drawable = UiUtilities.getSelectableDrawable(view.getContext());
		AndroidUtils.setBackground(view.findViewById(R.id.selectable_list_item), drawable);
	}

	private void expandAllGroups() {
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			listView.expandGroup(i);
		}
	}

	private List<GpxDisplayGroup> getOriginalGroups() {
		return displayHelper.getPointsOriginalGroups();
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		GpxDisplayItem item = adapter.getChild(groupPosition, childPosition);
		if (item != null && item.locationStart != null) {
			notifyButtonPressed(OPEN_WAYPOINT_INDEX);
			LatLon location = new LatLon(item.locationStart.lat, item.locationStart.lon);
			PointDescription description = new PointDescription(PointDescription.POINT_TYPE_WPT, item.name);

			MapContextMenu contextMenu = mapActivity.getContextMenu();
			contextMenu.setCenterMarker(true);
			contextMenu.show(location, description, item.locationStart);
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
	public void onPointsDeleted() {
		updateGroups();
		update();
		CardListener listener = getListener();
		if (listener instanceof DisplayPointGroupsCallback) {
			((DisplayPointGroupsCallback) listener).onPointGroupsVisibilityChanged();
		}
	}

	public void filter(String text) {
		adapter.getFilter().filter(text);
	}

	@Override
	public void updateCompassValue(float heading) {
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			lastHeading = heading;
			onLocationDataUpdate();
		}
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(lastLocation, location)) {
			lastLocation = location;
			onLocationDataUpdate();
		}
	}

	private void onLocationDataUpdate() {
		if (locationDataUpdateAllowed) {
			app.runInUIThread(adapter::notifyDataSetChanged);
		}
	}

	private class PointGPXAdapter extends OsmandBaseExpandableListAdapter implements Filterable {

		private static final int SPANNED_FLAG = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

		private final List<GpxDisplayGroup> groups = new ArrayList<>();
		private final Map<GpxDisplayGroup, List<GpxDisplayItem>> itemGroups = new LinkedHashMap<>();
		private Filter pointsFilter;
		private Set<?> filteredItems;

		private final UpdateLocationViewCache locationViewCache;

		PointGPXAdapter() {
			locationViewCache = UpdateLocationUtils.getUpdateLocationViewCache(activity);
		}

		public void synchronizeGroups(@NonNull List<GpxDisplayGroup> displayGroups) {
			DisplayGroupsHolder displayGroupsHolder = DisplayPointsGroupsHelper.getGroups(app, displayGroups, filteredItems);
			groups.clear();
			itemGroups.clear();
			groups.addAll(displayGroupsHolder.groups);
			itemGroups.putAll(displayGroupsHolder.itemGroups);
			notifyDataSetChanged();
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
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			GpxDisplayGroup group = getGroup(groupPosition);
			Context context = view.getContext();
			View row = convertView;
			if (row == null) {
				row = themedInflater.inflate(R.layout.track_points_group_item, parent, false);
			}

			row.setOnClickListener(v -> {
				if (listView.isGroupExpanded(groupPosition)) {
					listView.collapseGroup(groupPosition);
				} else {
					listView.expandGroup(groupPosition);
				}
			});

			String groupName = group.getName();
			boolean groupHidden = selectedGpxFile.isGroupHidden(groupName);
			String nameToDisplay = Algorithms.isEmpty(groupName)
					? app.getString(R.string.shared_string_gpx_points)
					: groupName;

			TextView title = row.findViewById(R.id.label);
			title.setText(createStyledGroupTitle(context, nameToDisplay, groupPosition, groupHidden));

			Drawable icon = groupHidden
					? getColoredIcon(R.drawable.ic_action_folder_hidden, ColorUtilities.getSecondaryTextColorId(nightMode))
					: getContentIcon(R.drawable.ic_action_folder);
			ImageView groupImage = row.findViewById(R.id.icon);
			groupImage.setImageDrawable(icon);

			boolean expanded = listView.isGroupExpanded(groupPosition);
			ImageView expandImage = row.findViewById(R.id.expand_image);
			expandImage.setImageDrawable(getContentIcon(expanded ? R.drawable.ic_action_arrow_up : R.drawable.ic_action_arrow_down));

			CheckBox checkBox = row.findViewById(R.id.toggle_item);
			if (selectionMode) {
				checkBox.setChecked(selectedGroups.contains(groupPosition));
				checkBox.setOnClickListener(v -> {
					List<GpxDisplayItem> items = itemGroups.get(group);
					setGroupSelection(items, groupPosition, checkBox.isChecked());
					adapter.notifyDataSetInvalidated();
					updateSelectionMode();
				});
			}
			AndroidUiHelper.updateVisibility(checkBox, selectionMode);
			AndroidUiHelper.updateVisibility(groupImage, !selectionMode);

			ImageView options = row.findViewById(R.id.options);
			options.setImageDrawable(getContentIcon(R.drawable.ic_overflow_menu_with_background));
			options.setOnClickListener(v ->
					EditTrackGroupDialogFragment.showInstance(mapActivity.getSupportFragmentManager(),
							group, mapActivity.getFragmentsHelper().getTrackMenuFragment()));
			return row;
		}

		private CharSequence createStyledGroupTitle(@NonNull Context context, @NonNull String displayName,
		                                            int groupPosition, boolean groupHidden) {
			SpannableStringBuilder spannedName = new SpannableStringBuilder(displayName)
					.append(" – ")
					.append(String.valueOf(getChildrenCount(groupPosition)));

			if (groupHidden) {
				int secondaryTextColor = ColorUtilities.getSecondaryTextColor(context, nightMode);
				spannedName.setSpan(new ForegroundColorSpan(secondaryTextColor), 0, spannedName.length(), SPANNED_FLAG);
				spannedName.setSpan(new StyleSpan(Typeface.ITALIC), 0, spannedName.length(), SPANNED_FLAG);
			} else {
				int nameColor = AndroidUtils.getColorFromAttr(context, R.attr.wikivoyage_primary_text_color);
				int countColor = ContextCompat.getColor(context, R.color.text_color_secondary_light);

				spannedName.setSpan(new ForegroundColorSpan(nameColor), 0, displayName.length(), SPANNED_FLAG);
				spannedName.setSpan(new ForegroundColorSpan(countColor), displayName.length() + 1,
						spannedName.length(), SPANNED_FLAG);
			}

			spannedName.setSpan(new StyleSpan(Typeface.BOLD), 0, displayName.length(), SPANNED_FLAG);

			return spannedName;
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
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				row = themedInflater.inflate(R.layout.track_points_list_item, parent, false);
			}

			GpxDisplayGroup group = getGroup(groupPosition);
			GpxDisplayItem gpxItem = getChild(groupPosition, childPosition);

			TextView title = row.findViewById(R.id.label);
			title.setText(gpxItem.name);

			TextView description = row.findViewById(R.id.waypoint_description);
			if (!Algorithms.isEmpty(gpxItem.description)) {
				String content = getPartialContent(gpxItem.description);
				content = content != null ? content.replace("\n", " ") : null;
				description.setText(content);
				AndroidUiHelper.updateVisibility(description, true);
			} else {
				AndroidUiHelper.updateVisibility(description, false);
			}

			ImageView icon = row.findViewById(R.id.icon);
			CheckBox checkBox = row.findViewById(R.id.toggle_item);
			if (selectionMode) {
				checkBox.setVisibility(View.VISIBLE);
				checkBox.setChecked(selectedItems.get(group.getType()) != null && selectedItems.get(group.getType()).contains(gpxItem));
				checkBox.setOnClickListener(v -> {
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
				});
			}
			if (GpxDisplayItemType.TRACK_POINTS == group.getType()) {
				WptPt wpt = gpxItem.locationStart;
				int groupColor = wpt.getColor(group.getColor());
				if (groupColor == 0) {
					groupColor = ContextCompat.getColor(app, R.color.gpx_color_point);
				}
				icon.setImageDrawable(PointImageUtils.getFromPoint(app, groupColor, false, wpt));
			} else {
				icon.setImageDrawable(getContentIcon(R.drawable.ic_action_marker_dark));
			}
			setupLocationData(locationViewCache, row, gpxItem.locationStart);

			AndroidUiHelper.updateVisibility(icon, !selectionMode);
			AndroidUiHelper.updateVisibility(checkBox, selectionMode);
			AndroidUiHelper.updateVisibility(row.findViewById(R.id.list_divider), childPosition != 0);

			return row;
		}

		public int getGroupIndex(@NonNull GpxDisplayGroup group) {
			String name = group.getName();
			for (GpxDisplayGroup g : groups) {
				if (Algorithms.objectEquals(name, g.getName())) {
					return groups.indexOf(g);
				}
			}
			return -1;
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

	public static void setupLocationData(@NonNull UpdateLocationViewCache cache, @NonNull View container, @NonNull WptPt point) {
		TextView text = container.findViewById(R.id.distance);
		ImageView arrow = container.findViewById(R.id.direction_arrow);

		OsmandApplication app = (OsmandApplication) container.getContext().getApplicationContext();
		UpdateLocationUtils.updateLocationView(app, cache, arrow, text, point.lat, point.lon);

		String address = point.getAddress();
		TextView addressContainer = container.findViewById(R.id.address);
		addressContainer.setText(address);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.bullet_icon), !Algorithms.isEmpty(address));
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
				for (GpxDisplayGroup g : getOriginalGroups()) {
					for (GpxDisplayItem i : g.getDisplayItems()) {
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
				adapter.synchronizeGroups(getOriginalGroups());
			}
			adapter.notifyDataSetChanged();
			expandAllGroups();
			onSelectedGroupChanged();
		}
	}
}