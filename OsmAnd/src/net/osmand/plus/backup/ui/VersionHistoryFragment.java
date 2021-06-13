package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupHelper.OnDeleteFilesListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.backup.ui.BackupTypesAdapter.OnItemSelectedListener;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.items.SettingsItem;
import net.osmand.plus.settings.fragments.BaseSettingsListFragment;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;

import org.apache.commons.logging.Log;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VersionHistoryFragment extends BaseOsmAndFragment implements OnItemSelectedListener {

	public static final String TAG = VersionHistoryFragment.class.getSimpleName();
	private static final Log log = PlatformUtil.getLog(VersionHistoryFragment.class);

	private OsmandApplication app;
	private BackupHelper backupHelper;

	private List<SettingsItem> settingsItems;
	private Map<ExportSettingsCategory, SettingsCategoryItems> dataList = new LinkedHashMap<>();
	protected Map<ExportSettingsType, List<?>> selectedItemsMap = new EnumMap<>(ExportSettingsType.class);

	private ProgressBar progressBar;

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
		dataList = SettingsHelper.getSettingsToOperateByCategory(settingsItems, false, true);
		selectedItemsMap = getSelectedItems();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.fragment_backup_types, container, false);
		AndroidUtils.addStatusBarPadding21v(app, view);
		setupToolbar(view);

		progressBar = view.findViewById(R.id.progress_bar);

		BackupTypesAdapter adapter = new BackupTypesAdapter(app, this, nightMode);
		adapter.updateSettingsItems(dataList, selectedItemsMap);

		ExpandableListView expandableList = view.findViewById(R.id.list);
		expandableList.setAdapter(adapter);
		BaseSettingsListFragment.setupListView(expandableList);

		return view;
	}

	@Override
	public void onCategorySelected(ExportSettingsCategory category, boolean selected) {
		SettingsCategoryItems categoryItems = dataList.get(category);
		List<ExportSettingsType> types = categoryItems.getTypes();
		for (ExportSettingsType type : types) {
			backupHelper.getVersionHistoryTypePref(type).set(selected);
			selectedItemsMap.put(type, selected ? categoryItems.getItemsForType(type) : null);
		}
		if (!selected) {
			showClearHistoryBottomSheet(types);
		}
	}

	@Override
	public void onTypeSelected(ExportSettingsType type, boolean selected) {
		backupHelper.getVersionHistoryTypePref(type).set(selected);
		selectedItemsMap.put(type, selected ? getItemsForType(type) : null);

		if (!selected) {
			showClearHistoryBottomSheet(Collections.singletonList(type));
		}
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

	protected void deleteHistoryForTypes(List<ExportSettingsType> types) {
		try {
			updateProgressVisibility(true);
			backupHelper.deleteOldFiles(getOnDeleteFilesListener(), types);
		} catch (UserNotRegisteredException e) {
			log.error(e);
		}
	}

	private OnDeleteFilesListener getOnDeleteFilesListener() {
		return new OnDeleteFilesListener() {
			@Override
			public void onFileDeleteProgress(@NonNull RemoteFile file) {
				updateProgressVisibility(true);
			}

			@Override
			public void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {
				updateProgressVisibility(false);
			}

			@Override
			public void onFilesDeleteError(int status, @NonNull String message) {
				updateProgressVisibility(false);
			}
		};
	}

	private void updateProgressVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(progressBar, visible);
	}

	private void showClearHistoryBottomSheet(List<ExportSettingsType> types) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			VersionHistoryClearBottomSheet.showInstance(activity.getSupportFragmentManager(), types, this);
		}
	}

	private void setupToolbar(View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.backup_version_history);

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
			if (backupHelper.getVersionHistoryTypePref(type).get()) {
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
		return Collections.emptyList();
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

	public static void showInstance(@NonNull FragmentManager manager, @NonNull List<SettingsItem> oldItems) {
		if (manager.findFragmentByTag(TAG) == null) {
			VersionHistoryFragment fragment = new VersionHistoryFragment();
			fragment.settingsItems = oldItems;
			fragment.setRetainInstance(true);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}
