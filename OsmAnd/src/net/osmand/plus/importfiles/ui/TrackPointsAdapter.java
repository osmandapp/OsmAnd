package net.osmand.plus.importfiles.ui;

import static net.osmand.plus.track.cards.TrackPointsCard.setupLocationData;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.OsmandBaseExpandableListAdapter;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.track.helpers.DisplayPointsGroupsHelper;
import net.osmand.plus.track.helpers.DisplayPointsGroupsHelper.DisplayGroupsHolder;
import net.osmand.plus.track.helpers.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxDisplayItem;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.utils.UpdateLocationUtils;
import net.osmand.plus.utils.UpdateLocationUtils.UpdateLocationViewCache;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TrackPointsAdapter extends OsmandBaseExpandableListAdapter {

	private final OsmandApplication app;
	private final LayoutInflater inflater;
	private final UiUtilities uiUtilities;
	private final UpdateLocationViewCache viewCache;

	private final Set<WptPt> selectedPoints;
	private final List<GpxDisplayGroup> groups = new ArrayList<>();
	private final Map<GpxDisplayGroup, List<WptPt>> pointsGroups = new LinkedHashMap<>();

	private OnItemSelectedListener listener;

	private final boolean nightMode;

	TrackPointsAdapter(@NonNull Context context, @Nullable Set<WptPt> selectedPoints, boolean nightMode) {
		this.app = (OsmandApplication) context.getApplicationContext();
		this.nightMode = nightMode;
		this.selectedPoints = selectedPoints;
		inflater = UiUtilities.getInflater(app, nightMode);
		uiUtilities = app.getUIUtilities();
		viewCache = UpdateLocationUtils.getUpdateLocationViewCache(context);
	}

	public void setListener(@Nullable OnItemSelectedListener listener) {
		this.listener = listener;
	}

	public void synchronizeGroups(@NonNull List<GpxDisplayGroup> displayGroups) {
		groups.clear();
		pointsGroups.clear();

		DisplayGroupsHolder groupsHolder = DisplayPointsGroupsHelper.getGroups(app, displayGroups, null);
		groups.addAll(groupsHolder.groups);
		for (Map.Entry<GpxDisplayGroup, List<GpxDisplayItem>> entry : groupsHolder.itemGroups.entrySet()) {
			List<WptPt> points = new ArrayList<>();
			for (GpxDisplayItem item : entry.getValue()) {
				points.add(item.locationStart);
			}
			pointsGroups.put(entry.getKey(), points);
		}
		notifyDataSetChanged();
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View view, ViewGroup parent) {
		if (view == null) {
			view = inflater.inflate(R.layout.points_group_item, parent, false);
		}
		GpxDisplayGroup group = getGroup(groupPosition);
		List<WptPt> points = pointsGroups.get(group);

		String name = group.getName();
		String nameToDisplay = Algorithms.isEmpty(name) ? app.getString(R.string.shared_string_gpx_points) : name;

		TextView title = view.findViewById(R.id.title);
		title.setText(nameToDisplay);

		int selectedCount = 0;
		for (WptPt point : points) {
			if (selectedPoints.contains(point)) {
				selectedCount++;
			}
		}
		String count = app.getString(R.string.ltr_or_rtl_combine_via_slash, String.valueOf(selectedCount), String.valueOf(points.size()));
		TextView description = view.findViewById(R.id.description);
		description.setText(count);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(selectedPoints.containsAll(points));
		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);

		view.findViewById(R.id.compound_container).setOnClickListener(v -> {
			boolean selected = !compoundButton.isChecked();
			if (listener != null) {
				listener.onCategorySelected(points, selected);
			}
			notifyDataSetChanged();
		});

		int color = group.getColor();
		if (color == 0) {
			color = ContextCompat.getColor(app, R.color.gpx_color_point);
		}
		ImageView groupImage = view.findViewById(R.id.icon);
		groupImage.setImageDrawable(uiUtilities.getPaintedIcon(R.drawable.ic_action_folder, color));

		adjustIndicator(app, groupPosition, isExpanded, view, !nightMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_top_divider), true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_bottom_divider), !isExpanded);

		return view;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
		if (view == null) {
			view = inflater.inflate(R.layout.track_point_item, parent, false);
		}
		GpxDisplayGroup group = getGroup(groupPosition);
		WptPt point = getChild(groupPosition, childPosition);

		TextView title = view.findViewById(R.id.title);
		title.setText(point.name);

		CompoundButton compoundButton = view.findViewById(R.id.compound_button);
		compoundButton.setChecked(selectedPoints.contains(point));
		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);

		view.setOnClickListener(v -> {
			boolean selected = !compoundButton.isChecked();
			if (listener != null) {
				listener.onItemSelected(point, selected);
			}
			notifyDataSetChanged();
		});

		int color = point.getColor(group.getColor());
		if (color == 0) {
			color = ContextCompat.getColor(app, R.color.gpx_color_point);
		}
		ImageView icon = view.findViewById(R.id.icon);
		icon.setImageDrawable(PointImageUtils.getFromPoint(app, color, false, point));

		setupLocationData(viewCache, view, point);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.list_divider), childPosition != 0);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_bottom_divider), isLastChild);

		return view;
	}

	@Override
	public int getGroupCount() {
		return pointsGroups.size();
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
		return pointsGroups.get(groups.get(groupPosition)).size();
	}

	@Override
	public WptPt getChild(int groupPosition, int childPosition) {
		return pointsGroups.get(groups.get(groupPosition)).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	interface OnItemSelectedListener {

		void onItemSelected(WptPt point, boolean selected);

		void onCategorySelected(List<WptPt> points, boolean selected);

	}
}
