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

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ui.BackupTypesAdapter.OnItemSelectedListener;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.BackupClearType;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.OnClearTypesListener;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.OsmAndProPlanFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper.InAppPurchaseListener;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.fragments.BaseSettingsListFragment;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseBackupTypesFragment extends BaseOsmAndFragment implements OnItemSelectedListener,
		OnClearTypesListener, OnDeleteFilesListener, InAppPurchaseListener {

	protected BackupHelper backupHelper;

	protected Map<ExportCategory, SettingsCategoryItems> dataList = new LinkedHashMap<>();
	protected Map<ExportType, List<?>> selectedItemsMap = new EnumMap<>(ExportType.class);

	protected ProgressBar progressBar;
	protected BackupTypesAdapter adapter;
	protected BackupClearType clearType;

	protected boolean cloudRestore;
	protected boolean wasDrawerDisabled;

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		backupHelper = app.getBackupHelper();
		clearType = getClearType();
		dataList = getDataList();
		selectedItemsMap = getSelectedItems();
		cloudRestore = requireMapActivity().getFragmentsHelper().isFirstScreenShowing();
	}

	protected abstract int getTitleId();

	protected abstract BackupClearType getClearType();

	protected abstract RemoteFilesType getRemoteFilesType();

	protected abstract Map<ExportType, List<?>> getSelectedItems();

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_backup_types, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		setupToolbar(view);

		progressBar = view.findViewById(R.id.progress_bar);

		adapter = new BackupTypesAdapter(app, this, cloudRestore, nightMode);
		adapter.updateSettingsItems(dataList, selectedItemsMap);

		ExpandableListView expandableList = view.findViewById(R.id.list);
		expandableList.setAdapter(adapter);
		BaseSettingsListFragment.setupListView(expandableList);

		return view;
	}

	protected void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(getTitleId());

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
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
	public void onCategorySelected(ExportCategory exportCategory, boolean selected) {
		boolean hasItemsToDelete = false;
		SettingsCategoryItems categoryItems = dataList.get(exportCategory);
		List<ExportType> exportTypes = categoryItems.getTypes();
		for (ExportType exportType : exportTypes) {
			if (isExportTypeAvailable(exportType)) {
				List<?> items = getItemsForType(exportType);
				hasItemsToDelete |= !Algorithms.isEmpty(items);
				selectedItemsMap.put(exportType, selected ? items : null);
			}
		}
		if (!selected && hasItemsToDelete) {
			showClearTypesBottomSheet(exportTypes);
		}
	}

	@Override
	public void onTypeSelected(@NonNull ExportType exportType, boolean selected) {
		if (isExportTypeAvailable(exportType)) {
			List<?> items = getItemsForType(exportType);
			selectedItemsMap.put(exportType, selected ? items : null);
			if (!selected && !Algorithms.isEmpty(items)) {
				showClearTypesBottomSheet(Collections.singletonList(exportType));
			}
		} else {
			OsmAndProPlanFragment.showInstance(requireActivity());
		}
	}

	protected boolean isExportTypeAvailable(@NonNull ExportType exportType) {
		return InAppPurchaseUtils.isExportTypeAvailable(app, exportType) || cloudRestore;
	}

	protected void showClearTypesBottomSheet(List<ExportType> types) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			ClearTypesBottomSheet.showInstance(activity.getSupportFragmentManager(), types, clearType, this);
		}
	}

	@NonNull
	protected Map<ExportCategory, SettingsCategoryItems> getDataList() {
		Map<String, RemoteFile> remoteFiles = backupHelper.getBackup().getRemoteFiles(getRemoteFilesType());
		if (remoteFiles == null) {
			remoteFiles = Collections.emptyMap();
		}
		Map<ExportType, List<?>> dataToOperate = new EnumMap<>(ExportType.class);
		for (ExportType exportType : ExportType.enabledValues()) {
			List<RemoteFile> filesByType = new ArrayList<>();
			for (RemoteFile remoteFile : remoteFiles.values()) {
				if (ExportType.findBy(remoteFile) == exportType) {
					filesByType.add(remoteFile);
				}
			}
			dataToOperate.put(exportType, filesByType);
		}
		return SettingsHelper.categorizeSettingsToOperate(dataToOperate, true);
	}

	@NonNull
	protected List<?> getItemsForType(@NonNull ExportType exportType) {
		for (SettingsCategoryItems categoryItems : dataList.values()) {
			if (categoryItems.getTypes().contains(exportType)) {
				return categoryItems.getItemsForType(exportType);
			}
		}
		return Collections.emptyList();
	}

	@Override
	public void onFileDeleteProgress(@NonNull RemoteFile file, int progress) {
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

	@Override
	public void onItemPurchased(String sku, boolean active) {
		dataList = getDataList();
		selectedItemsMap = getSelectedItems();

		if (isResumed() && adapter != null) {
			adapter.updateSettingsItems(dataList, selectedItemsMap);
		}
	}

	protected void updateProgressVisibility(boolean visible) {
		AndroidUiHelper.updateVisibility(progressBar, visible);
	}

	@NonNull
	protected MapActivity requireMapActivity() {
		return ((MapActivity) requireActivity());
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