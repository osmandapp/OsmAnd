package net.osmand.plus.settings.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.github.ksoichiro.android.observablescrollview.ScrollUtils;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.fragments.ExportSettingsAdapter.OnItemSelectedListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseSettingsListFragment extends BaseOsmAndFragment implements OnItemSelectedListener {

	public static final String SETTINGS_LIST_TAG = "settings_list_tag";

	protected Map<ExportType, List<?>> selectedItemsMap = new EnumMap<>(ExportType.class);
	protected Map<ExportCategory, SettingsCategoryItems> dataList = new LinkedHashMap<>();

	protected View header;
	protected View continueBtn;
	protected View headerShadow;
	protected View headerDivider;
	protected View itemsSizeContainer;
	protected View availableSpaceContainer;
	protected TextViewEx selectedItemsSize;
	protected TextViewEx availableSpaceDescr;
	protected LinearLayout buttonsContainer;
	protected ExpandableListView expandableList;
	protected ExportSettingsAdapter adapter;

	protected boolean exportMode;
	private boolean wasDrawerDisabled;

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (hasSelectedData()) {
					showExitDialog();
				} else {
					dismissFragment();
				}
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View root = themedInflater.inflate(R.layout.fragment_import, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), root);

		selectedItemsSize = root.findViewById(R.id.file_size);
		itemsSizeContainer = root.findViewById(R.id.file_size_container);
		expandableList = root.findViewById(R.id.list);
		buttonsContainer = root.findViewById(R.id.buttons_container);

		Toolbar toolbar = root.findViewById(R.id.toolbar);
		setupToolbar(toolbar);
		ViewCompat.setNestedScrollingEnabled(expandableList, true);

		header = themedInflater.inflate(R.layout.list_item_description_header, null);
		headerDivider = header.findViewById(R.id.divider);
		headerShadow = header.findViewById(R.id.card_bottom_divider);
		expandableList.addHeaderView(header);

		availableSpaceContainer = themedInflater.inflate(R.layout.enough_space_warning_card, null);
		availableSpaceDescr = availableSpaceContainer.findViewById(R.id.warning_descr);

		continueBtn = root.findViewById(R.id.continue_button);
		root.findViewById(R.id.continue_button_container).setOnClickListener(v -> {
			if (expandableList.getHeaderViewsCount() <= 1) {
				if (hasSelectedData()) {
					onContinueButtonClickAction();
				}
			} else {
				expandableList.smoothScrollToPosition(0);
			}
		});

		adapter = new ExportSettingsAdapter(app, exportMode, this, nightMode);
		adapter.updateSettingsItems(dataList, selectedItemsMap);
		expandableList.setAdapter(adapter);
		setupListView(expandableList);
		updateAvailableSpace();

		return root;
	}

	protected abstract void onContinueButtonClickAction();

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
		}
	}

	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !wasDrawerDisabled) {
			mapActivity.enableDrawer();
		}
	}

	protected void dismissFragment() {
		FragmentManager fm = getFragmentManager();
		if (fm != null && !fm.isStateSaved()) {
			getFragmentManager().popBackStack(SETTINGS_LIST_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		}
	}

	public void showExitDialog() {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), nightMode);
		AlertDialog.Builder dismissDialog = new AlertDialog.Builder(themedContext);
		dismissDialog.setTitle(getString(R.string.shared_string_dismiss));
		dismissDialog.setMessage(getString(R.string.exit_without_saving));
		dismissDialog.setNegativeButton(R.string.shared_string_cancel, null);
		dismissDialog.setPositiveButton(R.string.shared_string_exit, (dialog, which) -> dismissFragment());
		dismissDialog.show();
	}

	private void setupToolbar(Toolbar toolbar) {
		int color = ColorUtilities.getActiveButtonsAndLinksTextColor(app, nightMode);
		toolbar.setNavigationIcon(getPaintedContentIcon(R.drawable.ic_action_close, color));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(v -> {
			if (hasSelectedData()) {
				showExitDialog();
			} else {
				dismissFragment();
			}
		});
	}

	public static void setupListView(@NonNull ListView listView) {
		if (listView.getFooterViewsCount() == 0) {
			Context context = listView.getContext();
			int padding = context.getResources().getDimensionPixelSize(R.dimen.toolbar_height_expanded);

			View emptyView = new View(context);
			emptyView.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, padding));
			listView.addFooterView(emptyView);
			ScrollUtils.addOnGlobalLayoutListener(listView, listView::requestLayout);
		}
	}

	protected void updateAvailableSpace() {
		long calculatedSize = ExportSettingsAdapter.calculateItemsSize(adapter.getData());
		if (calculatedSize != 0) {
			selectedItemsSize.setText(AndroidUtils.formatSize(app, calculatedSize));

			long availableSizeBytes = AndroidUtils.getAvailableSpace(app);
			if (calculatedSize > availableSizeBytes) {
				String availableSize = AndroidUtils.formatSize(app, availableSizeBytes);
				availableSpaceDescr.setText(getString(R.string.export_not_enough_space_descr, availableSize));
				updateWarningHeaderVisibility(true);
				continueBtn.setEnabled(false);
			} else {
				updateWarningHeaderVisibility(false);
				continueBtn.setEnabled(hasSelectedData());
			}
			itemsSizeContainer.setVisibility(View.VISIBLE);
		} else {
			updateWarningHeaderVisibility(false);
			itemsSizeContainer.setVisibility(View.INVISIBLE);
			continueBtn.setEnabled(hasSelectedData());
		}
	}

	public boolean hasSelectedData() {
		for (List<?> items : selectedItemsMap.values()) {
			if (!Algorithms.isEmpty(items)) {
				return true;
			}
		}
		return false;
	}

	private void updateWarningHeaderVisibility(boolean visible) {
		if (visible) {
			if (expandableList.getHeaderViewsCount() < 2) {
				expandableList.addHeaderView(availableSpaceContainer);
			}
			AndroidUiHelper.updateVisibility(headerShadow, false);
			AndroidUiHelper.updateVisibility(headerDivider, true);
		} else {
			expandableList.removeHeaderView(availableSpaceContainer);
			AndroidUiHelper.updateVisibility(headerShadow, true);
			AndroidUiHelper.updateVisibility(headerDivider, false);
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	@Override
	public void onCategorySelected(@NonNull ExportCategory exportCategory, boolean selected) {
		SettingsCategoryItems categoryItems = dataList.get(exportCategory);
		for (ExportType exportType : categoryItems.getNotEmptyTypes()) {
			List<?> selectedItems = selected ? categoryItems.getItemsForType(exportType) : new ArrayList<>();
			selectedItemsMap.put(exportType, selectedItems);
		}
		updateAvailableSpace();
	}

	@Override
	public void onItemsSelected(@NonNull ExportType exportType, List<?> selectedItems) {
		selectedItemsMap.put(exportType, selectedItems);
		adapter.notifyDataSetChanged();
		updateAvailableSpace();
	}

	@Nullable
	protected List<?> getItemsForType(@NonNull ExportType exportType) {
		for (SettingsCategoryItems categoryItems : dataList.values()) {
			if (categoryItems.getTypes().contains(exportType)) {
				return categoryItems.getItemsForType(exportType);
			}
		}
		return null;
	}

	@Nullable
	protected List<Object> getSelectedItemsForType(@NonNull ExportType exportType) {
		List<?> itemsForType = selectedItemsMap.get(exportType);
		return itemsForType != null ? new ArrayList<>(itemsForType) : null;
	}

	@Override
	public void onTypeClicked(@NonNull ExportType exportType) {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager != null && exportType != ExportType.GLOBAL && exportType != ExportType.SEARCH_HISTORY && exportType != ExportType.NAVIGATION_HISTORY) {
			ExportItemsBottomSheet.showInstance(fragmentManager, exportType, this, exportMode);
		}
	}
}