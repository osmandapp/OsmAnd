package net.osmand.plus.settings.fragments;

import static net.osmand.plus.settings.enums.HistorySource.NAVIGATION;
import static net.osmand.plus.settings.enums.HistorySource.SEARCH;
import static net.osmand.plus.utils.UiUtilities.CompoundButtonType.TOOLBAR;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndLocationProvider.OsmAndCompassListener;
import net.osmand.plus.OsmAndLocationProvider.OsmAndLocationListener;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.ui.DeleteAllDataConfirmationBottomSheet.OnConfirmDeletionListener;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.enums.HistorySource;
import net.osmand.plus.settings.fragments.DeleteHistoryTask.DeleteHistoryListener;
import net.osmand.plus.settings.fragments.HistoryAdapter.OnItemSelectedListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class HistoryItemsFragment extends BaseFullScreenDialogFragment implements OnItemSelectedListener,
		OsmAndCompassListener, OsmAndLocationListener, OnConfirmDeletionListener, DeleteHistoryListener {

	protected final List<Object> items = new ArrayList<>();
	protected final Set<Object> selectedItems = new HashSet<>();
	protected final Map<Integer, List<?>> itemsGroups = new HashMap<>();

	protected View appbar;
	protected DialogButton deleteButton;
	protected DialogButton selectAllButton;
	protected ImageButton shareButton;
	protected HistoryAdapter adapter;
	protected RecyclerView recyclerView;
	protected View warningCard;

	private Float heading;
	private Location location;
	private boolean locationUpdateStarted;
	private boolean compassUpdateAllowed = true;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		updateHistoryItems();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		MapActivity mapActivity = (MapActivity) requireActivity();
		View view = inflate(R.layout.history_preferences_fragment, container, false);

		appbar = view.findViewById(R.id.appbar);
		recyclerView = view.findViewById(R.id.list);
		recyclerView.setLayoutManager(new LinearLayoutManager(mapActivity));
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				compassUpdateAllowed = newState == RecyclerView.SCROLL_STATE_IDLE;
			}
		});

		adapter = new HistoryAdapter(mapActivity, this, nightMode);
		adapter.updateSettingsItems(items, itemsGroups, selectedItems);
		recyclerView.setAdapter(adapter);

		setupToolbar(view);
		setupButtons(view);
		setupWarningCard(view);
		updateDisabledItems();

		return view;
	}

	protected abstract void shareItems();

	protected abstract void updateHistoryItems();

	protected abstract boolean isHistoryEnabled();

	public void clearItems() {
		items.clear();
		itemsGroups.clear();
		selectedItems.clear();
	}

	protected void setupToolbar(@NonNull View appbar) {
		ViewCompat.setElevation(appbar, 5.0f);
		ImageView closeButton = appbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> {
			dismiss();
		});

		ViewGroup container = appbar.findViewById(R.id.actions_container);
		LayoutInflater inflater = UiUtilities.getInflater(appbar.getContext(), nightMode);
		shareButton = (ImageButton) inflater.inflate(R.layout.action_button, container, false);
		shareButton.setOnClickListener(v -> {
			if (selectedItems.isEmpty()) {
				app.showShortToastMessage(R.string.no_items_selected_warning);
			} else {
				shareItems();
			}
		});
		container.addView(shareButton);
		updateToolbarSwitch(appbar);
	}

	protected void updateToolbarSwitch(@NonNull View view) {
		boolean checked = isHistoryEnabled();

		if (checked && !selectedItems.isEmpty()) {
			shareButton.setImageDrawable(getIcon(R.drawable.ic_action_upload));
		} else {
			int color = ContextCompat.getColor(app, R.color.active_buttons_and_links_text_light);
			int colorWithAlpha = ColorUtilities.getColorWithAlpha(color, 0.5f);
			shareButton.setImageDrawable(getPaintedIcon(R.drawable.ic_action_upload, colorWithAlpha));
		}
		shareButton.setEnabled(checked);

		View selectableView = view.findViewById(R.id.selectable_item);

		SwitchCompat switchView = selectableView.findViewById(R.id.switchWidget);
		switchView.setChecked(checked);
		UiUtilities.setupCompoundButton(switchView, nightMode, TOOLBAR);

		TextView title = selectableView.findViewById(R.id.switchButtonText);
		title.setText(checked ? R.string.shared_string_enabled : R.string.shared_string_disabled);

		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, ColorUtilities.getActiveColor(app, nightMode), 0.3f);
		AndroidUtils.setBackground(selectableView, drawable);

		int color = checked ? ColorUtilities.getActiveColor(app, nightMode) : ContextCompat.getColor(app, R.color.preference_top_switch_off);
		View switchContainer = view.findViewById(R.id.toolbar_switch_container);
		AndroidUtils.setBackground(switchContainer, new ColorDrawable(color));
	}

	protected void setupButtons(@NonNull View view) {
		View buttonsContainer = view.findViewById(R.id.bottom_buttons_container);
		buttonsContainer.setBackgroundColor(AndroidUtils.getColorFromAttr(view.getContext(), R.attr.bg_color));

		deleteButton = view.findViewById(R.id.right_bottom_button);
		deleteButton.setButtonType(DialogButtonType.PRIMARY);
		deleteButton.setTitleId(R.string.shared_string_delete);
		deleteButton.setOnClickListener(v -> {
			FragmentManager fragmentManager = getFragmentManager();
			if (fragmentManager != null) {
				DeleteHistoryConfirmationBottomSheet.showInstance(fragmentManager, selectedItems.size(), this);
			}
		});
		selectAllButton = view.findViewById(R.id.dismiss_button);
		selectAllButton.setButtonType(DialogButtonType.SECONDARY);
		selectAllButton.setTitleId(R.string.shared_string_select_all);
		selectAllButton.setOnClickListener(v -> {
			if (isAllItemsSelected()) {
				selectedItems.clear();
			} else {
				selectedItems.addAll(getAllItems());
			}
			updateSelectAllButton();
			updateButtonsState();
			adapter.notifyDataSetChanged();
		});
		updateSelectAllButton();

		AndroidUiHelper.updateVisibility(deleteButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);
	}

	protected void setupWarningCard(@NonNull View view) {
		warningCard = view.findViewById(R.id.disabled_history_card);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.settings_button_container), false);
	}

	protected void updateDisabledItems() {
		boolean checked = isHistoryEnabled();
		AndroidUiHelper.updateVisibility(recyclerView, checked);
		AndroidUiHelper.updateVisibility(warningCard, !checked);
		updateButtonsState();
	}

	protected void updateButtonsState() {
		boolean checked = isHistoryEnabled();
		selectAllButton.setEnabled(checked);
		deleteButton.setEnabled(checked && !selectedItems.isEmpty());
	}

	private void updateSelectAllButton() {
		selectAllButton.setTitleId(isAllItemsSelected() ?
				R.string.shared_string_deselect_all :
				R.string.shared_string_select_all);
	}

	private boolean isAllItemsSelected() {
		return selectedItems.size() == getAllItems().size();
	}

	@NonNull
	public List<Object> getAllItems() {
		List<Object> allItems = new ArrayList<>();
		for (List<?> items : itemsGroups.values()) {
			allItems.addAll(items);
		}
		return allItems;
	}

	@Override
	public void onItemSelected(Object item, boolean selected) {
		if (selected) {
			selectedItems.add(item);
		} else {
			selectedItems.remove(item);
		}
		updateToolbarSwitch(appbar);
		updateSelectAllButton();
		updateButtonsState();
	}

	@Override
	public void onCategorySelected(List<Object> items, boolean selected) {
		if (selected) {
			selectedItems.addAll(items);
		} else {
			selectedItems.removeAll(items);
		}
		updateToolbarSwitch(appbar);
		updateSelectAllButton();
		updateButtonsState();
	}

	@Override
	public void onDeletionConfirmed() {
		DeleteHistoryTask deleteHistoryTask = new DeleteHistoryTask(requireActivity(), selectedItems, this);
		OsmAndTaskManager.executeTask(deleteHistoryTask);
	}

	public void onDeletionComplete() {
		updateHistoryItems();
		updateButtonsState();
		adapter.notifyDataSetChanged();
		Fragment fragment = getTargetFragment();
		if (fragment instanceof OnPreferenceChanged) {
			((OnPreferenceChanged) fragment).onPreferenceChanged(settings.SEARCH_HISTORY.getId());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		startLocationUpdate();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopLocationUpdate();
	}

	@Override
	public void updateLocation(Location location) {
		if (!MapUtils.areLatLonEqual(this.location, location)) {
			this.location = location;
			updateLocationUi();
		}
	}

	@Override
	public void updateCompassValue(float value) {
		// 99 in next line used to one-time initialize arrows (with reference vs. fixed-north direction)
		// on non-compass devices
		float lastHeading = heading != null ? heading : 99;
		heading = value;
		if (Math.abs(MapUtils.degreesDiff(lastHeading, heading)) > 5) {
			updateLocationUi();
		} else {
			heading = lastHeading;
		}
	}

	private void updateLocationUi() {
		if (compassUpdateAllowed && adapter != null) {
			app.runInUIThread(() -> {
				if (location == null) {
					location = app.getLocationProvider().getLastKnownLocation();
				}
				adapter.notifyDataSetChanged();
			});
		}
	}

	private void startLocationUpdate() {
		if (!locationUpdateStarted) {
			locationUpdateStarted = true;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeCompassListener(locationProvider.getNavigationInfo());
			locationProvider.addCompassListener(this);
			locationProvider.addLocationListener(this);
			updateLocationUi();
		}
	}

	private void stopLocationUpdate() {
		if (locationUpdateStarted) {
			locationUpdateStarted = false;
			OsmAndLocationProvider locationProvider = app.getLocationProvider();
			locationProvider.removeLocationListener(this);
			locationProvider.removeCompassListener(this);
			locationProvider.addCompassListener(locationProvider.getNavigationInfo());
		}
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull HistorySource source, @Nullable Fragment target) {
		if (source == NAVIGATION) {
			NavigationHistorySettingsFragment.showInstance(manager, target);
		} else if (source == SEARCH) {
			SearchHistorySettingsFragment.showInstance(manager, target);
		}
	}
}