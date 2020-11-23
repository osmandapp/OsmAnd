package net.osmand.plus.settings.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.fragments.ExportSettingsAdapter.OnItemSelectedListener;
import net.osmand.plus.widgets.TextViewEx;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseSettingsListFragment extends BaseOsmAndFragment implements OnItemSelectedListener {

	protected static final String SETTINGS_LIST_TAG = "settings_list_tag";

	protected OsmandApplication app;

	protected Map<ExportSettingsType, List<?>> selectedItemsMap = new EnumMap<>(ExportSettingsType.class);
	protected Map<ExportSettingsCategory, SettingsCategoryItems> dataList = new LinkedHashMap<>();

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

	protected boolean nightMode;
	private boolean wasDrawerDisabled;

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		nightMode = !app.getSettings().isLightContent();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View root = themedInflater.inflate(R.layout.fragment_import, container, false);
		AndroidUtils.addStatusBarPadding21v(app, root);

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
		UiUtilities.setupDialogButton(nightMode, continueBtn, DialogButtonType.PRIMARY, getString(R.string.shared_string_continue));
		continueBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onContinueButtonClickAction();
			}
		});

		ViewTreeObserver treeObserver = buttonsContainer.getViewTreeObserver();
		treeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				if (buttonsContainer != null) {
					ViewTreeObserver vts = buttonsContainer.getViewTreeObserver();
					int height = buttonsContainer.getMeasuredHeight();
					expandableList.setPadding(0, 0, 0, height);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
						vts.removeOnGlobalLayoutListener(this);
					} else {
						vts.removeGlobalOnLayoutListener(this);
					}
				}
			}
		});

		adapter = new ExportSettingsAdapter(app, this, nightMode);
		adapter.updateSettingsItems(dataList, selectedItemsMap);
		expandableList.setAdapter(adapter);
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
		dismissDialog.setPositiveButton(R.string.shared_string_exit, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismissFragment();
			}
		});
		dismissDialog.show();
	}

	private void setupToolbar(Toolbar toolbar) {
		toolbar.setNavigationIcon(getPaintedContentIcon(R.drawable.ic_action_close, nightMode
				? getResources().getColor(R.color.active_buttons_and_links_text_dark)
				: getResources().getColor(R.color.active_buttons_and_links_text_light)));
		toolbar.setNavigationContentDescription(R.string.shared_string_close);
		toolbar.setNavigationOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showExitDialog();
			}
		});
	}

	protected void updateAvailableSpace() {
		long calculatedSize = ExportSettingsAdapter.calculateItemsSize(adapter.getData());
		if (calculatedSize != 0) {
			selectedItemsSize.setText(AndroidUtils.formatSize(app, calculatedSize));

			File dir = app.getAppPath("").getParentFile();
			long availableSizeBytes = AndroidUtils.getAvailableSpace(dir);
			if (calculatedSize > availableSizeBytes) {
				String availableSize = AndroidUtils.formatSize(app, availableSizeBytes);
				availableSpaceDescr.setText(getString(R.string.export_not_enough_space_descr, availableSize));
				updateWarningHeaderVisibility(true);
				continueBtn.setEnabled(false);
			} else {
				updateWarningHeaderVisibility(false);
				continueBtn.setEnabled(adapter.hasSelectedData());
			}
			itemsSizeContainer.setVisibility(View.VISIBLE);
		} else {
			updateWarningHeaderVisibility(false);
			itemsSizeContainer.setVisibility(View.INVISIBLE);
			continueBtn.setEnabled(adapter.hasSelectedData());
		}
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
	public void onCategorySelected(ExportSettingsCategory category, boolean selected) {
		SettingsCategoryItems categoryItems = dataList.get(category);
		for (ExportSettingsType type : categoryItems.getTypes()) {
			List<?> selectedItems = selected ? categoryItems.getItemsForType(type) : new ArrayList<>();
			selectedItemsMap.put(type, selectedItems);
		}
		updateAvailableSpace();
	}

	@Override
	public void onItemsSelected(ExportSettingsType type, List<?> selectedItems) {
		selectedItemsMap.put(type, selectedItems);
		adapter.notifyDataSetChanged();
		updateAvailableSpace();
	}

	@Override
	public void onTypeClicked(ExportSettingsCategory category, ExportSettingsType type) {
		FragmentManager fragmentManager = getFragmentManager();
		if (fragmentManager != null && type != ExportSettingsType.GLOBAL && type != ExportSettingsType.SEARCH_HISTORY) {
			List<Object> items = (List<Object>) dataList.get(category).getItemsForType(type);
			List<Object> selectedItems = (List<Object>) selectedItemsMap.get(type);
			ExportItemsBottomSheet.showInstance(type, selectedItems, items, fragmentManager, this);
		}
	}
}