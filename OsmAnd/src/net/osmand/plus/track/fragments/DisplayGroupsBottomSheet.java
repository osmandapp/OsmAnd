package net.osmand.plus.track.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.PointsGroup;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.SelectionBottomSheet.SelectableItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.myplaces.tracks.tasks.UpdatePointsGroupsTask;
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
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayGroupsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = DisplayGroupsBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private TrackDisplayHelper displayHelper;
	private SelectedGpxFile selectedGpxFile;

	private final List<SelectableItem<GpxDisplayGroup>> uiItems = new ArrayList<>();
	private final Map<SelectableItem<GpxDisplayGroup>, View> listViews = new HashMap<>();
	private LayoutInflater inflater;
	private LinearLayout listContainer;
	private TextView sizeIndication;
	private View stateButton;

	private DisplayPointGroupsCallback callback;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initData();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		inflater = UiUtilities.getInflater(requireContext(), nightMode);
		View view = inflater.inflate(R.layout.bottom_sheet_display_groups_visibility, null);
		sizeIndication = view.findViewById(R.id.selected_size);
		stateButton = view.findViewById(R.id.state_button);
		listContainer = view.findViewById(R.id.list);
		updateListItems();
		setupStateButton();
		items.add(new SimpleBottomSheetItem.Builder().setCustomView(view).create());
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		fullUpdate();
	}

	private void initData() {
		app = requiredMyApplication();
		callback = (DisplayPointGroupsCallback) getTargetFragment();
		if (callback != null) {
			displayHelper = callback.getDisplayHelper();
			selectedGpxFile = callback.getSelectedGpxFile();
			initSelectableItems();
		}
	}

	private void initSelectableItems() {
		List<GpxDisplayGroup> displayGroups = displayHelper.getPointsOriginalGroups();
		DisplayGroupsHolder groupsHolder = DisplayPointsGroupsHelper.getGroups(app, displayGroups, null);
		uiItems.clear();
		for (GpxDisplayGroup group : groupsHolder.groups) {
			if (group.getType() != GpxDisplayItemType.TRACK_POINTS) {
				continue;
			}

			SelectableItem<GpxDisplayGroup> uiItem = new SelectableItem<>();
			List<GpxDisplayItem> groupItems = groupsHolder.itemGroups.get(group);

			String categoryName = group.getName();
			if (TextUtils.isEmpty(categoryName)) {
				categoryName = app.getString(R.string.shared_string_gpx_points);
			}
			uiItem.setTitle(categoryName);
			uiItem.setColor(group.getColor());

			int size = groupItems != null ? groupItems.size() : 0;
			uiItem.setDescription(String.valueOf(size));

			uiItem.setObject(group);
			uiItems.add(uiItem);
		}
	}

	private void updateStateButton() {
		TextView title = stateButton.findViewById(R.id.state_button_text);
		if (isAnyGroupVisible()) {
			title.setText(R.string.shared_string_hide_all);
		} else {
			title.setText(R.string.shared_string_show_all);
		}
	}

	private void updateListItems() {
		listContainer.removeAllViews();
		listViews.clear();
		for (SelectableItem<GpxDisplayGroup> item : uiItems) {
			View view = inflater.inflate(R.layout.bottom_sheet_item_with_descr_and_switch_56dp, listContainer, false);
			TextView title = view.findViewById(R.id.title);
			title.setText(item.getTitle());

			TextView description = view.findViewById(R.id.description);
			description.setText(item.getDescription());

			CompoundButton cb = view.findViewById(R.id.compound_button);
			UiUtilities.setupCompoundButton(cb, nightMode, CompoundButtonType.GLOBAL);

			view.setOnClickListener(v -> {
				GpxDisplayGroup group = item.getObject();
				updateGroupVisibility(group.getName(), !cb.isChecked());
				callback.onPointGroupsVisibilityChanged();
				fullUpdate();
			});

			listContainer.addView(view);
			listViews.put(item, view);
		}
	}

	private void setupStateButton() {
		stateButton.setOnClickListener(view -> {
			boolean newState = !isAnyGroupVisible();
			for (String groupName : getGroupsNames()) {
				updateGroupVisibility(groupName, newState);
			}
			callback.onPointGroupsVisibilityChanged();
			fullUpdate();
		});
	}

	private void updateGroupVisibility(String groupName, boolean visible) {
		GPXFile gpxFile = selectedGpxFile.getGpxFile();
		PointsGroup pointsGroup = gpxFile.getPointsGroups().get(groupName);
		if (pointsGroup != null) {
			pointsGroup.setHidden(!visible);
		}
	}

	private List<String> getGroupsNames() {
		List<String> names = new ArrayList<>();
		for (SelectableItem<GpxDisplayGroup> item : uiItems) {
			GpxDisplayGroup group = item.getObject();
			names.add(group.getName());
		}
		return names;
	}

	private void fullUpdate() {
		updateStateButton();
		updateGroupsNumberRatio();
		updateList();
	}

	private void updateGroupsNumberRatio() {
		int visibleCount = getVisibleGroupsNumber();
		int totalCount = uiItems.size();
		String description = getString(
				R.string.ltr_or_rtl_combine_via_slash,
				String.valueOf(visibleCount),
				String.valueOf(totalCount)
		);
		sizeIndication.setText(description);
	}

	private void updateList() {
		int defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		for (SelectableItem<GpxDisplayGroup> item : uiItems) {
			View view = listViews.get(item);
			if (view == null) {
				continue;
			}

			GpxDisplayGroup group = item.getObject();
			boolean isVisible = group != null && !selectedGpxFile.isGroupHidden(group.getName());
			int iconId = isVisible ? R.drawable.ic_action_folder : R.drawable.ic_action_folder_hidden;
			int iconColor = item.getColor();
			if (iconColor == 0 || !isVisible) {
				iconColor = defaultIconColor;
			}
			ImageView icon = view.findViewById(R.id.icon);
			icon.setImageDrawable(getPaintedIcon(iconId, iconColor));
			CompoundButton cb = view.findViewById(R.id.compound_button);
			cb.setChecked(isVisible);
		}
	}

	private int getVisibleGroupsNumber() {
		int visibleGroupsCount = 0;
		for (SelectableItem<GpxDisplayGroup> selectableItem : uiItems) {
			GpxDisplayGroup group = selectableItem.getObject();
			if (group != null && !selectedGpxFile.isGroupHidden(group.getName())) {
				visibleGroupsCount++;
			}
		}
		return visibleGroupsCount;
	}

	private boolean isAnyGroupVisible() {
		return getVisibleGroupsNumber() > 0;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	public void onDestroy() {
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			updateGroupsVisibility();
		}
		super.onDestroy();
	}

	private void updateGroupsVisibility() {
		GPXFile gpxFile = selectedGpxFile.getGpxFile();
		MapActivity activity = (MapActivity) getActivity();
		if (activity != null) {
			Map<String, PointsGroup> groups = new HashMap<>(gpxFile.getPointsGroups());
			UpdatePointsGroupsTask task = new UpdatePointsGroupsTask(activity, gpxFile, groups, null);
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull Fragment target, boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			DisplayGroupsBottomSheet fragment = new DisplayGroupsBottomSheet();
			fragment.setUsedOnMap(usedOnMap);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
		}
	}

	public interface DisplayPointGroupsCallback {

		void onPointGroupsVisibilityChanged();

		SelectedGpxFile getSelectedGpxFile();

		TrackDisplayHelper getDisplayHelper();
	}
}