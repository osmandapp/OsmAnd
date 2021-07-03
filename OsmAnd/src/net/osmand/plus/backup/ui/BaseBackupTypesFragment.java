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

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ui.BackupTypesAdapter.OnItemSelectedListener;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.BackupClearType;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.OnClearTypesListener;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.fragments.BaseSettingsListFragment;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseBackupTypesFragment extends BaseOsmAndFragment
		implements OnItemSelectedListener, OnClearTypesListener, OnDeleteFilesListener {

	protected OsmandApplication app;
	protected BackupHelper backupHelper;

	protected Map<ExportSettingsCategory, SettingsCategoryItems> dataList = new LinkedHashMap<>();
	protected Map<ExportSettingsType, List<?>> selectedItemsMap = new EnumMap<>(ExportSettingsType.class);

	protected ProgressBar progressBar;
	protected BackupClearType clearType;

	protected boolean nightMode;
	protected boolean wasDrawerDisabled;

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
		clearType = getClearType();
		dataList = getDataList();
		selectedItemsMap = getSelectedItems();
	}

	protected abstract int getTitleId();

	protected abstract BackupClearType getClearType();

	protected abstract RemoteFilesType getRemoteFilesType();

	protected abstract Map<ExportSettingsType, List<?>> getSelectedItems();

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

	protected void setupToolbar(View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(getTitleId());

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

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			wasDrawerDisabled = mapActivity.isDrawerDisabled();
			if (!wasDrawerDisabled) {
				mapActivity.disableDrawer();
			}
			backupHelper.getBackupListeners().addDeleteFilesListener(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !wasDrawerDisabled) {
			mapActivity.enableDrawer();
		}
		backupHelper.getBackupListeners().removeDeleteFilesListener(this);
	}

	@Override
	public void onCategorySelected(ExportSettingsCategory category, boolean selected) {
		SettingsCategoryItems categoryItems = dataList.get(category);
		List<ExportSettingsType> types = categoryItems.getTypes();
		for (ExportSettingsType type : types) {
			selectedItemsMap.put(type, selected ? categoryItems.getItemsForType(type) : null);
		}
		if (!selected) {
			showClearTypesBottomSheet(types);
		}
	}

	@Override
	public void onTypeSelected(ExportSettingsType type, boolean selected) {
		selectedItemsMap.put(type, selected ? getItemsForType(type) : null);

		if (!selected) {
			showClearTypesBottomSheet(Collections.singletonList(type));
		}
	}

	protected void showClearTypesBottomSheet(List<ExportSettingsType> types) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			ClearTypesBottomSheet.showInstance(activity.getSupportFragmentManager(), types, clearType, this);
		}
	}

	protected Map<ExportSettingsCategory, SettingsCategoryItems> getDataList() {
		List<RemoteFile> remoteFiles = backupHelper.getBackup().getRemoteFiles(getRemoteFilesType());
		if (remoteFiles == null) {
			remoteFiles = Collections.emptyList();
		}

		Map<ExportSettingsType, List<?>> settingsToOperate = new EnumMap<>(ExportSettingsType.class);
		for (ExportSettingsType type : ExportSettingsType.getEnabledTypes()) {
			List<RemoteFile> filesByType = new ArrayList<>();
			for (RemoteFile remoteFile : remoteFiles) {
				if (ExportSettingsType.getExportSettingsTypeForRemoteFile(remoteFile) == type) {
					filesByType.add(remoteFile);
				}
			}
			settingsToOperate.put(type, filesByType);
		}
		return SettingsHelper.getSettingsToOperateByCategory(settingsToOperate, true);
	}

	protected List<Object> getItemsForType(ExportSettingsType type) {
		for (SettingsCategoryItems categoryItems : dataList.values()) {
			if (categoryItems.getTypes().contains(type)) {
				return (List<Object>) categoryItems.getItemsForType(type);
			}
		}
		return Collections.emptyList();
	}

	@Override
	public void onFileDeleteProgress(@NonNull RemoteFile file) {
		updateProgressVisibility(true);
	}

	@Override
	public void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {
		updateProgressVisibility(false);
		backupHelper.prepareBackup();
	}

	@Override
	public void onFilesDeleteError(int status, @NonNull String message) {
		updateProgressVisibility(false);
		backupHelper.prepareBackup();
	}

	protected void updateProgressVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(progressBar, visible);
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
}