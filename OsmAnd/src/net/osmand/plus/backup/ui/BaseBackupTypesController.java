package net.osmand.plus.backup.ui;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.UserNotRegisteredException;
import net.osmand.plus.backup.ui.BackupTypesAdapter.OnItemSelectedListener;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.BackupClearType;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.OnClearTypesListener;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.chooseplan.OsmAndProPlanFragment;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseBackupTypesController extends BaseDialogController
		implements OnItemSelectedListener, OnClearTypesListener, OnDeleteFilesListener {

	private static final Log LOG = PlatformUtil.getLog(BaseBackupTypesController.class);

	protected final BackupHelper backupHelper;
	protected final BackupClearType clearType;
	protected final RemoteFilesType remoteFilesType;
	protected Map<ExportCategory, SettingsCategoryItems> dataList = new LinkedHashMap<>();
	protected Map<ExportType, List<?>> selectedItemsMap = new EnumMap<>(ExportType.class);
	protected BackupTypesFragment screen;
	protected boolean cloudRestore;

	public BaseBackupTypesController(@NonNull OsmandApplication app,
	                                 @NonNull BackupClearType clearType,
	                                 @NonNull RemoteFilesType remoteFilesType) {
		super(app);
		this.backupHelper = app.getBackupHelper();
		this.clearType = clearType;
		this.remoteFilesType = remoteFilesType;
	}

	@Override
	public void registerDialog(@NonNull IDialog dialog) {
		super.registerDialog(dialog);
		this.screen = (BackupTypesFragment) dialog;
		cloudRestore = screen.requireMapActivity().getFragmentsHelper().isFirstScreenShowing();
		updateData();
	}

	public void onResume() {
		backupHelper.getBackupListeners().addDeleteFilesListener(this);
	}

	public void onPause() {
		backupHelper.getBackupListeners().removeDeleteFilesListener(this);
	}

	@StringRes
	public abstract int getTitleId();

	public void updateData() {
		this.dataList = collectAllItems();
		this.selectedItemsMap = collectSelectedItems();
	}

	@NonNull
	public Map<ExportCategory, SettingsCategoryItems> getDataList() {
		return dataList;
	}

	@NonNull
	public Map<ExportType, List<?>> getSelectedItems() {
		return selectedItemsMap;
	}

	@NonNull
	private Map<ExportCategory, SettingsCategoryItems> collectAllItems() {
		Map<String, RemoteFile> remoteFiles = backupHelper.getBackup().getRemoteFiles(remoteFilesType);
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
	protected abstract Map<ExportType, List<?>> collectSelectedItems();

	@NonNull
	public List<?> getItemsForType(@NonNull ExportType exportType) {
		for (SettingsCategoryItems categoryItems : dataList.values()) {
			if (categoryItems.getTypes().contains(exportType)) {
				return categoryItems.getItemsForType(exportType);
			}
		}
		return Collections.emptyList();
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
			FragmentActivity activity = getActivity();
			if (activity != null) {
				OsmAndProPlanFragment.showInstance(activity);
			}
		}
	}

	protected void showClearTypesBottomSheet(List<ExportType> types) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			ClearTypesBottomSheet.showInstance(activity, this, types);
		}
	}

	// TODO: delete
	protected boolean isExportTypeAvailable(@NonNull ExportType exportType) {
		return InAppPurchaseUtils.isExportTypeAvailable(app, exportType) || cloudRestore;
	}

	protected boolean isBackupAvailable() {
		return InAppPurchaseUtils.isBackupAvailable(app) || cloudRestore;
	}

	@Override
	public void onClearTypesConfirmed(@NonNull List<ExportType> types) {
		try {
			screen.updateProgressVisibility(true);
			clearDataForTypes(types);
		} catch (UserNotRegisteredException e) {
			screen.updateProgressVisibility(false);
			LOG.error(e);
		}
	}

	protected abstract void clearDataForTypes(@NonNull List<ExportType> types) throws UserNotRegisteredException;

	@Override
	public void onFileDeleteProgress(@NonNull RemoteFile file, int progress) {
		screen.updateProgressVisibility(true);
	}

	@Override
	public void onFilesDeleteDone(@NonNull Map<RemoteFile, String> errors) {
		screen.updateProgressVisibility(false);
		backupHelper.prepareBackup();
	}

	@Override
	public void onFilesDeleteError(int status, @NonNull String message) {
		screen.updateProgressVisibility(false);
		backupHelper.prepareBackup();
	}

	public boolean isCloudRestore() {
		return cloudRestore;
	}
}
