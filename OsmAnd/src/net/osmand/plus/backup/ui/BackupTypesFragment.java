package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.ui.BackupTypesAdapter.OnItemSelectedListener;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.fragments.BaseSettingsListFragment;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BackupTypesFragment extends BaseOsmAndFragment implements OnItemSelectedListener {

	public static final String TAG = BackupTypesFragment.class.getSimpleName();

	private OsmandApplication app;
	private BackupHelper backupHelper;

	private Map<ExportSettingsCategory, SettingsCategoryItems> dataList = new LinkedHashMap<>();
	protected Map<ExportSettingsType, List<?>> selectedItemsMap = new EnumMap<>(ExportSettingsType.class);

	private boolean nightMode;
	private boolean wasDrawerDisabled;

	@Override
	public int getStatusBarColorId() {
		return nightMode ? R.color.status_bar_color_dark : R.color.status_bar_color_light;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		backupHelper = app.getBackupHelper();
		nightMode = !app.getSettings().isLightContent();
		dataList = app.getFileSettingsHelper().getSettingsByCategory(true);
		selectedItemsMap = getSelectedItems();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View root = themedInflater.inflate(R.layout.fragment_backup_types, container, false);
		AndroidUtils.addStatusBarPadding21v(app, root);
		setupToolbar(root);

		BackupTypesAdapter adapter = new BackupTypesAdapter(app, this, nightMode);
		adapter.updateSettingsItems(dataList, selectedItemsMap);

		ExpandableListView expandableList = root.findViewById(R.id.list);
		expandableList.setAdapter(adapter);
		BaseSettingsListFragment.setupListView(expandableList);

		return root;
	}

	@Override
	public void onCategorySelected(ExportSettingsCategory category, boolean selected) {
		SettingsCategoryItems categoryItems = dataList.get(category);
		for (ExportSettingsType type : categoryItems.getTypes()) {
			backupHelper.getBackupTypePref(type).set(selected);
			selectedItemsMap.put(type, selected ? categoryItems.getItemsForType(type) : new ArrayList<>());
		}
	}

	@Override
	public void onTypeSelected(ExportSettingsType type, boolean selected) {
		backupHelper.getBackupTypePref(type).set(selected);
		selectedItemsMap.put(type, selected ? getItemsForType(type) : new ArrayList<>());
	}

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

	private void setupToolbar(View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.backup_data);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		toolbar.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity activity = getActivity();
				if (activity != null) {
					activity.onBackPressed();
				}
			}
		});
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	protected Map<ExportSettingsType, List<?>> getSelectedItems() {
		Map<ExportSettingsType, List<?>> selectedItemsMap = new EnumMap<>(ExportSettingsType.class);
		for (ExportSettingsType type : ExportSettingsType.values()) {
			if (backupHelper.getBackupTypePref(type).get()) {
				selectedItemsMap.put(type, getItemsForType(type));
			}
		}
		return selectedItemsMap;
	}

	protected List<Object> getItemsForType(ExportSettingsType type) {
		for (SettingsCategoryItems categoryItems : dataList.values()) {
			if (categoryItems.getTypes().contains(type)) {
				return (List<Object>) categoryItems.getItemsForType(type);
			}
		}
		return null;
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

	public static void showInstance(@NonNull FragmentManager manager) {
		if (manager.findFragmentByTag(TAG) == null) {
			BackupTypesFragment fragment = new BackupTypesFragment();
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}