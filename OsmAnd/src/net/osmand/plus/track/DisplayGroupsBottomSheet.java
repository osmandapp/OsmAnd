package net.osmand.plus.track;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.CompoundButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.SelectionBottomSheet.SelectableItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DisplayGroupsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = DisplayGroupsBottomSheet.class.getSimpleName();

	private OsmandApplication app;
	private LayoutInflater inflater;
	private UiUtilities uiUtilities;

	private List<SelectableItem> allItems = new ArrayList<>();
	private Set<SelectableItem> visibleItems = new HashSet<>();
	private Map<SelectableItem, View> listViews = new HashMap<>();
	private LinearLayout listContainer;
	private TextView sizeIndication;
	private View hideAllButton;

	private int defaultIconColor;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = requiredMyApplication();
		uiUtilities = app.getUIUtilities();
		inflater = UiUtilities.getInflater(requireContext(), nightMode);
		defaultIconColor = ColorUtilities.getDefaultIconColor(app, nightMode);

		View view = inflater.inflate(R.layout.bottom_sheet_display_groups_visibility, null);
		sizeIndication = view.findViewById(R.id.selected_size);
		hideAllButton = view.findViewById(R.id.hide_all_button);
		listContainer = view.findViewById(R.id.list);
		createItemsList();
		setupHideAllButton();
		items.add(new SimpleBottomSheetItem.Builder().setCustomView(view).create());
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		fullUpdate();
	}

	private void createItemsList() {
		listContainer.removeAllViews();
		listViews.clear();
		for (SelectableItem item : allItems) {
			View view = inflater.inflate(R.layout.bottom_sheet_item_with_descr_and_switch_56dp, listContainer, false);
			TextView title = view.findViewById(R.id.title);
			title.setText(item.getTitle());

			TextView description = view.findViewById(R.id.description);
			description.setText(item.getDescription());

			CompoundButton cb = view.findViewById(R.id.compound_button);
			UiUtilities.setupCompoundButton(cb, nightMode, CompoundButtonType.GLOBAL);

			view.setOnClickListener(v -> {
				boolean newVisible = !cb.isChecked();
				if (newVisible) {
					visibleItems.add(item);
				} else {
					visibleItems.remove(item);
				}
				if (getTargetFragment() instanceof DisplayPointGroupsVisibilityListener) {
					DisplayPointGroupsVisibilityListener listener =
							(DisplayPointGroupsVisibilityListener) getTargetFragment();
					listener.onChangePointsGroupVisibility(item, newVisible);
					fullUpdate();
				}
			});

			listContainer.addView(view);
			listViews.put(item, view);
		}
	}

	private void setupHideAllButton() {
		hideAllButton.setOnClickListener(view -> {
			visibleItems.clear();
			if (getTargetFragment() instanceof DisplayPointGroupsVisibilityListener) {
				DisplayPointGroupsVisibilityListener listener =
						(DisplayPointGroupsVisibilityListener) getTargetFragment();
				listener.onHideAllPointGroups(allItems);
				fullUpdate();
			}
		});
	}

	private void fullUpdate() {
		updateSizeView();
		updateList();
	}

	private void updateSizeView() {
		String description = getString(
				R.string.ltr_or_rtl_combine_via_slash,
				String.valueOf(visibleItems.size()),
				String.valueOf(allItems.size())
		);
		sizeIndication.setText(description);
	}

	private void updateList() {
		for (SelectableItem item : allItems) {
			View view = listViews.get(item);
			if (view == null) {
				continue;
			}
			boolean isVisible = visibleItems.contains(item);
			int iconId = isVisible ? R.drawable.ic_action_folder : R.drawable.ic_action_folder_hidden;
			int iconColor = item.getColor();
			if (iconColor == 0 || !isVisible) {
				iconColor = defaultIconColor;
			}
			ImageView icon = view.findViewById(R.id.icon);
			icon.setImageDrawable(uiUtilities.getPaintedIcon(iconId, iconColor));
			CompoundButton cb = view.findViewById(R.id.compound_button);
			cb.setChecked(isVisible);
		}
	}

	public static DisplayGroupsBottomSheet showInstance(@NonNull AppCompatActivity activity,
	                                                    @NonNull List<SelectableItem> allItems,
	                                                    @Nullable Set<SelectableItem> visibleItems,
	                                                    @NonNull Fragment targetFragment,
	                                                    boolean usedOnMap) {
		DisplayGroupsBottomSheet fragment = new DisplayGroupsBottomSheet();
		fragment.setUsedOnMap(usedOnMap);
		fragment.setTargetFragment(targetFragment, 0);
		fragment.allItems = allItems;
		fragment.visibleItems = visibleItems;
		FragmentManager fm = activity.getSupportFragmentManager();
		fragment.show(fm, TAG);
		return fragment;
	}

	public interface DisplayPointGroupsVisibilityListener {
		void onChangePointsGroupVisibility(SelectableItem item, boolean newState);
		void onHideAllPointGroups(List<SelectableItem> items);
	}

}
