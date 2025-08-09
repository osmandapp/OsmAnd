package net.osmand.plus.backup.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.BackupListeners.OnDeleteFilesListener;
import net.osmand.plus.backup.PrepareBackupResult;
import net.osmand.plus.backup.PrepareBackupResult.RemoteFilesType;
import net.osmand.plus.backup.PrepareBackupTask.OnPrepareBackupListener;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.backup.ui.BackupTypesAdapter.OnItemSelectedListener;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.BackupClearType;
import net.osmand.plus.backup.ui.ClearTypesBottomSheet.OnClearTypesListener;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.inapp.InAppPurchaseUtils;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.SettingsHelper;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class BaseBackupTypesController extends BaseDialogController
		implements OnItemSelectedListener, OnClearTypesListener, OnDeleteFilesListener, OnPrepareBackupListener {

	protected static final Log LOG = PlatformUtil.getLog(BaseBackupTypesController.class);

	protected final BackupHelper backupHelper;
	protected final BackupClearType clearType;
	protected final RemoteFilesType remoteFilesType;
	protected Map<ExportCategory, SettingsCategoryItems> data = new LinkedHashMap<>();
	protected List<ExportCategory> categories;
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

	@NonNull
	public abstract BackupTypesAdapter createUiAdapter(@NonNull Context context);

	@StringRes
	public abstract int getTitleId();

	public void updateData() {
		this.data = collectData();
		this.categories = new ArrayList<>(data.keySet());
		Collections.sort(categories);
	}

	public void updateListeners(boolean register) {
		if (register) {
			backupHelper.getBackupListeners().addDeleteFilesListener(this);
			backupHelper.addPrepareBackupListener(this);
		} else {
			backupHelper.getBackupListeners().removeDeleteFilesListener(this);
			backupHelper.removePrepareBackupListener(this);
		}
	}

	@NonNull
	public Map<ExportCategory, SettingsCategoryItems> getData() {
		return data;
	}

	@NonNull
	public Collection<ExportCategory> getCategories() {
		return categories;
	}

	@NonNull
	public ExportCategory getCategory(int position) {
		return categories.get(position);
	}

	@NonNull
	public SettingsCategoryItems getCategoryItems(@NonNull ExportCategory category) {
		return Objects.requireNonNull(data.get(category));
	}

	@NonNull
	private Map<ExportCategory, SettingsCategoryItems> collectData() {
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

	@Override
	public void onBackupPreparing() {
	}

	@Override
	public void onBackupPrepared(@Nullable PrepareBackupResult backupResult) {
		screen.updateContent();
	}

	protected boolean isExportTypeAvailable(@NonNull ExportType exportType) {
		return InAppPurchaseUtils.isExportTypeAvailable(app, exportType) || cloudRestore;
	}

	protected boolean isBackupAvailable() {
		return InAppPurchaseUtils.isBackupAvailable(app) || cloudRestore;
	}

	protected boolean isBackupPreparing() {
		return backupHelper.isBackupPreparing();
	}

	@NonNull
	public List<RemoteFile> collectRemoteFilesForTypes(@NonNull Collection<ExportType> types) {
		List<RemoteFile> result = new ArrayList<>();
		for (ExportType exportType : types) {
			for (Object item : getItemsForType(exportType)) {
				if (item instanceof RemoteFile remoteFile) {
					result.add(remoteFile);
				}
			}
		}
		return result;
	}

	@NonNull
	public List<?> getItemsForType(@NonNull ExportType exportType) {
		for (SettingsCategoryItems categoryItems : data.values()) {
			if (categoryItems.getTypes().contains(exportType)) {
				return categoryItems.getItemsForType(exportType);
			}
		}
		return Collections.emptyList();
	}

	public long getMaximumAccountSize() {
		return backupHelper.getMaximumAccountSize();
	}
}
